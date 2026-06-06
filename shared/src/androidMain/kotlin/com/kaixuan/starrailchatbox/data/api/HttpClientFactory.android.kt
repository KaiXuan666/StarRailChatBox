package com.kaixuan.starrailchatbox.data.api

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpClient(): HttpClient {
    AndroidHttpLogger.initialize()
    return HttpClient(OkHttp) {
        configureOpenAiClient()
    }
}

private object AndroidHttpLogger {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        Napier.base(DebugAntilog())
        initialized = true
    }
}
