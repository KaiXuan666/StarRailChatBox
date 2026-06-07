package com.kaixuan.starrailchatbox.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun formatLocalTime(epochMilliseconds: Long): String {
    return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(epochMilliseconds))
}
