package com.kaixuan.starrailchatbox

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.createCharacterStorage
import com.kaixuan.starrailchatbox.data.chat.InMemoryChatSessionRepository

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        App(
            characterRepository = DefaultCharacterRepository(createCharacterStorage()),
            chatSessionRepository = InMemoryChatSessionRepository(),
        )
    }
}
