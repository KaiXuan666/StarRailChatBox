package com.kaixuan.starrailchatbox

import web.navigator.navigator

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
    override val versionCode: Int = 1
    override val versionName: String = "1.0.0"
}

actual fun getPlatform(): Platform = JsPlatform()