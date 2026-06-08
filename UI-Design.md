# StarRail Chat Box UI Design Guide

本文档是 StarRailChatBox 的 UI 实现规范，供后续 AI Agent、设计人员和开发人员共同遵循。

项目使用 Kotlin Multiplatform 与 Compose Multiplatform，UI 优先实现在
`shared/src/commonMain`。视觉设计基于现有浅色、深色聊天界面，并遵循 Material Design 3
的组件、语义颜色、排版、形状、状态与无障碍原则。

设计参考：

- [Figma：崩铁 Chat Box - 参考图复刻](https://www.figma.com/design/FzGGzLdd2apwFHX2bh3kBn)
- 浅色模式：明亮、柔和、轻盈的星空玻璃质感
- 深色模式：深蓝星空背景、青蓝高光、低亮度分层表面

---

## 1. 核心原则

### 1.1 Material 3 优先

- 必须优先使用 `androidx.compose.material3` 组件。
- 必须通过 `MaterialTheme.colorScheme`、`MaterialTheme.typography` 和
  `MaterialTheme.shapes` 获取视觉属性。
- 不得在业务 Composable 内直接使用十六进制颜色。
- 不得通过降低文字透明度来模拟禁用状态；应使用语义颜色与 M3 状态层。
- 自定义组件必须保留 M3 的交互语义、触控范围、状态反馈和无障碍能力。

### 1.2 语义优先于视觉值

业务代码只描述颜色用途，不描述具体色值。

正确：

```kotlin
Text(
    text = message,
    color = MaterialTheme.colorScheme.onSurface,
)
```

错误：

```kotlin
Text(
    text = message,
    color = Color(0xFF1D2550),
)
```

所有浅色与深色差异必须收敛在主题层。切换主题时，不应在页面内编写大量
`if (darkTheme)` 分支。

### 1.3 品牌装饰不能破坏可用性

星空、轨道、星芒、渐变和微光属于装饰层：

- 不承载唯一信息。
- 不作为文字或图标的必要背景。
- 不抢占点击事件。
- 不得导致正文对比度下降。
- 在“减少动态效果”环境下必须停止或简化动画。

---

## 2. 工程组织

推荐将 UI 按以下结构组织：

```text
shared/src/commonMain/kotlin/com/kaixuan/starrailchatbox/
├── App.kt
├── design/
│   ├── StarRailTheme.kt
│   ├── Color.kt
│   ├── Type.kt
│   ├── Shape.kt
│   ├── Spacing.kt
│   └── StarRailExtendedColors.kt
├── model/
│   ├── CharacterUiModel.kt
│   └── ChatMessageUiModel.kt
├── ui/
│   ├── main/
│   │   ├── MainNavigationContainer.kt
│   │   ├── MainUiState.kt
│   │   ├── MainAction.kt
│   │   ├── MainEffect.kt
│   │   └── MainViewModel.kt
│   ├── chat/
│   │   ├── ChatSessionScreen.kt
│   │   ├── ChatUiState.kt
│   │   ├── ChatAction.kt
│   │   ├── ChatEffect.kt
│   │   └── ChatViewModel.kt
│   ├── settings/
│   │   ├── SettingsScreen.kt
│   │   ├── ApiSettingsScreen.kt
│   │   ├── SettingsUiState.kt
│   │   ├── SettingsAction.kt
│   │   ├── SettingsEffect.kt
│   │   └── SettingsViewModel.kt
│   └── navigation/
│       └── Navigation.kt
└── resources/
```

要求：

- 页面负责组合、状态提升和事件转发。
- 小组件保持无状态，使用 `value`、`selected`、`enabled` 和回调参数。
- 相同语义、结构和视觉样式应优先复用已有公共组件；不得在不同页面复制返回按钮、
  主操作按钮等通用实现。确需差异时，应通过公共组件参数或主题令牌表达。
- 带返回按钮的二级页面必须复用统一页面骨架（当前为 `StarRailPageLayout`），由公共
  布局统一处理顶部安全区、水平边距、标题栏尺寸与位置、滚动容器和底部 inset。
  页面不得自行复制标题栏，或单独计算 `statusBars` 顶部间距。
- 可见文本必须使用 Compose Multiplatform Resources。
- 图片和图标必须提供语义名称；纯装饰资源使用 `contentDescription = null`。
- Preview 应覆盖浅色、深色、长文本和紧凑宽度。

---

## 3. 主题入口

应用必须提供统一主题：

```kotlin
@Composable
fun StarRailTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme =
        if (darkTheme) StarRailDarkColorScheme
        else StarRailLightColorScheme

    CompositionLocalProvider(
        LocalStarRailExtendedColors provides
            if (darkTheme) DarkExtendedColors else LightExtendedColors,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = StarRailTypography,
            shapes = StarRailShapes,
            content = content,
        )
    }
}
```

主题模式至少支持：

- 跟随系统。
- 强制浅色。
- 强制深色。

Android 动态颜色可以作为未来的可选设置，但默认品牌主题必须保持两套设计稿的视觉识别。

---

## 4. Material 3 语义颜色

以下色值是主题层的建议起点。实现时允许根据真实设备截图微调，但不得改变语义用途。

### 4.1 浅色方案

| M3 角色 | 建议值 | 用途 |
|---|---:|---|
| `primary` | `#4F6FFF` | 选中状态、主要操作、发送按钮 |
| `onPrimary` | `#FFFFFF` | 主色按钮上的图标或文字 |
| `primaryContainer` | `#E7EEFF` | 自己发送的消息气泡、弱强调区域 |
| `onPrimaryContainer` | `#174F9F` | 自己发送的消息文字 |
| `secondary` | `#6257D9` | 紫色品牌辅助色、剧情操作 |
| `onSecondary` | `#FFFFFF` | 辅助色上的内容 |
| `secondaryContainer` | `#ECE9FF` | 次级选中状态 |
| `onSecondaryContainer` | `#30266F` | 次级容器内容 |
| `tertiary` | `#008FA8` | 在线状态、语音和青色强调 |
| `onTertiary` | `#FFFFFF` | 青色表面上的内容 |
| `tertiaryContainer` | `#C8F3FA` | 在线提示、弱青色容器 |
| `onTertiaryContainer` | `#00363F` | 青色容器内容 |
| `background` | `#F7F9FF` | 页面背景 |
| `onBackground` | `#171D38` | 页面主要文字 |
| `surface` | `#FBFCFF` | 默认卡片、消息气泡 |
| `onSurface` | `#202947` | 卡片和气泡主要文字 |
| `surfaceVariant` | `#EEF2FC` | 输入框、快捷回复、低层容器 |
| `onSurfaceVariant` | `#626D8D` | 次要文字、时间、占位文字 |
| `surfaceContainerLowest` | `#FFFFFF` | 浮起的最亮表面 |
| `surfaceContainerLow` | `#F5F7FE` | 页面区域容器 |
| `surfaceContainer` | `#EFF3FC` | 输入框和普通卡片 |
| `surfaceContainerHigh` | `#E8EDF8` | 按下、悬停或高层容器 |
| `surfaceContainerHighest` | `#E1E7F3` | 最高层非模态表面 |
| `outline` | `#7883A3` | 强边界、焦点外框 |
| `outlineVariant` | `#CBD4EA` | 卡片细边框、分割线 |
| `error` | `#BA1A1A` | 错误状态 |
| `onError` | `#FFFFFF` | 错误色上的内容 |
| `errorContainer` | `#FFDAD6` | 错误提示容器 |
| `onErrorContainer` | `#410002` | 错误容器内容 |
| `scrim` | `#000000` | 模态遮罩，按 M3 透明度使用 |

### 4.2 深色方案

| M3 角色 | 建议值 | 用途 |
|---|---:|---|
| `primary` | `#58E7F2` | 当前导航、发送按钮、选中指示 |
| `onPrimary` | `#00363C` | 青色主操作上的内容 |
| `primaryContainer` | `#173F66` | 自己发送的消息气泡 |
| `onPrimaryContainer` | `#E9F7FF` | 自己发送的消息文字 |
| `secondary` | `#B8A9FF` | 紫色品牌辅助强调 |
| `onSecondary` | `#2D2465` | 辅助色上的内容 |
| `secondaryContainer` | `#443A80` | 紫色弱强调容器 |
| `onSecondaryContainer` | `#E7DEFF` | 紫色容器内容 |
| `tertiary` | `#49DCEB` | 在线状态、青色状态反馈 |
| `onTertiary` | `#00373D` | 青色表面上的内容 |
| `tertiaryContainer` | `#004F58` | 在线或连接状态容器 |
| `onTertiaryContainer` | `#9CF0FA` | 状态容器内容 |
| `background` | `#020817` | 页面背景 |
| `onBackground` | `#EEF3FF` | 页面主要文字 |
| `surface` | `#08152E` | 默认深色表面 |
| `onSurface` | `#EDF2FF` | 气泡和卡片主要文字 |
| `surfaceVariant` | `#111D38` | 对方气泡、输入框、快捷回复 |
| `onSurfaceVariant` | `#A9B5D1` | 次要文字和时间 |
| `surfaceContainerLowest` | `#020817` | 最低层背景 |
| `surfaceContainerLow` | `#071229` | 低层卡片 |
| `surfaceContainer` | `#0B1731` | 输入框和常规容器 |
| `surfaceContainerHigh` | `#101D39` | 对方气泡、较高层卡片 |
| `surfaceContainerHighest` | `#172440` | 按下、悬停或最高层容器 |
| `outline` | `#7282A4` | 强边界、键盘焦点 |
| `outlineVariant` | `#283B5C` | 卡片边框和分割线 |
| `error` | `#FFB4AB` | 错误状态 |
| `onError` | `#690005` | 错误色上的内容 |
| `errorContainer` | `#93000A` | 错误提示容器 |
| `onErrorContainer` | `#FFDAD6` | 错误容器内容 |
| `scrim` | `#000000` | 模态遮罩 |

### 4.3 扩展语义色

M3 `ColorScheme` 无法表达少量产品专属用途时，使用不可变扩展色类，而不是在组件中硬编码。

```kotlin
@Immutable
data class StarRailExtendedColors(
    val online: Color,
    val onOnline: Color,
    val constellation: Color,
    val constellationMuted: Color,
    val sentBubbleBorder: Color,
    val receivedBubbleBorder: Color,
    val avatarRingStart: Color,
    val avatarRingEnd: Color,
    val successCheck: Color,
)
```

扩展色只允许用于：

- 在线状态。
- 星空装饰。
- 头像渐变环。
- 消息气泡的品牌边框。
- 消息送达或已读标记。

普通文字、卡片、按钮、导航、输入框仍必须使用 M3 标准语义色。

---

## 5. 色彩使用规则

| 界面元素 | 语义角色 |
|---|---|
| 页面背景 | `background` |
| 页面主要文字 | `onBackground` |
| 对方消息气泡 | `surfaceContainerHigh` + `onSurface` |
| 自己消息气泡 | `primaryContainer` + `onPrimaryContainer` |
| 角色选择卡片 | `surfaceContainerLow` |
| 输入框 | `surfaceContainer` + `onSurface` |
| 输入占位文字 | `onSurfaceVariant` |
| 快捷回复容器 | `surfaceContainerLow` |
| 快捷回复图标 | `secondary` 或 `tertiary` |
| 当前导航项 | `primary` |
| 未选导航项 | `onSurfaceVariant` |
| 普通细边框 | `outlineVariant` |
| 焦点、键盘导航边框 | `outline` 或 `primary` |
| 时间和辅助说明 | `onSurfaceVariant` |
| 主要发送按钮 | `primary` + `onPrimary` |
| 禁用内容 | `onSurface` 38% |
| 禁用容器 | `onSurface` 12% |

同一容器内的内容必须使用对应的 `on*` 色。例如：

- `primary` 上使用 `onPrimary`。
- `primaryContainer` 上使用 `onPrimaryContainer`。
- `surface` 上使用 `onSurface`。

禁止凭肉眼混用不成对的前景色和背景色。

---

## 6. 排版

字体应优先使用平台系统无衬线字体：

- Android / Desktop / Web：Noto Sans SC 或系统 sans-serif。
- iOS：系统中文字体。
- 时间和数字可以使用 Inter 或平台默认数字字体。

不得依赖只在开发机安装的字体。若需要品牌字体，必须作为 Compose Resource 随应用分发。

推荐排版映射：

| 场景 | M3 样式 | 建议 |
|---|---|---|
| 页面标题“崩铁 Chat Box” | `headlineLarge` | 32sp，Bold |
| 当前角色名称 | `headlineSmall` | 24sp，SemiBold |
| 角色卡片名称 | `titleMedium` | 16sp，Medium |
| 消息正文 | `bodyLarge` | 16sp，Regular，1.45 行高 |
| 描述文字 | `bodyMedium` | 14sp |
| 快捷回复 | `labelLarge` | 14sp，Medium |
| 时间、在线状态 | `bodySmall` | 12sp |
| 底部导航标签 | `labelMedium` | 12sp，Medium |

规则：

- 用户文字缩放到 200% 时，消息不得裁切或重叠。
- 不使用固定高度包裹可换行文字。
- 中文正文避免过紧字距。
- 时间、状态等辅助信息不得小于 11sp。
- 标题允许缩小，但不得截断为设计参考中的错误标题。

---

## 7. 间距与尺寸

使用 4dp 基础网格。推荐令牌：

```kotlin
object StarRailSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 24.dp
    val xl = 32.dp
    val xxl = 40.dp
}
```

关键尺寸：

| 元素 | 规格 |
|---|---|
| 页面水平内边距 | 手机 16dp；平板/桌面 24-32dp |
| 最小触控目标 | 48 x 48dp |
| 顶部主头像 | 72-88dp |
| 角色选择头像 | 56-64dp |
| 消息头像 | 40-44dp |
| 圆形操作按钮 | 48-56dp |
| 发送区域按钮 | 38-52dp |
| 输入框最小高度 | 38-56dp |
| 底部导航栏高度 | 72-80dp |
| 消息气泡最大宽度 | 手机可用宽度的 72%-78% |
| 卡片内部间距 | 12-16dp |
| 消息组间距 | 12-20dp |
| 快捷回复间距 | 8-12dp |

不得将设计图的 942px 直接当作 Compose dp。移动端以逻辑尺寸和约束布局实现。

---

## 8. 形状、边框与层级

使用 Material 3 `Shapes`：

```kotlin
val StarRailShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)
```

推荐映射：

- 消息气泡：`large`。
- 角色选择卡片：`large`。
- 快捷回复：`extraLarge` 或胶囊形。
- 输入框：胶囊形。
- 图标按钮：圆形。
- 头像：圆形。

边框：

- 普通卡片使用 1dp `outlineVariant`。
- 选中项使用 2dp `primary` 或品牌头像渐变环。
- 键盘焦点必须有清晰的 2dp 焦点环。
- 深色模式不能仅依靠阴影区分层级，应结合 surface container 角色和边框。

阴影应克制：

- 浅色模式可使用 1-3dp 的柔和层级阴影。
- 深色模式主要依靠表面亮度和描边，避免大面积黑色阴影。
- 发光仅用于主要操作、选中头像或星芒，不用于所有容器。

### 8.1 统一 Dialog

所有业务弹窗必须复用公共 `StarRailDialog`，不得直接使用默认 `AlertDialog`，也不得在
业务页面复制 `Dialog + Surface + 按钮` 的弹窗骨架。`ThemeStyleDialog` 是统一视觉的
基准实现，其容器、标题与操作区已收敛到 `StarRailDialog`。

统一弹窗结构：

- 使用 Compose `Dialog` 提供模态语义、遮罩、外部点击关闭和系统返回关闭行为。
- 容器使用 `surfaceContainerLow`、1dp `outlineVariant` 边框和 24dp 圆角。
- 内容区统一使用 24dp 内边距，各区块使用 20dp 垂直间距。
- 标题使用 `titleLarge` 加粗，左侧显示暖色星芒装饰。
- 标题下方使用由 `constellation` 渐变至透明的 1dp 分隔线。
- 操作区右对齐；取消与确认按钮均为 96 x 38dp 胶囊按钮，并提供按压缩放反馈。
- 普通确认按钮使用 `primary` 到 `secondary` 的品牌渐变及 `onPrimary` 文字。
- 删除等破坏性确认按钮设置 `destructive = true`，使用 `error` 容器语义与 `onError`
  文字；不得只用普通主色表达破坏性操作。
- 弹窗正文使用 `onSurface` 或 `onSurfaceVariant`，可换行且不得设置会裁切内容的固定高度。
- 弹窗打开时，系统返回键只关闭弹窗，不得同时触发页面返回或业务确认。
- 所有弹窗可见文案必须资源化，并提供浅色、深色 Preview。
- 弹窗内支持表单输入时（如提示词自动生成弹窗），可在 `content` 回调中放置 `LabeledTextField` 等输入框（如多行文本框建议 `minLines = 4`）。取消和确认按钮动作应精确对应并绑定 ViewModel 派发的 Action；Dialog 关闭时，ViewModel 必须清空或妥善保留临时输入草稿状态。

---

## 9. 页面结构

聊天页面按以下顺序构建：

```text
Scaffold
├── 背景与星空装饰
├── HorizontalPager (横向滑动 Page 容器，无切换动画，不启用 beyondViewportPageCount 预载)
│   └── Page (每个角色)
│       └── 可滚动内容 (LazyColumn，独立保存滚动状态)
│           ├── ChatHeader
│           ├── CharacterSelector (吸顶 stickyHeader)
│           ├── DateDivider
│           └── MessageList
├── QuickReplyRow
├── MessageComposer
└── NavigationBar
```


### 9.1 顶部区域

顶部必须沿用浅色设计的结构：

- 系统状态栏。
- 完整标题“崩铁 Chat Box”。
- 角色资料与操作区域。

不得采用深色参考图中的返回箭头和被截断的“崩铁Chat Bo”标题。

角色资料区包含：

- 当前角色头像。
- 角色名。
- 在线状态。
- 简介。
- 语音、档案、设置三个操作。

紧凑宽度下可将简介和操作区换行，但不得缩小触控目标。

### 9.2 角色选择器

- 使用横向列表或固定四列布局。
- 选中角色必须同时具有：强调边框、名称强调和底部指示条。
- 不能只依靠颜色表达选中状态。
- 每个角色项必须可通过键盘聚焦，并提供
  `"选择角色：星旅·流萤，当前已选择"` 等无障碍描述。

### 9.3 消息列表

- 使用 `LazyColumn`。
- 对方消息左对齐并显示头像。
- 自己消息右对齐，不重复显示自己的头像；可显示品牌星芒动作。
- 连续同一发送者的消息可以合并头像和时间，但数据语义不能丢失。
- 新消息到达时只在用户接近列表底部时自动滚动。
- 用户正在阅读历史消息时，不得强制抢夺滚动位置。
- 点击角色选择器中的头像（无论切换角色与否）时，对应消息列表必须自动平滑滚动到最底部。
- 当聊天列表离开最顶部时，需在页面左下角（底栏上方，适配 bottom 避开输入区并应用 Insets 间距）浮现一键回顶按钮。点击后平滑回滚到最顶部（露出 ChatHeader）。
  - 视觉与触控规范：回顶按钮使用 `StarRailIconKind.ARROW_UP` 图标（崩铁风格手绘矢量线条），背景采用 M3 的 `surfaceContainerHigh` 并设置透明度（`alpha = 0.88f`），圆角使用 `Shapes.medium`。其最小触控目标大小应保持 48 x 48dp。


消息类型建议：

```kotlin
sealed interface ChatMessageUiModel {
    val id: String
    val timestamp: String

    data class Received(...)
    data class Sent(...)
    data class System(...)
    data class Typing(...)
}
```

### 9.4 快捷回复

- 视觉上使用 M3 `AssistChip` 或等价语义的自定义胶囊组件。
- 改为两行展示，每行最多展示两个（如果有的话），使用网格平分宽度。
- 限制物理高度不滚动：在 compact 模式下固定高度为 `32.dp`，正常模式下为 `40.dp`，子按钮和占位填充元素均设为 `fillMaxHeight` 并使其内部文字垂直居中。
- 图标只辅助识别，文字始终保留。
- 点击后应有 pressed、loading 或已提交反馈。

### 9.5 消息输入区

输入区包含：

- 附加操作按钮。
- 文本输入框。
- 表情按钮。
- 发送按钮。
- 语音按钮。

行为：

- 空文本时发送按钮禁用。
- 多行输入最多显示 4-6 行，之后内部滚动。
- Enter 行为按平台适配；桌面端可用 Enter 发送、Shift+Enter 换行。
- 发送中显示进度，但避免改变按钮尺寸。
- 输入内容不得因旋转、窗口缩放或主题切换而丢失。

### 9.6 底部导航

使用 Material 3 `NavigationBar` 与 `NavigationBarItem`，包含：

- 对话
- 角色
- 发现
- 我的

当前项同时通过图标状态和标签颜色强调。不要只显示颜色变化。

桌面和大屏可以将底部导航替换为 `NavigationRail`，但目的地、顺序与语义保持一致。

---

## 10. 响应式布局

不要假设所有平台都是手机竖屏。

### Compact：宽度小于 600dp

- 单列手机布局。
- 底部 `NavigationBar`。
- 顶部资料区允许换行。
- 消息气泡最大宽度约 78%。
- 快捷回复为两行两列网格自适应展示，限高不滚动。

### Medium：600-839dp

- 内容居中，最大宽度 720dp。
- 顶部角色资料与操作可保持同一行。
- 可使用 `NavigationRail` 或底部导航。
- 消息气泡最大宽度约 68%。

### Expanded：840dp 及以上

- 使用双栏或三栏布局。
- 左侧角色/会话导航，中间聊天，右侧可选角色资料。
- 聊天主列建议限制在 720-840dp。
- 禁止把手机界面等比例放大铺满桌面。

实现可基于 `BoxWithConstraints` 或项目统一的 Window Size Class 抽象。

---

## 11. 系统区域和平台适配

- 根容器必须处理系统栏、刘海、圆角和手势区域。
- Compose UI 使用 `WindowInsets.safeDrawing`、`safeDrawingPadding()` 或等价方式。
- 同级二级页面的标题栏必须由同一公共页面布局处理系统区域和外边距，确保返回按钮、
  标题字号、水平位置与顶部位置一致。
- 底部输入区和导航不得被 iOS Home Indicator 或 Android 手势条遮挡。
- Web 和 Desktop 应支持鼠标悬停、滚轮、键盘 Tab 与窗口缩放。
- iOS 保持 Compose 共享界面，不在 SwiftUI 重复实现同一聊天页。
- 平台差异应通过 `expect/actual` 或小型适配层处理，避免复制整个页面。

---

## 12. 图标与资源

- 优先使用 Material Symbols / Material Icons 中语义相符的图标。
- 品牌星芒、头像环和星空背景作为项目资源维护。
- 同一动作必须在全应用使用同一图标。
- 图标默认尺寸 24dp；紧凑辅助图标可为 18-22dp。
- 图标按钮必须使用 `IconButton` 或具备等价语义和 48dp 触控范围。
- 不使用 Emoji 代替正式 UI 图标。

头像图片：

- 使用圆形裁切。
- 设置裁切策略为 `ContentScale.Crop`。
- 为加载中、失败和无头像状态提供占位。
- 选中头像可使用品牌渐变环，但环外仍需满足布局尺寸约束。
- 头像图片需紧贴内圆裁切，头像与渐变外环之间避免使用内边距（如避免露出底色白圈）。

---

## 13. 状态与动效

所有交互组件至少覆盖：

- Enabled
- Hovered（Desktop/Web）
- Focused
- Pressed
- Selected
- Disabled
- Loading
- Error（适用时）

遵循 M3 状态层，不直接永久改变组件底色。

推荐动效：

- 主题切换：颜色渐变 200-300ms。
- 角色切换：Tab 选中瞬间直接跳转，无滑动或淡入动画，以实现即时高响应的秒切切换体验。此外，底层 `HorizontalPager` 不得配置任何预加载机制（如启用 `beyondViewportPageCount`），必须采用默认懒加载，以完全规避多页面同步测量与滚动带来的卡顿（Jank）。
- 新消息：轻微淡入和位移 160-220ms。
- 发送按钮：短促缩放或高光反馈，不超过 180ms。
- 输入中状态：低频率、低对比度动画。


禁止：

- 持续闪烁。
- 大幅视差。
- 长时间循环旋转。
- 影响阅读的背景移动。

---

## 14. 无障碍

必须满足：

- 普通文字与背景对比度至少 4.5:1。
- 大文字和关键图标至少 3:1。
- 最小触控目标 48 x 48dp。
- 所有交互元素可通过键盘访问。
- 焦点顺序与视觉顺序一致。
- 不只依靠颜色表达在线、选中、已读或错误。
- 动态更新使用适当 live region，避免重复朗读整页。
- 消息应能读出发送者、内容、时间和状态。
- 装饰星星、轨道、背景光晕的 `contentDescription` 必须为 `null`。

示例：

```kotlin
Modifier.semantics {
    contentDescription = "星旅·流萤，在线"
    selected = isSelected
}
```

---

## 15. AI Agent 实现规则

后续 AI Agent 修改 UI 时必须遵循以下流程：

1. 先读取本文件和相关现有组件。
2. 优先复用已有主题令牌和组件，不重复创建相同实现；新增页面样式前必须先检查
   `ui/components` 和相邻页面，已有同语义组件时直接复用。
3. 带返回导航的二级页面必须优先使用 `StarRailPageLayout`；不得在业务页面中重复
   实现返回标题栏或自行设置标题栏顶部安全区与外边距。
4. 新颜色先判断是否能映射到 M3 `ColorScheme`。
5. 只有产品专属语义才允许加入 `StarRailExtendedColors`。
6. 业务 Composable 中不得出现 `Color(0x...)`。
7. 不以绝对坐标复刻设计图，必须使用 Compose 布局约束。
8. 不将整张设计图作为背景图片冒充 UI。
9. 所有文字进入资源文件，不直接散落在 Composable 中。
10. 新组件及新页面必须提供浅色和深色 Preview，且统一尺寸规范为 `@Preview(widthDp = 360, heightDp = 800)`（所有后续编写的界面都应加上该规格的预览）。
11. 新页面必须验证 Compact、Medium、Expanded 三种宽度。
12. 新交互必须包含 enabled、disabled、pressed、focused 状态。
13. 修改完成后至少构建 Android 或 Desktop，并尽可能验证 Web。
14. 新增或修改业务弹窗必须复用 `StarRailDialog`；不得直接使用默认 `AlertDialog`
    或自行复制弹窗容器和按钮布局。

推荐 Preview：

```kotlin
@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenLightPreview() {
    StarRailTheme(darkThemeOverride = false) {
        ChatSessionScreen(state = previewState, onAction = {}, onMainAction = {})
    }
}

@Preview(widthDp = 360, heightDp = 800)
@Composable
private fun ChatSessionScreenDarkPreview() {
    StarRailTheme(darkThemeOverride = true) {
        ChatSessionScreen(state = previewState, onAction = {}, onMainAction = {})
    }
}
```

---

## 16. 验收清单

提交 UI 变更前确认：

- [ ] 浅色和深色均可正常显示。
- [ ] 没有业务组件硬编码颜色。
- [ ] 所有前景色使用对应的 `on*` 语义色。
- [ ] 页面标题完整显示为“崩铁 Chat Box”。
- [ ] 系统安全区未遮挡内容。
- [ ] 文字缩放后无裁切、重叠或固定高度问题。
- [ ] 触控目标不小于 48dp。
- [ ] 键盘可操作并有明确焦点样式。
- [ ] 装饰元素不参与无障碍朗读。
- [ ] 消息列表、输入区和导航在窄屏与宽屏均合理。
- [ ] 深色模式主要依靠语义表面分层，而非纯黑背景和过度阴影。
- [ ] 组件状态与 Material Design 3 行为一致。
- [ ] 所有新编写的页面和组件都编写了浅色与深色的 `@Preview` 预览。
- [ ] 所有业务弹窗复用 `StarRailDialog`，系统返回、取消和破坏性操作语义正确。
- [ ] Android/Desktop 至少一个目标构建通过。
