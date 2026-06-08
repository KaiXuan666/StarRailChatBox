package com.kaixuan.starrailchatbox.platform

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

actual fun formatLocalTime(epochMilliseconds: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMilliseconds))
}

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
        SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(date)
    } else {
        SimpleDateFormat("yyyy年M月d日 HH:mm", Locale.getDefault()).format(date)
    }
}
