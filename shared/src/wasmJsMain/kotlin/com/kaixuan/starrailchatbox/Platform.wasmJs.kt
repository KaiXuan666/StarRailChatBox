package com.kaixuan.starrailchatbox

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = WasmPlatform()