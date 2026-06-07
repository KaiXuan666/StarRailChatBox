package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.api.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatMessageStatus
import com.kaixuan.starrailchatbox.data.chat.ChatRole
import com.kaixuan.starrailchatbox.data.chat.ChatSession
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.chat.NewChatMessage
import com.kaixuan.starrailchatbox.data.chat.NewChatSession
import com.kaixuan.starrailchatbox.data.chat.StoredChatMessage
import com.kaixuan.starrailchatbox.data.chat.buildChatContext
import com.kaixuan.starrailchatbox.data.chat.newChatId
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.platform.formatLocalTime
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
    private val openAiRepository: OpenAiRepository,
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
    private val chatCaches = mutableMapOf<String, CharacterChatCache>()

    init {
        viewModelScope.launch {
            val characters = runCatching { characterRepository.loadCharacters() }
                .getOrDefault(emptyList())
            val selectedId = characters.firstOrNull { it.name == "流萤" }?.id
                ?: characters.firstOrNull()?.id
            _uiState.update {
                it.copy(
                    characters = characters,
                    selectedCharacterId = selectedId,
                    isLoadingCharacters = false,
                )
            }
            selectedId?.let(::loadLatestSession)
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.CharacterSelected -> selectCharacter(action.characterId)
            is ChatAction.MessageChanged -> {
                _uiState.update { it.copy(messageDraft = action.message) }
            }
            ChatAction.SendClicked -> sendMessage()
            is ChatAction.QuickReplyClicked -> {
                _uiState.update { it.copy(messageDraft = action.message) }
            }
            is ChatAction.HeaderActionClicked -> handleHeaderAction(action.action)
            is ChatAction.ComposerActionClicked -> handleComposerAction(action.action)
        }
    }

    private fun selectCharacter(characterId: String) {
        val state = uiState.value
        if (state.isSending || state.selectedCharacterId == characterId) return
        if (state.characters.none { it.id == characterId }) return

        val currentId = state.selectedCharacterId
        if (currentId != null) {
            chatCaches[currentId] = CharacterChatCache(
                activeSessionId = state.activeSessionId,
                messages = state.messages,
                messageDraft = state.messageDraft,
                isLoadingSession = state.isLoadingSession
            )
        }

        val cache = chatCaches[characterId]
        if (cache != null) {
            _uiState.update {
                it.copy(
                    selectedCharacterId = characterId,
                    activeSessionId = cache.activeSessionId,
                    messages = cache.messages,
                    messageDraft = cache.messageDraft,
                    isLoadingSession = cache.isLoadingSession,
                )
            }
        } else {
            _uiState.update {
                it.copy(
                    selectedCharacterId = characterId,
                    activeSessionId = null,
                    messages = emptyList(),
                    messageDraft = "",
                    isLoadingSession = true,
                )
            }
        }
        loadLatestSession(characterId, hasCache = cache != null)
    }

    private fun loadLatestSession(characterId: String, hasCache: Boolean = false) {
        sessionJob?.cancel()
        sessionJob = viewModelScope.launch {
            if (!hasCache) {
                _uiState.update { it.copy(isLoadingSession = true) }
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
                _uiState.update {
                    it.copy(
                        activeSessionId = null,
                        messages = greeting,
                        isLoadingSession = false,
                    )
                }
                chatCaches[characterId] = CharacterChatCache(
                    activeSessionId = null,
                    messages = greeting,
                    messageDraft = uiState.value.messageDraft,
                    isLoadingSession = false
                )
                return@launch
            }
            _uiState.update {
                it.copy(
                    activeSessionId = session.id,
                    messages = if (hasCache) it.messages else emptyList(),
                    isLoadingSession = false,
                )
            }
            chatSessionRepository.observeMessages(session.id).collect { messages ->
                if (uiState.value.selectedCharacterId == characterId) {
                    val uiModels = messages.toUiModels(characterId, timeFormatter)
                    _uiState.update {
                        it.copy(messages = uiModels)
                    }
                    chatCaches[characterId] = CharacterChatCache(
                        activeSessionId = session.id,
                        messages = uiModels,
                        messageDraft = uiState.value.messageDraft,
                        isLoadingSession = false
                    )
                }
            }
        }
    }

    private fun sendMessage() {
        val state = uiState.value
        val content = state.messageDraft.trim()
        val character = state.selectedCharacter
        if (
            content.isEmpty() ||
            state.isSending ||
            state.isLoadingSession ||
            character == null
        ) {
            return
        }
        _uiState.update { it.copy(messageDraft = "", isSending = true) }
        viewModelScope.launch {
            try {
                sendMessage(character, content)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                emitMessage(EffectMessage.CHAT_REQUEST_FAILED)
            } finally {
                _uiState.update { it.copy(isSending = false) }
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
        var history = previousSession?.let {
            chatSessionRepository.findContextMessages(
                sessionId = it.id,
                maxHistoryMessageCount = null,
            )
        }.orEmpty()
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
            history = openingMessage?.let {
                listOf(it.toStored(seq = 1))
            }.orEmpty()
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
            history = history,
            currentUserMessage = content,
            maxHistoryMessageCount = session.maxContextMessageCount,
        )
        when (val result = openAiRepository.createChatCompletion(config, requestMessages)) {
            is ApiResult.Success -> handleSuccess(session, config, result.value)
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
            _uiState.update {
                it.copy(activeSessionId = session.id, isLoadingSession = false)
            }
            chatSessionRepository.observeMessages(session.id).collect { messages ->
                if (uiState.value.selectedCharacterId == characterId) {
                    val uiModels = messages.toUiModels(characterId, timeFormatter)
                    _uiState.update {
                        it.copy(messages = uiModels)
                    }
                    chatCaches[characterId] = CharacterChatCache(
                        activeSessionId = session.id,
                        messages = uiModels,
                        messageDraft = uiState.value.messageDraft,
                        isLoadingSession = false
                    )
                }
            }
        }
    }

    private suspend fun handleSuccess(
        session: ChatSession,
        config: ModelConfig,
        result: ChatCompletionResult,
    ) {
        val response = result.content.trim()
        if (response.isEmpty()) {
            appendFailedAssistant(
                session,
                config,
                errorCode = "empty_response",
                errorMessage = "Chat completion returned empty content.",
            )
            emitMessage(EffectMessage.CHAT_EMPTY_RESPONSE)
            return
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
            ),
        )
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
            HeaderAction.PROFILE -> emitMessage(EffectMessage.PROFILE_NOT_READY)
            HeaderAction.SETTINGS -> Unit
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

private data class CharacterChatCache(
    val activeSessionId: String?,
    val messages: List<ChatMessageUiModel>,
    val messageDraft: String,
    val isLoadingSession: Boolean
)
