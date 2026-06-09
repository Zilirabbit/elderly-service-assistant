# Phase 6.4 语音面板正式化、Me 页 i18n、回答卡片与首页顶栏修复总结

日期：2026-06-03

## 本轮目标

- 只做 Android 前端最小修改。
- 不修改后端。
- 不修改 Dify / RAG / ASR / TTS 主链路。
- 不改变 Dify 入参、`rewritten_search_query`、字段级翻译或语音主链路。

## 修改范围

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
- `ElderCareApp/app/src/main/res/values/strings.xml`
- `ElderCareApp/app/src/main/res/values-zh-rHK/strings.xml`
- `ElderCareApp/app/src/main/res/values-en/strings.xml`

本阶段同时承接前序 Android i18n 静态壳层改动，相关模型和 mock 数据资源化文件包括：

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/model/MaterialModels.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/FaqData.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/MaterialViewModel.kt`

## 语音面板正式化

- 正式 UI 隐藏“普通话 / 方言 / 英语”语种选择按钮。
- 使用 `BuildConfig.DEBUG` 作为 debug/devMode 显示条件。
- Debug 包中仍保留语种选择控件，方便测试。
- 无论 UI 是否显示语种选择，ASR 上传参数都保持：

```text
language = auto
```

- 语音面板副标题改为：
  - 简中：`直接说出您的问题即可`
  - 繁中：`直接說出您的問題即可`
  - 英文：`Tap the button and speak naturally`

## 测试音频入口隐藏

正式 UI 隐藏：

- `Audio sample: xxx KB`
- 音频文件名，例如 `hk_macau_pass_renewal_zh.wav`
- `Choose audio sample`

处理方式：

- `VoiceSampleInfoCard` 只在 `BuildConfig.DEBUG` 下显示。
- `VoiceInputPanel` 的 `Choose audio sample` 按钮只在 `showDebugControls = true` 时显示。
- 语音确认页中，如果 draft 来自测试音频，重新选择音频样本入口也只在 Debug 下触发。
- 不删除测试音频能力，只从正式主流程隐藏。

## Me 页 i18n

新增资源 key：

- `my_welcome_title`
- `my_welcome_desc`

已资源化：

- 顶栏“我的”：继续使用 `nav_my`
- “您好，欢迎使用粤同心”：使用 `my_welcome_title`
- “为您保存材料清单和办事草稿”：使用 `my_welcome_desc`

三套资源均已补齐：

- `values/strings.xml`
- `values-zh-rHK/strings.xml`
- `values-en/strings.xml`

## 结构化回答卡片溢出修复

- `QaConclusionBox` 默认显示 3 行。
- 长结论使用 `TextOverflow.Ellipsis`。
- 当结论文本较长时显示展开 / 收起按钮。
- 新增资源 key：
  - `qa_expand_conclusion`
  - `qa_collapse_conclusion`
- 不改变 Dify 返回字段。
- 不把 `structured_answer` 压平成普通回答。
- 不修改后端字段级翻译逻辑。
- 聊天列表底部 padding 从 `112.dp` 增加到 `160.dp`，降低底部输入栏遮挡卡片内容的风险。

## 首页顶栏修复

问题：

- 英文系统下首页顶栏左侧标题区域出现两行。
- 副标题 `Bay Area Helper...` 被省略，视觉不完整。

处理：

- 首页 `UnifiedTopBar` 不再传入 `subtitle = stringResource(R.string.app_subtitle)`。
- 顶栏只显示主标题 `app_name`。
- `app_subtitle` key 保留，不删除，避免影响其他潜在使用场景。
- 右侧 Voice 按钮和设置按钮保持不变。

主标题资源：

- 简中：`粤同心`
- 繁中：`粵同心`
- 英文：`YueTongXin`

## 保持不变

- 未修改 Dify 入参。
- 未修改 `rewritten_search_query` 逻辑。
- 未修改后端 `display_localization_service`。
- 未修改 ASR / TTS 主链路。
- 未启动真实 Dify / DashScope / ADB。
- 未新增字段级翻译。
- 未把 `speechLanguage` 和 `displayLanguage` 绑定。

## 验证

已在 `ElderCareApp` 目录运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

仅保留既有 Material Icons deprecated warnings，与本阶段修改无关。

## 剩余风险

- 未做真实设备截图级视觉 QA。
- Debug 包仍保留测试音频和语种选择入口，正式 UI 隐藏。
- 结构化卡片中 steps / warnings 等其他长文本字段如果未来英文极长，可能还需要进一步逐块折叠。
