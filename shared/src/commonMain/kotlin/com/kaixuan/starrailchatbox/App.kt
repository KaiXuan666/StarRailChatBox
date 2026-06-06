package com.kaixuan.starrailchatbox

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kaixuan.starrailchatbox.design.StarRailTheme
import com.kaixuan.starrailchatbox.ui.main.MainRoute
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel

@Composable
fun App() {
    val chatViewModel = viewModel { ChatViewModel() }
    val state by chatViewModel.uiState.collectAsStateWithLifecycle()

    StarRailTheme(darkThemeOverride = state.darkThemeOverride) {
        MainRoute(
            state = state,
            effects = chatViewModel.effects,
            onAction = chatViewModel::onAction,
        )
    }
}
