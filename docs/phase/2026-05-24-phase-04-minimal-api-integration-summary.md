# Phase 04 最小接口联调完成总结

> 日期：2026-05-24
> 阶段：后端网关与 Android 前端通信打通
> 目标：完成 Android App -> FastAPI 后端 -> Dify 知识库问答 -> Android 展示回答的最小闭环

## 1. 本阶段完成内容

本阶段已从“Android 静态 UI + Dify API 单独验证”推进到“真机 App 可调用真实后端并展示 AI 回答”。

已完成的核心链路：

```text
华为 nova 7 真机 App
  -> Windows FastAPI 后端：0.0.0.0:8080
  -> VM Dify 服务：http://192.168.221.131
  -> Dify 知识库 + Qwen
  -> FastAPI 返回 answer/sources/usage
  -> App 首页展示 AI 回复
```

关键实现：

- 新增 FastAPI 后端网关，提供 `GET /health` 和 `POST /api/v1/chat-policy`。
- 后端通过环境变量读取 Dify 地址和 API Key，真实 Key 不进入 Android 代码。
- 后端封装 Dify `/v1/chat-messages` blocking 调用，并提取 `answer`、`conversation_id`、`sources`、`usage`。
- Android 增加网络权限、明文 HTTP 开发配置、Retrofit/OkHttp 网络层、请求/响应模型和 `ChatViewModel`。
- 首页输入框、发送按钮、FAQ 点击已接入真实问答；支持 loading、错误提示和回答展示。
- 真机联调时 `ApiClient` 当前使用电脑 WLAN 地址：`http://172.21.77.249:8080/`。

## 2. 重要联调步骤

后端环境：

```powershell
conda activate elderly-backend
cd C:\Users\86136\Desktop\work\elderly-service-assistant\backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

后端 `.env` 关键配置：

```env
DIFY_BASE_URL=http://192.168.221.131
DIFY_CHAT_PATH=/v1/chat-messages
DIFY_TIMEOUT_SECONDS=60
DEFAULT_USER_ID=demo-user-001
```

本机健康检查：

```cmd
curl.exe --noproxy "*" http://127.0.0.1:8080/health
```

政策问答验证：

```cmd
curl.exe --noproxy "*" -X POST "http://127.0.0.1:8080/api/v1/chat-policy" -H "Content-Type: application/json" -d "{\"message\":\"\u6e2f\u6fb3\u901a\u884c\u8bc1\u7eed\u7b7e\u9700\u8981\u4ec0\u4e48\u6750\u6599\uff1f\",\"conversation_id\":\"\",\"user_id\":\"demo-user-001\"}"
```

真机访问检查：

```text
http://172.21.77.249:8080/health
```

## 3. 验证结果

已验证通过：

- `GET /health` 返回：

```json
{"status":"ok","service":"yue-tongxin-backend"}
```

- `POST /api/v1/chat-policy` 能返回非空 `answer`、`conversation_id`、知识库 `sources` 和 `usage`。
- Dify 命中知识库文档，例如：
  - `02_港澳签注办理与续签说明.md`
  - `01_港澳通行证办理指南.md`
- 华为 nova 7 真机 App 可成功展示 `AI 回复` 卡片。

本阶段遇到并解决的问题：

- 本机 curl 被系统 HTTP/HTTPS 代理接管，导致 localhost 请求返回 502；验证本地接口时使用 `--noproxy "*"`。
- 后端 `httpx` 自动读取 SOCKS 代理，因缺少 `socksio` 导致 500；已在 Dify 调用中设置 `trust_env=False`。
- `.env` 初始 `DIFY_BASE_URL=http://localhost` 不正确；已改为 VM 地址 `http://192.168.221.131`。
- Android 官方模拟器多次因 QEMU/AEHD/GPU 环境问题终止；改用华为 nova 7 真机完成联调。
- 真机不能使用模拟器专用 `10.0.2.2`；已改为电脑 WLAN IP。

## 4. 下一步建议

优先建议：

- 对 App 展示的 Markdown 进行基础清理或渲染，避免直接显示 `**标题**`、`*` 等符号。
- 将后端错误日志补充为可排查但不泄露 Key 的结构化日志。
- 补充 Android 端错误分类：网络不可达、后端 502、超时、空回答分别提示。
- 将真机/模拟器后端地址切换整理成更轻量的说明或本地常量，避免误用 `10.0.2.2`。

后续阶段建议：

- Phase 05：接入材料清单真实生成，新增结构化材料清单接口。
- Phase 06：接入语音输入与 Android TTS 播报。
- 演示准备：固定一组测试问题、保留成功截图和后端日志，但不要暴露 Dify API Key。

## 5. 改进空间

- 当前 `BASE_URL` 仍是手动常量，适合 Demo，但后续可改为 Gradle 配置、构建变体或运行时设置页。
- 当前 Android 直接展示模型返回文本，缺少 Markdown/段落排版优化。
- 后端错误兜底较粗，后续可区分连接失败、鉴权失败、Dify 非 JSON、模型超时等场景。
- 当前接口未加入 CORS、鉴权、限流和请求日志脱敏；正式部署前需要补齐。
- 当前仅验证单轮问答，多轮 `conversation_id` 的连续对话体验还未完整测试。
