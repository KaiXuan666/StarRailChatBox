package com.kaixuan.starrailchatbox.data.settings

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class WasmAppSettingsStore : AppSettingsStore {
    private val _darkThemeOverride = MutableStateFlow<Boolean?>(
        localStorage.getItem("dark_theme_override")?.let {
            if (it == "true") true else if (it == "false") false else null
        }
    )
    private val _userNickname = MutableStateFlow(localStorage.getItem("user_nickname") ?: "")
    override val darkThemeOverride: Flow<Boolean?> = _darkThemeOverride.asStateFlow()
    override val userNickname: Flow<String> = _userNickname.asStateFlow()

    override suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?) {
        if (darkThemeOverride != null) {
            localStorage.setItem("dark_theme_override", darkThemeOverride.toString())
        } else {
            localStorage.removeItem("dark_theme_override")
        }
        _darkThemeOverride.value = darkThemeOverride
    }

    override suspend fun getCharacterUpdateToken(characterKey: String): String? {
        return localStorage.getItem("character_update_token_$characterKey")
    }

    override suspend fun setCharacterUpdateToken(characterKey: String, token: String) {
        localStorage.setItem("character_update_token_$characterKey", token)
    }

    override suspend fun setUserNickname(nickname: String) {
        localStorage.setItem("user_nickname", nickname)
        _userNickname.value = nickname
    }
}

actual fun createAppSettingsStore(path: String?, context: Any?): AppSettingsStore = WasmAppSettingsStore()
