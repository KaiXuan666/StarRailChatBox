package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.material3.MaterialTheme
import coil3.compose.AsyncImage
import starrailchatbox.shared.generated.resources.Res
import starrailchatbox.shared.generated.resources.avatar_default_ai

@Composable
fun AvatarImage(
    avatarUri: String,
    contentDescription: String?,
    placeholderKind: StarRailIconKind,
    placeholderSize: Dp,
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUri.isNotBlank()) {
            AsyncImage(
                model = avatarUri,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else if (isUser) {
            StarRailIcon(
                kind = placeholderKind,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(placeholderSize),
            )
        } else {
            androidx.compose.foundation.Image(
                painter = org.jetbrains.compose.resources.painterResource(Res.drawable.avatar_default_ai),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}
