package com.kaixuan.starrailchatbox

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import com.kaixuan.starrailchatbox.data.database.createPersistentRepositories

fun main() {
    application {
        val repositories = createPersistentRepositories()
        Window(
            onCloseRequest = ::exitApplication,
            title = "崩铁ChatBox",
            icon = painterResource("app-icon.png"),
            state = rememberWindowState(
                width = 520.dp,
                height = 900.dp,
            ),
        ) {
            App(
                modelConfigRepository = repositories.modelConfigRepository,
                profileStore = repositories.profileStore,
                appSettingsStore = repositories.appSettingsStore,
                characterRepository = repositories.characterRepository,
                chatSessionRepository = repositories.chatSessionRepository,
            )
        }
    }
}
