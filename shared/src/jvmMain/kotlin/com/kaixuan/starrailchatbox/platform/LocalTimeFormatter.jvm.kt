package com.kaixuan.starrailchatbox.platform

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }

    val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    if (isSameDay) return "今天"

    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val isYesterday = isSameYear && yesterday.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)

    if (isYesterday) return "昨天"

    return if (isSameYear) {
        SimpleDateFormat("M月d日", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("yyyy年M月d日", Locale.getDefault()).format(date)
    }
}

actual fun formatLastChatTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds)
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply { time = date }
    val isSameYear = now.get(Calendar.YEAR) == target.get(Calendar.YEAR)
    val isSameDay = isSameYear && now.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR)
    val pattern = when {
        isSameDay -> "HH:mm"
        isSameYear -> "M月d日 HH:mm"
        else -> "yyyy年M月d日"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(date)
}

actual fun formatMessageTime(epochMilliseconds: Long): String {
    val date = Date(epochMilliseconds)
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(date)
}

actual fun isSameDay(time1: Long, time2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = time1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = time2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
