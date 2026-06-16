# Phase：问答页本地历史记录 MVP（前端本地持久版）

## 背景

当前问答页右上角已经有“历史”按钮，但点击后只是 Toast 占位。现有 `ChatViewModel` 已经维护了完整的问答消息列表、`conversationId`、语音识别草稿、结构化回答卡片等状态，因此可以先做一个前端本地历史记录 MVP，不改后端、不改 Dify、不新增登录同步。

请基于当前项目代码最小实现问答历史功能。

重点涉及文件：

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/model/ChatModels.kt`
- 多语言文案：`values/strings.xml`、`values-en/strings.xml`、`values-zh-rHK/strings.xml` 等现有资源文件

可以新增一个轻量本地存储类，例如：

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/data/local/ChatHistoryStorage.kt`

## 总目标

实现“前端本地持久历史记录 MVP”：

1. 点击问答页右上角“历史”按钮，打开历史 `ModalBottomSheet`，不再 Toast。
2. 成功完成一次问答后，自动保存这一轮问答到本地历史。
3. 历史最多保留最近 30 条，按时间倒序显示。
4. 点击历史项后恢复完整问答内容。
5. 支持历史项“重新提问”。
6. 支持删除单条历史、清空全部历史。
7. 不改后端接口，不改 Dify 参数，不新增登录/云同步。
8. 不新增独立底部 Tab，不新增一级页面，不改变现有首页/服务页/问答主结构。

## 现状约束

当前代码中：

- `ChatScreen` 顶部 `UnifiedTopBar` 已有：
  - `TopBarAction(stringResource(R.string.common_history), Icons.Filled.DateRange) { Toast... }`
- `ElderCareScreens.kt` 已使用过 `ModalBottomSheet`，可以复用 Material3 bottom sheet 风格。
- `ChatViewModel` 中已有：
  - `uiState.messages`
  - `uiState.conversationId`
  - `sendQuestion(...)`
  - `submitPrefilledQuestion(...)`
  - `confirmVoiceDraft(...)`
  - `ChatMessageUi`
  - `QaAnswerUiModel`
- 语音录音文件不需要保存到历史；只保存识别后的文字。
- TTS 音频 URL 可以保存，但恢复历史时不要强依赖它；如果不可用，不影响展示。

## 数据模型建议

在 `ChatModels.kt` 新增历史相关前端 model。不要让 model 层依赖 viewmodel 层，建议使用独立的历史消息结构。

建议新增：

```kotlin
data class ChatHistoryItem(
    val id: String = "",
    val question: String = "",
    val answerPreview: String = "",
    val messages: List<ChatHistoryMessage> = emptyList(),
    val conversationId: String = "",
    val inputType: String = "text",
    val displayLanguage: String = "zh-CN",
    val speechLanguage: String = "zh-CN",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

data class ChatHistoryMessage(
    val id: Long = 0L,
    val role: String = "user", // "user" or "assistant"
    val text: String = "",
    val answerUiModel: QaAnswerUiModel? = null,
    val ttsText: String = "",
    val ttsAudioUrl: String? = null,
)
```

注意：

- `QaAnswerUiModel` 已经是纯 data class，可以继续用于恢复结构化卡片。
- `role` 用字符串，避免把 `ChatMessageRole` 从 ViewModel 暴露到 model 层。
- `id` 使用 `UUID.randomUUID().toString()` 或基于时间生成均可。

## 本地存储建议

当前项目不建议为了 MVP 新增 Room/DataStore 重依赖。优先使用轻量方案：

- 如果项目已有 Gson/Moshi 依赖，可以复用。
- 如果没有现成 JSON 依赖，不要为了历史功能新增重依赖，使用 Android 自带 `org.json` 手写序列化/反序列化。

新增 `ChatHistoryStorage`，职责：

```kotlin
class ChatHistoryStorage(context: Context) {
    fun load(): List<ChatHistoryItem>
    fun save(items: List<ChatHistoryItem>)
}
```

实现要求：

- 使用 `context.applicationContext.getSharedPreferences(...)`，不要持有 Activity context。
- key 示例：`chat_history_items_v1`
- 解析失败时返回空列表，不要让 App 崩溃。
- 保存前按 `updatedAtMillis` 倒序，最多保留 30 条。
- 字段缺失时使用默认值，保证后续版本兼容。

## ChatViewModel 修改要求

在 `ChatUiState` 中新增：

```kotlin
val historyItems: List<ChatHistoryItem> = emptyList()
```

在 `ChatViewModel` 中新增：

```kotlin
fun attachHistoryStorage(storage: ChatHistoryStorage)
fun restoreHistory(itemId: String)
fun deleteHistory(itemId: String)
fun clearHistory()
fun resendHistoryQuestion(
    itemId: String,
    displayLanguage: String = "zh-CN",
    speechLanguage: String = "zh-CN",
    uiStrings: ChatUiStrings = ChatUiStrings(),
)
```

实现细节：

1. `attachHistoryStorage(storage)`
   - 只初始化一次即可，避免重复加载。
   - 从本地读取历史并写入 `uiState.historyItems`。

2. 成功问答后保存历史
   - 在 `sendQuestion(...)` 请求成功并构造 assistant 消息后保存。
   - 只保存成功回答，不保存 loading 状态。
   - 可以暂不保存空回答。
   - 失败问答是否保存可暂缓，MVP 不做也可以。

3. 保存内容
   - 保存当前这一轮问答对应的完整 messages。
   - `question` 使用用户发送的问题。
   - `answerPreview` 使用 assistant 回答前 60～80 字，去掉换行。
   - `conversationId` 使用后端返回的 `response.conversation_id.orEmpty()`。
   - `inputType` 使用本次 `sendQuestion(...)` 的 `inputType`。
   - `displayLanguage` / `speechLanguage` 使用本次参数。

4. 恢复历史
   - `restoreHistory(itemId)` 将历史里的 messages 转回 `ChatMessageUi` 并替换当前 `uiState.messages`。
   - 同步恢复 `conversationId`，这样恢复后继续追问时可以沿用旧会话。
   - 同步恢复 `lastQuestion`、`answer`、`answerUiModel`、`ttsText`、`ttsAudioUrl`，方便现有 UI 状态一致。
   - 恢复历史时不要自动发起网络请求。
   - 恢复后更新 `nextMessageId`，避免后续新消息 id 与历史消息冲突。

5. 重新提问
   - `resendHistoryQuestion(...)` 不直接恢复历史，而是把历史问题重新走现有 `sendQuestion(...)`。
   - `inputType` 可以用 `history` 或沿用原始 `item.inputType`。推荐用 `history`，便于后续识别来源。

6. 删除
   - `deleteHistory(itemId)` 删除单条并落盘。
   - `clearHistory()` 清空全部并落盘。

## UI 修改要求

在 `ChatScreen` 中：

1. 新增状态：

```kotlin
var showHistorySheet by remember { mutableStateOf(false) }
```

2. 用本地存储连接 ViewModel：

```kotlin
val historyStorage = remember(context.applicationContext) {
    ChatHistoryStorage(context.applicationContext)
}
LaunchedEffect(historyStorage) {
    chatViewModel.attachHistoryStorage(historyStorage)
}
```

3. 修改顶部历史按钮：

```kotlin
TopBarAction(stringResource(R.string.common_history), Icons.Filled.DateRange) {
    showHistorySheet = true
}
```

4. 当 `showHistorySheet == true` 时打开 `ModalBottomSheet`：

- 标题：问答历史
- 空状态：暂无历史记录 / 您问过的问题会显示在这里
- 历史项：大卡片、大点击区域、白底圆角、政务蓝点缀
- 每条显示：
  - 问题标题
  - 回答摘要
  - 时间
  - 输入类型标签：语音提问 / 快捷问题 / 手动输入 / 历史重问
- 每条至少提供：
  - 点击卡片：恢复完整问答，并关闭 sheet
  - “重新提问”按钮：调用 `resendHistoryQuestion(...)`，关闭 sheet
  - “删除”按钮：删除单条，不关闭或关闭均可，推荐不关闭
- 底部提供“清空历史”按钮，有二次确认更好；MVP 如果不做二次确认，也至少按钮文案明确。

建议新增 composable：

```kotlin
@Composable
private fun ChatHistorySheet(
    items: List<ChatHistoryItem>,
    onRestore: (String) -> Unit,
    onResend: (String) -> Unit,
    onDelete: (String) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier,
)
```

也可以拆成：

```kotlin
@Composable
private fun ChatHistoryItemCard(...)
```

视觉要求：

- 沿用当前项目 UI：`ElderBackground`、`ElderBlue`、`ElderBlueSoft`、`ElderLine`、`ElderText`、`ElderTextMuted`。
- 卡片白底、圆角、边框淡蓝/灰线。
- 点击区域高度不小于 64dp，适老化间距。
- 不使用复杂大图 Banner。
- 不新增导航结构。
- 不影响底部 `ChatInputBar`、语音面板、结构化回答卡片现有逻辑。

## 时间显示

新增轻量函数即可，不需要引入时间库：

```kotlin
private fun formatHistoryTime(context: Context, millis: Long): String
```

MVP 可以简单显示：

- 今天 HH:mm
- 昨天 HH:mm
- 更早 MM/dd HH:mm

如果多语言不好处理，第一版也可以只显示 `MM/dd HH:mm`，但文案资源仍要补齐。

## 多语言文案

请在现有多语言资源中补齐，不要只加简中。

建议新增 key：

```xml
<string name="chat_history_title">问答历史</string>
<string name="chat_history_empty_title">暂无历史记录</string>
<string name="chat_history_empty_desc">您问过的问题会显示在这里。</string>
<string name="chat_history_restore">查看完整问答</string>
<string name="chat_history_resend">重新提问</string>
<string name="chat_history_delete">删除</string>
<string name="chat_history_clear_all">清空历史</string>
<string name="chat_history_answered">已回答</string>
<string name="chat_history_input_voice">语音提问</string>
<string name="chat_history_input_quick">快捷问题</string>
<string name="chat_history_input_example">示例问题</string>
<string name="chat_history_input_guidance">引导提问</string>
<string name="chat_history_input_text">手动输入</string>
<string name="chat_history_input_history">历史重问</string>
<string name="chat_history_today">今天</string>
<string name="chat_history_yesterday">昨天</string>
<string name="chat_history_earlier">更早</string>
```

英文、繁中请给自然翻译，保持短句。

## 不要做的事

- 不改后端。
- 不改 Dify 参数。
- 不新增登录或云同步。
- 不新增底部 Tab。
- 不新增独立历史页面。
- 不接 Room/DataStore，除非项目已经明确引入并正在使用。
- 不保存语音音频文件。
- 不改变现有 ASR auto 逻辑。
- 不改变现有 displayLanguage 跟随系统、speechLanguage 单独选择的逻辑。
- 不改首页、服务页、Me 页既有功能。
- 不做大范围重构。

## 验收标准

1. App 编译通过。
2. 进入问答页，点击右上角“历史”，不再 Toast，而是弹出历史 BottomSheet。
3. 没有历史时显示适老化空状态。
4. 发送一个文本问题并成功回答后，再打开历史，可以看到该问题。
5. 点击历史卡片，可以恢复完整问答，结构化回答卡片仍能显示。
6. 点击“重新提问”，会重新调用现有问答流程并生成新回答。
7. 删除单条历史后，该条从列表消失，重进问答页仍保持删除状态。
8. 清空历史后，列表为空，重启 App 后仍为空。
9. 语音提问确认后成功回答，也会保存识别后的文字，不保存音频文件。
10. 最近历史最多保留 30 条。
11. 简中、繁中、英文下历史相关文案不缺失。
12. 不影响现有语音输入、TTS 朗读、快捷问题、引导提问、材料清单入口。

## 建议提交信息

```text
feat(chat): add local persistent chat history sheet
```
