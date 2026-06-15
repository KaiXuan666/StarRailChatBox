@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

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
import kotlin.uuid.Uuid
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
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProviderRegistry
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationRequest
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationOutput
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.call.body
import io.ktor.http.contentType
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlin.io.encoding.Base64
import kotlinx.io.readByteArray
import kotlinx.coroutines.flow.first
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

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
    private val imageProviderRegistry: ImageGenerationProviderRegistry,
    private val httpClient: HttpClient,
    private val appSettingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CharacterEditUiState(
            characterId = characterId,
            isImporting = importPath != null && importName != null && importExtension != null,
        ),
    )
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            if (characterId != null) {
                characterRepository.getCharacter(characterId)?.let { character ->
                    _uiState.value = character.toEditUiState()
                }
            } else {
                val nickname = appSettingsStore.userNickname.first()
                update { it.copy(author = nickname) }
            }
            if (importPath != null && importName != null && importExtension != null) {
                importCharacter(importPath, importName, importExtension)
            }
        }
    }

    fun onAction(action: CharacterAction) {
        when (action) {
            is CharacterAction.CharacterNameChanged -> update { it.copy(name = action.name) }
            is CharacterAction.CharacterAuthorChanged -> update { it.copy(author = action.author) }
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
            CharacterAction.RestoreBuiltinCharactersClicked -> Unit
            is CharacterAction.CharacterPromptGenClicked -> openPromptGenerator(action.defaultPromptRequestText)
            is CharacterAction.CharacterPromptGenInputChanged -> update { it.copy(promptGenInputText = action.text) }
            CharacterAction.CharacterPromptGenCancelClicked -> update { it.copy(isPromptGenDialogOpen = false) }
            CharacterAction.CharacterPromptGenConfirmClicked -> generatePrompt()
            is CharacterAction.CharacterAvatarGenClicked -> openAvatarGenerator(action.defaultPromptRequestText)
            is CharacterAction.CharacterAvatarGenInputChanged -> update { it.copy(avatarGenInputText = action.text) }
            CharacterAction.CharacterAvatarGenCancelClicked -> update { it.copy(isAvatarGenDialogOpen = false) }
            CharacterAction.CharacterAvatarGenConfirmClicked -> generateAvatar()
            CharacterAction.CharacterVoiceGenClicked -> openVoiceGenerator()
            is CharacterAction.CharacterVoiceGenInputChanged -> update { it.copy(voiceGenInputText = action.text) }
            CharacterAction.CharacterVoiceGenCancelClicked -> update { it.copy(isVoiceGenDialogOpen = false) }
            CharacterAction.CharacterVoiceGenConfirmClicked -> generateVoice()
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
            is CharacterAction.CharacterExportDirectorySelected -> export(action.directory)
            is CharacterAction.CharacterExportClicked,
            CharacterAction.CharacterExportDialogDismissed,
            CharacterAction.CharacterExportLocalClicked,
            CharacterAction.CharacterSharePublicClicked,
            CharacterAction.CharacterShareCategoryConfirmed,
            CharacterAction.CharacterShareCategoryDialogDismissed,
            CharacterAction.CharacterShareCustomCategorySelected,
            is CharacterAction.CharacterShareCategorySelected,
            is CharacterAction.CharacterShareCustomCategoryNameChanged,
            is CharacterAction.CharacterShareTagToggled,
            -> Unit
            is CharacterAction.CharacterEditOpened,
            CharacterAction.CharacterImportClicked,
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
                fileManager.writeBytes(path, fileManager.readSourceBytes(uri))
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
                fileManager.writeBytes(path, fileManager.readSourceBytes(uri))
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
        val isDescEmpty = _uiState.value.description.isBlank()
        val isOpeningEmpty = _uiState.value.openingMessage.isBlank()
        val isInjecting = isDescEmpty || isOpeningEmpty
        
        val actualInput = if (isInjecting) {
            val itemsNeeded = mutableListOf<String>()
            val xmlExamples = mutableListOf<String>()
            
            if (isDescEmpty) {
                itemsNeeded.add("“角色描述”")
                xmlExamples.add("<description>在此处填写你为该角色设计的简短描述或背景介绍（适合放在角色卡片上）</description>")
            }
            if (isOpeningEmpty) {
                itemsNeeded.add("“第一句聊天开场白”")
                xmlExamples.add("<opening_message>在此处填写你为该角色设计的第一句聊天开场白（要符合角色性格和语气）</opening_message>")
            }
            
            val itemsStr = itemsNeeded.joinToString("和")
            val xmlExamplesStr = (xmlExamples + "<prompt>在此处填写你为该角色生成的完整系统提示词（System Prompt）</prompt>").joinToString("\n")

            """
            $input

            [系统指令：请根据上述要求生成该角色的设定。由于目前该角色的${itemsStr}为空，请在生成该角色的设定（Prompt）时，同时为其设计${itemsStr}。请严格按照以下 XML 标签格式回复，不要包含任何其他多余的说明文字：
            $xmlExamplesStr]
            """.trimIndent()
        } else {
            input
        }

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
                    listOf(AiMessage(role = ChatRole.USER.apiValue, content = actualInput)),
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
                    if (isInjecting) {
                        val descriptionRegex = Regex("<description>([\\s\\S]*?)</description>", RegexOption.IGNORE_CASE)
                        val openingMessageRegex = Regex("<opening_message>([\\s\\S]*?)</opening_message>", RegexOption.IGNORE_CASE)
                        val promptRegex = Regex("<prompt>([\\s\\S]*?)</prompt>", RegexOption.IGNORE_CASE)

                        val parsedDesc = if (isDescEmpty) descriptionRegex.find(generated)?.groupValues?.getOrNull(1)?.trim() else null
                        val parsedOpening = if (isOpeningEmpty) openingMessageRegex.find(generated)?.groupValues?.getOrNull(1)?.trim() else null
                        val parsedPrompt = promptRegex.find(generated)?.groupValues?.getOrNull(1)?.trim()

                        if (parsedDesc != null || parsedOpening != null || parsedPrompt != null) {
                            update {
                                it.copy(
                                    description = parsedDesc ?: it.description,
                                    openingMessage = parsedOpening ?: it.openingMessage,
                                    prompt = parsedPrompt ?: it.prompt
                                )
                            }
                        } else {
                            update { it.copy(prompt = generated) }
                        }
                    } else {
                        update { it.copy(prompt = generated) }
                    }
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

    private fun openAvatarGenerator(defaultText: String) {
        if (_uiState.value.name.isBlank()) {
            showMessage(CharacterEffectMessage.CHARACTER_NAME_REQUIRED)
        } else {
            update { it.copy(isAvatarGenDialogOpen = true, avatarGenInputText = defaultText) }
        }
    }

    private fun generateAvatar() {
        val prompt = _uiState.value.avatarGenInputText
        update { it.copy(isAvatarGenDialogOpen = false, isGeneratingAvatar = true) }
        viewModelScope.launch {
            try {
                val config = modelConfigRepository.getImageGeneration()?.takeIf {
                    it.enabled && it.baseUrl.isNotBlank() && it.apiKey.isNotBlank() && it.modelName.isNotBlank()
                }
                if (config == null) {
                    showMessage(CharacterEffectMessage.IMAGE_CONFIG_REQUIRED)
                    return@launch
                }
                val provider = imageProviderRegistry.find(config.provider)
                if (provider == null) {
                    showMessage(CharacterEffectMessage.IMAGE_CONFIG_REQUIRED)
                    return@launch
                }
                val result = provider.generate(
                    config = config,
                    request = ImageGenerationRequest(
                        prompt = prompt,
                        aspectRatio = "1:1",
                    )
                )
                when (result) {
                    is ApiResult.Success -> {
                        val localPath = saveGeneratedAvatar(result.value)
                        val ext = localPath.substringAfterLast('.', "png")
                        val source = CharacterAvatarSource(
                            uri = localPath,
                            name = "avatar_${now()}.$ext",
                            extension = ext
                        )
                        update {
                            it.copy(
                                avatarUri = localPath,
                                pendingAvatarSource = source
                            )
                        }
                    }
                    else -> {
                        showMessage(CharacterEffectMessage.AVATAR_GEN_FAILED)
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Napier.e("Failed to generate avatar", error)
                showMessage(CharacterEffectMessage.AVATAR_GEN_FAILED)
            } finally {
                update { it.copy(isGeneratingAvatar = false) }
            }
        }
    }

    private fun openVoiceGenerator() {
        viewModelScope.launch {
            try {
                val voiceConfig = modelConfigRepository.getVoice()?.takeIf {
                    it.baseUrl.isNotBlank() && it.apiKey.isNotBlank()
                }
                if (voiceConfig == null) {
                    showMessage(CharacterEffectMessage.VOICE_CONFIG_REQUIRED)
                    return@launch
                }
                val defaultPrompt = "一个16岁的动漫少女，甜美清脆的声音，语速中等，充满活力却不失温柔，带有轻微的南方口音，清晰自然。"
                update { 
                    it.copy(
                        isVoiceGenDialogOpen = true, 
                        voiceGenInputText = defaultPrompt
                    ) 
                }
            } catch (t: Throwable) {
                Napier.e("Failed to open voice generator", t)
            }
        }
    }

    private fun generateVoice() {
        val input = _uiState.value.voiceGenInputText
        update { it.copy(isVoiceGenDialogOpen = false, isGeneratingVoice = true) }
        viewModelScope.launch {
            try {
                val voiceConfig = modelConfigRepository.getVoice()?.takeIf {
                    it.baseUrl.isNotBlank() && it.apiKey.isNotBlank()
                }
                if (voiceConfig == null) {
                    showMessage(CharacterEffectMessage.VOICE_CONFIG_REQUIRED)
                    update { it.copy(isGeneratingVoice = false) }
                    return@launch
                }

                val requestBody = buildJsonObject {
                    put("model", voiceConfig.modelName.takeIf(String::isNotBlank) ?: "mimo-v2.5-tts-voicedesign")
                    putJsonArray("messages") {
                        add(buildJsonObject {
                            put("role", "user")
                            put("content", input)
                        })
                        add(buildJsonObject {
                            put("role", "assistant")
                            put("content", "你好，很高兴认识你。")
                        })
                    }
                }

                val response = httpClient.post("${voiceConfig.baseUrl.trimEnd('/')}/chat/completions") {
                    header(HttpHeaders.Authorization, "Bearer ${voiceConfig.apiKey.trim()}")
                    header("api-key", voiceConfig.apiKey.trim())
                    contentType(ContentType.Application.Json)
                    setBody(requestBody)
                }

                val responseText = response.bodyAsText()
                val jsonObject = Json.parseToJsonElement(responseText).jsonObject
                val base64Data = jsonObject["choices"]?.jsonArray?.getOrNull(0)?.jsonObject
                    ?.get("message")?.jsonObject
                    ?.get("audio")?.jsonObject
                    ?.get("data")?.jsonPrimitive?.content

                if (!base64Data.isNullOrBlank()) {
                    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
                    val audioBytes = kotlin.io.encoding.Base64.Default.decode(base64Data.trim())
                    val randomSuffix = now().toString(36)
                    val path = fileManager.cacheDir / "temp_voice_$randomSuffix.wav".toPath()
                    fileManager.writeBytes(path, audioBytes)
                    update { it.copy(voiceSampleUri = path.toString()) }
                } else {
                    showMessage(CharacterEffectMessage.VOICE_GEN_FAILED)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (t: Throwable) {
                Napier.e("Failed to generate voice", t)
                showMessage(CharacterEffectMessage.VOICE_GEN_FAILED)
            } finally {
                update { it.copy(isGeneratingVoice = false) }
            }
        }
    }

    private suspend fun saveGeneratedAvatar(
        output: ImageGenerationOutput,
    ): String {
        val randomSuffix = now().toString(36)
        val fullPath = when (output) {
            is ImageGenerationOutput.Url -> {
                httpClient.prepareGet(output.url).execute { response ->
                    val extension = extensionFromMime(response.contentType()?.toString()) ?: "png"
                    val path = fileManager.cacheDir / "temp_avatar_$randomSuffix.$extension".toPath()
                    fileManager.fileSystem.write(path) {
                        val channel: ByteReadChannel = response.body()
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(8192)
                            while (!packet.exhausted()) {
                                write(packet.readByteArray())
                            }
                        }
                    }
                    path
                }
            }
            is ImageGenerationOutput.Base64 -> {
                val extension = extensionFromMime(output.data.dataUrlMimeType()) ?: "png"
                val path = fileManager.cacheDir / "temp_avatar_$randomSuffix.$extension".toPath()
                val encoded = output.data.substringAfter("base64,", output.data).trim()
                fileManager.fileSystem.write(path) {
                    write(Base64.Default.decode(encoded))
                }
                path
            }
        }
        return fullPath.toString()
    }

    private fun extensionFromMime(mimeType: String?): String? {
        return when (mimeType?.substringBefore(';')?.lowercase()) {
            "image/jpeg" -> "jpg"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/png" -> "png"
            else -> null
        }
    }

    private fun String.dataUrlMimeType(): String? {
        if (!startsWith("data:", ignoreCase = true)) return null
        return substringAfter("data:").substringBefore(';').takeIf(String::isNotBlank)
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
                val id = edit.characterId ?: Uuid.random().toString()
                val original = edit.characterId?.let { characterRepository.getCharacter(it) }
                characterRepository.updateCharacter(
                    Character(
                        id = id,
                        name = edit.name.trim(),
                        author = edit.author.trim(),
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
                            author = draft.author,
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
        try {
            val state = _uiState.value
            listOfNotNull(
                state.avatarUri,
                state.voiceSampleUri,
                importPath,
            ).filter { it.startsWith(fileManager.cacheDir.toString()) }
                .forEach { fileManager.delete(it.toPath()) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        super.onCleared()
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
