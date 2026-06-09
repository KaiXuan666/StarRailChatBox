package com.kaixuan.starrailchatbox

import androidx.compose.ui.window.ComposeUIViewController
import com.kaixuan.starrailchatbox.data.database.createPersistentRepositories

fun MainViewController(): platform.UIKit.UIViewController {
    val repositories = createPersistentRepositories()
    return ComposeUIViewController {
        App(
            modelConfigRepository = repositories.modelConfigRepository,
            profileStore = repositories.profileStore,
            appSettingsStore = repositories.appSettingsStore,
            characterRepository = repositories.characterRepository,
            chatSessionRepository = repositories.chatSessionRepository,
        )
    }
}
