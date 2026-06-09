package com.kaixuan.starrailchatbox.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.icerock.moko.permissions.DeniedAlwaysException
import dev.icerock.moko.permissions.DeniedException
import dev.icerock.moko.permissions.Permission
import dev.icerock.moko.permissions.RequestCanceledException
import dev.icerock.moko.permissions.compose.BindEffect
import dev.icerock.moko.permissions.compose.rememberPermissionsControllerFactory
import dev.icerock.moko.permissions.microphone.RECORD_AUDIO

@Composable
actual fun rememberRecordAudioPermissionController(): RecordAudioPermissionController {
    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    return remember(controller) {
        object : RecordAudioPermissionController {
            override suspend fun isPermissionGranted(): Boolean {
                return controller.isPermissionGranted(Permission.RECORD_AUDIO)
            }

            override suspend fun providePermission(): Boolean {
                return try {
                    controller.providePermission(Permission.RECORD_AUDIO)
                    true
                } catch (_: DeniedAlwaysException) {
                    false
                } catch (_: DeniedException) {
                    false
                } catch (_: RequestCanceledException) {
                    false
                }
            }
        }
    }
}
