package com.kaixuan.starrailchatbox.ui.character.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminOperationPayload
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminOperationRequest
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminOperationType
import com.kaixuan.starrailchatbox.data.character.catalog.CatalogAdminRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.PlatformType
import com.kaixuan.starrailchatbox.getPlatform
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Clock
import okio.Path.Companion.toPath

class CharacterCatalogViewModel(
    private val catalogRepository: PublicCharacterCatalogRepository,
    private val characterRepository: CharacterRepository,
    private val httpClient: HttpClient,
    private val adminRepository: CatalogAdminRepository,
    private val appSettingsStore: AppSettingsStore,
    private val fileManager: KmpFileManager = KmpFileManager.Default,
) : ViewModel() {
    private var adminKey: String? = null
    private var titleClickCount = 0
    private var lastTitleClickAt = 0L
    private var catalogLoaded = false
    private var loadJob: Job? = null
    private val loadJobs = mutableMapOf<String?, Job>()

    private val _uiState = MutableStateFlow(CharacterCatalogUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterCatalogEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        val adminSupported = getPlatform().type in setOf(PlatformType.Android, PlatformType.Windows)
        _uiState.update { it.copy(adminSupported = adminSupported) }
        // 观察本地已经导入的角色，随时刷新勾选状态
        viewModelScope.launch {
            characterRepository.observeCharacterSummaries().collect { list ->
                _uiState.update { it.copy(importedCharacterIds = list.map { c -> c.id }.toSet()) }
            }
        }
        if (adminSupported) restoreAdminMode()
    }

    fun onAction(action: CharacterCatalogAction) {
        when (action) {
            is CharacterCatalogAction.LoadCatalog -> loadCatalog()
            CharacterCatalogAction.RefreshCatalog -> refreshCatalog()
            CharacterCatalogAction.SelectAll -> selectAll()
            is CharacterCatalogAction.SelectCategory -> selectCategory(action.categoryId)
            is CharacterCatalogAction.PreloadCategory -> {
                val catState = _uiState.value.categoryStates[action.categoryId]
                if (catState != null && catState.characters.isEmpty() && !catState.isPageLoading) {
                    loadCategoryPage(action.categoryId, action.firstPageUrl)
                }
            }
            is CharacterCatalogAction.ToggleTag -> toggleTag(action.tagId)
            is CharacterCatalogAction.ClearTags -> clearTags()
            is CharacterCatalogAction.SearchQueryChanged -> changeSearchQuery(action.query)
            is CharacterCatalogAction.ImportCharacterClicked -> importCharacter(action.character)
            is CharacterCatalogAction.ToggleTagFilter -> _uiState.update { it.copy(isTagFilterOpen = !it.isTagFilterOpen) }
            is CharacterCatalogAction.LoadNextPage -> loadNextPage()
            CharacterCatalogAction.TitleClicked -> onTitleClicked()
            is CharacterCatalogAction.AdminKeyChanged -> {
                _uiState.update { it.copy(adminKeyDraft = action.value) }
            }
            CharacterCatalogAction.ConfirmAdminKey -> confirmAdminKey()
            CharacterCatalogAction.DismissAdminKeyDialog -> {
                _uiState.update { it.copy(showAdminKeyDialog = false, adminKeyDraft = "") }
            }
            CharacterCatalogAction.DisableAdminMode -> disableAdminMode()
            CharacterCatalogAction.RefreshTaxonomy -> rebuildCatalog()
            CharacterCatalogAction.CreateCategoryClicked -> {
                _uiState.update { it.copy(showCreateCategoryDialog = true, categoryNameDraft = "") }
            }
            is CharacterCatalogAction.CategoryNameChanged -> {
                _uiState.update { it.copy(categoryNameDraft = action.value) }
            }
            CharacterCatalogAction.ConfirmCreateCategory -> createCategory()
            CharacterCatalogAction.DismissCreateCategoryDialog -> {
                _uiState.update { it.copy(showCreateCategoryDialog = false, categoryNameDraft = "") }
            }
            is CharacterCatalogAction.MoveCharacterClicked -> {
                _uiState.update { it.copy(movingCharacter = action.character) }
            }
            CharacterCatalogAction.DismissMoveCharacterDialog -> {
                _uiState.update { it.copy(movingCharacter = null) }
            }
            is CharacterCatalogAction.ConfirmMoveCharacter -> moveCharacter(action.categoryId)
            is CharacterCatalogAction.DeleteCharacterClicked -> {
                _uiState.update { it.copy(deletingCharacter = action.character) }
            }
            CharacterCatalogAction.DismissDeleteCharacterDialog -> {
                _uiState.update { it.copy(deletingCharacter = null) }
            }
            CharacterCatalogAction.ConfirmDeleteCharacter -> deleteCharacter()
        }
    }

    private fun restoreAdminMode() {
        viewModelScope.launch {
            val storedKey = appSettingsStore.getCatalogAdminKey() ?: return@launch
            when (val result = adminRepository.verify(storedKey)) {
                is ApiResult.Success -> {
                    if (result.value.success && result.value.admin) {
                        adminKey = storedKey
                        _uiState.update { it.copy(adminModeEnabled = true) }
                    } else {
                        appSettingsStore.setCatalogAdminKey(null)
                    }
                }
                is ApiResult.HttpError -> {
                    if (result.statusCode == 401 || result.statusCode == 403) {
                        appSettingsStore.setCatalogAdminKey(null)
                    }
                }
                else -> Unit
            }
        }
    }

    private fun onTitleClicked() {
        if (!_uiState.value.adminSupported || _uiState.value.adminModeEnabled) return
        val now = Clock.System.now().toEpochMilliseconds()
        titleClickCount = if (now - lastTitleClickAt <= 2_000) titleClickCount + 1 else 1
        lastTitleClickAt = now
        if (titleClickCount >= 7) {
            titleClickCount = 0
            _uiState.update { it.copy(showAdminKeyDialog = true, adminKeyDraft = "") }
        }
    }

    private fun confirmAdminKey() {
        val key = _uiState.value.adminKeyDraft.trim()
        if (key.isBlank() || _uiState.value.isAdminBusy) return
        _uiState.update { it.copy(isAdminBusy = true) }
        viewModelScope.launch {
            when (val result = adminRepository.verify(key)) {
                is ApiResult.Success -> {
                    if (result.value.success && result.value.admin) {
                        adminKey = key
                        appSettingsStore.setCatalogAdminKey(key)
                        _uiState.update {
                            it.copy(
                                adminModeEnabled = true,
                                isAdminBusy = false,
                                showAdminKeyDialog = false,
                                adminKeyDraft = "",
                            )
                        }
                        _effects.send(CharacterCatalogEffect.ShowToast("管理员模式已开启"))
                    } else {
                        _uiState.update { it.copy(isAdminBusy = false) }
                        _effects.send(CharacterCatalogEffect.ShowToast("管理员密钥无效"))
                    }
                }
                else -> {
                    _uiState.update { it.copy(isAdminBusy = false) }
                    _effects.send(CharacterCatalogEffect.ShowToast("管理员密钥验证失败"))
                }
            }
        }
    }

    private fun disableAdminMode() {
        adminKey = null
        viewModelScope.launch { appSettingsStore.setCatalogAdminKey(null) }
        _uiState.update {
            it.copy(
                adminModeEnabled = false,
                showAdminKeyDialog = false,
                adminKeyDraft = "",
            )
        }
    }

    private fun createCategory() {
        val key = adminKey ?: return
        val name = _uiState.value.categoryNameDraft.trim()
        if (name.isBlank() || _uiState.value.isAdminBusy) return
        _uiState.update { it.copy(isAdminBusy = true) }
        viewModelScope.launch {
            val result = adminRepository.createOperation(
                adminKey = key,
                request = CatalogAdminOperationRequest(
                    type = CatalogAdminOperationType.CreateCategory,
                    payload = CatalogAdminOperationPayload(name = name),
                ),
                idempotencyKey = "create-category-${Clock.System.now().toEpochMilliseconds()}",
            )
            if (result is ApiResult.Success && result.value.status == "APPROVED") {
                _uiState.update {
                    it.copy(
                        isAdminBusy = false,
                        showCreateCategoryDialog = false,
                        categoryNameDraft = "",
                    )
                }
                _effects.send(CharacterCatalogEffect.ShowToast("分类已创建"))
                reloadCatalog()
            } else {
                _uiState.update { it.copy(isAdminBusy = false) }
                handleAdminFailure(result, "创建分类失败")
            }
        }
    }

    private fun rebuildCatalog() {
        val key = adminKey ?: return
        if (_uiState.value.isAdminBusy || _uiState.value.isLoading) return
        _uiState.update { it.copy(isAdminBusy = true) }
        viewModelScope.launch {
            val result = adminRepository.createOperation(
                adminKey = key,
                request = CatalogAdminOperationRequest(
                    type = CatalogAdminOperationType.RebuildCatalog,
                    payload = CatalogAdminOperationPayload(),
                ),
                idempotencyKey = "rebuild-catalog-${Clock.System.now().toEpochMilliseconds()}",
            )
            if (result is ApiResult.Success && result.value.status == "APPROVED") {
                _uiState.update { it.copy(isAdminBusy = false) }
                _effects.send(CharacterCatalogEffect.ShowToast("OSS 目录已重新生成"))
                reloadCatalog()
            } else {
                _uiState.update { it.copy(isAdminBusy = false) }
                handleAdminFailure(result, "重新生成目录失败")
            }
        }
    }

    private fun moveCharacter(categoryId: String) {
        val key = adminKey ?: return
        val character = _uiState.value.movingCharacter ?: return
        if (_uiState.value.isAdminBusy) return
        _uiState.update { it.copy(isAdminBusy = true) }
        viewModelScope.launch {
            val result = adminRepository.createOperation(
                adminKey = key,
                request = CatalogAdminOperationRequest(
                    type = CatalogAdminOperationType.MoveCharacter,
                    payload = CatalogAdminOperationPayload(
                        characterKey = character.characterKey,
                        primaryCategoryId = categoryId,
                    ),
                ),
                idempotencyKey = "move-${character.characterKey}-$categoryId-${Clock.System.now().toEpochMilliseconds()}",
            )
            if (result is ApiResult.Success && result.value.status == "APPROVED") {
                _uiState.update { it.copy(isAdminBusy = false, movingCharacter = null) }
                _effects.send(CharacterCatalogEffect.ShowToast("角色分类已更新"))
                reloadCatalog()
            } else {
                _uiState.update { it.copy(isAdminBusy = false) }
                handleAdminFailure(result, "移动角色失败")
            }
        }
    }

    private fun deleteCharacter() {
        val key = adminKey ?: return
        val character = _uiState.value.deletingCharacter ?: return
        if (_uiState.value.isAdminBusy) return
        _uiState.update { it.copy(isAdminBusy = true) }
        viewModelScope.launch {
            val result = adminRepository.createOperation(
                adminKey = key,
                request = CatalogAdminOperationRequest(
                    type = CatalogAdminOperationType.DeleteCharacter,
                    payload = CatalogAdminOperationPayload(characterKey = character.characterKey),
                ),
                idempotencyKey = "delete-${character.characterKey}-${Clock.System.now().toEpochMilliseconds()}",
            )
            if (result is ApiResult.Success && result.value.status == "PENDING_REVIEW") {
                _uiState.update {
                    it.copy(
                        isAdminBusy = false,
                        deletingCharacter = null,
                        pendingDeleteCharacterKeys =
                            it.pendingDeleteCharacterKeys + character.characterKey,
                    )
                }
                _effects.send(CharacterCatalogEffect.ShowToast("已提交飞书审批"))
                pollDeleteOperation(key, result.value.operationId, character.characterKey)
            } else {
                _uiState.update { it.copy(isAdminBusy = false) }
                handleAdminFailure(result, "提交下架审批失败")
            }
        }
    }

    private fun pollDeleteOperation(key: String, operationId: String, characterKey: String) {
        viewModelScope.launch {
            while (true) {
                delay(3_000)
                when (val result = adminRepository.getOperation(key, operationId)) {
                    is ApiResult.Success -> when (result.value.status) {
                        "APPROVED" -> {
                            _uiState.update {
                                it.copy(
                                    pendingDeleteCharacterKeys =
                                        it.pendingDeleteCharacterKeys - characterKey,
                                )
                            }
                            _effects.send(CharacterCatalogEffect.ShowToast("角色已下架"))
                            reloadCatalog()
                            return@launch
                        }
                        "REJECTED", "FAILED" -> {
                            _uiState.update {
                                it.copy(
                                    pendingDeleteCharacterKeys =
                                        it.pendingDeleteCharacterKeys - characterKey,
                                )
                            }
                            _effects.send(
                                CharacterCatalogEffect.ShowToast(
                                    result.value.message ?: "角色下架未通过",
                                ),
                            )
                            return@launch
                        }
                    }
                    is ApiResult.HttpError -> if (result.statusCode == 401 || result.statusCode == 403) {
                        disableAdminMode()
                        return@launch
                    }
                    else -> Unit
                }
            }
        }
    }

    private suspend fun handleAdminFailure(result: ApiResult<*>, fallback: String) {
        if (result is ApiResult.HttpError && (result.statusCode == 401 || result.statusCode == 403)) {
            disableAdminMode()
            _effects.send(CharacterCatalogEffect.ShowToast("管理员凭证已失效"))
        } else {
            _effects.send(CharacterCatalogEffect.ShowToast(fallback))
        }
    }

    fun resolveUrl(url: String): String {
        return catalogRepository.resolveUrl(url)
    }

    private fun loadCatalog() {
        if (_uiState.value.isLoading || catalogLoaded) return
        _uiState.update { it.copy(isLoading = true) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val result = catalogRepository.getCatalog()) {
                is ApiResult.Success -> {
                    val catalogFetch = result.value
                    val catalog = catalogFetch.catalog
                    if (catalog != null) {
                        _uiState.update {
                            it.copy(
                                allCharacters = catalog.allCharacters,
                                tags = catalog.tags,
                            )
                        }
                        // 获取品类
                        when (val categoriesResult = catalogRepository.getCategories(catalog.categoriesUrl)) {
                            is ApiResult.Success -> {
                                val categoriesData = categoriesResult.value.categories.sortedBy { it.sortOrder }
                                catalogLoaded = true
                                val newStates = mutableMapOf<String?, CategoryState>()
                                if (catalog.allCharacters != null) {
                                    newStates[null] = CategoryState(categoryId = null, firstPageUrl = catalog.allCharacters.firstPageUrl)
                                }
                                categoriesData.filter { it.characterCount > 0 }.forEach { cat ->
                                    newStates[cat.id] = CategoryState(categoryId = cat.id, firstPageUrl = cat.firstPageUrl)
                                }
                                _uiState.update { 
                                    it.copy(
                                        categories = categoriesData, 
                                        categoryStates = newStates,
                                        isLoading = false
                                    ) 
                                }
                                if (catalog.allCharacters != null) {
                                    selectAll()
                                } else {
                                    categoriesData.firstOrNull { it.characterCount > 0 }?.let { firstCat ->
                                        selectCategory(firstCat.id)
                                    }
                                }
                            }
                            else -> {
                                catalogLoaded = false
                                _uiState.update { it.copy(isLoading = false) }
                                _effects.send(CharacterCatalogEffect.ShowToast("加载角色品类失败"))
                            }
                        }
                    } else {
                        catalogLoaded = true
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
                else -> {
                    catalogLoaded = false
                    _uiState.update { it.copy(isLoading = false) }
                    _effects.send(CharacterCatalogEffect.ShowToast("加载目录配置失败"))
                }
            }
        }
    }

    private fun reloadCatalog() {
        catalogLoaded = false
        loadCatalog()
    }

    private fun refreshCatalog() {
        val currentState = _uiState.value
        if (currentState.isLoading || currentState.isRefreshing) return

        _uiState.update { it.copy(isRefreshing = true) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            when (val catalogResult = catalogRepository.getCatalog()) {
                is ApiResult.Success -> {
                    val catalog = catalogResult.value.catalog
                    if (catalog == null) {
                        _uiState.update { it.copy(isRefreshing = false) }
                        return@launch
                    }

                    when (val categoriesResult = catalogRepository.getCategories(catalog.categoriesUrl)) {
                        is ApiResult.Success -> {
                            val categories = categoriesResult.value.categories.sortedBy { it.sortOrder }
                            val previousCategoryId = _uiState.value.selectedCategoryId
                            val selectedCategory = previousCategoryId?.let { categoryId ->
                                categories.firstOrNull {
                                    it.id == categoryId && it.characterCount > 0
                                }
                            }
                            val fallbackCategory = categories.firstOrNull { it.characterCount > 0 }
                            val targetCategoryId = when {
                                selectedCategory != null -> selectedCategory.id
                                catalog.allCharacters != null -> null
                                else -> fallbackCategory?.id
                            }
                            val targetPageUrl = when {
                                selectedCategory != null -> selectedCategory.firstPageUrl
                                catalog.allCharacters != null -> catalog.allCharacters.firstPageUrl
                                else -> fallbackCategory?.firstPageUrl
                            }

                            catalogLoaded = true
                            val newStates = mutableMapOf<String?, CategoryState>()
                            if (catalog.allCharacters != null) {
                                newStates[null] = CategoryState(categoryId = null, firstPageUrl = catalog.allCharacters.firstPageUrl)
                            }
                            categories.filter { it.characterCount > 0 }.forEach { cat ->
                                newStates[cat.id] = CategoryState(categoryId = cat.id, firstPageUrl = cat.firstPageUrl)
                            }

                            _uiState.update { state ->
                                val availableTagIds = catalog.tags.mapTo(mutableSetOf()) { it.id }
                                state.copy(
                                    allCharacters = catalog.allCharacters,
                                    categories = categories,
                                    tags = catalog.tags,
                                    selectedCategoryId = targetCategoryId,
                                    activeFirstPageUrl = targetPageUrl,
                                    selectedTagIds = state.selectedTagIds.intersect(availableTagIds),
                                    categoryStates = newStates,
                                    characters = emptyList(),
                                    filteredCharacters = emptyList(),
                                    page = 1,
                                    totalPages = 1,
                                )
                            }

                            if (targetPageUrl == null) {
                                _uiState.update { it.copy(isRefreshing = false) }
                            } else {
                                loadCategoryPage(targetCategoryId, targetPageUrl, finishRefresh = true)
                            }
                        }
                        else -> {
                            _uiState.update { it.copy(isRefreshing = false) }
                            _effects.send(CharacterCatalogEffect.ShowToast("刷新角色品类失败"))
                        }
                    }
                }
                else -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                    _effects.send(CharacterCatalogEffect.ShowToast("刷新目录配置失败"))
                }
            }
        }
    }

    private fun filterCategoryCharacters(
        characters: List<PublicCharacterSummary>,
        query: String,
        tagIds: Set<String>
    ): List<PublicCharacterSummary> {
        return characters.filter { char ->
            val matchesQuery = query.isBlank() ||
                    char.name.contains(query, ignoreCase = true) ||
                    char.author.contains(query, ignoreCase = true)
            val matchesTags = tagIds.isEmpty() || char.tagIds.containsAll(tagIds)
            matchesQuery && matchesTags
        }.distinctBy { it.id }
    }

    private fun selectCategoryInternal(categoryId: String?) {
        _uiState.update { state ->
            val catState = state.categoryStates[categoryId]
            if (catState != null) {
                state.copy(
                    selectedCategoryId = categoryId,
                    activeFirstPageUrl = catState.firstPageUrl,
                    characters = catState.characters,
                    filteredCharacters = catState.filteredCharacters,
                    page = catState.page,
                    totalPages = catState.totalPages,
                    isPageLoading = catState.isPageLoading
                )
            } else {
                state.copy(selectedCategoryId = categoryId)
            }
        }
        val currentCatState = _uiState.value.categoryStates[categoryId]
        if (currentCatState != null && currentCatState.characters.isEmpty() && !currentCatState.isPageLoading) {
            loadCategoryPage(categoryId, currentCatState.firstPageUrl)
        }
    }

    private fun loadCategoryPage(
        categoryId: String?,
        url: String,
        finishRefresh: Boolean = false,
    ) {
        _uiState.update { state ->
            val catState = state.categoryStates[categoryId]
            if (catState != null) {
                val newState = catState.copy(isPageLoading = true)
                val newStates = state.categoryStates + (categoryId to newState)
                if (state.selectedCategoryId == categoryId) {
                    state.copy(
                        categoryStates = newStates,
                        isPageLoading = true
                    )
                } else {
                    state.copy(categoryStates = newStates)
                }
            } else {
                state
            }
        }

        loadJobs[categoryId]?.cancel()
        loadJobs[categoryId] = viewModelScope.launch {
            when (val result = catalogRepository.getCharacterPage(url)) {
                is ApiResult.Success -> {
                    val pageData = result.value
                    _uiState.update { state ->
                        val catState = state.categoryStates[categoryId]
                        if (catState != null) {
                            val updatedList = (catState.characters + pageData.items).distinctBy { it.id }
                            val filtered = filterCategoryCharacters(updatedList, state.searchQuery, state.selectedTagIds)
                            val newState = catState.copy(
                                characters = updatedList,
                                filteredCharacters = filtered,
                                page = pageData.page,
                                totalPages = pageData.totalPages,
                                isPageLoading = false
                            )
                            val newStates = state.categoryStates + (categoryId to newState)
                            
                            val isCurrent = state.selectedCategoryId == categoryId
                            state.copy(
                                categoryStates = newStates,
                                isRefreshing = if (finishRefresh && isCurrent) false else state.isRefreshing,
                                characters = if (isCurrent) updatedList else state.characters,
                                filteredCharacters = if (isCurrent) filtered else state.filteredCharacters,
                                page = if (isCurrent) pageData.page else state.page,
                                totalPages = if (isCurrent) pageData.totalPages else state.totalPages,
                                isPageLoading = if (isCurrent) false else state.isPageLoading
                            )
                        } else {
                            state
                        }
                    }
                }
                else -> {
                    _uiState.update { state ->
                        val catState = state.categoryStates[categoryId]
                        if (catState != null) {
                            val newState = catState.copy(isPageLoading = false)
                            val newStates = state.categoryStates + (categoryId to newState)
                            val isCurrent = state.selectedCategoryId == categoryId
                            state.copy(
                                categoryStates = newStates,
                                isRefreshing = if (finishRefresh && isCurrent) false else state.isRefreshing,
                                isPageLoading = if (isCurrent) false else state.isPageLoading
                            )
                        } else {
                            state
                        }
                    }
                    _effects.send(CharacterCatalogEffect.ShowToast("加载角色列表失败"))
                }
            }
        }
    }

    private fun selectAll() {
        selectCategoryInternal(null)
    }

    private fun selectCategory(categoryId: String) {
        selectCategoryInternal(categoryId)
    }

    private fun loadNextPage() {
        val categoryId = _uiState.value.selectedCategoryId
        val catState = _uiState.value.categoryStates[categoryId] ?: return
        if (catState.isPageLoading || catState.page >= catState.totalPages) return
        val firstPageUrl = catState.firstPageUrl
        val nextPage = catState.page + 1
        val nextPageUrl = getPageUrl(firstPageUrl, nextPage)
        loadCategoryPage(categoryId, nextPageUrl)
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
        val updatedStates = categoryStates.mapValues { (_, catState) ->
            val filtered = filterCategoryCharacters(catState.characters, query, tagIds)
            catState.copy(filteredCharacters = filtered)
        }
        val currentCatState = updatedStates[selectedCategoryId]
        return this.copy(
            categoryStates = updatedStates,
            searchQuery = query,
            selectedTagIds = tagIds,
            characters = currentCatState?.characters ?: emptyList(),
            filteredCharacters = currentCatState?.filteredCharacters ?: emptyList(),
            page = currentCatState?.page ?: 1,
            totalPages = currentCatState?.totalPages ?: 1,
            isPageLoading = currentCatState?.isPageLoading ?: false
        )
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
