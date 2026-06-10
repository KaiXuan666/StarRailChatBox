package com.kaixuan.starrailchatbox

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = IOSPlatform()