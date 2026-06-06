package com.kaixuan.starrailchatbox.data.api

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createPlatformHttpClient(): HttpClient = HttpClient(CIO) {
    configureOpenAiClient()
}
