# App Design System v2

## 1. 改版目标

本设计系统用于后续 HomeScreen、ChatScreen、ServiceScreen 和 MyScreen 的 UI 统一改版。它只定义展示层规范，不改变现有功能、不改变现有导航、不改动 ViewModel、不改动后端接口和 RAG/ASR/TTS/i18n 逻辑。

- 统一首页、问答页、服务页、我的页的视觉语言，让它们看起来属于同一个适老化政务服务 App。
- 保持适老化：大字号、清晰层级、足够触控面积、明确反馈、低学习成本。
- 保持政务蓝主色，沿用当前 `ElderBlue` 为主色，不创建完全不同的主题。
- 保持当前 `MainTab + OverlayScreen` 导航方式，不建议改成 `NavHost`，不新增底部 Tab。
- 保持现有功能边界，不把 Toast 占位包装成完整功能。
- 不进行大范围重构，优先小步整理展示组件，再逐页改版。
- 参考政务服务 App 图片时，只借鉴大圆角卡片、柔和色块、分类感、图标入口；不照搬状态栏、底部导航、大型 Banner。

## 2. 页面角色定义

### HomeScreen：首页 / 快速开始页

HomeScreen 是快速入口页，不是服务大厅。它的核心任务是让用户一进入 App 就能快速开始：提问、查看少量常用入口、进入常见问题。

- 主要任务：让用户快速提问、查看常用服务、进入常见问题。
- 不做服务大厅，不承载完整服务分类。
- 不堆太多功能，避免首页信息过载。
- 入口优先级应为：问一问办事助手、操作指南、少量常用服务、FAQ、材料清单。
- 人工帮助、视频讲解等未接入入口应弱化或标注“暂未开放”，不要设计成完整可用功能。

### ChatScreen：AI 问答工作台

ChatScreen 是 AI 办事问答工作台。它承载文字提问、语音提问、结构化回答、朗读、历史记录，和 RAG/ASR/TTS/历史存储绑定较深。

- 主要任务：文字/语音提问、展示结构化回答、朗读回答、查看历史。
- 保持现有 `ChatViewModel` 和 API 调用，不改变请求字段、响应模型、历史记录结构。
- 改版重点只放在展示层：回答卡片、输入栏、语音确认卡、错误/Loading 状态。
- 语音确认、ASR 上传、TTS 播放、历史恢复/重问流程必须保持。
- 不隐藏关键错误提示，不把后端失败、语音识别失败、无知识库答案等状态做成静默失败。

### ServiceScreen：服务大厅 / 服务聚合页

ServiceScreen 才是服务入口聚合页。它负责集中展示当前 App 的服务入口，并区分已接入、半接入、占位功能。

- 主要任务：集中展示服务入口。
- 可借鉴政务服务 App 的大圆角卡片、柔和色块、分类入口、图标宫格。
- 未接入功能要明确显示为占位或 Toast，不要包装成完整功能。
- 已接入跳转保持不变：过关材料进入 `CrossBorderPreparePickerScreen`，办理判断进入 `GuidanceScreen`，养老服务进入 `ElderCareServiceScreen`。
- 不新增底部 Tab，不改变 Service 与 Home 的角色分工。

### MyScreen：个人与设置页

MyScreen 是个人与设置页，目前功能较轻，主要展示欢迎卡和已保存材料清单。

- 主要任务：保存材料、字体/语言设置、历史记录等个人相关能力。
- 当前只适合小步增强，不应一次性塞入复杂账户体系。
- 后续可以逐步承载字体设置、语言设置、问答历史入口、保存材料清单等。
- 已保存材料清单当前来自 `MaterialViewModel` 内存状态，未持久化前不应设计成强账户资产。

## 3. 统一视觉规范

本项目已有一套适老化 UI 基础：`ElderBlue`、`ElderGreen`、`ElderOrange`、`ElderRed`、`ElderResponsiveSpec`、`SoftCard`、`UnifiedTopBar`、`PrimaryActionButton`、`SecondaryActionButton` 等。v2 应优先沿用和收敛这些体系，而不是另起一套主题。

### 主色

- 主色：沿用 `ElderBlue`，用于主按钮、选中态、重点图标、标题强调条。
- 深蓝：沿用 `ElderBlueDark`，用于重要正文、提示强调、信息标题。
- 浅蓝：沿用 `ElderBlueSoft` / `ElderBluePale`，用于大入口卡背景、图标容器、选中底色。
- 不新增大面积紫色、渐变蓝紫、强饱和装饰色。

### 背景色

- 页面背景沿用 `ElderBackground`，保持浅色、干净、低负担。
- 内容区不使用复杂渐变和大装饰图形。
- 页面主体通过卡片和分区建立层级，而不是依赖大 Banner。

### 卡片颜色

- 默认卡片：沿用 `ElderCard` / 白色。
- 重点卡片：使用 `ElderBlueSoft` 或 `ElderBluePale`。
- 警示卡片：使用浅橙底，如当前 `ElderOrangeSoft`。
- 成功/已完成：使用 `ElderGreenSoft`。
- 错误：使用浅红底或红色边框，但保持克制。

### 状态色

- 正常/可用：`ElderGreen`。
- 重要提醒/必备材料：`ElderOrange`。
- 错误/失败/不可用：`ElderRed`。
- 进行中/可操作/选中：`ElderBlue`。
- 占位/未开放：建议使用中性灰蓝，不使用成功色，不给用户“已可用”的暗示。

### 字号层级

优先使用 `ElderResponsiveSpec` 中已有字号：

- 顶部标题：`topBarTitle`。
- 顶部副标题：`topBarSubtitle`。
- 分区标题：`sectionTitle`。
- 卡片标题：`cardTitle`。
- 正文：`body`。
- 重点正文/按钮文字：`bodyLarge`。
- 辅助说明：`label` / `labelSmall`。

避免新增散落的硬编码字号。局部已有的 `19.sp`、`28.sp` 等后续应逐步收敛到响应式规格。

### 圆角

- 卡片圆角：优先使用 `responsive.cardCorner`。
- 按钮、Chip、输入框圆角：优先使用 `responsive.controlCorner`。
- 图标容器：圆形为主，延续当前 `IconBadge`。
- 不继续扩散新的 `8.dp`、`14.dp`、`22.dp`、`28.dp` 魔法值；确需特殊值时先纳入设计 token。

### 间距

优先使用 `ElderResponsiveSpec`：

- 页面边距：`pagePadding`。
- 页面区块间距：`pageSpacing`。
- 卡片内边距：`cardPadding`。
- 卡片内部元素间距：`cardSpacing`。
- 行内元素间距：`rowSpacing`。
- 小间距：`smallSpacing`。

ChatScreen 的底部输入栏预留、语音面板和滚动区间距可以单独优化，但不要影响 RAG/ASR/TTS 流程。

### 图标容器

- 统一使用圆形或柔和圆角浅色容器承载图标。
- 入口卡图标大小优先使用 `iconMedium` / `iconLarge`。
- 顶部栏和小操作图标优先使用 `iconSmall`。
- 图标语义应更准确：问答可使用问答/消息类图标，材料用清单类图标，养老用护理/家庭/服务类图标。
- 如果仍使用 Material Icons，应避免多个不同功能都使用同一个图标。

### 按钮高度

- 主按钮：使用 `buttonMinHeight` 或现有 `PrimaryActionButton` 默认高度。
- 次按钮：使用 `compactButtonMinHeight` 或现有 `SecondaryActionButton` 默认高度。
- 输入栏按钮：保持足够触控面积，最低不小于当前 `inputMinHeight`。
- 大字模式下按钮应自动增高，不压缩文字。

### 适老化规则

- 所有主要操作应有明确文字，不只给图标。
- 每个可点击区域要足够大，优先使用现有按钮高度。
- 不使用过小、过灰、对比不足的辅助文案。
- 不让长文字挤压按钮和图标；必要时换行。
- 大字模式下服务卡从网格转为列表，沿用当前响应式思路。
- 页面滚动自然，不为了视觉完整强行固定高度。

## 4. 统一组件规范

### UnifiedTopBar / AppPageHeader

- 用在哪些页面：Home、Chat、Service、My、PortDetail、Guide、Guidance、MaterialList、ElderCareService。
- 应该包含的参数：`title`、`subtitle`、`showBack`、`onBack`、`actions`、`leadingIcon`、`elevated`、`gradient`。
- 设计规范：
  - 标题不应被英文或过长文案截断到不可识别。
  - Home/Service/My 可保留轻微浅蓝渐变或浅底；Chat/详情页更适合白底加边线。
  - 顶部 action 在窄屏或大字模式可只显示图标，但 `contentDescription` 必须清晰。
- 暂时不要抽取的场景：
  - 不要为了改顶部栏而改变根导航结构。
  - 不要新增底部 Tab 或改 `MainTab`。

### LargeActionCard

- 用在哪些页面：Home 主问答入口、Service 办理判断、CrossBorderPreparePicker 说明、ElderCareService 问 AI。
- 应该包含的参数：`title`、`subtitle`、`icon`、`primaryActionText`、`primaryActionIcon`、`onPrimaryClick`、可选 `secondaryAction`、`tone`。
- 设计规范：
  - 用于页面主任务，不用于普通列表项。
  - 背景使用柔和浅蓝或白底，图标使用大圆形容器。
  - 主按钮只能承载真实可执行的主路径。
- 暂时不要抽取的场景：
  - Chat 的结构化回答卡不归入 LargeActionCard。
  - Toast 占位入口不应被设计成强主按钮。

### ServiceEntryCard

- 用在哪些页面：ServiceScreen、CrossBorderPreparePickerScreen、PortDetailScreen 相关服务、未来 MyScreen 设置入口。
- 应该包含的参数：`title`、`subtitle`、`icon`、`onClick`、`enabled`、`status`、`layoutMode`、`tone`。
- 设计规范：
  - 支持列表和网格两种模式，沿用当前 `CardLayoutMode`。
  - 已接入服务使用正常主色；未开放服务使用弱化样式或状态标签。
  - 服务卡要表达“入口”，不要包含太长说明。
- 暂时不要抽取的场景：
  - 养老机构详情卡不应强行套用 ServiceEntryCard。
  - 材料核对行不属于服务入口。

### NoticeCard(type)

- 用在哪些页面：Home、Service、PortDetail、MaterialList、FontSize、Chat 错误/提示区域。
- 应该包含的参数：`text`、`type`、`icon`、`actionText`、`onActionClick`。
- 设计规范：
  - `info`：浅蓝底，普通说明。
  - `warning`：浅橙底，办理前提醒。
  - `success`：浅绿底，保存成功/已完成。
  - `error`：浅红边框或浅红底，失败提示。
  - 未开放功能可使用 `disabled` 或 `comingSoon` 类型，避免用成功色。
- 暂时不要抽取的场景：
  - 长结构化回答不应塞进 NoticeCard，应使用 AnswerSectionCard。

### StatusPill(type)

- 用在哪些页面：PortDetail 状态、材料必带/可选、养老机构标签、服务未开放状态。
- 应该包含的参数：`text`、`type`、`size`、可选 `icon`。
- 设计规范：
  - `normal` 使用绿色。
  - `busy/warning/required` 使用橙色。
  - `closed/error` 使用红色。
  - `tag/info` 使用蓝色或灰蓝。
  - `comingSoon` 使用灰蓝，不要使用绿色。
- 暂时不要抽取的场景：
  - 不要让 StatusPill 替代按钮。

### SectionTitle

- 用在哪些页面：Home、Service、PortDetail、MaterialList、ElderCareService、Chat 回答分区。
- 应该包含的参数：`text`、`icon`、`showAccentBar`、可选 `actionText`。
- 设计规范：
  - 分区标题用于帮助用户扫描页面结构。
  - 首页标题数量要少，避免像服务大厅。
  - Chat 回答区标题要短：结论、材料、步骤、提醒、来源。
- 暂时不要抽取的场景：
  - 顶部页面标题不使用 SectionTitle。

### EmptyStateCard

- 用在哪些页面：Chat 空状态、Chat 历史空状态、My 保存材料为空、MaterialList 加载失败或空列表。
- 应该包含的参数：`icon`、`title`、`description`、`primaryActionText`、`onPrimaryAction`、可选 `examples`。
- 设计规范：
  - 空状态要告诉用户下一步可以做什么。
  - 不要放大量说明文字。
  - Chat 空状态可保留示例问题，但示例问题提交文本需注意 i18n 风险。
- 暂时不要抽取的场景：
  - Guidance 问卷结果不是空状态。

### AnswerSectionCard

- 用在哪些页面：ChatScreen 的结构化 AI 回答。
- 应该包含的参数：`title`、`items`、`body`、`type`、`expandable`、`source`。
- 设计规范：
  - 回答卡按“结论、材料、步骤、提醒、来源”分区。
  - 长内容自然滚动，不固定高度。
  - 材料分区要清楚区分必备和可能需要。
  - 来源和提醒不能隐藏，避免用户误以为 AI 答案就是最终官方结论。
- 暂时不要抽取的场景：
  - 在不稳定结构化回答模型前，不要重写 `QaAnswerUiModel` 或 `ChatModels`。

### VoiceInputBar

- 用在哪些页面：ChatScreen 底部输入栏、语音面板、语音确认卡。
- 应该包含的参数：`value`、`onValueChange`、`enabled`、`isRecording`、`isTranscribing`、`onVoiceClick`、`onSendClick`、`placeholder`、`errorText`。
- 设计规范：
  - 文字输入、语音输入、发送按钮要保持当前流程。
  - 录音后必须保留语音确认卡，不能识别后自动发送。
  - 错误提示要清楚展示，不隐藏语音失败原因。
  - Debug 音频样本选择只在 Debug 场景出现。
- 暂时不要抽取的场景：
  - 不要改 `VoiceRecorder` 生命周期。
  - 不要改 ASR/TTS 请求结构。

## 5. 首页 HomeScreen v2 设计方向

HomeScreen v2 应继续作为“快速开始页”，目标是减少用户犹豫，让用户最快进入问答或常用入口。

- 顶部标题避免英文省略，App 名称、朗读语言、字体设置都要在窄屏下保持可理解。
- 主入口突出“问一问办事助手”，作为首页第一优先级。
- 操作指南作为辅助入口，保持次级样式。
- 常用服务保留 2x2 或少量入口，不扩展成完整服务大厅。
- FAQ 保持简洁，默认只展示少量高频问题，分类不要压过主问答入口。
- 人工帮助 Toast 入口要弱化或标注“暂未开放”，不要使用强主按钮样式。
- 口岸概览只保留摘要能力，不在首页展开完整口岸服务。
- 我的材料清单可以保留为轻入口，但不应成为首页主视觉。
- 不要把 HomeScreen 变成 ServiceScreen；完整服务入口聚合应放在 ServiceScreen。

## 6. 问答页 ChatScreen v2 设计方向

ChatScreen v2 是 AI 办事问答工作台，改版时只能做展示层小步改版。它与 RAG/ASR/TTS/历史记录绑定较深，不动现有 `ChatViewModel`、模型和 API 调用。

- 顶部保留标题、历史入口、字体设置；如果未来从覆盖页进入，也应保留返回能力。
- 快捷问题保留，但样式应更轻，不要挤占消息区。
- AI 回答卡片按结论、材料、步骤、提醒、来源分区。
- 结论区应最醒目，但提醒和来源不能被隐藏。
- 长内容自然滚动，不固定高度，不做复杂折叠导致老人找不到信息。
- 输入栏发送后清空，沿用当前 `sendQuestion` 行为。
- 语音确认流程保持：录音 -> ASR -> 展示识别文本 -> 用户确认 -> 提交问答。
- 朗读按钮保留，正在准备/正在朗读/停止状态要明显。
- 不动 RAG/ASR/TTS/历史记录逻辑，不改请求字段。
- 不隐藏关键错误提示：空问题、查询失败、语音不清、权限失败、网络不稳定都要可见。
- 历史记录 BottomSheet 可优化视觉，但不改存储字段和恢复/重问/删除/清空行为。

## 7. 服务页 ServiceScreen v2 设计方向

ServiceScreen v2 是服务大厅 / 服务聚合页，可以比首页承载更多入口，但必须明确哪些是真功能、哪些是占位。

- 可以参考政务服务 App 的大圆角卡片、柔和色块、分类入口和图标入口。
- 可增加轻量分类 Tab 或分段控件，但不要新增底部 Tab。
- 分类应服务于扫描：通关办理、出行辅助、养老服务、其他服务。
- 未接入服务不要做成完整可用状态，应显示“暂未开放”或使用弱化样式，并保持 Toast 占位事实。
- 养老服务继续跳转 `ElderCareServiceScreen`。
- 过关材料继续跳转 `CrossBorderPreparePickerScreen`。
- 办理判断继续跳转 `GuidanceScreen`。
- 服务入口组件应支持状态：`available`、`comingSoon`、`toastOnly`。
- 不要把 ServiceScreen 的未接入服务包装成后端数据或真实办理流程。

## 8. 不允许改动范围

后续 UI 改版不得改动以下范围，除非另开明确的业务改造任务：

- `ChatViewModel.kt`
- `ChatModels.kt`
- `ApiClient.kt`
- `AssistantApi.kt`
- `VoiceRecorder.kt`
- `AppLanguageResolver.kt`
- `MaterialViewModel.kt`
- 现有 `MainTab / OverlayScreen` 导航逻辑
- 后端接口路径
- RAG / ASR / TTS 请求结构
- 历史记录存储字段
- 底部 Tab 数量和基本含义
- `ChatHistoryStorage` 中历史记录字段含义
- 材料清单 code 与当前跳转关系

明确禁止：

- 不建议改成 `NavHost`。
- 不新增底部 Tab。
- 不改变 HomeScreen、ChatScreen、ServiceScreen、MyScreen 的主角色。
- 不在 UI 改版中顺手改后端接口、RAG 结构、ASR/TTS 参数。

## 9. 后续实施顺序

1. 只生成 `design-system-v2.md`
   - 当前阶段只沉淀规范，不改代码。
2. 再整理少量复用组件
   - 优先 `LargeActionCard`、`ServiceEntryCard`、`NoticeCard(type)`、`StatusPill(type)`、`EmptyStateCard`。
3. 先改 HomeScreen
   - 首页风险较低，可先验证统一视觉语言。
4. 再改 ChatScreen
   - 只做展示层小步改版，保持 RAG/ASR/TTS/历史记录逻辑。
5. 最后改 ServiceScreen
   - 在服务项状态明确后再统一入口卡和分类。
6. 每一步完成后截图对比，不连续大改
   - 每次只改一页或一组低风险组件，避免视觉与业务逻辑同时变化。

## 10. 风险提示

- ChatScreen 风险最高
  - 它同时绑定 RAG、ASR、TTS、历史记录、结构化回答和材料入口，后续只能做展示层小步改版。
- ServiceScreen 占位最多
  - 很多服务入口当前只是 Toast，不能在视觉上包装成完整可用功能。
- Guide/Guidance 硬编码中文较多
  - 后续做多语言或繁体/英文适配前，应先迁移文案或至少建立文案清单。
- FAQ query 与显示文案分离存在 i18n 风险
  - 标题可能来自 `strings.xml`，但提交给 RAG 的 query 是硬编码中文，后续不能只改显示文本。
- 材料清单仍是 Mock MVP
  - `AssistantApi` 已定义材料接口，但当前 `MaterialViewModel` 使用本地 Mock 清单；UI 不应暗示材料来自实时官方后端。
- HomeScreen 容易功能堆叠
  - 首页应保持快速开始，不要变成服务大厅。
- 视觉统一不能覆盖功能状态
  - 卡片、按钮、标签再统一，也必须明确真实跳转、Toast 占位、Mock 数据和后端数据边界。
