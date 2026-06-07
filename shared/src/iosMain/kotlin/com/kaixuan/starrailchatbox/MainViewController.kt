package com.kaixuan.starrailchatbox

import androidx.compose.ui.window.ComposeUIViewController
import com.kaixuan.starrailchatbox.data.database.createModelConfigRepository

fun MainViewController() = ComposeUIViewController {
    App(modelConfigRepository = createModelConfigRepository())
}
