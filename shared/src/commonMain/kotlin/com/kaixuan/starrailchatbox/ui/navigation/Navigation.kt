package com.kaixuan.starrailchatbox.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    // “对话” Tab 主屏幕
    @Serializable
    data object ChatSession : Route

    // 当前角色的对话管理二级界面
    @Serializable
    data object ConversationManagement : Route

    // 当前角色的角色编辑二级界面
    @Serializable
    data class CharacterEdit(
        val characterId: String?,
        val importPath: String? = null,
        val importName: String? = null,
        val importExtension: String? = null,
    ) : Route

    // “角色” Tab 占位屏幕
    @Serializable
    data object Characters : Route
    
    // “我的/设置” Tab 主屏幕
    @Serializable
    data object Settings : Route
    
    // API 配置二级界面 (挂载在“我的”页面之下)
    @Serializable
    data object ApiSettings : Route

    // 多模态 API 配置二级界面 (挂载在“我的”页面之下)
    @Serializable
    data object MultimodalApiSettings : Route

    // 语音合成 API 配置二级界面 (挂载在“我的”页面之下)
    @Serializable
    data object VoiceApiSettings : Route

    // 图片生成 API 配置二级界面 (挂载在“我的”页面之下)
    @Serializable
    data object ImageGenerationApiSettings : Route

    // 个人信息二级界面 (挂载在“我的”页面之下)
    @Serializable
    data object Profile : Route

    // 关于界面 (挂载在“我的”页面之下)
    @Serializable
    data object About : Route

    // 隐私与安全界面 (挂载在“我的”页面之下)
    @Serializable
    data object PrivacyPolicy : Route

    // 某个特定角色的二级对话界面
    @Serializable
    data class CharacterChat(val characterId: String) : Route
}
