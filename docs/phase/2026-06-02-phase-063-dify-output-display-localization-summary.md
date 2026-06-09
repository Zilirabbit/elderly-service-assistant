# Phase 6.3 Dify 输出内容 displayLanguage 本地化总结

日期：2026-06-02

## 本轮修改目标

- 在后端新增 Dify / RAG 输出内容本地化层，让动态回答内容最终跟随 Android 传入的 `displayLanguage` 展示。
- 只处理 Dify 返回之后的展示内容，不改变 Dify 查询输入。
- 对有效 `structured_answer` 做字段级翻译，避免把结构化卡片整段改写成普通文本。
- 对普通 `answer` 只生成本地化后的 `display_text`，保留 Dify 原始 `answer` 便于排查。
- 翻译失败时回退原始 Dify 内容，不让问答接口因为本地化失败而失败。

## 修改文件列表

- `backend/app/services/display_localization_service.py`
  - 新增 Dify 输出展示本地化服务。
- `backend/app/services/qwen_text_service.py`
  - 新增 `translate_structured_answer`，用于结构化回答字段级 JSON 翻译。
  - 新增结构化本地化 prompt，要求保持 JSON 结构和字段名。
- `backend/app/api/routes_chat.py`
  - 接入 `localize_display_answer`。
  - 调整 `answer` / `display_text` / `structured_answer` 的返回分工。
- `backend/tests/test_tts_service.py`
  - 扩展 fake Qwen 服务和 chat-policy 测试，覆盖结构化翻译、普通显示翻译和失败回退。

## display_localization_service 行为

新增 `localize_display_answer` 作为后端 Dify 输出本地化入口。

核心行为：

- `displayLanguage = zh-CN`
  - 不调用本地化翻译。
  - 直接返回 Dify 原始解析后的展示内容。
  - `usage.display_localized = false`。
  - `usage.localization_mode = "none"`。
- 有效 `structured_answer` 且 `displayLanguage = zh-HK / en`
  - 调用 `qwen_text_service.translate_structured_answer`。
  - 将当前 `StructuredAnswer` 转为 JSON dict 后交给 Qwen。
  - 解析 Qwen 返回 JSON，并用 `StructuredAnswer.model_validate` 校验。
  - 成功后返回本地化后的 `structured_answer` 和对应 `display_text`。
- 非结构化普通回答且 `displayLanguage = zh-HK / en`
  - 调用既有 `rewrite_display_text`。
  - 只生成本地化后的 `display_text`。
  - 不改 Dify 原始 `answer`。
- 翻译失败
  - 回退到原始 `structured_answer` 或原始普通展示文本。
  - 在 `usage.localization_error` 记录错误类型。

## routes_chat.py 字段分工

本轮调整后，`/api/v1/chat-policy` 返回字段分工如下：

- `answer`
  - 保留 Dify 原始 `answer`。
  - 用于排查 Dify 原始输出有没有变化。
- `display_text`
  - 面向 Android 展示的最终文本。
  - 普通回答时，这里是按 `displayLanguage` 本地化后的展示文本。
  - 结构化回答时，这里优先取本地化后 `structured_answer.detail_text`，其次取 `summary`。
- `structured_answer`
  - 面向结构化卡片展示。
  - `zh-CN` 下保留 Dify 原始解析结果。
  - `zh-HK / en` 下做字段级翻译后的结构化结果。
- `tts.text`
  - 仍基于最终 `display_text` 继续走 TTS 文本改写。
  - 本轮未修改 ASR / TTS 主链路。

## Dify 入参保持不变

本轮没有修改 Dify 调用参数。

仍保持：

```text
search_query = original_text
usage.dify_query = search_query
```

也就是说，真实送入 Dify 的仍是用户原始问题。

## rewritten_search_query 仍只用于 usage 排查

Qwen 查询改写结果仍保存在：

```text
usage.rewritten_search_query
```

它只用于排查，不作为 Dify 入参。

本轮继续保持：

- 不把 `rewritten_search_query` 传给 Dify。
- 不让 `displayLanguage = en / zh-HK` 改变 Dify query。
- 不把英文或繁体展示语言反向影响 RAG 检索输入。

## structured_answer 字段级翻译策略

当 Dify 返回有效 `structured_answer`，且 `displayLanguage` 为 `zh-HK` 或 `en` 时，采用字段级翻译。

当前最小实现基于 `StructuredAnswer` schema，覆盖字段包括：

- `title`
- `summary`
- `scenario_options`
- `steps`
- `materials.required`
- `materials.optional`
- `warnings`
- `detail_text`
- `source_note`

翻译要求：

- 保持 JSON 字段名不变。
- 保持对象结构不变。
- 保持数组顺序不变。
- 保持布尔值、数字值不变。
- 不新增字段。
- 不删除字段。
- 不把结构化回答压平成普通段落。

## 普通 answer 的 display_text 翻译策略

当 Dify 没有返回有效 `structured_answer`，只有普通 `answer` 或展示文本时：

- `answer` 保留 Dify 原始回答。
- `display_text` 走 `rewrite_display_text` 翻译为目标展示语言。
- `usage.localization_mode = "plain_answer_translation"`。
- 若翻译成功，`usage.display_localized = true`。

这样可以同时保留：

- Dify 原始回答。
- 面向用户展示的本地化文本。
- 本地化调用 usage。

## 翻译失败回退策略

本轮新增的本地化层不允许影响问答主接口稳定性。

结构化字段翻译失败时：

- 回退原始 `structured_answer`。
- `display_text` 回退原始结构化展示文本。
- `usage.display_localized = false`。
- `usage.localization_mode = "structured_field_translation"`。
- `usage.localization_error` 记录错误类型。

普通回答显示翻译失败时：

- 回退原始普通展示文本。
- `answer` 仍保留 Dify 原始回答。
- `usage.display_localized = false`。
- `usage.localization_mode = "plain_answer_translation"`。
- `usage.localization_error` 记录错误类型。

## 测试结果

未启动真实 Dify / DashScope / ADB。

已执行：

```powershell
python -m compileall backend\app
```

结果：通过。

已执行：

```powershell
$env:PYTHONPATH='backend'; pytest backend\tests
```

结果：通过，`14 passed`。

## 剩余风险

- `StructuredAnswer` schema 仍是固定结构，当前字段级翻译只能覆盖现有卡片字段。
- 尚未连接真实 Dify / DashScope 做联调，只完成静态实现和 fake 服务测试。
- 如果后续 Dify 返回更复杂的嵌套 JSON，例如对象数组、按钮动作、来源链接、跳转字段，需要扩展 schema 或增加更通用的递归白名单 / 黑名单翻译策略。
- 复杂嵌套字段中的 `id`、`action`、`route`、`url`、`source_url`、`query` 等内部字段需要继续保持不可翻译。
