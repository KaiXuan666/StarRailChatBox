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

## 技术栈

新增数据层、网络层、依赖注入、存储和加密功能时，优先使用以下统一技术栈，
不要为相同职责重复引入其他框架：

- 网络请求：Ktor Client。
- JSON：`kotlinx.serialization`，并通过 Ktor `ContentNegotiation` 集成。
- Android HTTP 引擎：Ktor OkHttp Engine，依赖仅放在 Android 源码集。
- iOS HTTP 引擎：Ktor Darwin Engine，依赖仅放在 iOS 源码集。
- Desktop、JavaScript 和 WasmJS HTTP 引擎：选择 Ktor 官方支持对应目标的引擎，
  并保持上层 `HttpClient` 配置和接口共享。
- 接口封装：Ktorfit。接口定义和可共享转换逻辑放在公共源码集。
- 错误封装：项目自定义不可变 `ApiResult`，统一表达成功、业务错误、网络错误和
  未预期错误；不得把 Ktor、OkHttp 或平台异常直接暴露给 UI。
- 日志：Ktor Logging + Napier。Ktor 日志通过自定义 `Logger` 转发给 Napier，
  并脱敏 `Authorization`、Cookie、令牌及其他敏感请求头和请求体。
- 依赖注入：Koin。公共模块声明放在 `commonMain`，需要平台对象的绑定放在对应
  平台源码集，并由各应用入口初始化。
- 权限管理：MOKO Permissions。用于跨平台（Android/iOS）请求麦克风等敏感权限。
- 文件选择与处理：FileKit。提供跨平台的文件/图片选择器，并支持与 Coil 集成。
- 轻量键值存储：DataStore Preferences KMP。
- I/O 操作：Okio。用于跨平台文件流读写和文件系统抽象。
- 加密：`cryptography-kotlin`，优先使用适合目标平台的 provider，不自行实现
  密码算法。
- 图片加载：Coil 3 Compose Multiplatform。公共 UI 使用共享头像/图片组件加载
  `avatarUri`、资源 URI、文件路径或浏览器 `data:` URI，不在业务 Composable 中
  手动解码头像字节。
- 结构化数据库：Room KMP。Room 实体、DAO、数据库和共享实现放在仅供 Android、
  iOS、Desktop/JVM 使用的 `shared/src/roomMain`；数据库路径、Builder、SQLite
  driver、私有文件目录和其他平台配置放在对应平台源码集。

### KMP 支持边界

以上技术选型已按本项目 Android、iOS、Desktop/JVM、JavaScript 和 WasmJS 目标
核对，但“KMP”不代表自动覆盖全部目标。引入具体版本时仍必须重新检查其发布构件
与当前 Kotlin、KSP、Ktor 和 Compose 版本的兼容性。

| 技术 | Android | iOS | Desktop/JVM | JavaScript | WasmJS | 使用约束 |
| --- | --- | --- | --- | --- | --- | --- |
| Ktor Client | 支持 | 支持 | 支持 | 支持 | 支持 | 引擎按平台配置；OkHttp 仅用于 Android/JVM，Darwin 仅用于 Apple Native |
| `kotlinx.serialization` JSON | 支持 | 支持 | 支持 | 支持 | 支持 | JSON 模型与配置可放入 `commonMain` |
| Ktorfit | 支持 | 支持 | 支持 | 支持 | 支持 | 使用前核对 KSP、Kotlin 与 Ktorfit 版本矩阵 |
| 自定义 `ApiResult` | 支持 | 支持 | 支持 | 支持 | 支持 | 仅使用公共 Kotlin 类型，不包含平台异常类型 |
| Ktor Logging | 支持 | 支持 | 支持 | 支持 | 支持 | 生产环境限制级别并强制脱敏 |
| Napier | 支持 | 支持 | 支持 | 支持 | 未明确支持 | 不得直接加入会破坏 WasmJS 编译的公共源码集；WasmJS 使用公共日志抽象和兼容实现 |
| Koin | 支持 | 支持 | 支持 | 支持 | 支持 | 平台对象通过平台模块绑定 |
| MOKO Permissions | 支持 | 支持 | 不支持 | 不支持 | 不支持 | 仅用于 Android 和 iOS；其他平台通过平台能力探测跳过权限请求 |
| FileKit | 支持 | 支持 | 支持 | 支持 | 支持 | 优先使用 `FileKit.pickFile` 或 `pickImage` 进行多媒体输入 |
| DataStore Preferences KMP | 支持 | 支持 | 支持 | 不支持 | 不支持 | 仅用于 Android、iOS 和 Desktop；Web 通过公共存储接口接入浏览器实现 |
| Okio | 支持 | 支持 | 支持 | 支持 | 支持 | 用于文件系统操作和测试 mock |
| `cryptography-kotlin` | 支持 | 支持 | 支持 | 支持 | 支持 | provider 和算法支持可能因平台不同，使用前逐项确认 |
| Coil 3 Compose | 支持 | 支持 | 支持 | 支持 | 支持 | 公共 UI 使用 `avatarUri` 等可加载模型；引入或升级时需检查 Compose/Skiko 兼容警告 |
| Room KMP | 支持 | 支持 | 支持 | 不支持 | 不支持 | 仅用于 Android、iOS 和 Desktop；Web 通过公共仓库接口使用独立持久化实现 |

不得为了接入 DataStore、Room 或 Napier 而删除或禁用 JavaScript/WasmJS 目标，也不得
把不支持 Web 的构件加入 Web 可见的源码集。公共业务层应依赖项目自有接口，由
Android、iOS、Desktop 和 Web 分别提供实现；平台差异只停留在数据源创建、引擎、
driver、provider 和日志出口等边界。

### 跨平台代码编写规范
- 编写跨平台代码前，先检测已经依赖的第三方库是否支持所需功能，如果支持，优先使用第三方库，不要直接编写跨平台代码。
- 编写跨平台代码前，如果有比较成熟的第三方库，优先使用第三方库，不要自行实现功能。

### 文件处理规范
- 用户选择头像或文件时，优先使用 FileKit，文件读写采用Okio 封装类 `KmpFileManager`，对于文件操作，不要自己写各平台实现。
- 对于 Android、iOS 和 Desktop，将返回的 `PlatformFile` 通过 `KmpFileManager` 处理并复制到应用私有目录。
- 对于 JS/WasmJS，直接使用 `PlatformFile` 提供的 Data URL (Base64) 或内存 URI，避免复杂的平台持久化。
- 注意FileKit选择出来的文件，可能路径会很奇怪，比如“content://media/picker/0/com.android.providers.media.photopicker/media/1000125767”
  所以拿到FileKit返回的path后，除了path，我们还需要传递image.name 和 image.extension给调用方，避免调用方拿不到扩展名，导致存储下来无扩展名的文件。
- 修改头像或删除角色时，应清理由应用私有目录下的旧头像文件。
- 头像文件名必须由角色 ID 经过跨平台安全编码生成，不直接使用角色名、中文、
  冒号或路径分隔符作为文件名。
- 内置角色资源来自 `shared/src/commonMain/composeResources/files/characters`。
  `DefaultCharacterRepository` 首次加载时将缺失角色幂等导入 `agent_role`；已存在
  或已软删除的相同 ID 不得被重新导入或覆盖头像。
- JS/WasmJS 不使用 Room，当前角色使用浏览器存储并以可加载的 `data:` URI 表示头像，
  模型配置使用内存实现；公共 UI 和业务代码不得直接依赖 Room 实体、DAO 或平台文件路径。

#### 两阶段落盘法 (Cache ➔ Files)
当出现用户选择文件 → 后续可以保存入库(uri入库)的情况时，（比如 `AgentRoleEntity` 中的 `avatarUri`和 `voiceSampleUri`，`MessageAttachmentEntity`里面的uri，还有 `AttachmentPanel`，用户选择添加附件）
应采用“临时缓存 -> 确认入库”的策略：
1. **阶段一（选择即缓存）：** 当用户选完文件后，立即将文件拷贝到 App 沙盒的【临时缓存目录 (Cache Dir)】中，并生成一个临时路径。
2. **阶段二（确认才入库）：** 只有当用户在 UI 上真正点击了“保存/提交”按钮时，将文件从【临时缓存目录】移动到【正式私有目录 (Files Dir)】，并完成数据库入库。


### Room 数据库
- 数据库统一使用 `StarRailDatabase`。Android、iOS 和 Desktop 启动时只创建一个
  Room 实例，并从该实例提供模型配置、角色、会话和消息相关 DAO 或 Repository；
  不要为不同 Repository 重复打开同一路径的数据库。
- 当前数据库表为 `agent_role`、`chat_session`、`chat_message`、`chat_summary`
  和 `model_config`。实体和 DAO 位于 `shared/src/roomMain`，不得移动到
  JS/WasmJS 可见的源码集。
- `chat_summary` 保存滚动摘要及其覆盖的消息序号范围；原始消息继续保留。默认在
  未压缩有效消息达到 30 条时后台总结，并保留最近 10 条原文。
- `agent_role` 的头像内容不保存为数据库 BLOB。Android、iOS 和 Desktop 应先将
  头像复制到应用私有目录 `character_avatars`，数据库仅在 `avatar_uri` 中保存
  对应路径。领域模型只暴露 `avatarUri`，公共 UI 使用 Coil 加载该 URI；不得在
  领域模型或 UI 状态中长期持有头像 `ByteArray`。
- 角色 Tab 和聊天角色选择器必须使用轻量 `CharacterSummary`，当前只包含角色 ID、
  名称、头像 URI 和最近会话时间。不得把 prompt、开场白、模型参数、语音样本或编辑
  草稿加入摘要模型。
- Room 的角色摘要必须由 DAO 投影直接查询所需列，禁止先全量读取 `AgentRoleEntity`
  或完整 `Character` 再映射。`RoomCharacterStorage.loadCharacters()` 会加载所有角色
  的完整 prompt，已属于废弃兼容 API；列表和选择器必须使用
  `loadCharacterSummaries()`，进入聊天、编辑或导出时再按 ID 调用 `getCharacter(id)`。

### AI 上下文编排规范

任何涉及以下内容的任务，开始分析或修改前必须完整阅读：

- [`AI Context Orchestration Specification.md`](AI%20Context%20Orchestration%20Specification.md)

触发场景包括但不限于：

- 角色会话的创建、恢复、切换、欢迎消息和消息持久化。
- system prompt、角色 prompt、会话 prompt 或请求消息角色/顺序的调整。
- 历史消息筛选、上下文条数或 token 裁剪、消息排除、摘要和压缩。
- `ChatContextBuilder`、`ChatViewModel`、`ChatSessionRepository`、聊天 DAO/实体、
  `AiRepository`、Provider、工具注册或 CHAT 请求体的修改。
- CHAT API 的发送、响应保存、流式输出、取消、失败、重试或重新生成。
- 附件、图片、OCR、工具调用、工具结果、消息分支或父消息链进入上下文。
- Android、iOS、Desktop、JavaScript 或 WasmJS 的聊天实现、Provider、工具执行和
  持久化差异。

实现不得破坏该规范定义的 prompt 优先级、消息顺序、当前输入保留、失败消息排除、
原子持久化和跨平台一致性。若需求需要改变这些规则，应同步更新规范、实现和回归测试；
不得只修改某个平台或 UI 层绕过公共编排流程。

### AI Provider、工具系统与本地测试配置

- UI 和 ViewModel 只依赖 `AiRepository`。`DefaultAiRepository` 通过
  `AiProviderRegistry` 按 `ModelConfig.provider` 选择 Provider，并通过
  `ToolCallCoordinator` 处理工具调用；不得恢复对具体 Provider 或协议 DTO 的直接依赖。
- 对于纯文本生成任务（如提示词自动生成 `createPromptCompletion`、会话总结 `createConversationSummary` 与会话命名 `createSessionTitle` 等），`DefaultAiRepository` 必须绕过 `ToolCallCoordinator`，将 `toolChoice` 设为 `ToolChoice.None` 并直接调用 `provider.complete`，以从根本上防止辅助任务误触发设备或本地平台工具执行。
- 当前 `OpenAiCompatibleProvider` 使用 OpenAI 兼容 API：模型列表调用
  `GET /models`，聊天调用 `POST /chat/completions`。接口通过 Ktorfit 声明，Provider
  负责领域模型与 wire DTO 的转换，ViewModel、Composable 和工具不得直接拼装请求。
- 新增 Provider 时实现 `AiProvider` 并注册到 Koin；Provider 只负责鉴权、协议映射、
  网络调用和能力探测，不得包含具体工具业务。
- 新增工具时实现 `AiTool` 并注册到 Koin。工具 schema 使用 `JsonObject` 表达；
  参数校验、fallback prompt 和结果解析归工具所有，不得写回 `ChatContextBuilder`
  或 Provider。当前包含 `QuickRepliesTool` 和 `VoiceSynthesisTool`。
- `ToolExecutionType.TerminalOutput` 直接形成最终助手输出；
  `ToolExecutionType.Executable` 生成 tool result 并继续请求模型。协调器按模型返回
  顺序执行，最多 4 轮，并拒绝重复调用签名。
- 设备状态、敏感读取和外部写入工具必须经过 `ToolApprovalGateway`；平台 API 只能
  通过各源码集提供的 `PlatformToolExecutor` 接入，公共工具代码不得引用平台类型。
- API Host、API Key 和所选模型通过 `ModelConfigRepository` 读取和保存。Android、
  iOS 与 Desktop 使用 Room 的 `model_config` 表；JS/WasmJS 当前使用内存实现。
- 当前设置页维护五组独立的模型配置记录：固定 ID 为 `default` 的默认普通模型、固定
  ID 为 `multimodal` 的多模态模型（`supportVision = true`）、固定 ID 为 `voice`
  的语音合成模型、固定 ID 为 `voice_clone` 的音色克隆模型，以及图片生成模型。
  设置概览只保留默认、多模态、语音（合并普通语音与音色克隆）和图片生成四类
  “是否已配置”状态。后续扩展多模型配置时应继续通过 `ModelConfigRepository`。
- API Key 持久化必须使用 `cryptography-kotlin` 的 AES-GCM 加密，`model_config`
  中只保存带版本标识的密文；DataStore 仅保存加密密钥，不得增加明文兼容字段或
  明文回退存储。
- 本地开发可在被 Git 忽略的根目录 `local.properties` 中配置：

  ```properties
  OPENAI_API_HOST=https://api.openai.com/v1
  OPENAI_API_KEY=
  ```

- 构建脚本会把上述值生成到 `shared/build/generated/localApiSettings`。已保存的
  非空配置优先；持久化配置缺失或对应字段为空时才使用本地值。
- `local.properties`、生成的配置源码和包含真实密钥的构建产物不得提交。新增测试
  必须显式注入空值或测试值，不能依赖开发者机器上的 `local.properties`。

### 多模态输入与跨平台附件读取

- **多模态模型选择**：当发送的消息中包含图片附件，或者在不启用文本拼接的情况下包含任意文件附件时，系统必须选用多模态模型配置（`id = "multimodal"`，其 `supportVision` 为 `true`）发起请求；否则默认使用普通模型配置。
- **跨平台附件读取使用`KmpFileManager`，不得自行编写各平台实现。
- **常规文件传输与文本拼接可选方案**：常规文件（包括文本文件，如 `.txt`, `.kt`, `.pdf` 等）优先使用常规多模态文件传输方式，即通过 `KmpFileManager` 读取字节并转换为 Base64 编码的 Data URL（带有相应的 MIME 类型），封装为多模态内容（`AiContentPart.FileUrl`）进行传输。之前的文本拼接方案保留作为可选方案（由 `enableFileAppend` 开关控制，默认不启用），该方案将非图片文本文件以特殊格式拼接到用户 Prompt 尾部并持久化至数据库。
- **Base64 编码**：多模态图片的 Base64 转换统一使用 Kotlin 1.9.0+ 标准库提供的 `kotlin.io.encoding.Base64`。

### 网络日志

- Ktor Client 统一安装 Logging，并通过 Napier 输出；Android 创建 HTTP Client 前
  必须注册 `DebugAntilog`，Logcat 使用 `OpenAiHttp` 标签。
- 开发构建可使用 `LogLevel.BODY` 查看请求和响应；`Authorization`、Cookie、API Key
  以及其他令牌必须始终脱敏，日志和异常信息不得输出密钥明文。
- 发布构建不得保留包含请求体或响应体的详细网络日志。调整构建类型后，应将生产
  日志降低到安全级别或关闭，并保留敏感头脱敏。

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
- 针对包含多角色的对话主界面等场景，`UiState` 必须支持多角色独立状态管理（如通过 `characterStates: Map<String, CharacterChatState>` 隔离各个角色的消息列表、输入草稿和发送状态），避免状态互锁，支持在等待 AI 回复的过程中立刻切换角色。对外暴露的当前角色属性可通过 Getter 代理动态路由至当前选中的角色状态，以保持对外签名的向下兼容性。


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
- 在 `HorizontalPager` 中加载包含重度 `LazyColumn` 和复杂图文排版的子页面时，**禁止**开启预载参数（如 `beyondViewportPageCount` 大于 0），必须保持默认的按需懒加载。这可规避因从其他 Bottom Tab 路由切回该界面时一帧内同步渲染组合多个重度页面导致的主线程卡顿（Jank）。

### 模块状态解耦与状态拆分规范

- **状态细粒度拆分**：为防止单个 `UiState` 或 `Action` 过于臃肿导致状态互锁或无效重组，应按照业务模块进行细粒度拆分。例如将角色管理与聊天会话完全解耦。
- **解耦结构**：
  - **角色包 (`ui/character`)**：`CharactersViewModel` 只管理
    `CharactersUiState`（`List<CharacterSummary>`、选择 ID 和加载状态）；
    `CharacterEditViewModel` 独立管理完整角色、编辑草稿、导入导出、文件缓存和提示词
    生成状态。
  - **聊天包 (`ui/chat`)**：`ChatUiState` 按
    `characterStates: Map<String, CharacterChatState>` 隔离各角色消息、输入草稿和
    发送状态；角色选择列表使用独立的 `ChatCharactersUiState`，只保存
    `CharacterSummary`。
- **聊天动作边界**：`ChatViewModel` 可分别公开聊天状态与轻量角色选择状态，并提供
  `onAction(ChatAction)`、`onCharacterAction(CharacterAction)`；角色编辑、删除、
  导入导出等管理逻辑不得重新放回 `ChatViewModel`。AI 调用通过
  `ChatMessageSender` 委托给 `AiRepository`。
- **主路由与分发**：外层根 Route 为角色、对话、设置三个 Tab。
  `MainNavigationContainer` 负责绑定状态和分发事件；个人资料、角色编辑、API 配置等
  都是二级 Route，不是常驻根绑定节点。

### Navigation 3 与页面状态生命周期

- 主导航使用 Navigation 3。`Route` 必须实现可序列化 `NavKey`，导航栈由
  `rememberNavBackStack` 管理，不得重新放入 `MainViewModel`。
- 必须配置 `rememberSaveableStateHolderNavEntryDecorator` 和
  `rememberViewModelStoreNavEntryDecorator`。拥有独立 ViewModel 的二级页面必须在
  对应 entry 中创建并收集状态；entry 出栈后，其 ViewModel、协程和临时 `UiState`
  应随之释放。
- `ChatViewModel` 在根 `ViewModelStore` 中常驻，以保留多角色聊天草稿、消息状态和
  后台 AI 请求。`CharactersViewModel` 与 `SettingsOverviewViewModel` 首次访问
  对应 Tab 时按需创建，创建后允许常驻。
- 点击外层 Tab 时必须将 back stack 重置为目标根 Route。已被移除的角色编辑、API
  配置、个人资料等二级页面不得继续在根部持有或收集状态。
- `MainViewModel` 只持有主题、版本检查和应用级弹窗，不持有导航栈或页面输入状态。
- Navigation 3 页面切换当前不使用淡入淡出动画。`ChatSessionBottomBar` 必须与
  `ChatSessionScreen` 位于同一个聊天 entry 中同步进入和退出，不得在
  `NavDisplay` 外根据已更新的 back stack 提前隐藏。


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
- 将头像、文件等尽量不要使用 `ByteArray`、`byte` 或 Base64 存储在变量/内存中，尽量使用`KmpFileManager`操作文件， `okio.Path`传递路径。
- 修改保持聚焦，不顺带格式化或重构无关文件。

## 测试要求

- 公共业务逻辑测试优先放在 `shared/src/commonTest`，使用 `kotlin.test`。
- Room DAO、数据库映射、加密持久化和私有文件读写集成测试放在
  `shared/src/jvmTest`，使用临时数据库和临时目录，不依赖开发者本机数据。
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
