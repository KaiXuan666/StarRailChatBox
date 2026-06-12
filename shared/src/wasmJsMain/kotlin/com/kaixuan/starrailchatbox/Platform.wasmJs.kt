package com.kaixuan.starrailchatbox

import com.kaixuan.starrailchatbox.data.settings.AppConfig

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val versionCode: Int = AppConfig.versionCode
    override val versionName: String = AppConfig.versionName
}

actual fun getPlatform(): Platform = WasmPlatform()