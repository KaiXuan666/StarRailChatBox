package com.kaixuan.starrailchatbox.ui.settings.api

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.AiModelDiscovery
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProviderRegistry
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.DefaultModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsDefaults
import com.kaixuan.starrailchatbox.data.model.MultimodalModelConfig
import com.kaixuan.starrailchatbox.data.model.ImageGenerationModelConfig
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffectMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApiSettingsViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialStateIsCorrect() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        val state = viewModel.uiState.value
        assertEquals("https://api.openai.com/v1", state.apiHost)
        assertEquals("", state.apiKey)
        assertFalse(state.showApiKey)
        assertEquals("", state.selectedModel)
        assertTrue(state.modelsList.isEmpty())
        assertFalse(state.isFetchingModels)
    }

    @Test
    fun storedSettingsAreLoaded() = runTest {
        val repository = FakeModelConfigRepository(
            modelConfig(
                baseUrl = "https://example.com/v1",
                apiKey = "stored-key",
                modelName = "model-b",
            ),
        )
        val viewModel = createViewModel(modelConfigRepository = repository)
        runCurrent()

        assertEquals("https://example.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("stored-key", viewModel.uiState.value.apiKey)
        assertEquals("model-b", viewModel.uiState.value.selectedModel)
        assertEquals(listOf("model-b"), viewModel.uiState.value.modelsList)
    }

    @Test
    fun localDefaultsAreUsedWhenStoredSettingsAreMissing() = runTest {
        val viewModel = createViewModel(
            defaults = ApiSettingsDefaults(
                apiHost = "https://local.example.com/v1",
                apiKey = "local-key",
            ),
        )
        runCurrent()

        assertEquals("https://local.example.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("local-key", viewModel.uiState.value.apiKey)
    }

    @Test
    fun localDefaultsFillBlankStoredFields() = runTest {
        val repository = FakeModelConfigRepository(
            modelConfig(
                baseUrl = "",
                apiKey = "",
                modelName = "model-a",
            ),
        )
        val viewModel = createViewModel(
            modelConfigRepository = repository,
            defaults = ApiSettingsDefaults(
                apiHost = "https://local.example.com/v1",
                apiKey = "local-key",
            ),
        )
        runCurrent()

        assertEquals("https://local.example.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("local-key", viewModel.uiState.value.apiKey)
        assertEquals("model-a", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun hostAndKeyChangesUpdateState() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onAction(ApiSettingsAction.ApiHostChanged("https://api.openai.com/v1"))
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("sk-12345"))

        assertEquals("https://api.openai.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("sk-12345", viewModel.uiState.value.apiKey)
    }

    @Test
    fun fetchModelsUpdatesListAndSelection() = runTest {
        val repository = FakeOpenAiRepository(
            ApiResult.Success(listOf("model-b", "model-a")),
        )
        val viewModel = createViewModel(repository = repository)
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("sk-test"))

        viewModel.onAction(ApiSettingsAction.FetchModelsClicked)
        advanceUntilIdle()

        assertEquals(listOf("model-b", "model-a"), viewModel.uiState.value.modelsList)
        assertEquals("model-b", viewModel.uiState.value.selectedModel)
        assertFalse(viewModel.uiState.value.isFetchingModels)
        assertEquals("https://api.openai.com/v1", repository.lastHost)
        assertEquals("sk-test", repository.lastKey)
    }

    @Test
    fun authenticationFailureEmitsEffect() = runTest {
        val repository = FakeOpenAiRepository(
            ApiResult.HttpError(statusCode = 401, message = "Unauthorized"),
        )
        val viewModel = createViewModel(repository = repository)
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("bad-key"))
        val effects = async { viewModel.effects.take(2).toList() }

        viewModel.onAction(ApiSettingsAction.FetchModelsClicked)
        advanceUntilIdle()

        assertEquals(
            listOf(
                ApiSettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START),
                ApiSettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_AUTH_FAILED),
            ),
            effects.await(),
        )
        assertFalse(viewModel.uiState.value.isFetchingModels)
    }

    @Test
    fun saveApiSettingsPersistsValuesAndEmitsSavedEffect() = runTest {
        val repository = FakeModelConfigRepository()
        val viewModel = createViewModel(modelConfigRepository = repository)
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged(" sk-test "))
        viewModel.onAction(ApiSettingsAction.SelectModel("model-a"))
        val effect = async { viewModel.effects.first { it is ApiSettingsEffect.ApiSettingsSaved } }

        viewModel.onAction(ApiSettingsAction.SaveSettingsClicked)
        advanceUntilIdle()

        assertEquals(
            modelConfig(
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test",
                modelName = "model-a",
            ),
            repository.saved,
        )
        assertEquals(ApiSettingsEffect.ApiSettingsSaved, effect.await())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun saveMultimodalApiSettingsPersistsValuesAndSetsSupportVisionTrue() = runTest {
        val repository = FakeModelConfigRepository()
        val viewModel = createViewModel(
            isMultimodal = true,
            modelConfigRepository = repository
        )
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("sk-multimodal-test"))
        viewModel.onAction(ApiSettingsAction.SelectModel("model-omni"))
        val effect = async { viewModel.effects.first { it is ApiSettingsEffect.ApiSettingsSaved } }

        viewModel.onAction(ApiSettingsAction.SaveSettingsClicked)
        advanceUntilIdle()

        val saved = requireNotNull(repository.savedMultimodal)
        assertEquals(MultimodalModelConfig.Id, saved.id)
        assertEquals("https://api.openai.com/v1", saved.baseUrl)
        assertEquals("sk-multimodal-test", saved.apiKey)
        assertEquals("model-omni", saved.modelName)
        assertTrue(saved.supportVision)
        assertEquals(ApiSettingsEffect.ApiSettingsSaved, effect.await())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun toggleKeyVisibilityWorks() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        assertFalse(viewModel.uiState.value.showApiKey)

        viewModel.onAction(ApiSettingsAction.ToggleApiKeyVisibility)
        assertTrue(viewModel.uiState.value.showApiKey)

        viewModel.onAction(ApiSettingsAction.ToggleApiKeyVisibility)
        assertFalse(viewModel.uiState.value.showApiKey)
    }

    @Test
    fun saveApiSettingsWithToolCallSupportedPersistsTrue() = runTest {
        val modelConfigRepository = FakeModelConfigRepository()
        val openAiRepository = FakeOpenAiRepository(
            result = ApiResult.Success(emptyList()),
            toolCallSupportResult = true
        )
        val viewModel = createViewModel(
            repository = openAiRepository,
            modelConfigRepository = modelConfigRepository
        )
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("sk-test"))
        viewModel.onAction(ApiSettingsAction.SelectModel("model-a"))

        viewModel.onAction(ApiSettingsAction.SaveSettingsClicked)
        advanceUntilIdle()

        val saved = requireNotNull(modelConfigRepository.saved)
        assertTrue(saved.supportToolCall)
        assertEquals("https://api.openai.com/v1", openAiRepository.lastHost)
        assertEquals("sk-test", openAiRepository.lastKey)
        assertEquals("model-a", openAiRepository.lastModelTested)
    }

    @Test
    fun saveApiSettingsWithToolCallNotSupportedPersistsFalse() = runTest {
        val modelConfigRepository = FakeModelConfigRepository()
        val openAiRepository = FakeOpenAiRepository(
            result = ApiResult.Success(emptyList()),
            toolCallSupportResult = false
        )
        val viewModel = createViewModel(
            repository = openAiRepository,
            modelConfigRepository = modelConfigRepository
        )
        runCurrent()
        viewModel.onAction(ApiSettingsAction.ApiKeyChanged("sk-test"))
        viewModel.onAction(ApiSettingsAction.SelectModel("model-a"))

        viewModel.onAction(ApiSettingsAction.SaveSettingsClicked)
        advanceUntilIdle()

        val saved = requireNotNull(modelConfigRepository.saved)
        assertFalse(saved.supportToolCall)
        assertEquals("https://api.openai.com/v1", openAiRepository.lastHost)
        assertEquals("sk-test", openAiRepository.lastKey)
        assertEquals("model-a", openAiRepository.lastModelTested)
    }

    private fun createViewModel(
        isMultimodal: Boolean = false,
        repository: AiRepository = FakeOpenAiRepository(ApiResult.Success(emptyList())),
        modelConfigRepository: ModelConfigRepository = FakeModelConfigRepository(),
        defaults: ApiSettingsDefaults = ApiSettingsDefaults(),
    ) = ApiSettingsViewModel(
        isMultimodal = isMultimodal,
        aiRepository = repository,
        modelConfigRepository = modelConfigRepository,
        imageProviderRegistry = ImageGenerationProviderRegistry(emptyList()),
        defaultApiSettings = defaults,
    )
}

private class FakeOpenAiRepository(
    private val result: ApiResult<List<String>>,
    private val toolCallSupportResult: Boolean = false,
) : AiRepository {
    var lastHost: String? = null
    var lastKey: String? = null
    var lastModelTested: String? = null

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> {
        lastHost = apiHost
        lastKey = apiKey
        return result
    }

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
        voiceSampleUri: String?,
    ): ApiResult<ChatCompletionResult> {
        return ApiResult.UnexpectedError("Not used by settings tests.")
    }

    override suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        return ApiResult.UnexpectedError("Not used by settings tests.")
    }

    override suspend fun createSessionTitle(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        return ApiResult.UnexpectedError("Not used by settings tests.")
    }

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String,
    ): Boolean {
        lastHost = apiHost
        lastKey = apiKey
        lastModelTested = model
        return toolCallSupportResult
    }

    override fun createPromptCompletion(
        config: ModelConfig,
        messages: List<AiMessage>
    ): Flow<ApiResult<ChatCompletionResult>> = flow {
        emit(ApiResult.UnexpectedError("Not used by settings tests."))
    }
}

private class FakeModelConfigRepository(
    private val initial: ModelConfig? = null,
    private val initialMultimodal: ModelConfig? = null,
    private val initialVoice: ModelConfig? = null,
    private val initialVoiceClone: ModelConfig? = null,
    private val initialImageGeneration: ModelConfig? = null,
) : ModelConfigRepository {
    private val _default = MutableStateFlow(initial)
    private val _multimodal = MutableStateFlow(initialMultimodal)
    private val _voice = MutableStateFlow(initialVoice)
    private val _voiceClone = MutableStateFlow(initialVoiceClone)
    private val _imageGeneration = MutableStateFlow(initialImageGeneration)

    var saved: ModelConfig? = null
    var savedMultimodal: ModelConfig? = null
    var savedVoice: ModelConfig? = null
    var savedVoiceClone: ModelConfig? = null
    var savedImageGeneration: ModelConfig? = null

    override suspend fun getDefault(): ModelConfig? = _default.value
    override fun observeDefault(): Flow<ModelConfig?> = _default.asStateFlow()
    override suspend fun saveDefault(config: ModelConfig) {
        saved = config
        _default.value = config
    }

    override suspend fun getMultimodal(): ModelConfig? = _multimodal.value
    override fun observeMultimodal(): Flow<ModelConfig?> = _multimodal.asStateFlow()
    override suspend fun saveMultimodal(config: ModelConfig) {
        savedMultimodal = config
        _multimodal.value = config
    }

    override suspend fun getVoice(): ModelConfig? = _voice.value
    override fun observeVoice(): Flow<ModelConfig?> = _voice.asStateFlow()
    override suspend fun saveVoice(config: ModelConfig) {
        savedVoice = config
        _voice.value = config
    }

    override suspend fun getVoiceClone(): ModelConfig? = _voiceClone.value
    override fun observeVoiceClone(): Flow<ModelConfig?> = _voiceClone.asStateFlow()
    override suspend fun saveVoiceClone(config: ModelConfig) {
        savedVoiceClone = config
        _voiceClone.value = config
    }

    override suspend fun getImageGeneration(): ModelConfig? = _imageGeneration.value
    override fun observeImageGeneration(): Flow<ModelConfig?> = _imageGeneration.asStateFlow()
    override suspend fun saveImageGeneration(config: ModelConfig) {
        savedImageGeneration = config
        _imageGeneration.value = config
    }

    override suspend fun deleteConfig(id: String) {
        when (id) {
            DefaultModelConfig.Id -> {
                saved = null
                _default.value = null
            }
            MultimodalModelConfig.Id -> {
                savedMultimodal = null
                _multimodal.value = null
            }
            "voice" -> {
                savedVoice = null
                _voice.value = null
            }
            "voice_clone" -> {
                savedVoiceClone = null
                _voiceClone.value = null
            }
            ImageGenerationModelConfig.Id -> {
                savedImageGeneration = null
                _imageGeneration.value = null
            }
        }
    }
}

private fun modelConfig(
    baseUrl: String,
    apiKey: String,
    modelName: String,
) = ModelConfig(
    id = DefaultModelConfig.Id,
    provider = DefaultModelConfig.Provider,
    name = DefaultModelConfig.Name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    modelName = modelName,
    contextWindow = DefaultModelConfig.ContextWindow,
    maxOutputTokens = DefaultModelConfig.MaxOutputTokens,
    supportVision = false,
    supportToolCall = false,
    supportReasoning = false,
    temperature = DefaultModelConfig.Temperature,
    topP = DefaultModelConfig.TopP,
    enabled = true,
)
