package com.kaixuan.starrailchatbox.ui.character

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCatalog
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCatalogFetch
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategories
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategory
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterDetail
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterPage
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.sharing.PublicCharacterRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
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
    fun sharingFailureClosesDialog() = runTest {
        val sharingRepository = FakePublicCharacterRepository { ApiResult.NetworkError("connection error") }
        val viewModel = createViewModel(publicRepository = sharingRepository)
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        viewModel.uiState.first { it.shareCategories.isNotEmpty() }
        viewModel.onAction(CharacterAction.CharacterShareCategorySelected("game"))
        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)

        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_FAILED, "网络错误: connection error"),
            viewModel.effects.first(),
        )
        assertNull(viewModel.uiState.value.exportDialogCharacterId)
    }

    @Test
    fun repeatedShareClickStartsOnlyOneUpload() = runTest {
        val sharingRepository = FakePublicCharacterRepository { ApiResult.Success(Unit) }
        val viewModel = createViewModel(publicRepository = sharingRepository)
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))

        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        viewModel.uiState.first { it.shareCategories.isNotEmpty() }
        viewModel.onAction(CharacterAction.CharacterShareCategorySelected("game"))
        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)
        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_SUCCESS),
            viewModel.effects.first(),
        )

        assertEquals(1, sharingRepository.shareCount)
        assertNull(viewModel.uiState.value.sharingCharacterId)
        assertNull(viewModel.uiState.value.exportDialogCharacterId)
    }

    @Test
    fun sharingRequiresNickname() = runTest {
        val appSettingsStore = InMemoryAppSettingsStore()
        val viewModel = createViewModel(appSettingsStore = appSettingsStore)
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)

        assertEquals(
            CharacterEffect.NavigateToProfile,
            viewModel.effects.first(),
        )
        assertNull(viewModel.uiState.value.exportDialogCharacterId)
    }

    @Test
    fun sharingRestrictsBuiltinCharacters() = runTest {
        val viewModel = createViewModel()
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("builtin:role"))
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)

        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_BUILTIN_RESTRICTED),
            viewModel.effects.first(),
        )
        assertNull(viewModel.uiState.value.exportDialogCharacterId)
    }

    @Test
    fun sharingUsesUserNicknameAsAuthor() = runTest {
        val sharingRepository = FakePublicCharacterRepository { ApiResult.Success(Unit) }
        val appSettingsStore = InMemoryAppSettingsStore().apply {
            kotlinx.coroutines.runBlocking { setUserNickname("custom_nickname") }
        }
        val viewModel = createViewModel(
            character = testCharacter(author = "original_author"),
            publicRepository = sharingRepository,
            appSettingsStore = appSettingsStore,
        )
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))
        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        viewModel.uiState.first { it.shareCategories.isNotEmpty() }
        viewModel.onAction(CharacterAction.CharacterShareCategorySelected("game"))
        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)

        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_SUCCESS),
            viewModel.effects.first(),
        )
        assertEquals("custom_nickname", sharingRepository.lastSharedCharacter?.author)
        assertEquals("game", sharingRepository.lastCategoryId)
    }

    @Test
    fun sharingLoadsCategoriesAndWaitsForSelectionBeforeUploading() = runTest {
        val sharingRepository = FakePublicCharacterRepository { ApiResult.Success(Unit) }
        val catalogRepository = FakePublicCharacterCatalogRepository()
        val viewModel = createViewModel(
            publicRepository = sharingRepository,
            catalogRepository = catalogRepository,
        )
        runCurrent()
        viewModel.onAction(CharacterAction.CharacterExportClicked("role"))

        viewModel.onAction(CharacterAction.CharacterSharePublicClicked)
        viewModel.uiState.first { it.shareCategories.isNotEmpty() }

        assertEquals(1, catalogRepository.catalogRequestCount)
        assertEquals(1, catalogRepository.categoriesRequestCount)
        assertEquals("role", viewModel.uiState.value.shareCategoryDialogCharacterId)
        assertEquals(listOf("game", "general"), viewModel.uiState.value.shareCategories.map { it.id })
        assertEquals(listOf("gentle", "healing"), viewModel.uiState.value.shareTags.map { it.id })
        assertEquals(0, sharingRepository.shareCount)

        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)
        runCurrent()
        assertEquals(0, sharingRepository.shareCount)

        viewModel.onAction(CharacterAction.CharacterShareCategorySelected("general"))
        viewModel.onAction(CharacterAction.CharacterShareTagToggled("healing"))
        viewModel.onAction(CharacterAction.CharacterShareTagToggled("gentle"))
        viewModel.onAction(CharacterAction.CharacterShareTagToggled("healing"))
        viewModel.onAction(CharacterAction.CharacterShareCategoryConfirmed)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.CHARACTER_SHARE_SUCCESS),
            viewModel.effects.first(),
        )
        assertEquals(1, sharingRepository.shareCount)
        assertEquals("general", sharingRepository.lastCategoryId)
        assertEquals(listOf("gentle"), sharingRepository.lastTagIds)
    }
}

private fun createViewModel(
    character: Character = testCharacter(),
    publicRepository: PublicCharacterRepository = FakePublicCharacterRepository {
        ApiResult.Success(Unit)
    },
    catalogRepository: PublicCharacterCatalogRepository = FakePublicCharacterCatalogRepository(),
    appSettingsStore: AppSettingsStore = InMemoryAppSettingsStore().apply {
        kotlinx.coroutines.runBlocking { setUserNickname("tester") }
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
    publicCharacterCatalogRepository = catalogRepository,
    appSettingsStore = appSettingsStore,
)

private class FakePublicCharacterRepository(
    private val result: suspend () -> ApiResult<Unit>,
) : PublicCharacterRepository {
    override val isSupported: Boolean = true
    var shareCount: Int = 0
    var lastSharedCharacter: Character? = null
    var lastCategoryId: String? = null
    var lastTagIds: List<String> = emptyList()

    override suspend fun share(
        character: Character,
        primaryCategoryId: String,
        tagIds: List<String>,
    ): ApiResult<Unit> {
        shareCount++
        lastSharedCharacter = character
        lastCategoryId = primaryCategoryId
        lastTagIds = tagIds
        return result()
    }
}

private class FakePublicCharacterCatalogRepository : PublicCharacterCatalogRepository {
    var catalogRequestCount: Int = 0
    var categoriesRequestCount: Int = 0

    override suspend fun getCatalog(etag: String?): ApiResult<PublicCatalogFetch> {
        catalogRequestCount++
        return ApiResult.Success(
            PublicCatalogFetch(
                catalog = PublicCatalog(
                    schemaVersion = 1,
                    catalogVersion = "test",
                    generatedAt = "",
                    categoriesUrl = "/categories.json",
                    tags = listOf(
                        PublicTag(
                            id = "healing",
                            name = "治愈",
                            sortOrder = 2,
                            firstPageUrl = "",
                        ),
                        PublicTag(
                            id = "gentle",
                            name = "温柔",
                            sortOrder = 1,
                            firstPageUrl = "",
                        ),
                    ),
                ),
                etag = null,
                notModified = false,
            ),
        )
    }

    override suspend fun getCategories(url: String): ApiResult<PublicCategories> {
        categoriesRequestCount++
        return ApiResult.Success(
            PublicCategories(
                schemaVersion = 1,
                catalogVersion = "test",
                categories = listOf(
                    testCategory(id = "general", sortOrder = 2),
                    testCategory(id = "game", sortOrder = 1),
                ),
            ),
        )
    }

    override suspend fun getCharacterPage(url: String): ApiResult<PublicCharacterPage> =
        error("Not used")

    override suspend fun getCharacterDetail(url: String): ApiResult<PublicCharacterDetail> =
        error("Not used")

    override fun resolveUrl(url: String): String = url
}

private fun testCategory(
    id: String,
    sortOrder: Int,
) = PublicCategory(
    id = id,
    name = id,
    sortOrder = sortOrder,
    characterCount = 0,
    firstPageUrl = "",
)

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
