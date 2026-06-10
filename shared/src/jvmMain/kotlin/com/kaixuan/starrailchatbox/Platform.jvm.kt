package com.kaixuan.starrailchatbox

class JVMPlatform: Platform {
    override val name: String = "Java ${System.getProperty("java.version")}"
    override val versionCode: Int = 1
}

actual fun getPlatform(): Platform = JVMPlatform()