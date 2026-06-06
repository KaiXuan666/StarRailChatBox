package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val characterRepository: CharacterRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var sentMessageCount = 0

    init {
        viewModelScope.launch {
            val characters = runCatching { characterRepository.loadCharacters() }
                .getOrDefault(emptyList())
            _uiState.update { state ->
                state.copy(
                    characters = characters,
                    selectedCharacterId = state.selectedCharacterId
                        ?.takeIf { selectedId -> characters.any { it.id == selectedId } }
                        ?: characters.firstOrNull { it.id == "流萤" }?.id
                        ?: characters.firstOrNull()?.id,
                    isLoadingCharacters = false,
                )
            }
        }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.CharacterSelected -> {
                _uiState.update { state ->
                    if (state.characters.any { it.id == action.characterId }) {
                        state.copy(selectedCharacterId = action.characterId)
                    } else {
                        state
                    }
                }
            }

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

    private fun sendMessage() {
        val message = uiState.value.messageDraft.trim()
        if (message.isEmpty() || uiState.value.isSending) return

        sentMessageCount += 1
        _uiState.update { state ->
            state.copy(
                messages = state.messages + ChatMessageUiModel.Sent(
                    id = "sent-$sentMessageCount",
                    timestamp = "10:${25 + sentMessageCount}",
                    content = MessageContent.Custom(message),
                    isRead = true,
                ),
                messageDraft = "",
            )
        }
    }

    private fun handleHeaderAction(action: HeaderAction) {
        when (action) {
            HeaderAction.VOICE -> emitMessage(EffectMessage.VOICE_NOT_READY)
            HeaderAction.PROFILE -> emitMessage(EffectMessage.PROFILE_NOT_READY)
            HeaderAction.SETTINGS -> {
                // 点击设置按钮时，由 UI 收集并转派给 MainAction.NavigationSelected(Route.Settings)
            }
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
