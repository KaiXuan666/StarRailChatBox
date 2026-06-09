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
    private val coroutineScope: CoroutineScope? = null,
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
                    customAvatarUri = profile?.customAvatarUri,
                    summaryThreshold = profile?.summaryThreshold ?: 20,
                    saveMultimodalToken = profile?.saveMultimodalToken ?: false,
                    enableWebSearch = profile?.enableWebSearch ?: false,
                    isLoaded = true
                )
            }
        }
    }

    fun onAction(action: ProfileAction) {
        when (action) {
            is ProfileAction.AvatarChanged -> {
                _uiState.update { it.copy(customAvatarUri = action.avatarUri) }
                saveProfile()
            }
            is ProfileAction.SummaryThresholdChanged -> {
                _uiState.update { it.copy(summaryThreshold = action.threshold) }
                saveProfile()
            }
            is ProfileAction.SaveMultimodalTokenChanged -> {
                _uiState.update { it.copy(saveMultimodalToken = action.enabled) }
                saveProfile()
            }
            is ProfileAction.EnableWebSearchChanged -> {
                _uiState.update { it.copy(enableWebSearch = action.enabled) }
                saveProfile()
            }
            ProfileAction.ExportDataClicked -> {
                // Placeholder for future implementation
            }
            ProfileAction.ImportDataClicked -> {
                // Placeholder for future implementation
            }
            ProfileAction.RestoreDefaultAvatar -> {
                _uiState.update { it.copy(customAvatarUri = null) }
                saveProfile()
            }
            ProfileAction.BackClicked -> {
                scope().launch { _effects.send(ProfileEffect.NavigateBack) }
            }
        }
    }

    private fun saveProfile() {
        val state = _uiState.value
        scope().launch {
            try {
                val updatedProfile = UserProfile(
                    customAvatarUri = state.customAvatarUri,
                    summaryThreshold = state.summaryThreshold,
                    saveMultimodalToken = state.saveMultimodalToken,
                    enableWebSearch = state.enableWebSearch
                )
                profileStore.save(updatedProfile)
                // If the store modified fields (like avatar path), update UI state to match
                val loaded = profileStore.load()
                if (loaded != null && loaded.customAvatarUri != state.customAvatarUri) {
                    _uiState.update { it.copy(customAvatarUri = loaded.customAvatarUri) }
                }
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }

    private fun scope(): CoroutineScope = coroutineScope ?: viewModelScope
}
