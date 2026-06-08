package com.kaixuan.starrailchatbox.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val profileStore: ProfileStore,
    private val coroutineScope: CoroutineScope? = null
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<ProfileEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        scope().launch {
            val profile = profileStore.load()
            _uiState.update { state ->
                state.copy(
                    nickname = profile?.nickname ?: "星空旅人",
                    customAvatarUri = profile?.customAvatarUri,
                    isLoaded = true
                )
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.NicknameChanged -> {
                _uiState.update { it.copy(nickname = action.name) }
            }
            is ProfileAction.AvatarChanged -> {
                _uiState.update { it.copy(customAvatarUri = action.avatarUri) }
            }
            ProfileAction.RestoreDefaultAvatar -> {
                _uiState.update { it.copy(customAvatarUri = null) }
            }
            ProfileAction.SaveClicked -> {
                saveProfile()
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        if (state.nickname.isBlank()) {
            emitMessage(ProfileEffectMessage.NICKNAME_EMPTY)
            return
        }

        _uiState.update { it.copy(isSaving = true) }
        scope().launch {
            try {
                profileStore.save(
                    UserProfile(
                        nickname = state.nickname.trim(),
                        customAvatarUri = state.customAvatarUri
                    )
                )
                _uiState.update { it.copy(isSaving = false) }
                _effects.send(ProfileEffect.ProfileSaved)
            } catch (t: Throwable) {
                t.printStackTrace()
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun scope(): CoroutineScope = coroutineScope ?: viewModelScope

    private fun emitMessage(message: ProfileEffectMessage) {
        _effects.trySend(ProfileEffect.ShowMessage(message))
    }
}
