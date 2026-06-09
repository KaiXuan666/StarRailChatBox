package com.kaixuan.starrailchatbox.platform

actual fun restartApp() {
    // Web can reload the page
    kotlinx.browser.window.location.reload()
}
