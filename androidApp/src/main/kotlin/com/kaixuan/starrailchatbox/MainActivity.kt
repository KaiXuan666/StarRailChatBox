package com.kaixuan.starrailchatbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.kaixuan.starrailchatbox.data.character.DefaultCharacterRepository
import com.kaixuan.starrailchatbox.data.character.createCharacterStorage
import com.kaixuan.starrailchatbox.data.settings.createApiSettingsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            App(
                apiSettingsStore = createApiSettingsStore(
                    filesDir.resolve("api_settings.preferences_pb").absolutePath,
                ),
                characterRepository = DefaultCharacterRepository(
                    createCharacterStorage(filesDir.resolve("characters").absolutePath),
                ),
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
