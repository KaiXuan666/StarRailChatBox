# StarRailChatBox Agent Guide

## 适用范围与指令优先级

- 本文件适用于整个仓库。
- 若子目录中存在更具体的 `AGENTS.md`，则以距离目标文件最近的规则为准。
- 用户当前任务中的明确要求优先于本文件；发生冲突时应指出冲突并遵循更高优先级要求。
- 开始修改前先检查相关源码、构建脚本、测试和 `git status`，不要覆盖或回退用户已有的未提交改动。
- 读取已知具体文本文件时，使用：

  ```powershell
  Get-Content -LiteralPath "<path>" -Encoding UTF8
  ```

## 项目概览

StarRailChatBox 是使用 Kotlin Multiplatform 和 Compose Multiplatform 构建的多端项目。

主要模块：

- `shared/`：共享业务逻辑、状态管理、Compose UI 和多平台资源。
- `androidApp/`：Android 应用入口与 Android 专属配置。
- `desktopApp/`：Desktop/JVM 应用入口与桌面打包配置。
- `webApp/`：JS 与 WasmJS Web 应用入口。
- `iosApp/`：iOS/Xcode 应用入口，加载 `Shared` framework。

`shared` 当前目标平台：

- Android
- iOS Arm64
- iOS Simulator Arm64
- Desktop/JVM
- JavaScript
- WasmJS

除非平台能力确实不同，功能、业务逻辑和 UI 应优先实现在
`shared/src/commonMain`。

## 工程边界

- 应用入口模块只负责平台启动、窗口或 Activity 配置，以及接入共享 `App()`。
- 不要在 `androidApp`、`desktopApp`、`webApp` 或 SwiftUI 中重复实现共享业务页面。
- 平台 API 应放在对应源码集，并通过小型接口或 `expect`/`actual` 暴露给公共代码。
- `expect`/`actual` 只用于真实平台差异；不要为普通依赖注入或可由接口解决的问题滥用它。
- 新增源文件时沿用包名 `com.kaixuan.starrailchatbox`，并按功能分包。
- UI、领域逻辑、数据访问和平台适配应保持边界清晰，避免 Composable 直接访问平台 API 或数据源。

推荐的功能目录结构：

```text
shared/src/commonMain/kotlin/com/kaixuan/starrailchatbox/
├── design/
├── model/
├── data/
├── domain/
├── platform/
└── ui/
    └── <feature>/
        ├── <Feature>Screen.kt
        ├── <Feature>ViewModel.kt
        ├── <Feature>UiState.kt
        ├── <Feature>Action.kt
        ├── <Feature>Effect.kt
        └── components/
```

目录可根据功能规模合并，但架构职责不能混合。

## 架构：MVVM + UDF

所有新增页面和有交互的功能采用 MVVM + 单向数据流：

```text
UI --Action--> ViewModel --StateFlow<UiState>--> UI
                         \--Effect-----------> UI
```

### UiState

- 使用不可变 `data class` 或 `sealed interface` 表达可渲染的完整页面状态。
- 持久展示状态、输入值、加载状态、选择状态和可恢复错误属于 `UiState`。
- 集合优先暴露只读类型；不要把可变集合或 `MutableStateFlow` 暴露给 UI。
- 状态默认值应使初始渲染有效、可预测。

### Action

- 使用 `sealed interface <Feature>Action` 描述所有用户意图和需要交给 ViewModel 的 UI 事件。
- UI 仅通过 `onAction(action)` 向上发送事件，不直接修改 ViewModel 状态。
- Action 使用业务意图命名，例如 `MessageChanged`、`SendClicked`，不要使用模糊名称。

### ViewModel

- ViewModel 是状态所有者，公开 `StateFlow<UiState>`，内部持有私有
  `MutableStateFlow`。
- 公开状态使用 `asStateFlow()`；状态更新应集中、原子化并使用不可变 `copy`。
- 提供统一的 `onAction(action)` 入口，并保证每种 Action 都有明确处理路径。
- ViewModel 不依赖 Composable、Activity、UIViewController 或浏览器 DOM。
- 异步任务在 ViewModel 生命周期作用域中启动；取消和异常必须有明确处理。
- 不把导航、Snackbar、打开外部页面等一次性事件永久保存在 `UiState` 中。

### Effect

- 使用 `sealed interface <Feature>Effect` 表达仅消费一次的副作用。
- Effect 可通过 `Channel` + `receiveAsFlow()` 或语义等价的只读流公开。
- UI 在受生命周期管理的协程中收集 Effect。
- 状态不得通过 Effect 传递；旋转或重组后仍应存在的信息必须进入 `UiState`。

### Compose 层

- Route/Page 级 Composable 负责收集状态、收集 Effect 和转发 Action。
- Screen 与小型组件尽量保持无状态，接收 `state`、显示参数和事件回调。
- 使用 `collectAsStateWithLifecycle()` 或项目统一的多平台生命周期方案收集状态。
- 不在 Composable 中执行业务请求、持久化或不可重复的副作用；使用
  `LaunchedEffect` 仅处理与 Compose 生命周期相关的 Effect 收集或 UI 行为。
- `remember` 只保存纯 UI 的短暂状态；业务状态必须提升到 `UiState`。

## Kotlin Multiplatform 依赖规则

- 向 `commonMain` 添加依赖前，必须确认其支持本项目需要的 KMP 目标，尤其是
  Android、iOS、JVM、JS 和 WasmJS。
- 可在 [Klibs.io](https://klibs.io/) 查询 KMP 库和目标支持情况，同时核对库的官方文档、发布版本、许可证和维护状态。
- 不要仅因库标注 “Kotlin” 或支持 Android 就认为它支持 Kotlin Multiplatform。
- 若依赖不支持全部公共目标，不得直接加入 `commonMain`。应选择兼容库，或将其限制在对应平台源码集并提供公共抽象。
- 优先使用 Kotlin/JetBrains 官方多平台库和项目已有依赖，避免为简单功能引入大型依赖。
- AndroidX 依赖放入公共代码前，必须确认使用的是支持 KMP 的构件；注意 JetBrains
  多平台构件可能使用 `org.jetbrains.androidx` 坐标。
- 所有版本和库别名统一维护在 `gradle/libs.versions.toml`，模块构建脚本使用
  `libs.*`，不要在多个 `build.gradle.kts` 中散落版本号。
- 禁止动态版本，如 `+`、`latest.release` 或未锁定的快照版本。
- 新依赖加入后至少编译它影响的所有宿主机可用目标；不能执行的目标要在结果中明确说明。

## UI 变更规则

任何涉及 UI、主题、布局、资源、文字、动效、响应式行为或无障碍的改动，开始前必须完整阅读：

- [`UI-Design.md`](UI-Design.md)

`UI-Design.md` 是本项目 UI 实现和验收的权威规范。尤其要遵守：

- 优先使用 Material 3 与项目主题令牌。
- 业务 Composable 不硬编码颜色、排版、形状或重复设计令牌。
- 可见文本使用 Compose Multiplatform Resources，不直接写死在 Composable 中。
- 公共 UI 优先放入 `shared/src/commonMain`。
- 组件保持状态提升，并提供必要的浅色、深色与不同宽度 Preview。
- 同时考虑 Compact、Medium、Expanded 布局，以及 Android/iOS 安全区和
  Desktop/Web 的鼠标、键盘与窗口缩放。
- 满足触控尺寸、对比度、焦点顺序和语义描述等无障碍要求。
- UI 变更完成后按 `UI-Design.md` 的验收清单逐项检查。

若实现与 `UI-Design.md` 冲突，应先修改方案；除非用户明确要求更新设计规范本身。

## 资源与文案

- 共享图片、图标和字符串放在 `shared/src/commonMain/composeResources`。
- 用户可见文案必须资源化，并考虑本地化；不要用代码字符串拼接构造可翻译句子。
- 纯装饰图片使用 `contentDescription = null`；承载信息或操作的图标必须提供语义。
- 不提交构建产物、IDE 配置、机器本地配置、密钥或 `local.properties`。

## Kotlin 代码规范

- 遵循 Kotlin 官方代码风格，保持现有 4 空格缩进和尾随逗号习惯。
- 优先使用不可变值、不可变模型、表达式和小型纯函数。
- 公共 API 和架构类型使用明确名称；仅在逻辑不直观时添加简短注释。
- 避免通配符导入、无意义包装层、全局可变状态和不必要的单例。
- 协程不得使用 `GlobalScope`，也不要吞掉 `CancellationException`。
- 不在公共代码中使用 JVM、Android 或 Java 专属类型。
- 修改保持聚焦，不顺带格式化或重构无关文件。

## 测试要求

- 公共业务逻辑测试优先放在 `shared/src/commonTest`，使用 `kotlin.test`。
- Reducer、ViewModel 或状态转换至少覆盖：初始状态、主要 Action、成功、失败、重试和 Effect。
- 测试应断言可观察行为，不依赖延时、执行顺序偶然性或私有实现细节。
- 平台专属逻辑放在相应平台测试源码集。
- 修复缺陷时应添加能在修复前失败、修复后通过的回归测试。

Windows/PowerShell 常用命令：

```powershell
# 查看任务
.\gradlew.bat tasks

# 公共代码 JVM 与 Android Host 测试
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest

# Android 构建
.\gradlew.bat :androidApp:assembleDebug

# Desktop 构建
.\gradlew.bat :desktopApp:build

# Web 两个目标编译
.\gradlew.bat :webApp:compileKotlinJs :webApp:compileKotlinWasmJs

# 完整构建与测试（可能需要本机浏览器或平台工具）
.\gradlew.bat build
```

运行应用：

```powershell
.\gradlew.bat :desktopApp:run
.\gradlew.bat :desktopApp:hotRun --auto
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
.\gradlew.bat :webApp:jsBrowserDevelopmentRun
```

iOS 构建和模拟器测试需要 macOS/Xcode。在 Windows 上出现 iOS 目标被禁用的提示是宿主平台限制，不要通过删除 iOS 目标来规避。

验证范围应与变更风险匹配：

- 文档改动：检查链接、路径、命令和 Markdown。
- 公共业务逻辑：运行相关公共测试，并编译至少一个消费模块。
- 公共 UI：至少构建 Android 或 Desktop，并尽可能编译 JS 与 WasmJS。
- 平台代码：构建对应平台；无法在当前宿主机执行时明确记录未验证项。
- 构建配置或依赖：编译所有当前宿主机可用且受影响的目标。

## 完成标准

提交结果前确认：

- 实现符合 MVVM + UDF，数据流保持单向。
- 公共代码没有泄漏平台专属 API。
- 新依赖已验证 KMP 目标支持并通过 Version Catalog 管理。
- UI 变更已阅读并符合 `UI-Design.md`。
- 已运行与改动匹配的测试或构建；失败和未运行项被如实说明。
- 没有覆盖用户改动，没有修改无关文件，也没有加入生成物或敏感信息。
- 最终说明简要列出修改内容、验证命令及任何剩余限制。
