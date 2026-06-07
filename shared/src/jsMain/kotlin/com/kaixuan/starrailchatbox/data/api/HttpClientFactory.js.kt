package com.kaixuan.starrailchatbox.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Js) {
    configureOpenAiClient()
}

actual fun saveNetworkLog(message: String) {
    // No-op for JS
}
