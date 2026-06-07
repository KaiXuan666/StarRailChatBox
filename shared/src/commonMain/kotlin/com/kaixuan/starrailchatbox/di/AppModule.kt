package com.kaixuan.starrailchatbox.di

import com.kaixuan.starrailchatbox.data.api.KtorfitOpenAiRepository
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.api.createPlatformHttpClient
import com.kaixuan.starrailchatbox.data.model.ModelConfigRepository
import com.kaixuan.starrailchatbox.data.settings.ProfileStore
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import com.kaixuan.starrailchatbox.ui.profile.ProfileViewModel
import org.koin.dsl.module

fun appModule(
    modelConfigRepository: ModelConfigRepository,
    profileStore: ProfileStore
) = module {
    single { createPlatformHttpClient() }
    single<OpenAiRepository> { KtorfitOpenAiRepository(get()) }
    single { modelConfigRepository }
    single { profileStore }
    factory { SettingsViewModel(get(), get()) }
    factory { ProfileViewModel(get()) }
}
