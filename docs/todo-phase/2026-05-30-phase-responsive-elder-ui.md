# 2026-05-30 Phase：适老化 UI 响应式改造说明

项目：粤同心-湾区中老年助手  
目标读者：Codex / Android UI 开发  
当前范围：只改 UI 响应式、字体策略、顶栏、卡片与按钮布局；不改后端、不新增真实跳转、不改 RAG/语音接口逻辑。

---

## 1. 本阶段目标

当前 App 已经具备适老化基础：政务蓝、卡片化、大按钮、大字体、底部导航、语音朗读入口。但从当前截图看，存在一个明显问题：

> 页面不是“不够适老化”，而是“所有东西同时变大”，导致手机竖屏空间被挤爆，标题换行、服务卡文字截断、底部按钮贴近系统手势区。

本阶段目标是把现有 UI 从“固定手机大字号布局”改成“适老化响应式布局”：

1. 手机竖屏保持单列、清楚、大按钮。
2. 大字/超大字时自动降低布局密度，不强行双列。
3. 平板、横屏、折叠屏展开时使用更多列或主从分栏。
4. 顶栏标题在二级页面真正居中，不被返回按钮和右侧按钮挤偏。
5. 组件大小分级：主操作大，次要按钮适中，图标和卡片不要无差别放大。
6. 保留当前 Demo 逻辑：服务页未开放入口继续 Toast；不新增真实页面跳转。

参考依据：Android 官方建议使用 Window Size Classes 做响应式/自适应布局，宽度一般比高度更影响 UI 布局。官方断点包括 Compact `<600dp`、Medium `600dp–839dp`、Expanded `840dp–1199dp` 等。  
参考链接：
- https://developer.android.com/develop/ui/compose/layouts/adaptive/use-window-size-classes
- https://developer.android.com/develop/ui/compose/build-adaptive-apps

---

## 2. 当前文件与主要入口

请优先检查并修改这三个文件：

```text
ElderResponsive.kt
ElderCareComponents.kt
ElderCareScreens.kt
```

当前关键结构：

```kotlin
// ElderResponsive.kt
fun elderResponsiveSpec(width: Dp, fontScale: Float): ElderResponsiveSpec
```

```kotlin
// ElderCareScreens.kt
BoxWithConstraints {
    val responsive = remember(maxWidth, selectedFont.scale) {
        elderResponsiveSpec(maxWidth, selectedFont.scale)
    }
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = selectedFont.scale
        ),
        LocalElderResponsive provides responsive
    ) { ... }
}
```

```kotlin
// ElderCareComponents.kt
@Composable
fun UnifiedTopBar(...)

@Composable
fun PillIconButton(...)

@Composable
fun ActionCard(...)
```

---

## 3. 目前发现的问题

### 3.1 顶栏标题不是真正居中

二级页面如“操作指南”“字体设置”“办理情况判断”有返回按钮后，标题只是位于剩余空间中居中，而不是在整屏中居中。

当前 `UnifiedTopBar` 的问题：

```kotlin
Column(modifier = Modifier.weight(1f)) {
    Text(
        textAlign = if (showBack) TextAlign.Center else TextAlign.Start,
        modifier = Modifier.fillMaxWidth()
    )
}
```

这会导致：

- 有返回按钮时标题偏右或偏左。
- 有右侧 actions 时标题被压缩。
- “智能问答”在手机窄屏下可能换成两行：`智能问 / 答`。

### 3.2 当前响应式断点不合理

当前 `ElderResponsive.kt`：

```kotlin
val sizeClass = when {
    width < 390.dp -> ElderWindowSizeClass.Compact
    width < 600.dp -> ElderWindowSizeClass.Medium
    else -> ElderWindowSizeClass.Expanded
}
```

问题：

- 很多普通 Android 手机竖屏宽度是 360dp、390dp、411dp 左右。
- 现在 `390dp–599dp` 会被当成 Medium，导致普通手机误判成中屏。
- 中屏策略会更容易出现按钮并排、卡片双列、顶栏 actions 过宽等问题。

### 3.3 四种字体大小可以保留，但不能让布局跟着“硬放大”

当前字体：

```kotlin
FontChoice("小字", "小", 0.9f)
FontChoice("中字", "中", 1.0f)
FontChoice("大字", "大", 1.15f)
FontChoice("超大字", "超大", 1.3f)
```

问题不是四档太多，而是大字/超大字时布局没有足够降密度。

四档可以保留，但它们应该影响：

1. 字号。
2. 行高。
3. 按钮最小高度。
4. 是否强制单列。
5. 顶栏按钮是否只显示图标。
6. 服务卡是否从双列退回单列。

不要为四种字体写四套页面。

### 3.4 服务页双列在大字模式下太挤

当前服务页卡片在手机上使用 2 列，卡片内部又是横向结构：

```text
[图标] 通关
       流程
```

会导致：

- “通关流程”被折成两行。
- “特殊人群预约”等文字被截断成“特殊人...” 。
- 图标占据横向空间，文字区过窄。

### 3.5 操作指南页纵向空间被挤压

当前 GuideScreen 有硬编码：

```kotlin
IconBadge(icon = current.icon, size = 150.dp)
fontSize = 28.sp
fontSize = 21.sp
Modifier.padding(24.dp)
```

并且底部有三个全宽大按钮：

```text
上一步
下一步
关闭
```

问题：

- 大图标太占高度。
- 禁用的“上一步”仍然占完整主按钮高度。
- “关闭”容易贴近底部手势区。
- 手机竖屏尤其容易显得拥挤。

### 3.6 聊天输入区过重

当前 ChatInputBar 是：

```text
麦克风 + 输入框
发送按钮
```

当输入为空时，灰色“发送”按钮仍然占据一整行。它视觉存在感很强，但此时没有实际操作价值。

### 3.7 首页首屏信息较多

首页包含：

- 顶部标题。
- 字体/语言按钮。
- 两行 SegmentedControl。
- 欢迎卡。
- 口岸卡。
- 常见问题。
- 材料清单入口。
- 底部导航。

在大字模式下会显得挤。首页需要保留重点入口，但非核心信息可以下移。

---

## 4. 本阶段设计原则

### 4.1 适老化不是所有东西都放大

请按重要程度分级：

| 组件 | 建议策略 |
|---|---|
| 主操作按钮 | 大，清楚，优先全宽 |
| 次要按钮 | 适中，不抢主操作 |
| 图标背景 | 不要过大，避免占用文字空间 |
| 卡片 | 清楚但不要过厚 |
| 标签/说明 | 可读即可，不需要和标题一样大 |
| 顶栏 actions | 手机下优先图标化 |

### 4.2 大字时降低信息密度

规则：

```text
字体越大，布局越保守。
字体越大，越应该单列。
字体越大，越不应该强行横向并排。
```

### 4.3 响应式先看有效宽度

建议不要只看 `width`，而是引入 `effectiveWidth`：

```kotlin
val effectiveWidth = width / fontScale.coerceAtLeast(1f)
```

含义：

- 同样是 600dp 宽，如果字体放大到 1.25，实际可用的阅读布局空间应该按更小处理。
- 这样超大字会自动退回 Compact，更适合老人使用。

---

## 5. 需要修改的响应式规则

### 5.1 修改 ElderResponsive.kt 的断点

把当前逻辑：

```kotlin
val sizeClass = when {
    width < 390.dp -> ElderWindowSizeClass.Compact
    width < 600.dp -> ElderWindowSizeClass.Medium
    else -> ElderWindowSizeClass.Expanded
}
```

改成有效宽度逻辑：

```kotlin
val effectiveWidth = width / fontScale.coerceAtLeast(1f)

val sizeClass = when {
    effectiveWidth < 600.dp -> ElderWindowSizeClass.Compact
    effectiveWidth < 840.dp -> ElderWindowSizeClass.Medium
    else -> ElderWindowSizeClass.Expanded
}
```

### 5.2 建议新增 responsive 字段

在 `ElderResponsiveSpec` 中建议增加：

```kotlin
val effectiveWidth: Dp,
val isLargeText: Boolean,
val isExtraLargeText: Boolean,
val contentMaxWidth: Dp,
val chatContentMaxWidth: Dp,
val serviceColumns: Int,
val guideHeroIconSize: Dp,
val showTopBarActionText: Boolean,
val useCenterTitleTopBar: Boolean
```

建议值：

```kotlin
val largeText = fontScale >= 1.12f
val extraLargeText = fontScale >= 1.25f

val contentMaxWidth = when (sizeClass) {
    ElderWindowSizeClass.Compact -> Dp.Unspecified
    ElderWindowSizeClass.Medium -> 720.dp
    ElderWindowSizeClass.Expanded -> 1100.dp
}

val chatContentMaxWidth = when (sizeClass) {
    ElderWindowSizeClass.Compact -> Dp.Unspecified
    else -> 760.dp
}

val serviceColumns = when {
    extraLargeText && effectiveWidth < 840.dp -> 1
    largeText && effectiveWidth < 600.dp -> 1
    sizeClass == ElderWindowSizeClass.Compact -> 1
    sizeClass == ElderWindowSizeClass.Medium -> 3
    else -> 4
}

val guideHeroIconSize = when {
    extraLargeText -> 96.dp
    sizeClass == ElderWindowSizeClass.Compact -> 104.dp
    else -> 120.dp
}

val showTopBarActionText = !largeText && sizeClass != ElderWindowSizeClass.Compact
```

注意：如果认为手机中字模式仍然要服务页 2 列，可以把 `Compact -> 2`，但当前截图显示 2 列不稳。建议本阶段先用手机 1 列，保证适老化可读性。

---

## 6. 字体策略

### 6.1 字体档位保留，但建议调整 scale

可以保留四档：

```kotlin
FontChoice("小字", "小", 0.95f)
FontChoice("中字", "中", 1.0f)
FontChoice("大字", "大", 1.12f)
FontChoice("超大字", "超大", 1.25f)
```

如果不想改用户可感知的字号，也可以暂时保留原值：

```kotlin
0.9f / 1.0f / 1.15f / 1.3f
```

但如果保留 `1.3f`，必须强化单列和图标化策略。

### 6.2 统一字号 token，不要在页面里硬写大字号

建议基础 token：

```kotlin
topBarTitle = 21.sp
sectionTitle = 20.sp
cardTitle = 19.sp
bodyLarge = 18.sp
body = 17.sp
label = 15.sp
labelSmall = 13.sp
```

这些 `sp` 会受 `LocalDensity.fontScale` 影响。不要在页面里再大量硬写 `28.sp`、`21.sp`。

### 6.3 按钮高度分级

```kotlin
buttonMinHeight = when {
    extraLargeText -> 62.dp
    largeText -> 58.dp
    else -> 54.dp
}

compactButtonMinHeight = when {
    extraLargeText -> 56.dp
    largeText -> 52.dp
    else -> 48.dp
}
```

主按钮可以大；次要按钮不要全部做成主按钮体量。

---

## 7. 顶栏改造任务

### 7.1 UnifiedTopBar 分成两种模式

建议保留一个函数，但内部区分：

1. 首页/一级页：左标题 + 右 actions。
2. 二级页：返回按钮 + 绝对居中标题 + 右侧等宽占位或 actions。

### 7.2 二级页顶栏实现思路

`showBack == true` 时，用 `Box` 结构：

```kotlin
Box(
    modifier = Modifier.fillMaxWidth(),
    contentAlignment = Alignment.Center
) {
    Row(
        modifier = Modifier.align(Alignment.CenterStart),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RoundIconButton(...)
    }

    Text(
        text = title,
        textAlign = TextAlign.Center,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .align(Alignment.Center)
            .padding(horizontal = 72.dp)
    )

    Row(
        modifier = Modifier.align(Alignment.CenterEnd),
        verticalAlignment = Alignment.CenterVertically
    ) {
        actions.forEach { ... }
    }
}
```

要求：

- `操作指南`、`字体设置`、`办理情况判断`、`材料清单`等二级页标题必须真正居中。
- 标题默认 `maxLines = 1`。
- 如果标题过长，使用省略号，不要换成两行挤占高度。

### 7.3 顶栏 actions 手机下图标化

当前 `PillIconButton` 在隐藏文字后仍然是 OutlinedButton 形态，可能仍偏宽。建议：

- `showTopBarActionText == true`：显示胶囊按钮，带文字。
- `showTopBarActionText == false`：显示圆形或方圆图标按钮，不显示文字。

首页手机顶栏建议：

```text
粤同心 / 湾区中老年助手        [字体图标] [语言图标]
```

问答页手机顶栏建议：

```text
智能问答                       [历史图标] [字体图标]
```

不要让“智能问答”被挤成两行。

---

## 8. 页面级改造任务

## 8.1 HomeScreen 首页

### 当前问题

- 首页信息偏多。
- 欢迎卡标题过长。
- 两行筛选器挤占首屏。
- 口岸卡横向内容容易拥挤。

### 修改要求

1. 欢迎卡标题改短：

```text
您好，我来帮您办事
```

说明文字：

```text
查政策、看材料、问流程，都可以直接提问。
```

2. 欢迎卡内按钮：

- Compact：`点击提问` 与 `操作指南` 可以上下堆叠或并排，视 `stackActionRows` 而定。
- Medium/Expanded：可以并排。

3. 筛选器：

- 如果空间紧张，大字/超大字时只保留一行核心筛选，或下移到口岸区之前。
- 不要让两行 SegmentedControl 抢走欢迎卡首屏重点。

4. 口岸卡：

建议结构：

```text
深圳湾口岸                  正常
开放：6:30 - 24:00
预计等待：约 15 分钟
[查看详情]
```

按钮可以放在底部右侧，不要挤压中间文字。

5. Expanded 下可以做左右分区：

```text
左侧：欢迎卡 + 服务入口
右侧：常用口岸 + 常见问题 + 材料清单
```

本阶段如果不做大屏主从，也至少限制内容最大宽度，不要横向拉满。

---

## 8.2 ChatScreen 智能问答页

### 当前问题

- 顶栏 actions 过宽，标题换行。
- 输入区两行显示，空输入时发送按钮占位过大。
- 回答卡内多个按钮都很大，抢正文注意力。

### 修改要求

1. 顶栏：

- 手机下 actions 图标化。
- 标题一行显示：`智能问答`。

2. 输入栏：

Compact 下建议改成单行：

```text
[麦克风] [请输入您的问题................] [发送图标]
```

发送按钮为空时置灰即可，不要单独占一整行。

如果超大字导致单行不稳，可以退回两行，但发送按钮宽度不要全屏巨大；可以右对齐小按钮。

3. 回答卡按钮：

当前：

```text
朗读回答 / 查看材料清单
继续追问
```

建议：

- 主按钮保留一个最重要动作。
- 次要按钮高度用 `compactButtonMinHeight`。
- 大字/超大字时允许堆叠，但不要所有按钮都 60dp+。

4. Expanded 下可选主从布局：

```text
左侧：历史记录 / 常见问题
右侧：聊天消息 + 输入栏
```

本阶段可先不实现复杂历史侧栏，但应预留结构。

---

## 8.3 ServiceScreen 服务页

### 当前问题

- 手机 2 列导致文字被挤压。
- 卡片横向结构不适合大字。
- 服务入口标题被截断。

### 修改要求

1. `ServiceGrid` 不要固定 1/2 列：

当前：

```kotlin
val columns = if (responsive.useSingleColumnCards) 1 else 2
```

改为：

```kotlin
val columns = responsive.serviceColumns
```

2. 手机 Compact 建议强制 1 列。

3. 如果未来恢复手机 2 列，则卡片内部必须改成上下结构：

```text
图标
通关流程
查看过关步骤
```

不要使用横向图标 + 文字。

4. 大字/超大字下服务标题 `maxLines` 可为 2，但不要省略核心标题。说明文字可以 `maxLines = 2` 或弱化。

5. “我不知道自己该怎么办”按钮建议文案缩短为：

```text
我不知道该办哪种
```

这更贴近“港澳通行证 / 签注办理判断”的场景。

---

## 8.4 GuideScreen 操作指南页

### 当前问题

- 大图标 150dp 太占空间。
- 标题 28sp、正文 21sp 写死，超大字时容易爆。
- 三个全宽按钮占用太多纵向空间。
- 顶栏标题不是真正居中。

### 修改要求

1. 替换硬编码尺寸：

```kotlin
IconBadge(icon = current.icon, size = responsive.guideHeroIconSize)
fontSize = responsive.cardTitle 或 responsive.sectionTitle
fontSize = responsive.bodyLarge
Modifier.padding(responsive.cardPadding)
```

2. 图标尺寸建议：

```text
Compact：96dp - 104dp
Medium：112dp
Expanded：120dp
```

不要继续使用 150dp。

3. 底部按钮改造：

Compact 建议：

```text
[上一步] [下一步/完成]
关闭
```

或者：

```text
[下一步/完成]
关闭
```

禁用的“上一步”不要作为完整全宽主按钮出现。

4. `关闭` 可以是 SecondaryActionButton，也可以是文字按钮；不要贴近系统手势区。

5. 进度点和 `1/5` 保留，但间距收紧。

---

## 8.5 MaterialListScreen / GuidanceScreen

### 当前目标

这两个页面适合后续做主从结构，但本阶段先保证：

1. 二级页顶栏标题居中。
2. 大字/超大字下问题卡片、材料卡片单列。
3. 按钮不要挤出屏幕。
4. 选项按钮可以大，但同一题的多个选项不要强行横排。

GuidanceScreen 中“回答 5 个问题判断办理类型”的逻辑保留，不接真实跳转。

---

## 9. 组件级改造任务

### 9.1 ActionCard

需要支持两种布局：

```kotlin
enum class CardLayoutMode {
    Horizontal,
    Vertical,
    List
}
```

或用 responsive 自动判断：

- 单列服务列表：横向图标 + 文本。
- 多列宫格：上下结构，避免横向挤压。

建议：

```text
单列列表卡：图标 48-56dp，文字完整。
多列宫格卡：图标在上，标题居中或左对齐。
```

### 9.2 AdaptivePairRow

检查当前 `AdaptivePairRow` 是否仅依赖 `stackActionRows`。要求：

- Compact + 大字/超大字：堆叠。
- Medium/Expanded：并排。
- 如果任一按钮文案过长，允许堆叠。

### 9.3 PrimaryActionButton / SecondaryActionButton

要求：

- 主按钮默认高度使用 `responsive.buttonMinHeight`。
- 次按钮默认高度使用 `responsive.compactButtonMinHeight`。
- 不要在页面里频繁硬写 `height = 58.dp`、`60.dp`、`54.dp`。
- 禁用按钮降低视觉权重，不要抢主操作。

### 9.4 IconBadge

建议按用途区分尺寸：

```text
普通卡片图标：44-52dp
重点入口图标：56-64dp
操作指南大图标：96-120dp
```

不要所有场景都用大图标。

---

## 10. 推荐执行顺序

### Step 1：先改 ElderResponsive.kt

1. 引入 `effectiveWidth`。
2. 改断点为 600/840。
3. 增加大字/超大字判断。
4. 增加 `serviceColumns`、`guideHeroIconSize`、`showTopBarActionText` 等字段。

### Step 2：改 UnifiedTopBar

1. `showBack == true` 使用绝对居中标题布局。
2. 手机 actions 图标化。
3. 标题默认一行省略，不允许被挤成两行。

### Step 3：改 ServiceGrid 和 ActionCard

1. `ServiceGrid` 使用 `responsive.serviceColumns`。
2. Compact 手机优先一列。
3. 多列时 ActionCard 改上下结构。

### Step 4：改 GuideScreen

1. 移除 `150.dp`、`28.sp`、`21.sp` 等硬编码。
2. 调整底部按钮：上一步/下一步并排或弱化上一步。
3. 保证关闭按钮不贴底部手势区。

### Step 5：改 ChatInputBar

1. Compact 下优先单行输入栏。
2. 空输入时发送按钮只保留图标置灰，不占整行。
3. 超大字时允许回退为两行，但按钮不全屏巨大。

### Step 6：首页轻量优化

1. 欢迎文案变短。
2. 筛选器和口岸卡减少横向挤压。
3. 大屏限制最大内容宽度。

---

## 11. 验收标准

请在以下尺寸和字体档位测试：

### 手机竖屏

```text
360 x 800 dp
390 x 844 dp
411 x 891 dp
```

字体档位：

```text
中字 / 大字 / 超大字
```

必须满足：

- 顶栏标题不被挤成两行。
- 二级页标题视觉居中。
- 服务卡标题不出现“特殊人...”这类核心信息截断。
- 操作指南页底部按钮不贴系统手势区。
- 问答输入栏不因为空发送按钮占掉过多高度。

### 平板 / 横屏

```text
600 x 960 dp
840 x 1080 dp
1024 x 768 dp
```

必须满足：

- 内容不要横向铺满全屏。
- 服务卡可 3/4 列。
- 聊天内容最大宽度合理，不要一条消息横跨全屏。
- 首页信息层级清楚。

---

## 12. 不在本阶段做的事情

本阶段不要做：

1. 不接真实口岸数据。
2. 不新增真实服务跳转。
3. 不改 RAG 问答接口。
4. 不改 ASR/TTS 云端逻辑。
5. 不重做整体视觉品牌。
6. 不新增复杂动画。
7. 不引入新的页面路由系统，除非现有结构必须修复。

---

## 13. Codex 修改提示词

可以直接把下面这段发给 Codex：

```text
请根据当前项目的 ElderResponsive.kt、ElderCareComponents.kt、ElderCareScreens.kt，完成一轮适老化 UI 响应式改造。

重点目标：
1. 修复顶栏：二级页面有返回按钮时，标题必须相对于整屏真正居中；手机下顶栏 actions 图标化，避免“智能问答”被挤成两行。
2. 修复响应式断点：不要使用 width < 390dp 作为 Compact。请使用 effectiveWidth = width / fontScale，并按 <600dp、600-839dp、>=840dp 判断 Compact/Medium/Expanded。
3. 保留四种字体大小，但字体大小只影响字号、行高、按钮高度、布局密度和是否单列，不要写四套页面。
4. 大字/超大字下自动降低布局密度：服务页手机强制单列，按钮行可堆叠，顶栏按钮只显示图标。
5. 改造服务页 ServiceGrid：不要固定 2 列，使用 responsive.serviceColumns。手机大字/超大字下必须避免服务标题截断。
6. 改造操作指南 GuideScreen：去掉 150dp 图标、28sp 标题、21sp 正文等硬编码，改用 responsive token；底部按钮减少纵向占用，避免贴底部手势区。
7. 改造 ChatInputBar：手机下优先使用“麦克风 + 输入框 + 发送图标”的单行布局；空输入时发送按钮置灰，不要占一整行。
8. 首页轻量优化：欢迎卡标题变短，减少首屏拥挤；口岸卡避免按钮挤压文字。
9. 保持原有业务逻辑：服务页未开放入口继续 Toast；不要新增真实跳转；不要改后端/RAG/语音接口。
10. 修改完成后请保证 360dp、390dp、411dp 手机宽度，以及 600dp、840dp 以上宽度都能正常显示。
```

---

## 14. 当前阶段结论

当前 UI 已经具备适老化方向，但需要从“控件全部变大”改为“主操作突出、次要操作收敛、字体越大布局越保守”。

最优先的三个改动是：

1. 顶栏重构。
2. `effectiveWidth` 响应式断点。
3. 服务页和操作指南页在大字模式下降密度。

这三项完成后，整体观感会明显更稳，也更适合后续扩展到横屏、折叠屏和平板。
