# Phase：问答页结构化办事卡片改造方案（给 Codex）

> 文件建议路径：`docs/phase/2026-05-30-phase-qa-structured-answer-ui.md`  
> 当前任务类型：Android 前端 UI / ViewModel 小步改造  
> 当前主线：本地 / 局域网演示优先，不新增云端部署任务  
> 核心目标：把问答页从“RAG 长文本聊天气泡”改成“适老化办事结果卡片”

---

## 1. 背景

当前项目结构以单仓库为主，Android App 位于：

```text
ElderCareApp/
  app/
    src/main/java/com/example/eldercareapp/
      api/
      model/
      ui/
        component/
        screen/
        theme/
      viewmodel/
      voice/
```

后端位于：

```text
backend/
  app/
    api/
      routes_chat.py
      routes_materials.py
      routes_asr.py
      routes_tts.py
    services/
    schemas/
```

当前问答页已经可以展示用户提问和 RAG 返回内容，但回答区域仍然偏“长文本摘要”。  
本阶段只改问答页展示和相关前端状态，不重做后端、不重做 Dify、不改语音链路、不改底部导航。

---

## 2. 本次改造目标

把问答页从：

```text
用户问一句
↓
AI 返回一整段长文本
↓
用户需要自己阅读和判断
```

改成：

```text
用户问一句 / 点击快捷问题
↓
页面展示结构化办事卡片
↓
用户快速看到：结论、情况确认、怎么办、带什么、注意事项、来源
↓
用户可继续点击：查看材料清单 / 朗读回答 / 继续追问
```

本次不是要让 RAG 立刻输出完美 JSON，而是先在 Android 前端建立稳定的结构化展示壳。  
后续再让后端 / Dify 按该结构返回 JSON。

---

## 3. 本次做什么

### 3.1 问答页视觉结构调整

在现有问答页中保留：

- 顶部栏；
- 用户提问气泡；
- 底部输入栏；
- 底部导航；
- 已有语音按钮；
- 已有发送按钮；
- 已有接口调用流程。

新增或调整：

1. 顶部栏下方增加“常问事项”快捷问题区；
2. 空状态增加示例问题；
3. RAG 加载时展示“正在查询办事资料”的 loading 状态；
4. AI 回答卡片改为结构化办事卡片；
5. 回答顶部增加“结论”高亮区；
6. 回答中增加“您是哪种情况？”选项按钮；
7. 回答内容拆成：
   - 结论
   - 情况确认
   - 怎么办
   - 需要什么材料
   - 注意事项
   - 资料来源
8. 回答底部按钮改为：
   - 查看材料清单
   - 朗读回答
   - 继续追问
9. 对话列表底部增加足够 padding，避免最后内容被输入栏和底部导航遮挡；
10. 弱化资料来源展示，不直接把 `.md` 文件名作为主要用户文案。

---

## 4. 本次不做什么

为了避免影响已有闭环，本次明确不做：

1. 不改后端 FastAPI 接口逻辑；
2. 不改 Dify 应用和知识库；
3. 不新增真实 RAG JSON 输出要求；
4. 不重做导航结构；
5. 不改首页、服务页、我的页面；
6. 不重做办理情况问卷页；
7. 不新增登录、历史同步、云端存储；
8. 不新增人工客服真实接入；
9. 不新增预约、日历、地图功能；
10. 不改现有 ASR / TTS 主链路；
11. 不引入新的第三方 UI 依赖；
12. 不大改主题色、字体系统、底部导航；
13. 不做复杂文本解析，不要用脆弱的正则强行拆 RAG 长文本。

---

## 5. 目标用户体验

### 5.1 空状态

用户第一次进入问答页，没有历史消息时，显示：

```text
您好，我可以帮您查询港澳办事问题

您可以这样问：
[第一次办港澳通行证要带什么？]
[签注过期了怎么办？]
[去香港过关要准备什么？]

不知道怎么问？
[我不确定，帮我判断]
```

说明：

- 示例问题是按钮，不是纯文本；
- 点击示例问题后，直接提交对应标准问题；
- “我不确定，帮我判断”如果已有办理判断页，则跳转办理判断页；如果当前导航参数不方便接入，先 Toast：`将进入办理情况判断`，不要硬改导航。

---

### 5.2 顶部快捷问题区

在顶部栏下方增加横向滚动胶囊按钮：

```text
常问事项：
[首次办证] [签注续签] [过关材料] [我不确定]
```

点击后发送标准问题：

| 按钮 | 标准问题 |
|---|---|
| 首次办证 | 第一次办理港澳通行证需要怎么做？ |
| 签注续签 | 已有港澳通行证，签注过期或用完了怎么办？ |
| 过关材料 | 去香港澳门过关需要准备什么材料？ |
| 我不确定 | 我不确定自己属于哪种港澳办理情况，应该怎么判断？ |

如果已有办理情况问卷入口，则“我不确定”优先跳转问卷；否则按标准问题提问。

---

### 5.3 加载状态

提交问题后，在 AI 回答位置展示：

```text
正在查询相关办事资料...
```

不要写“AI 正在思考”。  
项目定位是政务办事助手，表达应偏“查资料 / 办事依据”。

---

### 5.4 错误状态

接口失败时展示：

```text
查询失败，请稍后再试。
```

按钮：

```text
[重新查询]
```

知识库无明确答案时，如果后端返回空答案或错误态，展示：

```text
暂时没有在知识库中找到明确说明。
建议咨询当地出入境窗口或官方渠道。
```

按钮：

```text
[换个说法再问]
```

---

## 6. AI 回答卡片目标结构

结构化卡片大致如下：

```text
🤖 我帮您查到这些
根据办事资料整理，办理前请以当地窗口要求为准

【结论】
可以办理。首次申请一般需要本人到出入境窗口办理。

【您是哪种情况？】
[首次办理] [已有证件续签] [证件过期/遗失] [我不确定]

【怎么办】
1. 准备身份证件和照片
2. 到出入境窗口提交申请
3. 现场采集信息
4. 等待审核和取证

【需要什么材料】
常见必备材料：
□ 身份证件
□ 符合要求的照片
□ 出入境证件申请表

可能还需要：
□ 监护人证明
□ 居住证或异地办理材料

【注意事项】
- 各地政策可能不同，办理前建议确认。
- 首次办理通常需要本人到场。
- 未满 16 周岁通常需要监护人陪同。

资料来源：港澳通行证办理指南

[查看材料清单]
[朗读回答] [继续追问]
```

---

## 7. 推荐涉及文件

请优先在以下文件中做最小改动。  
如果实际项目文件名不同，请按当前仓库真实结构就近处理。

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/model/ChatModels.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/component/ElderCareComponents.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/api/AssistantApi.kt
```

原则：

- `ElderCareScreens.kt`：只改问答页相关 Composable；
- `ElderCareComponents.kt`：新增可复用 UI 组件；
- `ChatViewModel.kt`：增加 UI 状态映射，不重写业务流；
- `ChatModels.kt`：增加兼容字段或 UI model，不破坏现有接口；
- `AssistantApi.kt`：除非现有响应模型必须补字段，否则不改接口定义。

---

## 8. 数据结构建议

### 8.1 保留现有接口模型

不要删除现有 `ChatRequest`、`ChatResponse`、消息列表模型。  
如果当前已有模型字段名不同，以现有为准。

### 8.2 新增前端 UI Model

建议新增一个只用于前端展示的 UI model。可以放在 `ChatModels.kt` 或 `ChatViewModel.kt` 附近。

示例：

```kotlin
data class QaAnswerUiModel(
    val title: String = "我帮您查到这些",
    val subtitle: String = "根据办事资料整理，办理前请以当地窗口要求为准",
    val conclusion: String,
    val scenarioOptions: List<QaScenarioOption> = defaultScenarioOptions(),
    val steps: List<String> = emptyList(),
    val requiredMaterials: List<String> = emptyList(),
    val optionalMaterials: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val sourceTitle: String? = null,
    val originalAnswer: String? = null,
    val confidence: String? = null,
    val needHumanReminder: Boolean = true
)

data class QaScenarioOption(
    val label: String,
    val standardQuestion: String
)
```

默认情况按钮：

```kotlin
fun defaultScenarioOptions(): List<QaScenarioOption> = listOf(
    QaScenarioOption(
        label = "首次办理",
        standardQuestion = "第一次办理港澳通行证需要怎么做？"
    ),
    QaScenarioOption(
        label = "已有证件续签",
        standardQuestion = "已有港澳通行证，签注过期或用完了怎么办？"
    ),
    QaScenarioOption(
        label = "证件过期/遗失",
        standardQuestion = "港澳通行证过期、遗失或损坏了应该怎么办？"
    ),
    QaScenarioOption(
        label = "我不确定",
        standardQuestion = "我不确定自己属于哪种港澳办理情况，应该怎么判断？"
    )
)
```

---

## 9. 响应映射策略

当前后端可能只返回一段自然语言。  
本阶段不要强行复杂解析。

建议在 `ChatViewModel` 或 mapper 中采用兼容映射：

### 9.1 如果后端已有结构化字段

优先使用：

```text
summary / conclusion
steps
materials
warnings
sources
nextActions
displayText
answer
```

### 9.2 如果后端只有 answer/displayText

采用保守 fallback：

```text
conclusion = answer/displayText 的前 1～2 句，最多 80 字
originalAnswer = 完整 answer/displayText
steps = emptyList()
requiredMaterials = emptyList()
optionalMaterials = emptyList()
warnings = 默认人工核实提醒
sourceTitle = sources 第一个标题；没有则显示“知识库资料”
```

此时卡片显示：

```text
结论
{前 1～2 句}

详细说明
{完整回答}

注意事项
- 具体要求以当地出入境管理部门或现场窗口为准。
```

不要为了视觉效果编造材料、步骤或来源。

---

## 10. 组件拆分建议

在 `ElderCareComponents.kt` 中新增或就近实现：

```kotlin
@Composable
fun QaQuickQuestionChips(...)

@Composable
fun QaEmptyState(...)

@Composable
fun AssistantStructuredAnswerCard(...)

@Composable
fun QaConclusionBox(...)

@Composable
fun QaScenarioChips(...)

@Composable
fun QaInfoSection(...)

@Composable
fun QaMaterialSummarySection(...)

@Composable
fun QaSourceNotice(...)

@Composable
fun QaAnswerActions(...)

@Composable
fun QaLoadingCard(...)

@Composable
fun QaErrorCard(...)
```

如果项目当前不习惯这么细拆，也可以只新增 3 个核心组件：

```kotlin
QaQuickQuestionChips
QaEmptyState
AssistantStructuredAnswerCard
```

---

## 11. UI 样式要求

整体保持现有适老化政务蓝风格。

### 11.1 字号

建议：

```text
页面标题：20sp 左右
卡片标题：18sp 左右，加粗
正文：16sp 起
按钮文字：16sp 起
来源/说明：13-14sp
```

### 11.2 点击区域

所有按钮最小高度建议：

```text
48dp
```

不要使用过小的 icon-only 操作入口。

### 11.3 卡片

建议：

```text
圆角：16dp - 20dp
内边距：16dp
卡片间距：12dp
```

### 11.4 颜色

不要新增复杂配色。使用现有 Theme 中的蓝色、浅蓝背景、灰色文字即可。  
不要大改 `Color.kt`、`Theme.kt`、`Type.kt`。

---

## 12. 交互要求

### 12.1 快捷问题按钮

点击后调用现有发送逻辑，不新建接口。

伪逻辑：

```kotlin
onQuickQuestionClick(question) {
    chatViewModel.sendMessage(question)
}
```

### 12.2 情况确认按钮

点击后也走现有发送逻辑：

```kotlin
onScenarioClick(option.standardQuestion) {
    chatViewModel.sendMessage(option.standardQuestion)
}
```

### 12.3 查看材料清单

如果当前已有材料清单页导航：

```kotlin
onViewMaterialsClick {
    navigateToMaterialScreen(...)
}
```

如果当前材料页还不能接收动态参数：

```kotlin
Toast: "正在整理材料清单"
```

不要为了这个按钮重做导航。

### 12.4 朗读回答

复用已有朗读逻辑。

优先朗读：

```text
conclusion + steps + materials + warnings
```

如果没有结构化字段，则朗读完整 `originalAnswer`。

不要新增 TTS 后端接口。

### 12.5 继续追问

点击后：

- 聚焦输入框；或
- Toast：`可以继续输入您的问题`；或
- 在输入框 placeholder 显示“继续追问，例如：需要预约吗？”

不要新建页面。

---

## 13. 顶部栏处理建议

当前右上角图标如果是“日历”和“设置”，本次不重做功能，只调整文案/语义。

可选最小做法：

- 日历图标点击 Toast：`查看问答历史`
- 设置图标点击 Toast：`设置朗读语言`
- 不新增完整历史页；
- 不新增完整设置页。

如果现有图标已有逻辑，不要删除。

---

## 14. 列表与底部遮挡处理

当前问答页底部同时存在输入栏和底部导航，长回答容易被遮挡。

请在消息列表底部增加 padding，例如：

```kotlin
contentPadding = PaddingValues(
    start = 12.dp,
    end = 12.dp,
    top = 12.dp,
    bottom = 120.dp
)
```

具体数值按当前布局调整。  
目标是最后一个 AI 卡片底部按钮不被输入栏 / 底部导航挡住。

---

## 15. 建议页面结构伪代码

仅供参考，按现有代码风格改。

```kotlin
@Composable
fun ChatScreen(...) {
    Column {
        QaTopBar(...)

        QaQuickQuestionChips(
            items = quickQuestions,
            onClick = { question -> chatViewModel.sendMessage(question) }
        )

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                QaEmptyState(
                    onExampleClick = { question -> chatViewModel.sendMessage(question) },
                    onGuidanceClick = { /* navigate or toast */ }
                )
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(messages) { message ->
                        if (message.isUser) {
                            UserMessageBubble(message.text)
                        } else {
                            AssistantStructuredAnswerCard(
                                answer = message.answerUiModel,
                                onScenarioClick = { option ->
                                    chatViewModel.sendMessage(option.standardQuestion)
                                },
                                onViewMaterialsClick = { /* existing material navigation or toast */ },
                                onSpeakClick = { /* existing speak logic */ },
                                onContinueClick = { /* focus input or toast */ }
                            )
                        }
                    }

                    if (isLoading) {
                        item { QaLoadingCard() }
                    }

                    errorMessage?.let {
                        item { QaErrorCard(...) }
                    }
                }
            }
        }

        ChatInputBar(...)
    }
}
```

---

## 16. 验收标准

### 16.1 基础验收

- App 可以正常编译；
- 问答页可以正常打开；
- 现有文字问答仍可调用后端；
- 现有语音按钮不被破坏；
- 底部导航不被破坏；
- 首页、服务页、我的页不受影响。

### 16.2 UI 验收

- 空状态展示 3 个示例问题；
- 顶部展示“常问事项”快捷问题；
- 点击快捷问题能自动发起问答；
- AI 回答不再只是一整段无结构文本；
- 回答卡片包含：
  - 结论
  - 情况确认
  - 怎么办 / 详细说明
  - 需要什么材料
  - 注意事项
  - 资料来源
  - 操作按钮
- 长回答滚动到底时，底部按钮不被输入栏遮挡；
- 资料来源显示为用户友好标题，不突出 `.md` 文件名。

### 16.3 兼容验收

- 后端只返回 `answer` 时，页面仍能显示；
- 后端返回空 answer 时，有错误 / 空结果提示；
- 不因为缺少结构化字段崩溃；
- 不编造材料和步骤；
- 不强依赖新的后端字段。

---

## 17. 后续拓展方向

本次完成后，后续可以分阶段拓展。

### 17.1 RAG 结构化 JSON 输出

后端 / Dify 后续可返回：

```json
{
  "answer_type": "gov_service_qa",
  "summary": "可以办理。首次申请一般需要本人到出入境窗口办理。",
  "need_clarification": true,
  "clarification_options": ["首次办理", "已有证件续签", "证件过期或遗失", "我不确定"],
  "steps": ["准备身份证件和照片", "到出入境窗口提交申请", "现场采集信息", "等待审核和取证"],
  "materials": {
    "required": ["身份证件", "符合要求的照片", "出入境证件申请表"],
    "possible_extra": ["监护人证明", "居住证或异地办理材料"]
  },
  "warnings": ["各地政策可能不同", "首次办理通常需要本人到场"],
  "sources": [{"title": "港澳通行证办理指南"}],
  "confidence": "medium"
}
```

Android 前端只需要继续复用本阶段的结构化卡片。

### 17.2 材料清单页联动

后续“查看材料清单”可跳转到真正 checklist：

```text
必备材料
□ 身份证原件
□ 符合要求的照片
□ 出入境证件申请表

可能需要
□ 监护人证明
□ 居住证
```

支持：

- 勾选；
- 保存；
- 分享给家人；
- 生成窗口说明。

### 17.3 办理判断流程联动

后续“我不确定”进入现有办理判断问卷：

```text
是否已有港澳通行证
证件是否有效
办理事项
目的地
出行目的
```

问卷结果生成 `standardQuestion` 后，复用问答页结构化卡片展示。

### 17.4 召回测试与知识流水

后续应补充：

```text
eval/questions.csv
eval/retrieval_records.md
docs/knowledge/raw/
docs/knowledge/clean/
```

测试目标：

- 是否召回正确文档；
- 是否区分首次办理、签注续签、换发补发；
- 是否没有编造材料；
- 是否能提示以窗口为准；
- 是否回答足够短。

### 17.5 志愿者 / 客服入口

后续只在这些场景展示：

- 知识库无答案；
- 用户连续追问仍不清楚；
- 用户点击“我还是不会办”；
- 涉及特殊情况。

文案：

```text
还是不确定？
[联系志愿者协助]
```

本阶段不接入真实客服。

---

## 18. 给 Codex 的执行要求

请按以下要求执行：

1. 只做问答页结构化 UI 改造；
2. 保持现有项目结构；
3. 优先修改 Android 前端文件；
4. 不改后端接口逻辑；
5. 不改 Dify 配置；
6. 不新增第三方依赖；
7. 不大改主题；
8. 不重写导航；
9. 不删除现有功能；
10. 不做调试性大范围重构；
11. 所有新增字段必须 nullable 或有默认值；
12. 后端仍只返回纯文本时，页面必须可用；
13. 不要编造材料、步骤、来源；
14. 代码完成后只说明修改了哪些文件和如何验证。

---

## 19. 推荐提交标题

```text
feat(android): improve QA page with structured service answer card
```

---

## 20. 最小验证路径

1. 启动 Android App；
2. 进入问答页；
3. 未提问时看到空状态；
4. 点击“首次办证”；
5. 页面显示 loading；
6. 返回回答后显示结构化卡片；
7. 点击“已有证件续签”情况按钮；
8. 能继续发起标准问题；
9. 点击“朗读回答”，复用已有朗读能力或出现当前项目已有提示；
10. 点击“查看材料清单”，能跳转已有材料页或显示 Toast；
11. 滚动到底部，确认按钮没有被输入栏遮挡。

---

## 21. 备注

本阶段重点不是让 AI 回答变得更“聪明”，而是让老人更容易看懂。  
真正的准确率提升应放到后续 RAG Prompt、知识库清洗、召回测试和办理判断规则中继续做。
