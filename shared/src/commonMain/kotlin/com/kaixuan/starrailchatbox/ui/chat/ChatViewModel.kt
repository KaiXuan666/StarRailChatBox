package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatMessageStatus
import com.kaixuan.starrailchatbox.data.chat.ChatRole
import com.kaixuan.starrailchatbox.data.chat.ChatSession
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.chat.ChatContextSnapshot
import com.kaixuan.starrailchatbox.data.chat.ChatSummaryCoordinator
import com.kaixuan.starrailchatbox.data.chat.NewChatMessage
import com.kaixuan.starrailchatbox.data.chat.NewChatSession
import com.kaixuan.starrailchatbox.data.chat.StoredChatMessage
import com.kaixuan.starrailchatbox.data.chat.buildChatContext
import com.kaixuan.starrailchatbox.data.chat.newChatId
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.platform.formatLocalTime
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import org.jetbrains.compose.resources.getString
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.chat_new_session_title

class ChatViewModel(
    private val characterRepository: CharacterRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val aiRepository: AiRepository,
    private val chatSummaryCoordinator: ChatSummaryCoordinator = ChatSummaryCoordinator(
        chatSessionRepository = chatSessionRepository,
        aiRepository = aiRepository,
    ),
    private val currentTimeMillis: () -> Long = {
        Clock.System.now().toEpochMilliseconds()
    },
    private val idGenerator: (String) -> String = { prefix -> newChatId(prefix) },
    private val sessionTitleProvider: suspend () -> String = {
        getString(Res.string.chat_new_session_title)
    },
    private val timeFormatter: (Long) -> String = ::formatLocalTime,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var activeSession: ChatSession? = null
    private var sessionJob: Job? = null
    private var sessionListJob: Job? = null

    init {
        viewModelScope.launch {
            val characters = runCatching { characterRepository.loadCharacters() }
                .getOrDefault(emptyList())
            val selectedId = characters.firstOrNull { it.name == "流萤" }?.id
                ?: characters.firstOrNull()?.id
            val initialStates = characters.associate { character ->
                character.id to CharacterChatState(
                    activeSessionId = null,
                    messages = emptyList(),
                    messageDraft = "",
                    isLoadingSession = character.id == selectedId,
                    isSending = false,
                )
            }
            _uiState.update {
                it.copy(
                    characters = characters,
                    selectedCharacterId = selectedId,
                    characterStates = initialStates,
                    isLoadingCharacters = false,
                )
            }
            selectedId?.let {
                observeSessions(it)
                loadLatestSession(it)
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.CharacterSelected -> selectCharacter(action.characterId)
            is ChatAction.MessageChanged -> updateMessageDraft(action.message)
            ChatAction.SendClicked -> sendMessage()
            is ChatAction.QuickReplyClicked -> {
                val state = uiState.value
                val character = state.selectedCharacter
                if (character != null && !state.isSending && !state.isLoadingSession) {
                    _uiState.update { s ->
                        val curState = s.characterStates[character.id] ?: CharacterChatState()
                        s.copy(
                            characterStates = s.characterStates + (character.id to curState.copy(
                                isSending = true
                            ))
                        )
                    }
                    viewModelScope.launch {
                        try {
                            sendMessage(character, action.message)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                            emitMessage(EffectMessage.CHAT_REQUEST_FAILED)
                        } finally {
                            _uiState.update { s ->
                                val curState = s.characterStates[character.id] ?: CharacterChatState()
                                s.copy(
                                    characterStates = s.characterStates + (character.id to curState.copy(
                                        isSending = false
                                    ))
                                )
                            }
                        }
                    }
                }
            }
            ChatAction.NewSessionClicked -> startNewSession()
            is ChatAction.SessionSelected -> selectSession(action.sessionId)
            is ChatAction.SessionDeleteClicked -> deleteSession(action.sessionId)
            is ChatAction.HeaderActionClicked -> handleHeaderAction(action.action)
            is ChatAction.ComposerActionClicked -> handleComposerAction(action.action)
        }
    }

    private fun updateMessageDraft(message: String) {
        val selectedId = _uiState.value.selectedCharacterId
        if (selectedId != null) {
            _uiState.update { state ->
                val currentState = state.characterStates[selectedId] ?: CharacterChatState()
                state.copy(
                    characterStates = state.characterStates + (selectedId to currentState.copy(messageDraft = message))
                )
            }
        }
    }

    private fun selectCharacter(characterId: String) {
        val state = uiState.value
        if (state.characters.none { it.id == characterId }) return
        if (state.selectedCharacterId == characterId) return

        _uiState.update {
            it.copy(selectedCharacterId = characterId)
        }
        observeSessions(characterId)
        val hasCache = state.characterStates[characterId]?.messages?.isNotEmpty() == true
        loadLatestSession(characterId, hasCache = hasCache)
    }

    private fun observeSessions(characterId: String) {
        sessionListJob?.cancel()
        sessionListJob = viewModelScope.launch {
            chatSessionRepository.observeSessions(characterId).collect { sessions ->
                _uiState.update { state ->
                    val currentState = state.characterStates[characterId] ?: CharacterChatState()
                    state.copy(
                        characterStates = state.characterStates + (
                            characterId to currentState.copy(
                                sessions = sessions.map { summary ->
                                    ConversationSummaryUiModel(
                                        id = summary.session.id,
                                        title = summary.session.title,
                                        preview = summary.lastMessagePreview,
                                        updatedAt = timeFormatter(summary.session.lastMessageAt),
                                        messageCount = summary.messageCount,
                                    )
                                },
                            )
                        ),
                    )
                }
            }
        }
    }

    private fun loadLatestSession(characterId: String, hasCache: Boolean = false) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            if (!hasCache) {
                _uiState.update { state ->
                    val currentState = state.characterStates[characterId] ?: CharacterChatState()
                    state.copy(
                        characterStates = state.characterStates + (characterId to currentState.copy(isLoadingSession = true))
                    )
                }
            }
            val session = runCatching {
                chatSessionRepository.findLatestSession(characterId)
            }.getOrNull()
            if (uiState.value.selectedCharacterId != characterId) return@launch
            activeSession = session
            if (session == null) {
                val greeting = emptyGreeting(
                    character = uiState.value.selectedCharacter,
                    now = currentTimeMillis(),
                    timeFormatter = timeFormatter,
                )
                _uiState.update { state ->
                    val currentState = state.characterStates[characterId] ?: CharacterChatState()
                    state.copy(
                        characterStates = state.characterStates + (characterId to currentState.copy(
                            activeSessionId = null,
                            messages = greeting,
                            isLoadingSession = false,
                        ))
                    )
                }
                return@launch
            }
            _uiState.update { state ->
                val currentState = state.characterStates[characterId] ?: CharacterChatState()
                state.copy(
                    characterStates = state.characterStates + (characterId to currentState.copy(
                        activeSessionId = session.id,
                        messages = if (hasCache) currentState.messages else emptyList(),
                        isLoadingSession = false,
                    ))
                )
            }
            chatSessionRepository.observeMessages(session.id).collect { messages ->
                val uiModels = messages.toUiModels(characterId, timeFormatter)
                val lastMsg = messages.lastOrNull()
                val suggestions = if (lastMsg != null && lastMsg.role == ChatRole.ASSISTANT && lastMsg.status == ChatMessageStatus.COMPLETED) {
                    lastMsg.suggestions
                } else {
                    emptyList()
                }
                _uiState.update { state ->
                    val currentState = state.characterStates[characterId] ?: CharacterChatState()
                    state.copy(
                        characterStates = state.characterStates + (characterId to currentState.copy(
                            messages = uiModels,
                            suggestions = suggestions,
                            isLoadingSession = false,
                        ))
                    )
                }
            }
        }
    }

    private fun sendMessage() {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterId = character.id
        val characterState = state.characterStates[characterId] ?: CharacterChatState()
        val content = characterState.messageDraft.trim()
        if (
            content.isEmpty() ||
            characterState.isSending ||
            characterState.isLoadingSession
        ) {
            return
        }
        _uiState.update { s ->
            val curState = s.characterStates[characterId] ?: CharacterChatState()
            s.copy(
                characterStates = s.characterStates + (characterId to curState.copy(
                    messageDraft = "",
                    isSending = true
                ))
            )
        }
        viewModelScope.launch {
            try {
                sendMessage(character, content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                emitMessage(EffectMessage.CHAT_REQUEST_FAILED)
            } finally {
                _uiState.update { s ->
                    val curState = s.characterStates[characterId] ?: CharacterChatState()
                    s.copy(
                        characterStates = s.characterStates + (characterId to curState.copy(
                            isSending = false
                        ))
                    )
                }
            }
        }
    }

    private fun startNewSession() {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterState = state.characterStates[character.id] ?: return
        if (characterState.isSending || characterState.isLoadingSession) return

        sessionJob?.cancel()
        activeSession = null
        _uiState.update { current ->
            val currentState = current.characterStates[character.id] ?: CharacterChatState()
            current.copy(
                characterStates = current.characterStates + (
                    character.id to currentState.copy(
                        activeSessionId = null,
                        messages = emptyGreeting(
                            character = character,
                            now = currentTimeMillis(),
                            timeFormatter = timeFormatter,
                        ),
                        suggestions = emptyList(),
                    )
                ),
            )
        }
    }

    private fun selectSession(sessionId: String) {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterState = state.characterStates[character.id] ?: return
        if (
            characterState.isSending ||
            characterState.isLoadingSession ||
            characterState.activeSessionId == sessionId ||
            characterState.sessions.none { it.id == sessionId }
        ) {
            return
        }
        loadSession(character.id, sessionId)
    }

    private fun loadSession(characterId: String, sessionId: String) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            updateCharacterState(characterId) { it.copy(isLoadingSession = true) }
            val session = runCatching {
                chatSessionRepository.findSession(sessionId)
            }.getOrNull()
            if (
                session == null ||
                session.agentId != characterId ||
                uiState.value.selectedCharacterId != characterId
            ) {
                updateCharacterState(characterId) { it.copy(isLoadingSession = false) }
                return@launch
            }
            activeSession = session
            updateCharacterState(characterId) {
                it.copy(
                    activeSessionId = session.id,
                    messages = emptyList(),
                    suggestions = emptyList(),
                    isLoadingSession = false,
                )
            }
            collectSessionMessages(characterId, session.id)
        }
    }

    private fun deleteSession(sessionId: String) {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterState = state.characterStates[character.id] ?: return
        if (
            characterState.isSending ||
            characterState.isLoadingSession ||
            characterState.sessions.none { it.id == sessionId }
        ) {
            return
        }
        viewModelScope.launch {
            runCatching {
                chatSessionRepository.deleteSession(sessionId, currentTimeMillis())
            }.onSuccess {
                if (characterState.activeSessionId == sessionId) {
                    loadLatestSession(character.id)
                }
            }
        }
    }

    private suspend fun sendMessage(
        character: Character,
        content: String,
    ) {
        val config = modelConfigRepository.getDefault()
            ?.takeIf(ModelConfig::isUsable)
        val previousSession = activeSession
        var context = previousSession?.let {
            chatSessionRepository.findContext(
                sessionId = it.id,
                maxHistoryMessageCount = null,
            )
        } ?: ChatContextSnapshot(summary = null, messages = emptyList())
        val now = currentTimeMillis()
        val session = previousSession ?: NewChatSession(
            id = idGenerator("session"),
            title = sessionTitleProvider(),
            agentId = character.id,
            modelConfigId = config?.id,
            systemPromptSnapshot = character.prompt,
            maxContextMessageCount = null,
            createdAt = now,
        ).let { newSession ->
            val userMessage = newUserMessage(
                sessionId = newSession.id,
                content = content,
                config = config,
                now = now,
            )
            val openingMessage = character.openingMessage
                .takeIf(String::isNotBlank)
                ?.let {
                    NewChatMessage(
                        id = idGenerator("message"),
                        sessionId = newSession.id,
                        role = ChatRole.ASSISTANT,
                        content = it,
                        status = ChatMessageStatus.COMPLETED,
                        modelConfigId = config?.id,
                        modelNameSnapshot = config?.modelName,
                        createdAt = now,
                    )
                }
            val initialMessages = listOfNotNull(openingMessage, userMessage)
            chatSessionRepository.createSessionWithMessages(newSession, initialMessages)
            context = ChatContextSnapshot(
                summary = null,
                messages = openingMessage?.let {
                    listOf(it.toStored(seq = 1))
                }.orEmpty(),
            )
            newSession.toDomain(lastMessageAt = now).also {
                activeSession = it
                observeCreatedSession(it, character.id)
            }
        }
        if (previousSession != null) {
            chatSessionRepository.appendMessage(
                newUserMessage(
                    sessionId = session.id,
                    content = content,
                    config = config,
                    now = now,
                ),
            )
        }

        if (config == null) {
            appendFailedAssistant(
                session = session,
                config = null,
                errorCode = "model_config_required",
                errorMessage = "A usable model configuration is required.",
            )
            emitMessage(EffectMessage.MODEL_CONFIG_REQUIRED)
            return
        }

        val prompt = session.customSystemPrompt
            ?.takeIf(String::isNotBlank)
            ?: session.systemPromptSnapshot
        val requestMessages = buildChatContext(
            systemPrompt = prompt,
            summary = context.summary,
            history = context.messages,
            currentUserMessage = content,
            maxHistoryMessageCount = session.maxContextMessageCount,
        )

        Napier.d("maxContextMessageCount: ${session.maxContextMessageCount}, summary: ${context.summary} content: $content, history size: ${context.messages.size}", tag = "sendMessage")
        context.messages.forEach {
            Napier.d(tag = "sendMessage") { "messages item=${it}" }
        }
        when (val result = aiRepository.createChatCompletion(config, requestMessages, character.name)) {
            is ApiResult.Success -> {
                if (handleSuccess(session, config, result.value)) {
                    viewModelScope.launch {
                        try {
                            chatSummaryCoordinator.summarizeIfNeeded(session, config)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                            // Summary generation is best-effort and must not fail the chat request.
                        }
                    }
                }
            }
            is ApiResult.HttpError -> handleFailure(
                session,
                config,
                "http_${result.statusCode}",
                result.message,
            )
            is ApiResult.NetworkError -> handleFailure(
                session,
                config,
                "network_error",
                result.message,
            )
            is ApiResult.UnexpectedError -> handleFailure(
                session,
                config,
                "unexpected_error",
                result.message,
            )
        }
    }

    private fun observeCreatedSession(session: ChatSession, characterId: String) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            _uiState.update { state ->
                val currentState = state.characterStates[characterId] ?: CharacterChatState()
                state.copy(
                    characterStates = state.characterStates + (characterId to currentState.copy(
                        activeSessionId = session.id,
                        isLoadingSession = false,
                    ))
                )
            }
            collectSessionMessages(characterId, session.id)
        }
    }

    private suspend fun collectSessionMessages(characterId: String, sessionId: String) {
        chatSessionRepository.observeMessages(sessionId).collect { messages ->
            val lastMsg = messages.lastOrNull()
            val suggestions = if (
                lastMsg != null &&
                lastMsg.role == ChatRole.ASSISTANT &&
                lastMsg.status == ChatMessageStatus.COMPLETED
            ) {
                lastMsg.suggestions
            } else {
                emptyList()
            }
            updateCharacterState(characterId) {
                it.copy(
                    messages = messages.toUiModels(characterId, timeFormatter),
                    suggestions = suggestions,
                    isLoadingSession = false,
                )
            }
        }
    }

    private fun updateCharacterState(
        characterId: String,
        transform: (CharacterChatState) -> CharacterChatState,
    ) {
        _uiState.update { state ->
            val currentState = state.characterStates[characterId] ?: CharacterChatState()
            state.copy(
                characterStates = state.characterStates + (
                    characterId to transform(currentState)
                ),
            )
        }
    }

    private suspend fun handleSuccess(
        session: ChatSession,
        config: ModelConfig,
        result: ChatCompletionResult,
    ): Boolean {
        val response = result.content.trim()
        if (response.isEmpty()) {
            appendFailedAssistant(
                session,
                config,
                errorCode = "empty_response",
                errorMessage = "Chat completion returned empty content.",
            )
            emitMessage(EffectMessage.CHAT_EMPTY_RESPONSE)
            return false
        }
        chatSessionRepository.appendMessage(
            NewChatMessage(
                id = idGenerator("message"),
                sessionId = session.id,
                role = ChatRole.ASSISTANT,
                content = response,
                status = ChatMessageStatus.COMPLETED,
                modelConfigId = config.id,
                modelNameSnapshot = config.modelName,
                promptTokens = result.promptTokens,
                completionTokens = result.completionTokens,
                totalTokens = result.totalTokens,
                createdAt = currentTimeMillis(),
                suggestions = result.suggestions,
            ),
        )
        return true
    }

    private suspend fun handleFailure(
        session: ChatSession,
        config: ModelConfig,
        errorCode: String,
        errorMessage: String?,
    ) {
        appendFailedAssistant(session, config, errorCode, errorMessage)
        emitMessage(EffectMessage.CHAT_REQUEST_FAILED)
    }

    private suspend fun appendFailedAssistant(
        session: ChatSession,
        config: ModelConfig?,
        errorCode: String,
        errorMessage: String?,
    ) {
        chatSessionRepository.appendMessage(
            NewChatMessage(
                id = idGenerator("message"),
                sessionId = session.id,
                role = ChatRole.ASSISTANT,
                content = "",
                status = ChatMessageStatus.FAILED,
                errorCode = errorCode,
                errorMessage = errorMessage,
                modelConfigId = config?.id,
                modelNameSnapshot = config?.modelName,
                createdAt = currentTimeMillis(),
            ),
        )
    }

    private fun newUserMessage(
        sessionId: String,
        content: String,
        config: ModelConfig?,
        now: Long,
    ) = NewChatMessage(
        id = idGenerator("message"),
        sessionId = sessionId,
        role = ChatRole.USER,
        content = content,
        status = ChatMessageStatus.COMPLETED,
        modelConfigId = config?.id,
        modelNameSnapshot = config?.modelName,
        createdAt = now,
    )

    private fun handleHeaderAction(action: HeaderAction) {
        when (action) {
            HeaderAction.VOICE -> emitMessage(EffectMessage.VOICE_NOT_READY)
            HeaderAction.CONVERSATION_MANAGEMENT,
            HeaderAction.CHARACTER_SETTINGS,
            -> Unit
        }
    }

    private fun handleComposerAction(action: ComposerAction) {
        emitMessage(
            when (action) {
                ComposerAction.ATTACH -> EffectMessage.ATTACH_NOT_READY
                ComposerAction.EMOJI -> EffectMessage.EMOJI_NOT_READY
                ComposerAction.VOICE -> EffectMessage.MICROPHONE_NOT_READY
            },
        )
    }

    private fun emitMessage(message: EffectMessage) {
        _effects.trySend(ChatEffect.ShowMessage(message))
    }
}

private fun ModelConfig.isUsable(): Boolean {
    return enabled &&
        baseUrl.isNotBlank() &&
        apiKey.isNotBlank() &&
        modelName.isNotBlank()
}

private fun NewChatSession.toDomain(lastMessageAt: Long) = ChatSession(
    id = id,
    title = title,
    agentId = agentId,
    modelConfigId = modelConfigId,
    systemPromptSnapshot = systemPromptSnapshot,
    customSystemPrompt = null,
    maxContextMessageCount = maxContextMessageCount,
    enableSummary = enableSummary,
    summaryThresholdMessageCount = summaryThresholdMessageCount,
    summaryRetainedMessageCount = summaryRetainedMessageCount,
    lastMessageAt = lastMessageAt,
)

private fun emptyGreeting(
    character: Character?,
    now: Long,
    timeFormatter: (Long) -> String,
): List<ChatMessageUiModel> {
    val openingMessage = character?.openingMessage?.takeIf(String::isNotBlank)
        ?: return emptyList()
    return listOf(
        ChatMessageUiModel.Received(
            id = "empty-greeting:${character.id}",
            timestamp = timeFormatter(now),
            content = MessageContent.Custom(openingMessage),
            senderId = character.id,
        ),
    )
}

private fun List<StoredChatMessage>.toUiModels(
    characterId: String,
    timeFormatter: (Long) -> String,
): List<ChatMessageUiModel> {
    return filter { it.status == ChatMessageStatus.COMPLETED }.map { message ->
        when (message.role) {
            ChatRole.USER -> ChatMessageUiModel.Sent(
                id = message.id,
                timestamp = timeFormatter(message.createdAt),
                content = MessageContent.Custom(message.content),
                isRead = true,
            )
            ChatRole.ASSISTANT -> ChatMessageUiModel.Received(
                id = message.id,
                timestamp = timeFormatter(message.createdAt),
                content = MessageContent.Custom(message.content),
                senderId = characterId,
            )
        }
    }
}

private fun NewChatMessage.toStored(seq: Long) = StoredChatMessage(
    id = id,
    sessionId = sessionId,
    seq = seq,
    role = role,
    content = content,
    status = status,
    errorCode = errorCode,
    errorMessage = errorMessage,
    modelConfigId = modelConfigId,
    modelNameSnapshot = modelNameSnapshot,
    promptTokens = promptTokens,
    completionTokens = completionTokens,
    totalTokens = totalTokens,
    createdAt = createdAt,
)
