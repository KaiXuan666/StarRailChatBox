package com.kaixuan.starrailchatbox.data.update

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.getPlatform
import com.kaixuan.starrailchatbox.PlatformType
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
    @SerialName("cloudStorageUpdateText") val cloudStorageUpdateText: String? = null,
    @SerialName("forceUpdate") val forceUpdate: Boolean
)

interface UpdateRepository {
    suspend fun checkUpdate(isManual: Boolean): ApiResult<UpdateResponse>
}

class DefaultUpdateRepository(
    private val httpClient: HttpClient
) : UpdateRepository {
    override suspend fun checkUpdate(isManual: Boolean): ApiResult<UpdateResponse> {
        return try {
            val url = when (getPlatform().type) {
                PlatformType.Android -> "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/android.json"
                PlatformType.Windows -> "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/windows.json"
                else -> "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/android.json"
            }
            val response = httpClient.get(url)
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
