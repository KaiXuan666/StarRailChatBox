package com.kaixuan.starrailchatbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel

@Composable
fun App() {
    val mainViewModel = viewModel { MainViewModel() }
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val chatViewModel = viewModel { ChatViewModel() }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel = viewModel { SettingsViewModel() }
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    StarRailTheme(darkThemeOverride = mainState.darkThemeOverride) {
        MainRoute(
            mainState = mainState,
            chatState = chatState,
            settingsState = settingsState,
            mainEffects = mainViewModel.effects,
            chatEffects = chatViewModel.effects,
            settingsEffects = settingsViewModel.effects,
            onMainAction = mainViewModel::onAction,
            onChatAction = chatViewModel::onAction,
            onSettingsAction = settingsViewModel::onAction,
        )
    }
}

