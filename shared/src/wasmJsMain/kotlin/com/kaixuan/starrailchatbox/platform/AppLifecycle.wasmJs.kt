package com.kaixuan.starrailchatbox.platform

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun restartApp() {
    // Web can reload the page
    js("window.location.reload()")
}
