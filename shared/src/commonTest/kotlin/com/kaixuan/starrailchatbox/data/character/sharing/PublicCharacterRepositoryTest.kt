package com.kaixuan.starrailchatbox.data.character.sharing

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.readText
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class PublicCharacterRepositoryTest {
    @Test
    fun packageIncludesManifestAndOriginalMediaExtensions() = runTest {
        val archiveWriter = CapturingArchiveWriter()
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(MockEngine { error("Network should not be called") }),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = archiveWriter,
        )
        val character = testCharacter(
            avatarUri = "data:image/webp;base64,${Base64.encode(byteArrayOf(1, 2))}",
            voiceSampleUri = "data:audio/wav;base64,${Base64.encode(byteArrayOf(3, 4))}",
        )

        repository.buildArchive(character)

        assertEquals(listOf("character.json", "avatar.webp", "sample.wav"), archiveWriter.entries.map { it.name })
        val manifest = Json.decodeFromString<PublicCharacterManifest>(
            archiveWriter.entries.first().content.decodeToString(),
        )
        assertEquals("author", manifest.author)
        assertEquals("avatar.webp", manifest.avatarUrl)
        assertEquals("sample.wav", manifest.voiceSampleUrl)
    }

    @Test
    fun packageOmitsMissingMedia() = runTest {
        val archiveWriter = CapturingArchiveWriter()
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(MockEngine { error("Network should not be called") }),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = archiveWriter,
        )

        repository.buildArchive(testCharacter())

        assertEquals(listOf("character.json"), archiveWriter.entries.map { it.name })
        val manifest = Json.decodeFromString<PublicCharacterManifest>(
            archiveWriter.entries.single().content.decodeToString(),
        )
        assertNull(manifest.avatarUrl)
        assertNull(manifest.voiceSampleUrl)
        val rawJson = archiveWriter.entries.single().content.decodeToString()
        assertFalse("avatarUrl" in rawJson)
        assertFalse("voiceSampleUrl" in rawJson)
    }

    @Test
    fun fingerprintMatchesServerCanonicalAlgorithm() = runTest {
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(MockEngine { error("Network should not be called") }),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = CapturingArchiveWriter(),
        )

        val prepared = repository.prepareArchive(testCharacter())

        assertEquals(
            "b265442247a54be404c988e8b8e47a478683d049df18b3c89039facc5a8a3742",
            prepared.contentFingerprint,
        )
    }

    @Test
    fun shareRequestsUploadUrlThenPutsZip() = runTest {
        var requestCount = 0
        var submissionBody = ""
        val engine = MockEngine { request ->
            requestCount++
            when (requestCount) {
                1 -> {
                    submissionBody = request.body.readText()
                    respond(
                        content = """
                            {
                              "success": true,
                              "submissionId": "0123456789abcdef0123456789abcdef",
                              "characterKey": "abc",
                              "updateToken": "secret",
                              "upload": {
                                "url": "https://oss.example/upload",
                                "method": "POST",
                                "expiresAt": "2026-06-14T00:00:00Z",
                                "fields": {
                                  "key": "private/incoming/id/package.zip",
                                  "policy": "policy",
                                  "Signature": "signature"
                                }
                              }
                            }
                        """.trimIndent(),
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
                else -> {
                    respond("", HttpStatusCode.OK)
                }
            }
        }
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(engine),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = CapturingArchiveWriter(),
        )

        val result = repository.share(
            testCharacter(),
            categorySelection = ShareCategorySelection.Existing("game"),
            tagIds = listOf("gentle", "healing"),
        )

        assertIs<ApiResult.Success<Unit>>(result)
        assertEquals(2, requestCount)
        assertEquals(HttpMethod.Post, engine.requestHistory[0].method)
        assertTrue(engine.requestHistory[0].url.toString().startsWith("https://api.qyaichat.com/v1/submissions"))
        assertTrue(submissionBody.contains("\"primaryCategoryId\":\"game\""))
        assertTrue(submissionBody.contains("\"tagIds\":[\"gentle\",\"healing\"]"))
        assertEquals(HttpMethod.Post, engine.requestHistory[1].method)
        assertTrue(engine.requestHistory[1].url.toString().startsWith("https://oss.example/upload"))
        assertTrue(engine.requestHistory[1].body.contentType.toString().startsWith("multipart/form-data"))
    }

    @Test
    fun proposedCategorySendsOnlyTheCategoryName() = runTest {
        var submissionBody = ""
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount++
            if (requestCount == 1) {
                submissionBody = request.body.readText()
                respond(
                    content = """
                        {
                          "success": true,
                          "submissionId": "0123456789abcdef0123456789abcdef",
                          "characterKey": "abc",
                          "upload": {
                            "url": "https://oss.example/upload",
                            "method": "POST",
                            "expiresAt": "2026-06-14T00:00:00Z",
                            "fields": {}
                          }
                        }
                    """.trimIndent(),
                    headers = headersOf(
                        HttpHeaders.ContentType,
                        ContentType.Application.Json.toString(),
                    ),
                )
            } else {
                respond("", HttpStatusCode.OK)
            }
        }
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(engine),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = CapturingArchiveWriter(),
        )

        val result = repository.share(
            testCharacter(),
            categorySelection = ShareCategorySelection.Proposed("新类目"),
            tagIds = emptyList(),
        )

        assertIs<ApiResult.Success<Unit>>(result)
        assertTrue(submissionBody.contains("\"primaryCategoryName\":\"新类目\""))
        assertFalse(submissionBody.contains("\"primaryCategoryId\""))
    }

    @Test
    fun shareReturnsServerReviewMessageWithoutUploading() = runTest {
        var requestCount = 0
        val engine = MockEngine {
            requestCount++
            respond(
                content = """{"success":false,"message":"该角色正在审核中，请勿重复上传"}""",
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(engine),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = CapturingArchiveWriter(),
        )

        val result = repository.share(
            testCharacter(),
            categorySelection = ShareCategorySelection.Existing("game"),
            tagIds = emptyList(),
        )

        val error = assertIs<ApiResult.UnexpectedError>(result)
        assertTrue(error.message.orEmpty().contains("审核中"))
        assertEquals(1, requestCount)
    }

    @Test
    fun shareRejectsBlankSystemPromptBeforePackagingOrNetwork() = runTest {
        var requestCount = 0
        val archiveWriter = CapturingArchiveWriter()
        val repository = DefaultPublicCharacterRepository(
            httpClient = testClient(MockEngine {
                requestCount++
                error("Network should not be called")
            }),
            fileManager = FakeFileManager(),
            appSettingsStore = InMemoryAppSettingsStore(),
            archiveWriter = archiveWriter,
        )

        val result = repository.share(
            testCharacter(prompt = " \n "),
            categorySelection = ShareCategorySelection.Existing("game"),
            tagIds = emptyList(),
        )

        val error = assertIs<ApiResult.UnexpectedError>(result)
        assertEquals(DefaultPublicCharacterRepository.ERROR_SYSTEM_PROMPT_REQUIRED, error.message)
        assertEquals(0, requestCount)
        assertTrue(archiveWriter.entries.isEmpty())
    }
}

private class CapturingArchiveWriter : CharacterArchiveWriter {
    override val isSupported: Boolean = true
    var entries: List<CharacterArchiveEntry> = emptyList()

    override fun createArchive(entries: List<CharacterArchiveEntry>): ByteArray {
        this.entries = entries
        return byteArrayOf(9, 8, 7)
    }
}

private class FakeFileManager : KmpFileManager {
    override val appDataDir: Path = "/app".toPath()
    override val cacheDir: Path = "/cache".toPath()
    override val fileSystem: FileSystem = FakeFileSystem()

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) = Unit
}

private fun testClient(engine: MockEngine) = HttpClient(engine) {
    install(ContentNegotiation) {
        json(Json { ignoreUnknownKeys = true })
    }
}

private suspend fun OutgoingContent.readText(): String {
    return when (this) {
        is TextContent -> text
        is OutgoingContent.ByteArrayContent -> bytes().decodeToString()
        is OutgoingContent.WriteChannelContent -> {
            val channel = io.ktor.utils.io.ByteChannel(true)
            writeTo(channel)
            channel.close()
            channel.readRemaining().readText()
        }
        else -> ""
    }
}

private fun testCharacter(
    avatarUri: String = "",
    voiceSampleUri: String? = null,
    prompt: String = "prompt",
) = Character(
    id = "role",
    name = "Role",
    author = "author",
    description = "description",
    prompt = prompt,
    openingMessage = "hello",
    avatarUri = avatarUri,
    voiceSampleUri = voiceSampleUri,
)
