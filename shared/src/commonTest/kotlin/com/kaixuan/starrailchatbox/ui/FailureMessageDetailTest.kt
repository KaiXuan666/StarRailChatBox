package com.kaixuan.starrailchatbox.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class FailureMessageDetailTest {
    @Test
    fun appendFailureDetailCanBeDisabled() {
        FailureMessageDetailOptions.enabled = false
        try {
            assertEquals(
                "消息发送失败，请检查网络和 API 配置",
                appendFailureDetail("消息发送失败，请检查网络和 API 配置", "timeout"),
            )
        } finally {
            FailureMessageDetailOptions.enabled = true
        }
    }

    @Test
    fun appendFailureDetailAddsDetailWhenEnabled() {
        FailureMessageDetailOptions.enabled = true

        assertEquals(
            "角色保存失败：File empty",
            appendFailureDetail("角色保存失败", "File empty"),
        )
    }
}
