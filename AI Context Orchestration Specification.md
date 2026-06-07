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
- `shared/src/commonMain/.../data/chat/ChatSessionRepository.kt`
- `shared/src/roomMain/.../data/chat/RoomChatSessionRepository.kt`
- `shared/src/commonMain/.../data/api/OpenAiRepository.kt`

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

### 3.3 Current Input

当前输入是本次发送操作产生的用户消息。它在构造上下文前 MUST 已写入数据库，
但编排器通过独立参数追加它，避免仓库读取时序导致重复或遗漏。

## 4. 会话选择规则

打开或切换角色时：

1. 取消上一角色的消息订阅。
2. 查询该角色最近的未归档、未软删除会话。
3. 最近会话按 `last_message_at DESC` 选择。
4. 找到会话后，按 `seq ASC` 观察并展示消息。
5. 未找到会话时，读取角色 `openingMessage`；非空时只在 UI 显示该欢迎消息。

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
  -> 调用 CHAT API
  -> 持久化成功或失败的助手消息
  -> 更新 UI 状态和一次性 Effect
```

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
+ [裁剪后的有效历史消息，保持原始顺序]
+ [当前用户输入]
```

参考算法：

```kotlin
val effectiveHistory = history
    .filter { it.status == COMPLETED && !it.isContextExcluded }
    .takeLastIfLimited(maxContextMessageCount)

return listOfNotNull(nonBlankSystemPrompt) +
    effectiveHistory +
    currentUserMessage
```

### 6.1 System Prompt

system prompt 的选择优先级 MUST 为：

1. 非空的 `customSystemPrompt`。
2. `systemPromptSnapshot`。
3. 两者都为空时不发送 system 消息。

system prompt 必须位于请求消息列表首位，且不计入历史消息数量限制。

### 6.2 历史过滤

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

### 6.3 历史数量限制

`maxContextMessageCount` 的语义是“最多保留多少条历史消息”：

- `null`：不按条数裁剪。
- `0`：不保留历史，但仍发送 system prompt 和当前输入。
- 正数：保留过滤后最近 N 条历史消息。
- 负数：视为未配置，不执行裁剪；写入层 SHOULD 阻止负数配置。

裁剪 MUST 在过滤之后执行，并使用 `takeLast(N)` 语义。裁剪后的消息仍按 `seq ASC`
发送。当前用户输入不计入 N，并且 MUST 始终保留。

### 6.4 去重要求

仓库读取的 `history` MUST 表示发送本次输入之前的历史。当前用户消息只能由
`currentUserMessage` 参数追加一次。

若未来改为从数据库重新读取包含当前输入的消息，必须同时修改编排接口或增加基于
消息 ID 的去重，禁止依靠文本内容去重。

## 7. CHAT API 请求

当前使用 OpenAI 兼容接口：

- 方法：`POST /chat/completions`
- 鉴权：`Authorization: Bearer <apiKey>`
- Content-Type：`application/json`
- `stream`：`false`

请求参数来自当前默认 `ModelConfig`：

- `model`
- `temperature`
- `top_p`
- `max_tokens`
- `messages`

### 7.1 快捷回复（Quick Replies）请求编排

为了向用户提供针对助手回复的快捷回复选项，编排器必须根据模型对工具调用的支持情况进行相应处理：

- **当 `supportToolCall == true` 时（优先采用工具调用）**：
  1. 在请求体中必须装配 `tools` 参数，定义 `respond_with_quick_replies` 函数，参数包含 `ai_response`（字符串，人设扮演正文）和 `suggestions`（字符串数组，3 个快捷选项建议）。
  2. 在请求体中必须指定 `tool_choice: "required"`。当前请求只提供
     `respond_with_quick_replies` 一个工具，因此该设置等价于强制调用此函数，同时
     兼容更多仅实现字符串形式 `tool_choice` 的 OpenAI 兼容服务。
  3. 工具调用能力检测必须验证响应中实际存在目标 `tool_calls`，不得仅凭接口接受
     `tools` 参数或返回 HTTP 200 判定支持。
- **当 `supportToolCall == false` 时（文本注入 fallback 方案）**：
  1. 编排器在 `ChatContextBuilder` 构建上下文时，必须在 system 提示词尾部注入格式化规范，要求模型在正文末尾将建议回复选项用特定的 XML 标签包围，并在最新的 user 消息末尾加入格式提醒。

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
   - **不支持工具调用的模型**：读取助手消息的文本内容，通过正则表达式 `<suggestions>([\s\S]*?)</suggestions>` 提取匹配的内容（按行解析出最多 3 个纯文本选项作为 `suggestions` 列表），并将 `<suggestions>...</suggestions>` 标签块（含内容）从文本中彻底移除、剥离，清理出纯净正文内容。
2. 对内容执行首尾空白清理。
3. 内容非空时写入 `completed` 助手消息，并将提取到的 `suggestions` 持久化到数据库（序列化为 JSON 字符串保存）。
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

- ViewModel MUST 使用 `isSending` 阻止同一页面的并发发送。
- 会话创建与首条用户消息 MUST 原子提交。
- 追加消息和更新会话最后消息信息 MUST 原子提交。
- 消息顺序 MUST 由持久化层的 `seq` 决定，不能依赖时间戳排序。
- 切换角色时 MUST 取消旧会话 Flow，旧 Flow 不得覆盖新角色 UI。
- ID MUST 跨平台安全且在本地数据库范围内唯一。

未来若允许并发生成、分支或重试，必须先引入明确的 parent message、请求 ID 或分支
模型，不得继续假设“会话内永远只有一个进行中的请求”。

## 10. 平台边界

公共编排逻辑位于 `commonMain`，只依赖：

- `ChatSessionRepository`
- `ModelConfigRepository`
- `OpenAiRepository`
- 公共领域模型和 Flow

Android、iOS 和 Desktop 使用同一个 `StarRailDatabase` 实例提供 Room 仓库。
JavaScript/WasmJS 不得依赖 Room 实体、DAO 或文件路径，应使用符合相同接口语义的
浏览器持久化或内存实现。

不同平台实现 MAY 改变存储介质，但不得改变会话选择、过滤、裁剪、错误记录和请求
消息顺序。

## 11. 当前未启用能力

以下字段或能力可能已有数据结构预留，但不属于当前基础流程：

- 流式响应与停止生成。
- 对话摘要和 token 阈值压缩。
- 附件、图片和 OCR 内容注入。
- 多轮交互的工具调用（Tool Use）及工具结果反馈消息。
- 消息分支、重新生成和父消息链。
- 自动会话命名。
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
- 本规范及相关测试。

## 13. 测试与验收

最低测试覆盖：

- system prompt 位于首位，空 prompt 不产生 system 消息。
- 失败和排除消息不会进入上下文。
- 历史限制为 `null`、`0`、正数时行为正确。
- 当前用户输入始终位于最后且只出现一次。
- 首次发送原子创建会话和用户消息。
- 后续发送复用会话并保持 `seq` 顺序。
- 配置缺失、HTTP、网络、空响应均保留用户消息并写失败记录。
- 成功响应保存助手内容、模型快照和 usage。
- 角色切换不会被旧会话 Flow 覆盖。
- CHAT API 路径、鉴权、Content-Type 和请求参数正确。

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
