package com.kaixuan.starrailchatbox.data.api

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import android.content.pm.ApplicationInfo
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual fun createPlatformHttpClient(): HttpClient {
    AndroidHttpLogger.initialize()
    return HttpClient(OkHttp) {
        configureOpenAiClient()
    }
}

actual fun saveNetworkLog(message: String) {
    AndroidHttpFileLogger.log(message)
}

private object AndroidHttpLogger {
    private var initialized = false

    fun initialize() {
        if (initialized) return
        Napier.base(DebugAntilog())
        initialized = true
    }
}

private object AndroidHttpFileLogger {
    private var currentLogFile: File? = null

    @Synchronized
    fun log(message: String) {
        val context = AndroidContextHolder.context ?: return
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!isDebuggable) return

        val logDir = context.filesDir.resolve("log")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }

        if (message.startsWith("REQUEST")) {
            cleanOldLogs(logDir, "req_")
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            currentLogFile = File(logDir, "req_${timeStr}.log")
            currentLogFile?.writeText(message + "\n")
        } else if (message.startsWith("RESPONSE")) {
            cleanOldLogs(logDir, "res_")
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(Date())
            currentLogFile = File(logDir, "res_${timeStr}.log")
            currentLogFile?.writeText(message + "\n")
        } else {
            currentLogFile?.appendText(message + "\n")
        }
    }

    private fun cleanOldLogs(logDir: File, prefix: String) {
        try {
            val files = logDir.listFiles { _, name -> name.startsWith(prefix) && name.endsWith(".log") }
            if (files != null && files.size >= 10) {
                files.sortBy { it.lastModified() }
                val deleteCount = files.size - 9
                for (i in 0 until deleteCount) {
                    files[i].delete()
                }
            }
        } catch (e: Exception) {
            // No-op
        }
    }
}
