package com.kaixuan.starrailchatbox.ui.main

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
import com.kaixuan.starrailchatbox.data.update.UpdateRepository
import com.kaixuan.starrailchatbox.data.update.UpdateResponse
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val mockUpdateRepository = object : UpdateRepository {
        override suspend fun checkUpdate(isManual: Boolean): ApiResult<UpdateResponse> {
            return ApiResult.NetworkError("Not implemented in test")
        }
    }

    @Test
    fun initialStateIsCorrect() {
        val viewModel = MainViewModel(InMemoryAppSettingsStore(), mockUpdateRepository)
        val state = viewModel.uiState.value

        assertNull(state.darkThemeOverride)
        assertFalse(state.showThemeDialog)
    }

    @Test
    fun navigationActionsDoNotCreatePageStateInMainViewModel() {
        val viewModel = MainViewModel(InMemoryAppSettingsStore(), mockUpdateRepository)
        val initial = viewModel.uiState.value

        viewModel.onAction(MainAction.NavigationSelected(com.kaixuan.starrailchatbox.ui.navigation.Route.Settings))
        viewModel.onAction(MainAction.NavigateTo(com.kaixuan.starrailchatbox.ui.navigation.Route.ConversationManagement))
        viewModel.onAction(MainAction.PopBackStack)

        assertEquals(initial, viewModel.uiState.value)
    }

    @Test
    fun settingsItemClickedTriggersCorrectFlow() {
        val viewModel = MainViewModel(InMemoryAppSettingsStore(), mockUpdateRepository)

        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.THEME_STYLE))
        assertTrue(viewModel.uiState.value.showThemeDialog)
    }

    @Test
    fun themeDialogConfirmUpdatesThemeAndEmitsEffect() = runTest {
        val viewModel = MainViewModel(InMemoryAppSettingsStore(), mockUpdateRepository)

        viewModel.onAction(MainAction.ThemeDialogConfirm(true))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.darkThemeOverride == true)
        assertFalse(viewModel.uiState.value.showThemeDialog)

        assertEquals(
            MainEffect.ShowMessage(MainEffectMessage.THEME_CHANGED),
            viewModel.effects.first()
        )
    }

    @Test
    fun themeDialogDismissClosesDialog() {
        val viewModel = MainViewModel(InMemoryAppSettingsStore(), mockUpdateRepository)

        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.THEME_STYLE))
        assertTrue(viewModel.uiState.value.showThemeDialog)

        viewModel.onAction(MainAction.ThemeDialogDismiss)
        assertFalse(viewModel.uiState.value.showThemeDialog)
    }
}
