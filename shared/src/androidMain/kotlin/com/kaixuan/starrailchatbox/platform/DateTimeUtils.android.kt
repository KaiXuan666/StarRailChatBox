package com.kaixuan.starrailchatbox.platform

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun getFormattedDateTime(): String {
    val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.getDefault())
    return sdf.format(Date())
}
