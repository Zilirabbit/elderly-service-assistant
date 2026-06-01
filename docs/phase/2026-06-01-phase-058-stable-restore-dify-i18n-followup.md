# Phase 5.8 稳定恢复与 Dify / i18n 纠偏记录

日期：2026-06-01

## 当前阶段边界

- 不启动 Dify。
- 不调用真实 DashScope / Qwen / Dify API。
- 不运行 ADB。
- 不连接真机或模拟器。
- 仅执行静态检查、编译检查、后端语法检查和本地单元测试。

## 已完成

- 已从稳定基线 `379ee0d` 创建恢复分支 `restore-stable-base-2026-06-01`。
- 已保留当前工作备份分支 `backup-before-stable-rollback-2026-06-01`。
- 已恢复稳定版本上的结构化问答、材料清单、语音朗读和基础办理判断能力。
- 已拆分 `displayLanguage` 与 `speechLanguage`：
  - `displayLanguage` 跟随 Android 系统语言，用于界面和回答显示。
  - `speechLanguage` 由用户单独选择，用于 TTS 朗读。
  - ASR 仍保持自动/用户选择识别语种，不绑定显示语言。
- 已修正 Dify 检索口径：
  - Dify 知识库检索现在使用用户原始问题。
  - Qwen 改写后的检索问题仅保存在 `usage.rewritten_search_query` 里，便于排查，不再作为 Dify 入参。
  - `usage.dify_query` 记录真实送入 Dify 的问题，方便与 Dify 控制台对比。
- 已回退结构化 JSON 的二次显示改写：
  - 当 Dify 已返回完整结构化 JSON，且显示语言为简体中文时，后端直接保留 Dify 的 `title`、`summary`、`scenario_options`、`steps`、`materials`、`warnings`、`detail_text` 等字段。
  - 此时不再调用 Qwen `rewrite_display_text` 重写展示文本，避免把知识库字段压平成另一段回答。
  - 前端结构化卡片主标题改为使用 Dify 返回的 `title`，例如“赴香港过关材料准备”。
- 已补充一批高频 Android 字符串资源：
  - 主导航。
  - 首页主要卡片。
  - 智能问答标题、空状态、回答卡片、朗读按钮。
  - 服务页主标题和提示。
  - 材料清单页标题、保存、加载、空状态。
  - 简体中文、繁体中文、英文资源文件均已补齐对应 key。

## 已验证

- `python -m compileall backend/app`：通过。
- `$env:PYTHONPATH='backend'; pytest backend/tests`：10 passed。
- `.\gradlew.bat assembleDebug`：通过，仅保留既有 Material Icons deprecated warning。

## 未完成 / 剩余风险

- 未连接真实 Dify，因此只能保证后端入参口径与 Dify 控制台一致，不能在本阶段确认真实知识库返回完全一致。
- Phase 5.7 的全部结果尚未完整重做：
  - 办理判断问卷内部业务文案仍有大量硬编码中文。
  - 口岸详情、操作指南、字体设置、个人中心、语音面板等次级页面仍需继续资源化。
  - 本地 mock 材料数据仍是中文内容，尚未建立多语言数据源或显示层转换。
- 结构化回答里的材料名、政策原文和知识库来源标题仍取决于 Dify 返回内容；如果 Dify 知识库只存中文，英文/繁中显示后续需要做“结构化字段级翻译”，不能再用整段重写替代字段。

## 后续建议

1. 手动用同一个原始问题分别在 Dify 控制台和本后端接口测试，确认 `usage.dify_query` 与控制台输入一致。
2. 继续 Phase 5.7 补齐，把 `ElderCareScreens.kt` 中剩余可见中文分批迁移到 `strings.xml`。
3. 为办理判断结果、材料清单 mock 数据建立多语言映射，避免只翻译按钮但业务内容仍是中文。
4. 后续真实服务验证时再执行联调命令，不在当前阶段自动启动服务或调用真实 API。
