package com.kaixuan.starrailchatbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.InMemoryCharacterStorage
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.chat.InMemoryChatSessionRepository
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.di.appModule
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.main.ChatRouteBinding
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.main.MainRouteBinding
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import com.kaixuan.starrailchatbox.ui.main.CharactersRouteBinding
import com.kaixuan.starrailchatbox.ui.main.SettingsRouteBinding
import com.kaixuan.starrailchatbox.ui.main.ProfileRouteBinding
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import org.koin.dsl.koinApplication

@Composable
fun App(
    modelConfigRepository: ModelConfigRepository = remember { InMemoryModelConfigRepository() },
    profileStore: ProfileStore = remember { createProfileStore() },
    characterRepository: CharacterRepository = remember {
        DefaultCharacterRepository(InMemoryCharacterStorage())
    },
    chatSessionRepository: ChatSessionRepository = remember {
        InMemoryChatSessionRepository()
    },
) {
    val koinApplication = remember(
        modelConfigRepository,
        profileStore,
        characterRepository,
        chatSessionRepository,
    ) {
        koinApplication {
            modules(
                appModule(
                    modelConfigRepository,
                    profileStore,
                    characterRepository,
                    chatSessionRepository,
                ),
            )
        }
    }
    DisposableEffect(koinApplication) {
        onDispose {
            koinApplication.close()
        }
    }

    val mainViewModel = viewModel { MainViewModel() }
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    val chatViewModel = viewModel { koinApplication.koin.get<ChatViewModel>() }
    val chatState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val characterState by chatViewModel.characterUiState.collectAsStateWithLifecycle()

    val settingsViewModel = viewModel { koinApplication.koin.get<SettingsViewModel>() }
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val profileViewModel = viewModel { koinApplication.koin.get<ProfileViewModel>() }
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    StarRailTheme(darkThemeOverride = mainState.darkThemeOverride) {
        MainRoute(
            main = MainRouteBinding(
                state = mainState,
                effects = mainViewModel.effects,
                onAction = mainViewModel::onAction,
            ),
            characters = CharactersRouteBinding(
                state = characterState,
                effects = chatViewModel.characterEffects,
                onAction = chatViewModel::onCharacterAction,
            ),
            chat = ChatRouteBinding(
                state = chatState,
                effects = chatViewModel.effects,
                onAction = chatViewModel::onAction,
            ),
            settings = SettingsRouteBinding(
                state = settingsState,
                effects = settingsViewModel.effects,
                onAction = settingsViewModel::onAction,
            ),
            profile = ProfileRouteBinding(
                state = profileState,
                effects = profileViewModel.effects,
                onAction = profileViewModel::onAction,
            ),
        )
    }
}
