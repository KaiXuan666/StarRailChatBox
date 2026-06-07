package com.kaixuan.starrailchatbox.data.database

import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository

data class PersistentRepositories(
    val modelConfigRepository: ModelConfigRepository,
    val characterRepository: CharacterRepository,
)
