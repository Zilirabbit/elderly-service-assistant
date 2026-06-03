# Phase 6.1 对话页 UI 壳 i18n 最小修复总结

日期：2026-06-03

## 本轮目标

- 只处理对话页 UI 外壳文案资源化，改善 `zh-HK` / `en` 系统语言下的中英繁混杂问题。
- 不修改 RAG / Dify / ASR / TTS 主链路。
- 不做结构化回答字段级翻译。
- 不修改 Dify 入参，不把 `rewritten_search_query` 作为 Dify 入参。

## 修改范围

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
  - 快捷问题 chip 的展示 label 改为资源 key。
  - 空态示例的展示文案改为资源 key，点击后仍发送原有标准中文问题。
  - 对话页 toast、语音确认卡、语音输入面板、错误卡、输入框 placeholder、来源前缀等 UI 壳文案改为资源化。
  - 继续保持 `displayLanguage` 来自系统 Locale，`speechLanguage` 只传给 TTS / ASR 相关调用。

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt`
  - 新增 `ChatUiStrings`，由 UI 层注入本地化 UI 壳文案。
  - 查询错误、语音错误、结构化卡 fallback 标题/副标题/警告/source/scenario label 使用 `ChatUiStrings`。
  - 仍保留标准问题中文文本用于现有问答链路，避免改变 Dify 查询输入。

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/model/ChatModels.kt`
  - 移除 `QaAnswerUiModel` 与默认 scenario options 中的中文 UI 默认值。
  - 默认 scenario options 改由 `ChatViewModel` 根据 `ChatUiStrings` 生成。

- `strings.xml`
  - 在 `values`、`values-zh-rHK`、`values-en` 中补齐对话页 UI 壳文案 key。

## 新增 / 迁移 key

- 对话页错误与重试：
  - `chat_error_empty_question`
  - `chat_error_no_knowledge`
  - `chat_error_query_failed`
  - `chat_error_voice_not_clear`
  - `chat_error_voice_not_clear_confirm`
  - `chat_error_voice_timeout`
  - `chat_error_recording_too_large`
  - `chat_error_voice_service_busy`
  - `chat_error_backend_unavailable`
  - `chat_error_voice_failed`
  - `chat_error_network_unstable`
  - `chat_error_voice_no_response`
  - `chat_retry_rephrase`
  - `chat_retry_query`
  - `chat_retry_rephrase_toast`

- 对话页快捷入口和空态：
  - `qa_common_questions`
  - `qa_quick_first_permit`
  - `qa_quick_endorsement`
  - `qa_quick_border_materials`
  - `qa_quick_unsure`
  - `qa_example_first_permit`
  - `qa_example_endorsement_expired`
  - `qa_example_border_materials`

- 结构化卡片 fallback UI 壳：
  - `qa_fallback_subtitle`
  - `qa_fallback_warning`
  - `qa_source_permit_guide`
  - `qa_source_prefix`
  - `qa_scenario_first_permit`
  - `qa_scenario_renewal`
  - `qa_scenario_expired_lost`
  - `qa_scenario_unsure`

- 语音面板与输入框：
  - `voice_*`
  - `chat_input_placeholder`
  - `chat_send_content_description`
  - `common_cancel`

## 保持不变

- `displayLanguage` 仍由 Android 系统 Locale 解析。
- `speechLanguage` 仍只影响 TTS / ASR 相关调用，不影响页面显示语言。
- 有效 `structured_answer` 仍不走整段 `rewrite_display_text`。
- Dify 入参仍不使用 `rewritten_search_query`。
- Dify 返回字段值仍可能是中文；这是字段级翻译未做的已知风险。

## 验证

- 已在 `ElderCareApp` 目录运行：

```powershell
.\gradlew.bat assembleDebug
```

- 结果：通过。
- 仅保留既有 Material Icons deprecated warnings。
- 本轮未修改后端文件，因此未运行 `python -m compileall backend/app` 和 `pytest backend/tests`。
- 未启动真实 Dify / DashScope / ADB。

## 仍未资源化 / 后续风险

- 对话页语音识别语种选项 `普通话 / 方言 / 英语` 仍是硬编码，并参与 ASR 语种映射，本轮未调整。
- 快捷问题、空态示例和场景 fallback 的实际标准查询文本仍为中文，用于保持 Dify 输入稳定。
- `readableText()` 中的朗读拼接提示仍为中文，属于 TTS fallback 文本，不是本轮 UI 显示壳。
- 非对话页仍存在大量中文硬编码，包括办理判断、口岸详情、操作指南、字体设置、个人中心、材料 mock 数据等。
