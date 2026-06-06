package com.kaixuan.starrailchatbox.ui.settings

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsStore
import com.kaixuan.starrailchatbox.data.settings.StoredApiSettings
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
        val store = FakeApiSettingsStore(
            StoredApiSettings(
                apiHost = "https://example.com/v1",
                apiKey = "stored-key",
                selectedModel = "model-b",
            ),
        )
        val viewModel = createViewModel(store = store, scope = this)
        runCurrent()

        assertEquals("https://example.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("stored-key", viewModel.uiState.value.apiKey)
        assertEquals("model-b", viewModel.uiState.value.selectedModel)
        assertEquals(listOf("model-b"), viewModel.uiState.value.modelsList)
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
        val store = FakeApiSettingsStore()
        val viewModel = createViewModel(store = store, scope = this)
        viewModel.onAction(SettingsAction.ApiKeyChanged(" sk-test "))
        viewModel.onAction(SettingsAction.SelectModel("model-a"))
        val effect = async { viewModel.effects.first { it is SettingsEffect.ApiSettingsSaved } }

        viewModel.onAction(SettingsAction.SaveApiSettingsClicked)
        advanceUntilIdle()

        assertEquals(
            StoredApiSettings(
                apiHost = "https://api.openai.com/v1",
                apiKey = "sk-test",
                selectedModel = "model-a",
            ),
            store.saved,
        )
        assertEquals(SettingsEffect.ApiSettingsSaved, effect.await())
        assertFalse(viewModel.uiState.value.isSaving)
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

    private fun createViewModel(
        repository: OpenAiRepository = FakeOpenAiRepository(ApiResult.Success(emptyList())),
        store: ApiSettingsStore = FakeApiSettingsStore(),
        scope: kotlinx.coroutines.CoroutineScope,
    ) = SettingsViewModel(
        openAiRepository = repository,
        apiSettingsStore = store,
        coroutineScope = scope,
    )
}

private class FakeOpenAiRepository(
    private val result: ApiResult<List<String>>,
) : OpenAiRepository {
    var lastHost: String? = null
    var lastKey: String? = null

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
    ): ApiResult<List<String>> {
        lastHost = apiHost
        lastKey = apiKey
        return result
    }
}

private class FakeApiSettingsStore(
    private val initial: StoredApiSettings? = null,
) : ApiSettingsStore {
    var saved: StoredApiSettings? = null

    override suspend fun load(): StoredApiSettings? = initial

    override suspend fun save(settings: StoredApiSettings) {
        saved = settings
    }
}
