package com.kaixuan.starrailchatbox

interface Platform {
    val name: String
    val versionCode: Int
}

expect fun getPlatform(): Platform