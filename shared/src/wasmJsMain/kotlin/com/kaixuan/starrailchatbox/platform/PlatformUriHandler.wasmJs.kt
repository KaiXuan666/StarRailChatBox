package com.kaixuan.starrailchatbox.platform

import kotlinx.browser.window

actual fun openUri(uri: String, mimeType: String?) {
    window.open(uri, "_blank")
}

actual fun installPackage(path: String) {
    // Web doesn't support local package installation
}

