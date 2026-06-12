package com.kaixuan.starrailchatbox.platform

expect fun formatHeaderDate(epochMilliseconds: Long): String

expect fun formatLastChatTime(epochMilliseconds: Long): String

expect fun isSameDay(time1: Long, time2: Long): Boolean
