package com.kaixuan.starrailchatbox.data.database

import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.ProfileStore

data class PersistentRepositories(
    val modelConfigRepository: ModelConfigRepository,
    val characterRepository: CharacterRepository,
    val chatSessionRepository: ChatSessionRepository,
    val profileStore: ProfileStore,
    val appSettingsStore: AppSettingsStore,
)
