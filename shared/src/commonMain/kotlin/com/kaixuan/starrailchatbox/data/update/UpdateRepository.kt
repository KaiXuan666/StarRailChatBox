package com.kaixuan.starrailchatbox.data.update

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.getPlatform
import com.kaixuan.starrailchatbox.PlatformType
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import com.kaixuan.starrailchatbox.data.settings.LocalApiSettings
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import okio.Path
import io.ktor.client.request.prepareGet
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import com.kaixuan.starrailchatbox.platform.KmpFileManager

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
    
    suspend fun downloadUpdate(
        url: String,
        targetPath: Path,
        onProgress: (progress: Float) -> Unit
    ): ApiResult<Unit>
}

class DefaultUpdateRepository(
    private val httpClient: HttpClient
) : UpdateRepository {
    override suspend fun checkUpdate(isManual: Boolean): ApiResult<UpdateResponse> {
        return try {
            val url = when (getPlatform().type) {
                PlatformType.Android -> {
                    LocalApiSettings.androidUpdateUrl.takeIf { it.isNotEmpty() }
                        ?: "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/android.json"
                }
                PlatformType.Windows -> {
                    LocalApiSettings.windowsUpdateUrl.takeIf { it.isNotEmpty() }
                        ?: "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/windows.json"
                }
                else -> {
                    LocalApiSettings.androidUpdateUrl.takeIf { it.isNotEmpty() }
                        ?: "https://cdn.jsdelivr.net/gh/KaiXuan666/StarRailChatBox@main/update/android.json"
                }
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

    override suspend fun downloadUpdate(
        url: String,
        targetPath: Path,
        onProgress: (progress: Float) -> Unit
    ): ApiResult<Unit> {
        return try {
            httpClient.prepareGet(url).execute { response ->
                if (response.status.value !in 200..299) {
                    return@execute ApiResult.HttpError(response.status.value, response.status.description)
                }
                val contentLength = response.headers["Content-Length"]?.toLongOrNull() ?: -1L
                val channel: ByteReadChannel = response.body()
                var bytesCopied = 0L

                KmpFileManager.Default.fileSystem.write(targetPath) {
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8192)
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            write(bytes)
                            bytesCopied += bytes.size
                            if (contentLength > 0) {
                                onProgress(bytesCopied.toFloat() / contentLength)
                            }
                        }
                    }
                }
                ApiResult.Success(Unit)
            }
        } catch (e: Exception) {
            ApiResult.NetworkError(e.message)
        }
    }
}
