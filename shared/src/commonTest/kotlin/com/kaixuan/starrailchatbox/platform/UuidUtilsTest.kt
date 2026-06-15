@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package com.kaixuan.starrailchatbox.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class UuidUtilsTest {

    @Test
    fun testRandomUuidFormatAndUniqueness() {
        val uuid1 = Uuid.random().toString()
        val uuid2 = Uuid.random().toString()

        // 验证非空且不重复
        assertNotEquals(uuid1, uuid2)

        // 验证长度为 36 字符
        assertEquals(36, uuid1.length)
        assertEquals(36, uuid2.length)

        // 验证符合 UUID v4 格式：8-4-4-4-12 结构
        val pattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[4][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        assertTrue(pattern.matches(uuid1), "uuid1: $uuid1 does not match UUID v4 format")
        assertTrue(pattern.matches(uuid2), "uuid2: $uuid2 does not match UUID v4 format")
    }
}
