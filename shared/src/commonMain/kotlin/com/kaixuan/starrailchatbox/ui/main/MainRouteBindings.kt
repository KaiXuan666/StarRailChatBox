package com.kaixuan.starrailchatbox.ui.main

import com.kaixuan.starrailchatbox.ui.chat.ChatAction
import com.kaixuan.starrailchatbox.ui.chat.ChatEffect
import com.kaixuan.starrailchatbox.ui.chat.ChatUiState
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharacterEffect
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.settings.SettingsAction
import com.kaixuan.starrailchatbox.ui.settings.SettingsEffect
import com.kaixuan.starrailchatbox.ui.settings.SettingsUiState
import com.kaixuan.starrailchatbox.ui.profile.ProfileAction
import com.kaixuan.starrailchatbox.ui.profile.ProfileEffect
import com.kaixuan.starrailchatbox.ui.profile.ProfileUiState
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

data class ChatRouteBinding(
    val state: ChatUiState,
    val effects: Flow<ChatEffect>,
    val onAction: (ChatAction) -> Unit,
)

data class SettingsRouteBinding(
    val state: SettingsUiState,
    val effects: Flow<SettingsEffect>,
    val onAction: (SettingsAction) -> Unit,
)

data class ProfileRouteBinding(
    val state: ProfileUiState,
    val effects: Flow<ProfileEffect>,
    val onAction: (ProfileAction) -> Unit,
)
