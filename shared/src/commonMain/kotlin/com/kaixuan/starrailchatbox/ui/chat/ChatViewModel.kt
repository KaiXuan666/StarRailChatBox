package com.kaixuan.starrailchatbox.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
            is ChatAction.SettingsItemClicked -> handleSettingsItemClick(action.item)
            is ChatAction.ThemeDialogConfirm -> {
                _uiState.update { state ->
                    state.copy(
                        darkThemeOverride = action.themeOverride,
                        showThemeDialog = false
                    )
                }
                emitMessage(EffectMessage.THEME_CHANGED)
            }
            is ChatAction.ThemeDialogDismiss -> {
                _uiState.update { it.copy(showThemeDialog = false) }
            }

            is ChatAction.ApiHostChanged -> {
                _uiState.update { it.copy(apiHost = action.host) }
            }
            is ChatAction.ApiKeyChanged -> {
                _uiState.update { it.copy(apiKey = action.key) }
            }
            ChatAction.ToggleApiKeyVisibility -> {
                _uiState.update { it.copy(showApiKey = !it.showApiKey) }
            }
            ChatAction.FetchModelsClicked -> {
                fetchModels()
            }
            is ChatAction.SelectModel -> {
                _uiState.update { it.copy(selectedModel = action.model) }
            }
            ChatAction.SaveApiSettingsClicked -> {
                _uiState.update { it.copy(showApiSettings = false) }
                emitMessage(EffectMessage.SETTINGS_API_SAVED)
            }
            ChatAction.BackFromApiSettings -> {
                _uiState.update { it.copy(showApiSettings = false) }
            }
        }
    }

    private fun handleSettingsItemClick(item: SettingsItem) {
        when (item) {
            SettingsItem.API_SETTINGS -> {
                _uiState.update { it.copy(showApiSettings = true) }
            }
            SettingsItem.CHECK_UPDATE -> emitMessage(EffectMessage.SETTINGS_UPDATE_CHECK)
            SettingsItem.MESSAGE_NOTIFICATION -> emitMessage(EffectMessage.SETTINGS_NOTICE_NOT_READY)
            SettingsItem.THEME_STYLE -> {
                _uiState.update { it.copy(showThemeDialog = true) }
            }
            SettingsItem.ABOUT_US -> emitMessage(EffectMessage.SETTINGS_ABOUT_INFO)
            SettingsItem.PRIVACY_SECURITY -> emitMessage(EffectMessage.SETTINGS_PRIVACY_INFO)
        }
    }

    private fun fetchModels() {
        if (_uiState.value.isFetchingModels) return
        _uiState.update { it.copy(isFetchingModels = true) }
        emitMessage(EffectMessage.SETTINGS_API_FETCH_START)
        viewModelScope.launch {
            delay(1500)
            _uiState.update { state ->
                val newModels = listOf("gpt-4o-mini", "gpt-4.1", "deepseek-chat", "qwen-plus", "claude-3.5-sonnet")
                // Keep the current selection if it's in the list, otherwise select the first one
                val nextSelection = if (state.selectedModel in newModels) state.selectedModel else newModels.first()
                state.copy(
                    isFetchingModels = false,
                    modelsList = newModels,
                    selectedModel = nextSelection
                )
            }
            emitMessage(EffectMessage.SETTINGS_API_FETCH_SUCCESS)
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
