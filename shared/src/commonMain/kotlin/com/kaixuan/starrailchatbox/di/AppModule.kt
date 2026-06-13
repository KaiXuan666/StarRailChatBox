package com.kaixuan.starrailchatbox.di

import com.kaixuan.starrailchatbox.data.ai.AiProvider
import com.kaixuan.starrailchatbox.data.ai.AiProviderRegistry
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.AliCompatibleProvider
import com.kaixuan.starrailchatbox.data.ai.DefaultAiRepository
import com.kaixuan.starrailchatbox.data.ai.OpenAiCompatibleProvider
import com.kaixuan.starrailchatbox.data.ai.XiaomiMimoProvider
import com.kaixuan.starrailchatbox.data.ai.image.AliImageProvider
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProvider
import com.kaixuan.starrailchatbox.data.ai.image.ImageGenerationProviderRegistry
import com.kaixuan.starrailchatbox.data.ai.image.OpenAiCompatibleImageProvider
import com.kaixuan.starrailchatbox.data.ai.tool.AiTool
import com.kaixuan.starrailchatbox.data.ai.tool.QuickRepliesTool
import com.kaixuan.starrailchatbox.data.ai.tool.VoiceSynthesisTool
import com.kaixuan.starrailchatbox.data.ai.tool.ImageGenerationTool
import com.kaixuan.starrailchatbox.data.ai.tool.BochaSearchTool
import com.kaixuan.starrailchatbox.data.ai.tool.RiskBasedToolApprovalGateway
import com.kaixuan.starrailchatbox.data.ai.tool.PlatformToolExecutor
import com.kaixuan.starrailchatbox.data.ai.tool.ToolApprovalGateway
import com.kaixuan.starrailchatbox.data.ai.tool.ToolCallCoordinator
import com.kaixuan.starrailchatbox.data.ai.tool.ToolRegistry
import com.kaixuan.starrailchatbox.data.ai.tool.createPlatformToolExecutor
import com.kaixuan.starrailchatbox.data.api.createPlatformHttpClient
import io.ktor.client.HttpClient
import com.kaixuan.starrailchatbox.data.database.DatabaseManager
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardImporter
import com.kaixuan.starrailchatbox.data.character.importer.DefaultCharacterCardImporter
import com.kaixuan.starrailchatbox.data.character.importer.CharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.importer.getCharacterCardExporter
import com.kaixuan.starrailchatbox.data.character.sharing.DefaultPublicCharacterRepository
import com.kaixuan.starrailchatbox.data.character.sharing.PublicCharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.update.DefaultUpdateRepository
import com.kaixuan.starrailchatbox.data.update.UpdateRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.platform.KmpFileManager
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.settings.SettingsOverviewViewModel
import com.kaixuan.starrailchatbox.ui.settings.api.ApiSettingsViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatMessageSender
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
import com.kaixuan.starrailchatbox.ui.character.CharactersViewModel
import com.kaixuan.starrailchatbox.ui.character.CharacterEditViewModel
import com.kaixuan.starrailchatbox.ui.character.CharacterEditArgs
import org.koin.core.qualifier.named
import org.koin.dsl.module

fun appModule(
    modelConfigRepository: ModelConfigRepository,
    profileStore: ProfileStore,
    appSettingsStore: AppSettingsStore,
    characterRepository: CharacterRepository,
    chatSessionRepository: ChatSessionRepository,
    databaseManager: DatabaseManager,
) = module {
    single { createPlatformHttpClient() }
    single { OpenAiCompatibleProvider(get()) }
    single<AiProvider>(named("OpenAiCompatible")) { get<OpenAiCompatibleProvider>() }
    single<AiProvider>(named("AliCompatible")) { AliCompatibleProvider(get()) }
    single<AiProvider>(named("XiaomiMimo")) { XiaomiMimoProvider(get()) }
    single {
        AiProviderRegistry(
            listOf(
                get(named("OpenAiCompatible")),
                get(named("AliCompatible")),
                get(named("XiaomiMimo")),
            ),
        )
    }
    single<ImageGenerationProvider>(named("OpenAiCompatibleImage")) {
        OpenAiCompatibleImageProvider(get())
    }
    single<ImageGenerationProvider>(named("AliImage")) {
        AliImageProvider(get())
    }
    single {
        ImageGenerationProviderRegistry(
            listOf(
                get(named("OpenAiCompatibleImage")),
                get(named("AliImage")),
            ),
        )
    }
    single<AiTool>(named("QuickReplies")) { QuickRepliesTool() }
    single<AiTool>(named("VoiceSynthesis")) { VoiceSynthesisTool(get(), get()) }
    single<AiTool>(named("ImageGeneration")) { ImageGenerationTool(get(), get(), get(), get()) }
    single<AiTool>(named("BochaSearch")) { BochaSearchTool(get(), get()) }
    single { ToolRegistry(getAll()) }
    single<ToolApprovalGateway> { RiskBasedToolApprovalGateway }
    single<PlatformToolExecutor> { createPlatformToolExecutor() }
    single { ToolCallCoordinator(get(), get()) }
    single<AiRepository> { DefaultAiRepository(get(), get()) }
    single { modelConfigRepository }
    single { profileStore }
    single { appSettingsStore }
    single { characterRepository }
    single { chatSessionRepository }
    single { databaseManager }
    single<CharacterCardImporter> { DefaultCharacterCardImporter(get()) }
    single<CharacterCardExporter> { getCharacterCardExporter() }
    single<PublicCharacterRepository> { DefaultPublicCharacterRepository(get(), get()) }
    single<KmpFileManager> { KmpFileManager.Default }
    single<UpdateRepository> { DefaultUpdateRepository(get()) }
    factory { MainViewModel(get(), get()) }
    factory { ChatMessageSender(get()) }
    factory { CharactersViewModel(get(), get(), get()) }
    factory { parameters ->
        val args = parameters.get<CharacterEditArgs>()
        CharacterEditViewModel(
            characterId = args.characterId,
            importPath = args.importPath,
            importName = args.importName,
            importExtension = args.importExtension,
            characterRepository = get(),
            modelConfigRepository = get(),
            aiRepository = get(),
            characterCardImporter = get(),
            characterCardExporter = get(),
            fileManager = get(),
            imageProviderRegistry = get(),
            httpClient = get(),
        )
    }
    factory {
        ChatViewModel(
            characterRepository = get(),
            chatSessionRepository = get(),
            modelConfigRepository = get(),
            aiRepository = get(),
            profileStore = get(),
            chatMessageSender = get(),
            fileManager = get(),
        )
    }
    factory { SettingsViewModel() }
    factory { parameters ->
        ApiSettingsViewModel(
            isMultimodal = parameters.get<Boolean>(0),
            isVoice = parameters.get<Boolean>(1),
            isImageGeneration = parameters.get<Boolean>(2),
            aiRepository = get(),
            modelConfigRepository = get(),
            imageProviderRegistry = get(),
        )
    }
    factory { SettingsOverviewViewModel(get()) }
    factory { ProfileViewModel(get(), get()) }
}
