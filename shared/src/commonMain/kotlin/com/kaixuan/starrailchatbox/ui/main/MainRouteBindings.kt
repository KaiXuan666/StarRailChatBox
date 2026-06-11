package com.kaixuan.starrailchatbox.ui.main

import com.kaixuan.starrailchatbox.ui.chat.ChatAction
import com.kaixuan.starrailchatbox.ui.chat.ChatEffect
import com.kaixuan.starrailchatbox.ui.chat.ChatUiState
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharacterEffect
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.character.ChatCharactersUiState
import com.kaixuan.starrailchatbox.ui.settings.SettingsOverviewUiState
import kotlinx.coroutines.flow.Flow

data class MainRouteBinding(
    val state: MainUiState,
    val effects: Flow<MainEffect>,
    val onAction: (MainAction) -> Unit,
)

data class CharactersRouteBinding(
    val state: CharactersUiState,
    val effects: Flow<CharacterEffect>,
    val onAction: (CharacterAction) -> Unit,
)

data class ChatCharactersRouteBinding(
    val state: ChatCharactersUiState,
    val effects: Flow<CharacterEffect>,
    val onAction: (CharacterAction) -> Unit,
)

data class ChatRouteBinding(
    val state: ChatUiState,
    val effects: Flow<ChatEffect>,
    val onAction: (ChatAction) -> Unit,
)

data class SettingsRouteBinding(
    val state: SettingsOverviewUiState,
)
