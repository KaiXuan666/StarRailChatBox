package com.kaixuan.starrailchatbox

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.ui.unit.dp

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "StarRailChatBox",
        state = rememberWindowState(
            width = 520.dp,
            height = 900.dp,
        ),
    ) {
        App()
    }
}
