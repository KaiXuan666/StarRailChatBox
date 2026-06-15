package com.kaixuan.starrailchatbox.data.character.sharing

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.api.SuppressNetworkLogging
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.aakira.napier.Napier
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ResponseException
import io.ktor.client.request.post
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.Buffer
import okio.ByteString.Companion.toByteString
import kotlin.time.TimeSource

@Serializable
data class PublicCharacterManifest(
    val id: String,
    val name: String,
    val author: String,
    val description: String,
    val systemPrompt: String,
    val openingMessage: String,
    val temperature: Double,
    val topP: Double,
    val avatarUrl: String? = null,
    val voiceSampleUrl: String? = null,
)

@Serializable
private data class SubmissionRequest(
    val characterId: String,
    val author: String,
    val primaryCategoryId: String? = null,
    val primaryCategoryName: String? = null,
    val tagIds: List<String>,
    val contentFingerprint: String,
    val packageSize: Int,
    val updateToken: String? = null,
)

@Serializable
private data class UploadForm(
    val url: String,
    val method: String,
    val fields: Map<String, String>,
    val expiresAt: String,
)

@Serializable
private data class SubmissionResponse(
    val success: Boolean,
    val code: String? = null,
    val message: String? = null,
    val submissionId: String? = null,
    val characterKey: String? = null,
    val updateToken: String? = null,
    val upload: UploadForm? = null,
)

sealed interface ShareCategorySelection {
    data class Existing(val id: String) : ShareCategorySelection
    data class Proposed(val name: String) : ShareCategorySelection
}

interface PublicCharacterRepository {
    val isSupported: Boolean

    suspend fun share(
        character: Character,
        categorySelection: ShareCategorySelection,
        tagIds: List<String>,
    ): ApiResult<Unit>
}

class DefaultPublicCharacterRepository(
    private val httpClient: HttpClient,
    private val fileManager: KmpFileManager,
    private val appSettingsStore: AppSettingsStore,
    private val archiveWriter: CharacterArchiveWriter = createCharacterArchiveWriter(),
    private val json: Json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = true
    },
) : PublicCharacterRepository {
    override val isSupported: Boolean
        get() = archiveWriter.isSupported

    override suspend fun share(
        character: Character,
        categorySelection: ShareCategorySelection,
        tagIds: List<String>,
    ): ApiResult<Unit> {
        if (!isSupported) {
            return ApiResult.UnexpectedError(ERROR_PLATFORM_UNSUPPORTED)
        }
        if (character.prompt.isBlank()) {
            return ApiResult.UnexpectedError(ERROR_SYSTEM_PROMPT_REQUIRED)
        }
        var stage = ShareStage.PACKAGE
        val totalTimer = TimeSource.Monotonic.markNow()
        return try {
            val packageTimer = TimeSource.Monotonic.markNow()
            val prepared = prepareArchive(character)
            Napier.i(
                message = "Package ready: characterId=${character.id}, " +
                    "zipSize=${prepared.archive.size} bytes (${prepared.archive.size.toReadableSize()}), " +
                    "elapsed=${packageTimer.elapsedNow().inWholeMilliseconds} ms",
                tag = LOG_TAG,
            )

            stage = ShareStage.GET_UPLOAD_URL
            val characterKey = characterKey(character.author, character.id)
            val updateToken = appSettingsStore.getCharacterUpdateToken(characterKey)
            val uploadUrlTimer = TimeSource.Monotonic.markNow()
            Napier.i(
                message = "Requesting upload URL: characterId=${character.id}",
                tag = LOG_TAG,
            )
            val submission = httpClient.post(SUBMISSION_ENDPOINT) {
                attributes.put(SuppressNetworkLogging, true)
                contentType(ContentType.Application.Json)
                setBody(
                    SubmissionRequest(
                        characterId = character.id,
                        author = character.author,
                        primaryCategoryId = (categorySelection as? ShareCategorySelection.Existing)?.id,
                        primaryCategoryName = (categorySelection as? ShareCategorySelection.Proposed)?.name,
                        tagIds = tagIds,
                        contentFingerprint = prepared.contentFingerprint,
                        packageSize = prepared.archive.size,
                        updateToken = updateToken,
                    ),
                )
            }.body<SubmissionResponse>()
            Napier.i(
                message = "Upload URL response received: characterId=${character.id}, " +
                    "success=${submission.success}, " +
                    "elapsed=${uploadUrlTimer.elapsedNow().inWholeMilliseconds} ms",
                tag = LOG_TAG,
            )
            if (!submission.success || submission.upload == null) {
                return ApiResult.UnexpectedError(
                    submission.message ?: submission.code ?: ERROR_UPLOAD_URL,
                )
            }
            if (!submission.updateToken.isNullOrBlank() && !submission.characterKey.isNullOrBlank()) {
                appSettingsStore.setCharacterUpdateToken(
                    submission.characterKey,
                    submission.updateToken,
                )
            }

            stage = ShareStage.UPLOAD_ZIP
            val uploadTimer = TimeSource.Monotonic.markNow()
            Napier.i(
                message = "Uploading ZIP: characterId=${character.id}, " +
                    "zipSize=${prepared.archive.size} bytes (${prepared.archive.size.toReadableSize()})",
                tag = LOG_TAG,
            )
            val response = httpClient.post(submission.upload.url) {
                attributes.put(SuppressNetworkLogging, true)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            submission.upload.fields.forEach { (name, value) ->
                                append(name, value)
                            }
                            append(
                                key = "file",
                                value = prepared.archive,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, ContentType.Application.Zip.toString())
                                    append(HttpHeaders.ContentDisposition, "filename=\"package.zip\"")
                                },
                            )
                        },
                    ),
                )
            }
            Napier.i(
                message = "ZIP upload completed: characterId=${character.id}, " +
                    "status=${response.status.value}, " +
                    "elapsed=${uploadTimer.elapsedNow().inWholeMilliseconds} ms, " +
                    "totalElapsed=${totalTimer.elapsedNow().inWholeMilliseconds} ms",
                tag = LOG_TAG,
            )
            if (response.status.value in 200..299) {
                ApiResult.Success(Unit)
            } else {
                ApiResult.HttpError(response.status.value, response.status.description)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (response: ResponseException) {
            logFailure(character.id, stage, totalTimer, response)
            ApiResult.HttpError(response.response.status.value, response.message)
        } catch (error: IllegalStateException) {
            logFailure(character.id, stage, totalTimer, error)
            if (error.message == ERROR_MEDIA_READ) {
                ApiResult.UnexpectedError(ERROR_MEDIA_READ)
            } else {
                ApiResult.UnexpectedError(error.message)
            }
        } catch (error: Throwable) {
            logFailure(character.id, stage, totalTimer, error)
            ApiResult.NetworkError(error.message)
        }
    }

    internal suspend fun buildArchive(character: Character): ByteArray = prepareArchive(character).archive

    internal suspend fun prepareArchive(character: Character): PreparedCharacterArchive =
        withContext(Dispatchers.Default) {
        val avatar = readOptionalMedia(character.avatarUri, "avatar")
        val voice = readOptionalMedia(character.voiceSampleUri, "sample")
        val manifest = PublicCharacterManifest(
            id = character.id,
            name = character.name,
            author = character.author,
            description = character.description,
            systemPrompt = character.prompt,
            openingMessage = character.openingMessage,
            temperature = character.temperature,
            topP = character.topP,
            avatarUrl = avatar?.name,
            voiceSampleUrl = voice?.name,
        )
        val entries = buildList {
            add(CharacterArchiveEntry("character.json", json.encodeToString(manifest).encodeToByteArray()))
            avatar?.let(::add)
            voice?.let(::add)
        }
        Napier.i(
            message = "Creating ZIP: characterId=${character.id}, entries=" +
                entries.joinToString { "${it.name}=${it.content.size} bytes" },
            tag = LOG_TAG,
        )
        PreparedCharacterArchive(
            archive = archiveWriter.createArchive(entries),
            contentFingerprint = contentFingerprint(
                manifestBytes = entries.first().content,
                avatarBytes = avatar?.content,
                voiceBytes = voice?.content,
            ),
        )
    }

    private fun logFailure(
        characterId: String,
        stage: ShareStage,
        totalTimer: TimeSource.Monotonic.ValueTimeMark,
        error: Throwable,
    ) {
        Napier.e(
            message = "Share failed: characterId=$characterId, stage=${stage.logName}, " +
                "elapsed=${totalTimer.elapsedNow().inWholeMilliseconds} ms, " +
                "error=${error::class.simpleName}",
            throwable = error,
            tag = LOG_TAG,
        )
    }

    private suspend fun readOptionalMedia(
        uri: String?,
        baseName: String,
    ): CharacterArchiveEntry? {
        if (uri.isNullOrBlank()) return null
        val bytes = fileManager.readSourceBytes(uri)
        if (bytes.isEmpty()) {
            throw IllegalStateException(ERROR_MEDIA_READ)
        }
        val extension = mediaExtension(uri)
        return CharacterArchiveEntry("$baseName.$extension", bytes)
    }

    private fun mediaExtension(uri: String): String {
        val dataMime = uri.takeIf { it.startsWith("data:", ignoreCase = true) }
            ?.substringAfter("data:")
            ?.substringBefore(';')
            ?.lowercase()
        val fromMime = when (dataMime) {
            "image/jpeg" -> "jpg"
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "audio/mpeg" -> "mp3"
            "audio/wav", "audio/x-wav" -> "wav"
            "audio/ogg" -> "ogg"
            "audio/mp4" -> "m4a"
            else -> null
        }
        if (fromMime != null) return fromMime
        return uri.substringBefore('?')
            .substringBefore('#')
            .substringAfterLast('/')
            .substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.matches(Regex("[a-z0-9]{1,8}")) }
            ?: "bin"
    }

    companion object {
        const val ERROR_PLATFORM_UNSUPPORTED = "platform_unsupported"
        const val ERROR_MEDIA_READ = "media_read_failed"
        const val ERROR_SYSTEM_PROMPT_REQUIRED = "系统提示词为空，无法提交"
        private const val ERROR_UPLOAD_URL = "upload_url_failed"
        private const val SUBMISSION_ENDPOINT = "https://api.qyaichat.com/v1/submissions"
        private const val LOG_TAG = "PublicCharacterShare"
    }
}

internal data class PreparedCharacterArchive(
    val archive: ByteArray,
    val contentFingerprint: String,
)

private fun characterKey(author: String, characterId: String): String {
    val normalizedAuthor = author
        .trim()
        .replace(Regex("\\s+"), " ")
        .lowercase()
    return "$normalizedAuthor\n$characterId"
        .encodeToByteArray()
        .toByteString()
        .sha256()
        .hex()
}

private fun contentFingerprint(
    manifestBytes: ByteArray,
    avatarBytes: ByteArray?,
    voiceBytes: ByteArray?,
): String {
    val buffer = Buffer()
    listOf(
        manifestBytes,
        avatarBytes ?: byteArrayOf(),
        voiceBytes ?: byteArrayOf(),
    ).forEach { bytes ->
        buffer.writeUtf8(bytes.size.toString(16).padStart(8, '0'))
        buffer.writeByte(':'.code)
        buffer.write(bytes)
    }
    return buffer.readByteString().sha256().hex()
}

private enum class ShareStage(
    val logName: String,
) {
    PACKAGE("package"),
    GET_UPLOAD_URL("get_upload_url"),
    UPLOAD_ZIP("upload_zip"),
}

private fun Int.toReadableSize(): String {
    val kib = this / 1024.0
    return if (kib < 1024.0) {
        "${(kib * 10).toInt() / 10.0} KiB"
    } else {
        val mib = kib / 1024.0
        "${(mib * 10).toInt() / 10.0} MiB"
    }
}
