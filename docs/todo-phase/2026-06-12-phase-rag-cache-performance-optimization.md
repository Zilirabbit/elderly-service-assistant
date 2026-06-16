# Phase：RAG 返回速度优化与后端缓存改造方案

## 1. 背景

当前项目为“粤同心-湾区中老年助手”，核心链路为：

```text
Android App
→ FastAPI 后端
→ Qwen query rewrite
→ Dify RAG blocking 请求
→ 展示语言本地化
→ TTS rewrite / TTS 合成
→ Android 渲染
```

根据当前代码审计结果，项目已经实现了部分缓存能力：

- 后端已实现 **TTS 音频文件缓存**
- Android 已实现 **本地问答历史保存**
- 首页 FAQ 已实现 **本地静态问题列表**

但当前尚未实现真正能加速 RAG 的缓存能力：

- 未实现 RAG 答案缓存
- 未实现 Dify 调用缓存
- 未实现 `cache_hit` / `cache_miss` 字段
- 未实现 RAG 缓存 TTL
- 未实现 `prompt_version` / `kb_version` 等版本化缓存 key
- Dify 请求仍使用 `"response_mode": "blocking"`
- 首页 FAQ 虽然是本地问题列表，但点击后仍会直接触发 RAG 请求

因此，用户每次询问政策类问题时，基本都会重新走完整链路，导致等待时间较长。

---

## 2. 当前问题总结

### 2.1 主要性能瓶颈

当前 RAG 返回慢主要来自以下几个环节：

```text
用户提问
→ Qwen query rewrite
→ Dify blocking 请求
→ 知识库检索
→ 大模型生成完整答案
→ 展示语言本地化
→ TTS rewrite / TTS 合成
→ 返回 Android
```

其中最影响体感速度的是：

1. **Dify 使用 blocking 模式**
   - 用户必须等待完整结果生成后才能看到回答。
   - 即使模型已经开始生成，Android 端也不会逐步显示。

2. **没有 RAG 答案缓存**
   - 相同问题重复询问时，仍然重新请求 Dify。
   - 高频 FAQ 也不能秒回。

3. **首页 FAQ 点击仍触发 RAG**
   - FAQ 当前只是“快捷提问入口”，不是“本地答案详情页”。
   - 用户点击高频问题仍然要等待 Dify 生成。

4. **RAG 生成内容较长**
   - 当前回答往往包含结论、材料、步骤、注意事项、来源等内容。
   - 生成 token 多，耗时自然较长。

5. **本地化与 TTS 可能额外增加耗时**
   - 非简体中文展示语言下，可能存在字段级本地化处理。
   - 如果需要朗读，还会继续走 TTS 链路。

---

## 3. 改造目标

本阶段目标不是一次性重构整套 RAG 架构，而是先实现一个**最小、稳定、可验证**的后端 RAG 缓存闭环。

### 3.1 本阶段目标

实现：

- FastAPI 后端 RAG 答案缓存
- 缓存命中 / 未命中判断
- 响应返回 `cache_hit`
- 响应返回 `latency_ms`
- 响应返回 `source`
- 支持 TTL 过期
- 支持 `prompt_version` / `kb_version` 控制缓存失效
- 不影响现有 Android 端解析
- 不改变现有 Dify 配置
- 不引入 Redis
- 不做 streaming
- 不改 Android UI

### 3.2 本阶段不做

本阶段暂不处理：

- 不做 Redis
- 不做语义相似缓存
- 不做 Dify streaming
- 不做 Android 流式显示
- 不改首页 FAQ 页面结构
- 不改 Dify 应用配置
- 不缓存用户上传音频
- 不缓存包含明显个人隐私的自由问答
- 不把 Android 本地历史当作 RAG 缓存

---

## 4. 当前缓存状态

| 类型                        | 当前状态 | 是否能加速 RAG                   |
| --------------------------- | -------- | -------------------------------- |
| TTS 音频缓存                | 已实现   | 只能加速朗读，不加速 RAG         |
| Android 本地历史            | 已实现   | 不能加速 RAG，只用于查看历史     |
| 首页 FAQ 问题列表           | 已实现   | 不能加速 RAG，因为点击仍触发提问 |
| RAG 答案缓存                | 未实现   | 需要新增                         |
| Dify 调用缓存               | 未实现   | 需要新增                         |
| cache_hit 字段              | 未实现   | 需要新增                         |
| latency_ms 字段             | 未实现   | 需要新增                         |
| TTL                         | 未实现   | 需要新增                         |
| prompt_version / kb_version | 未实现   | 需要新增                         |
| streaming                   | 未实现   | 后续阶段再做                     |

---

## 5. 推荐总体方案

### 5.1 分层原则

本项目缓存建议分三层：

```text
Android 本地：
- 问答历史
- 首页 FAQ 静态展示
- 操作指南
- 最近一次材料清单
- 后期可选：本地音频文件缓存

FastAPI 后端：
- RAG 最终答案缓存
- cache_hit / latency_ms
- TTL
- prompt_version / kb_version
- TTS 音频缓存
- 后期可选：分段耗时统计

Dify：
- 知识库检索
- RAG 生成
- 后期可选：Annotation Reply
- 后期可选：Top K / Score Threshold / Rerank 调整
- 后期可选：Streaming
```

### 5.2 本阶段优先实现

本阶段只做：

```text
FastAPI RAG 答案缓存
```

不动 Android、不动 Dify、不做 streaming。

---

## 6. 缓存设计

### 6.1 缓存位置

开发阶段使用 SQLite。

数据库路径：

```text
backend/app/data/rag_cache.sqlite3
```

新增缓存服务文件：

```text
backend/app/services/rag_cache_service.py
```

---

## 7. 缓存 key 设计

### 7.1 key 组成

RAG 文本缓存 key 应包含：

```text
normalized_query
display_language
answer_mode
prompt_version
kb_version
```

最终生成方式：

```text
cache_key = sha256(
  normalized_query + "|" +
  display_language + "|" +
  answer_mode + "|" +
  prompt_version + "|" +
  kb_version
)
```

### 7.2 不把 tts_language 放进 RAG 缓存 key

不要把 `tts_language` 放进 RAG 文本缓存 key。

原因：

- RAG 文本答案和 TTS 音色是两件事。
- 同一段中文答案，可以用普通话、粤语、英语等不同语音朗读。
- 项目已经有独立的 TTS 音频缓存。
- 如果把 `tts_language` 放进 RAG key，同一个政策答案会因为朗读语言不同生成多份缓存，浪费空间。

正确关系应该是：

```text
RAG 文本缓存：
normalized_query + display_language + answer_mode + prompt_version + kb_version

TTS 音频缓存：
text + language + voice + model + format + sample_rate + speed + volume
```

### 7.3 answer_mode

第一版可以先固定为：

```text
policy_chat
```

后续如果区分不同回答模式，可以扩展为：

```text
short_answer
structured_policy_card
material_checklist
faq_answer
```

---

## 8. query 规范化设计

第一版只做轻量规范化，不做语义相似匹配。

### 8.1 规范化规则

对用户输入问题做：

1. 去除首尾空格
2. 合并连续空白字符
3. 去掉末尾常见标点
4. 英文转小写
5. 保留中文原语义
6. 不做复杂 query rewrite
7. 不调用模型做规范化

### 8.2 示例

原问题：

```text
港澳通行证续签需要什么材料？
```

规范化后：

```text
港澳通行证续签需要什么材料
```

原问题：

```text
  请问港澳通行证续签需要什么材料？？
```

规范化后：

```text
请问港澳通行证续签需要什么材料
```

注意：第一版不要求这两个问题一定命中同一个缓存，因为它们规范化后仍不完全相同。

语义相似命中属于后续优化，可以通过：

- Dify Annotation Reply
- 向量语义缓存
- FAQ 标准问答表

来实现。

---

## 9. 数据库表设计

新增表：

```sql
CREATE TABLE IF NOT EXISTS rag_cache (
    cache_key TEXT PRIMARY KEY,
    cache_key_hash TEXT NOT NULL,
    normalized_query TEXT NOT NULL,
    display_language TEXT NOT NULL,
    answer_mode TEXT NOT NULL,
    prompt_version TEXT NOT NULL,
    kb_version TEXT NOT NULL,
    response_json TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    hit_count INTEGER NOT NULL DEFAULT 0
);
```

可选增加索引：

```sql
CREATE INDEX IF NOT EXISTS idx_rag_cache_expires_at
ON rag_cache(expires_at);

CREATE INDEX IF NOT EXISTS idx_rag_cache_created_at
ON rag_cache(created_at);
```

---

## 10. 缓存内容设计

### 10.1 应该缓存什么

缓存 RAG 生成后的最终展示内容。

建议缓存：

```json
{
  "answer": "...",
  "display_text": "...",
  "structured": {},
  "sources": [],
  "display_language": "zh-CN",
  "answer_mode": "policy_chat",
  "prompt_version": "prompt_v1",
  "kb_version": "kb_202606"
}
```

### 10.2 不应该缓存什么

不要缓存：

- TTS 音频字段
- 用户上传音频文件
- 临时请求状态
- 当前请求耗时
- 明显包含个人隐私的自由问答
- Android 本地 UI 状态

特别是不要把下面内容塞进 RAG 缓存：

```json
{
  "tts": {
    "audioUrl": "...",
    "cached": true
  }
}
```

TTS 已经有独立缓存，不应该和 RAG 文本缓存混在一起。

---

## 11. chat_policy 改造流程

### 11.1 当前流程

当前大致流程：

```text
1. 接收 Android 请求
2. 归一化 display_language
3. Qwen query rewrite
4. 请求 Dify blocking
5. 解析 Dify 返回
6. 展示语言本地化
7. TTS rewrite / TTS 合成
8. 返回 Android
```

### 11.2 改造后流程

新增缓存后：

```text
1. 接收 Android 请求
2. 归一化 display_language
3. 生成 normalized_query
4. 生成 cache_key
5. 查询 RAG 缓存

如果 cache hit：
    6. 读取缓存 response_json
    7. 不调用 Qwen query rewrite
    8. 不调用 Dify
    9. 如当前请求需要 TTS，继续走现有 TTS 服务
    10. 返回 Android，cache_hit=true，source=rag_cache

如果 cache miss：
    6. 走原有流程：Qwen query rewrite
    7. 请求 Dify blocking
    8. 解析 Dify 返回
    9. 展示语言本地化
    10. 将 RAG 文本/结构化结果写入缓存
    11. 如当前请求需要 TTS，继续走现有 TTS 服务
    12. 返回 Android，cache_hit=false，source=dify
```

---

## 12. 响应模型修改

修改：

```text
backend/app/schemas/chat_schema.py
```

在 `ChatPolicyResponse` 中新增调试字段：

```python
cache_hit: bool = False
cache_key_hash: str | None = None
latency_ms: int | None = None
source: str | None = None
```

字段含义：

| 字段           | 含义                             |
| -------------- | -------------------------------- |
| cache_hit      | 本次是否命中 RAG 缓存            |
| cache_key_hash | 缓存 key 的短 hash，用于日志排查 |
| latency_ms     | 后端本次总耗时                   |
| source         | `rag_cache` 或 `dify`            |

注意：

- Android 暂时可以不展示这些字段。
- 新增字段应保持向后兼容，不破坏现有 Android 解析。
- 如果 Android 使用 Kotlin data class 严格匹配，需要同步增加可空字段或确认 Retrofit/Gson/Moshi 是否忽略未知字段。

---

## 13. 配置项设计

优先在项目现有配置文件中增加。

如果当前项目已有：

```text
backend/app/core/config.py
```

或类似 settings 文件，则加入：

```python
RAG_CACHE_ENABLED = True
RAG_CACHE_TTL_SECONDS = 86400
RAG_PROMPT_VERSION = "prompt_v1"
RAG_KB_VERSION = "kb_202606"
RAG_CACHE_DB_PATH = "backend/app/data/rag_cache.sqlite3"
```

如果暂时没有统一配置，也可以先在 `rag_cache_service.py` 中集中定义默认值，但需要方便后续迁移。

### 13.1 默认 TTL

第一版建议：

```text
RAG_CACHE_TTL_SECONDS = 86400
```

即缓存 1 天。

原因：

- 政务政策类内容不能长期无脑缓存。
- 项目开发阶段知识库和 prompt 可能经常变。
- 1 天 TTL 既能看到明显加速，又不容易长期返回旧答案。

### 13.2 版本控制

当知识库或提示词更新时，可以手动修改：

```text
RAG_PROMPT_VERSION=prompt_v2
RAG_KB_VERSION=kb_20260615
```

这样旧缓存自动不会命中。

---

## 14. 日志要求

在 `chat_policy` 日志中增加：

```text
cache_hit
cache_key_hash
latency_ms
source
display_language
answer_mode
prompt_version
kb_version
```

示例日志：

```text
[chat_policy] cache_hit=false source=dify cache_key_hash=8fa21c9d latency_ms=12680 display_language=zh-CN answer_mode=policy_chat prompt_version=prompt_v1 kb_version=kb_202606
```

缓存命中后示例：

```text
[chat_policy] cache_hit=true source=rag_cache cache_key_hash=8fa21c9d latency_ms=420 display_language=zh-CN answer_mode=policy_chat prompt_version=prompt_v1 kb_version=kb_202606
```

---

## 15. 安全与准确性限制

### 15.1 第一版只缓存普通政策问答

适合缓存：

- 港澳通行证续签材料
- 首次办理方式
- 自助机续签说明
- 过关材料准备
- 口岸通关注意事项
- 老年人办理建议
- 高频 FAQ

不建议缓存：

- 用户上传文件后的个性化总结
- 用户包含身份证号、手机号、住址等信息的问题
- 明显带有个人隐私的信息
- 一次性临时问题
- 录音文件内容
- 家属确认信息

### 15.2 简单隐私过滤

第一版可以先通过简单规则避免缓存高风险问题。

例如，如果 query 包含以下明显个人信息特征，可以跳过缓存：

```text
身份证
手机号
电话
住址
家庭地址
银行卡
姓名是
我叫
```

也可以先只缓存 `policy_chat` 模式下的普通问题。

---

## 16. 修改文件范围

### 16.1 新增文件

```text
backend/app/services/rag_cache_service.py
```

可选新增：

```text
backend/app/data/rag_cache.sqlite3
```

该文件运行时生成，不建议手动提交到 Git。

### 16.2 修改文件

```text
backend/app/api/routes_chat.py
backend/app/schemas/chat_schema.py
backend/app/core/config.py
```

如果配置文件名称不同，以当前项目实际文件为准。

### 16.3 不修改文件

本阶段不修改：

```text
ElderCareApp Android UI 文件
Dify 应用配置
Dify 知识库配置
TTS 缓存逻辑
ChatHistoryStorage.kt
FaqData.kt
```

---

## 17. Codex 执行要求

请 Codex 按以下要求执行：

```text
请基于当前项目做最小改造，实现 FastAPI 后端 RAG 答案缓存。

要求：
1. 不改 Android UI。
2. 不改 Dify 配置。
3. 不引入 Redis。
4. 不做 streaming。
5. 使用 Python 标准库 sqlite3。
6. 保持现有 chat_policy 主流程不变，只在外层增加缓存命中/写入逻辑。
7. TTS 音频缓存继续使用现有实现。
8. RAG 文本缓存不要把 tts_language 放进 key。
9. 响应增加 cache_hit、cache_key_hash、latency_ms、source。
10. 日志中记录 cache_hit、source、latency_ms、cache_key_hash。
11. 第一版只做精确规范化命中，不做语义缓存。
12. 缓存命中时不能再调用 Qwen query rewrite 和 Dify。
13. 缓存未命中时保持原有流程。
14. 完成后输出修改文件列表和验证方法。
```

---

## 18. 验证方法

### 18.1 第一次请求

请求：

```text
港澳通行证续签需要什么材料？
```

预期：

```json
{
  "cache_hit": false,
  "source": "dify"
}
```

后端日志中应该能看到：

```text
cache_hit=false
source=dify
```

并且会出现 Dify 请求。

### 18.2 第二次请求

再次请求相同问题：

```text
港澳通行证续签需要什么材料？
```

预期：

```json
{
  "cache_hit": true,
  "source": "rag_cache"
}
```

后端日志中应该能看到：

```text
cache_hit=true
source=rag_cache
```

并且不应该再次调用：

```text
qwen_text_service.rewrite_query
dify_service.send_chat_message
```

### 18.3 耗时对比

预期效果：

| 请求次数 | source    | cache_hit | 预期耗时           |
| -------- | --------- | --------- | ------------------ |
| 第一次   | dify      | false     | 5–20 秒            |
| 第二次   | rag_cache | true      | 1 秒以内或明显更快 |

### 18.4 TTL 验证

将配置临时改为：

```text
RAG_CACHE_TTL_SECONDS=10
```

测试：

1. 第一次请求，未命中
2. 第二次请求，命中
3. 等待超过 10 秒
4. 第三次请求，应重新走 Dify

### 18.5 版本失效验证

修改：

```text
RAG_PROMPT_VERSION=prompt_v2
```

或：

```text
RAG_KB_VERSION=kb_20260615
```

预期：

- 旧缓存不会命中
- 新请求重新走 Dify
- 新结果写入新版本缓存

---

## 19. 手动清空缓存方法

如果需要清空 RAG 缓存，可以删除：

```text
backend/app/data/rag_cache.sqlite3
```

也可以后续增加管理脚本：

```text
backend/scripts/clear_rag_cache.py
```

第一版可以暂时不做脚本。

---

## 20. 后续阶段规划

### P0：当前阶段

完成：

- FastAPI RAG 答案缓存
- `cache_hit`
- `latency_ms`
- `source`
- TTL
- `prompt_version`
- `kb_version`

### P1：FAQ 本地答案页

当前首页 FAQ 只是问题列表，点击后会直接提交到 RAG。

后续建议改为：

```text
点击 FAQ
→ 打开本地静态答案详情页
→ 页面底部提供“继续追问”
→ 用户点击后再进入 RAG 问答页
```

这样首页高频问题可以做到秒开，尤其适合中老年用户。

### P2：Dify streaming

后续将 Dify 调用从：

```json
{
  "response_mode": "blocking"
}
```

改为可配置：

```json
{
  "response_mode": "streaming"
}
```

再配合 Android 端流式显示。

目标是：

```text
用户点击提问
→ 1~3 秒出现第一段
→ 后续内容逐步展示
```

streaming 主要改善体感等待，不一定减少总生成耗时。

### P3：分段耗时统计

后续可以记录：

```text
query_rewrite_ms
dify_ms
localization_ms
tts_rewrite_ms
tts_synthesize_ms
total_latency_ms
```

这样能明确知道慢在哪里。

### P4：Dify Annotation Reply

对于高频标准问题，可以在 Dify 中配置 Annotation Reply。

适合：

- 港澳通行证续签需要什么材料？
- 首次办理是否必须线下？
- 可以自助机续签吗？
- 过关需要带什么？
- 老人不会预约怎么办？

Annotation Reply 更像“人工维护的 FAQ 标准答案库”，不是自动缓存，但适合高频问题秒回。

---

## 21. 风险与注意事项

### 21.1 政策类内容不能永久缓存

本项目涉及政务办理信息，缓存需要考虑时效性。

建议：

- 默认 TTL 1 天
- 知识库更新后修改 `kb_version`
- prompt 更新后修改 `prompt_version`
- 不要永久缓存所有答案

### 21.2 不要把历史记录当缓存

Android 本地历史记录只表示：

```text
用户之前看过什么
```

RAG 缓存表示：

```text
相同问题再次请求时，不再调用 Dify，直接返回已有答案
```

两者用途不同，不能混淆。

### 21.3 不要缓存 TTS 到 RAG response_json

TTS 已经有独立音频缓存。

RAG 缓存只存文本和结构化内容。

### 21.4 不要过早做语义缓存

语义缓存容易出现：

```text
两个问题看起来相似，但政策含义不同
```

例如：

```text
首次办理港澳通行证需要什么材料？
```

和：

```text
续签港澳通行证需要什么材料？
```

它们都在问材料，但办理类型不同，不能随便命中同一个缓存。

第一版只做精确规范化缓存，更安全。

---

## 22. 最终预期效果

改造完成后：

### 第一次问

```text
港澳通行证续签需要什么材料？
```

流程：

```text
Android
→ FastAPI
→ cache miss
→ Qwen rewrite
→ Dify
→ localization
→ 写入 RAG 缓存
→ TTS
→ 返回
```

返回：

```json
{
  "cache_hit": false,
  "source": "dify",
  "latency_ms": 12800
}
```

### 第二次问

```text
港澳通行证续签需要什么材料？
```

流程：

```text
Android
→ FastAPI
→ cache hit
→ 读取 RAG 缓存
→ TTS
→ 返回
```

返回：

```json
{
  "cache_hit": true,
  "source": "rag_cache",
  "latency_ms": 420
}
```

### 关键收益

- 重复问题明显加速
- 高频 FAQ 后续可进一步秒开
- 后端可观测性增强
- 可以明确区分“第一次慢”和“重复问题仍然慢”
- 为后续 streaming、FAQ 本地答案页、Redis 缓存打基础

---

## 23. 本阶段结论

当前项目不是“缓存不完善”，而是：

```text
RAG 层尚未实现缓存。
```

本阶段最小、稳妥、收益明显的改造是：

```text
在 FastAPI 后端新增 SQLite RAG 答案缓存，
并在 chat_policy 响应中增加 cache_hit、latency_ms、source 等字段。
```

优先顺序建议为：

```text
1. FastAPI RAG 答案缓存
2. cache_hit / latency_ms 可观测字段
3. 首页 FAQ 本地答案详情页
4. Dify streaming
5. Redis / 语义缓存 / Annotation Reply
```
