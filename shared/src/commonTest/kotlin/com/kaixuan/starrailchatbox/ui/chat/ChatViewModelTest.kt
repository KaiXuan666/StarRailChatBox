package com.kaixuan.starrailchatbox.ui.chat

import androidx.paging.testing.asSnapshot
import com.kaixuan.starrailchatbox.data.ai.AiContentPart
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import kotlin.test.assertTrue
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.InMemoryChatSessionRepository
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.data.settings.UserProfile
import com.kaixuan.starrailchatbox.ui.character.CharacterAction
import com.kaixuan.starrailchatbox.ui.character.CharacterEffect
import com.kaixuan.starrailchatbox.ui.character.CharacterEffectMessage
import com.kaixuan.starrailchatbox.ui.character.CharactersUiState
import com.kaixuan.starrailchatbox.ui.character.CharacterEditUiState
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import com.kaixuan.starrailchatbox.platform.formatMessageTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runCurrent
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
    private var currentViewModel: ChatViewModel? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        val scope = currentViewModel?.viewModelScope
        scope?.cancel()
        java.lang.Thread.sleep(100)
        dispatcher.scheduler.advanceUntilIdle()
        currentViewModel = null
        Dispatchers.resetMain()
    }

    @Test
    fun noSessionShowsNonPersistentGreeting() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        val greeting = assertIs<ChatMessageUiModel.Received>(state.messageSnapshot().single())
        assertEquals(MessageContent.Custom("今天要聊点什么呢？"), greeting.content)
        assertEquals(formatMessageTime(60000L), greeting.timestamp)
        assertEquals(null, state.activeSessionId)
        assertFalse(state.isLoadingSession)
    }

    @Test
    fun blankOpeningMessageShowsNoInitialMessage() = runTest {
        val fixture = createFixture(characterRepository = NoOpeningCharacterRepository)
        advanceUntilIdle()

        assertEquals(emptyList(), fixture.viewModel.uiState.value.messageSnapshot())
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
            fixture.sessions.messageSnapshot(session.id).map { it.content },
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
        val stored = fixture.sessions.messageSnapshot(session.id)
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
            fixture.viewModel.uiState.value.messageSnapshot().map {
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


    @Test
    fun restoreMainCharacterResetsSelectedToLastActiveMainCharacter() = runTest {
        val characterRepository = EditableCharacterRepository(
            initialCharacters = listOf(
                Character("char1", "流萤", "", "", "", sortOrder = 0),
                Character("char2", "三月七", "", "", "", sortOrder = 1),
                Character("char3", "黄泉", "", "", "", sortOrder = 2),
                Character("char4", "姬子", "", "", "", sortOrder = 3),
                Character("char5", "银狼", "", "", "", sortOrder = 4),
            )
        )
        val fixture = createFixture(characterRepository = characterRepository)
        advanceUntilIdle()

        assertEquals("char1", fixture.viewModel.uiState.value.selectedCharacterId)

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterSelected("char2"))
        advanceUntilIdle()
        assertEquals("char2", fixture.viewModel.uiState.value.selectedCharacterId)

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterSelected("char5"))
        advanceUntilIdle()
        assertEquals("char5", fixture.viewModel.uiState.value.selectedCharacterId)

        fixture.viewModel.onAction(ChatAction.RestoreMainCharacter)
        advanceUntilIdle()

        assertEquals("char2", fixture.viewModel.uiState.value.selectedCharacterId)
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
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
        )
        currentViewModel = viewModel
        viewModel.onCharacterAction(com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterSelected("builtin:流萤"))
        return Fixture(viewModel, sessions, api)
    }

    @Test
    fun sendWithImageAttachmentUsesMultimodalConfigAndSendsContentParts() = runTest {
        val multimodalConfig = ModelConfig(
            id = "multimodal",
            provider = "custom",
            name = "Multimodal Test",
            baseUrl = "https://example.com/v1",
            apiKey = "test-key-multimodal",
            modelName = "test-model-multimodal",
            contextWindow = 128_000,
            maxOutputTokens = 4_096,
            supportVision = true,
            supportToolCall = true,
            supportReasoning = false,
            temperature = 0.7,
            topP = 1.0,
            enabled = true,
        )
        val modelConfigRepository = InMemoryModelConfigRepository(testConfig(), multimodalConfig)
        
        val tempImageFile = java.io.File.createTempFile("test_image", ".png")
        tempImageFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        tempImageFile.deleteOnExit()

        val sessions = InMemoryChatSessionRepository()
        val api = FakeOpenAiRepository()
        var id = 0
        val viewModel = ChatViewModel(
            characterRepository = FakeCharacterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = modelConfigRepository,
            aiRepository = api,
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
        )
        currentViewModel = viewModel
        viewModel.onCharacterAction(com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterSelected("builtin:流萤"))
        advanceUntilIdle()

        viewModel.onAction(ChatAction.ImageSelected(tempImageFile.absolutePath, tempImageFile.name, "png"))
        viewModel.onAction(ChatAction.MessageChanged("Describe this image"))
        viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        var attempts = 0
        while (api.requests.isEmpty() && attempts < 50) {
            attempts++
            java.lang.Thread.sleep(20)
            runCurrent()
        }

        assertEquals(1, api.requests.size)
        val sentRequest = api.requests.single()
        val userMsg = sentRequest.last()
        assertEquals("user", userMsg.role)
        assertEquals("Describe this image", userMsg.content)
        
        val parts = requireNotNull(userMsg.contentParts)
        assertEquals(2, parts.size)
        
        val textPart = assertIs<AiContentPart.Text>(parts[0])
        assertEquals("Describe this image", textPart.text)
        
        val imagePart = assertIs<AiContentPart.ImageUrl>(parts[1])
        println("DEBUG IMAGE PART URL: ${imagePart.url}")
        assertTrue(imagePart.url.startsWith("data:image/png;base64,"))
    }

    @Test
    fun sendWithFileAttachmentAppendsContentAndStoresToDatabase() = runTest {
        val tempTextFile = java.io.File.createTempFile("test_file", ".txt")
        tempTextFile.writeText("hello file content")
        tempTextFile.deleteOnExit()

        val sessions = InMemoryChatSessionRepository()
        val api = FakeOpenAiRepository()
        var id = 0
        val viewModel = ChatViewModel(
            characterRepository = FakeCharacterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = InMemoryModelConfigRepository(testConfig()),
            aiRepository = api,
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
            enableFileAppend = true,
        )
        currentViewModel = viewModel
        viewModel.onCharacterAction(com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterSelected("builtin:流萤"))
        advanceUntilIdle()

        viewModel.onAction(ChatAction.FileSelected(tempTextFile.absolutePath, "test.txt", "txt"))
        viewModel.onAction(ChatAction.MessageChanged("Check this file:"))
        viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        var attempts = 0
        while (api.requests.isEmpty() && attempts < 50) {
            attempts++
            java.lang.Thread.sleep(20)
            runCurrent()
        }

        val expectedText = "Check this file:\n\n[File: test.txt]\nhello file content\n[End File]"
        
        val sentRequest = api.requests.single()
        val userMsg = sentRequest.last()
        assertEquals("user", userMsg.role)
        assertEquals(expectedText, userMsg.content)
        assertNull(userMsg.contentParts)

        val session = requireNotNull(sessions.findLatestSession("builtin:流萤"))
        val storedMessages = sessions.messageSnapshot(session.id)
        val userMsgIndex = storedMessages.size - 2
        assertEquals(expectedText, storedMessages[userMsgIndex].content)
    }

    @Test
    fun sendWithFileAttachmentAsMultimodalPart() = runTest {
        val tempTextFile = java.io.File.createTempFile("test_file", ".txt")
        tempTextFile.writeText("hello file content")
        tempTextFile.deleteOnExit()

        val sessions = InMemoryChatSessionRepository()
        val api = FakeOpenAiRepository()
        var id = 0
        val viewModel = ChatViewModel(
            characterRepository = FakeCharacterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = InMemoryModelConfigRepository(testConfig()),
            aiRepository = api,
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
            enableFileAppend = false,
        )
        currentViewModel = viewModel
        viewModel.onCharacterAction(com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterSelected("builtin:流萤"))
        advanceUntilIdle()

        viewModel.onAction(ChatAction.FileSelected(tempTextFile.absolutePath, "test.txt", "txt"))
        viewModel.onAction(ChatAction.MessageChanged("Check this file:"))
        viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        var attempts = 0
        while (api.requests.isEmpty() && attempts < 50) {
            attempts++
            java.lang.Thread.sleep(20)
            runCurrent()
        }

        val sentRequest = api.requests.single()
        val userMsg = sentRequest.last()
        assertEquals("user", userMsg.role)
        assertEquals("Check this file:", userMsg.content)

        val parts = requireNotNull(userMsg.contentParts)
        assertEquals(2, parts.size)

        val textPart = assertIs<AiContentPart.Text>(parts[0])
        assertEquals("Check this file:", textPart.text)

        val filePart = assertIs<AiContentPart.FileUrl>(parts[1])
        assertTrue(filePart.url.startsWith("data:text/plain;base64,"))
        assertEquals("text/plain", filePart.mimeType)
    }

    @Test
    fun sendWithoutAttachmentButWithHistoryAttachmentUsesMultimodalConfig() = runTest {
        val multimodalConfig = ModelConfig(
            id = "multimodal",
            provider = "custom",
            name = "Multimodal Test",
            baseUrl = "https://example.com/v1",
            apiKey = "test-key-multimodal",
            modelName = "test-model-multimodal",
            contextWindow = 128_000,
            maxOutputTokens = 4_096,
            supportVision = true,
            supportToolCall = true,
            supportReasoning = false,
            temperature = 0.7,
            topP = 1.0,
            enabled = true,
        )
        val modelConfigRepository = InMemoryModelConfigRepository(testConfig(), multimodalConfig)
        
        val tempImageFile = java.io.File.createTempFile("test_image", ".png")
        tempImageFile.writeBytes(byteArrayOf(1, 2, 3, 4))
        tempImageFile.deleteOnExit()

        val sessions = InMemoryChatSessionRepository()
        val api = FakeOpenAiRepository()
        var id = 0
        val viewModel = ChatViewModel(
            characterRepository = FakeCharacterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = modelConfigRepository,
            aiRepository = api,
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
        )
        currentViewModel = viewModel
        viewModel.onCharacterAction(com.kaixuan.starrailchatbox.ui.character.CharacterAction.CharacterSelected("builtin:流萤"))
        advanceUntilIdle()

        // 1. 第一轮：带图片附件发送，验证使用的是多模态配置
        viewModel.onAction(ChatAction.ImageSelected(tempImageFile.absolutePath, tempImageFile.name, "png"))
        viewModel.onAction(ChatAction.MessageChanged("Describe this image"))
        viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        var attempts = 0
        while (api.requests.isEmpty() && attempts < 50) {
            attempts++
            java.lang.Thread.sleep(20)
            runCurrent()
        }

        assertEquals(1, api.requests.size)
        assertEquals("multimodal", api.configs.single().id)

        // 2. 第二轮：发送普通纯文本消息（不带附件），验证由于未总结历史消息中包含图片附件，依旧使用多模态配置
        viewModel.onAction(ChatAction.MessageChanged("What color was it?"))
        viewModel.onAction(ChatAction.SendClicked)
        advanceUntilIdle()

        attempts = 0
        while (api.requests.size < 2 && attempts < 50) {
            attempts++
            java.lang.Thread.sleep(20)
            runCurrent()
        }

        assertEquals(2, api.requests.size)
        assertEquals("multimodal", api.configs[1].id)
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

private suspend fun ChatUiState.messageSnapshot(): List<ChatMessageUiModel> {
    return messagePagingData.flow.asSnapshot()
        .mapNotNull { (it as? ChatTimelineItem.Message)?.message }
        .asReversed()
}

private suspend fun InMemoryChatSessionRepository.messageSnapshot(
    sessionId: String,
) = getAllMessagesDirectly()
    .filter { it.sessionId == sessionId }
    .sortedBy { it.seq }

private object FakeCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(
        Character(
            "builtin:流萤",
            "流萤",
            "role prompt",
            "今天要聊点什么呢？",
            "avatar://firefly",
        ),
        Character(
            "builtin:黄泉",
            "黄泉",
            "another prompt",
            "今天要聊点什么呢？",
            "avatar://acheron",
        ),
    )

    override suspend fun getCharacter(id: String): Character? = loadCharacters().firstOrNull { it.id == id }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character = Character(name, name, prompt, "", avatarSource?.uri.orEmpty())

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character = character.copy(avatarUri = avatarSource?.uri ?: character.avatarUri)

    override suspend fun updateSortOrder(id: String, sortOrder: Int) = Unit

    override suspend fun deleteCharacter(id: String, deletedAt: Long) = Unit

    override suspend fun getDefaultCharacter(id: String): Character? = getCharacter(id)
}

private object NoOpeningCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(
        Character("builtin:流萤", "流萤", "role prompt", "", "avatar://firefly"),
    )

    override suspend fun getCharacter(id: String): Character? = loadCharacters().firstOrNull { it.id == id }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character = Character(name, name, prompt, "", avatarSource?.uri.orEmpty())

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character = character.copy(avatarUri = avatarSource?.uri ?: character.avatarUri)

    override suspend fun updateSortOrder(id: String, sortOrder: Int) = Unit

    override suspend fun deleteCharacter(id: String, deletedAt: Long) = Unit

    override suspend fun getDefaultCharacter(id: String): Character? = getCharacter(id)
}

private class EditableCharacterRepository(
    initialCharacters: List<Character> = listOf(
        Character(
            "builtin:流萤",
            "流萤",
            "role prompt",
            "今天要聊点什么呢？",
            "avatar://firefly",
        ),
    ),
) : CharacterRepository {
    private var characters = initialCharacters

    override suspend fun loadCharacters(): List<Character> = characters

    override suspend fun getCharacter(id: String): Character? = characters.firstOrNull { it.id == id }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character = Character(name, name, prompt, "", avatarSource?.uri.orEmpty())

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character {
        val saved = character.copy(avatarUri = avatarSource?.uri ?: character.avatarUri)
        characters = characters.map { if (it.id == saved.id) saved else it }
        return saved
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) {
        characters = characters.map {
            if (it.id == id) it.copy(sortOrder = sortOrder) else it
        }
    }

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        characters = characters.filterNot { it.id == id }
    }

    override suspend fun getDefaultCharacter(id: String): Character? = getCharacter(id)
}

private open class FakeOpenAiRepository : AiRepository {
    val requests = mutableListOf<List<AiMessage>>()
    val configs = mutableListOf<ModelConfig>()

    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> = ApiResult.Success(emptyList())

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
        voiceSampleUri: String?,
    ): ApiResult<ChatCompletionResult> {
        requests += messages
        configs += config
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

    override fun createPromptCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): Flow<ApiResult<ChatCompletionResult>> {
        requests += messages
        return flowOf(
            ApiResult.Success(
                ChatCompletionResult(
                    content = "你好呀",
                    finishReason = "stop",
                    promptTokens = 10,
                    completionTokens = 2,
                    totalTokens = 12,
                ),
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

private class FakeProfileStore(
    initial: UserProfile? = UserProfile("星空旅人", 20)
) : ProfileStore {
    private val _profile = MutableStateFlow(initial)
    override val profile: Flow<UserProfile?> = _profile

    override suspend fun load(): UserProfile? = _profile.value
    override suspend fun save(profile: UserProfile) {
        _profile.value = profile
    }
}
