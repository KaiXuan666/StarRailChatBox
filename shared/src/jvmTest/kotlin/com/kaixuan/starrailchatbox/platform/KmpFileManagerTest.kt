package com.kaixuan.starrailchatbox.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class KmpFileManagerTest {
    @Test
    fun testFileOperations() {
        val fileManager = KmpFileManager.Default
        assertTrue(fileManager.isSupported)
        
        val testFile = "test_file.txt"
        val content = "Hello StarRail ChatBox!"
        
        // Ensure clean state
        fileManager.delete(testFile)
        
        fileManager.writeText(testFile, content)
        assertTrue(fileManager.exists(testFile))
        
        val readContent = fileManager.readText(testFile)
        assertEquals(content, readContent)
        
        fileManager.delete(testFile)
        assertFalse(fileManager.exists(testFile))
    }
}
