package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<ChatEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    private var sentMessageCount = 0

    fun onAction(action: ChatAction) {
        when (action) {
            is ChatAction.CharacterSelected -> {
                _uiState.update { it.copy(selectedCharacter = action.character) }
            }

            is ChatAction.MessageChanged -> {
                _uiState.update { it.copy(messageDraft = action.message) }
            }

            ChatAction.SendClicked -> sendMessage()

            is ChatAction.QuickReplyClicked -> {
                _uiState.update { it.copy(messageDraft = action.message) }
            }

            is ChatAction.NavigationSelected -> {
                _uiState.update {
                    it.copy(selectedDestination = action.destination)
                }
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
                _uiState.update { state ->
                    state.copy(
                        darkThemeOverride = when (state.darkThemeOverride) {
                            null -> true
                            true -> false
                            false -> null
                        },
                    )
                }
                emitMessage(EffectMessage.THEME_CHANGED)
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
