package com.kaixuan.starrailchatbox.ui.character.catalog

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminOperation
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminOperationRequest
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminRepository
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminVerifyResponse
import com.kaixuan.starrailchatbox.data.character.catalog.PublicAllCharacters
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCatalog
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCatalogFetch
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategories
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategory
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterDetail
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterPage
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterCatalogViewModelTest {
    @Test
    fun catalogInitialPageRestoresSelectedCategory() {
        val tabs = listOf(
            CategoryTab(id = null, name = "全部", firstPageUrl = "/all.json"),
            CategoryTab(id = "game", name = "崩坏：星穹铁道", firstPageUrl = "/game.json"),
            CategoryTab(id = "new", name = "崩坏3", firstPageUrl = "/new.json"),
        )

        assertEquals(2, catalogCategoryIndex(tabs, selectedCategoryId = "new"))
        assertEquals(0, catalogCategoryIndex(tabs, selectedCategoryId = "missing"))
    }

    @Test
    fun dynamicCategoryListOnlyContainsServerGeneratedCategoryIds() {
        val state = CharacterCatalogUiState(
            categories = listOf(
                category("bt", "崩坏：星穹铁道"),
                category("cat_123456789abc", "动态分类"),
                category("cat_invalid", "无效动态分类"),
            ),
        )

        assertEquals(
            listOf("cat_123456789abc"),
            state.dynamicCategories.map { it.id },
        )
    }

    @Test
    fun pagerSyncDoesNotChangeSelectionUntilTargetPageSettles() {
        val tabs = listOf(
            CategoryTab(id = null, name = "全部", firstPageUrl = "/all.json"),
            CategoryTab(id = "game", name = "崩坏：星穹铁道", firstPageUrl = "/game.json"),
            CategoryTab(id = "new", name = "崩坏3", firstPageUrl = "/new.json"),
        )

        assertNull(
            catalogPageSelectionAction(
                tabs = tabs,
                snapshot = CatalogPagerSyncSnapshot(
                    settledPage = 2,
                    isScrollInProgress = true,
                ),
                selectedCategoryId = null,
            ),
        )
        assertNull(
            catalogPageSelectionAction(
                tabs = tabs,
                snapshot = CatalogPagerSyncSnapshot(
                    settledPage = 0,
                    isScrollInProgress = true,
                ),
                selectedCategoryId = "new",
            ),
        )
        assertEquals(
            CharacterCatalogAction.SelectAll,
            catalogPageSelectionAction(
                tabs = tabs,
                snapshot = CatalogPagerSyncSnapshot(
                    settledPage = 0,
                    isScrollInProgress = false,
                ),
                selectedCategoryId = "new",
            ),
        )
    }

    @Test
    fun refreshReloadsCatalogTaxonomyAndSelectedCategoryPage() = runTest {
        val catalogRepository = RefreshableCatalogRepository()
        val viewModel = CharacterCatalogViewModel(
            catalogRepository = catalogRepository,
            characterRepository = EmptyCharacterRepository,
            httpClient = HttpClient(MockEngine { respondOk() }),
            adminRepository = NoOpCatalogAdminRepository,
            appSettingsStore = InMemoryAppSettingsStore(),
        )

        viewModel.onAction(CharacterCatalogAction.LoadCatalog)
        viewModel.uiState.first {
            catalogRepository.catalogRequestCount == 1 &&
                !it.isLoading &&
                !it.isPageLoading
        }
        viewModel.onAction(CharacterCatalogAction.SelectCategory("game"))
        viewModel.uiState.first { !it.isPageLoading && it.selectedCategoryId == "game" }

        catalogRepository.version = 2
        viewModel.onAction(CharacterCatalogAction.RefreshCatalog)
        viewModel.uiState.first {
            catalogRepository.catalogRequestCount == 2 && !it.isRefreshing
        }

        assertEquals(2, catalogRepository.catalogRequestCount)
        assertEquals(listOf("game-v2", "new-v2"), viewModel.uiState.value.categories.map { it.name })
        assertEquals(listOf("tag-v2"), viewModel.uiState.value.tags.map { it.id })
        assertEquals("game", viewModel.uiState.value.selectedCategoryId)
        assertEquals("/game-v2/page1.json", catalogRepository.lastPageUrl)
        assertFalse(viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun preloadAndSwipeCategoryMaintainsSeparateListsAndStatus() = runTest {
        val catalogRepository = RefreshableCatalogRepository()
        val viewModel = CharacterCatalogViewModel(
            catalogRepository = catalogRepository,
            characterRepository = EmptyCharacterRepository,
            httpClient = HttpClient(MockEngine { respondOk() }),
            adminRepository = NoOpCatalogAdminRepository,
            appSettingsStore = InMemoryAppSettingsStore(),
        )

        // 1. 加载目录
        viewModel.onAction(CharacterCatalogAction.LoadCatalog)
        viewModel.uiState.first {
            catalogRepository.catalogRequestCount == 1 && !it.isLoading
        }

        // 2. 发送 PreloadCategory 预加载另一个分类，比如 "new"
        viewModel.onAction(CharacterCatalogAction.PreloadCategory("new", "/new-v1/page1.json"))
        // 等待该预加载完成
        viewModel.uiState.first {
            it.categoryStates["new"]?.isPageLoading == false
        }

        // 验证预加载分类 "new" 的 firstPageUrl 正确
        assertEquals("/new-v1/page1.json", viewModel.uiState.value.categoryStates["new"]?.firstPageUrl)

        // 3. 用户切换选中到 "game"
        viewModel.onAction(CharacterCatalogAction.SelectCategory("game"))
        viewModel.uiState.first {
            it.selectedCategoryId == "game" && !it.isPageLoading
        }

        // 4. 再次验证 "new" 和 "game" 拥有独立的数据加载路径
        assertEquals("/game-v1/page1.json", catalogRepository.lastPageUrl)
    }
}

private fun category(id: String, name: String) = PublicCategory(
    id = id,
    name = name,
    sortOrder = 1,
    characterCount = 0,
    firstPageUrl = "/$id/page1.json",
)

private class RefreshableCatalogRepository : PublicCharacterCatalogRepository {
    var version = 1
    var catalogRequestCount = 0
    var lastPageUrl: String? = null

    override suspend fun getCatalog(etag: String?): ApiResult<PublicCatalogFetch> {
        catalogRequestCount++
        return ApiResult.Success(
            PublicCatalogFetch(
                catalog = PublicCatalog(
                    schemaVersion = 1,
                    catalogVersion = "v$version",
                    generatedAt = "",
                    categoriesUrl = "/categories-v$version.json",
                    allCharacters = PublicAllCharacters(
                        name = "全部",
                        characterCount = 1,
                        firstPageUrl = "/all-v$version/page1.json",
                    ),
                    tags = listOf(
                        PublicTag(
                            id = "tag-v$version",
                            name = "标签 $version",
                            sortOrder = 1,
                            firstPageUrl = "/tag-v$version/page1.json",
                        ),
                    ),
                ),
                etag = null,
                notModified = false,
            ),
        )
    }

    override suspend fun getCategories(url: String): ApiResult<PublicCategories> =
        ApiResult.Success(
            PublicCategories(
                schemaVersion = 1,
                catalogVersion = "v$version",
                categories = listOf(
                    PublicCategory(
                        id = "game",
                        name = "game-v$version",
                        sortOrder = 1,
                        characterCount = 1,
                        firstPageUrl = "/game-v$version/page1.json",
                    ),
                    PublicCategory(
                        id = "new",
                        name = "new-v$version",
                        sortOrder = 2,
                        characterCount = 1,
                        firstPageUrl = "/new-v$version/page1.json",
                    ),
                ),
            ),
        )

    override suspend fun getCharacterPage(url: String): ApiResult<PublicCharacterPage> {
        lastPageUrl = url
        return ApiResult.Success(
            PublicCharacterPage(
                schemaVersion = 1,
                catalogVersion = "v$version",
                page = 1,
                pageSize = 50,
                total = 0,
                totalPages = 1,
                items = emptyList(),
            ),
        )
    }

    override suspend fun getCharacterDetail(url: String): ApiResult<PublicCharacterDetail> =
        error("Not used")

    override fun resolveUrl(url: String): String = url
}

private object EmptyCharacterRepository : CharacterRepository {
    override suspend fun loadCharacters(): List<Character> = emptyList()
    override fun observeCharacterSummaries(): Flow<List<CharacterSummary>> = flowOf(emptyList())
    override suspend fun getCharacter(id: String): Character? = null
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

private object NoOpCatalogAdminRepository : CatalogAdminRepository {
    override suspend fun verify(adminKey: String): ApiResult<CatalogAdminVerifyResponse> =
        ApiResult.NetworkError("Not used")

    override suspend fun createOperation(
        adminKey: String,
        request: CatalogAdminOperationRequest,
        idempotencyKey: String,
    ): ApiResult<CatalogAdminOperation> = ApiResult.NetworkError("Not used")

    override suspend fun getOperation(
        adminKey: String,
        operationId: String,
    ): ApiResult<CatalogAdminOperation> = ApiResult.NetworkError("Not used")
}
