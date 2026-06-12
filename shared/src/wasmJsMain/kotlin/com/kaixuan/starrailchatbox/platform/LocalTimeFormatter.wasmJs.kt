package com.kaixuan.starrailchatbox.platform

actual fun formatHeaderDate(epochMilliseconds: Long): String {
    return formatBrowserHeaderDate(epochMilliseconds.toDouble())
}

actual fun formatLastChatTime(epochMilliseconds: Long): String {
    return formatBrowserLastChatTime(epochMilliseconds.toDouble())
}

actual fun formatMessageTime(epochMilliseconds: Long): String {
    return formatBrowserMessageTime(epochMilliseconds.toDouble())
}

actual fun isSameDay(time1: Long, time2: Long): Boolean {
    return isBrowserSameDay(time1.toDouble(), time2.toDouble())
}

@JsFun(
    """(epochMilliseconds) => {
        const date = new Date(epochMilliseconds);
        const now = new Date();
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();
        
        const isSameYear = year === now.getFullYear();
        const isSameDay = isSameYear &&
            date.getMonth() === now.getMonth() &&
            day === now.getDate();
            
        if (isSameDay) return "今天";
        
        const yesterday = new Date(now.getTime() - 24 * 60 * 60 * 1000);
        const isYesterday = year === yesterday.getFullYear() &&
            date.getMonth() === yesterday.getMonth() &&
            day === yesterday.getDate();
            
        if (isYesterday) return "昨天";
        
        if (isSameYear) return month + "月" + day + "日";
        return year + "年" + month + "月" + day + "日";
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun formatBrowserHeaderDate(epochMilliseconds: Double): String

@JsFun(
    """(epochMilliseconds) => {
        const date = new Date(epochMilliseconds);
        const year = date.getFullYear();
        const month = date.getMonth() + 1;
        const day = date.getDate();
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        const time = hours + ":" + minutes;
        const now = new Date();
        const isSameYear = year === now.getFullYear();
        const isSameDay = isSameYear &&
            date.getMonth() === now.getMonth() &&
            day === now.getDate();
        if (isSameDay) return time;
        if (isSameYear) return month + "月" + day + "日 " + time;
        return year + "年" + month + "月" + day + "日";
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun formatBrowserLastChatTime(epochMilliseconds: Double): String

@JsFun(
    """(epochMilliseconds) => {
        const date = new Date(epochMilliseconds);
        const hours = String(date.getHours()).padStart(2, '0');
        const minutes = String(date.getMinutes()).padStart(2, '0');
        return hours + ":" + minutes;
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun formatBrowserMessageTime(epochMilliseconds: Double): String

@JsFun(
    """(t1, t2) => {
        const d1 = new Date(t1);
        const d2 = new Date(t2);
        return d1.getFullYear() === d2.getFullYear() &&
               d1.getMonth() === d2.getMonth() &&
               d1.getDate() === d2.getDate();
    }""",
)
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private external fun isBrowserSameDay(t1: Double, t2: Double): Boolean
