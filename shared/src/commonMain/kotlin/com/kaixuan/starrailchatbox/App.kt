package com.kaixuan.starrailchatbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createApiSettingsStore
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.di.appModule
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import org.koin.dsl.koinApplication

@Composable
fun App(
    apiSettingsStore: ApiSettingsStore = remember { createApiSettingsStore() },
) {
    val koinApplication = remember(apiSettingsStore) {
        koinApplication {
            modules(appModule(apiSettingsStore))
        }
    }
    DisposableEffect(koinApplication) {
        onDispose {
            koinApplication.close()
        }
    }

    val mainViewModel = viewModel { MainViewModel() }
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val chatViewModel = viewModel { ChatViewModel() }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel = viewModel { koinApplication.koin.get<SettingsViewModel>() }
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
