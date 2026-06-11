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
import com.kaixuan.starrailchatbox.data.database.DatabaseManager
import com.kaixuan.starrailchatbox.data.database.InMemoryDatabaseManager
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.chat.InMemoryChatSessionRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.createAppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.createProfileStore
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.di.appModule
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.main.MainRouteBinding
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import org.koin.dsl.koinApplication

@Composable
fun App(
    modelConfigRepository: ModelConfigRepository = remember { InMemoryModelConfigRepository() },
    profileStore: ProfileStore = remember { createProfileStore() },
    appSettingsStore: AppSettingsStore = remember { createAppSettingsStore() },
    characterRepository: CharacterRepository = remember {
        DefaultCharacterRepository(InMemoryCharacterStorage())
    },
    chatSessionRepository: ChatSessionRepository = remember {
        InMemoryChatSessionRepository()
    },
    databaseManager: DatabaseManager = remember { InMemoryDatabaseManager() },
) {
    val koinApplication = remember(
        modelConfigRepository,
        profileStore,
        appSettingsStore,
        characterRepository,
        chatSessionRepository,
        databaseManager,
    ) {
        koinApplication {
            modules(
                appModule(
                    modelConfigRepository,
                    profileStore,
                    appSettingsStore,
                    characterRepository,
                    chatSessionRepository,
                    databaseManager,
                ),
            )
        }
    }
    DisposableEffect(koinApplication) {
        onDispose {
            koinApplication.close()
        }
    }

    val mainViewModel = viewModel { koinApplication.koin.get<MainViewModel>() }
    val mainState by mainViewModel.uiState.collectAsStateWithLifecycle()

    StarRailTheme(darkThemeOverride = mainState.darkThemeOverride) {
        MainRoute(
            main = MainRouteBinding(
                state = mainState,
                effects = mainViewModel.effects,
                onAction = mainViewModel::onAction,
            ),
            koin = koinApplication.koin,
        )
    }
}
