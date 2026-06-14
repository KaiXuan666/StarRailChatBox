package com.kaixuan.starrailchatbox.ui.character.catalog

sealed interface CharacterCatalogEffect {
    data class ShowToast(val message: String) : CharacterCatalogEffect
}
