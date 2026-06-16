# 当前 UI 页面与组件盘点

## 1. 页面总览

| 页面名称 | 对应文件 | 当前功能 | 数据来源 | 跳转关系 | 完成状态 | 备注 |
| --- | --- | --- | --- | --- | --- | --- |
| ElderCareAppRoot | `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt` | 应用根容器、底部 Tab、覆盖页状态管理、字体缩放、朗读语言选择 | 本地 Compose state、`ChatViewModel`、`MaterialViewModel`、系统 Locale | `MainTab` 切换 Home/Chat/Service/My；`OverlayScreen` 打开详情、指南、材料清单、字体设置等 | 已实现本地导航骨架 | 未使用 `NavHost`，当前跳转是状态切换；改版时要保留返回逻辑 |
| HomeScreen | `ElderCareScreens.kt` | 首页入口、口岸概览、FAQ、材料清单入口、语音朗读偏好入口 | 静态 `ports`、`FaqData.kt`、`strings.xml`、本地 state | 可进入 Chat、Guide、PortDetail、MaterialList、FontSize；FAQ 会提交问题并切到 Chat | 主体已实现，部分入口占位 | 客服/志愿者/视频为 Toast 占位；口岸数据为演示数据 |
| ChatScreen | `ElderCareScreens.kt` + `ChatViewModel.kt` | 智能问答、结构化回答、语音输入、朗读、历史记录 | 后端 `chat-policy` RAG、`asr/transcribe`、`tts/synthesize`、本地 SharedPreferences 历史 | 顶部历史 BottomSheet；快捷问题可发起问答；不确定入口到 Guidance；材料入口到 MaterialList | 核心功能已接入 | 与 RAG/ASR/TTS/i18n 绑定最深，UI 改动需谨慎 |
| ServiceScreen | `ElderCareScreens.kt` | 服务入口聚合、办理判断、过关材料、养老服务入口 | 本地 `ServiceItem` 静态列表、`strings.xml` | 可进入 Guidance、CrossBorderPreparePicker、ElderCareService；其余服务 Toast | 半成品入口页 | 多个入口只是 Toast，不能当作完整服务 |
| MyScreen | `ElderCareScreens.kt` | 我的页、欢迎卡、已保存材料清单 | `MaterialViewModel.savedChecklists` 内存状态 | 点击已保存清单进入 MaterialChecklist | 轻量已实现 | 保存清单未持久化，退出进程后可能丢失 |
| PortDetailScreen | `ElderCareScreens.kt` | 深圳湾口岸详情、提醒、过关步骤、相关服务 | 静态口岸演示数据、`strings.xml` | 返回；问 AI 切 Chat；查看材料进材料清单；导航/家人帮助 Toast | Demo 详情页 | 只有深圳湾固定详情，非真实口岸数据 |
| GuideScreen | `ElderCareScreens.kt` | 操作指南分步卡片、上一步/下一步/关闭、朗读本步骤 | 硬编码 `GuideStep`、TTS 后端 | 返回/完成关闭覆盖页；朗读调用 TTS | 已实现引导壳 | 文案大量硬编码中文，需迁移到 `strings.xml` |
| ElderCareServiceScreen | `ElderCareScreens.kt` | 养老服务类型、精选机构、问 AI 入口 | 本地静态服务类型和机构数据、`strings.xml` | 返回；查看机构详情；地图/服务类型 Toast；问 AI 切 Chat | 静态原型 | 数据标注“公开信息”，但当前不是后端数据 |
| ElderCareInstitutionDetailScreen | `ElderCareScreens.kt` | 养老机构详情、适合人群、服务内容、来源、常见问题 | 本地静态机构数据、`strings.xml` | 返回 ElderCareService；来源/地图/问题 Toast；问 AI 切 Chat | 静态原型 | 属于养老服务详情子页，后续可并入养老服务信息架构 |
| CrossBorderPreparePickerScreen | `ElderCareScreens.kt` | 过关材料场景选择 | 本地 `ServiceItem` 静态配置、材料 Mock 清单 | 进入不同 MaterialChecklist；不清楚则进入 Guidance | 已实现入口 | 是材料清单页前置选择器 |
| GuidanceScreen | `ElderCareScreens.kt` | 港澳办理情况问卷判断、结果卡、朗读问题 | 本地规则 `buildGuidanceResult`、TTS 后端、硬编码中文 | 可打开详细政策到 Chat；可打开对应材料清单；可重置 | 已实现本地规则版 | 规则和文案硬编码较多，与 Chat/RAG 有衔接 |
| FontSizeScreen | `ElderCareScreens.kt` | 字体大小选择 | 本地 `fontChoices` 和 Compose density state | 返回根页面，影响全局 fontScale | 已实现 | 标题、说明、提示硬编码中文 |
| MaterialListScreen / MaterialChecklistScreen | `ElderCareScreens.kt` + `MaterialViewModel.kt` | 材料清单列表、材料核对、保存/更新清单 | `MaterialViewModel` 本地 Mock 清单；未调用 `AssistantApi` 材料接口 | 返回；打开具体清单；关联清单跳转 | Mock MVP | API 接口已定义但当前 ViewModel 使用 Mock |
| ChatHistorySheet | `ElderCareScreens.kt` + `ChatHistoryStorage.kt` | 问答历史 BottomSheet，恢复/重问/删除/清空 | SharedPreferences `eldercare_chat_history`，最多 30 条 | 从 Chat 顶部历史按钮打开 | 已实现本地历史 | 属于 Chat 的附属页面/面板 |

## 2. 页面详情

### HomeScreen

- 主要区块
  - 顶部 `UnifiedTopBar`：应用名、朗读语言偏好、字体设置。
  - Hero 问答入口卡：点击提问、操作指南。
  - 人工帮助卡：联系客服、志愿者协助、视频讲解。
  - 口岸筛选与口岸概览：地区/方向筛选、常用口岸卡、查看全部口岸。
  - FAQ 区：分类筛选和常见问题列表。
  - 我的材料清单入口。
- 主要组件
  - `UnifiedTopBar`、`SoftCard`、`IconBadge`、`PrimaryActionButton`、`SecondaryActionButton`、`SegmentedControl`、`SectionTitle`、`PortSummaryCard`、`FaqSection`、`ActionCard`。
- 点击行为
  - “点击提问”切到 Chat。
  - “操作指南”打开 Guide 覆盖页。
  - 朗读语言按钮打开 `ModalBottomSheet`，修改 `speechPreference`。
  - 字体按钮打开 FontSize。
  - 口岸卡和“查看全部口岸”打开 PortDetail。
  - FAQ 问题调用 `chatViewModel.submitPrefilledQuestion` 并切到 Chat。
  - 我的材料清单打开 MaterialList。
  - 客服、志愿者、视频讲解均为 Toast 占位。
- 当前样式特点
  - 蓝色系为主，白底卡片，圆角较大，适老化字号和按钮高度。
  - 有响应式规则，窄屏/大字下操作按钮纵向堆叠。
- 存在问题
  - 口岸筛选 state 当前只影响选中状态，没有看到实际过滤 `ports` 的逻辑。
  - 人工帮助入口是 Toast，占位功能易被误解为已完成。
  - `FaqData.query`、快捷问题文本为硬编码中文，标题显示接入了 `strings.xml`。
  - 终端读取到部分文件/资源存在乱码显示风险，需运行时确认简体/繁体/英文显示是否正常。
- 后续建议
  - 首页先作为第一批改版页，统一大入口卡、服务入口卡、口岸摘要卡。
  - 将 FAQ query、Toast 文案、筛选 key 与显示文案分离，便于 i18n。
  - 明确口岸筛选是否要真实过滤，避免只做视觉选中。

### ChatScreen

- 主要区块
  - 顶部栏：标题、历史、字体设置。
  - 快捷问题 Chips。
  - 空状态示例问题。
  - 消息列表：用户气泡、结构化 AI 回答卡、普通回答卡。
  - Loading/Error/语音确认卡。
  - 语音输入面板。
  - 底部输入栏。
  - 历史记录 BottomSheet。
- 主要组件
  - `QaQuickQuestionChips`、`QaChip`、`QaEmptyState`、`UserBubble`、`AssistantStructuredAnswerCard`、`AssistantAnswerCard`、`QaConclusionBox`、`QaScenarioChips`、`QaInfoSection`、`QaMaterialSummarySection`、`QaAnswerActions`、`VoiceInputPanel`、`VoiceConfirmCard`、`VoiceErrorCard`、`ChatInputBar`、`ChatHistorySheet`。
- 点击行为
  - 快捷问题直接提交到后端 RAG；“不确定”打开 Guidance。
  - 示例问题提交到后端 RAG。
  - 结构化回答中的场景选项会再次提交标准问题。
  - “查看材料清单”打开 MaterialList，目前固定打开 `hk_macau_pass_apply`。
  - “朗读回答/停止朗读”调用 TTS 或播放已有 audio_url。
  - 语音按钮申请麦克风权限，录音后调用 ASR，先展示识别草稿，确认后提交问答。
  - 历史记录支持恢复、重新提问、删除、清空。
- RAG / ASR / TTS / 历史记录相关点
  - RAG：`ChatViewModel.sendQuestion` 调用 `POST api/v1/chat-policy`，请求包含 `message`、`conversation_id`、`input_type`、`language`、`tts_language`。
  - 结构化回答：使用 `structured_answer`、`steps`、`materials`、`warnings`、`sources` 转成 `QaAnswerUiModel`。
  - ASR：`VoiceRecorder` 录制 `.m4a` 到 cache，`ChatViewModel.transcribeVoice` 调用 `POST api/v1/asr/transcribe`。
  - TTS：`rememberCloudSpeechController` 优先播放回答返回的 `tts.audio_url`；无音频时调用 `POST api/v1/tts/synthesize`。
  - 历史记录：`ChatHistoryStorage` 使用 SharedPreferences 保存最多 30 条，包含消息、结构化回答、tts 文本/音频 URL。
- 当前样式特点
  - Chat 使用居中最大宽度，底部输入栏固定，内容区底部预留较大 padding。
  - AI 回答已拆成结论、步骤、材料、注意事项、来源和操作按钮。
  - 语音确认卡、错误卡、Loading 卡风格接近，但仍在页面文件内。
- 存在问题
  - `qaQuickQuestions`、`qaEmptyExamples` 的 label 接入 strings，但实际提交 question 是硬编码中文。
  - `ChatUiStrings` 有默认硬编码中文，虽然屏幕层会用 `stringResource` 覆盖，默认值仍是 i18n 风险。
  - `selectedVoiceLanguage` 使用旧的本地 label/code 辅助逻辑，实际 ASR 当前传 `auto`，debug 控件与真实逻辑关系不够清晰。
  - “继续追问”目前是 Toast，不是焦点移动或输入态引导。
- 后续建议
  - Chat 改版应晚于 Home，先稳定回答卡、输入栏、语音面板三个复用组件。
  - 保留 ViewModel 调用路径和历史存储字段，优先只换 Compose 结构/样式。
  - 将快捷问题和示例问题迁入资源或配置层，避免多语言显示与提交语义不一致。

### ServiceScreen

- 主要区块
  - 顶部栏。
  - 办理情况判断提示卡。
  - 出发/到达服务网格。
  - 其他服务网格。
  - 未开放说明卡。
- 主要组件
  - `UnifiedTopBar`、`SoftCard`、`PrimaryActionButton`、`ServiceGrid`、`ActionCard`、`SectionTitle`、`NoticeCard`。
- 点击行为
  - 办理判断打开 Guidance。
  - 过关材料准备打开 CrossBorderPreparePicker。
  - 养老服务打开 ElderCareService。
  - 通关流程、预约停车、交通出行、特殊人群预约、志愿者呼叫、客服电话、视频通关、养老资源、医疗资源、旅游资源等未绑定 `onClick` 的项走统一 Toast。
- 当前样式特点
  - 与首页共享蓝色卡片、图标圆形底、服务卡网格/列表响应式布局。
  - 页面更像入口宫格，信息密度比首页高。
- 存在问题
  - 多个服务项只是入口外观，没有实际页面。
  - `ServiceItem` 本地静态定义，缺少服务状态字段，Toast 和真实跳转混杂。
  - 图标使用 Material Icons，语义有重复，如多个服务复用 Favorite/List。
- 后续建议
  - 改版前先给服务项加“已接入/占位/外部链接/后端数据”等状态定义。
  - 将 `ServiceGrid + ActionCard` 抽成统一服务入口组件。
  - 对未开放项使用明确的 Disabled/Coming soon 状态，而不是可点击 Toast。

### MyScreen

- 主要区块
  - 顶部栏。
  - 欢迎用户卡。
  - 已保存材料清单区。
- 主要组件
  - `UnifiedTopBar`、`SoftCard`、`IconBadge`、`SavedMaterialsSection`、`NoticeCard`、`StatusPill`。
- 点击行为
  - 点击已保存材料清单进入对应 MaterialChecklist。
- 当前样式特点
  - 结构很轻，只有账号欢迎和保存清单。
  - 与首页/服务页共享卡片和蓝色图标风格。
- 存在问题
  - `SavedMaterialChecklist` 当前只存在 `MaterialViewModel` 内存状态，没有看到持久化。
  - `MyListItem` 组件存在但当前 MyScreen 未使用，可能是遗留或待扩展。
  - 没有语言设置入口；只有字体设置在 Home/Chat 顶部。
- 后续建议
  - 后续“我的”适合承载语言、字体、历史、保存材料等设置入口。
  - 保存清单若要成为正式功能，需先明确持久化方式，再做 UI 强化。

### PortDetailScreen

- 主要区块
  - 顶部返回栏。
  - 口岸状态摘要卡。
  - 重要提醒卡。
  - 过关步骤卡。
  - 相关服务网格。
  - 演示数据提醒。
- 主要组件
  - `UnifiedTopBar`、`InfoRow`、`StatusPill`、`ReminderRow`、`TimelineRow`、`ServiceGrid`、`NoticeCard`。
- 点击行为
  - 返回关闭覆盖页。
  - “问 AI”只切换到 Chat，不预填问题。
  - “查看材料”打开 `border_crossing_prepare` 材料清单。
  - “导航”“找家人帮忙”均为 Toast 占位。
- 当前样式特点
  - 状态、提醒、步骤和相关服务分卡展示，适合详情页阅读。
  - 使用橙色强调风险提醒，绿色表示正常状态。
- 存在问题
  - 详情页固定显示深圳湾口岸，`ports` 中其他口岸点击后仍进入同一个详情内容。
  - 开放时间和等待时间是静态演示数据。
  - 相关服务中真实跳转和 Toast 占位混在同一网格。
- 后续建议
  - 先抽象 `PortInfo` 到详情参数，再考虑 UI 改版。
  - 明确口岸数据来源后再做状态色、拥堵展示和更新时间样式。

### GuideScreen

- 主要区块
  - 顶部返回栏。
  - 当前步骤卡：图标、标题、说明、朗读按钮。
  - 步骤进度点和页码。
  - 上一步/下一步或完成。
  - 关闭按钮。
- 主要组件
  - `UnifiedTopBar`、`SoftCard`、`IconBadge`、`ProgressDot`、`PrimaryActionButton`、`SecondaryActionButton`。
- 点击行为
  - 上一步/下一步修改本地 `stepIndex`。
  - 最后一步“完成”和“关闭”关闭覆盖页。
  - 朗读本步骤使用 TTS。
- 当前样式特点
  - 单卡片强引导，按钮清晰，适合适老化操作教学。
- 存在问题
  - `GuideStep`、标题、按钮、说明文本大量硬编码中文，未接入 `strings.xml`。
  - `guideText` 字符串拼接疑似存在模板/标点问题，需运行时确认朗读内容。
  - 仅线性引导，没有从某个功能点进入对应步骤的能力。
- 后续建议
  - 先迁移文案到资源，再改视觉。
  - 可将其抽成通用 `StepGuideScreen` 或 `GuideStepCard`。

### ElderCareServiceScreen

- 主要区块
  - 顶部返回栏。
  - 服务类型列表：养老院、日间照料、长者饭堂、补贴政策。
  - 精选机构信息列表。
  - “不知道怎么选”问 AI 卡。
- 主要组件
  - `ElderCareServiceTypeCard`、`ElderCareInstitutionCard`、`SecondaryMiniButton`、`StatusPill`、`SoftCard`、`SectionTitle`。
- 点击行为
  - 服务类型点击显示“功能后续接入” Toast。
  - 机构“查看详情”进入 ElderCareInstitutionDetail。
  - 机构“地图导航”Toast。
  - 问 AI 切到 Chat，但不预填养老服务问题。
- 当前样式特点
  - 比 ServiceScreen 更偏信息列表，使用橙/绿/蓝多色图标底。
  - 机构卡包含标签、来源和双按钮。
- 存在问题
  - 养老数据是本地静态列表，不是后端或真实地图数据。
  - 来源、地图、问题入口大多是 Toast，占位较多。
  - 问 AI 没有携带上下文问题，用户进入 Chat 后仍需自行输入。
- 后续建议
  - 先明确养老服务是否属于正式范围；若保留，应抽出机构卡、标签、来源行。
  - 问 AI 入口建议预填标准问题，减少跨页后断点。

## 3. 当前可复用组件候选

| 组件候选 | 当前可能出现在哪些页面 | 建议统一哪些参数 | 是否适合现在抽取 |
| --- | --- | --- | --- |
| AppPageHeader | Home、Chat、Service、My、PortDetail、Guide、MaterialList、ElderCareService | title、subtitle、back、actions、gradient/elevated、leadingIcon | 已有 `UnifiedTopBar`，适合保留并轻量整理 |
| AppBottomNav | 根页面 Home/Chat/Service/My | item label/icon/selected、无障碍说明、未读/状态扩展 | 已有 `UnifiedBottomNav`，暂不必重抽 |
| LargeActionCard | Home Hero、Service 办理判断、ElderCare 问 AI 卡、CrossBorder 说明卡 | title、subtitle、icon、primary/secondary actions、色彩语义 | 适合现在抽取，能先统一首页和服务页 |
| ServiceEntryCard | ServiceGrid、CrossBorderPreparePicker、PortDetail 相关服务 | title、subtitle、icon、状态、onClick、grid/list 模式 | 适合现在抽取，建议从现有 `ActionCard` 扩展 |
| QuestionCard | GuidanceQuestionCard、FAQ 问题行、Chat 场景选项 | question/title、选项、selected、readAction、layout columns | 可稍后抽取，先稳定 Guidance 交互 |
| AnswerSectionCard | AssistantStructuredAnswerCard 内结论/步骤/材料/注意事项 | section title、items、empty state、expand/collapse、source | 适合 Chat 改版前抽取 |
| VoiceInputBar | ChatInputBar、VoiceInputPanel、VoiceConfirmCard | input value、send enabled、voice enabled、recording state、loading state | 适合 Chat 改版时抽取，不建议先动 |
| SectionTitle | Home、Service、PortDetail、MaterialList、ElderCare | title、icon、强调条、间距 | 已有 `SectionTitle`，适合标准化用法 |
| EmptyStateCard | QaEmptyState、材料空保存、历史空状态 | icon、title、desc、primary action、example list | 适合现在抽取一个轻量版本 |
| NoticeCard | Home/Service/Port/Material/Font 等提示 | text、type(info/warning/success)、icon、color | 已有 `NoticeCard`，建议加语义 type |
| StatusPill | Port 状态、材料必带/可选、机构标签 | text、type、color、size | 适合现在抽取并统一色彩 token |
| ChecklistRow | MaterialChecklist 的 RequirementCheckRow | checked、required、name、description、note、linkedAction | 等材料功能确定后抽取 |
| InstitutionCard | ElderCareInstitutionCard、未来养老资源卡 | name、tags、source、actions、distance/map 状态 | 暂缓，养老服务仍偏静态原型 |

## 4. 当前视觉风格问题

- 颜色是否统一
  - 已有 `ElderBlue`、`ElderGreen`、`ElderOrange`、`ElderRed` 等基础色，整体趋于统一。
  - 但页面内仍有较多直接写死的 `Color(0x...)`，如蓝色边框、橙色提示、浅底色，后续应收敛到 design tokens。
- 字号是否统一
  - `ElderResponsiveSpec` 已统一 topBar、section、card、body、label 等字号，并支持大字模式。
  - 局部仍有硬编码字号，如 Guidance 序号 `19.sp`、FontChoice 采样 `28.sp`。
- 圆角是否统一
  - 卡片和控件大多使用 `responsive.cardCorner/controlCorner`。
  - 仍有局部 `14.dp`、`22.dp`、`28.dp`、`8.dp` 直接写死，按钮/Chip/输入框圆角体系不完全统一。
- 卡片间距是否统一
  - 大部分页面通过 `responsive.pagePadding/pageSpacing/cardPadding` 控制。
  - Chat 底部预留 `160.dp`、部分卡片内部 fixed spacing 较多，改版时需要重新校准。
- 图标风格是否统一
  - 全部使用 Material Icons，风格统一。
  - 但语义重复和不精确较多，例如 Chat 用 Email 图标、多个服务用 Favorite/List，后续可替换为更贴合的图标集合。
- 首页、服务页、问答页是否像同一个 App
  - 基础色、卡片、圆形图标底和适老化字号让三者有共同感。
  - Chat 的复杂回答卡和语音面板比 Home/Service 更“功能型”，需要通过统一卡片标题、操作按钮、提示状态来拉齐。
- 是否存在英文/繁体显示问题
  - 存在多语言资源目录：`values`、`values-zh-rCN`、`values-zh-rHK`、`values-zh-rMO`、`values-zh-rTW`、`values-en`。
  - 部分显示文案接入了 `strings.xml`，但 FAQ query、快捷问题提交文本、Guide、Guidance、FontSize、部分 Toast 仍硬编码中文。
  - 终端读取部分文件时出现乱码显示，需要在真机/模拟器或资源检查中确认运行时简体、繁体、英文是否正常；本次未运行设备验证。

## 5. 后续 UI 改版风险点

- 哪些文件不应该轻易改
  - `ChatViewModel.kt`：承载 RAG、ASR、TTS、历史记录保存，UI 改版时不应顺手改业务逻辑。
  - `ChatModels.kt`：后端响应结构和结构化回答模型，变动会影响 RAG 显示。
  - `ApiClient.kt` / `AssistantApi.kt`：后端接口路径和超时配置，UI 改版不应触碰。
  - `VoiceRecorder.kt`：录音生命周期和临时文件处理，语音 UI 改版时要保持调用顺序。
  - `AppLanguageResolver.kt`：显示语言和朗读语言映射，涉及 i18n/TTS。
  - `MaterialViewModel.kt`：虽然当前是 Mock，但已承载材料清单保存状态，改 UI 时不要改变清单 code 和选中逻辑。
- 哪些页面只适合做最小 UI 调整
  - ChatScreen：与后端 RAG、ASR、TTS、历史记录、材料入口绑定深，建议先抽组件再小步调整。
  - MaterialChecklistScreen：涉及勾选保存、关联清单跳转，适合保守改样式。
  - GuidanceScreen：本地规则、TTS 朗读、Chat 标准问题衔接较多，先迁文案再改 UI。
- 哪些功能与后端/RAG/ASR/TTS/i18n 绑定较深
  - ChatScreen、AssistantStructuredAnswerCard、VoiceInputPanel、VoiceConfirmCard、ChatInputBar。
  - GuideScreen 和 GuidanceScreen 使用 TTS。
  - Home 的朗读语言设置影响 Chat/Guide/Guidance 的 TTS language。
  - FAQ/快捷问题显示与提交问题分离，属于 i18n 风险点。
- 哪些入口目前只是 Toast，不能误认为完整功能
  - Home：联系客服、志愿者协助、视频讲解。
  - Chat：继续追问、部分重试提示。
  - Service：除办理判断、过关材料、养老服务外，多数服务入口未接入。
  - PortDetail：导航、找家人帮忙。
  - ElderCareService：服务类型、地图导航。
  - ElderCareInstitutionDetail：查看来源、地图导航、可了解的问题。
- Mock 数据、后端数据、RAG 数据边界
  - RAG 后端数据：Chat 问答与结构化回答。
  - ASR/TTS 后端数据：语音识别和朗读音频。
  - Mock/本地数据：材料清单、口岸列表/详情、养老服务类型/机构、Guidance 判断规则、FAQ 列表。
  - 已定义但当前 UI 未使用的材料后端接口：`materialItems`、`materialChecklist`、`saveMaterialChecklist`。

## 6. 建议的下一步

1. 先建立 `design-system-v2.md`
   - 固化颜色、字号、圆角、间距、按钮、卡片、状态色、图标语义和适老化响应式规则。
2. 再抽取少量复用 UI 组件
   - 优先整理 `LargeActionCard`、`ServiceEntryCard`、`NoticeCard(type)`、`StatusPill(type)`、`EmptyStateCard`。
3. 然后单页改 HomeScreen
   - 首页入口最多、风险较低，适合先验证统一风格。
4. 再改 ChatScreen
   - 保持 ViewModel/API 不动，先改回答卡、输入栏、语音确认卡的展示层。
5. 最后改 ServiceScreen
   - 在服务项状态清晰后，再统一入口网格、未开放状态和养老服务跳转。
