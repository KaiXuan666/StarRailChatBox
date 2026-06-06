package com.kaixuan.starrailchatbox.data.settings

data class StoredApiSettings(
    val apiHost: String,
    val apiKey: String,
    val selectedModel: String,
)

interface ApiSettingsStore {
    suspend fun load(): StoredApiSettings?

    suspend fun save(settings: StoredApiSettings)
}

class InMemoryApiSettingsStore(
    initial: StoredApiSettings? = null,
) : ApiSettingsStore {
    private var settings = initial

    override suspend fun load(): StoredApiSettings? = settings

    override suspend fun save(settings: StoredApiSettings) {
        this.settings = settings
    }
}

expect fun createApiSettingsStore(path: String? = null): ApiSettingsStore
