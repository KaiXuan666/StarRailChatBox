package com.kaixuan.starrailchatbox.data.settings

data class ApiSettingsDefaults(
    val apiHost: String = "",
    val apiKey: String = "",
)

internal fun localApiSettingsDefaults() = ApiSettingsDefaults(
    apiHost = LocalApiSettings.apiHost,
    apiKey = LocalApiSettings.apiKey,
)
