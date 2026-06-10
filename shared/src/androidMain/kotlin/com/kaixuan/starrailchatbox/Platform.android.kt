package com.kaixuan.starrailchatbox

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = AndroidPlatform()