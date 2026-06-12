package com.kaixuan.starrailchatbox

import platform.UIKit.UIDevice

import com.kaixuan.starrailchatbox.data.settings.AppConfig

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val type: PlatformType = PlatformType.Ios
    override val versionCode: Int = AppConfig.versionCode
    override val versionName: String = AppConfig.versionName
}

actual fun getPlatform(): Platform = IOSPlatform()