# Phase ：首页 FAQ 本地静态答案详情页改造计划

## 1. 背景

当前项目已经完成后端 RAG 答案缓存的最小闭环：

- 重复政策问答可以命中后端 SQLite RAG 缓存；
- cache hit 时可以绕过 Qwen query rewrite 和 Dify blocking；
- 后端响应中已经增加 `cache_hit`、`cache_key_hash`、`latency_ms`、`source` 等可观测字段；
- TTS 音频缓存也已进一步优化，目标是让 cache hit 后复用稳定 `tts_text`，提升音频缓存命中率。

但首页 FAQ 当前仍存在一个交互问题：

```text
首页 FAQ 虽然是本地静态问题列表，
但点击后仍直接跳转到问答页并提交 RAG 问题。
```

这会导致首页 FAQ 的体验仍然像“快捷提问按钮”，而不是“高频问题答案入口”。

因此， 阶段建议将首页 FAQ 改造为：

```text
首页 FAQ
→ 本地静态答案详情页
→ 用户需要进一步咨询时，再点击“继续追问”进入问答页
```

---

## 2. 目标

本阶段目标是：

```text
让首页高频 FAQ 点击后秒开本地答案详情页，
不再默认触发 RAG 请求。
```

具体目标：

1. 首页 FAQ 点击后进入本地 FAQ 答案详情页；
2. FAQ 答案内容存放在 Android 本地；
3. FAQ 详情页展示简短答案、办理材料、步骤、注意事项等内容；
4. 页面底部提供“继续追问”按钮；
5. 用户点击“继续追问”后，才进入 ChatScreen 并提交预设 follow-up 问题；
6. 保留问答页中的快捷提问 chips；
7. 不修改 Dify 配置；
8. 不修改 FastAPI 后端；
9. 不改变 RAG 缓存和 TTS 缓存逻辑。

---

## 3. 本阶段不做内容

不做以下内容：

- 不改 Dify 知识库；
- 不改 FastAPI 后端；
- 不新增后端 FAQ 接口；
- 不做 FAQ 后台管理；
- 不做远程 FAQ 更新；
- 不做 Dify Annotation Reply；
- 不做 streaming；
- 不把 FAQ 答案从 Dify 实时读取；
- 不删除问答页推荐问题；
- 不大改首页整体布局；
- 不改变底部 Tab 和导航结构；
- 不影响现有 ChatScreen、RAG、ASR、TTS 主链路。

---

## 4. 功能定位

### 4.1 首页 FAQ 的定位

首页 FAQ 应该是：

```text
高频固定问题的快速答案入口。
```

它负责：

- 快速展示稳定答案；
- 降低老人等待时间；
- 减少不必要的 RAG 请求；
- 让首页更像“办事助手入口”，而不是“所有问题都丢给 AI”。

点击首页 FAQ 后：

```text
不直接提交 RAG。
不直接跳转 ChatScreen。
先进入 FAQDetailScreen。
```

---

### 4.2 问答页快捷问题的定位

问答页里的简易问题仍然保留。

它的定位是：

```text
帮助用户在问答场景中快速开口。
```

问答页快捷问题可以继续：

```text
点击后直接提交到 chatPolicy，
走 RAG 或 RAG cache。
```

因此两类入口区分如下：

| 位置                 | 点击后行为                      | 是否默认走 RAG | 作用           |
| -------------------- | ------------------------------- | -------------- | -------------- |
| 首页 FAQ             | 打开本地 FAQ 详情页             | 否             | 高频答案秒开   |
| FAQ 详情页“继续追问” | 进入问答页并提交 follow-up 问题 | 是             | 处理个性化问题 |
| 问答页快捷问题       | 直接提交提问                    | 是             | 降低输入成本   |
| 服务页入口           | 展示服务或 Toast                | 不一定         | 功能导航       |

---

## 5. 数据存放方案

### 5.1 当前阶段推荐：Android 本地存储

FAQ 答案内容建议放在 Android 本地，例如扩展现有：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/FaqData.kt
```

原因：

1. 首页 FAQ 需要秒开；
2. 当前项目仍处于 Demo / 课程项目阶段；
3. 本地数据实现简单、稳定、可控；
4. 不需要新增后端接口；
5. 不依赖 Dify、网络和模型生成；
6. 更适合展示高频固定答案。

---

### 5.2 Dify 知识库的作用

Dify 知识库仍然保留，但它不是 FAQ 详情页的数据存放位置。

分工如下：

```text
Android 本地 FAQ：
用于首页高频问题秒开。

Dify 知识库：
用于继续追问、复杂情况、非固定问题、材料判断。
```

也就是说：

```text
FAQ 答案来源可以参考 Dify 知识库和官方资料，
但最终展示内容应固化到 Android 本地。
```

---

## 6. 数据结构设计

建议将 FAQ 数据从“只有问题”扩展为“问题 + 答案详情”。

示例 Kotlin 数据结构：

```kotlin
data class FaqItem(
    val id: String,
    val category: String,
    val question: String,
    val shortAnswer: String,
    val sections: List<FaqSection>,
    val tips: List<String> = emptyList(),
    val sourceNote: String = "请以当地出入境窗口或官方平台最新要求为准。",
    val updatedAt: String = "2026-06",
    val followUpQuestion: String
)

data class FaqSection(
    val title: String,
    val content: String
)
```

字段说明：

| 字段             | 含义                                     |
| ---------------- | ---------------------------------------- |
| id               | FAQ 唯一标识                             |
| category         | 分类，例如“港澳通行证”“续签”“过关材料”   |
| question         | 首页展示的问题标题                       |
| shortAnswer      | 详情页顶部简短结论                       |
| sections         | 详情内容分段                             |
| tips             | 注意事项                                 |
| sourceNote       | 来源和时效提醒                           |
| updatedAt        | 本地 FAQ 内容更新时间                    |
| followUpQuestion | 点击“继续追问”后提交给 ChatScreen 的问题 |

---

## 7. FAQ 内容建议

第一版不需要很多，建议先做 4–6 个高频问题。

### 7.1 推荐 FAQ 列表

建议包含：

1. 港澳通行证续签需要什么材料？
2. 首次办理港澳通行证必须去窗口吗？
3. 自助机可以办理续签吗？
4. 过关前需要带什么证件？
5. 老人不会网上预约怎么办？
6. 港澳通行证快过期了怎么办？

---

### 7.2 示例 FAQ 内容

#### 示例 1：港澳通行证续签需要什么材料？

```text
shortAnswer:
一般需要本人有效身份证件、有效往来港澳通行证，并根据签注类型准备相关材料。具体要求请以当地出入境窗口或官方平台为准。

sections:
1. 常见材料
- 本人有效身份证件；
- 有效往来港澳通行证；
- 根据办理类型可能需要补充相关证明材料。

2. 办理方式
- 部分地区支持自助机办理续签；
- 首次办理或特殊情况通常需要到窗口办理；
- 老年人不熟悉线上预约时，可以优先咨询窗口或现场工作人员。

3. 下一步建议
- 如果不确定自己属于首次办理、续签、补办还是换证，可以点击“继续追问”，让助手根据你的情况判断。

tips:
- 不同地区要求可能略有差异；
- 出发前建议确认通行证有效期和签注有效期；
- 现场办理时建议携带身份证和原通行证。
```

---

#### 示例 2：首次办理港澳通行证必须去窗口吗？

```text
shortAnswer:
首次办理通常需要本人到出入境窗口办理，因为需要进行身份核验、拍照或采集相关信息。

sections:
1. 为什么需要线下办理
首次办理涉及身份确认、证件申请和资料采集，一般不能完全在线完成。

2. 适合提前准备的事项
- 携带本人有效身份证件；
- 了解当地出入境窗口位置；
- 查看是否需要提前预约；
- 老年人可由家属陪同前往。

3. 后续签注
首次办好证件后，部分续签业务可能可以通过自助机或线上渠道办理，具体以当地规定为准。

tips:
- 如果老人不会预约，可以到现场咨询工作人员；
- 不同城市的窗口要求可能不同。
```

---

## 8. 页面交互设计

### 8.1 首页 FAQ 点击前

当前可能是：

```text
点击 FAQ
→ submitPrefilledQuestion()
→ 切到 ChatScreen
→ 自动发送问题
```

后改为：

```text
点击 FAQ
→ 打开 FaqDetailScreen
→ 展示本地静态答案
```

---

### 8.2 FAQ 详情页结构

页面建议：

```text
顶部栏
- 返回按钮
- 标题：常见问题

内容区
- 问题标题
- 简短结论卡片
- 分段答案卡片
- 注意事项卡片
- 更新时间 / 来源提醒

底部操作区
- 继续追问
```

---

### 8.3 继续追问按钮

点击“继续追问”后：

```text
进入 ChatScreen
自动提交 followUpQuestion
```

例如：

```text
请根据我的情况说明港澳通行证续签需要准备哪些材料。
```

或者：

```text
我不确定自己属于首次办理还是续签，请一步步帮我判断。
```

---

## 9. UI 风格要求

必须沿用当前项目风格：

- 政务蓝主色；
- 白底圆角卡片；
- 大字号；
- 适老化间距；
- 简短说明；
- 不使用复杂大图 Banner；
- 不新增独立底部 Tab；
- 不改变现有首页整体结构；
- 不改变问答页主链路。

建议 FAQ 详情页使用：

```text
白色背景
浅蓝提示卡片
圆角信息块
清晰标题层级
底部固定或靠下的“继续追问”按钮
```

---

## 10. 涉及文件范围

### 10.1 可能修改文件

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/FaqData.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt
```

说明：

- `FaqData.kt`：扩展 FAQ 数据结构和本地答案内容；
- `ElderCareScreens.kt`：新增或补充 FAQ 详情页 UI，修改首页 FAQ 点击行为；
- `ChatViewModel.kt`：如已有 `submitPrefilledQuestion` 可继续复用，不一定要改。

### 10.2 不应修改文件

```text
backend/app/api/routes_chat.py
backend/app/services/dify_service.py
backend/app/services/rag_cache_service.py
backend/app/services/tts_service.py
Dify 配置
```

---

## 11. 最小实现方案

### Step 1：扩展 FAQ 数据结构

在 `FaqData.kt` 中将 FAQ 从问题列表扩展为带答案的结构。

保留原有首页可用的 `faqItems` 或兼容字段，避免大范围修改。

---

### Step 2：新增 FAQ 详情页状态

如果当前项目没有独立导航，可以在现有 screen state 中增加：

```text
FaqDetail(faqId)
```

或者使用当前已有页面切换方式实现。

要求：

```text
点击首页 FAQ 后打开详情页，
不要直接提交问题。
```

---

### Step 3：实现 FaqDetailScreen

新增 composable：

```kotlin
@Composable
private fun FaqDetailScreen(
    faqItem: FaqItem,
    onBack: () -> Unit,
    onContinueAsk: (String) -> Unit
)
```

页面显示：

- 返回按钮；
- 问题标题；
- shortAnswer；
- sections；
- tips；
- sourceNote；
- updatedAt；
- 继续追问按钮。

---

### Step 4：修改首页 FAQ 点击行为

将当前：

```text
submitPrefilledQuestion(faq.question)
```

改为：

```text
openFaqDetail(faq.id)
```

---

### Step 5：继续追问接入 Chat

在 FAQ 详情页点击：

```text
继续追问
```

执行：

```text
切换到 ChatScreen
submitPrefilledQuestion(faq.followUpQuestion)
```

这个行为可以复用现有问答页逻辑。

---

## 12. 问答页快捷问题是否保留

保留。

但建议稍微调整内容，让它们与首页 FAQ 区分开。

### 首页 FAQ 更适合放

```text
港澳通行证续签需要什么材料？
首次办理港澳通行证必须去窗口吗？
自助机可以办理续签吗？
过关前需要带什么证件？
```

### 问答页快捷问题更适合放

```text
我不知道自己该怎么办
请根据我的情况帮我判断办理方式
老人不会预约怎么办？
我的港澳通行证快过期了，下一步怎么做？
```

这样首页负责“固定答案秒开”，问答页负责“智能对话引导”。

---

## 13. 验证标准

### 13.1 首页 FAQ 点击验证

点击首页 FAQ 后，预期：

```text
进入 FAQ 详情页
不进入 ChatScreen
不自动提交问题
不调用 chatPolicy
不触发 RAG
```

---

### 13.2 后端日志验证

点击首页 FAQ 后，后端不应该出现：

```text
/chat-policy
dify_service.send_chat_message
qwen_text_service.rewrite_query
cache_hit=false
cache_hit=true
```

因为它不应该请求后端。

---

### 13.3 继续追问验证

在 FAQ 详情页点击“继续追问”后，预期：

```text
进入 ChatScreen
自动提交 followUpQuestion
调用 chatPolicy
走 RAG 或 RAG cache
```

---

### 13.4 问答页快捷问题验证

问答页中的快捷问题仍应保留，并且点击后仍能：

```text
直接提交问题
显示 loading
返回 RAG 答案
```

---

## 14. Codex 执行 Prompt

请 Codex 按以下要求执行：

```text
请实现 ：首页 FAQ 本地静态答案详情页。要求最小修改，不要改后端，不要改 Dify，不要改 RAG/TTS 缓存逻辑。

背景：
当前首页 FAQ 是本地静态问题列表，但点击后会直接跳转问答页并提交 RAG 问题。 目标是让首页 FAQ 点击后先打开本地答案详情页，做到高频问题秒开。用户只有点击“继续追问”时，才进入问答页并提交问题。

要求：
1. 扩展 FaqData.kt，使 FAQ 数据包含：
   - id
   - category
   - question
   - shortAnswer
   - sections
   - tips
   - sourceNote
   - updatedAt
   - followUpQuestion

2. 新增或补充 FAQ 详情页 UI：
   - 顶部返回
   - 问题标题
   - 简短答案卡片
   - 分段答案内容
   - 注意事项
   - 更新时间 / 来源提醒
   - “继续追问”按钮

3. 修改首页 FAQ 点击行为：
   - 不再直接调用 submitPrefilledQuestion
   - 不再直接跳转 ChatScreen
   - 改为打开 FAQ 详情页

4. “继续追问”按钮行为：
   - 点击后进入 ChatScreen
   - 自动提交 faq.followUpQuestion
   - 复用现有 submitPrefilledQuestion 或等价逻辑

5. 保留问答页里的快捷提问 chips：
   - 问答页快捷问题仍然可以直接提交 RAG
   - 不要删除

6. UI 风格：
   - 沿用当前项目政务蓝主色
   - 白底圆角卡片
   - 大字号
   - 适老化间距
   - 不新增底部 Tab
   - 不改变首页整体结构

7. 不要做：
   - 不改 FastAPI 后端
   - 不改 Dify
   - 不改 RAG cache
   - 不改 TTS cache
   - 不做 streaming
   - 不新增网络接口
   - 不引入复杂导航框架

完成后请输出：
1. 修改文件列表
2. 新增文件列表
3. 首页 FAQ 点击前后的行为变化
4. FAQ 详情页展示内容说明
5. “继续追问”如何接入 ChatScreen
6. 如何验证点击 FAQ 不再触发 chatPolicy
```

---

## 15. 预期效果

改造前：

```text
首页 FAQ
→ 点击
→ 进入问答页
→ 自动提交问题
→ 等待 RAG
```

改造后：

```text
首页 FAQ
→ 点击
→ 本地 FAQ 详情页秒开
→ 用户阅读固定答案
→ 如需个性化咨询，点击“继续追问”
→ 进入问答页并提交 followUpQuestion
```

---

## 16. 本阶段结论

的核心不是提升模型能力，而是优化首页入口体验。

一句话概括：

```text
首页 FAQ 不再是“快捷提问按钮”，而是“高频答案详情入口”。
```

这样可以让首页承担“快速查看”的职责，让问答页承担“智能追问”的职责。

最终结构应为：

```text
首页负责快；
FAQ 详情页负责解释；
问答页负责个性化智能回答。
```
