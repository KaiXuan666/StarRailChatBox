package com.kaixuan.starrailchatbox.platform

import android.content.Intent
import com.kaixuan.starrailchatbox.data.database.AndroidContextHolder
import kotlin.system.exitProcess

actual fun restartApp() {
    val context = AndroidContextHolder.context ?: return
    val packageManager = context.packageManager
    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
    val componentName = intent?.component
    val mainIntent = Intent.makeRestartActivityTask(componentName)
    context.startActivity(mainIntent)
    exitProcess(0)
}
