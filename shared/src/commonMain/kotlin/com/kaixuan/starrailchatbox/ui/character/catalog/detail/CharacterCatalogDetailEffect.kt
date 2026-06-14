package com.kaixuan.starrailchatbox.ui.character.catalog.detail

sealed interface CharacterCatalogDetailEffect {
    data class ShowToast(val message: String) : CharacterCatalogDetailEffect
}
