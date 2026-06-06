package com.kaixuan.starrailchatbox.ui.components

import androidx.compose.runtime.Composable

@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // No-op for Desktop
}
