package com.kaixuan.starrailchatbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.data.database.createPersistentRepositories
import com.kaixuan.starrailchatbox.data.settings.createProfileStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repositories = createPersistentRepositories(this)

        setContent {
            App(
                modelConfigRepository = repositories.modelConfigRepository,
                profileStore = createProfileStore(
                    path = filesDir.resolve("profile_settings.preferences_pb").absolutePath,
                    context = this,
                ),
                characterRepository = repositories.characterRepository,
                chatSessionRepository = repositories.chatSessionRepository,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
