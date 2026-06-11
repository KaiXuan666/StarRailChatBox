package com.kaixuan.starrailchatbox.data.ai

import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class XiaomiMimoProviderTest {
    @Test
    fun subscriptionHostWinsWhenItReturnsModels() = runTest {
        val requestedHosts = mutableListOf<String>()
        val provider = provider(
            MockEngine { request ->
                requestedHosts += request.url.host
                assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])
                respond(
                    """{"data":[{"id":"mimo-v2"}]}""",
                    headers = jsonHeaders,
                )
            },
        )

        val result = provider.discoverModels(
            config(apiHost = XiaomiMimo.UsageBasedBaseUrl),
        )

        val discovery = assertIs<ApiResult.Success<AiModelDiscovery>>(result).value
        assertEquals(listOf("mimo-v2"), discovery.models)
        assertEquals(XiaomiMimo.SubscriptionBaseUrl, discovery.resolvedApiHost)
        assertEquals(listOf("token-plan-cn.xiaomimimo.com"), requestedHosts)
    }

    @Test
    fun usageHostIsUsedAfterSubscriptionFailure() = runTest {
        val requestedHosts = mutableListOf<String>()
        val provider = provider(
            MockEngine { request ->
                requestedHosts += request.url.host
                if (request.url.host.startsWith("token-plan")) {
                    respond(
                        """{"error":"unavailable"}""",
                        HttpStatusCode.ServiceUnavailable,
                        jsonHeaders,
                    )
                } else {
                    respond("""{"data":[{"id":"mimo-v2"}]}""", headers = jsonHeaders)
                }
            },
        )

        val discovery = assertIs<ApiResult.Success<AiModelDiscovery>>(
            provider.discoverModels(config()),
        ).value

        assertEquals(XiaomiMimo.UsageBasedBaseUrl, discovery.resolvedApiHost)
        assertEquals(
            listOf("token-plan-cn.xiaomimimo.com", "api.xiaomimimo.com"),
            requestedHosts,
        )
    }

    @Test
    fun usageHostIsUsedAfterSubscriptionReturnsEmptyModels() = runTest {
        val provider = provider(
            MockEngine { request ->
                if (request.url.host.startsWith("token-plan")) {
                    respond("""{"data":[]}""", headers = jsonHeaders)
                } else {
                    respond("""{"data":[{"id":"mimo-vl"}]}""", headers = jsonHeaders)
                }
            },
        )

        val discovery = assertIs<ApiResult.Success<AiModelDiscovery>>(
            provider.discoverModels(config()),
        ).value

        assertEquals(listOf("mimo-vl"), discovery.models)
        assertEquals(XiaomiMimo.UsageBasedBaseUrl, discovery.resolvedApiHost)
    }

    @Test
    fun bothFailuresReturnFailureAndKeepConfigurationOutsideProvider() = runTest {
        val provider = provider(
            MockEngine {
                respond(
                    """{"error":"unauthorized"}""",
                    HttpStatusCode.Unauthorized,
                    jsonHeaders,
                )
            },
        )

        val result = provider.discoverModels(config(apiHost = "https://existing.example/v1"))

        assertIs<ApiResult.HttpError>(result)
    }

    @Test
    fun bothEmptyListsKeepOriginalHost() = runTest {
        val provider = provider(
            MockEngine { respond("""{"data":[]}""", headers = jsonHeaders) },
        )

        val discovery = assertIs<ApiResult.Success<AiModelDiscovery>>(
            provider.discoverModels(config(apiHost = "https://existing.example/v1")),
        ).value

        assertEquals(emptyList(), discovery.models)
        assertEquals("https://existing.example/v1", discovery.resolvedApiHost)
    }

    private fun provider(engine: MockEngine): XiaomiMimoProvider {
        val client = HttpClient(engine) {
            expectSuccess = true
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return XiaomiMimoProvider(OpenAiCompatibleProvider(client))
    }

    private fun config(apiHost: String = XiaomiMimo.SubscriptionBaseUrl) = AiProviderConfig(
        providerId = XiaomiMimoProvider.Id,
        apiHost = apiHost,
        apiKey = "test-key",
        model = "",
    )

    companion object {
        private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
