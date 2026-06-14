package com.kaixuan.starrailchatbox.ui.character.catalog

import com.kaixuan.starrailchatbox.data.character.catalog.PublicCategory
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag

data class CharacterCatalogUiState(
    val isLoading: Boolean = false,
    val categories: List<PublicCategory> = emptyList(),
    val tags: List<PublicTag> = emptyList(),
    val selectedCategoryId: String? = null,
    val selectedTagIds: Set<String> = emptySet(),
    val searchQuery: String = "",
    val characters: List<PublicCharacterSummary> = emptyList(),
    val filteredCharacters: List<PublicCharacterSummary> = emptyList(),
    val isTagFilterOpen: Boolean = false,
    val importingCharacterIds: Set<String> = emptySet(),
    val importedCharacterIds: Set<String> = emptySet(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val isPageLoading: Boolean = false,
)
