package com.kaixuan.starrailchatbox.data.api

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createPlatformHttpClient(): HttpClient {
    JvmHttpLogger.initialize()
    return HttpClient(CIO) {
        configureOpenAiClient()
    }
}

actual fun saveNetworkLog(message: String) {
    // No-op for JVM
}

private object JvmHttpLogger {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        Napier.base(DebugAntilog())
        initialized = true
    }
}
