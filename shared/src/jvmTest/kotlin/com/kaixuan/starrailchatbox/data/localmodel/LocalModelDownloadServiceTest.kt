package com.kaixuan.starrailchatbox.data.localmodel

import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import okio.Path.Companion.toPath
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalModelDownloadServiceTest {
    @Test
    fun stagedImportKeepsLiteRtLmAsFinalExtensionForFormatValidation() = runTest {
        withFixture { fixture ->
            val source = fixture.root.resolve("selected-model.litertlm")
            Files.write(source, TinyModelBytes)
            val service = fixture.service(MockEngine { error("Network must not be used for import") })

            val staged = service.stageImport(
                source = source.toString(),
                sourceFileName = "selected-model.litertlm",
                displayName = "Selected model",
            ).getOrThrow()

            assertTrue(staged.path.endsWith(".litertlm"))
            assertEquals(staged.path, fixture.runtime.validatedPath)
            service.discardImport(staged)
            assertTrue(!fixture.files.exists(staged.path.toPath()))
        }
    }

    @Test
    fun downloadsVerifiesAndAtomicallyInstallsModel() = runTest {
        withFixture { fixture ->
            val model = tinyCatalogModel()
            val service = fixture.service(
                MockEngine {
                    respond(
                        content = TinyModelText,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentLength, TinyModelBytes.size.toString()),
                    )
                },
            )

            val events = service.download(model).toList()
            val completed = assertIs<LocalModelDownloadEvent.Completed>(events.last())
            assertTrue(fixture.files.exists(completed.model.filePath.toPath()))
            assertEquals(TinyModelSha256, fixture.files.sha256(completed.model.filePath.toPath()))
            assertEquals(completed.model, fixture.repository.getById(model.id))
            assertTrue(!fixture.files.exists(service.partialPath(model.id)))
        }
    }

    @Test
    fun resumesPartialDownloadWithRangeAndIfRange() = runTest {
        withFixture { fixture ->
            val model = tinyCatalogModel()
            lateinit var observedRange: String
            lateinit var observedIfRange: String
            val service = fixture.service(
                MockEngine { request ->
                    observedRange = requireNotNull(request.headers[HttpHeaders.Range])
                    observedIfRange = requireNotNull(request.headers[HttpHeaders.IfRange])
                    respond(
                        content = "world",
                        status = HttpStatusCode.PartialContent,
                        headers = headersOf(HttpHeaders.ContentLength, "5"),
                    )
                },
            )
            fixture.files.writeBytes(service.partialPath(model.id), "hello ".encodeToByteArray())
            fixture.files.writeText(service.partialValidatorPath(model.id), "\"fixed-etag\"")

            val completed = assertIs<LocalModelDownloadEvent.Completed>(service.download(model).toList().last())
            assertEquals("bytes=6-", observedRange)
            assertEquals("\"fixed-etag\"", observedIfRange)
            assertEquals(TinyModelText, fixture.files.readBytes(completed.model.filePath.toPath()).decodeToString())
        }
    }

    @Test
    fun restartsSafelyWhenServerIgnoresRange() = runTest {
        withFixture { fixture ->
            val model = tinyCatalogModel()
            val service = fixture.service(MockEngine { respond(TinyModelText, HttpStatusCode.OK) })
            fixture.files.writeBytes(service.partialPath(model.id), "stale".encodeToByteArray())

            val completed = assertIs<LocalModelDownloadEvent.Completed>(service.download(model).toList().last())
            assertEquals(TinyModelText, fixture.files.readBytes(completed.model.filePath.toPath()).decodeToString())
        }
    }

    @Test
    fun checksumFailureKeepsPartialForExplicitRecovery() = runTest {
        withFixture { fixture ->
            val model = tinyCatalogModel().copy(sha256 = "0".repeat(64))
            val service = fixture.service(MockEngine { respond(TinyModelText, HttpStatusCode.OK) })

            val failed = assertIs<LocalModelDownloadEvent.Failed>(service.download(model).toList().last())
            assertEquals("local_checksum_failed", failed.code)
            assertTrue(fixture.files.exists(service.partialPath(model.id)))
            assertNull(fixture.repository.getById(model.id))
        }
    }
}

private class DownloadFixture(val root: java.nio.file.Path) {
    val repository = InMemoryLocalModelRepository()
    val runtime = DownloadRuntime()
    val files = object : KmpFileManager {
        override val appDataDir: Path = root.resolve("files").toOkioPath()
        override val cacheDir: Path = root.resolve("cache").toOkioPath()
        override val fileSystem: FileSystem = FileSystem.SYSTEM
        override suspend fun availableSpaceBytes(path: Path): Long = Long.MAX_VALUE
        override suspend fun saveImageToGallery(bytes: ByteArray, name: String) = Unit
    }

    fun service(engine: MockEngine) = DefaultLocalModelDownloadService(
        httpClient = HttpClient(engine),
        repository = repository,
        runtime = runtime,
        files = files,
        now = { System.currentTimeMillis() },
    )
}

private class DownloadRuntime : LocalLanguageModelRuntime {
    override val isSupported = true
    override val status = MutableStateFlow(LocalRuntimeStatus())
    var validatedPath: String? = null
    override suspend fun validate(modelPath: String): LocalRuntimeResult {
        validatedPath = modelPath
        return if (modelPath.endsWith(".litertlm")) {
            LocalRuntimeResult.Success(LocalInferenceResult("", InferenceBackend.CPU))
        } else {
            LocalRuntimeResult.Failure("local_model_incompatible", "Unknown file format")
        }
    }
    override suspend fun complete(model: LocalModel, request: LocalInferenceRequest) =
        LocalRuntimeResult.Failure("unused", "unused")
    override suspend fun close(modelId: String?) = Unit
}

private suspend fun withFixture(block: suspend (DownloadFixture) -> Unit) {
    val root = Files.createTempDirectory("local-model-download")
    try {
        block(DownloadFixture(root))
    } finally {
        root.toFile().deleteRecursively()
    }
}

private fun tinyCatalogModel() = CatalogLocalModel(
    id = "tiny",
    name = "Tiny",
    revision = "fixed",
    fileName = "tiny.litertlm",
    sizeBytes = TinyModelBytes.size.toLong(),
    sha256 = TinyModelSha256,
    license = "Apache-2.0",
    contextWindow = 128,
    maxOutputTokens = 32,
)

private const val TinyModelText = "hello world"
private val TinyModelBytes = TinyModelText.encodeToByteArray()
private const val TinyModelSha256 = "b94d27b9934d3e08a52e52d7da7dabfac484efe37a5380ee9088f7ace2efcde9"
