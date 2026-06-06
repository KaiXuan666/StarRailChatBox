package com.kaixuan.starrailchatbox.di

import com.kaixuan.starrailchatbox.data.api.KtorfitOpenAiRepository
import com.kaixuan.starrailchatbox.data.api.OpenAiRepository
import com.kaixuan.starrailchatbox.data.api.createPlatformHttpClient
import com.kaixuan.starrailchatbox.data.settings.ApiSettingsStore
import com.kaixuan.starrailchatbox.ui.settings.SettingsViewModel
import org.koin.dsl.module

fun appModule(apiSettingsStore: ApiSettingsStore) = module {
    single { createPlatformHttpClient() }
    single<OpenAiRepository> { KtorfitOpenAiRepository(get()) }
    single { apiSettingsStore }
    factory { SettingsViewModel(get(), get()) }
}
