package com.kaixuan.starrailchatbox.platform

import java.util.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeFormatterTest {
    @Test
    fun formatsEpochUsingDeviceTimeZone() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+08:00"))

            assertEquals("1970年1月1日 08:00", formatLastChatTime(0L))
        } finally {
            TimeZone.setDefault(original)
        }
    }
}
