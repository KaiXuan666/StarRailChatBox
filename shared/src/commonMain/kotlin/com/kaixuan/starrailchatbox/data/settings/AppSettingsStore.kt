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
}

class InMemoryAppSettingsStore(
    initialTheme: Boolean? = null
) : AppSettingsStore {
    private val _darkThemeOverride = MutableStateFlow(initialTheme)
    private val characterUpdateTokens = mutableMapOf<String, String>()
    override val darkThemeOverride: Flow<Boolean?> = _darkThemeOverride.asStateFlow()

    override suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?) {
        _darkThemeOverride.value = darkThemeOverride
    }

    override suspend fun getCharacterUpdateToken(characterKey: String): String? {
        return characterUpdateTokens[characterKey]
    }

    override suspend fun setCharacterUpdateToken(characterKey: String, token: String) {
        characterUpdateTokens[characterKey] = token
    }
}

expect fun createAppSettingsStore(path: String? = null, context: Any? = null): AppSettingsStore
