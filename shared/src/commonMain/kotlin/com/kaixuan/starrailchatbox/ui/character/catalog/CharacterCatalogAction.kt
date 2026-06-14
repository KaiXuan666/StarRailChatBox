package com.kaixuan.starrailchatbox.ui.character.catalog

import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary

sealed interface CharacterCatalogAction {
    data object LoadCatalog : CharacterCatalogAction
    data class SelectCategory(val categoryId: String) : CharacterCatalogAction
    data class ToggleTag(val tagId: String) : CharacterCatalogAction
    data object ClearTags : CharacterCatalogAction
    data class SearchQueryChanged(val query: String) : CharacterCatalogAction
    data class ImportCharacterClicked(val character: PublicCharacterSummary) : CharacterCatalogAction
    data object ToggleTagFilter : CharacterCatalogAction
    data object LoadNextPage : CharacterCatalogAction
}
