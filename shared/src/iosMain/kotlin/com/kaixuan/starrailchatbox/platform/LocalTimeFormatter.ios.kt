package com.kaixuan.starrailchatbox.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970

actual fun formatLocalTime(epochMilliseconds: Long): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "HH:mm"
        locale = NSLocale.currentLocale
    }
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    return formatter.stringFromDate(date)
}
