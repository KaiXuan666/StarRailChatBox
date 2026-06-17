package com.kaixuan.starrailchatbox.ui

import com.kaixuan.starrailchatbox.data.api.ApiResult

object FailureMessageDetailOptions {
    var enabled: Boolean = true
}

fun appendFailureDetail(baseMessage: String, detail: String?): String {
    val cleanDetail = detail?.trim()?.takeIf { it.isNotEmpty() } ?: return baseMessage
    if (!FailureMessageDetailOptions.enabled) return baseMessage
    if (baseMessage.contains(cleanDetail)) return baseMessage
    return "$baseMessage：$cleanDetail"
}

fun Throwable.failureDetail(): String? =
    message?.trim()?.takeIf { it.isNotEmpty() }

fun ApiResult<*>.failureDetail(): String? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.HttpError -> listOfNotNull(
        "HTTP $statusCode",
        message?.trim()?.takeIf { it.isNotEmpty() },
    ).joinToString(": ")
    is ApiResult.NetworkError -> message?.trim()?.takeIf { it.isNotEmpty() }
    is ApiResult.UnexpectedError -> message?.trim()?.takeIf { it.isNotEmpty() }
}
