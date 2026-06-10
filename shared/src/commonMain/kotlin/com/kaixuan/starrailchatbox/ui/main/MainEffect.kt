package com.kaixuan.starrailchatbox.ui.main

sealed interface MainEffect {
    data class ShowMessage(val message: MainEffectMessage) : MainEffect
}

enum class MainEffectMessage {
    THEME_CHANGED,
    IMAGE_SAVED,
    IMAGE_SAVE_FAILED
}
