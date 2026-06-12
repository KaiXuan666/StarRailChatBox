import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

@DisableCachingByDefault(because = "The generated source contains local development credentials.")
abstract class GenerateLocalApiSettingsTask : DefaultTask() {
    @get:InputFile
    @get:Optional
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val localPropertiesFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val properties = Properties()
        localPropertiesFile.asFile.get()
            .takeIf(File::isFile)
            ?.reader(Charsets.UTF_8)
            ?.use(properties::load)

        fun propertyValue(name: String): String {
            return properties.getProperty(name).orEmpty()
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n")
                .replace("$", "\\$")
        }

        val outputFile = outputDirectory.file(
            "com/kaixuan/starrailchatbox/data/settings/LocalApiSettings.kt",
        ).get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package com.kaixuan.starrailchatbox.data.settings

            internal object LocalApiSettings {
                const val apiHost = "${propertyValue("OPENAI_API_HOST")}"
                const val apiKey = "${propertyValue("OPENAI_API_KEY")}"
                const val multimodalHost = "${propertyValue("MULTIMODAL_API_HOST")}"
                const val multimodalKey = "${propertyValue("MULTIMODAL_API_KEY")}"
                const val voiceHost = "${propertyValue("VOICE_API_HOST")}"
                const val voiceKey = "${propertyValue("VOICE_API_KEY")}"
                const val imageHost = "${propertyValue("IMAGE_GENERATION_API_HOST")}"
                const val imageKey = "${propertyValue("IMAGE_GENERATION_API_KEY")}"
            }
            """.trimIndent() + "\n",
            Charsets.UTF_8,
        )
    }
}

val rootLocalPropertiesFile = rootProject.layout.projectDirectory.file("local.properties")
val generatedLocalApiSettingsDirectory =
    layout.buildDirectory.dir("generated/localApiSettings/commonMain/kotlin")

val generateLocalApiSettings by tasks.registering(GenerateLocalApiSettingsTask::class) {
    localPropertiesFile.set(rootLocalPropertiesFile)
    outputDirectory.set(generatedLocalApiSettingsDirectory)
}

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktorfit)
    alias(libs.plugins.ksp)
}

ktorfit {
    compilerPluginVersion = "-"
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    jvm()
    
    js {
        browser()
    }
    
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    androidLibrary {
       namespace = "com.kaixuan.starrailchatbox.shared"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }

    applyDefaultHierarchyTemplate()
    
    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.ktor.client.okhttp)
            implementation(libs.androidx.activity.compose)
            implementation(libs.mokoPermissionsCompose)
            implementation(libs.mokoPermissionsMicrophone)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.coil.compose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelNavigation3)
            implementation(libs.navigation3.ui)
            implementation(libs.paging.common)
            implementation(libs.paging.compose)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.contentNegotiation)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.serialization.kotlinxJson)
            implementation(libs.ktorfit.lib)
            implementation(libs.koin.core)
            implementation(libs.napier)
            implementation(libs.cryptography.core)
            implementation(libs.cryptography.provider.optimal)
            implementation(libs.filekit.core)
            implementation(libs.filekit.dialogs.compose)
            implementation(libs.filekit.coil)
            implementation(libs.okio)
        }
        commonMain {
            kotlin.srcDir(layout.buildDirectory.dir("generated/ksp/metadata/commonMain/kotlin"))
            kotlin.srcDir(generatedLocalApiSettingsDirectory)
        }
        val roomMain by creating {
            dependsOn(commonMain.get())
            dependencies {
                implementation(libs.datastore.preferences)
                implementation(libs.room.runtime)
                implementation(libs.room.paging)
                implementation(libs.sqlite.bundled)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.paging.testing)
            implementation(libs.ktor.client.mock)
            implementation(libs.okio.fakefilesystem)
        }
        jsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        wasmJsMain.dependencies {
            implementation(libs.wrappers.browser)
            implementation(libs.ktor.client.js)
        }
        jvmMain.dependencies {
            implementation(libs.ktor.client.cio)
        }
        jvmMain.get().dependsOn(roomMain)
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.mokoPermissionsCompose)
            implementation(libs.mokoPermissionsMicrophone)
        }
        iosMain.get().dependsOn(roomMain)
        androidMain.get().dependsOn(roomMain)
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
    add("kspCommonMainMetadata", libs.ktorfit.ksp)
    add("kspAndroid", libs.room.compiler)
    add("kspJvm", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)
    add("kspIosSimulatorArm64", libs.room.compiler)
}

tasks.matching {
    it.name.startsWith("compileKotlin") || it.name == "compileAndroidMain"
}.configureEach {
    dependsOn("kspCommonMainKotlinMetadata")
    dependsOn(generateLocalApiSettings)
}

tasks.matching {
    it.name.startsWith("ksp")
}.configureEach {
    dependsOn(generateLocalApiSettings)
    if (name != "kspCommonMainKotlinMetadata") {
        dependsOn("kspCommonMainKotlinMetadata")
    }
}
