import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(projects.shared)

    implementation(libs.napier)
    implementation(libs.filekit.core)
    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "com.kaixuan.starrailchatbox.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "崩铁ChatBox"
            packageVersion = "1.0.0"

            windows {
                iconFile.set(project.file("src/main/resources/app-icon.ico"))
            }
            macOS {
                iconFile.set(project.file("src/main/resources/app-icon.icns"))
            }
            linux {
                iconFile.set(project.file("src/main/resources/app-icon.png"))
            }
        }
    }
}
