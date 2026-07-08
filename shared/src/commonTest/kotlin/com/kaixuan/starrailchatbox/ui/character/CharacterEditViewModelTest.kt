package com.kaixuan.starrailchatbox.ui.character

import androidx.lifecycle.viewModelScope
import com.kaixuan.starrailchatbox.data.ai.AiMessage
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.ChatCompletionResult
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProviderRegistry
import com.kaixuan.starrailchatbox.data.api.ApiResult
import com.kaixuan.starrailchatbox.data.character.Character
import com.kaixuan.starrailchatbox.data.character.CharacterAvatarSource
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.CharacterSummary
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardImporter
import com.kaixuan.starrailchatbox.data.character.importer.ImportedCharacterDraft
import com.kaixuan.starrailchatbox.data.model.InMemoryModelConfigRepository
import com.kaixuan.starrailchatbox.data.model.ModelConfig
import com.kaixuan.starrailchatbox.data.settings.InMemoryAppSettingsStore
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import io.github.vinceglb.filekit.PlatformFile
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class CharacterEditViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private var currentViewModel: CharacterEditViewModel? = null

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        currentViewModel?.viewModelScope?.cancel()
        currentViewModel = null
        Dispatchers.resetMain()
    }

    @Test
    fun oversizedVoiceSampleSelectionShowsMessageAndKeepsDraftUnchanged() = runTest {
        val fixture = createFixture()
        val sourcePath = "/source/voice.wav".toPath()
        fixture.fileManager.writeBytes(sourcePath, ByteArray(OversizedVoiceSampleBytes))

        fixture.viewModel.onAction(
            CharacterAction.CharacterVoiceSampleChanged(
                uri = sourcePath.toString(),
                extension = "wav",
            ),
        )
        advanceUntilIdle()

        assertNull(fixture.viewModel.uiState.value.voiceSampleUri)
        assertEquals(0, fixture.fileManager.fullReadCount)
        assertEquals(0, fixture.fileManager.boundedReadCount)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.VOICE_SAMPLE_TOO_LARGE),
            fixture.viewModel.effects.first(),
        )
        fixture.httpClient.close()
    }

    @Test
    fun oversizedVoiceSampleBlocksSave() = runTest {
        val sourcePath = "/source/voice.wav".toPath()
        val fileManager = FakeFileManager()
        fileManager.writeBytes(sourcePath, ByteArray(OversizedVoiceSampleBytes))
        val repository = FakeCharacterEditRepository(
            initial = testCharacter(voiceSampleUri = sourcePath.toString()),
        )
        val fixture = createFixture(
            characterId = "role",
            repository = repository,
            fileManager = fileManager,
        )
        advanceUntilIdle()

        fixture.viewModel.onAction(CharacterAction.CharacterSaveClicked)
        advanceUntilIdle()

        val effect = fixture.viewModel.effects.first()
        assertNull(repository.savedCharacter)
        assertEquals(0, fileManager.fullReadCount)
        assertEquals(0, fileManager.boundedReadCount)
        assertFalse(fixture.viewModel.uiState.value.isSaving)
        assertEquals(
            CharacterEffect.ShowMessage(CharacterEffectMessage.VOICE_SAMPLE_TOO_LARGE),
            effect,
        )
        fixture.httpClient.close()
    }

    private fun createFixture(
        characterId: String? = null,
        repository: FakeCharacterEditRepository = FakeCharacterEditRepository(),
        fileManager: FakeFileManager = FakeFileManager(),
    ): Fixture {
        val httpClient = HttpClient(MockEngine { respond("") })
        val viewModel = CharacterEditViewModel(
            characterId = characterId,
            importPath = null,
            importName = null,
            importExtension = null,
            characterRepository = repository,
            modelConfigRepository = InMemoryModelConfigRepository(),
            aiRepository = FakeAiRepository,
            characterCardImporter = FakeCharacterCardImporter,
            characterCardExporter = FakeCharacterCardExporter,
            fileManager = fileManager,
            imageProviderRegistry = ImageGenerationProviderRegistry(emptyList()),
            httpClient = httpClient,
            appSettingsStore = InMemoryAppSettingsStore(),
        )
        currentViewModel = viewModel
        return Fixture(viewModel, repository, fileManager, httpClient)
    }
}

private const val OversizedVoiceSampleBytes = 5 * 1024 * 1024 + 1

private data class Fixture(
    val viewModel: CharacterEditViewModel,
    val repository: FakeCharacterEditRepository,
    val fileManager: FakeFileManager,
    val httpClient: HttpClient,
)

private class FakeFileManager : KmpFileManager {
    override val appDataDir: Path = "/app".toPath()
    override val cacheDir: Path = "/cache".toPath()
    override val fileSystem: FileSystem = FakeFileSystem()
    var fullReadCount: Int = 0
        private set
    var boundedReadCount: Int = 0
        private set

    override suspend fun readSourceBytes(source: String): ByteArray {
        fullReadCount += 1
        return super<KmpFileManager>.readSourceBytes(source)
    }

    override suspend fun readSourceBytesUpTo(source: String, maxBytes: Long): ByteArray {
        boundedReadCount += 1
        return super<KmpFileManager>.readSourceBytesUpTo(source, maxBytes)
    }

    override suspend fun saveImageToGallery(bytes: ByteArray, name: String) = Unit
}

private class FakeCharacterEditRepository(
    initial: Character? = null,
) : CharacterRepository {
    private val characters = mutableMapOf<String, Character>()
    var savedCharacter: Character? = null
        private set

    init {
        if (initial != null) {
            characters[initial.id] = initial
        }
    }

    override suspend fun loadCharacters(): List<Character> = characters.values.toList()

    override fun observeCharacterSummaries(): Flow<List<CharacterSummary>> =
        flowOf(
            characters.values.map { character ->
                CharacterSummary(
                    id = character.id,
                    name = character.name,
                    avatarUri = character.avatarUri,
                    createdAt = character.createdAt,
                    lastMessageAt = character.lastMessageAt,
                )
            },
        )

    override suspend fun getCharacter(id: String): Character? = characters[id]

    override suspend fun addCharacter(
        name: String,
        prompt: String,
        avatarSource: CharacterAvatarSource?,
    ): Character = testCharacter(name = name, prompt = prompt)

    override suspend fun updateCharacter(
        character: Character,
        avatarSource: CharacterAvatarSource?,
    ): Character {
        savedCharacter = character
        characters[character.id] = character
        return character
    }

    override suspend fun updateSortOrder(id: String, sortOrder: Int) = Unit

    override suspend fun deleteCharacter(id: String, deletedAt: Long) {
        characters.remove(id)
    }

    override suspend fun getDefaultCharacter(id: String): Character? = null
}

private object FakeAiRepository : AiRepository {
    override suspend fun getModels(
        apiHost: String,
        apiKey: String,
        providerId: String,
    ): ApiResult<List<String>> = ApiResult.Success(emptyList())

    override suspend fun createChatCompletion(
        config: ModelConfig,
        messages: List<AiMessage>,
        characterName: String,
        voiceSampleUri: String?,
    ): ApiResult<ChatCompletionResult> = ApiResult.UnexpectedError("Not used")

    override suspend fun createConversationSummary(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> = ApiResult.UnexpectedError("Not used")

    override suspend fun createSessionTitle(
        config: ModelConfig,
        messages: List<AiMessage>,
    ): ApiResult<ChatCompletionResult> = ApiResult.UnexpectedError("Not used")

    override suspend fun testToolCallSupport(
        apiHost: String,
        apiKey: String,
        model: String,
        providerId: String,
    ): Boolean = false
}

private object FakeCharacterCardImporter : CharacterCardImporter {
    override suspend fun importFromFile(
        path: String,
        name: String,
        extension: String,
    ): ApiResult<ImportedCharacterDraft> = ApiResult.UnexpectedError("Not used")
}

private object FakeCharacterCardExporter : CharacterCardExporter {
    override suspend fun exportToPng(
        character: Character,
        directory: PlatformFile,
    ): ApiResult<Unit> = ApiResult.Success(Unit)
}

private fun testCharacter(
    id: String = "role",
    name: String = "Role",
    prompt: String = "prompt",
    voiceSampleUri: String? = null,
) = Character(
    id = id,
    name = name,
    prompt = prompt,
    openingMessage = "hello",
    avatarUri = "",
    voiceSampleUri = voiceSampleUri,
)
