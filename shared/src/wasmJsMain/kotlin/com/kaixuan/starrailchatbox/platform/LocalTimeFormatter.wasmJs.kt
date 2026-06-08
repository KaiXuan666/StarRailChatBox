package com.kaixuan.starrailchatbox.platform

@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
actual fun formatLocalTime(epochMilliseconds: Long): String {
    return formatBrowserLocalTime(epochMilliseconds.toDouble())
}

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    return formatBrowserHeaderDate(epochMilliseconds.toDouble())
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

@JsFun(
    """(epochMilliseconds) => {
        const date = new Date(epochMilliseconds);
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return year + "年" + month + "月" + day + "日 " + hours + ":" + minutes;
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun formatBrowserHeaderDate(epochMilliseconds: Double): String
