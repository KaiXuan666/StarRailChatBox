package com.kaixuan.starrailchatbox.di

import com.kaixuan.starrailchatbox.data.ai.AiProvider
import com.kaixuan.starrailchatbox.data.ai.AiProviderRegistry
import com.kaixuan.starrailchatbox.data.ai.AiRepository
import com.kaixuan.starrailchatbox.data.ai.DefaultAiRepository
import com.kaixuan.starrailchatbox.data.ai.OpenAiCompatibleProvider
import com.kaixuan.starrailchatbox.data.ai.tool.AiTool
import com.kaixuan.starrailchatbox.data.ai.tool.QuickRepliesTool
import com.kaixuan.starrailchatbox.data.ai.tool.VoiceSynthesisTool
import com.kaixuan.starrailchatbox.data.ai.tool.BochaSearchTool
import com.kaixuan.starrailchatbox.data.ai.tool.RiskBasedToolApprovalGateway
import com.kaixuan.starrailchatbox.data.ai.tool.PlatformToolExecutor
import com.kaixuan.starrailchatbox.data.ai.tool.ToolApprovalGateway
import com.kaixuan.starrailchatbox.data.ai.tool.ToolCallCoordinator
import com.kaixuan.starrailchatbox.data.ai.tool.ToolRegistry
import com.kaixuan.starrailchatbox.data.ai.tool.createPlatformToolExecutor
import com.kaixuan.starrailchatbox.data.api.createPlatformHttpClient
import com.kaixuan.starrailchatbox.data.database.DatabaseManager
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.settings.AppSettingsStore
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import com.kaixuan.starrailchatbox.ui.main.MainViewModel
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
    single<AiProvider> { OpenAiCompatibleProvider(get()) }
    single { AiProviderRegistry(getAll()) }
    single<AiTool>(named("QuickReplies")) { QuickRepliesTool() }
    single<AiTool>(named("VoiceSynthesis")) { VoiceSynthesisTool(get(), get()) }
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
    factory { MainViewModel(get()) }
    factory { ChatViewModel(get(), get(), get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { ProfileViewModel(get(), get()) }
}
