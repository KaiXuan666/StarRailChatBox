package com.kaixuan.starrailchatbox.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openUri(uri: String, mimeType: String?) {
    val nsUrl = NSURL.URLWithString(uri) ?: return
    if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
