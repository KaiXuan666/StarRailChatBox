package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kaixuan.starrailchatbox.design.StarRailTheme

/**
 * 导航切换至未实现或仅占位的目的地时所呈现的通用组件。
 */
@Composable
fun NavigationPlaceholderScreen(
    title: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(widthDp = 360, heightDp = 640)
@Composable
private fun NavigationPlaceholderScreenPreview() {
    StarRailTheme {
        NavigationPlaceholderScreen(
            title = "功能开发中...",
            contentPadding = PaddingValues(0.dp)
        )
    }
}
