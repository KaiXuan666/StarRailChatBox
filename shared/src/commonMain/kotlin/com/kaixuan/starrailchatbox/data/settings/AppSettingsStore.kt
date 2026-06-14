package com.kaixuan.starrailchatbox.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 应用全局设置存储接口 (包含主题风格等)
 */
interface AppSettingsStore {
    val darkThemeOverride: Flow<Boolean?>
    suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?)
    suspend fun getCharacterUpdateToken(characterKey: String): String?
    suspend fun setCharacterUpdateToken(characterKey: String, token: String)
    val userNickname: Flow<String>
    suspend fun setUserNickname(nickname: String)
}

class InMemoryAppSettingsStore(
    initialTheme: Boolean? = null
) : AppSettingsStore {
    private val _darkThemeOverride = MutableStateFlow(initialTheme)
    private val characterUpdateTokens = mutableMapOf<String, String>()
    private val _userNickname = MutableStateFlow("")
    override val darkThemeOverride: Flow<Boolean?> = _darkThemeOverride.asStateFlow()
    override val userNickname: Flow<String> = _userNickname.asStateFlow()

    override suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?) {
        _darkThemeOverride.value = darkThemeOverride
    }

    override suspend fun getCharacterUpdateToken(characterKey: String): String? {
        return characterUpdateTokens[characterKey]
    }

    override suspend fun setCharacterUpdateToken(characterKey: String, token: String) {
        characterUpdateTokens[characterKey] = token
    }

    override suspend fun setUserNickname(nickname: String) {
        _userNickname.value = nickname
    }
}

expect fun createAppSettingsStore(path: String? = null, context: Any? = null): AppSettingsStore
