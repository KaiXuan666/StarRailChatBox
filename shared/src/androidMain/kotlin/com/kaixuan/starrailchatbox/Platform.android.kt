package com.kaixuan.starrailchatbox

import android.os.Build

import com.kaixuan.starrailchatbox.data.settings.AppConfig

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val type: PlatformType = PlatformType.Android
    override val versionCode: Int = AppConfig.versionCode
    override val versionName: String = AppConfig.versionName
}

actual fun getPlatform(): Platform = AndroidPlatform()