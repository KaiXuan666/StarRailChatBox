package com.kaixuan.starrailchatbox.ui.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest {

    @Test
    fun initialStateIsCorrect() {
        val viewModel = SettingsViewModel()
        val state = viewModel.uiState.value

        assertEquals("https://api.example.com/v1", state.apiHost)
        assertEquals("", state.apiKey)
        assertFalse(state.showApiKey)
        assertEquals("gpt-4o-mini", state.selectedModel)
        assertFalse(state.isFetchingModels)
    }

    @Test
    fun hostAndKeyChangesUpdateState() {
        val viewModel = SettingsViewModel()

        viewModel.onAction(SettingsAction.ApiHostChanged("https://api.openai.com/v1"))
        viewModel.onAction(SettingsAction.ApiKeyChanged("sk-12345"))

        assertEquals("https://api.openai.com/v1", viewModel.uiState.value.apiHost)
        assertEquals("sk-12345", viewModel.uiState.value.apiKey)
    }

    @Test
    fun toggleKeyVisibilityWorks() {
        val viewModel = SettingsViewModel()
        assertFalse(viewModel.uiState.value.showApiKey)

        viewModel.onAction(SettingsAction.ToggleApiKeyVisibility)
        assertTrue(viewModel.uiState.value.showApiKey)

        viewModel.onAction(SettingsAction.ToggleApiKeyVisibility)
        assertFalse(viewModel.uiState.value.showApiKey)
    }

    @Test
    fun clickSettingsItemEmitsEffect() = runTest {
        val viewModel = SettingsViewModel()

        viewModel.onAction(SettingsAction.SettingsItemClicked(SettingsItem.CHECK_UPDATE))

        assertEquals(
            SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_UPDATE_CHECK),
            viewModel.effects.first()
        )
    }

    @Test
    fun saveApiSettingsEmitsEffect() = runTest {
        val viewModel = SettingsViewModel()

        viewModel.onAction(SettingsAction.SaveApiSettingsClicked)

        assertEquals(
            SettingsEffect.ShowMessage(SettingsEffectMessage.SETTINGS_API_SAVED),
            viewModel.effects.first()
        )
    }
}
