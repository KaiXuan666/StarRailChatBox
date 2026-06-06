package com.kaixuan.starrailchatbox.data.api

sealed interface ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>

    data class HttpError(
        val statusCode: Int,
        val message: String?,
    ) : ApiResult<Nothing>

    data class NetworkError(val message: String?) : ApiResult<Nothing>

    data class UnexpectedError(val message: String?) : ApiResult<Nothing>
}
