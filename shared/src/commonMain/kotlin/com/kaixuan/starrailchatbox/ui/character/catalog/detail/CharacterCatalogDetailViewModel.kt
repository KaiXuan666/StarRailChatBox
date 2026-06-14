package com.kaixuan.starrailchatbox.ui.character.catalog.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import okio.Path.Companion.toPath

class CharacterCatalogDetailViewModel(
    private val characterId: String,
    private val detailUrl: String,
    private val initialName: String,
    private val initialAvatarUrl: String?,
    private val catalogRepository: PublicCharacterCatalogRepository,
    private val characterRepository: CharacterRepository,
    private val httpClient: HttpClient,
    private val fileManager: KmpFileManager = KmpFileManager.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CharacterCatalogDetailUiState(
            characterId = characterId,
            detailUrl = detailUrl,
            initialName = initialName,
            initialAvatarUrl = initialAvatarUrl,
        )
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterCatalogDetailEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // 观察本地已导入的角色，随时刷新状态
        viewModelScope.launch {
            characterRepository.observeCharacterSummaries().collect { list ->
                val imported = list.any { it.id == characterId }
                _uiState.update { it.copy(isImported = imported) }
            }
        }
        onAction(CharacterCatalogDetailAction.LoadDetail)
    }

    fun onAction(action: CharacterCatalogDetailAction) {
        when (action) {
            is CharacterCatalogDetailAction.LoadDetail -> loadDetail()
            is CharacterCatalogDetailAction.ImportClicked -> importCharacter()
            is CharacterCatalogDetailAction.PlayVoiceClicked -> downloadVoiceIfNeed()
        }
    }

    fun resolveUrl(url: String): String {
        return catalogRepository.resolveUrl(url)
    }

    private fun loadDetail() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = catalogRepository.getCharacterDetail(detailUrl)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(detail = result.value, isLoading = false) }
                    loadTags()
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.send(CharacterCatalogDetailEffect.ShowToast("加载角色详情失败"))
                }
            }
        }
    }

    private fun loadTags() {
        viewModelScope.launch {
            val result = catalogRepository.getCatalog()
            if (result is ApiResult.Success) {
                _uiState.update {
                    it.copy(tags = result.value.catalog?.tags.orEmpty())
                }
            }
        }
    }

    private fun downloadVoiceIfNeed() {
        val detail = _uiState.value.detail ?: return
        val voiceUrl = detail.voiceSampleUrl
        if (voiceUrl.isNullOrBlank()) return

        val currentLocalPath = _uiState.value.voiceSampleLocalPath
        if (currentLocalPath != null && fileManager.exists(currentLocalPath.toPath())) {
            return
        }

        _uiState.update { it.copy(isVoiceDownloading = true) }
        viewModelScope.launch {
            try {
                val resolvedUrl = catalogRepository.resolveUrl(voiceUrl)
                val responseBytes = httpClient.get(resolvedUrl).body<ByteArray>()
                val ext = voiceUrl.substringAfterLast('.', "mp3")
                val cacheFileName = "temp_voice_preview_${characterId}_${Clock.System.now().toEpochMilliseconds()}.$ext"
                val cachePath = fileManager.cacheDir / cacheFileName.toPath()
                fileManager.writeBytes(cachePath, responseBytes)
                _uiState.update { it.copy(voiceSampleLocalPath = cachePath.toString()) }
            } catch (e: Exception) {
                Napier.e("Failed to download voice preview", e)
                _effects.send(CharacterCatalogDetailEffect.ShowToast("下载语音样本失败"))
            } finally {
                _uiState.update { it.copy(isVoiceDownloading = false) }
            }
        }
    }

    private fun importCharacter() {
        val detail = _uiState.value.detail ?: return
        if (_uiState.value.isImporting || _uiState.value.isImported) return

        _uiState.update { it.copy(isImporting = true) }
        viewModelScope.launch {
            try {
                // 两阶段落盘法第一阶段：下载图片存入缓存
                val avatarSource = if (!detail.avatarUrl.isNullOrBlank()) {
                    try {
                        val responseBytes = httpClient.get(catalogRepository.resolveUrl(detail.avatarUrl)).body<ByteArray>()
                        val ext = detail.avatarUrl.substringAfterLast('.', "png")
                        val cacheFileName = "temp_import_${characterId}_${Clock.System.now().toEpochMilliseconds()}.$ext"
                        val cachePath = fileManager.cacheDir / cacheFileName.toPath()
                        fileManager.writeBytes(cachePath, responseBytes)
                        CharacterAvatarSource(
                            uri = cachePath.toString(),
                            name = cacheFileName,
                            extension = ext
                        )
                    } catch (e: Exception) {
                        Napier.e("Failed to download avatar", e)
                        null
                    }
                } else {
                    null
                }

                // 第一阶段：下载语音存入缓存
                val voiceSampleUri = if (!detail.voiceSampleUrl.isNullOrBlank()) {
                    try {
                        val responseBytes = httpClient.get(catalogRepository.resolveUrl(detail.voiceSampleUrl)).body<ByteArray>()
                        val ext = detail.voiceSampleUrl.substringAfterLast('.', "mp3")
                        val cacheFileName = "temp_voice_${characterId}_${Clock.System.now().toEpochMilliseconds()}.$ext"
                        val cachePath = fileManager.cacheDir / cacheFileName.toPath()
                        fileManager.writeBytes(cachePath, responseBytes)
                        cachePath.toString()
                    } catch (e: Exception) {
                        Napier.e("Failed to download voice", e)
                        null
                    }
                } else {
                    null
                }

                // 保存角色并激活 Room 内部两阶段落盘法
                characterRepository.updateCharacter(
                    Character(
                        id = characterId,
                        name = detail.name.trim(),
                        prompt = detail.systemPrompt,
                        openingMessage = detail.openingMessage,
                        avatarUri = "",
                        author = detail.author.trim(),
                        description = detail.description,
                        voiceSampleUri = voiceSampleUri,
                        temperature = detail.temperature,
                        topP = detail.topP,
                        createdAt = Clock.System.now().toEpochMilliseconds(),
                    ),
                    avatarSource = avatarSource
                )

                _effects.send(CharacterCatalogDetailEffect.ShowToast("角色“${detail.name}”导入成功"))
            } catch (e: Exception) {
                Napier.e("Import character failed in detail screen", e)
                _effects.send(CharacterCatalogDetailEffect.ShowToast("导入失败：${e.message}"))
            } finally {
                _uiState.update { it.copy(isImporting = false) }
            }
        }
    }
}
