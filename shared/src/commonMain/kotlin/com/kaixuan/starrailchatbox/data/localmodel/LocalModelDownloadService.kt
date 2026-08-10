package com.kaixuan.starrailchatbox.data.localmodel

import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import kotlin.time.Clock

sealed interface LocalModelDownloadEvent {
    data class Progress(
        val downloadedBytes: Long,
        val totalBytes: Long,
        val bytesPerSecond: Long,
    ) : LocalModelDownloadEvent
    data object Verifying : LocalModelDownloadEvent
    data class Completed(val model: LocalModel) : LocalModelDownloadEvent
    data class Failed(val code: String, val message: String) : LocalModelDownloadEvent
}

data class StagedLocalModel(
    val path: String,
    val name: String,
    val sizeBytes: Long,
    val sha256: String,
)

interface LocalModelDownloadService {
    fun download(model: CatalogLocalModel): Flow<LocalModelDownloadEvent>
    suspend fun stageImport(source: String, sourceFileName: String, displayName: String): Result<StagedLocalModel>
    suspend fun installImport(staged: StagedLocalModel): Result<LocalModel>
    suspend fun discardImport(staged: StagedLocalModel)
    suspend fun delete(model: LocalModel)
    fun partialPath(modelId: String): Path
}

class DefaultLocalModelDownloadService(
    private val httpClient: HttpClient,
    private val repository: LocalModelRepository,
    private val runtime: LocalLanguageModelRuntime,
    private val files: KmpFileManager,
    private val now: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) : LocalModelDownloadService {
    private val downloadDir get() = files.cacheDir / "local_models".toPath()
    private val modelDir get() = files.appDataDir / "local_models".toPath()

    override fun partialPath(modelId: String): Path = downloadDir / "$modelId.part".toPath()
    internal fun partialValidatorPath(modelId: String): Path = downloadDir / "$modelId.ifrange".toPath()

    override fun download(model: CatalogLocalModel): Flow<LocalModelDownloadEvent> = flow {
        val required = model.sizeBytes + OneGiB
        val available = files.availableSpaceBytes(modelDir)
        if (available != null && available < required) {
            emit(LocalModelDownloadEvent.Failed("local_storage_insufficient", "Not enough storage space."))
            return@flow
        }
        files.createDirectories(downloadDir)
        val partial = partialPath(model.id)
        val validatorFile = partialValidatorPath(model.id)
        var existing = files.fileSystem.metadataOrNull(partial)?.size ?: 0L
        if (existing > model.sizeBytes) {
            files.delete(partial)
            files.delete(validatorFile)
            existing = 0L
        }
        try {
            httpClient.prepareGet(model.downloadUrl) {
                if (existing > 0L) {
                    header(HttpHeaders.Range, "bytes=$existing-")
                    validatorFile.takeIf(files::exists)?.let(files::readText)?.takeIf(String::isNotBlank)?.let {
                        header(HttpHeaders.IfRange, it)
                    }
                }
            }.execute { response ->
                if (response.status.value !in 200..299) {
                    emit(LocalModelDownloadEvent.Failed("local_download_failed", response.status.description))
                    return@execute
                }
                val append = existing > 0L && response.status == HttpStatusCode.PartialContent
                if (existing > 0L && !append) {
                    files.delete(partial)
                    files.delete(validatorFile)
                    existing = 0L
                }
                val validator = response.headers[HttpHeaders.ETag]
                    ?: response.headers[HttpHeaders.LastModified]
                if (!validator.isNullOrBlank()) {
                    files.writeText(validatorFile, validator)
                }
                val channel: ByteReadChannel = response.body()
                var downloaded = existing
                var speedBytes = 0L
                var speedStartedAt = now()
                val sink = if (append) {
                    files.fileSystem.appendingSink(partial, mustExist = false)
                } else {
                    files.fileSystem.sink(partial, mustCreate = false)
                }
                val output = sink.buffer()
                try {
                        while (!channel.isClosedForRead) {
                            val packet = channel.readRemaining(64 * 1024L)
                            while (!packet.exhausted()) {
                                val bytes = packet.readByteArray()
                                output.write(bytes)
                                output.emitCompleteSegments()
                                downloaded += bytes.size
                                speedBytes += bytes.size
                                val elapsed = now() - speedStartedAt
                                if (elapsed >= 500L) {
                                    emit(
                                        LocalModelDownloadEvent.Progress(
                                            downloaded,
                                            model.sizeBytes,
                                            speedBytes * 1_000L / elapsed.coerceAtLeast(1L),
                                        ),
                                    )
                                    speedBytes = 0L
                                    speedStartedAt = now()
                                }
                            }
                        }
                } finally {
                    output.close()
                }
            }
            val actualSize = files.fileSystem.metadataOrNull(partial)?.size ?: 0L
            if (actualSize != model.sizeBytes) {
                emit(LocalModelDownloadEvent.Failed("local_size_mismatch", "Downloaded model size is invalid."))
                return@flow
            }
            emit(LocalModelDownloadEvent.Verifying)
            val actualHash = files.sha256(partial)
            if (!actualHash.equals(model.sha256, ignoreCase = true)) {
                emit(LocalModelDownloadEvent.Failed("local_checksum_failed", "Downloaded model checksum is invalid."))
                return@flow
            }
            val target = modelDir / "${model.id}.litertlm".toPath()
            files.createDirectories(modelDir)
            files.move(partial, target)
            files.delete(validatorFile)
            val timestamp = now()
            val installed = LocalModel(
                id = model.id,
                name = model.name,
                filePath = target.toString(),
                sizeBytes = model.sizeBytes,
                sha256 = model.sha256,
                source = LocalModelSource.CATALOG,
                sourceUrl = model.downloadUrl,
                license = model.license,
                contextWindow = model.contextWindow,
                maxOutputTokens = model.maxOutputTokens,
                createdAt = timestamp,
                updatedAt = timestamp,
            )
            repository.upsert(installed)
            emit(LocalModelDownloadEvent.Completed(installed))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            emit(LocalModelDownloadEvent.Failed("local_download_failed", error.message ?: "Download failed."))
        }
    }

    override suspend fun stageImport(
        source: String,
        sourceFileName: String,
        displayName: String,
    ): Result<StagedLocalModel> {
        var staged: Path? = null
        val result = runCatching {
            require(sourceFileName.endsWith(".litertlm", ignoreCase = true)) {
                "Only .litertlm files are supported."
            }
            val sourceSize = files.sourceSizeBytes(source)
            val availableBeforeCopy = files.availableSpaceBytes(modelDir)
            require(sourceSize == null || availableBeforeCopy == null || availableBeforeCopy >= sourceSize + OneGiB) {
                "Not enough storage space."
            }
            files.createDirectories(downloadDir)
            // LiteRT-LM inspects the final extension during Engine initialization.
            // Keep the staged file in cache, but preserve `.litertlm` as the last suffix.
            val stagedPath = downloadDir / "import_pending_${now()}.litertlm".toPath()
            staged = stagedPath
            files.copySourceTo(source, stagedPath)
            val size = files.fileSystem.metadataOrNull(stagedPath)?.size ?: 0L
            require(size > 0L) { "The selected model is empty." }
            val available = files.availableSpaceBytes(modelDir)
            require(available == null || available >= size + OneGiB) { "Not enough storage space." }
            val hash = files.sha256(stagedPath)
            require(repository.observeModels().first().none { it.sha256 == hash }) {
                "This model is already installed."
            }
            when (val validation = runtime.validate(stagedPath.toString())) {
                is LocalRuntimeResult.Failure -> error(validation.message)
                is LocalRuntimeResult.Success -> Unit
            }
            StagedLocalModel(stagedPath.toString(), displayName, size, hash)
        }
        if (result.isFailure) staged?.let(files::delete)
        return result
    }

    override suspend fun installImport(staged: StagedLocalModel): Result<LocalModel> = runCatching {
        val id = "imported-${staged.sha256.take(16)}"
        val target = modelDir / "$id.litertlm".toPath()
        files.createDirectories(modelDir)
        files.move(staged.path.toPath(), target)
        val timestamp = now()
        val model = LocalModel(
            id = id,
            name = staged.name,
            filePath = target.toString(),
            sizeBytes = staged.sizeBytes,
            sha256 = staged.sha256,
            source = LocalModelSource.IMPORTED,
            sourceUrl = null,
            license = "User supplied",
            contextWindow = 4_096,
            maxOutputTokens = 1_024,
            createdAt = timestamp,
            updatedAt = timestamp,
        )
        repository.upsert(model)
        model
    }

    override suspend fun discardImport(staged: StagedLocalModel) {
        files.delete(staged.path.toPath())
    }

    override suspend fun delete(model: LocalModel) {
        runtime.close(model.id)
        files.delete(model.filePath.toPath())
        files.delete(partialPath(model.id))
        files.delete(partialValidatorPath(model.id))
        repository.delete(model.id)
    }
}

private const val OneGiB = 1_073_741_824L
