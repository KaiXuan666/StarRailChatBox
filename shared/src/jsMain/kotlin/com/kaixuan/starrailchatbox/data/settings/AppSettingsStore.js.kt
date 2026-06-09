package com.kaixuan.starrailchatbox.data.settings

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

private class JsAppSettingsStore : AppSettingsStore {
    private val _darkThemeOverride = MutableStateFlow<Boolean?>(
        localStorage.getItem("dark_theme_override")?.let {
            if (it == "true") true else if (it == "false") false else null
        }
    )
    override val darkThemeOverride: Flow<Boolean?> = _darkThemeOverride.asStateFlow()

    override suspend fun setDarkThemeOverride(darkThemeOverride: Boolean?) {
        if (darkThemeOverride != null) {
            localStorage.setItem("dark_theme_override", darkThemeOverride.toString())
        } else {
            localStorage.removeItem("dark_theme_override")
        }
        _darkThemeOverride.value = darkThemeOverride
    }
}

actual fun createAppSettingsStore(path: String?, context: Any?): AppSettingsStore = JsAppSettingsStore()
