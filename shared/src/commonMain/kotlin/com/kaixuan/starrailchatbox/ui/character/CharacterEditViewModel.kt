package com.kaixuan.starrailchatbox.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardImporter
import com.kaixuan.starrailchatbox.data.chat.ChatRole
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import com.kaixuan.starrailchatbox.platform.readUriAsBytes
import io.github.aakira.napier.Napier
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okio.Path.Companion.toPath
import kotlin.time.Clock

data class CharacterEditArgs(
    val characterId: String?,
    val importPath: String?,
    val importName: String?,
    val importExtension: String?,
)

class CharacterEditViewModel(
    private val characterId: String?,
    private val importPath: String?,
    private val importName: String?,
    private val importExtension: String?,
    private val characterRepository: CharacterRepository,
    private val modelConfigRepository: ModelConfigRepository,
    private val aiRepository: AiRepository,
    private val characterCardImporter: CharacterCardImporter,
    private val characterCardExporter: CharacterCardExporter,
    private val fileManager: KmpFileManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharacterEditUiState(characterId = characterId))
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (characterId != null) {
                characterRepository.getCharacter(characterId)?.let { character ->
                    _uiState.value = character.toEditUiState()
                }
            }
            if (importPath != null && importName != null && importExtension != null) {
                importCharacter(importPath, importName, importExtension)
            }
        }
    }

    fun onAction(action: CharacterAction) {
        when (action) {
            is CharacterAction.CharacterNameChanged -> update { it.copy(name = action.name) }
            is CharacterAction.CharacterDescriptionChanged -> update { it.copy(description = action.description) }
            is CharacterAction.CharacterPromptChanged -> update { it.copy(prompt = action.prompt) }
            is CharacterAction.CharacterOpeningMessageChanged -> update {
                it.copy(openingMessage = action.openingMessage)
            }
            is CharacterAction.CharacterAvatarChanged -> cacheAvatar(action.avatarSource)
            is CharacterAction.CharacterVoiceSampleChanged -> cacheVoice(action.uri, action.extension)
            is CharacterAction.CharacterTemperatureChanged -> update {
                it.copy(temperature = action.temperature.coerceIn(0.0, 2.0))
            }
            is CharacterAction.CharacterTopPChanged -> update {
                it.copy(topP = action.topP.coerceIn(0.0, 1.0))
            }
            CharacterAction.CharacterSaveClicked -> save()
            is CharacterAction.CharacterDeleteClicked -> delete(action.characterId)
            CharacterAction.CharacterDeleteBuiltinClicked -> {
                showMessage(CharacterEffectMessage.CHARACTER_DELETE_BUILTIN_RESTRICTED)
            }
            is CharacterAction.CharacterPromptGenClicked -> openPromptGenerator(action.defaultPromptRequestText)
            is CharacterAction.CharacterPromptGenInputChanged -> update { it.copy(promptGenInputText = action.text) }
            CharacterAction.CharacterPromptGenCancelClicked -> update { it.copy(isPromptGenDialogOpen = false) }
            CharacterAction.CharacterPromptGenConfirmClicked -> generatePrompt()
            CharacterAction.CharacterRestoreDefaultClicked -> restoreDefault()
            is CharacterAction.CharacterImportFileSelected -> {
                importCharacter(action.path, action.name, action.extension)
            }
            CharacterAction.CharacterImportWarningDismissed -> update {
                it.copy(importDraft = it.importDraft?.copy(warnings = emptyList()))
            }
            CharacterAction.CharacterImportCancelled -> update {
                it.copy(importDraft = null, isImporting = false, importError = null)
            }
            CharacterAction.CharacterExportClicked -> viewModelScope.launch {
                _effects.send(CharacterEffect.RequestDirectoryPicker)
            }
            is CharacterAction.CharacterExportDirectorySelected -> export(action.directory)
            is CharacterAction.CharacterEditOpened,
            CharacterAction.CharacterImportClicked,
            CharacterAction.RefreshCharacters,
            is CharacterAction.CharacterSelected,
            is CharacterAction.CharactersReordered,
            -> Unit
        }
    }

    private fun cacheAvatar(source: CharacterAvatarSource) {
        viewModelScope.launch {
            val uri = source.uri
            if (!shouldCache(uri)) {
                update { it.copy(avatarUri = uri, pendingAvatarSource = source) }
                return@launch
            }
            runCatching {
                val extension = source.extension ?: uri.substringAfterLast('.', "png")
                val path = fileManager.cacheDir / "temp_avatar_${now()}.$extension".toPath()
                fileManager.writeBytes(path, readUriAsBytes(uri))
                update {
                    it.copy(
                        avatarUri = path.toString(),
                        pendingAvatarSource = source.copy(uri = path.toString(), extension = extension),
                    )
                }
            }.onFailure {
                Napier.e("Failed to cache character avatar", it)
                update { state -> state.copy(avatarUri = uri, pendingAvatarSource = source) }
            }
        }
    }

    private fun cacheVoice(uri: String?, extension: String?) {
        viewModelScope.launch {
            if (uri == null || !shouldCache(uri) || uri.startsWith("builtin:")) {
                update { it.copy(voiceSampleUri = uri) }
                return@launch
            }
            runCatching {
                val suffix = extension ?: uri.substringAfterLast('.', "mp3")
                val path = fileManager.cacheDir / "temp_voice_${now()}.$suffix".toPath()
                fileManager.writeBytes(path, readUriAsBytes(uri))
                update { it.copy(voiceSampleUri = path.toString()) }
            }.onFailure {
                Napier.e("Failed to cache character voice sample", it)
                update { state -> state.copy(voiceSampleUri = uri) }
            }
        }
    }

    private fun shouldCache(uri: String): Boolean =
        fileManager.isSupported &&
            uri.isNotBlank() &&
            !uri.startsWith("http") &&
            !uri.startsWith("data:") &&
            !uri.startsWith("picked:")

    private fun openPromptGenerator(defaultText: String) {
        if (_uiState.value.name.isBlank()) {
            showMessage(CharacterEffectMessage.CHARACTER_NAME_REQUIRED)
        } else {
            update { it.copy(isPromptGenDialogOpen = true, promptGenInputText = defaultText) }
        }
    }

    private fun generatePrompt() {
        val input = _uiState.value.promptGenInputText
        update { it.copy(isPromptGenDialogOpen = false, isGeneratingPrompt = true) }
        viewModelScope.launch {
            try {
                val config = modelConfigRepository.getDefault()?.takeIf {
                    it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.modelName.isNotBlank()
                }
                if (config == null) {
                    showMessage(CharacterEffectMessage.MODEL_CONFIG_REQUIRED)
                    update { it.copy(isGeneratingPrompt = false) }
                    return@launch
                }
                var generated = ""
                var failed = false
                aiRepository.createPromptCompletion(
                    config,
                    listOf(AiMessage(role = ChatRole.USER.apiValue, content = input)),
                ).collect { result ->
                    if (result is ApiResult.Success) {
                        generated = result.value.content.trim()
                    } else {
                        failed = true
                    }
                }
                if (failed || generated.isBlank()) {
                    showMessage(CharacterEffectMessage.PROMPT_GEN_FAILED)
                } else {
                    update { it.copy(prompt = generated) }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                showMessage(CharacterEffectMessage.PROMPT_GEN_FAILED)
            } finally {
                update { it.copy(isGeneratingPrompt = false) }
            }
        }
    }

    private fun restoreDefault() {
        val id = _uiState.value.characterId ?: return
        viewModelScope.launch {
            characterRepository.getDefaultCharacter(id)?.let { default ->
                _uiState.value = default.toEditUiState()
            }
        }
    }

    private fun save() {
        val edit = _uiState.value
        if (edit.name.isBlank()) {
            showMessage(CharacterEffectMessage.CHARACTER_NAME_EMPTY)
            return
        }
        update { it.copy(isSaving = true) }
        viewModelScope.launch {
            runCatching {
                val id = edit.characterId ?: "user_${now()}"
                val original = edit.characterId?.let { characterRepository.getCharacter(it) }
                characterRepository.updateCharacter(
                    Character(
                        id = id,
                        name = edit.name.trim(),
                        description = edit.description,
                        prompt = edit.prompt,
                        openingMessage = edit.openingMessage,
                        avatarUri = edit.avatarUri,
                        voiceSampleUri = edit.voiceSampleUri,
                        temperature = edit.temperature,
                        topP = edit.topP,
                        createdAt = original?.createdAt ?: now(),
                        sortOrder = original?.sortOrder ?: 0,
                        lastMessageAt = original?.lastMessageAt,
                    ),
                    edit.pendingAvatarSource,
                )
            }.onSuccess { saved ->
                _uiState.value = saved.toEditUiState()
                _effects.send(CharacterEffect.CharacterSaved)
            }.onFailure {
                update { state -> state.copy(isSaving = false) }
                showMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED)
            }
        }
    }

    private fun delete(id: String) {
        viewModelScope.launch {
            runCatching { characterRepository.deleteCharacter(id, now()) }
                .onSuccess { _effects.send(CharacterEffect.CharacterDeleted) }
                .onFailure { showMessage(CharacterEffectMessage.CHARACTER_SAVE_FAILED) }
        }
    }

    private fun importCharacter(path: String, name: String, extension: String) {
        update { it.copy(isImporting = true, importError = null) }
        viewModelScope.launch {
            when (val result = characterCardImporter.importFromFile(path, name, extension)) {
                is ApiResult.Success -> {
                    val draft = result.value
                    update {
                        it.copy(
                            isImporting = false,
                            importDraft = draft,
                            name = draft.name,
                            description = draft.description,
                            prompt = draft.prompt,
                            openingMessage = draft.openingMessage,
                            temperature = draft.temperature,
                            topP = draft.topP,
                            avatarUri = draft.avatarUri ?: it.avatarUri,
                            voiceSampleUri = draft.voice?.let { voice ->
                                "data:${voice.mimeType};base64,${voice.base64Content}"
                            } ?: it.voiceSampleUri,
                            pendingAvatarSource = draft.avatarUri?.let { uri ->
                                CharacterAvatarSource(uri, name, extension)
                            } ?: it.pendingAvatarSource,
                        )
                    }
                    showMessage(CharacterEffectMessage.CHARACTER_IMPORT_SUCCESS)
                }
                else -> {
                    update { it.copy(isImporting = false, importError = "Import failed") }
                    showMessage(CharacterEffectMessage.CHARACTER_IMPORT_FAILED)
                }
            }
        }
    }

    private fun export(directory: PlatformFile) {
        val id = _uiState.value.characterId ?: return
        update { it.copy(isExporting = true, exportError = null) }
        viewModelScope.launch {
            val result = runCatching {
                characterRepository.getCharacter(id)?.let {
                    characterCardExporter.exportToPng(it, directory)
                }
            }.getOrNull()
            update { it.copy(isExporting = false) }
            showMessage(
                if (result is ApiResult.Success) {
                    CharacterEffectMessage.CHARACTER_EXPORT_SUCCESS
                } else {
                    CharacterEffectMessage.CHARACTER_EXPORT_FAILED
                },
            )
        }
    }

    private fun update(transform: (CharacterEditUiState) -> CharacterEditUiState) {
        _uiState.update(transform)
    }

    private fun showMessage(message: CharacterEffectMessage) {
        _effects.trySend(CharacterEffect.ShowMessage(message))
    }

    override fun onCleared() {
        val state = _uiState.value
        listOfNotNull(
            state.avatarUri,
            state.voiceSampleUri,
            importPath,
        ).filter { it.startsWith(fileManager.cacheDir.toString()) }
            .forEach { fileManager.delete(it.toPath()) }
        super.onCleared()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
