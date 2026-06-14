package com.kaixuan.starrailchatbox.ui.character.catalog.detail

sealed interface CharacterCatalogDetailAction {
    data object LoadDetail : CharacterCatalogDetailAction
    data object ImportClicked : CharacterCatalogDetailAction
    data object PlayVoiceClicked : CharacterCatalogDetailAction
}
