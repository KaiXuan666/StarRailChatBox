package com.kaixuan.starrailchatbox

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform