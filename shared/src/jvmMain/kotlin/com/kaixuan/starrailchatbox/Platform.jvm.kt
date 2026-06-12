package com.kaixuan.starrailchatbox

import com.kaixuan.starrailchatbox.data.settings.AppConfig

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val type: PlatformType = PlatformType.Windows
    override val versionCode: Int = AppConfig.versionCode
    override val versionName: String = AppConfig.versionName
}

actual fun getPlatform(): Platform = JVMPlatform()