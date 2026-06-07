package com.kaixuan.starrailchatbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.createCharacterStorage
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createApiSettingsStore
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.di.appModule
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import org.koin.dsl.koinApplication

@Composable
fun App(
    apiSettingsStore: ApiSettingsStore = remember { createApiSettingsStore() },
    profileStore: ProfileStore = remember { createProfileStore() },
    characterRepository: CharacterRepository = remember {
        DefaultCharacterRepository(createCharacterStorage())
    },
) {
    val koinApplication = remember(apiSettingsStore, profileStore) {
        koinApplication {
            modules(appModule(apiSettingsStore, profileStore))
        }
    }
    DisposableEffect(koinApplication) {
        onDispose {
            koinApplication.close()
        }
    }

    val mainViewModel = viewModel { MainViewModel() }
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val chatViewModel = viewModel { ChatViewModel(characterRepository) }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()

    val settingsViewModel = viewModel { koinApplication.koin.get<SettingsViewModel>() }
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val profileViewModel = viewModel { koinApplication.koin.get<ProfileViewModel>() }
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    StarRailTheme(darkThemeOverride = mainState.darkThemeOverride) {
        MainRoute(
            mainState = mainState,
            chatState = chatState,
            settingsState = settingsState,
            profileState = profileState,
            mainEffects = mainViewModel.effects,
            chatEffects = chatViewModel.effects,
            settingsEffects = settingsViewModel.effects,
            profileEffects = profileViewModel.effects,
            onMainAction = mainViewModel::onAction,
            onChatAction = chatViewModel::onAction,
            onSettingsAction = settingsViewModel::onAction,
            onProfileAction = profileViewModel::onAction,
        )
    }
}
