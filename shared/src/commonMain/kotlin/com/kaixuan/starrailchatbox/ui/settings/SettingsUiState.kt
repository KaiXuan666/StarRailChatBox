package com.kaixuan.starrailchatbox.ui.settings

import androidx.compose.runtime.Immutable

/**
 * 设置与 API 配置模块状态
 */
@Immutable
data class SettingsUiState(
    val apiHost: String = "https://api.openai.com/v1",
    val apiKey: String = "",
    val showApiKey: Boolean = false,
    val modelsList: List<String> = emptyList(),
    val selectedModel: String = "",
    val isFetchingModels: Boolean = false,
    val isSaving: Boolean = false,

    // 多模态 API 设置
    val multimodalApiHost: String = "https://api.openai.com/v1",
    val multimodalApiKey: String = "",
    val multimodalShowApiKey: Boolean = false,
    val multimodalModelsList: List<String> = emptyList(),
    val multimodalSelectedModel: String = "",
    val multimodalIsFetchingModels: Boolean = false,
    val multimodalIsSaving: Boolean = false,

    // 语音合成 API 设置
    val voiceApiHost: String = "https://api.openai.com/v1",
    val voiceApiKey: String = "",
    val voiceShowApiKey: Boolean = false,
    val voiceModelsList: List<String> = emptyList(),
    val voiceSelectedModel: String = "",
    val voiceSelectedCloneModel: String = "",
    val voiceIsFetchingModels: Boolean = false,
    val voiceIsSaving: Boolean = false,

    // 图片生成 API 设置
    val imageGenerationApiHost: String = "https://dashscope.aliyuncs.com/compatible-mode/v1",
    val imageGenerationApiKey: String = "",
    val imageGenerationShowApiKey: Boolean = false,
    val imageGenerationModelsList: List<String> = emptyList(),
    val imageGenerationSelectedModel: String = "",
    val imageGenerationIsFetchingModels: Boolean = false,
    val imageGenerationIsSaving: Boolean = false,

    // 配置状态（真实持久化状态）
    val isDefaultConfigured: Boolean = false,
    val isMultimodalConfigured: Boolean = false,
    val isVoiceConfigured: Boolean = false,
    val isImageGenerationConfigured: Boolean = false,

    // 更新弹窗状态
    val showUpdateDialog: Boolean = false,
    val updateInfo: UpdateInfo? = null,
)

@Immutable
data class UpdateInfo(
    val version: String,
    val description: String,
    val downloadUrl: String,
    val isForceUpdate: Boolean = false,
)

