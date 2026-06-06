package com.kaixuan.starrailchatbox.ui.main

import com.kaixuan.starrailchatbox.ui.navigation.Route
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MainViewModelTest {

    @Test
    fun initialStateIsCorrect() {
        val viewModel = MainViewModel()
        val state = viewModel.uiState.value

        assertEquals(listOf(Route.ChatSession), state.backStack)
        assertNull(state.darkThemeOverride)
        assertFalse(state.showThemeDialog)
    }

    @Test
    fun navigationSelectedUpdatesBackStack() {
        val viewModel = MainViewModel()

        viewModel.onAction(MainAction.NavigationSelected(Route.Settings))

        assertEquals(listOf(Route.Settings), viewModel.uiState.value.backStack)
    }

    @Test
    fun popBackStackDoesNothingWhenOnlyOneElement() {
        val viewModel = MainViewModel()

        viewModel.onAction(MainAction.PopBackStack)

        assertEquals(listOf(Route.ChatSession), viewModel.uiState.value.backStack)
    }

    @Test
    fun popBackStackRemovesLastElement() {
        val viewModel = MainViewModel()

        // 压入一个
        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.API_SETTINGS))
        assertEquals(2, viewModel.uiState.value.backStack.size)

        // 弹出
        viewModel.onAction(MainAction.PopBackStack)
        assertEquals(listOf(Route.ChatSession), viewModel.uiState.value.backStack)
    }

    @Test
    fun settingsItemClickedTriggersCorrectFlow() {
        val viewModel = MainViewModel()

        // 点击 API 设置
        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.API_SETTINGS))
        assertEquals(listOf(Route.ChatSession, Route.ApiSettings), viewModel.uiState.value.backStack)

        // 点击主题样式
        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.THEME_STYLE))
        assertTrue(viewModel.uiState.value.showThemeDialog)
    }

    @Test
    fun themeDialogConfirmUpdatesThemeAndEmitsEffect() = runTest {
        val viewModel = MainViewModel()

        viewModel.onAction(MainAction.ThemeDialogConfirm(true))

        assertTrue(viewModel.uiState.value.darkThemeOverride == true)
        assertFalse(viewModel.uiState.value.showThemeDialog)

        assertEquals(
            MainEffect.ShowMessage(MainEffectMessage.THEME_CHANGED),
            viewModel.effects.first()
        )
    }

    @Test
    fun themeDialogDismissClosesDialog() {
        val viewModel = MainViewModel()

        viewModel.onAction(MainAction.SettingsItemClicked(MainSettingsItem.THEME_STYLE))
        assertTrue(viewModel.uiState.value.showThemeDialog)

        viewModel.onAction(MainAction.ThemeDialogDismiss)
        assertFalse(viewModel.uiState.value.showThemeDialog)
    }
}
