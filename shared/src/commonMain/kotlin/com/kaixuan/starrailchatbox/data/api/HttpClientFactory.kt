package com.kaixuan.starrailchatbox.data.api

import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

expect fun createPlatformHttpClient(): HttpClient
expect fun saveNetworkLog(message: String)

internal fun HttpClientConfig<*>.configureOpenAiClient() {
    expectSuccess = true

    install(HttpTimeout) {
        requestTimeoutMillis = 60_000
        connectTimeoutMillis = 15_000
        socketTimeoutMillis = 60_000
    }
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                isLenient = true
                explicitNulls = false
            },
        )
    }
    install(Logging) {
        logger = object : Logger {
            override fun log(message: String) {
                val maxLogLength = 1000
                if (message.length <= maxLogLength) {
                    Napier.d(message, tag = "OpenAiHttp")
                } else {
                    var start = 0
                    while (start < message.length) {
                        val end = if (start + maxLogLength < message.length) start + maxLogLength else message.length
                        Napier.d(message.substring(start, end), tag = "OpenAiHttp")
                        start = end
                    }
                }
                saveNetworkLog(message)
            }
        }
        level = LogLevel.BODY
        sanitizeHeader { header -> header == HttpHeaders.Authorization }
    }
}
