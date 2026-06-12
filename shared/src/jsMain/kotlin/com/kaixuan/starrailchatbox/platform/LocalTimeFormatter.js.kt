package com.kaixuan.starrailchatbox.platform

import kotlin.js.Date

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds.toDouble())
    val now = Date()
    val isSameYear = date.getFullYear() == now.getFullYear()
    val isSameDay = isSameYear &&
        date.getMonth() == now.getMonth() &&
        date.getDate() == now.getDate()

    if (isSameDay) return "今天"

    val yesterday = Date(now.getTime() - 24 * 60 * 60 * 1000)
    val isYesterday = date.getFullYear() == yesterday.getFullYear() &&
        date.getMonth() == yesterday.getMonth() &&
        date.getDate() == yesterday.getDate()

    if (isYesterday) return "昨天"

    return if (isSameYear) {
        "${date.getMonth() + 1}月${date.getDate()}日"
    } else {
        "${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日"
    }
}

actual fun formatLastChatTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds.toDouble())
    val now = Date()
    val time = "${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}"
    val isSameYear = date.getFullYear() == now.getFullYear()
    val isSameDay = isSameYear &&
        date.getMonth() == now.getMonth() &&
        date.getDate() == now.getDate()
    return when {
        isSameDay -> time
        isSameYear -> "${date.getMonth() + 1}月${date.getDate()}日 $time"
        else -> "${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日"
    }
}

actual fun formatMessageTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds.toDouble())
    return "${date.getHours().toString().padStart(2, '0')}:${date.getMinutes().toString().padStart(2, '0')}"
}

actual fun isSameDay(time1: Long, time2: Long): Boolean {
    val d1 = Date(time1.toDouble())
    val d2 = Date(time2.toDouble())
    return d1.getFullYear() == d2.getFullYear() &&
        d1.getMonth() == d2.getMonth() &&
        d1.getDate() == d2.getDate()
}
