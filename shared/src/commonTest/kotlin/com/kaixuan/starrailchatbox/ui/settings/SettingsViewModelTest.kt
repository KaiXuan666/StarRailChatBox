package com.kaixuan.starrailchatbox.ui.settings

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.model.DefaultModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsDefaults
import com.kaixuan.starrailchatbox.data.model.MultimodalModelConfig
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Test
    fun initialStateIsCorrect() = runTest {
        val viewModel = createViewModel(scope = this)
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
        val viewModel = createViewModel(modelConfigRepository = repository, scope = this)
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
            scope = this,
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
            scope = this,
        )
        runCurrent()

        assertEquals("https://local.example.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("local-key", viewModel.uiState.value.apiKey)
        assertEquals("model-a", viewModel.uiState.value.selectedModel)
    }

    @Test
    fun hostAndKeyChangesUpdateState() = runTest {
        val viewModel = createViewModel(scope = this)

        viewModel.onAction(SettingsAction.ApiHostChanged("https://api.openai.com/v1"))
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-12345"))

        assertEquals("https://api.openai.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("sk-12345", viewModel.uiState.value.apiKey)
    }

    @Test
    fun fetchModelsUpdatesListAndSelection() = runTest {
        val repository = FakeOpenAiRepository(
            ApiResult.Success(listOf("model-b", "model-a")),
        )
        val viewModel = createViewModel(repository = repository, scope = this)
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-test"))

        viewModel.onAction(SettingsAction.FetchModelsClicked)
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
        val viewModel = createViewModel(repository = repository, scope = this)
        viewModel.onAction(SettingsAction.ApiKeyChanged("bad-key"))
        val effects = async { viewModel.effects.take(2).toList() }

        viewModel.onAction(SettingsAction.FetchModelsClicked)
        advanceUntilIdle()

        assertEquals(
            listOf(
                SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_FETCH_START),
                SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_AUTH_FAILED),
            ),
            effects.await(),
        )
        assertFalse(viewModel.uiState.value.isFetchingModels)
    }

    @Test
    fun saveApiSettingsPersistsValuesAndEmitsSavedEffect() = runTest {
        val repository = FakeModelConfigRepository()
        val viewModel = createViewModel(modelConfigRepository = repository, scope = this)
        viewModel.onAction(SettingsAction.ApiKeyChanged(" sk-test "))
        viewModel.onAction(SettingsAction.SelectModel("model-a"))
        val effect = async { viewModel.effects.first { it is SettingsEffect.ApiSettingsSaved } }

        viewModel.onAction(SettingsAction.SaveApiSettingsClicked)
        advanceUntilIdle()

        assertEquals(
            modelConfig(
                baseUrl = "https://api.openai.com/v1",
                apiKey = "sk-test",
                modelName = "model-a",
            ),
            repository.saved,
        )
        assertEquals(SettingsEffect.ApiSettingsSaved, effect.await())
        assertFalse(viewModel.uiState.value.isSaving)
    }

    @Test
    fun saveMultimodalApiSettingsPersistsValuesAndSetsSupportVisionTrue() = runTest {
        val repository = FakeModelConfigRepository()
        val viewModel = createViewModel(modelConfigRepository = repository, scope = this)
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-multimodal-test", isMultimodal = true))
        viewModel.onAction(SettingsAction.SelectModel("model-omni", isMultimodal = true))
        val effect = async { viewModel.effects.first { it is SettingsEffect.ApiSettingsSaved } }

        viewModel.onAction(SettingsAction.SaveMultimodalApiSettingsClicked)
        advanceUntilIdle()

        val saved = requireNotNull(repository.savedMultimodal)
        assertEquals(MultimodalModelConfig.Id, saved.id)
        assertEquals("https://api.openai.com/v1", saved.baseUrl)
        assertEquals("sk-multimodal-test", saved.apiKey)
        assertEquals("model-omni", saved.modelName)
        assertTrue(saved.supportVision)
        assertEquals(SettingsEffect.ApiSettingsSaved, effect.await())
        assertFalse(viewModel.uiState.value.multimodalIsSaving)
    }

    @Test
    fun toggleKeyVisibilityWorks() = runTest {
        val viewModel = createViewModel(scope = this)
        assertFalse(viewModel.uiState.value.showApiKey)

        viewModel.onAction(SettingsAction.ToggleApiKeyVisibility)
        assertTrue(viewModel.uiState.value.showApiKey)

        viewModel.onAction(SettingsAction.ToggleApiKeyVisibility)
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
            modelConfigRepository = modelConfigRepository,
            scope = this
        )
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-test"))
        viewModel.onAction(SettingsAction.SelectModel("model-a"))

        viewModel.onAction(SettingsAction.SaveApiSettingsClicked)
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
            modelConfigRepository = modelConfigRepository,
            scope = this
        )
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-test"))
        viewModel.onAction(SettingsAction.SelectModel("model-a"))

        viewModel.onAction(SettingsAction.SaveApiSettingsClicked)
        advanceUntilIdle()

        val saved = requireNotNull(modelConfigRepository.saved)
        assertFalse(saved.supportToolCall)
        assertEquals("https://api.openai.com/v1", openAiRepository.lastHost)
        assertEquals("sk-test", openAiRepository.lastKey)
        assertEquals("model-a", openAiRepository.lastModelTested)
    }


    private fun createViewModel(
        repository: AiRepository = FakeOpenAiRepository(ApiResult.Success(emptyList())),
        modelConfigRepository: ModelConfigRepository = FakeModelConfigRepository(),
        defaults: ApiSettingsDefaults = ApiSettingsDefaults(),
        scope: kotlinx.coroutines.CoroutineScope,
    ) = SettingsViewModel(
        aiRepository = repository,
        modelConfigRepository = modelConfigRepository,
        coroutineScope = scope,
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
}

private class FakeModelConfigRepository(
    private val initial: ModelConfig? = null,
    private val initialMultimodal: ModelConfig? = null,
    private val initialVoice: ModelConfig? = null,
) : ModelConfigRepository {
    var saved: ModelConfig? = null
    var savedMultimodal: ModelConfig? = null
    var savedVoice: ModelConfig? = null

    override suspend fun getDefault(): ModelConfig? = initial

    override suspend fun saveDefault(config: ModelConfig) {
        saved = config
    }

    override suspend fun getMultimodal(): ModelConfig? = initialMultimodal

    override suspend fun saveMultimodal(config: ModelConfig) {
        savedMultimodal = config
    }

    override suspend fun getVoice(): ModelConfig? = initialVoice

    override suspend fun saveVoice(config: ModelConfig) {
        savedVoice = config
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
