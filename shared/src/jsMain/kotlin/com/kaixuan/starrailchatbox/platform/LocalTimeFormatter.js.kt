package com.kaixuan.starrailchatbox.platform

import kotlin.js.Date

actual fun formatLocalTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds.toDouble())
    return date.getHours().toString().padStart(2, '0') + ":" +
        date.getMinutes().toString().padStart(2, '0')
}

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds.toDouble())
    // 简略实现
    return "${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日 ${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}"
}

actual fun isSameDay(time1: Long, time2: Long): Boolean {
    val d1 = Date(time1.toDouble())
    val d2 = Date(time2.toDouble())
    return d1.getFullYear() == d2.getFullYear() &&
        d1.getMonth() == d2.getMonth() &&
        d1.getDate() == d2.getDate()
}
