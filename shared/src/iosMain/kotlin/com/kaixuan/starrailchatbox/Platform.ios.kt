package com.kaixuan.starrailchatbox

import platform.UIKit.UIDevice

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val versionCode: Int = 1
}

actual fun getPlatform(): Platform = IOSPlatform()