package com.kaixuan.starrailchatbox.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.database.DatabaseManager
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath

class ProfileViewModel(
    private val profileStore: ProfileStore,
    private val databaseManager: DatabaseManager,
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
                scope().launch {
                    val finalUri = if (action.avatarUri != null && action.name != null) {
                        try {
                            val bytes = com.kaixuan.starrailchatbox.platform.KmpFileManager.Default
                                .readSourceBytes(action.avatarUri)
                            val extension = action.extension ?: action.avatarUri.substringAfterLast('.', "png")
                            val fileName = "temp_user_avatar_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.$extension"
                            val cachePath = (com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.cacheDir / fileName.toPath()).toString()
                            com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.writeBytes(cachePath, bytes)
                            cachePath
                        } catch (e: Exception) {
                            action.avatarUri
                        }
                    } else {
                        action.avatarUri
                    }
                    _uiState.update { it.copy(customAvatarUri = finalUri) }
                    // 规范：两阶段。这里仅更新 UI/Cache，正式保存由 saveProfile 触发
                }
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
            is ProfileAction.ExportData -> {
                scope().launch {
                    databaseManager.exportDatabase(action.directoryPath).onSuccess {
                        _effects.send(ProfileEffect.ShowMessage(ProfileEffectMessage.EXPORT_SUCCESS))
                    }
                }
            }
            is ProfileAction.ImportData -> {
                scope().launch {
                    databaseManager.importDatabase(action.filePath).onSuccess {
                        _effects.send(ProfileEffect.RestartApp)
                    }
                }
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
                // 如果是 Cache 目录的文件，正式移动到 Files 目录
                val finalAvatarUri = state.customAvatarUri?.let { uri ->
                    if (uri.startsWith(com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.cacheDir.toString())) {
                        val fileName = "user_avatar_${kotlin.time.Clock.System.now().toEpochMilliseconds()}.${uri.substringAfterLast('.')}"
                        val targetPath = com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.appDataDir / fileName.toPath()
                        com.kaixuan.starrailchatbox.platform.KmpFileManager.Default.move(uri.toPath(), targetPath)
                        targetPath.toString()
                    } else {
                        uri
                    }
                }

                val updatedProfile = UserProfile(
                    customAvatarUri = finalAvatarUri,
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
