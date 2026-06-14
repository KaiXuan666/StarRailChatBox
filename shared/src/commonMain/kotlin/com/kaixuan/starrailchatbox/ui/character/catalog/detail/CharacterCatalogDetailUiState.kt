package com.kaixuan.starrailchatbox.ui.character.catalog.detail

import androidx.compose.runtime.Immutable
import com.kaixuan.starrailchatbox.data.character.catalog.PublicCharacterDetail
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag

@Immutable
data class CharacterCatalogDetailUiState(
    val characterId: String,
    val detailUrl: String,
    val initialName: String = "",
    val initialAvatarUrl: String? = null,
    val isLoading: Boolean = true,
    val detail: PublicCharacterDetail? = null,
    val tags: List<PublicTag> = emptyList(),
    val isImported: Boolean = false,
    val isImporting: Boolean = false,
    val isVoiceDownloading: Boolean = false,
    val voiceSampleLocalPath: String? = null,
)
