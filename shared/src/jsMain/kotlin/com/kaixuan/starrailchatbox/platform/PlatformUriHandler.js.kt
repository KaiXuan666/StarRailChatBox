package com.kaixuan.starrailchatbox.platform

import kotlinx.browser.window

actual fun openUri(uri: String, mimeType: String?) {
    window.open(uri, "_blank")
}
