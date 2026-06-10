package com.kaixuan.starrailchatbox

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = JVMPlatform()