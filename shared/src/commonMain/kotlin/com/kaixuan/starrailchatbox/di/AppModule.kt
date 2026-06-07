package com.kaixuan.starrailchatbox.di

import com.kaixuan.starrailchatbox.data.api.KtorfitOpenAiRepository
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.api.createPlatformHttpClient
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.character.CharacterRepository
import com.kaixuan.starrailchatbox.data.chat.ChatSessionRepository
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import com.kaixuan.starrailchatbox.ui.chat.ChatViewModel
import org.koin.dsl.module

fun appModule(
    modelConfigRepository: ModelConfigRepository,
    profileStore: ProfileStore,
    characterRepository: CharacterRepository,
    chatSessionRepository: ChatSessionRepository,
) = module {
    single { createPlatformHttpClient() }
    single<OpenAiRepository> { KtorfitOpenAiRepository(get()) }
    single { modelConfigRepository }
    single { profileStore }
    single { characterRepository }
    single { chatSessionRepository }
    factory { ChatViewModel(get(), get(), get(), get()) }
    factory { SettingsViewModel(get(), get()) }
    factory { ProfileViewModel(get()) }
}
