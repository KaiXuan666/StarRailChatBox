@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiContentPart
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.platform.persistAttachment
import com.kaixuan.starrailchatbox.platform.readUriAsBytes
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
import com.kaixuan.starrailchatbox.data.chat.ChatTitleCoordinator
import com.kaixuan.starrailchatbox.data.chat.MessageAttachment
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
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharacterEffect
import com.kaixuan.starrailchatbox.ui.character.CharacterEffectMessage
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.character.CharacterEditUiState
import com.kaixuan.starrailchatbox.ui.character.toEditUiState

class ChatViewModel(
    private val characterRepository: CharacterRepository,
    private val chatSessionRepository: ChatSessionRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val aiRepository: AiRepository,
    private val profileStore: ProfileStore,
    private val chatSummaryCoordinator: ChatSummaryCoordinator = ChatSummaryCoordinator(
        chatSessionRepository = chatSessionRepository,
        aiRepository = aiRepository,
    ),
    private val chatTitleCoordinator: ChatTitleCoordinator = ChatTitleCoordinator(
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
    private val enableFileAppend: Boolean = false,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _characterUiState = MutableStateFlow(CharactersUiState())
    val characterUiState = _characterUiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private val _characterEffects = Channel<CharacterEffect>(Channel.BUFFERED)
    val characterEffects = _characterEffects.receiveAsFlow()

    private var activeSession: ChatSession? = null
    private var sessionJob: Job? = null
    private var sessionListJob: Job? = null
    private var lastActiveMainCharacterId: String? = null

    init {
        viewModelScope.launch {
            profileStore.profile.collect { profile ->
                _uiState.update { it.copy(userAvatarUri = profile?.customAvatarUri) }
            }
        }
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
            val selectedChar = characters.firstOrNull { it.id == selectedId } ?: characters.firstOrNull()
            _characterUiState.update {
                it.copy(
                    characters = characters,
                    selectedCharacterId = selectedId,
                    isLoadingCharacters = false,
                )
            }
            _uiState.update {
                it.copy(
                    selectedCharacterId = selectedId,
                    selectedCharacter = selectedChar,
                    characterStates = initialStates,
                )
            }
            selectedId?.let {
                lastActiveMainCharacterId = it
                observeSessions(it)
                loadLatestSession(it)
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
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
                        } catch (e: Throwable) {
                            Napier.e("Send message (text-only) failed", e)
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
            is ChatAction.FileSelected -> {
                Napier.d("File selected: ${action.name} at ${action.uri}")
                val characterId = uiState.value.selectedCharacterId ?: return
                updateCharacterState(characterId) { state ->
                    state.copy(
                        selectedAttachments = state.selectedAttachments + SelectedAttachment.File(action.uri, action.name),
                        isAttachmentPanelVisible = false
                    )
                }
            }
            is ChatAction.ImageSelected -> {
                Napier.d("Image selected at ${action.uri}")
                val characterId = uiState.value.selectedCharacterId ?: return
                val name = action.name ?: action.uri.substringAfterLast('/')
                updateCharacterState(characterId) { state ->
                    state.copy(
                        selectedAttachments = state.selectedAttachments + SelectedAttachment.Image(action.uri, name),
                        isAttachmentPanelVisible = false
                    )
                }
            }
            is ChatAction.RemoveAttachment -> {
                val characterId = uiState.value.selectedCharacterId ?: return
                updateCharacterState(characterId) { state ->
                    state.copy(
                        selectedAttachments = state.selectedAttachments - action.attachment
                    )
                }
            }
            ChatAction.RestoreMainCharacter -> {
                lastActiveMainCharacterId?.let { selectCharacter(it) }
            }
            ChatAction.VoiceModeToggled -> {
                val characterId = uiState.value.selectedCharacterId ?: return
                updateCharacterState(characterId) { state ->
                    state.copy(isVoiceMode = !state.isVoiceMode)
                }
            }
            ChatAction.VoiceRecordingStarted -> {
                // 可选：在这里记录开始录音的状态
            }
            is ChatAction.VoiceRecordingFinished -> {
                sendVoiceMessage(action.uri, action.durationMs)
            }
            ChatAction.VoiceRecordingCancelled -> {
                // 处理录音取消
            }
            is ChatAction.OpenAttachment -> {
                // 已经在 UI 层通过 LocalUriHandler 处理了，ViewModel 暂时不需要处理
            }
            is ChatAction.RetrySendMessage -> retrySendMessage(action.messageId)
        }
    }

    private fun retrySendMessage(messageId: String) {
        val state = uiState.value
        val characterId = state.selectedCharacterId ?: return
        val characterState = state.characterStates[characterId] ?: return
        val character = state.selectedCharacter ?: return
        if (characterState.isSending) return

        val messageToRetry = characterState.messages.find { it.id == messageId } as? ChatMessageUiModel.Sent ?: return

        viewModelScope.launch {
            val sessionId = characterState.activeSessionId ?: return@launch
            chatSessionRepository.deleteFailedMessages(sessionId)

            val session = chatSessionRepository.findSession(sessionId) ?: return@launch

            updateCharacterState(characterId) { it.copy(isSending = true) }
            try {
                performChatRequest(character, session)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Throwable) {
                Napier.e("Retry message failed (msgId: ${messageToRetry.id})", e)
                emitMessage(EffectMessage.CHAT_REQUEST_FAILED)
            } finally {
                updateCharacterState(characterId) { it.copy(isSending = false) }
            }
        }
    }

    private fun sendVoiceMessage(uri: String, durationMs: Long) {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterId = character.id

        _uiState.update { s ->
            val curState = s.characterStates[characterId] ?: CharacterChatState()
            s.copy(
                characterStates = s.characterStates + (characterId to curState.copy(
                    isSending = true
                ))
            )
        }

        viewModelScope.launch {
            try {
                val fileName = uri.substringAfterLast('/')
                val attachment = SelectedAttachment.Voice(uri, fileName, durationMs)
                Napier.d { "sendVoiceMessage: uri=$uri, fileName=$fileName" }
                sendMessage(character, "", listOf(attachment))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Throwable) {
                Napier.e("Send voice message failed", e)
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

    fun onCharacterAction(action: CharacterAction) {
        when (action) {
            is CharacterAction.CharacterSelected -> selectCharacter(action.characterId)
            is CharacterAction.CharacterEditOpened -> openCharacterEdit(action.characterId)
            is CharacterAction.CharacterNameChanged -> updateCharacterEdit { it.copy(name = action.name) }
            is CharacterAction.CharacterPromptChanged -> updateCharacterEdit { it.copy(prompt = action.prompt) }
            is CharacterAction.CharacterOpeningMessageChanged -> updateCharacterEdit {
                it.copy(openingMessage = action.openingMessage)
            }
            is CharacterAction.CharacterAvatarChanged -> updateCharacterEdit {
                it.copy(
                    avatarUri = action.avatarSource.uri,
                    pendingAvatarSource = action.avatarSource,
                )
            }
            is CharacterAction.CharacterTemperatureChanged -> updateCharacterEdit {
                it.copy(temperature = action.temperature.coerceIn(0.0, 2.0))
            }
            is CharacterAction.CharacterTopPChanged -> updateCharacterEdit {
                it.copy(topP = action.topP.coerceIn(0.0, 1.0))
            }
            CharacterAction.CharacterSaveClicked -> saveCharacterEdit()
            CharacterAction.CharacterDeleteBuiltinClicked -> emitCharacterMessage(CharacterEffectMessage.CHARACTER_DELETE_BUILTIN_RESTRICTED)
            is CharacterAction.CharacterDeleteClicked -> deleteCharacter(action.characterId)
            is CharacterAction.CharacterPromptGenClicked -> {
                val name = characterUiState.value.characterEdit.name.trim()
                if (name.isEmpty()) {
                    emitCharacterMessage(CharacterEffectMessage.CHARACTER_NAME_REQUIRED)
                } else {
                    updateCharacterEdit {
                        it.copy(
                            isPromptGenDialogOpen = true,
                            promptGenInputText = action.defaultPromptRequestText
                        )
                    }
                }
            }
            is CharacterAction.CharacterPromptGenInputChanged -> {
                updateCharacterEdit {
                    it.copy(promptGenInputText = action.text)
                }
            }
            CharacterAction.CharacterPromptGenCancelClicked -> {
                updateCharacterEdit {
                    it.copy(isPromptGenDialogOpen = false)
                }
            }
            CharacterAction.CharacterPromptGenConfirmClicked -> {
                val inputPrompt = characterUiState.value.characterEdit.promptGenInputText
                updateCharacterEdit {
                    it.copy(
                        isPromptGenDialogOpen = false,
                        isGeneratingPrompt = true
                    )
                }
                viewModelScope.launch {
                    try {
                        val config = modelConfigRepository.getDefault()
                            ?.takeIf(ModelConfig::isUsable)
                        if (config == null) {
                            emitCharacterMessage(CharacterEffectMessage.MODEL_CONFIG_REQUIRED)
                            updateCharacterEdit { it.copy(isGeneratingPrompt = false) }
                            return@launch
                        }

                        val requestMessages = listOf(
                            AiMessage(
                                role = ChatRole.USER.apiValue,
                                content = inputPrompt
                            )
                        )

                        var generatedPrompt = ""
                        var hasError = false
                        aiRepository.createPromptCompletion(config, requestMessages).collect { result ->
                            when (result) {
                                is ApiResult.Success -> {
                                    generatedPrompt = result.value.content
                                    if (generatedPrompt.isNotBlank()) {
                                        updateCharacterEdit {
                                            it.copy(prompt = generatedPrompt)
                                        }
                                    }
                                }
                                else -> {
                                    hasError = true
                                }
                            }
                        }

                        val finalPrompt = generatedPrompt.trim()
                        if (hasError || finalPrompt.isEmpty()) {
                            emitCharacterMessage(CharacterEffectMessage.PROMPT_GEN_FAILED)
                            updateCharacterEdit { it.copy(isGeneratingPrompt = false) }
                        } else {
                            updateCharacterEdit {
                                it.copy(
                                    prompt = finalPrompt,
                                    isGeneratingPrompt = false
                                )
                            }
                        }
                    } catch (cancellation: CancellationException) {
                        throw cancellation
                    } catch (_: Throwable) {
                        emitCharacterMessage(CharacterEffectMessage.PROMPT_GEN_FAILED)
                        updateCharacterEdit { it.copy(isGeneratingPrompt = false) }
                    }
                }
            }
            is CharacterAction.CharactersReordered -> reorderCharacters(action.orderedCharacters)
        }
    }

    private fun reorderCharacters(orderedCharacters: List<Character>) {
        val updatedCharacters = orderedCharacters.mapIndexed { index, character ->
            character.copy(sortOrder = index)
        }
        _characterUiState.update { state ->
            state.copy(characters = updatedCharacters)
        }
        val selectedId = _characterUiState.value.selectedCharacterId
        val selectedChar = updatedCharacters.firstOrNull { it.id == selectedId } ?: updatedCharacters.firstOrNull()
        _uiState.update { state ->
            state.copy(selectedCharacter = selectedChar)
        }
        viewModelScope.launch {
            runCatching {
                updatedCharacters.forEach { character ->
                    characterRepository.updateSortOrder(character.id, character.sortOrder)
                }
            }.onFailure {
                Napier.e("Failed to save reordered characters", it)
            }
        }
    }

    private fun openCharacterEdit(characterId: String?) {
        if (characterId == null) {
            _characterUiState.update {
                it.copy(
                    characterEdit = CharacterEditUiState(
                        characterId = null,
                        name = "",
                        prompt = "",
                        openingMessage = "",
                        avatarUri = "",
                        pendingAvatarSource = null,
                        temperature = 0.85,
                        topP = 0.9,
                    )
                )
            }
        } else {
            viewModelScope.launch {
                val fullCharacter = runCatching {
                    characterRepository.getCharacter(characterId)
                }.getOrNull() ?: return@launch
                _characterUiState.update {
                    it.copy(characterEdit = fullCharacter.toEditUiState())
                }
            }
        }
    }

    private fun updateCharacterEdit(transform: (CharacterEditUiState) -> CharacterEditUiState) {
        _characterUiState.update { state ->
            state.copy(characterEdit = transform(state.characterEdit))
        }
    }

    private fun saveCharacterEdit() {
        val editState = characterUiState.value.characterEdit
        val characterId = editState.characterId
        if (editState.name.isBlank()) {
            emitCharacterMessage(CharacterEffectMessage.CHARACTER_NAME_EMPTY)
            return
        }
        updateCharacterEdit { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                if (characterId == null) {
                    val newId = "user_${currentTimeMillis()}"
                    val newCharacter = Character(
                        id = newId,
                        name = editState.name.trim(),
                        prompt = editState.prompt,
                        openingMessage = editState.openingMessage,
                        avatarUri = editState.avatarUri,
                        temperature = editState.temperature.coerceIn(0.0, 2.0),
                        topP = editState.topP.coerceIn(0.0, 1.0),
                        voiceSampleUri = editState.voiceSampleUri,
                        createdAt = currentTimeMillis(),
                    )
                    characterRepository.updateCharacter(newCharacter, editState.pendingAvatarSource)
                } else {
                    val original = characterUiState.value.characters.firstOrNull { it.id == characterId }
                        ?: throw IllegalStateException("Character not found")
                    characterRepository.updateCharacter(
                        original.copy(
                            name = editState.name.trim(),
                            prompt = editState.prompt,
                            openingMessage = editState.openingMessage,
                            avatarUri = editState.avatarUri,
                            temperature = editState.temperature.coerceIn(0.0, 2.0),
                            topP = editState.topP.coerceIn(0.0, 1.0),
                            voiceSampleUri = editState.voiceSampleUri,
                        ),
                        editState.pendingAvatarSource,
                    )
                }
            }.onSuccess { saved ->
                val characters = runCatching { characterRepository.loadCharacters() }
                    .getOrElse {
                        if (characterId == null) {
                            characterUiState.value.characters + saved
                        } else {
                            characterUiState.value.characters.map { if (it.id == saved.id) saved else it }
                        }
                    }
                _characterUiState.update { state ->
                    state.copy(
                        characters = characters,
                        selectedCharacterId = saved.id,
                        characterEdit = saved.toEditUiState(),
                    )
                }
                _uiState.update { state ->
                    val currentCharacterState = state.characterStates[saved.id]
                    val updatedCharacterStates = if (currentCharacterState?.activeSessionId == null) {
                        state.characterStates + (
                            saved.id to (currentCharacterState ?: CharacterChatState()).copy(
                                messages = emptyGreeting(
                                    character = saved,
                                    now = currentTimeMillis(),
                                    timeFormatter = timeFormatter,
                                ),
                            )
                        )
                    } else {
                        state.characterStates
                    }
                    state.copy(
                        selectedCharacterId = saved.id,
                        selectedCharacter = saved,
                        characterStates = updatedCharacterStates,
                    )
                }
                _characterEffects.send(CharacterEffect.CharacterSaved)
            }.onFailure {
                updateCharacterEdit { it.copy(isSaving = false) }
                emitCharacterMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED)
            }
        }
    }

    private fun deleteCharacter(characterId: String) {
        viewModelScope.launch {
            runCatching {
                characterRepository.deleteCharacter(characterId, currentTimeMillis())
                val remaining = characterRepository.loadCharacters()
                    .sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
                remaining.forEachIndexed { index, character ->
                    characterRepository.updateSortOrder(character.id, index)
                }
                characterRepository.loadCharacters()
            }.onSuccess { characters ->
                val fallbackId = characters.firstOrNull()?.id
                val fallbackChar = characters.firstOrNull()
                _characterUiState.update { state ->
                    state.copy(
                        characters = characters,
                        selectedCharacterId = if (state.selectedCharacterId == characterId) {
                            fallbackId
                        } else {
                            state.selectedCharacterId?.takeIf { id -> characters.any { it.id == id } } ?: fallbackId
                        },
                    )
                }
                _uiState.update { state ->
                    val updatedSelectedId = if (state.selectedCharacterId == characterId) {
                        fallbackId
                    } else {
                        state.selectedCharacterId?.takeIf { id -> characters.any { it.id == id } } ?: fallbackId
                    }
                    if (lastActiveMainCharacterId == characterId) {
                        lastActiveMainCharacterId = updatedSelectedId
                    }
                    val updatedSelectedChar = characters.firstOrNull { it.id == updatedSelectedId } ?: fallbackChar
                    state.copy(
                        selectedCharacterId = updatedSelectedId,
                        selectedCharacter = updatedSelectedChar,
                        characterStates = state.characterStates - characterId,
                    )
                }
                fallbackId?.let {
                    observeSessions(it)
                    loadLatestSession(it)
                }
                _characterEffects.send(CharacterEffect.CharacterDeleted)
            }.onFailure {
                emitCharacterMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED)
            }
        }
    }

    private fun emitCharacterMessage(message: CharacterEffectMessage) {
        _characterEffects.trySend(CharacterEffect.ShowMessage(message))
    }

    private fun selectCharacter(characterId: String) {
        val charState = characterUiState.value
        val characters = charState.characters
        if (characters.none { it.id == characterId }) return
        if (charState.selectedCharacterId == characterId) return

        val sorted = characters.sortedWith(compareBy({ it.sortOrder }, { it.createdAt }))
        val isTopFour = sorted.take(4).any { it.id == characterId }
        if (isTopFour) {
            lastActiveMainCharacterId = characterId
        }

        val selectedChar = characters.firstOrNull { it.id == characterId }
        _characterUiState.update {
            it.copy(selectedCharacterId = characterId)
        }
        _uiState.update {
            it.copy(
                selectedCharacterId = characterId,
                selectedCharacter = selectedChar,
            )
        }
        observeSessions(characterId)
        val chatState = uiState.value
        val hasCache = chatState.characterStates[characterId]?.messages?.isNotEmpty() == true
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
                val uiModels = messages.toUiModels(
                    characterId = characterId,
                    timeFormatter = timeFormatter,
                    isSending = uiState.value.characterStates[characterId]?.isSending ?: false
                )
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

    private fun updateMessageDraft(message: String) {
        val characterId = uiState.value.selectedCharacterId ?: return
        updateCharacterState(characterId) {
            it.copy(messageDraft = message)
        }
    }

    private fun sendMessage() {
        val state = uiState.value
        val character = state.selectedCharacter ?: return
        val characterId = character.id
        val characterState = state.characterStates[characterId] ?: CharacterChatState()
        val content = characterState.messageDraft.trim()
        val attachments = characterState.selectedAttachments
        if (
            (content.isEmpty() && attachments.isEmpty()) ||
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
                    selectedAttachments = emptyList(),
                    isSending = true
                ))
            )
        }
        viewModelScope.launch {
            try {
                sendMessage(character, content, attachments)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Throwable) {
                e.printStackTrace()
                Napier.e("Send message with attachments failed", e)
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
        attachments: List<SelectedAttachment> = emptyList(),
    ) {
        val previousSession = activeSession
        val now = currentTimeMillis()

        val fullCharacter = characterRepository.getCharacter(character.id) ?: character
        val session = previousSession ?: NewChatSession(
            id = idGenerator("session"),
            title = sessionTitleProvider(),
            agentId = character.id,
            modelConfigId = null, // Will be updated if config is known
            systemPromptSnapshot = fullCharacter.prompt,
            maxContextMessageCount = null,
            createdAt = now,
        ).let { newSession ->
            val userMessageId = idGenerator("message")
            val domainAttachments = attachments.map {
                val persistedUri = persistAttachment(it.uri, it.name)
                val updatedAttachment = when (it) {
                    is SelectedAttachment.File -> it.copy(uri = persistedUri)
                    is SelectedAttachment.Image -> it.copy(uri = persistedUri)
                    is SelectedAttachment.Voice -> it.copy(uri = persistedUri)
                }
                Napier.d { "sendMessage: updatedAttachment=${updatedAttachment.uri}." }
                updatedAttachment.toMessageAttachment(userMessageId, now)
            }
            val userMessage = NewChatMessage(
                id = userMessageId,
                sessionId = newSession.id,
                role = ChatRole.USER,
                content = content,
                status = ChatMessageStatus.COMPLETED,
                modelConfigId = null,
                modelNameSnapshot = null,
                createdAt = now,
                attachments = domainAttachments,
            )
            val openingMessage = fullCharacter.openingMessage
                .takeIf(String::isNotBlank)
                ?.let {
                    NewChatMessage(
                        id = idGenerator("message"),
                        sessionId = newSession.id,
                        role = ChatRole.ASSISTANT,
                        content = it,
                        status = ChatMessageStatus.COMPLETED,
                        modelConfigId = null,
                        modelNameSnapshot = null,
                        createdAt = now,
                    )
                }
            val initialMessages = listOfNotNull(openingMessage, userMessage)
            chatSessionRepository.createSessionWithMessages(newSession, initialMessages)
            newSession.toDomain(lastMessageAt = now).also {
                activeSession = it
                observeCreatedSession(it, character.id)
            }
        }

        if (previousSession != null) {
            val userMessageId = idGenerator("message")
            val domainAttachments = attachments.map {
                val persistedUri = persistAttachment(it.uri, it.name)
                val updatedAttachment = when (it) {
                    is SelectedAttachment.File -> it.copy(uri = persistedUri)
                    is SelectedAttachment.Image -> it.copy(uri = persistedUri)
                    is SelectedAttachment.Voice -> it.copy(uri = persistedUri)
                }
                Napier.d { "sendMessage: updatedAttachment=${updatedAttachment.uri}." }
                updatedAttachment.toMessageAttachment(userMessageId, now)
            }
            chatSessionRepository.appendMessage(
                NewChatMessage(
                    id = userMessageId,
                    sessionId = session.id,
                    role = ChatRole.USER,
                    content = content,
                    status = ChatMessageStatus.COMPLETED,
                    modelConfigId = null,
                    modelNameSnapshot = null,
                    createdAt = now,
                    attachments = domainAttachments,
                ),
            )
        }

        performChatRequest(character, session)
    }

    private suspend fun performChatRequest(
        character: Character,
        session: ChatSession,
    ) {
        val context = chatSessionRepository.findContext(
            sessionId = session.id,
            maxHistoryMessageCount = null,
        )

        val lastUserMessage = context.messages.lastOrNull { it.role == ChatRole.USER } ?: return
        val originalHistory = context.messages.filter { it.seq < lastUserMessage.seq }
        val saveMultimodalToken = profileStore.load()?.saveMultimodalToken ?: false

        val history = if (saveMultimodalToken) {
            originalHistory.map { msg ->
                if (msg.attachments.isNotEmpty()) {
                    val attachmentText = msg.attachments.joinToString(" ") { "[${it.name}]" }
                    msg.copy(content = if (msg.content.isBlank()) attachmentText else "${msg.content}\n$attachmentText")
                } else {
                    msg
                }
            }
        } else {
            originalHistory
        }

        Napier.d { "performChatRequest: lastUserMessage.seq=${lastUserMessage.seq}, saveMultimodalToken=$saveMultimodalToken" }
        Napier.d { "performChatRequest: summary=${context.summary?.content ?: "None"}" }
        Napier.d { "performChatRequest: history messages (count=${history.size}):" }
        history.forEach { msg ->
            Napier.d { "  - seq=${msg.seq}, role=${msg.role}, content=${msg.content.take(100)}" }
        }

        val hasCurrentMultimodalAttachment = lastUserMessage.attachments.any {
            it.mimeType.startsWith("image/") || it.mimeType.startsWith("audio/") || !enableFileAppend
        }
        val hasHistoryMultimodalAttachment = !saveMultimodalToken && history.any { msg ->
            msg.attachments.any { att ->
                val isAiVoice = msg.role == ChatRole.ASSISTANT && att.mimeType.startsWith("audio/")
                !isAiVoice && (att.mimeType.startsWith("image/") || att.mimeType.startsWith("audio/") || !enableFileAppend)
            }
        }
        val hasMultimodalAttachment = hasCurrentMultimodalAttachment || hasHistoryMultimodalAttachment

        val config = if (hasMultimodalAttachment) {
            (modelConfigRepository.getMultimodal()?.takeIf(ModelConfig::isUsable)
                ?: modelConfigRepository.getDefault()?.takeIf(ModelConfig::isUsable))
        } else {
            modelConfigRepository.getDefault()?.takeIf(ModelConfig::isUsable)
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

        val userText = lastUserMessage.content
        val contentParts = mutableListOf<AiContentPart>()

        lastUserMessage.attachments.forEach { attachment ->
            if (enableFileAppend && (attachment.mimeType == "text/plain" || attachment.mimeType == "application/json")) {
                // 文件内容已在发送时被拼接到 content 中，此处无需重复处理
            } else {
                Napier.d { "lastUserMessage.attachments= attachment=$attachment" }
                val part = readAttachmentAsAiContentPart(attachment)
                Napier.d { "lastUserMessage.attachments= part=$part" }
                if (part != null) {
                    contentParts.add(part)
                }
            }
        }

        val messageParts = if (contentParts.isNotEmpty()) {
            buildList<AiContentPart> {
                if (userText.isNotBlank()) {
                    add(AiContentPart.Text(userText))
                }
                addAll(contentParts)
            }
        } else {
            null
        }

        val prompt = session.customSystemPrompt
            ?.takeIf(String::isNotBlank)
            ?: session.systemPromptSnapshot

        Napier.d { "performChatRequest messageParts=$messageParts" }
        val historyMessageParts = if (saveMultimodalToken) {
            emptyMap()
        } else {
            history.associate { msg ->
                msg.id to msg.attachments.mapNotNull { attachment ->
                    if (msg.role == ChatRole.ASSISTANT && attachment.mimeType.startsWith("audio/")) {
                        return@mapNotNull null
                    }
                    readAttachmentAsAiContentPart(attachment)
                }
            }.filterValues { it.isNotEmpty() }
        }

        val requestMessages = buildChatContext(
            systemPrompt = prompt,
            summary = context.summary,
            history = history,
            currentUserMessage = userText,
            maxHistoryMessageCount = session.maxContextMessageCount,
            currentUserMessageParts = messageParts,
            historyMessageParts = historyMessageParts,
        )

        when (val result = aiRepository.createChatCompletion(config, requestMessages, character.name, character.voiceSampleUri)) {
            is ApiResult.Success -> {
                if (handleSuccess(session, config, result.value)) {
                    viewModelScope.launch {
                        try {
                            chatSummaryCoordinator.summarizeIfNeeded(session, config)
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                        }
                    }
                    viewModelScope.launch {
                        try {
                            chatTitleCoordinator.renameSessionIfNeeded(
                                session = session,
                                config = config,
                                defaultTitle = sessionTitleProvider(),
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (_: Throwable) {
                        }
                    }
                }
            }
            is ApiResult.HttpError -> handleFailure(session, config, "http_${result.statusCode}", result.message)
            is ApiResult.NetworkError -> handleFailure(session, config, "network_error", result.message)
            is ApiResult.UnexpectedError -> handleFailure(session, config, "unexpected_error", result.message)
        }
    }

    private suspend fun readAttachmentAsAiContentPart(attachment: MessageAttachment): AiContentPart? {
        Napier.d { "readAttachmentAsAiContentPart attachment=$attachment" }
        if (attachment.uri.startsWith("data:")) {
            return if (attachment.mimeType.startsWith("image/")) {
                AiContentPart.ImageUrl(attachment.uri)
            } else {
                AiContentPart.FileUrl(attachment.uri, attachment.mimeType)
            }
        } else {
            val bytes = runCatching { readUriAsBytes(attachment.uri) }.getOrNull()
            Napier.d { "readAttachmentAsAiContentPart bytes=$bytes" }
            if (bytes != null && bytes.isNotEmpty()) {
                val base64 = "data:${attachment.mimeType};base64,${kotlin.io.encoding.Base64.encode(bytes)}"
                return if (attachment.mimeType.startsWith("image/")) {
                    AiContentPart.ImageUrl(base64)
                } else {
                    AiContentPart.FileUrl(base64, attachment.mimeType)
                }
            }
        }
        return null
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
            updateCharacterState(characterId) { state ->
                state.copy(
                    messages = messages.toUiModels(characterId, timeFormatter, state.isSending),
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
        val messageId = idGenerator("message")
        val now = currentTimeMillis()
        val attachments = if (!result.voiceAttachmentUri.isNullOrEmpty()) {
            listOf(
                MessageAttachment(
                    id = idGenerator("attachment"),
                    messageId = messageId,
                    name = "voice.wav",
                    size = 0,
                    mimeType = "audio/wav",
                    uri = result.voiceAttachmentUri,
                    createdAt = now,
                    durationMs = result.voiceDurationMs,
                )
            )
        } else {
            emptyList()
        }
        chatSessionRepository.appendMessage(
            NewChatMessage(
                id = messageId,
                sessionId = session.id,
                role = ChatRole.ASSISTANT,
                content = response,
                status = ChatMessageStatus.COMPLETED,
                modelConfigId = config.id,
                modelNameSnapshot = config.modelName,
                promptTokens = result.promptTokens,
                completionTokens = result.completionTokens,
                totalTokens = result.totalTokens,
                createdAt = now,
                suggestions = result.suggestions,
                attachments = attachments,
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
        Napier.e("CHAT API request failed: configModel=${config.modelName}, errorCode=$errorCode, errorMessage=$errorMessage")
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

    private fun SelectedAttachment.toMessageAttachment(messageId: String, now: Long): MessageAttachment {
        val ext = name.substringAfterLast('.', "").lowercase()
        val mimeType = when (this) {
            is SelectedAttachment.Image -> when (ext) {
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            is SelectedAttachment.File -> when (ext) {
                "txt", "kt", "java", "py", "js", "ts" -> "text/plain"
                "json" -> "application/json"
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "xls" -> "application/vnd.ms-excel"
                "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                else -> "application/octet-stream"
            }
            is SelectedAttachment.Voice -> "audio/m4a"
        }
        return MessageAttachment(
            id = idGenerator("attachment"),
            messageId = messageId,
            name = name,
            size = 0,
            mimeType = mimeType,
            uri = uri,
            createdAt = now,
            durationMs = if (this is SelectedAttachment.Voice) durationMs else null,
        )
    }

    private fun handleHeaderAction(action: HeaderAction) {
        when (action) {
            HeaderAction.VOICE -> emitMessage(EffectMessage.VOICE_NOT_READY)
            HeaderAction.CONVERSATION_MANAGEMENT,
            HeaderAction.CHARACTER_EDIT,
            -> Unit
        }
    }

    private fun handleComposerAction(action: ComposerAction) {
        when (action) {
            ComposerAction.ATTACH -> {
                val selectedId = uiState.value.selectedCharacterId ?: return
                updateCharacterState(selectedId) {
                    it.copy(isAttachmentPanelVisible = !it.isAttachmentPanelVisible)
                }
            }
            ComposerAction.EMOJI -> emitMessage(EffectMessage.EMOJI_NOT_READY)
            ComposerAction.VOICE -> onAction(ChatAction.VoiceModeToggled)
            ComposerAction.PICK_FILE,
            ComposerAction.TAKE_PHOTO,
            ComposerAction.PICK_IMAGE -> {
                // 暂时只显示对应的提示或空操作
                emitMessage(EffectMessage.ATTACH_NOT_READY)
                // 执行后关闭面板
                val selectedId = uiState.value.selectedCharacterId ?: return
                updateCharacterState(selectedId) {
                    it.copy(isAttachmentPanelVisible = false)
                }
            }
        }
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
            createdAt = now,
            content = MessageContent.Custom(openingMessage),
            senderId = character.id,
        ),
    )
}

private fun List<StoredChatMessage>.toUiModels(
    characterId: String,
    timeFormatter: (Long) -> String,
    isSending: Boolean = false,
): List<ChatMessageUiModel> {
    val results = mutableListOf<ChatMessageUiModel>()

    this.forEach { message ->
        if (message.status == ChatMessageStatus.FAILED && message.role == ChatRole.ASSISTANT) {
            // If this is a failed assistant message, mark the previous user message as failed
            val last = results.lastOrNull()
            if (last is ChatMessageUiModel.Sent) {
                results[results.size - 1] = last.copy(status = MessageStatus.FAILED)
            }
            // We don't add the failed assistant message itself to UI
            return@forEach
        }

        when (message.role) {
            ChatRole.USER -> results.add(
                ChatMessageUiModel.Sent(
                    id = message.id,
                    timestamp = timeFormatter(message.createdAt),
                    createdAt = message.createdAt,
                    content = MessageContent.Custom(message.content),
                    isRead = true,
                    status = MessageStatus.SENT,
                    attachments = message.attachments,
                )
            )
            ChatRole.ASSISTANT -> results.add(
                ChatMessageUiModel.Received(
                    id = message.id,
                    timestamp = timeFormatter(message.createdAt),
                    createdAt = message.createdAt,
                    content = MessageContent.Custom(message.content),
                    senderId = characterId,
                    attachments = message.attachments,
                )
            )
        }
    }

    if (isSending && results.isNotEmpty() && results.last() is ChatMessageUiModel.Sent) {
        val lastSent = results.last() as ChatMessageUiModel.Sent
        results[results.size - 1] = lastSent.copy(status = MessageStatus.SENDING)
    }

    return results
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
