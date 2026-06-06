package com.kaixuan.starrailchatbox.ui.chat

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ChatViewModelTest {
    @Test
    fun initialStateIsReadyToRender() {
        val state = ChatViewModel().uiState.value

        assertEquals(CharacterId.LIU_YING, state.selectedCharacter)
        assertEquals(5, state.messages.size)
        assertEquals("", state.messageDraft)
        assertFalse(state.isSending)
    }

    @Test
    fun characterSelectionUpdatesState() {
        val viewModel = ChatViewModel()

        viewModel.onAction(ChatAction.CharacterSelected(CharacterId.LI_GUANG))

        assertEquals(CharacterId.LI_GUANG, viewModel.uiState.value.selectedCharacter)
    }

    @Test
    fun quickReplyPopulatesComposer() {
        val viewModel = ChatViewModel()

        viewModel.onAction(ChatAction.QuickReplyClicked("聊聊今天"))

        assertEquals("聊聊今天", viewModel.uiState.value.messageDraft)
    }

    @Test
    fun sendAddsMessageAndClearsComposer() {
        val viewModel = ChatViewModel()
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
        val viewModel = ChatViewModel()
        val initialCount = viewModel.uiState.value.messages.size
        viewModel.onAction(ChatAction.MessageChanged("   "))

        viewModel.onAction(ChatAction.SendClicked)

        assertEquals(initialCount, viewModel.uiState.value.messages.size)
    }

}
