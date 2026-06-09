package com.kaixuan.starrailchatbox.platform

import kotlin.system.exitProcess

actual fun restartApp() {
    // For Desktop, we often can't easily restart without platform-specific launchers.
    // As a fallback, we exit and let the user restart, or if running from gradle/scripts,
    // they might have auto-restart.
    // In a more complete implementation, we'd use something like:
    // Runtime.getRuntime().addShutdownHook(Thread { ... restart command ... })
    exitProcess(0)
}
