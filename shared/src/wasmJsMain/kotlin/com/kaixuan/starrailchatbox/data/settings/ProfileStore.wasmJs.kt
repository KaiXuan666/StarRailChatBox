package com.kaixuan.starrailchatbox.data.settings

actual fun createProfileStore(path: String?): ProfileStore = InMemoryProfileStore()
