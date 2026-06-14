package com.kaixuan.starrailchatbox.ui.character.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import okio.Path.Companion.toPath

class CharacterCatalogViewModel(
    private val catalogRepository: PublicCharacterCatalogRepository,
    private val characterRepository: CharacterRepository,
    private val httpClient: HttpClient,
    private val fileManager: KmpFileManager = KmpFileManager.Default,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CharacterCatalogUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterCatalogEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        // 观察本地已经导入的角色，随时刷新勾选状态
        viewModelScope.launch {
            characterRepository.observeCharacterSummaries().collect { list ->
                _uiState.update { it.copy(importedCharacterIds = list.map { c -> c.id }.toSet()) }
            }
        }
        onAction(CharacterCatalogAction.LoadCatalog)
    }

    fun onAction(action: CharacterCatalogAction) {
        when (action) {
            is CharacterCatalogAction.LoadCatalog -> loadCatalog()
            is CharacterCatalogAction.SelectCategory -> selectCategory(action.categoryId)
            is CharacterCatalogAction.ToggleTag -> toggleTag(action.tagId)
            is CharacterCatalogAction.ClearTags -> clearTags()
            is CharacterCatalogAction.SearchQueryChanged -> changeSearchQuery(action.query)
            is CharacterCatalogAction.ImportCharacterClicked -> importCharacter(action.character)
            is CharacterCatalogAction.ToggleTagFilter -> _uiState.update { it.copy(isTagFilterOpen = !it.isTagFilterOpen) }
            is CharacterCatalogAction.LoadNextPage -> loadNextPage()
        }
    }

    fun resolveUrl(url: String): String {
        return catalogRepository.resolveUrl(url)
    }

    private fun loadCatalog() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            when (val result = catalogRepository.getCatalog()) {
                is ApiResult.Success -> {
                    val catalogFetch = result.value
                    val catalog = catalogFetch.catalog
                    if (catalog != null) {
                        _uiState.update { it.copy(tags = catalog.tags) }
                        // 获取品类
                        when (val categoriesResult = catalogRepository.getCategories(catalog.categoriesUrl)) {
                            is ApiResult.Success -> {
                                val categoriesData = categoriesResult.value.categories.sortedBy { it.sortOrder }
                                _uiState.update { it.copy(categories = categoriesData, isLoading = false) }
                                // 默认选择第一个分类
                                categoriesData.firstOrNull()?.let { firstCat ->
                                    selectCategory(firstCat.id)
                                }
                            }
                            else -> {
                                _uiState.update { it.copy(isLoading = false) }
                                _effects.send(CharacterCatalogEffect.ShowToast("加载角色品类失败"))
                            }
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.send(CharacterCatalogEffect.ShowToast("加载目录配置失败"))
                }
            }
        }
    }

    private fun selectCategory(categoryId: String) {
        val selectedCat = _uiState.value.categories.find { it.id == categoryId } ?: return
        _uiState.update {
            it.copy(
                selectedCategoryId = categoryId,
                characters = emptyList(),
                filteredCharacters = emptyList(),
                page = 1,
                totalPages = 1
            )
        }
        loadPage(selectedCat.firstPageUrl)
    }

    private fun loadPage(url: String) {
        _uiState.update { it.copy(isPageLoading = true) }
        viewModelScope.launch {
            when (val result = catalogRepository.getCharacterPage(url)) {
                is ApiResult.Success -> {
                    val pageData = result.value
                    _uiState.update { state ->
                        val updatedList = state.characters + pageData.items
                        state.copy(
                            characters = updatedList,
                            page = pageData.page,
                            totalPages = pageData.totalPages,
                            isPageLoading = false
                        ).applyFilter(state.searchQuery, state.selectedTagIds)
                    }
                }
                else -> {
                    _uiState.update { it.copy(isPageLoading = false) }
                    _effects.send(CharacterCatalogEffect.ShowToast("加载角色列表失败"))
                }
            }
        }
    }

    private fun loadNextPage() {
        val state = _uiState.value
        if (state.isPageLoading || state.page >= state.totalPages) return
        val selectedCat = state.categories.find { it.id == state.selectedCategoryId } ?: return
        val nextPage = state.page + 1
        val nextPageUrl = getPageUrl(selectedCat.firstPageUrl, nextPage)
        loadPage(nextPageUrl)
    }

    private fun toggleTag(tagId: String) {
        _uiState.update { state ->
            val updatedTags = if (state.selectedTagIds.contains(tagId)) {
                state.selectedTagIds - tagId
            } else {
                state.selectedTagIds + tagId
            }
            state.applyFilter(state.searchQuery, updatedTags)
        }
    }

    private fun clearTags() {
        _uiState.update { state ->
            state.applyFilter(state.searchQuery, emptySet())
        }
    }

    private fun changeSearchQuery(query: String) {
        _uiState.update { state ->
            state.applyFilter(query, state.selectedTagIds)
        }
    }

    private fun CharacterCatalogUiState.applyFilter(query: String, tagIds: Set<String>): CharacterCatalogUiState {
        val filtered = characters.filter { char ->
            val matchesQuery = query.isBlank() ||
                    char.name.contains(query, ignoreCase = true) ||
                    char.author.contains(query, ignoreCase = true)
            val matchesTags = tagIds.isEmpty() || char.tagIds.containsAll(tagIds)
            matchesQuery && matchesTags
        }
        return this.copy(filteredCharacters = filtered, searchQuery = query, selectedTagIds = tagIds)
    }

    private fun importCharacter(summary: PublicCharacterSummary) {
        val charId = summary.id
        if (_uiState.value.importingCharacterIds.contains(charId) || _uiState.value.importedCharacterIds.contains(charId)) {
            return
        }
        _uiState.update { it.copy(importingCharacterIds = it.importingCharacterIds + charId) }
        viewModelScope.launch {
            try {
                when (val result = catalogRepository.getCharacterDetail(summary.detailUrl)) {
                    is ApiResult.Success -> {
                        val detail = result.value

                        // 两阶段落盘法第一阶段：下载图片存入缓存
                        val avatarSource = if (!detail.avatarUrl.isNullOrBlank()) {
                            try {
                                val responseBytes = httpClient.get(catalogRepository.resolveUrl(detail.avatarUrl)).body<ByteArray>()
                                val ext = detail.avatarUrl.substringAfterLast('.', "png")
                                val cacheFileName = "temp_import_${charId}_${Clock.System.now().toEpochMilliseconds()}.$ext"
                                val cachePath = fileManager.cacheDir / cacheFileName.toPath()
                                fileManager.writeBytes(cachePath, responseBytes)
                                CharacterAvatarSource(
                                    uri = cachePath.toString(),
                                    name = cacheFileName,
                                    extension = ext
                                )
                            } catch (e: Exception) {
                                Napier.e("Failed to download avatar", e)
                                null
                            }
                        } else {
                            null
                        }

                        // 第一阶段：下载语音存入缓存
                        val voiceSampleUri = if (!detail.voiceSampleUrl.isNullOrBlank()) {
                            try {
                                val responseBytes = httpClient.get(catalogRepository.resolveUrl(detail.voiceSampleUrl)).body<ByteArray>()
                                val ext = detail.voiceSampleUrl.substringAfterLast('.', "mp3")
                                val cacheFileName = "temp_voice_${charId}_${Clock.System.now().toEpochMilliseconds()}.$ext"
                                val cachePath = fileManager.cacheDir / cacheFileName.toPath()
                                fileManager.writeBytes(cachePath, responseBytes)
                                cachePath.toString()
                            } catch (e: Exception) {
                                Napier.e("Failed to download voice", e)
                                null
                            }
                        } else {
                            null
                        }

                        // 保存角色并激活 Room 内部两阶段落盘法
                        characterRepository.updateCharacter(
                            Character(
                                id = charId,
                                name = detail.name.trim(),
                                prompt = detail.systemPrompt,
                                openingMessage = detail.openingMessage,
                                avatarUri = "",
                                author = detail.author.trim(),
                                description = detail.description,
                                voiceSampleUri = voiceSampleUri,
                                temperature = detail.temperature,
                                topP = detail.topP,
                                createdAt = Clock.System.now().toEpochMilliseconds(),
                            ),
                            avatarSource = avatarSource
                        )

                        _effects.send(CharacterCatalogEffect.ShowToast("角色“${detail.name}”导入成功"))
                    }
                    else -> {
                        _effects.send(CharacterCatalogEffect.ShowToast("获取角色详情失败"))
                    }
                }
            } catch (e: Exception) {
                Napier.e("Import character failed", e)
                _effects.send(CharacterCatalogEffect.ShowToast("导入失败：${e.message}"))
            } finally {
                _uiState.update { it.copy(importingCharacterIds = it.importingCharacterIds - charId) }
            }
        }
    }

    private fun getPageUrl(firstPageUrl: String, page: Int): String {
        if (page == 1) return firstPageUrl
        val regex = Regex("page1\\.json$")
        if (regex.containsMatchIn(firstPageUrl)) {
            return firstPageUrl.replace(regex, "page$page.json")
        }
        val regexUnderscore = Regex("_page1\\.json")
        if (regexUnderscore.containsMatchIn(firstPageUrl)) {
            return firstPageUrl.replace(regexUnderscore, "_page${page}.json")
        }
        return firstPageUrl.replace("1.json", "$page.json")
    }
}
