package com.kaixuan.starrailchatbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val repositories = (application as StarRailApplication).repositories

        setContent {
            App(
                modelConfigRepository = repositories.modelConfigRepository,
                profileStore = repositories.profileStore,
                appSettingsStore = repositories.appSettingsStore,
                characterRepository = repositories.characterRepository,
                chatSessionRepository = repositories.chatSessionRepository,
                databaseManager = repositories.databaseManager,
            )
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
