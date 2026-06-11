package com.kaixuan.starrailchatbox.data.settings

data class ApiSettingsDefaults(
    val apiHost: String = "",
    val apiKey: String = "",
    val multimodalHost: String = "",
    val multimodalKey: String = "",
    val voiceHost: String = "",
    val voiceKey: String = "",
    val imageHost: String = "",
    val imageKey: String = "",
)

internal fun localApiSettingsDefaults() = ApiSettingsDefaults(
    apiHost = LocalApiSettings.apiHost,
    apiKey = LocalApiSettings.apiKey,
    multimodalHost = LocalApiSettings.multimodalHost,
    multimodalKey = LocalApiSettings.multimodalKey,
    voiceHost = LocalApiSettings.voiceHost,
    voiceKey = LocalApiSettings.voiceKey,
    imageHost = LocalApiSettings.imageHost,
    imageKey = LocalApiSettings.imageKey,
)
