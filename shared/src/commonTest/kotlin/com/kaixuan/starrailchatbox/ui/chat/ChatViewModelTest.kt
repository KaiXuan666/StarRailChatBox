package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.InMemoryChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun noSessionShowsNonPersistentGreeting() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        val greeting = assertIs<ChatMessageUiModel.Received>(state.messages.single())
        assertEquals(MessageContent.Custom("今天要聊点什么呢？"), greeting.content)
        assertEquals("local-60000", greeting.timestamp)
        assertEquals(null, state.activeSessionId)
        assertFalse(state.isLoadingSession)
    }

    @Test
    fun blankOpeningMessageShowsNoInitialMessage() = runTest {
        val fixture = createFixture(characterRepository = NoOpeningCharacterRepository)
        advanceUntilIdle()

        assertEquals(emptyList(), fixture.viewModel.uiState.value.messages)
    }

    @Test
    fun firstSendCreatesSessionAndStoresBothMessages() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.viewModel.onAction(ChatAction.MessageChanged("  你好  "))
        fixture.viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        val session = requireNotNull(
            fixture.sessions.findLatestSession("builtin:流萤"),
        )
        assertEquals("新对话", session.title)
        assertEquals("role prompt", session.systemPromptSnapshot)
        assertEquals(
            listOf(
                "system" to "role prompt",
                "assistant" to "今天要聊点什么呢？",
                "user" to "你好",
            ),
            fixture.api.requests.single().map { it.role to it.content },
        )
        assertEquals(
            listOf("今天要聊点什么呢？", "你好", "你好呀"),
            fixture.sessions.observeMessages(session.id).first().map { it.content },
        )
        assertEquals("", fixture.viewModel.uiState.value.messageDraft)
        assertFalse(fixture.viewModel.uiState.value.isSending)
    }

    @Test
    fun subsequentSendReusesSessionAndIncludesPreviousConversation() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.send("第一句")
        advanceUntilIdle()
        fixture.send("第二句")
        advanceUntilIdle()

        assertEquals(
            listOf(
                "system" to "role prompt",
                "assistant" to "今天要聊点什么呢？",
                "user" to "第一句",
                "assistant" to "你好呀",
                "user" to "第二句",
            ),
            fixture.api.requests.last().map { it.role to it.content },
        )
    }

    @Test
    fun secondRoundConversationTriggersSessionAutomaticRenaming() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        // 第一轮对话完成
        fixture.send("第一句")
        advanceUntilIdle()

        val sessionAfterFirst = requireNotNull(fixture.sessions.findLatestSession("builtin:流萤"))
        assertEquals("新对话", sessionAfterFirst.title)
        assertEquals(0, fixture.api.titleRequests.size)

        // 第二轮对话完成
        fixture.send("第二句")
        advanceUntilIdle()

        val sessionAfterSecond = requireNotNull(fixture.sessions.findLatestSession("builtin:流萤"))
        // 应该自动被重命名为清洗过后的标题
        assertEquals("总结的标题", sessionAfterSecond.title)
        assertEquals(1, fixture.api.titleRequests.size)

        // 第三轮对话，不应再次触发
        fixture.send("第三句")
        advanceUntilIdle()

        assertEquals(1, fixture.api.titleRequests.size)
    }

    @Test
    fun missingModelConfigKeepsUserMessageAndEmitsEffect() = runTest {
        val fixture = createFixture(config = null)
        advanceUntilIdle()
        fixture.send("不要丢掉")
        advanceUntilIdle()

        val session = requireNotNull(
            fixture.sessions.findLatestSession("builtin:流萤"),
        )
        val stored = fixture.sessions.observeMessages(session.id).first()
        assertEquals(3, stored.size)
        assertEquals("今天要聊点什么呢？", stored.first().content)
        assertEquals("不要丢掉", stored[1].content)
        assertEquals("model_config_required", stored.last().errorCode)
        assertEquals(
            ChatEffect.ShowMessage(EffectMessage.MODEL_CONFIG_REQUIRED),
            fixture.viewModel.effects.first(),
        )
    }

    @Test
    fun conversationManagementCreatesSwitchesAndDeletesSessions() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.send("第一段对话")
        advanceUntilIdle()
        val firstSessionId = requireNotNull(fixture.viewModel.uiState.value.activeSessionId)

        fixture.viewModel.onAction(ChatAction.NewSessionClicked)
        assertNull(fixture.viewModel.uiState.value.activeSessionId)
        fixture.send("第二段对话")
        advanceUntilIdle()
        val secondSessionId = requireNotNull(fixture.viewModel.uiState.value.activeSessionId)
        assertEquals(2, fixture.viewModel.uiState.value.sessions.size)

        fixture.viewModel.onAction(ChatAction.SessionSelected(firstSessionId))
        advanceUntilIdle()
        assertEquals(firstSessionId, fixture.viewModel.uiState.value.activeSessionId)
        assertEquals(
            listOf("今天要聊点什么呢？", "第一段对话", "你好呀"),
            fixture.viewModel.uiState.value.messages.map {
                (it.content as MessageContent.Custom).text
            },
        )

        fixture.viewModel.onAction(ChatAction.SessionDeleteClicked(secondSessionId))
        advanceUntilIdle()
        assertEquals(
            listOf(firstSessionId),
            fixture.viewModel.uiState.value.sessions.map(ConversationSummaryUiModel::id),
        )
    }

    private fun createFixture(
        config: ModelConfig? = testConfig(),
        characterRepository: CharacterRepository = FakeCharacterRepository,
    ): Fixture {
        val sessions = InMemoryChatSessionRepository()
        val api = FakeOpenAiRepository()
        var id = 0
        val viewModel = ChatViewModel(
            characterRepository = characterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = InMemoryModelConfigRepository(config),
            aiRepository = api,
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
            timeFormatter = { "local-$it" },
        )
        return Fixture(viewModel, sessions, api)
    }
}

private data class Fixture(
    val viewModel: ChatViewModel,
    val sessions: InMemoryChatSessionRepository,
    val api: FakeOpenAiRepository,
) {
    fun send(content: String) {
        viewModel.onAction(ChatAction.MessageChanged(content))
        viewModel.onAction(ChatAction.SendClicked)
    }
}

private object FakeCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(
        Character(
            "builtin:流萤",
            "流萤",
            "role prompt",
            "今天要聊点什么呢？",
            byteArrayOf(),
        ),
        Character(
            "builtin:黄泉",
            "黄泉",
            "another prompt",
            "今天要聊点什么呢？",
            byteArrayOf(),
        ),
    )

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarBytes: ByteArray,
    ): Character = Character(name, name, prompt, "", avatarBytes)
}

private object NoOpeningCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(
        Character("builtin:流萤", "流萤", "role prompt", "", byteArrayOf()),
    )

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarBytes: ByteArray,
    ): Character = Character(name, name, prompt, "", avatarBytes)
}

private class FakeOpenAiRepository : AiRepository {
    val requests = mutableListOf<List<AiMessage>>()

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> = ApiResult.Success(emptyList())

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
    ): ApiResult<ChatCompletionResult> {
        requests += messages
        return ApiResult.Success(
            ChatCompletionResult(
                content = "你好呀",
                finishReason = "stop",
                promptTokens = 10,
                completionTokens = 2,
                totalTokens = 12,
            ),
        )
    }

    override suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> = ApiResult.Success(
        ChatCompletionResult(
            content = "summary",
            finishReason = "stop",
            promptTokens = 10,
            completionTokens = 2,
            totalTokens = 12,
        ),
    )

    val titleRequests = mutableListOf<List<AiMessage>>()

    override suspend fun createSessionTitle(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> {
        titleRequests += messages
        return ApiResult.Success(
            ChatCompletionResult(
                content = "「总结的标题」",
                finishReason = "stop",
                promptTokens = 10,
                completionTokens = 2,
                totalTokens = 12,
            ),
        )
    }

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String,
    ): Boolean = false
}

private fun testConfig() = ModelConfig(
    id = "default",
    provider = "custom",
    name = "Test",
    baseUrl = "https://example.com/v1",
    apiKey = "test-key",
    modelName = "test-model",
    contextWindow = 128_000,
    maxOutputTokens = 4_096,
    supportVision = false,
    supportToolCall = true,
    supportReasoning = false,
    temperature = 0.7,
    topP = 1.0,
    enabled = true,
)
