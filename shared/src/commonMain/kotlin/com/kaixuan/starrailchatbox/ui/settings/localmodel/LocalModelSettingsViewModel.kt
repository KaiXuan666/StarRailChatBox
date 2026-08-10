package com.kaixuan.starrailchatbox.ui.settings.localmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.localmodel.ChatModelMode
import com.kaixuan.starrailchatbox.data.localmodel.LocalLanguageModelRuntime
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelCatalog
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelDownloadEvent
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelDownloadService
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelInstallState
import com.kaixuan.starrailchatbox.data.localmodel.LocalModelRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocalModelSettingsViewModel(
    private val repository: LocalModelRepository,
    private val downloads: LocalModelDownloadService,
    private val settings: AppSettingsStore,
    private val runtime: LocalLanguageModelRuntime,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        LocalModelSettingsUiState(isRuntimeSupported = runtime.isSupported),
    )
    val uiState = _uiState.asStateFlow()
    private val _effects = Channel<LocalModelSettingsEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()
    private var downloadJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                repository.observeModels(),
                settings.chatModelMode,
                settings.selectedLocalModelId,
            ) { models, mode, selected -> Triple(models, mode, selected) }
                .collect { (models, mode, selected) ->
                    _uiState.update { it.copy(installedModels = models, mode = mode, selectedModelId = selected) }
                }
        }
        viewModelScope.launch {
            runtime.status.collect { status ->
                _uiState.update { it.copy(runtimeStatus = status) }
            }
        }
    }

    fun onAction(action: LocalModelSettingsAction) {
        when (action) {
            is LocalModelSettingsAction.ModeSelected -> viewModelScope.launch {
                if (action.mode == ChatModelMode.LOCAL && _uiState.value.selectedModelId == null) {
                    show("local_model_select_required")
                } else {
                    settings.setChatModelMode(action.mode)
                }
            }
            LocalModelSettingsAction.DownloadCatalogModel -> startDownload()
            LocalModelSettingsAction.CancelDownload -> downloadJob?.cancel()
            is LocalModelSettingsAction.ImportSelected -> stageImport(action)
            LocalModelSettingsAction.ConfirmImport -> installImport()
            LocalModelSettingsAction.DismissImport -> {
                val staged = _uiState.value.stagedImport
                _uiState.update { it.copy(stagedImport = null) }
                if (staged != null) viewModelScope.launch { downloads.discardImport(staged) }
            }
            is LocalModelSettingsAction.SelectModel -> viewModelScope.launch {
                settings.setSelectedLocalModelId(action.id)
                settings.setChatModelMode(ChatModelMode.LOCAL)
            }
            is LocalModelSettingsAction.RequestDelete -> _uiState.update { state ->
                state.copy(pendingDelete = state.installedModels.firstOrNull { it.id == action.id })
            }
            LocalModelSettingsAction.ConfirmDelete -> deletePending()
            LocalModelSettingsAction.DismissDelete -> _uiState.update { it.copy(pendingDelete = null) }
        }
    }

    private fun startDownload() {
        if (!_uiState.value.isRuntimeSupported || downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    installState = LocalModelInstallState.DOWNLOADING,
                    totalBytes = LocalModelCatalog.Qwen3_1_7B.sizeBytes,
                )
            }
            try {
                downloads.download(LocalModelCatalog.Qwen3_1_7B).collect { event ->
                    when (event) {
                        is LocalModelDownloadEvent.Progress -> _uiState.update {
                            it.copy(
                                downloadedBytes = event.downloadedBytes,
                                totalBytes = event.totalBytes,
                                bytesPerSecond = event.bytesPerSecond,
                            )
                        }
                        LocalModelDownloadEvent.Verifying -> _uiState.update {
                            it.copy(installState = LocalModelInstallState.VERIFYING)
                        }
                        is LocalModelDownloadEvent.Completed -> {
                            settings.setSelectedLocalModelId(event.model.id)
                            settings.setChatModelMode(ChatModelMode.LOCAL)
                            _uiState.update { it.copy(installState = LocalModelInstallState.READY) }
                            show("local_model_installed")
                        }
                        is LocalModelDownloadEvent.Failed -> {
                            _uiState.update { it.copy(installState = LocalModelInstallState.FAILED) }
                            show(event.code, event.message)
                        }
                    }
                }
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(installState = LocalModelInstallState.NOT_INSTALLED) }
                show("local_download_paused")
            }
        }
    }

    private fun stageImport(action: LocalModelSettingsAction.ImportSelected) {
        viewModelScope.launch {
            _uiState.update { it.copy(installState = LocalModelInstallState.VERIFYING) }
            downloads.stageImport(action.source, action.fileName, action.displayName)
                .onSuccess { staged ->
                    _uiState.update {
                        it.copy(stagedImport = staged, installState = LocalModelInstallState.NOT_INSTALLED)
                    }
                }
                .onFailure {
                    _uiState.update { state -> state.copy(installState = LocalModelInstallState.FAILED) }
                    show("local_import_failed", it.message)
                }
        }
    }

    private fun installImport() {
        val staged = _uiState.value.stagedImport ?: return
        viewModelScope.launch {
            downloads.installImport(staged)
                .onSuccess {
                    settings.setSelectedLocalModelId(it.id)
                    settings.setChatModelMode(ChatModelMode.LOCAL)
                    _uiState.update { state -> state.copy(stagedImport = null, installState = LocalModelInstallState.READY) }
                    show("local_model_installed")
                }
                .onFailure { show("local_import_failed", it.message) }
        }
    }

    private fun deletePending() {
        val model = _uiState.value.pendingDelete ?: return
        if (runtime.status.value.isBusy) {
            show("local_inference_busy")
            return
        }
        viewModelScope.launch {
            downloads.delete(model)
            if (_uiState.value.selectedModelId == model.id) {
                settings.setSelectedLocalModelId(null)
                settings.setChatModelMode(ChatModelMode.ONLINE)
            }
            _uiState.update { it.copy(pendingDelete = null) }
            show("local_model_deleted")
        }
    }

    private fun show(code: String, detail: String? = null) {
        _effects.trySend(LocalModelSettingsEffect.ShowMessage(code, detail))
    }
}
