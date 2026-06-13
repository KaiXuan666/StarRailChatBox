package com.kaixuan.starrailchatbox.data.character.sharing

import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
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
    fun shareRequestsUploadUrlThenPutsZip() = runTest {
        var requestCount = 0
        val engine = MockEngine { request ->
            requestCount++
            when (requestCount) {
                1 -> {
                    respond(
                        content = """{"success":true,"uploadUrl":"https://oss.example/upload","ossKey":"uploads/role.zip"}""",
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
            archiveWriter = CapturingArchiveWriter(),
        )

        val result = repository.share(testCharacter())

        assertIs<ApiResult.Success<Unit>>(result)
        assertEquals(2, requestCount)
        assertEquals(HttpMethod.Post, engine.requestHistory[0].method)
        assertTrue(engine.requestHistory[0].url.toString().startsWith("https://api.qyaichat.com/getUploadUrl"))
        assertEquals(HttpMethod.Put, engine.requestHistory[1].method)
        assertTrue(engine.requestHistory[1].url.toString().startsWith("https://oss.example/upload"))
        assertEquals(ContentType.Application.Zip, engine.requestHistory[1].body.contentType)
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
            archiveWriter = CapturingArchiveWriter(),
        )

        val result = repository.share(testCharacter())

        val error = assertIs<ApiResult.UnexpectedError>(result)
        assertTrue(error.message.orEmpty().contains("审核中"))
        assertEquals(1, requestCount)
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

private fun testCharacter(
    avatarUri: String = "",
    voiceSampleUri: String? = null,
) = Character(
    id = "role",
    name = "Role",
    author = "author",
    description = "description",
    prompt = "prompt",
    openingMessage = "hello",
    avatarUri = avatarUri,
    voiceSampleUri = voiceSampleUri,
)
