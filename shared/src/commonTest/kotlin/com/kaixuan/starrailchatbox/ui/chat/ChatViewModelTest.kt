package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun characterEditSavesAndRefreshesSelectedCharacter() = runTest {
        val characterRepository = EditableCharacterRepository()
        val fixture = createFixture(characterRepository = characterRepository)
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened("builtin:流萤"))
        advanceUntilIdle()
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterNameChanged("  新三月七  "))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptChanged("updated prompt"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterOpeningMessageChanged("updated opening"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterAvatarChanged(CharacterAvatarSource("picked://updated-avatar")))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterTemperatureChanged(1.2))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterTopPChanged(0.6))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterSaveClicked)
        advanceUntilIdle()

        val state = fixture.viewModel.uiState.value
        val selected = requireNotNull(state.selectedCharacter)
        assertEquals("builtin:流萤", selected.id)
        assertEquals("新三月七", selected.name)
        assertEquals("updated prompt", selected.prompt)
        assertEquals("updated opening", selected.openingMessage)
        assertEquals("picked://updated-avatar", selected.avatarUri)
        assertEquals(1.2, selected.temperature)
        assertEquals(0.6, selected.topP)
        assertEquals(CharacterEffect.CharacterSaved, fixture.viewModel.characterEffects.first())
    }

    @Test
    fun characterEditWithoutNewAvatarPreservesExistingAvatarUri() = runTest {
        val characterRepository = EditableCharacterRepository()
        val fixture = createFixture(characterRepository = characterRepository)
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened("builtin:流萤"))
        advanceUntilIdle()
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterNameChanged("流萤新版"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterSaveClicked)
        advanceUntilIdle()

        val selected = requireNotNull(fixture.viewModel.uiState.value.selectedCharacter)
        assertEquals("avatar://firefly", selected.avatarUri)
    }

    @Test
    fun characterDeleteRemovesCharacterAndSelectsFallback() = runTest {
        val characterRepository = EditableCharacterRepository(
            initialCharacters = listOf(
                Character("builtin:流萤", "流萤", "role prompt", "", "avatar://firefly"),
                Character("builtin:黄泉", "黄泉", "another prompt", "", "avatar://acheron"),
            ),
        )
        val fixture = createFixture(characterRepository = characterRepository)
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterDeleteClicked("builtin:流萤"))
        advanceUntilIdle()

        val charState = fixture.viewModel.characterUiState.value
        val chatState = fixture.viewModel.uiState.value
        assertEquals(listOf("builtin:黄泉"), charState.characters.map(Character::id))
        assertEquals("builtin:黄泉", chatState.selectedCharacterId)
        assertEquals(CharacterEffect.CharacterDeleted, fixture.viewModel.characterEffects.first())
    }

    @Test
    fun promptGenWithoutNameEmitsError() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened(null))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenClicked("请帮我设计一个流萤的提示词"))
        advanceUntilIdle()

        val charState = fixture.viewModel.characterUiState.value
        assertFalse(charState.characterEdit.isPromptGenDialogOpen)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_NAME_REQUIRED),
            fixture.viewModel.characterEffects.first(),
        )
    }

    @Test
    fun promptGenWithNameOpensDialog() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened(null))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterNameChanged("流萤"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenClicked("请帮我设计一个流萤的提示词"))
        advanceUntilIdle()

        val charState = fixture.viewModel.characterUiState.value
        assertEquals(true, charState.characterEdit.isPromptGenDialogOpen)
        assertEquals("请帮我设计一个流萤的提示词", charState.characterEdit.promptGenInputText)
    }

    @Test
    fun promptGenConfirmTriggersApiAndUpdatesPrompt() = runTest {
        val fixture = createFixture()
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened(null))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterNameChanged("流萤"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenClicked("请帮我设计一个流萤的提示词"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenConfirmClicked)
        advanceUntilIdle()

        val charState = fixture.viewModel.characterUiState.value
        assertEquals(false, charState.characterEdit.isPromptGenDialogOpen)
        assertEquals(false, charState.characterEdit.isGeneratingPrompt)
        assertEquals("你好呀", charState.characterEdit.prompt)

        val lastRequest = fixture.api.requests.last()
        assertEquals(1, lastRequest.size)
        assertEquals("user", lastRequest.first().role)
        assertEquals("请帮我设计一个流萤的提示词", lastRequest.first().content)
    }

    @Test
    fun promptGenConfirmFailureEmitsError() = runTest {
        val api = object : FakeOpenAiRepository() {
            override fun createPromptCompletion(
                config: ModelConfig,
                messages: List<AiMessage>,
            ): Flow<ApiResult<ChatCompletionResult>> {
                return flowOf(ApiResult.NetworkError("Network issue"))
            }
        }

        var id = 0
        val sessions = InMemoryChatSessionRepository()
        val viewModel = ChatViewModel(
            characterRepository = FakeCharacterRepository,
            chatSessionRepository = sessions,
            modelConfigRepository = InMemoryModelConfigRepository(testConfig()),
            aiRepository = api,
            profileStore = FakeProfileStore(),
            currentTimeMillis = { 60_000L },
            idGenerator = { prefix -> "$prefix-${++id}" },
            sessionTitleProvider = { "新对话" },
            timeFormatter = { "local-$it" },
        )

        val fixture = Fixture(viewModel, sessions, api)
        advanceUntilIdle()

        fixture.viewModel.onCharacterAction(CharacterAction.CharacterEditOpened(null))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterNameChanged("流萤"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenClicked("请帮我设计一个流萤的提示词"))
        fixture.viewModel.onCharacterAction(CharacterAction.CharacterPromptGenConfirmClicked)
        advanceUntilIdle()

        val charState = fixture.viewModel.characterUiState.value
        assertEquals(false, charState.characterEdit.isGeneratingPrompt)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.PROMPT_GEN_FAILED),
            fixture.viewModel.characterEffects.first(),
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
            profileStore = FakeProfileStore(),
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
}

private open class FakeOpenAiRepository : AiRepository {
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
    initial: UserProfile? = UserProfile("星空旅人", null)
) : ProfileStore {
    private val _profile = MutableStateFlow(initial)
    override val profile: Flow<UserProfile?> = _profile

    override suspend fun load(): UserProfile? = _profile.value
    override suspend fun save(profile: UserProfile) {
        _profile.value = profile
    }
}

