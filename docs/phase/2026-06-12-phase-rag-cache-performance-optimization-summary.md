# Phase: RAG 缓存性能优化阶段总结

日期：2026-06-12

## 本轮目标

- 在 FastAPI 后端新增最小可用的 RAG 答案缓存闭环。
- 对重复政策问答实现 SQLite 精确命中，避免重复调用 Qwen query rewrite 和 Dify blocking 请求。
- 在 `chat_policy` 响应中增加缓存可观测字段，便于排查命中情况和耗时。
- 保持 Android UI、Dify 配置、TTS 音频缓存逻辑和 streaming 策略不变。

## 修改文件

- `backend/app/services/rag_cache_service.py`
  - 新增 SQLite RAG 缓存服务。
  - 实现 query 规范化、cache key 生成、TTL 过期、版本隔离、命中计数和隐私关键词跳过缓存。
- `backend/app/api/routes_chat.py`
  - 在 `chat_policy` 外层增加 cache hit / cache miss 流程。
  - cache hit 时跳过 Qwen query rewrite 和 Dify，请求仍继续走现有 TTS 链路。
  - cache miss 时保持原有主流程，完成 Dify、显示本地化、TTS 后写入 RAG 文本缓存。
  - 写入缓存时排除 `tts`、`tts_rewrite`、`tts_synthesis` 等音频相关字段。
- `backend/app/schemas/chat_schema.py`
  - `ChatPolicyResponse` 新增 `cache_hit`、`cache_key_hash`、`latency_ms`、`source`。
- `backend/app/config.py`
  - 新增 `rag_cache_enabled`、`rag_cache_ttl_seconds`、`rag_prompt_version`、`rag_kb_version`、`rag_cache_db_path`。
- `backend/tests/test_tts_service.py`
  - 为既有 chat_policy 测试默认关闭 RAG 缓存，避免测试间状态污染。
  - 新增缓存命中测试，确认第二次相同问题不会调用 query rewrite 和 Dify。

## 核心实现

### 缓存 key

RAG 文本缓存 key 使用：

```text
normalized_query
display_language
answer_mode
prompt_version
kb_version
```

其中 `answer_mode` 当前固定为 `policy_chat`。`tts_language` 不进入 RAG 文本缓存 key，TTS 音频继续由现有 TTS 缓存独立处理。

### query 规范化

第一版只做轻量规范化：

- 去除首尾空白。
- 合并连续空白。
- 去掉末尾常见中英文标点。
- 英文转小写。
- 不做语义相似匹配。
- 不调用模型做 query normalization。

### cache hit 流程

命中缓存时：

- 读取缓存中的 RAG 文本/结构化结果。
- 不调用 `qwen_text_service.rewrite_query`。
- 不调用 `dify_service.send_chat_message`。
- 使用当前请求的 `conversation_id`、`original_text` 和 `search_query` 覆盖缓存快照中的会话相关字段。
- 继续走现有 TTS rewrite / synthesis 流程。
- 返回 `cache_hit=true`、`source=rag_cache`、`cache_key_hash`、`latency_ms`。

### cache miss 流程

未命中缓存时：

- 继续走原有 Qwen rewrite、Dify blocking、结构化解析、显示本地化、TTS 流程。
- 返回 `cache_hit=false`、`source=dify`、`cache_key_hash`、`latency_ms`。
- 将不含 TTS 音频信息的最终展示结果写入 SQLite 缓存。

### 隐私和安全

第一版通过关键词跳过高风险问题缓存，例如：

- 身份证 / 身份證
- 手机号 / 手機號 / 电话 / 電話
- 地址 / 住址 / 家庭地址
- 银行卡 / 銀行卡
- 姓名是 / 我叫

缓存服务异常不会中断主流程；读取或写入失败时记录 warning，并继续走 Dify 原流程。

## 验证结果

在 `backend` 目录执行：

```powershell
python -m pytest tests
```

结果：

```text
15 passed
```

验证覆盖：

- Dify 结构化回答解析仍通过。
- TTS 缓存和合成测试仍通过。
- chat_policy 原有 TTS 行为仍通过。
- 第二次相同问题命中 RAG 缓存。
- cache hit 时不再调用 query rewrite 和 Dify。
- RAG 缓存内容不包含 `tts`、`tts_rewrite`、`tts_synthesis`。

## 不做内容

- 未修改 Android UI。
- 未修改 Dify 应用配置。
- 未引入 Redis。
- 未做 streaming。
- 未做语义相似缓存。
- 未把 Android 本地历史当作 RAG 缓存。
- 未缓存用户上传音频或 TTS 音频字段到 RAG response_json。

## 剩余风险

- SQLite 适合当前开发和 Demo 阶段；如果后续多进程、高并发或多实例部署，应迁移到 Redis 或集中式存储。
- 当前只做精确规范化命中，表达相近但文字不同的问题不会命中。
- 默认 TTL 为 1 天，政策知识库或 prompt 更新时需要手动更新 `rag_kb_version` 或 `rag_prompt_version`。
- 隐私跳过规则是简单关键词规则，不能覆盖所有个人信息场景。
- 尚未用真实 Dify / DashScope 链路做端到端耗时对比，本轮验证以单元测试为主。

## 后续建议

1. 用真实后端请求验证相同问题的第一次 miss 和第二次 hit，并记录日志中的 `cache_hit`、`source`、`latency_ms`。
2. 为后台运维补充清理缓存脚本或管理接口。
3. 高频 FAQ 可继续推进本地静态答案页，进一步减少不必要的 RAG 请求。
4. 后续再评估 Dify streaming，用于改善首次请求的等待体感。
