package com.kaixuan.starrailchatbox.data.ai.tool

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiToolCall
import com.kaixuan.starrailchatbox.data.settings.InMemoryProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BochaSearchToolTest {
    private val json = Json { ignoreUnknownKeys = true }
    private val context = ToolContext("三月七")
    private val profileStore = InMemoryProfileStore(UserProfile(enableWebSearch = true))
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private fun createMockClient(responseContent: String): HttpClient {
        val engine = MockEngine { _ ->
            respond(
                content = responseContent,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
            )
        }
        return HttpClient(engine)
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun parsesExecutableToolOutput() = testScope.runTest {
        val mockResponse = """
            {
              "code": 200,
              "data": {
                "webPages": {
                  "value": [
                    {
                      "name": "上海天气",
                      "url": "https://weather.com",
                      "summary": "上海今天晴转多云。"
                    }
                  ]
                }
              }
            }
        """.trimIndent()
        
        val tool = BochaSearchTool(profileStore, createMockClient(mockResponse), json, testScope)
        advanceUntilIdle()
        
        val result = tool.execute(
            AiToolCall(
                id = "call-1",
                name = BochaSearchTool.Name,
                arguments = """{"query": "上海天气"}"""
            ),
            context
        )

        val continueResult = assertIs<ToolResult.Continue>(result)
        assertTrue(continueResult.content.contains("上海天气"))
        assertTrue(continueResult.content.contains("上海今天晴转多云"))
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun fallbackParsingWorks() = testScope.runTest {
        val mockResponse = """
            {
              "data": {
                "webPages": {
                  "value": [
                    {
                      "name": "搜索结果1",
                      "url": "https://example.com",
                      "summary": "这是搜索结果的摘要。"
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val tool = BochaSearchTool(profileStore, createMockClient(mockResponse), json, testScope)
        advanceUntilIdle()

        val content = """
            让我帮你查一下。
            <search>{"query":"测试查询"}</search>
        """.trimIndent()

        val parsed = requireNotNull(tool.parseFallback(content, context))
        assertTrue(parsed.content.contains("让我帮你查一下"))
        assertTrue(parsed.content.contains("[联网搜索结果]"))
        assertTrue(parsed.content.contains("搜索结果1"))
    }

    @Test
    fun prepareFallbackMessagesInjectsInstructions() {
        val tool = BochaSearchTool(profileStore, HttpClient(MockEngine { respond("") }), json, testScope)
        val messages = tool.prepareFallbackMessages(
            listOf(AiMessage(role = "user", content = "你好")),
            context
        )

        assertTrue(messages.any { it.role == "system" && it.content?.contains("<search_output_contract>") == true })
        assertTrue(messages.any { it.role == "user" && it.content?.contains("<search>") == true })
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun isAvailableFollowsProfileStore() = testScope.runTest {
        val store = InMemoryProfileStore(UserProfile(enableWebSearch = false))
        val tool = BochaSearchTool(store, HttpClient(MockEngine { respond("") }), json, testScope)
        
        advanceUntilIdle()
        assertFalse(tool.isAvailable())

        store.save(UserProfile(enableWebSearch = true))
        advanceUntilIdle()
        assertTrue(tool.isAvailable())
    }
}
