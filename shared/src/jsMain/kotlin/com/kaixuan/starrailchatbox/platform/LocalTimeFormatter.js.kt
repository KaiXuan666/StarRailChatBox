package com.kaixuan.starrailchatbox.platform

import kotlin.js.Date

actual fun formatLocalTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds)
    return date.getHours().toString().padStart(2, '0') + ":" +
        date.getMinutes().toString().padStart(2, '0')
}
