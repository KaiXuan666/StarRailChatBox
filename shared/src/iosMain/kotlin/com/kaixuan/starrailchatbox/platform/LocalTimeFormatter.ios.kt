package com.kaixuan.starrailchatbox.platform

import platform.Foundation.NSCalendar
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

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    val calendar = NSCalendar.currentCalendar

    if (calendar.isDateInToday(date)) return "今天"
    if (calendar.isDateInYesterday(date)) return "昨天"

    val isThisYear = calendar.component(platform.Foundation.NSCalendarUnitYear, fromDate = date) ==
        calendar.component(platform.Foundation.NSCalendarUnitYear, fromDate = NSDate())

    val formatter = NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        dateFormat = if (isThisYear) {
            "M月d日 HH:mm"
        } else {
            "yyyy年M月d日 HH:mm"
        }
    }
    return formatter.stringFromDate(date)
}

actual fun formatLastChatTime(epochMilliseconds: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(epochMilliseconds / 1_000.0)
    val calendar = NSCalendar.currentCalendar
    val isToday = calendar.isDateInToday(date)
    val isThisYear = calendar.component(platform.Foundation.NSCalendarUnitYear, fromDate = date) ==
        calendar.component(platform.Foundation.NSCalendarUnitYear, fromDate = NSDate())
    val formatter = NSDateFormatter().apply {
        locale = NSLocale.currentLocale
        dateFormat = when {
            isToday -> "HH:mm"
            isThisYear -> "M月d日 HH:mm"
            else -> "yyyy年M月d日 HH:mm"
        }
    }
    return formatter.stringFromDate(date)
}

actual fun isSameDay(time1: Long, time2: Long): Boolean {
    val date1 = NSDate.dateWithTimeIntervalSince1970(time1 / 1_000.0)
    val date2 = NSDate.dateWithTimeIntervalSince1970(time2 / 1_000.0)
    return NSCalendar.currentCalendar.isDate(date1, inSameDayAsDate = date2)
}
