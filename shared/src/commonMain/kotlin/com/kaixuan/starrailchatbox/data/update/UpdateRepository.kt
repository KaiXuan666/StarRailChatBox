package com.kaixuan.starrailchatbox.data.update

import com.kaixuan.starrailchatbox.data.api.ApiResult
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UpdateResponse(
    @SerialName("versionCode") val versionCode: Int,
    @SerialName("versionName") val versionName: String,
    @SerialName("updateLog") val updateLog: String,
    @SerialName("downloadUrl") val downloadUrl: String,
    @SerialName("forceUpdate") val forceUpdate: Boolean
)

interface UpdateRepository {
    suspend fun checkUpdate(): ApiResult<UpdateResponse>
}

class DefaultUpdateRepository(
    private val httpClient: HttpClient
) : UpdateRepository {
    override suspend fun checkUpdate(): ApiResult<UpdateResponse> {
        return try {
            val response = httpClient.get("https://raw.githubusercontent.com/KaiXuan666/StarRailChatBox/refs/heads/main/update.json")
            if (response.status.value in 200..299) {
                ApiResult.Success(response.body<UpdateResponse>())
            } else {
                ApiResult.HttpError(response.status.value, response.status.description)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message)
        }
    }
}
