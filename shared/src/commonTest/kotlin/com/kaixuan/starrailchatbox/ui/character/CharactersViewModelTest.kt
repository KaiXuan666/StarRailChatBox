package com.kaixuan.starrailchatbox.ui.character

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.sharing.PublicCharacterRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CharactersViewModelTest {
    @Test
    fun exportDialogTracksClickedCharacterAndLocalExportRequestsPicker() = runTest {
        val viewModel = createViewModel()
        runCurrent()

        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))
        assertEquals("role", viewModel.uiState.value.exportDialogCharacterId)

        viewModel.onAction(CharacterAction.CharacterExportLocalClicked)

        assertNull(viewModel.uiState.value.exportDialogCharacterId)
        assertEquals("role", viewModel.uiState.value.pendingLocalExportCharacterId)
        assertEquals(CharacterEffect.RequestDirectoryPicker, viewModel.effects.first())
    }

    @Test
    fun sharingRequiresAuthor() = runTest {
        val viewModel = createViewModel(character = testCharacter(author = ""))
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)

        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_AUTHOR_REQUIRED),
            viewModel.effects.first(),
        )
    }

    @Test
    fun repeatedShareClickStartsOnlyOneUpload() = runTest {
        val sharingRepository = FakePublicCharacterRepository { ApiResult.Success(Unit) }
        val viewModel = createViewModel(publicRepository = sharingRepository)
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))

        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_SUCCESS),
            viewModel.effects.first(),
        )

        assertEquals(1, sharingRepository.shareCount)
        assertNull(viewModel.uiState.value.sharingCharacterId)
        assertNull(viewModel.uiState.value.exportDialogCharacterId)
    }
}

private fun createViewModel(
    character: Character = testCharacter(),
    publicRepository: PublicCharacterRepository = FakePublicCharacterRepository {
        ApiResult.Success(Unit)
    },
) = CharactersViewModel(
    characterRepository = FakeCharacterRepository(character),
    characterCardExporter = object : CharacterCardExporter {
        override suspend fun exportToPng(
            character: Character,
            directory: PlatformFile,
        ): ApiResult<Unit> = ApiResult.Success(Unit)
    },
    publicCharacterRepository = publicRepository,
)

private class FakePublicCharacterRepository(
    private val result: suspend () -> ApiResult<Unit>,
) : PublicCharacterRepository {
    override val isSupported: Boolean = true
    var shareCount: Int = 0

    override suspend fun share(character: Character): ApiResult<Unit> {
        shareCount++
        return result()
    }
}

private class FakeCharacterRepository(
    private val character: Character,
) : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = listOf(character)

    override fun observeCharacterSummaries(): Flow<List<CharacterSummary>> = flowOf(
        listOf(
            CharacterSummary(
                id = character.id,
                name = character.name,
                avatarUri = character.avatarUri,
            ),
        ),
    )

    override suspend fun getCharacter(id: String): Character? = character.takeIf { it.id == id }

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character = error("Not used")

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character = error("Not used")

    override suspend fun updateSortOrder(id: String, sortOrder: Int) = Unit

    override suspend fun deleteCharacter(id: String, deletedAt: Long) = Unit

    override suspend fun getDefaultCharacter(id: String): Character? = null
}

private fun testCharacter(author: String = "author") = Character(
    id = "role",
    name = "Role",
    author = author,
    prompt = "prompt",
    openingMessage = "hello",
    avatarUri = "",
)
