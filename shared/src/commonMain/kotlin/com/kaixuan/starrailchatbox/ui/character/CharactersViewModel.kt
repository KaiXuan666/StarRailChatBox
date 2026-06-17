package com.kaixuan.starrailchatbox.ui.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterCatalogRepository
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.sharing.DefaultPublicCharacterRepository
import com.kaixuan.starrailchatbox.data.character.sharing.PublicCharacterRepository
import com.kaixuan.starrailchatbox.data.character.sharing.ShareCategorySelection
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.ui.failureDetail
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock

class CharactersViewModel(
    private val characterRepository: CharacterRepository,
    private val characterCardExporter: CharacterCardExporter,
    private val publicCharacterRepository: PublicCharacterRepository,
    private val publicCharacterCatalogRepository: PublicCharacterCatalogRepository,
    private val appSettingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CharactersUiState())
    val uiState = _uiState.asStateFlow()

    private val _effects = Channel<CharacterEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeCharacters()
    }

    fun onAction(action: CharacterAction) {
        when (action) {
            is CharacterAction.CharacterSelected -> {
                _uiState.update { it.copy(selectedCharacterId = action.characterId) }
            }
            is CharacterAction.CharactersReordered -> reorder(action.orderedCharacters.map { it.id })
            is CharacterAction.CharacterDeleteClicked -> delete(action.characterId)
            CharacterAction.CharacterDeleteBuiltinClicked -> {
                _effects.trySend(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_DELETE_BUILTIN_RESTRICTED,
                    ),
                )
            }
            is CharacterAction.CharacterExportClicked -> {
                _uiState.update { it.copy(exportDialogCharacterId = action.characterId) }
            }
            CharacterAction.CharacterExportDialogDismissed -> {
                _uiState.update { it.copy(exportDialogCharacterId = null) }
            }
            CharacterAction.RestoreBuiltinCharactersClicked -> restoreBuiltinCharacters()
            CharacterAction.CharacterExportLocalClicked -> requestLocalExport()
            CharacterAction.CharacterSharePublicClicked -> loadShareCategories()
            is CharacterAction.CharacterShareCategorySelected -> selectShareCategory(action.categoryId)
            CharacterAction.CharacterShareCustomCategorySelected -> selectCustomShareCategory()
            is CharacterAction.CharacterShareCustomCategoryNameChanged ->
                updateCustomShareCategoryName(action.name)
            is CharacterAction.CharacterShareTagToggled -> toggleShareTag(action.tagId)
            CharacterAction.CharacterShareCategoryConfirmed -> sharePublic()
            CharacterAction.CharacterShareCategoryDialogDismissed -> dismissShareCategoryDialog()
            is CharacterAction.CharacterExportDirectorySelected -> exportSelected(action.directory)
            else -> Unit
        }
    }

    private fun observeCharacters() {
        viewModelScope.launch {
            characterRepository.observeCharacterSummaries()
                .catch { emit(emptyList()) }
                .collect(::updateCharacters)
        }
    }

    private fun updateCharacters(summaries: List<CharacterSummary>) {
        _uiState.update { state ->
            val selectedId = state.selectedCharacterId
                ?.takeIf { id -> summaries.any { it.id == id } }
                ?: summaries.firstOrNull()?.id
            state.copy(
                characters = summaries,
                selectedCharacterId = selectedId,
                isLoadingCharacters = false,
            )
        }
    }

    private fun reorder(ids: List<String>) {
        val byId = _uiState.value.characters.associateBy { it.id }
        val reordered = ids.mapNotNull(byId::get)
        if (reordered.size != byId.size) return
        _uiState.update { it.copy(characters = reordered) }
        viewModelScope.launch {
            reordered.forEachIndexed { index, summary ->
                characterRepository.updateSortOrder(summary.id, index)
            }
        }
    }

    private fun delete(characterId: String) {
        viewModelScope.launch {
            val activeCharacters = _uiState.value.characters
            if (activeCharacters.size <= 1 && activeCharacters.any { it.id == characterId }) {
                _effects.send(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_DELETE_LAST_RESTRICTED,
                    ),
                )
                return@launch
            }
            runCatching {
                characterRepository.deleteCharacter(
                    characterId,
                    Clock.System.now().toEpochMilliseconds(),
                )
            }.onSuccess {
                _effects.send(CharacterEffect.CharacterDeleted)
            }.onFailure { error ->
                _effects.send(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_SAVE_FAILED,
                        detail = error.failureDetail(),
                    ),
                )
            }
        }
    }

    private fun restoreBuiltinCharacters() {
        viewModelScope.launch {
            val hasDeleted = characterRepository.hasDeletedBuiltinCharacters()
            if (hasDeleted) {
                characterRepository.restoreDeletedBuiltinCharacters()
                _effects.send(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_RESTORE_BUILTIN_SUCCESS
                    )
                )
            } else {
                _effects.send(
                    CharacterEffect.ShowMessage(
                        CharacterEffectMessage.CHARACTER_RESTORE_BUILTIN_NO_DELETED
                    )
                )
            }
        }
    }

    private fun exportSelected(directory: io.github.vinceglb.filekit.PlatformFile) {
        val characterId = _uiState.value.pendingLocalExportCharacterId ?: return
        viewModelScope.launch {
            val character = characterRepository.getCharacter(characterId)
            val result = character?.let { characterCardExporter.exportToPng(it, directory) }
            _uiState.update { it.copy(pendingLocalExportCharacterId = null) }
            _effects.send(
                CharacterEffect.ShowMessage(
                    if (result is ApiResult.Success) {
                        CharacterEffectMessage.CHARACTER_EXPORT_SUCCESS
                    } else {
                        CharacterEffectMessage.CHARACTER_EXPORT_FAILED
                    },
                    detail = result?.failureDetail(),
                ),
            )
        }
    }

    private fun requestLocalExport() {
        if (_uiState.value.sharingCharacterId != null) return
        val characterId = _uiState.value.exportDialogCharacterId ?: return
        _uiState.update {
            it.copy(
                exportDialogCharacterId = null,
                pendingLocalExportCharacterId = characterId,
            )
        }
        viewModelScope.launch {
            _effects.send(CharacterEffect.RequestDirectoryPicker)
        }
    }

    private fun loadShareCategories() {
        val characterId = _uiState.value.exportDialogCharacterId ?: return
        if (_uiState.value.isLoadingShareCategories || _uiState.value.sharingCharacterId != null) return
        _uiState.update {
            it.copy(
                exportDialogCharacterId = null,
                shareCategoryDialogCharacterId = characterId,
                shareCategories = emptyList(),
                shareTags = emptyList(),
                shareCategorySelection = null,
                selectedShareTagIds = emptySet(),
                isLoadingShareCategories = true,
            )
        }
        viewModelScope.launch {
            val nickname = appSettingsStore.userNickname.first()
            if (nickname.isBlank()) {
                _uiState.update {
                    it.copy(
                        shareCategoryDialogCharacterId = null,
                        isLoadingShareCategories = false,
                    )
                }
                _effects.send(CharacterEffect.NavigateToProfile)
                return@launch
            }
            if (characterId.startsWith("builtin:")) {
                _uiState.update {
                    it.copy(
                        shareCategoryDialogCharacterId = null,
                        isLoadingShareCategories = false,
                    )
                }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_BUILTIN_RESTRICTED)
                return@launch
            }
            if (!publicCharacterRepository.isSupported) {
                _uiState.update {
                    it.copy(
                        shareCategoryDialogCharacterId = null,
                        isLoadingShareCategories = false,
                    )
                }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_PLATFORM_UNSUPPORTED)
                return@launch
            }
            val catalogResult = publicCharacterCatalogRepository.getCatalog()
            val catalog = (catalogResult as? ApiResult.Success)?.value?.catalog
            val categoriesResult = catalog?.let {
                publicCharacterCatalogRepository.getCategories(it.categoriesUrl)
            }
            val categories = (categoriesResult as? ApiResult.Success)
                ?.value
                ?.categories
                ?.sortedBy { it.sortOrder }
                .orEmpty()
            val tags = catalog?.tags
                ?.sortedBy { it.sortOrder }
                .orEmpty()
            if (categories.isEmpty()) {
                _uiState.update {
                    it.copy(
                        shareCategoryDialogCharacterId = null,
                        isLoadingShareCategories = false,
                    )
                }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_FAILED)
                return@launch
            }

            _uiState.update {
                it.copy(
                    shareCategories = categories,
                    shareTags = tags,
                    isLoadingShareCategories = false,
                )
            }
        }
    }

    private fun selectShareCategory(categoryId: String) {
        val state = _uiState.value
        if (state.sharingCharacterId != null || state.shareCategories.none { it.id == categoryId }) return
        _uiState.update {
            it.copy(shareCategorySelection = ShareCategorySelection.Existing(categoryId))
        }
    }

    private fun selectCustomShareCategory() {
        if (_uiState.value.sharingCharacterId != null) return
        _uiState.update {
            val currentName = (it.shareCategorySelection as? ShareCategorySelection.Proposed)
                ?.name
                .orEmpty()
            it.copy(shareCategorySelection = ShareCategorySelection.Proposed(currentName))
        }
    }

    private fun updateCustomShareCategoryName(name: String) {
        if (_uiState.value.sharingCharacterId != null) return
        _uiState.update {
            it.copy(shareCategorySelection = ShareCategorySelection.Proposed(name))
        }
    }

    private fun toggleShareTag(tagId: String) {
        val state = _uiState.value
        if (state.sharingCharacterId != null || state.shareTags.none { it.id == tagId }) return
        _uiState.update {
            val selectedTagIds = it.selectedShareTagIds.toMutableSet()
            if (!selectedTagIds.add(tagId)) {
                selectedTagIds.remove(tagId)
            }
            it.copy(selectedShareTagIds = selectedTagIds)
        }
    }

    private fun dismissShareCategoryDialog() {
        if (_uiState.value.sharingCharacterId != null || _uiState.value.isLoadingShareCategories) return
        _uiState.update {
            it.copy(
                shareCategoryDialogCharacterId = null,
                shareCategories = emptyList(),
                shareTags = emptyList(),
                shareCategorySelection = null,
                selectedShareTagIds = emptySet(),
                isLoadingShareCategories = false,
            )
        }
    }

    private fun sharePublic() {
        val state = _uiState.value
        val characterId = state.shareCategoryDialogCharacterId ?: return
        val requestedCategory = state.shareCategorySelection ?: return
        if (state.sharingCharacterId != null || state.isLoadingShareCategories) return
        if (!state.canConfirmShareCategory) return
        val categorySelection = when (requestedCategory) {
            is ShareCategorySelection.Existing -> requestedCategory
            is ShareCategorySelection.Proposed -> {
                val name = requestedCategory.name.trim()
                val normalizedName = normalizeShareCategoryName(name)
                state.shareCategories.firstOrNull {
                    normalizeShareCategoryName(it.name) == normalizedName
                }?.let { ShareCategorySelection.Existing(it.id) }
                    ?: ShareCategorySelection.Proposed(name)
            }
        }
        _uiState.update { it.copy(sharingCharacterId = characterId) }
        viewModelScope.launch {
            val nickname = appSettingsStore.userNickname.first()
            val character = characterRepository.getCharacter(characterId)
            if (character == null || nickname.isBlank()) {
                _uiState.update {
                    it.copy(
                        sharingCharacterId = null,
                        shareCategoryDialogCharacterId = null,
                        shareCategories = emptyList(),
                        shareTags = emptyList(),
                        shareCategorySelection = null,
                        selectedShareTagIds = emptySet(),
                    )
                }
                showMessage(CharacterEffectMessage.CHARACTER_SHARE_FAILED)
                return@launch
            }
            val result = publicCharacterRepository.share(
                character = character.copy(author = nickname),
                categorySelection = categorySelection,
                tagIds = state.shareTags
                    .map { it.id }
                    .filter { it in state.selectedShareTagIds },
            )
            _uiState.update { state ->
                state.copy(
                    sharingCharacterId = null,
                    shareCategoryDialogCharacterId = null,
                    shareCategories = emptyList(),
                    shareTags = emptyList(),
                    shareCategorySelection = null,
                    selectedShareTagIds = emptySet(),
                )
            }
            var customMessage: String? = null
            val message = when (result) {
                is ApiResult.Success -> CharacterEffectMessage.CHARACTER_SHARE_SUCCESS
                is ApiResult.UnexpectedError -> when {
                    result.message == DefaultPublicCharacterRepository.ERROR_MEDIA_READ ->
                        CharacterEffectMessage.CHARACTER_SHARE_MEDIA_READ_FAILED
                    result.message?.contains("审核中") == true ->
                        CharacterEffectMessage.CHARACTER_SHARE_REVIEWING
                    result.message == DefaultPublicCharacterRepository.ERROR_PLATFORM_UNSUPPORTED ->
                        CharacterEffectMessage.CHARACTER_SHARE_PLATFORM_UNSUPPORTED
                    else -> {
                        customMessage = result.message
                        CharacterEffectMessage.CHARACTER_SHARE_FAILED
                    }
                }
                is ApiResult.HttpError -> {
                    customMessage = "HTTP ${result.statusCode}: ${result.message}"
                    CharacterEffectMessage.CHARACTER_SHARE_FAILED
                }
                is ApiResult.NetworkError -> {
                    customMessage = "网络错误: ${result.message}"
                    CharacterEffectMessage.CHARACTER_SHARE_FAILED
                }
            }
            showMessage(message, customMessage)
        }
    }

    private fun normalizeShareCategoryName(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase()

    private suspend fun showMessage(message: CharacterEffectMessage, customMessage: String? = null) {
        _effects.send(CharacterEffect.ShowMessage(message, customMessage))
    }
}
