package com.kaixuan.starrailchatbox.ui.chat

import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationManagementScreenTest {
    @Test
    fun companionDaysCountsCreationDayAsFirstDay() {
        val day = 24L * 60L * 60L * 1_000L

        assertEquals(1, calculateCompanionDays(createdAt = 1_000L, now = 1_000L))
        assertEquals(2, calculateCompanionDays(createdAt = 1_000L, now = 1_000L + day))
        assertEquals(0, calculateCompanionDays(createdAt = 0L, now = day))
        assertEquals(0, calculateCompanionDays(createdAt = day, now = 1_000L))
    }
}
