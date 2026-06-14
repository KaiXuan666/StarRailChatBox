package com.kaixuan.starrailchatbox.platform

actual fun getFormattedDateTime(): String {
    // Web WasmJs 仅需保证接口签名一致，不实际执行文件导出
    return "202606140000"
}
