package com.kaixuan.starrailchatbox

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val versionCode: Int = 1
}

actual fun getPlatform(): Platform = AndroidPlatform()