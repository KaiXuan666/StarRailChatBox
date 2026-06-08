package com.kaixuan.starrailchatbox.data.settings

actual fun createProfileStore(path: String?, context: Any?): ProfileStore = InMemoryProfileStore()
