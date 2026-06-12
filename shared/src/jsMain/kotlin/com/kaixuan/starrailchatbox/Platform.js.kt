package com.kaixuan.starrailchatbox

import web.navigator.navigator

import com.kaixuan.starrailchatbox.data.settings.AppConfig

class JsPlatform: Platform {
    private val userAgent = navigator.userAgent
    private val browserList = listOf("Chrome", "Firefox", "Safari", "Edge")

    override val name: String = userAgent.findAnyOf(browserList, ignoreCase = true)
            ?.let { (startIndex) -> userAgent.substring(startIndex).substringBefore(" ") }
            ?: "Unknown"
    override val versionCode: Int = AppConfig.versionCode
    override val versionName: String = AppConfig.versionName
}

actual fun getPlatform(): Platform = JsPlatform()