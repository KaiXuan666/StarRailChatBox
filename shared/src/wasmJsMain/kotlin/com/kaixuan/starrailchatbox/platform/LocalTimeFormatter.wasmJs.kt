package com.kaixuan.starrailchatbox.platform

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun formatLocalTime(epochMilliseconds: Long): String {
    return formatBrowserLocalTime(epochMilliseconds.toDouble())
}

@JsFun(
    """(epochMilliseconds) => {
        const date = new Date(epochMilliseconds);
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return hours + ":" + minutes;
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun formatBrowserLocalTime(epochMilliseconds: Double): String
