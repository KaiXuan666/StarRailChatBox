package com.kaixuan.starrailchatbox.ui.chat

import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
    fun initialStateIsReadyToRender() = runTest {
        val state = createViewModel().also { runCurrent() }.uiState.value

        assertEquals("流萤", state.selectedCharacter?.name)
        assertEquals(2, state.characters.size)
        assertEquals(5, state.messages.size)
        assertEquals("", state.messageDraft)
        assertFalse(state.isSending)
        assertFalse(state.isLoadingCharacters)
    }

    @Test
    fun characterSelectionUpdatesState() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onAction(ChatAction.CharacterSelected("黄泉"))

        assertEquals("黄泉", viewModel.uiState.value.selectedCharacter?.name)
    }

    @Test
    fun quickReplyPopulatesComposer() {
        val viewModel = createViewModel()

        viewModel.onAction(ChatAction.QuickReplyClicked("聊聊今天"))

        assertEquals("聊聊今天", viewModel.uiState.value.messageDraft)
    }

    @Test
    fun sendAddsMessageAndClearsComposer() {
        val viewModel = createViewModel()
        viewModel.onAction(ChatAction.MessageChanged("  今天好多了  "))

        viewModel.onAction(ChatAction.SendClicked)

        val state = viewModel.uiState.value
        val sentMessage = assertIs<ChatMessageUiModel.Sent>(state.messages.last())
        assertEquals(MessageContent.Custom("今天好多了"), sentMessage.content)
        assertEquals("", state.messageDraft)
        assertTrue(sentMessage.isRead)
    }

    @Test
    fun emptyMessageIsIgnored() {
        val viewModel = createViewModel()
        val initialCount = viewModel.uiState.value.messages.size
        viewModel.onAction(ChatAction.MessageChanged("   "))

        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(initialCount, viewModel.uiState.value.messages.size)
    }

    private fun createViewModel() = ChatViewModel(
        characterRepository = FakeCharacterRepository,
    )
}

private object FakeCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(
        Character("流萤", "流萤", "prompt", byteArrayOf()),
        Character("黄泉", "黄泉", "prompt", byteArrayOf()),
    )

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarBytes: ByteArray,
    ): Character = Character(name, name, prompt, avatarBytes)
}
