package com.kaixuan.starrailchatbox

import android.app.Application
import com.kaixuan.starrailchatbox.data.database.PersistentRepositories
import com.kaixuan.starrailchatbox.data.database.createPersistentRepositories

class StarRailApplication : Application() {
    val repositories: PersistentRepositories by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPersistentRepositories(this)
    }
}
