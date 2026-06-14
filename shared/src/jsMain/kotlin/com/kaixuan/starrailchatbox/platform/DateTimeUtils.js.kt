package com.kaixuan.starrailchatbox.platform

actual fun getFormattedDateTime(): String {
    val date = kotlin.js.Date()
    val pad = { n: Int -> if (n < 10) "0$n" else "$n" }
    val yyyy = date.getFullYear()
    val MM = pad(date.getMonth() + 1)
    val dd = pad(date.getDate())
    val HH = pad(date.getHours())
    val mm = pad(date.getMinutes())
    return "$yyyy$MM$dd$HH$mm"
}
