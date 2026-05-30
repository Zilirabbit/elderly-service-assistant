# 2026-05-30 Phase：问答页输入框与微信式语音输入轻修方案

> 目标：只修问答页底部文字输入框和语音输入框交互，不改 RAG、Dify、后端接口、材料清单、办理判断或结构化回答卡片。  
> 当前阶段重点是让“打字提问”和“语音提问”在问答页内更稳定、更像真实 App，并保证识别结果能进入当前聊天列表。

---

## 0. 背景

当前问答页已经完成结构化 RAG 回答展示，包括：

- 结论
- 情况选择
- 怎么办
- 需要什么材料
- 注意事项
- 详细说明展开/收起
- 查看材料清单
- 朗读回答
- 继续追问

现在只处理三个输入相关问题：

1. 底部文字输入框较长文本会被折叠、省略，且已发送问题会停留在输入框内。
2. 语音输入已经有面板，但录音没有后台最长时长限制。
3. 语音识别或音频样本识别后的文本，没有稳定作为用户消息追加到聊天列表最下方。

---

## 1. 修改范围

优先检查并只修改以下类型文件：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/component/ElderCareComponents.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/voice/VoiceRecorder.kt
```

如果实际项目中文件名不同，以现有问答页、输入栏、语音录制、语音面板所在文件为准。

---

## 2. 不要修改

本次是轻修，不要扩大范围。

不要修改：

```text
RAG Prompt
Dify 配置
FastAPI 后端接口
chat_schema.py
dify_service.py
routes_chat.py
材料清单页
首页
服务页
底部导航
结构化回答卡片布局
办理判断流程
语音 TTS 朗读逻辑
```

不要做：

```text
不要引入新依赖
不要重构项目结构
不要大范围格式化无关文件
不要实现数据库聊天历史
不要实现云端历史同步
不要改模型调用逻辑
不要跑全量测试
```

---

## 3. 目标一：修复底部文字输入框

### 3.1 当前问题

底部输入框在较长文本时会显示为类似：

```text
第一次办港澳通行证要带什...
```

并且问题发送后，输入框里仍然残留上一条已发送内容。

这会让用户误以为问题没有发送成功，或者输入框卡住了。

### 3.2 修改要求

请将问答页底部输入框改为：

1. 输入框默认显示 1 行。
2. 输入较长问题时，最多展开到 2 行。
3. 超过 2 行时不要撑爆布局，可以内部滚动或保持 2 行高度。
4. 输入框宽度应尽量占满麦克风按钮和发送按钮之间的空间。
5. 点击发送并成功提交后，清空输入框。
6. 输入框为空时显示 placeholder：

```text
输入或语音提问
```

7. 已发送的问题只显示在聊天列表中，不要继续停留在输入框里。

### 3.3 交互效果

发送前：

```text
[麦克风] [第一次办港澳通行证要带什么？] [发送]
```

发送后：

```text
聊天列表新增用户消息：第一次办港澳通行证要带什么？
输入框恢复为空，显示：输入或语音提问
```

### 3.4 验收标准

- 长文本输入不会异常折叠、重叠或只露出一半。
- 输入框最多展开到 2 行。
- 发送后输入框清空。
- 上一条问题不会长期停留在输入框内。
- 已发送问题正常显示为用户消息气泡。

---

## 4. 目标二：语音输入改成微信式体验

### 4.1 设计原则

语音输入可以像微信一样简单：

- UI 不显示具体倒计时。
- 用户只看到“正在听您说话”和“停止并识别”。
- 后台仍然必须设置最大录音时长，避免无限录音。
- 到达最大时长后自动停止并进入识别流程。

### 4.2 最大录音时长

后台设置最大录音时长：

```text
30 秒
```

如果项目中已有常量文件，可以定义为：

```kotlin
private const val MAX_RECORD_SECONDS = 30
```

如果没有统一常量文件，可以先放在语音录制或 ChatViewModel 相关文件中。

### 4.3 语音面板状态

#### 初始状态

显示：

```text
语音提问
选择语种后说话

[普通话] [方言] [英语]

[开始说话]
[选择音频样本]
```

说明：

- 保留现有“普通话 / 方言 / 英语”选择。
- 保留“选择音频样本”入口。
- 不显示计时器。

#### 录音中状态

点击“开始说话”后显示：

```text
正在听您说话
说完后点停止

[停止并识别]
[取消]
```

要求：

- 不显示 `00:08 / 00:30`。
- 不显示倒计时。
- 可以保留录音图标或动效，但不要增加复杂动画。

#### 用户点击“停止并识别”

流程：

```text
停止录音
↓
上传音频或调用现有识别流程
↓
进入 Recognizing 状态
```

#### 用户点击“取消”

流程：

```text
停止录音
↓
关闭录音状态
↓
不上传
↓
不生成用户消息
↓
回到语音初始状态或关闭面板
```

#### 自动超时

录音达到 30 秒时，自动执行：

```text
停止录音
↓
显示：录音已自动结束，正在识别...
↓
进入识别流程
```

可以短暂显示提示文案：

```text
录音已自动结束，正在识别...
```

不需要在录音过程中显示倒计时。

### 4.4 防重复录音

录音过程中应避免重复点击导致多个录音任务同时存在。

要求：

- Recording 状态下，“开始说话”按钮不可见或不可点。
- 点击“停止并识别”后，按钮进入不可重复点击状态。
- 点击“取消”后确保释放录音资源。

### 4.5 验收标准

- 录音过程中 UI 不显示具体计时器。
- 最长录音不会超过 30 秒。
- 到 30 秒会自动停止并识别。
- 点击“停止并识别”可以提前结束录音。
- 点击“取消”不会上传音频，也不会产生聊天消息。
- 录音状态不会卡住。
- 不会出现多个录音任务同时运行。

---

## 5. 目标三：语音识别结果进入当前聊天列表

### 5.1 当前问题

语音识别或音频样本识别后，识别文本没有稳定出现在聊天列表最下方。

这不要求做数据库历史记录，只需要当前页面内的消息列表能追加即可。

### 5.2 当前页面临时聊天记录

本次只做“当前页面临时 messages”。

含义：

```text
用户在当前问答页连续问答时：
用户问题 1
AI 回答 1
用户问题 2
AI 回答 2
```

这些消息在当前页面存在即可。

不要求：

```text
退出 App 后仍保留
右上角历史页可查看
云端同步
账号级历史
```

### 5.3 语音识别成功后的确认流程

语音识别成功后，不要直接丢失结果。

面板显示：

```text
我听到的是：
xxx

[发送查询]
[重新说一遍]
[手动修改]
```

其中 `xxx` 是识别文本。

### 5.4 点击“发送查询”

流程：

```text
读取 recognizedText
↓
如果为空，不发送，提示“没有听清，请重新说一遍，或改用文字输入”
↓
append 为用户消息
↓
清空输入框
↓
关闭语音面板
↓
自动滚动聊天列表到底部
↓
调用现有问答接口发起查询
```

注意：

- 不要重复追加同一条消息。
- 不要生成空白用户消息。
- 发送查询后，输入框应清空。

### 5.5 点击“手动修改”

流程：

```text
将 recognizedText 填入底部输入框
↓
关闭语音面板
↓
不自动发送
```

用户可以修改后再手动点击发送。

### 5.6 点击“重新说一遍”

流程：

```text
清空 recognizedText
↓
回到语音输入初始状态
```

### 5.7 识别失败或识别为空

显示：

```text
没有听清，请重新说一遍，或改用文字输入。
```

按钮：

```text
[重新说一遍]
[手动输入]
```

点击“手动输入”：

```text
关闭语音面板
聚焦底部输入框或仅返回输入框
```

### 5.8 验收标准

- 语音识别成功后，用户能看到识别文本。
- 点击“发送查询”后，识别文本作为用户气泡显示在聊天列表最下方。
- 页面自动滚动到底部。
- 点击“手动修改”后，识别文本进入底部输入框，但不自动发送。
- 点击“重新说一遍”后，能重新录音。
- 识别为空或失败时，不生成空白消息。

---

## 6. 目标四：音频样本识别结果走同一流程

当前语音面板有“选择音频样本”入口。请让音频样本识别结果和现场录音结果走同一套后续逻辑。

### 6.1 要求

音频样本识别成功后，也显示：

```text
我听到的是：
xxx

[发送查询]
[重新选择]
[手动修改]
```

点击“发送查询”后：

```text
append 用户消息
清空输入框
关闭语音面板
滚动到底部
调用现有问答接口
```

点击“重新选择”后：

```text
重新打开选择音频样本逻辑
```

点击“手动修改”后：

```text
识别文本进入底部输入框
关闭语音面板
不自动发送
```

### 6.2 验收标准

- 现场录音和音频样本的识别成功流程一致。
- 成功结果都可以进入聊天列表。
- 失败结果不会生成空白消息。

---

## 7. 实现建议

### 7.1 建议状态

如果现有 `ChatViewModel` 已有类似字段，请复用，不要强行重构。

可参考：

```kotlin
var inputText by mutableStateOf("")
var voicePanelVisible by mutableStateOf(false)
var voiceState by mutableStateOf<VoiceInputState>(VoiceInputState.Idle)
var recognizedText by mutableStateOf("")
val messages = mutableStateListOf<ChatMessage>()
```

### 7.2 VoiceInputState 示例

```kotlin
sealed interface VoiceInputState {
    data object Idle : VoiceInputState
    data object Recording : VoiceInputState
    data object Recognizing : VoiceInputState
    data class Recognized(val text: String) : VoiceInputState
    data class Error(val message: String) : VoiceInputState
}
```

说明：

- UI 不显示计时，所以 `Recording` 不需要暴露 elapsedSeconds。
- 后台仍然可以用 coroutine 或 Handler 做 30 秒自动停止。

### 7.3 提交识别文本

```kotlin
fun submitRecognizedText() {
    val text = recognizedText.trim()
    if (text.isBlank()) {
        voiceState = VoiceInputState.Error("没有听清，请重新说一遍，或改用文字输入。")
        return
    }

    appendUserMessage(text)
    inputText = ""
    recognizedText = ""
    voicePanelVisible = false
    sendMessage(text)
}
```

注意：

- 如果现有 `sendMessage(text)` 内部已经会 append 用户消息，不要重复 append。
- 如果现有 `sendMessage(text)` 不会 append，则在调用前 append。
- 最终效果必须保证用户消息只出现一次。

### 7.4 手动修改识别文本

```kotlin
fun editRecognizedText() {
    val text = recognizedText.trim()
    inputText = text
    recognizedText = ""
    voicePanelVisible = false
    voiceState = VoiceInputState.Idle
}
```

### 7.5 重新说一遍

```kotlin
fun retryVoiceInput() {
    recognizedText = ""
    voiceState = VoiceInputState.Idle
}
```

### 7.6 自动停止录音

伪代码：

```kotlin
private var recordTimeoutJob: Job? = null

fun startRecording() {
    if (voiceState is VoiceInputState.Recording) return

    voiceState = VoiceInputState.Recording
    voiceRecorder.start()

    recordTimeoutJob?.cancel()
    recordTimeoutJob = viewModelScope.launch {
        delay(30_000)
        if (voiceState is VoiceInputState.Recording) {
            stopRecordingAndRecognize(autoStopped = true)
        }
    }
}

fun stopRecordingAndRecognize(autoStopped: Boolean = false) {
    recordTimeoutJob?.cancel()
    recordTimeoutJob = null

    val audioFile = voiceRecorder.stop()
    voiceState = VoiceInputState.Recognizing

    if (autoStopped) {
        // 可选：设置临时提示“录音已自动结束，正在识别...”
    }

    transcribe(audioFile)
}

fun cancelRecording() {
    recordTimeoutJob?.cancel()
    recordTimeoutJob = null
    voiceRecorder.cancelOrStopSafely()
    recognizedText = ""
    voiceState = VoiceInputState.Idle
}
```

### 7.7 聊天列表自动滚动到底部

如果当前使用 `LazyColumn`：

```kotlin
LaunchedEffect(messages.size) {
    if (messages.isNotEmpty()) {
        listState.animateScrollToItem(messages.lastIndex)
    }
}
```

如果页面中 AI 回答流式更新或结构化回答更新不是直接改变 `messages.size`，也可以根据最后一条消息内容变化触发滚动。

需要确保聊天列表底部 padding 足够，避免最后一条消息被底部输入栏遮挡。

---

## 8. 关于“聊天历史记录”的阶段安排

本次不做真正历史记录。

### 8.1 当前阶段只做临时消息列表

当前要做的是：

```text
当前问答页内连续对话不丢消息
语音识别结果能作为用户消息进入聊天列表
页面能自动滚动到底部
```

这属于问答页基础交互，不是历史记录功能。

### 8.2 后续再做本地历史记录

建议放在问答页稳定、办理判断页联动之后。

后续本地历史记录可以做：

```text
保存最近 10 到 20 条问答
右上角“历史”按钮查看
保存在本机 DataStore / Room / 简单 JSON
支持清空历史
```

### 8.3 暂不做云端历史

比赛 Demo 阶段不做：

```text
账号登录
云端同步
多设备同步
长期用户画像
```

---

## 9. 最终验收清单

完成后逐项检查：

```text
[ ] 输入框为空时显示“输入或语音提问”
[ ] 输入较长问题时最多显示 2 行
[ ] 发送文字问题后输入框清空
[ ] 已发送问题显示在聊天列表，不残留在输入框
[ ] 点击麦克风能打开语音面板
[ ] 语音面板初始状态正常
[ ] 点击开始说话后显示“正在听您说话 / 说完后点停止”
[ ] 录音中不显示具体计时器
[ ] 后台录音最长 30 秒
[ ] 到 30 秒自动停止并识别
[ ] 点击“停止并识别”能提前停止录音
[ ] 点击“取消”不上传、不产生消息
[ ] 识别成功后显示“我听到的是：xxx”
[ ] 点击“发送查询”后，识别文本进入聊天列表最下方
[ ] 点击“手动修改”后，识别文本进入底部输入框但不发送
[ ] 点击“重新说一遍”后能重新录音
[ ] 识别为空或失败时不生成空白消息
[ ] 音频样本识别结果和现场录音走同一套确认流程
[ ] 页面自动滚动到底部
[ ] 不影响现有结构化 RAG 回答展示
```

---

## 10. 任务结论

本次只做问答页输入相关轻修：

```text
文字输入框：清空、placeholder、最多两行
语音输入：微信式体验、后台 30 秒限制、不显示计时
识别结果：确认后进入当前聊天列表
历史记录：只做当前页面临时 messages，不做持久化历史
```

完成后，问答页的输入体验即可进入可展示状态。真正的聊天历史记录功能放到后续阶段。
