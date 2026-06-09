# Phase 6.0 结构化回答 i18n 与 TTS 保守防错总结

日期：2026-06-02

## 本轮修改目标

- 基于 Phase 5.8 稳定恢复结果，只做最小修复，不重构语音链路。
- 修正有效 `structured_answer` 在 `zh-HK` / `en` 下仍可能走整段 `rewrite_display_text` 的风险。
- 补充后端单元测试，确认繁中/英文显示语言下结构化 JSON 形状不会被整段改写破坏。
- 对前端临时 TTS 入口做保守防错，避免明显的“粤语音色朗读长篇普通话书面文本”路径。
- 明确 ASR 返回字段语义：当前 `language` 不是模型真实检测出的 `detectedLanguage`。

## 修改文件列表

- `backend/app/api/routes_chat.py`
  - 调整显示改写条件：只要 Dify 返回有效结构化 JSON，就不调用整段 `rewrite_display_text`。
- `backend/tests/test_tts_service.py`
  - 新增 `zh-HK` / `en` 下结构化回答不触发整段显示改写的单元测试。
- `backend/app/schemas/asr_schema.py`
  - 补充 `AsrTranscribeResponse.language` 字段说明，标明它是请求识别语种回显，不是 `detectedLanguage`。
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
  - 普通文本回答朗读优先使用后端返回的 `ttsText` / `ttsAudioUrl`。
  - 临时朗读在“请求粤语、没有后端音频、文本像长篇普通话书面中文”时，保守改用普通话 TTS 语言。

## 关键设计结论

- Dify 入参仍使用用户原始问题。
  - `search_query` / `usage.dify_query` 继续记录真实送入 Dify 的用户原文。
  - `usage.rewritten_search_query` 只用于 usage 排查、日志和与 Dify 控制台对比，不作为 Dify 入参。
  - 本轮没有把 `rewritten_search_query` 改回 Dify 检索问题。
- 有效 `structured_answer` 不再走整段 `rewrite_display_text`。
  - 只要 Dify 返回有效结构化 JSON，`zh-CN`、`zh-HK`、`en` 都保留结构化字段。
  - `zh-HK` / `en` 下字段值暂时允许保持中文。
  - 字段级翻译作为后续任务，不在本轮实现。
  - 不再用整段 Qwen 展示改写替代结构化字段级翻译，避免把结构化卡片压平成普通回答。
- ASR `language` 当前不是 `detectedLanguage`。
  - 该字段目前表示请求识别语种回显，例如 `auto`、`zh`、`yue`、`en`。
  - 本轮没有假装实现模型真实检测语言，也没有做真实 DashScope 联调。
- 临时 TTS 只做保守防错，不是完整 `spokenText` 多语言系统。
  - 主问答朗读继续优先使用后端返回的 `tts.text` 和音频 URL。
  - `/tts/synthesize` 仍只负责合成传入文本，不自动调用 Qwen 改写。
  - 前端临时朗读只避免明显的长篇普通话文本被粤语音色直接朗读。
  - 本轮没有引入大规模 `spokenText` 生成、缓存或字段级转换系统。

## 验证结果

- `python -m compileall backend/app`：通过。
- `$env:PYTHONPATH='backend'; pytest backend/tests`：12 passed。
- 在 `ElderCareApp` 目录运行 `.\gradlew.bat assembleDebug`：通过。
  - 仅保留既有 Material Icons deprecated warnings。
- 未启动真实 Dify。
- 未调用真实 DashScope / Qwen / Dify API。
- 未运行 ADB。
- 未连接真机或模拟器。

## 仍未解决风险

- `zh-HK` / `en` 下结构化卡片字段仍可能显示中文，后续需要做结构化字段级翻译。
- ASR 仍没有真实 `detectedLanguage` 字段，当前只能确认请求语种和原始识别文本链路。
- 临时 TTS 防错只是保守降风险，无法保证所有非主问答入口的文本与音色完全匹配。
- 操作指南、语音面板、办理判断、口岸详情、个人中心等页面仍可能存在硬编码中文。
- 本地 mock 材料数据仍以中文为主，尚未建立多语言材料数据源。
- 未做真实服务联调，因此不能确认真实 Dify 知识库返回、DashScope ASR/TTS 行为完全符合预期。

## 后续建议

1. 为结构化回答实现字段级翻译，逐字段处理 `title`、`summary`、`scenario_options`、`steps`、`materials`、`warnings`、`detail_text`，并保持 JSON 形状。
2. 如果 DashScope ASR 后续能提供真实检测语种，新增独立 `detectedLanguage` 字段，不复用当前 `language`。
3. 为临时 TTS 入口设计轻量 `spokenText` 生成策略，但应分阶段推进，避免重构主语音链路。
4. 继续补齐 Android 次级页面字符串资源和 mock 数据多语言映射。
5. 后续真实联调时，再用同一用户原始问题分别对比 Dify 控制台与后端 `usage.dify_query`。
