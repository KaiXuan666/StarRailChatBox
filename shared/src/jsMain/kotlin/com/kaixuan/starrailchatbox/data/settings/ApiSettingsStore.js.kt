package com.kaixuan.starrailchatbox.data.settings

actual fun createApiSettingsStore(path: String?): ApiSettingsStore = InMemoryApiSettingsStore()
