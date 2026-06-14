package com.kaixuan.starrailchatbox.ui.character.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.data.character.catalog.PublicTag

internal fun resolveCharacterTagNames(
    tagIds: List<String>,
    tags: List<PublicTag>,
): List<String> {
    val namesById = tags.associate { it.id to it.name }
    return tagIds.distinct().map { tagId ->
        namesById[tagId].orEmpty().ifBlank { tagId }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CharacterTagChips(
    tagNames: List<String>,
    modifier: Modifier = Modifier,
) {
    if (tagNames.isEmpty()) return

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tagNames.forEach { tagName ->
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
            ) {
                Text(
                    text = tagName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
    }
}
