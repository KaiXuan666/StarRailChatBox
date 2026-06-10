package com.kaixuan.starrailchatbox.ui.main

import com.kaixuan.starrailchatbox.ui.navigation.Route

sealed interface MainAction {
    // 导航到指定路由 (通常在点击底部或侧边 Tab 时重置路由栈为单元素)
    data class NavigationSelected(val route: Route) : MainAction

    // 将二级页面压入当前回退栈
    data class NavigateTo(val route: Route) : MainAction
    
    // 返回上一级页面
    data object PopBackStack : MainAction
    
    // 在设置页点击了具体某项设置（主要由 MainAction 处理 API_SETTINGS 导航与 THEME_STYLE 主题弹窗）
    data class SettingsItemClicked(val item: MainSettingsItem) : MainAction
    
    // 确认主题切换
    data class ThemeDialogConfirm(val themeOverride: Boolean?) : MainAction
    
    // 取消主题选择
    data object ThemeDialogDismiss : MainAction
}

/**
 * 为了完全隔离，由于 SettingsItem 会在主容器和设置页共有，我们在主模块也定义对应的 Settings 选项枚举类型。
 * 这对应于我们原来的 SettingsItem，以防与 SettingsAction 内的动作冲突。
 */
enum class MainSettingsItem {
    PROFILE,
    API_SETTINGS,
    MULTIMODAL_API_SETTINGS,
    IMAGE_GENERATION_API_SETTINGS,
    VOICE_API_SETTINGS,
    CHECK_UPDATE,
    MESSAGE_NOTIFICATION,
    THEME_STYLE,
    ABOUT_US,
    PRIVACY_SECURITY,
}
