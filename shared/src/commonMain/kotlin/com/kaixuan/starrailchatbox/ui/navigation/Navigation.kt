package com.kaixuan.starrailchatbox.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.mutableStateListOf

/**
 * 对应 Navigation 3 设计理念的类型安全路由接口。
 * 不需要复杂的字符串映射，每一个类或对象都代表一个具体目的地。
 */
sealed interface Route {
    // “对话” Tab 主屏幕
    data object ChatSession : Route

    // 当前角色的对话管理二级界面
    data object ConversationManagement : Route

    // 当前角色的角色编辑二级界面
    data class CharacterEdit(val characterId: String?) : Route

    // “角色” Tab 占位屏幕
    data object Characters : Route
    
    // “我的/设置” Tab 主屏幕
    data object Settings : Route
    
    // API 配置二级界面 (挂载在“我的”页面之下)
    data object ApiSettings : Route

    // 个人信息二级界面 (挂载在“我的”页面之下)
    data object Profile : Route

    // 某个特定角色的二级对话界面
    data class CharacterChat(val characterId: String) : Route
}

/**
 * 封装的 EntryProvider DSL，用于将路由 Key 映射至对应的 Composable 组件渲染。
 */
class EntryProvider<T : Any>(
    private val contentMap: Map<kotlin.reflect.KClass<out T>, @Composable (T) -> Unit>
) {
    @Composable
    fun Render(key: T) {
        val content = contentMap[key::class]
        if (content != null) {
            content(key)
        }
    }
}

class EntryProviderBuilder<T : Any> {
    val contentMap = mutableMapOf<kotlin.reflect.KClass<out T>, @Composable (T) -> Unit>()
    
    inline fun <reified K : T> entry(noinline content: @Composable (K) -> Unit) {
        contentMap[K::class] = { content(it as K) }
    }
}

fun <T : Any> entryProvider(builder: EntryProviderBuilder<T>.() -> Unit): EntryProvider<T> {
    val builderInstance = EntryProviderBuilder<T>().apply(builder)
    return EntryProvider(builderInstance.contentMap)
}

/**
 * 对应 Navigation 3 的 NavDisplay 渲染组件。
 * 观察当前的回退栈 (backstack)，将栈顶目的地呈现至 UI。
 */
@Composable
fun <T : Any> NavDisplay(
    backstack: List<T>,
    entryProvider: EntryProvider<T>
) {
    val currentKey = backstack.lastOrNull()
    if (currentKey != null) {
        entryProvider.Render(currentKey)
    }
}
