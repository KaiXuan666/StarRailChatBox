package com.kaixuan.starrailchatbox.platform

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

actual fun getFormattedDateTime(): String {
    val formatter = NSDateFormatter()
    formatter.dateFormat = "yyyyMMddHHmm"
    return formatter.stringFromDate(NSDate())
}
