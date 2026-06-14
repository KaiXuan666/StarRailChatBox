package com.kaixuan.starrailchatbox.ui.character.catalog

import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterSummary

sealed interface CharacterCatalogAction {
    data object LoadCatalog : CharacterCatalogAction
    data object SelectAll : CharacterCatalogAction
    data class SelectCategory(val categoryId: String) : CharacterCatalogAction
    data class ToggleTag(val tagId: String) : CharacterCatalogAction
    data object ClearTags : CharacterCatalogAction
    data class SearchQueryChanged(val query: String) : CharacterCatalogAction
    data class ImportCharacterClicked(val character: PublicCharacterSummary) : CharacterCatalogAction
    data object ToggleTagFilter : CharacterCatalogAction
    data object LoadNextPage : CharacterCatalogAction
    data object TitleClicked : CharacterCatalogAction
    data class AdminKeyChanged(val value: String) : CharacterCatalogAction
    data object ConfirmAdminKey : CharacterCatalogAction
    data object DismissAdminKeyDialog : CharacterCatalogAction
    data object DisableAdminMode : CharacterCatalogAction
    data object CreateCategoryClicked : CharacterCatalogAction
    data class CategoryNameChanged(val value: String) : CharacterCatalogAction
    data object ConfirmCreateCategory : CharacterCatalogAction
    data object DismissCreateCategoryDialog : CharacterCatalogAction
    data class MoveCharacterClicked(val character: PublicCharacterSummary) : CharacterCatalogAction
    data object DismissMoveCharacterDialog : CharacterCatalogAction
    data class ConfirmMoveCharacter(val categoryId: String) : CharacterCatalogAction
    data class DeleteCharacterClicked(val character: PublicCharacterSummary) : CharacterCatalogAction
    data object DismissDeleteCharacterDialog : CharacterCatalogAction
    data object ConfirmDeleteCharacter : CharacterCatalogAction
}
