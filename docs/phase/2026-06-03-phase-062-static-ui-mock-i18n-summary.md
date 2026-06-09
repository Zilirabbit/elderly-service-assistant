# Phase 6.2 静态 UI 壳与本地 Mock 数据 i18n 总结

日期：2026-06-03

## 本轮目标

- 本轮只处理 Android App 内部可控的静态 UI 壳文案和本地 Mock 展示文案。
- 不处理 Dify / RAG 返回正文的字段级翻译。
- 不修改 Dify / RAG / ASR / TTS 主链路。

## 修改范围

- 三套目标资源文件已同步：
  - `values/strings.xml`
  - `values-zh-rHK/strings.xml`
  - `values-en/strings.xml`
- 当前三套 `strings.xml` 均为 288 个 string key。

已覆盖页面和区域：

- 首页口岸区域
- 首页 FAQ 区域
- 服务页
- 过关材料准备入口页
- 材料清单页
- 口岸详情页

## Mock 数据处理

- 首页口岸 Mock 已拆分为内部稳定 `id` + 展示资源 id。
- 首页 FAQ 已拆分为内部稳定 `id` / `categoryId` + 展示 `titleResId` + 稳定中文 `query`。
- 服务卡片已拆分为内部稳定 `id` + `titleResId` / `descriptionResId`。
- 材料清单本地 Mock 已补充 `titleResId`、`tipResIds`、`nameResId`、`descriptionResId`、`linkedActionLabelResId` 等展示资源字段。
- 点击 FAQ、快捷问题等进入问答时，仍保持稳定中文 query，不随 UI 语言切换改成英文或繁体。

## 保持不变

- 未修改 Dify 入参。
- 未把 `rewritten_search_query` 作为 Dify 入参。
- 未修改 RAG / Dify / ASR / TTS 主链路。
- 未启动真实 Dify、DashScope、Qwen 或 ADB。
- 未把 Dify 返回正文写入 Android `strings.xml`。
- 未重新绑定 `speechLanguage` 与 `displayLanguage`。

## 未处理内容

- Dify 返回的 `answer` / `display_text` / `structured_answer` 字段内容本轮不处理。
- RAG 动态正文不在 Android 资源文件中硬翻译。
- 后续应通过后端 `structured_answer` 字段级翻译处理动态内容，而不是在 Android 端用 `strings.xml` 兜底。

## 验证

已在 `ElderCareApp` 目录运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：通过。

仅保留既有 Material Icons deprecated warnings，与本轮 i18n 修复无关。

