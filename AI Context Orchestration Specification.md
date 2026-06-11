# AI Context Orchestration Specification

## 1. 文档目的

本文档定义 StarRailChatBox 从用户发送消息到 AI 响应持久化的上下文编排规范。
后续修改角色 prompt、历史裁剪、请求参数、消息状态、摘要或工具调用流程时，必须先
核对本规范，并同步更新实现、测试和本文档。

规范关键词：

- **MUST**：必须满足，不得由平台实现自行改变。
- **SHOULD**：默认应满足；偏离时需要说明理由并补充测试。
- **MAY**：可选扩展，不属于当前基础流程。

当前主要实现位置：

- `shared/src/commonMain/.../data/chat/ChatContextBuilder.kt`
- `shared/src/commonMain/.../ui/chat/ChatViewModel.kt`
- `shared/src/commonMain/.../ui/chat/ChatMessageSender.kt`
- `shared/src/commonMain/.../data/chat/ChatSessionRepository.kt`
- `shared/src/roomMain/.../data/chat/RoomChatSessionRepository.kt`
- `shared/src/commonMain/.../data/ai/AiRepository.kt`
- `shared/src/commonMain/.../data/ai/AiProvider.kt`
- `shared/src/commonMain/.../data/ai/tool/ToolCallCoordinator.kt`

## 2. 设计目标

上下文编排必须保证：

1. 同一会话内的角色身份和 system prompt 稳定。
2. 用户当前输入不会因历史裁剪而丢失。
3. 失败、软删除或显式排除的消息不会污染后续上下文。
4. 请求发送前，用户输入已经可靠持久化。
5. API 失败不会丢失用户输入，并留下可诊断的失败记录。
6. 公共业务层不依赖 Room、文件路径或平台异常类型。
7. Android、iOS、Desktop、JavaScript 和 WasmJS 使用一致的编排语义。

## 3. 术语与数据来源

### 3.1 Session

`ChatSession` 表示某个角色的一段连续对话，编排使用以下字段：

- `agentId`：会话所属角色。
- `systemPromptSnapshot`：创建会话时复制的角色 prompt。
- `customSystemPrompt`：会话级自定义 prompt；非空时优先。
- `maxContextMessageCount`：允许进入请求的历史消息数量；`null` 表示不限制。
- `modelConfigId`：创建会话时关联的模型配置。
- `enableSummary`：是否允许后台自动压缩。
- `summaryThresholdMessageCount`：未压缩有效消息达到该数量时触发总结。
- `summaryRetainedMessageCount`：每次压缩后仍保留的最近原始消息数量。

角色 prompt 更新后，已有会话 MUST 继续使用会话快照，不得被角色新 prompt
静默覆盖。新会话使用创建时的最新角色 prompt。

### 3.2 Stored Message

持久化消息至少包含：

- 单调递增的会话内 `seq`。
- `user` 或 `assistant` 角色。
- 文本内容。
- `completed` 或 `failed` 状态。
- `isContextExcluded` 上下文排除标记。
- 模型快照、token 使用量以及可选错误信息。
- 该消息对应的快捷回复建议列表（`suggestions: List<String>`，对应数据库的 `suggestions_json` 字段）。

消息展示列表与 AI 上下文是两个不同视图。某条消息可以保留在数据库中用于审计，
但因失败或排除状态而不进入 AI 请求。

### 3.3 Summary

`ChatSummary` 是独立持久化的滚动摘要，不属于聊天消息展示列表。摘要记录至少包含：

- 会话 ID。
- 被摘要覆盖的起止消息序号 `fromSeq`、`toSeq`。
- 摘要正文和累计覆盖消息数量。
- 模型快照、token 使用量和创建时间。

原始消息 MUST 保留，不得因摘要生成而删除或改写。会话只激活覆盖范围最大的最新摘要。

### 3.4 Current Input

当前输入是本次发送操作产生的用户消息。它在构造上下文前 MUST 已写入数据库，
但编排器通过独立参数追加它，避免仓库读取时序导致重复或遗漏。

### 3.5 Character Summary 与完整角色

角色列表和聊天角色选择器 MUST 只加载 `CharacterSummary`。当前摘要字段为角色 ID、
名称、头像 URI 和最近会话时间，不包含 prompt、开场白、模型参数、语音样本或编辑草稿。

Room 实现 MUST 使用 DAO 投影直接返回摘要字段，禁止先查询完整 `agent_role` 再映射。
只有选中角色并进入聊天、角色编辑或导出流程时，才允许通过
`CharacterRepository.getCharacter(id)` 加载单个完整角色。

`RoomCharacterStorage.loadCharacters()` 会全量读取所有完整角色，包含可能很长的 prompt，
已属于废弃兼容 API，不得用于聊天主界面、角色 Tab 或角色选择器。

## 4. 会话选择规则

打开或切换角色时：

1. 从摘要列表取得角色 ID，再按 ID 加载该角色的完整数据。
2. 查询该角色最近的未归档、未软删除会话。
3. 最近会话按 `last_message_at DESC` 选择。
4. 找到会话后，按 `seq ASC` 观察并展示消息。
5. 未找到会话时，读取完整角色的 `openingMessage`；非空时只在 UI 显示该欢迎消息。
6. 各角色的消息、草稿、发送和加载状态存放在独立 `CharacterChatState` 中；切换角色
   不得清除其他角色正在进行的后台请求。

对话管理界面允许用户显式选择、创建或删除当前角色的会话：

1. 选择已有会话时，取消当前消息订阅，按会话 ID 加载并观察所选会话。
2. “新建对话”先进入未持久化的空白会话状态；仍遵循首次发送时原子创建会话的规则。
3. 删除会话使用软删除；被删除会话不得继续出现在列表、最近会话查询或上下文中。
4. 删除当前会话后，自动选择该角色剩余的最近会话；没有剩余会话时恢复临时欢迎消息。

角色 `openingMessage` 为空时，初次进入会话界面不显示临时消息。

用户尚未发送消息前，临时欢迎消息 MUST NOT：

- 写入数据库。
- 创建会话。
- 占用消息序号。
- 提前进入 AI 上下文。

发送过程中允许角色切换。发送操作由协程在后台异步绑定具体的会话进行原子提交，切换角色不影响前一角色后台回复的持久化。

## 5. 发送与持久化流程

一次发送操作的标准顺序如下：

```text
校验输入和角色
  -> 读取默认模型配置
  -> 读取已有会话的有效历史
  -> 创建会话（仅首次发送）
  -> 持久化当前用户消息
  -> 校验模型配置
  -> 选择 system prompt
  -> 构造上下文
  -> 选择 Provider 并调用 CHAT API
  -> 编排工具调用、审批、执行与可选后续请求
  -> 持久化成功或失败的助手消息
  -> 成功回复后按阈值在后台尝试生成滚动摘要
  -> 更新 UI 状态和一次性 Effect
```

`ChatViewModel` 负责页面状态、会话选择、上下文准备和持久化协调；
`ChatMessageSender` 只负责把已准备好的模型配置与消息交给 `AiRepository`，不得持有
页面 `UiState`、导航状态或角色列表。

### 5.1 首次发送

没有活动会话时 MUST：

1. 创建 ID 唯一的会话，默认标题使用资源文案“新对话”。
2. 保存当前角色 ID 和角色 prompt 快照。
3. 若角色 `openingMessage` 非空，先写入一条 `assistant` 欢迎消息。
4. 再写入当前用户消息。
5. 将会话、可选欢迎消息和首条用户消息放在同一事务中写入。
6. 写入成功后开始观察该会话消息。

欢迎消息写入后属于正式历史，MUST 进入首次 CHAT 请求，顺序为 system prompt、
assistant 欢迎消息、当前用户输入。`openingMessage` 为空时不得创建空消息。

### 5.2 后续发送

已有活动会话时 MUST：

1. 复用当前会话，不创建新会话。
2. 为用户消息分配下一个 `seq`。
3. 同一事务内写入消息，并更新会话的最后消息 ID、时间和更新时间。

## 6. 上下文构造算法

CHAT 请求的 `messages` MUST 按以下顺序生成：

```text
[可选 system prompt]
+ [可选历史摘要 system 消息]
+ [裁剪后的有效历史消息，保持原始顺序]
+ [当前用户输入]
```

参考算法：

```kotlin
val effectiveHistory = history
    .filter { it.status == COMPLETED && !it.isContextExcluded }
    .takeLastIfLimited(maxContextMessageCount)

return listOfNotNull(nonBlankSystemPrompt) +
    activeSummary +
    effectiveHistory +
    currentUserMessage
```

### 6.1 System Prompt

system prompt 的选择优先级 MUST 为：

1. 非空的 `customSystemPrompt`。
2. `systemPromptSnapshot`。
3. 两者都为空时不发送 system 消息。

system prompt 必须位于请求消息列表首位，且不计入历史消息数量限制。

### 6.2 历史摘要

存在激活摘要时 MUST：

1. 将摘要作为第二条 `system` 消息注入，位于角色 system prompt 之后。
2. 摘要使用明确的 `<chat_history_summary>` 边界。
3. 只读取 `seq > summary.toSeq` 的有效原始消息。
4. 摘要不计入 `maxContextMessageCount`。
5. 当前用户输入仍由独立参数最后追加，不得被摘要覆盖或重复。

自动总结默认在未压缩有效消息达到 30 条时触发，并保留最近 10 条原始消息。总结请求
使用当前会话模型配置，但 MUST 禁用工具调用。新摘要应合并旧摘要与本次新增的较早
消息，形成单个可替代旧摘要的滚动摘要。

总结在助手成功消息持久化后后台执行。总结失败、空响应或取消不得影响已完成的聊天
请求，也不得推进摘要边界；后续发送继续使用旧摘要和原始消息。

#### 消息总结机制
假如：
summaryThresholdMessageCount=10
summaryRetainedMessageCount=5

消息机制如下：
收到消息时，未压缩消息数>summaryThresholdMessageCount 触发总结，
总结消息时，永远保留最近summaryRetainedMessageCount条原始消息不进行总结。

11条消息时会总结前6条
每次发送消息时，查最近一次的总结时间
找查最近一次的总结时间之后的原始消息作为历史消息

第16条时再次触发总结，使用最近一次的总结+原始消息7, 8, 9, 10, 11, 12。

### 6.3 历史过滤

历史消息只有同时满足以下条件时才能进入上下文：

- 状态为 `completed`。
- 未软删除。
- `isContextExcluded == false`。
- 角色可映射为 API 支持的消息角色。

以下消息 MUST 被排除：

- `failed` 助手消息。
- 被用户或压缩流程显式排除的消息。
- 尚未完成的生成中消息。
- 数据不完整或角色无法识别的消息。

### 6.4 历史数量限制

`maxContextMessageCount` 的语义是“最多保留多少条历史消息”：

- `null`：不按条数裁剪。
- `0`：不保留历史，但仍发送 system prompt 和当前输入。
- 正数：保留过滤后最近 N 条历史消息。
- 负数：视为未配置，不执行裁剪；写入层 SHOULD 阻止负数配置。

裁剪 MUST 在过滤之后执行，并使用 `takeLast(N)` 语义。裁剪后的消息仍按 `seq ASC`
发送。当前用户输入不计入 N，并且 MUST 始终保留。

### 6.5 去重要求

仓库读取的 `history` MUST 表示发送本次输入之前的历史。当前用户消息只能由
`currentUserMessage` 参数追加一次。

若未来改为从数据库重新读取包含当前输入的消息，必须同时修改编排接口或增加基于
消息 ID 的去重，禁止依靠文本内容去重。

### 6.6 多模态输入与附件处理

当会话消息中包含附件时，上下文构造与编排 MUST 遵循以下规则：

1. **多模态模型切换**：
   - 若当前待发送的用户消息中包含图片附件（包括相册选择、拍照等），或者在未启用文本文件拼接方案的情况下包含常规文件附件，编排器 MUST 切换使用多模态模型配置（`id = "multimodal"`，其 `supportVision` 为 `true`）；否则默认使用默认普通模型配置（`id = "default"`）。
2. **当前消息结构构造**：
   - 在多模态模式下，当前用户消息在 CHAT API 中映射为多段内容结构（在 OpenAI 协议中表现为 `parts` 数组，即 `List<AiContentPart>`）。
   - 文本部分封装为 `AiContentPart.Text`。
   - 图片部分必须通过跨平台读取服务转换为 Base64 字符串并封装为 `AiContentPart.ImageUrl`（其值为 `data:<mime>;base64,<data>` 的 Data URL 格式）；常规文件部分转为 Base64 并在封装为 `AiContentPart.FileUrl`。
   - Web 端若直接使用 `data:` URI 作为文件标识，底层可跳过字节读取，直接在 ContentPart 中使用。
3. **常规文件传输与文本拼接可选方案**：
   - **常规文件传输方式（默认）**：若 `enableFileAppend` 为 `false`（默认），常规文件（如 `.txt`, `.kt`, `.pdf` 等）会被转为对应的 Base64 Data URL 封装在 `AiContentPart.FileUrl` 中发送给支持多模态的大模型。此时，数据库中仅存储用户本来的消息文字，无需写入拼接正文。
   - **文本文件拼接方案（可选）**：当 `enableFileAppend` 开关为 `true` 时，作为可选的向下兼容和不依赖大模型文件读取能力的备用方案，文本文件被读取为字符串，并在写入数据库和发送前按约定格式拼接到用户提示词尾部：
     ```text
     [文件名.扩展名]
     ---
     <文件文本内容>
     ---
     ```
     拼接后的文本作为 `chat_message` 的正文存入 Room/浏览器数据库，以此确保历史会话上下文加载时不丢失内容。
4. **多模态历史重播**：
   - 编排器从消息附件表重新加载所有历史附件，并通过跨平台读取服务异步读取 URI（或直接使用 Data URL）转换为 Base64。在上下文构造过程中，这些历史附件被重新封装为 `AiContentPart` 并归类到 `historyMessageParts` 中，实现多模态历史信息的完整重播。

## 7. CHAT API 请求

当前使用 OpenAI 兼容接口：

- 方法：`POST /chat/completions`
- 鉴权：`Authorization: Bearer <apiKey>`
- Content-Type：`application/json`
- 角色会话 CHAT 请求 `stream`：`false`
- 角色提示词自动生成 `createPromptCompletion` 使用流式 CHAT 请求，并在仓库层累积
  chunk 形成当前提示词内容；该辅助任务仍不得进入工具调用流程。

请求参数来自当前默认 `ModelConfig`：

- `model`
- `temperature`
- `top_p`
- `max_tokens`
- `messages`

### 7.1 快捷回复（Quick Replies）请求编排

为了向用户提供针对助手回复的快捷回复选项，编排器必须根据模型对工具调用的支持情况进行相应处理：

- **当 `supportToolCall == true` 时（优先采用工具调用）**：
  1. `QuickRepliesTool` 必须通过 `ToolRegistry` 注册，并动态提供
     `respond_with_quick_replies` 定义；参数包含 `ai_response`（字符串，人设扮演正文）
     和 `suggestions`（字符串数组，4 个快捷选项建议）。
  2. 当前请求必须指定 `tool_choice: "required"`，要求模型从已注册且当前平台可用
     的工具中至少选择一个。当前默认只注册 `respond_with_quick_replies`；未来加入
     其他工具后，不得再假设 `required` 一定选择快捷回复工具。
  3. 工具调用能力检测必须验证响应中实际存在目标 `tool_calls`，不得仅凭接口接受
     `tools` 参数或返回 HTTP 200 判定支持。
- **当 `supportToolCall == false` 时（文本注入 fallback 方案）**：
  1. `QuickRepliesTool` 必须通过 `prepareFallbackMessages` 注入明确、带示例和自检项
     的输出契约。首选格式为正文末尾的
     `<quick_replies>{"suggestions":[...]}</quick_replies>` JSON 元数据块。
  2. `parseFallback` 必须兼容首选元数据块、历史 `<suggestions>` 标签和工具参数式纯
     JSON，并容忍标签大小写、Markdown JSON 围栏和常见列表前缀；格式偏差不得污染
     最终助手正文。
  3. `ChatContextBuilder` 不得包含工具专属逻辑。
- **当首次成功响应仍不包含快捷回复时（二次严格兜底）**：
  1. 协调器必须保留首次助手正文，并仅额外请求快捷回复，不得重新生成或覆盖正文。
  2. 二次请求优先使用兼容 OpenAI 与 DeepSeek 的
     `response_format: { "type": "json_object" }`。提示词必须包含 `JSON` 字样和目标
     格式示例；收到结果后在本地校验必须恰好包含 4 个 `suggestions` 字符串。
  3. 上下文只包含角色 system prompt、稳定的快捷回复生成要求，以及包含首次助手
     正文在内的最近 2 轮 user/assistant 对话；不携带摘要、较早历史或工具定义。
  4. 角色 prompt 和固定生成要求必须位于变化对话之前，以提高 Provider prompt cache
     的前缀命中机会；二次请求输出上限应保持较小。
  5. 二次请求成功时只在本地把 suggestions 合并进首次结果，并累计 token usage。
     二次请求失败、JSON 无效或建议为空时，首次成功正文仍按无快捷回复结果返回。

### 7.2 Provider 与工具调用生命周期

- `AiRepository` 是业务层唯一入口，根据 `ModelConfig.provider` 从
  `AiProviderRegistry` 选择 Provider。
- Provider 只负责协议映射、网络调用和能力探测，不得实现具体工具业务。
- 阿里百炼文本生成通过 `AliCompatibleProvider` 委托 OpenAI 兼容协议实现。
- 小米 MiMo 的默认、多模态和语音配置通过 `XiaomiMimoProvider` 委托 OpenAI 兼容
  协议实现。模型发现必须先请求套餐 Host
  `https://token-plan-cn.xiaomimimo.com/v1`，失败或返回空列表时再请求按量 Host
  `https://api.xiaomimimo.com/v1`，并保存实际返回非空模型列表的 Host。
- `ToolCallCoordinator` 按响应顺序执行工具；`TerminalOutput` 直接形成最终助手消息（支持提取 `suggestions`、`voiceAttachmentUri` 和 `voiceDurationMs`），`Executable` 生成 `tool` 消息并继续请求模型。
- 未显式传入工具名称时，协调器使用 `ToolRegistry` 中全部当前可用工具；新增工具
  通过 DI 注册，不得在 `AiRepository` 中增加按工具名分支。
- 同一调用签名重复出现时 MUST 终止；一次发送最多执行 4 轮工具请求。
- 未注册、不可用、参数错误、授权拒绝和执行失败必须转换为不含平台异常的结构化
  tool result。
- `ReadOnly` 工具可直接执行；`DeviceState`、`SensitiveRead` 和 `ExternalWrite`
  必须通过 `ToolApprovalGateway` 获得确认。
- 平台能力只能通过 `PlatformToolExecutor` 接入，并由各平台源码集提供实现。
- 纯文本生成任务（如提示词自动生成 `createPromptCompletion`、会话总结与会话命名）MUST 绕过工具调用协调器 `ToolCallCoordinator`，并设置 `ToolChoice.None`，以防止在辅助任务中误触发任何工具执行。`createPromptCompletion` MUST 通过 Provider 流式接口发起并由仓库层累积文本；会话总结与会话命名当前继续直接调用非流式 `provider.complete`。

### 7.3 Structured Outputs

- Provider 无关请求可通过 `AiResponseFormat` 选择 `JsonSchema` 或 `JsonObject`。
  `JsonSchema` 映射为 `response_format: { type: "json_schema", json_schema: ... }`；
  `JsonObject` 映射为 `response_format: { type: "json_object" }`，不得附带
  `json_schema` 字段。
- 指定 `AiResponseFormat` 后，Provider 必须将助手 `content` 解析为 JSON object 并
  通过 `AiCompletion.structuredOutput` 返回；缺少内容、JSON 无效或根值不是 object
  时作为未预期错误处理。
- 工具定义默认启用 strict 模式。`OpenAiCompatibleProvider` 必须在 function 定义中
  发送 `strict: true`，并在存在 strict 工具时发送 `parallel_tool_calls: false`。
- `QuickRepliesTool` 等工具继续拥有参数 schema 和业务解析逻辑；Provider 只映射
  schema 与解析通用 JSON，不得包含工具名称分支。

### 7.4 多模态、语音与模型配置（Model Configuration Matrix）

系统通过 `ModelConfigRepository` 维护多组独立配置，以适配不同任务：

1. **默认模型 (`id = "default"`)**：用于普通文本对话。
2. **多模态模型 (`id = "multimodal"`)**：当包含图片或文件附件时自动切换，其 `supportVision` 必须为 `true`。
3. **语音合成模型 (`id = "voice"`)**：用于 `VoiceSynthesisTool` 执行普通音色设计合成，Provider 通常为 `xiaomimimo`。
4. **音色克隆模型 (`id = "voice_clone"`)**：当 `ToolContext` 提供 `voiceSampleUri` 时，`VoiceSynthesisTool` 优先使用此配置进行音色克隆合成。
5. **图片生成模型 (`id = "image_generation"`)**：配置通过明确的图片 Provider ID
   选择 OpenAI 兼容协议或阿里百炼协议。`ImageGenerationTool` 只依赖公共图片生成
   Provider 接口，不得拼接服务商 URL、构造协议请求或解析服务商响应。Provider 将
   服务商响应统一转换为远程图片 URL 或 Base64 图片；工具负责将结果保存到应用私有
   目录后再返回消息附件 URI。

模型配置只有同时满足以下条件时才可使用：

- 配置已启用。
- API Host 非空。
- API Key 非空。
- 模型名称非空。

UI、ViewModel 或上下文构造器不得直接拼接 URL 或泄漏 Ktor/平台异常。

## 8. 响应与错误处理

### 8.1 成功响应

成功响应 MUST：

1. **提取消息正文与快捷回复（Suggestions）**：
   - **支持工具调用的模型**：从首选 choice 的 `message.tool_calls` 参数中反序列化 `respond_with_quick_replies` 函数的 arguments。提取其中的 `ai_response` 作为最终消息正文内容，并将 `suggestions` 作为消息对应的快捷回复。
   - **不支持工具调用的模型**：由 `QuickRepliesTool` 读取助手消息文本，通过
     首选 `<quick_replies>` JSON 元数据块提取最多 4 个选项，并兼容历史 XML 与纯
     JSON 输出；解析成功后从正文中移除元数据块。
2. 对内容执行首尾空白清理。
3. 内容非空时写入 `completed` 助手消息，并将提取到的 `suggestions` 持久化到数据库（序列化为 JSON 字符串保存）。若包含 `voiceAttachmentUri` 和 `voiceDurationMs`，也一并保存至对应消息。
4. 保存模型配置 ID、模型名称快照及 token usage。
5. 更新会话最后消息信息。

### 8.2 空响应

没有 choice 或助手内容为空时 MUST 作为失败处理，不得写入空的成功消息。

失败记录：

- `status = failed`
- `error_code = empty_response` 或可诊断的未预期错误码
- `content = ""`
- 保存模型快照（若模型配置可用）

UI 通过一次性 Effect 提示“模型没有返回有效内容”。

### 8.3 配置缺失

默认模型配置不可用时：

1. 会话和用户消息保持已保存状态。
2. 写入 `model_config_required` 助手失败记录。
3. 不调用网络接口。
4. UI 发出模型配置缺失 Effect。

### 8.4 HTTP、网络与未预期错误

错误码映射：

| 错误来源 | 持久化错误码 |
| --- | --- |
| HTTP 响应错误 | `http_<statusCode>` |
| 网络错误 | `network_error` |
| 未预期错误 | `unexpected_error` |
| 模型配置缺失 | `model_config_required` |
| 空响应 | `empty_response` |

所有错误都 MUST 保留用户消息，并追加一条 `failed` 助手记录。原始异常对象不得暴露
给 UI；错误信息不得包含 API Key、Authorization 或其他敏感信息。

协程 `CancellationException` MUST 继续抛出，不得转换为普通失败。

## 9. 并发与一致性

- ViewModel MUST 按角色维护 `isSending`，阻止同一角色并发发送；不同角色的发送状态
  相互隔离，切换角色不得取消前一角色已启动的 AI 请求。
- 会话创建与首条用户消息 MUST 原子提交。
- 追加消息和更新会话最后消息信息 MUST 原子提交。
- 摘要写入和会话激活摘要边界 MUST 原子提交；较旧的后台结果不得覆盖较新的摘要。
- 消息顺序 MUST 由持久化层的 `seq` 决定，不能依赖时间戳排序。
- 切换角色时 MUST 取消旧会话 Flow，旧 Flow 不得覆盖新角色 UI。
- ID MUST 跨平台安全且在本地数据库范围内唯一。

未来若允许并发生成、分支或重试，必须先引入明确的 parent message、请求 ID 或分支
模型，不得继续假设“会话内永远只有一个进行中的请求”。

## 10. 平台边界

聊天 UI 与 ViewModel 的公共编排逻辑位于 `commonMain`，只依赖：

- `ChatSessionRepository`
- `ModelConfigRepository`
- `AiRepository`
- 公共领域模型和 Flow

聊天主界面的角色选择列表依赖 `CharacterRepository.loadCharacterSummaries()`；角色被选中
后再依赖 `CharacterRepository.getCharacter(id)`。UI 状态不得长期持有所有完整角色。

`DefaultAiRepository` 内部再组合 `AiProviderRegistry`、`ToolRegistry` 和
`ToolCallCoordinator`；这些基础设施不得泄漏到 UI 或 ViewModel。

Android、iOS 和 Desktop 使用同一个 `StarRailDatabase` 实例提供 Room 仓库。
JavaScript/WasmJS 不得依赖 Room 实体、DAO 或文件路径，应使用符合相同接口语义的
浏览器持久化或内存实现。

不同平台实现 MAY 改变存储介质，但不得改变会话选择、过滤、裁剪、错误记录和请求
消息顺序。

## 11. 当前未启用能力

以下字段或能力可能已有数据结构预留，但不属于当前基础流程：

- 角色会话流式响应与停止生成。
- token 精确预算触发压缩。
- 具体设备工具实现（手电筒、亮度、振动、位置、日程等）及其权限 UI。
- 工具调用轨迹和审批结果的持久化审计。
- 消息分支、重新生成和父消息链。
- token 精确预算裁剪。

增加这些能力时 SHOULD 保持本规范的核心不变量：system prompt 优先、当前输入保留、
错误消息排除、顺序稳定和失败可追踪。

## 12. 修改流程

修改上下文编排前必须回答：

1. 新数据是否进入 AI 上下文，还是只用于 UI/审计？
2. 它在 system、历史、当前输入中的位置是什么？
3. 它是否参与历史条数或 token 预算？
4. 失败、取消、重试时如何持久化？
5. 是否会导致当前输入重复、遗漏或重新排序？
6. Android/iOS/Desktop 与 Web 是否保持相同语义？
7. 是否可能把密钥、附件隐私或异常详情写入日志？

修改完成后必须同步：

- 公共领域模型和仓库接口。
- Room/Web 实现。
- `ChatContextBuilder`。
- ViewModel 状态与 Effect。
- API 序列化模型。
- Provider、工具注册、审批与平台执行器。
- 本规范及相关测试。

## 13. 测试与验收

最低测试覆盖：

- system prompt 位于首位，空 prompt 不产生 system 消息。
- 失败和排除消息不会进入上下文。
- 历史限制为 `null`、`0`、正数时行为正确。
- 当前用户输入始终位于最后且只出现一次。
- 摘要位于角色 system prompt 后，摘要边界之前的原消息不再进入请求。
- 达到消息阈值后生成滚动摘要并保留最近原文；总结失败不影响聊天。
- 首次发送原子创建会话和用户消息。
- 后续发送复用会话并保持 `seq` 顺序。
- 配置缺失、HTTP、网络、空响应均保留用户消息并写失败记录。
- 成功响应保存助手内容、模型快照和 usage。
- 角色切换不会被旧会话 Flow 覆盖。
- 摘要查询不读取或暴露角色 prompt、开场白、模型参数和语音样本。
- 聊天主界面只保存 `CharacterSummary` 列表，选中后才按 ID 加载完整角色。
- 不同角色的草稿、发送状态和后台请求彼此独立。
- CHAT API 路径、鉴权、Content-Type 和请求参数正确。
- Provider ID 与兼容别名选择正确，未知 Provider 不静默回退。
- 工具定义、fallback、终止型结果、可执行结果、授权拒绝、重复调用和 4 轮上限正确。

建议验证命令：

```powershell
.\gradlew.bat :shared:jvmTest :shared:testAndroidHostTest
.\gradlew.bat :androidApp:assembleDebug
.\gradlew.bat :desktopApp:build
.\gradlew.bat :webApp:compileKotlinJs :webApp:compileKotlinWasmJs
```

若某个平台因宿主环境或既有问题无法验证，最终结果中必须明确记录，不得通过删除
目标或弱化公共接口规避。

## 14. 变更记录

| 版本 | 日期 | 内容 |
| --- | --- | --- |
| 1.0 | 2026-06-07 | 定义非流式角色会话、历史裁剪、CHAT API 和失败持久化基线 |
| 1.1 | 2026-06-07 | 增加快捷回复（Quick Replies）规范，支持 Tool Calling 单次强制调用与文本 XML 正则表达式提取双分支 |
| 1.2 | 2026-06-07 | 工具调用改用单工具 `tool_choice: "required"`，能力检测要求实际返回目标工具调用 |
| 1.3 | 2026-06-07 | 引入 Provider Registry、Tool Registry、工具协调器与风险审批边界，快捷回复迁移为独立终止型工具 |
| 1.4 | 2026-06-07 | 校正规范中的多工具、分层依赖和已启用工具循环描述，并补充设备工具与审计的未启用边界 |
| 1.5 | 2026-06-07 | 增加对话管理中的显式会话选择、新建空白状态和软删除规则 |
| 1.6 | 2026-06-07 | 增加独立摘要表、消息条数阈值后台滚动总结及“摘要 + 最近原文”上下文压缩规则 |
| 1.7 | 2026-06-07 | 增加 `response_format.json_schema` 解析和 strict 工具 Structured Outputs |
| 1.8 | 2026-06-07 | 强化不支持工具调用时的快捷回复输出契约，并增加多格式容错解析 |
| 1.9 | 2026-06-07 | 首次响应缺少快捷回复时，增加最近两轮上下文的 strict JSON 二次请求兜底 |
| 1.10 | 2026-06-07 | 增加 DeepSeek 兼容的 `response_format.json_object`，用于快捷回复二次兜底 |
| 1.11 | 2026-06-08 | 新增角色提示词自动生成规范，支持使用 createPromptCompletion 纯文本生成接口并绕过 ToolCallCoordinator |
| 1.12 | 2026-06-08 | 角色提示词自动生成改为流式 CHAT 请求，仓库层累积 chunk 且继续禁用工具调用 |
| 1.13 | 2026-06-08 | 增加多模态 API 与模型配置规范，支持单独配置多模态模型并将 `supportVision` 置为 `true` |
| 1.14 | 2026-06-08 | 开启多模态与附件选择支持，常规文件默认转为 Base64 多模态传输，文本拼接方案留作可选（`enableFileAppend` 开关控制） |
| 1.15 | 2026-06-08 | 支持从消息附件表重新加载并重播多模态历史信息，通过异步读取 URI 转换 Base64 实现 |
| 1.16 | 2026-06-09 | 增加语音合成与音色克隆模型配置，支持 VoiceSynthesisTool 产生的语音附件及其时长持久化 |
| 1.17 | 2026-06-11 | 角色列表改用 DAO 摘要投影，完整角色按 ID 加载；明确多角色状态隔离与 ChatMessageSender 职责 |
| 1.18 | 2026-06-11 | 图片生成增加 OpenAI 兼容与阿里百炼 Provider，统一 URL/Base64 输出并移除工具内协议拼装 |
| 1.19 | 2026-06-11 | 默认文本模型设置增加阿里百炼选项，通过委托适配器复用 OpenAI 兼容聊天协议 |
| 1.20 | 2026-06-11 | 默认、多模态和语音设置增加小米 MiMo，并支持套餐与按量 Host 自动探测 |
