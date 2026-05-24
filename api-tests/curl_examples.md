# 最小接口联调 curl 示例

> 适用阶段：Phase 04 最小接口联调
> 使用前提：FastAPI 后端已启动，并且后端 `.env` 已配置 Dify 地址和 API Key

---

## 1. 启动后端

```powershell
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

---

## 2. 健康检查

### Windows PowerShell

```powershell
curl.exe http://localhost:8080/health
```

### macOS / Linux

```bash
curl http://localhost:8080/health
```

### 期望响应

```json
{
  "status": "ok",
  "service": "yue-tongxin-backend"
}
```

---

## 3. 政策问答

### Windows PowerShell

```powershell
curl.exe -X POST "http://localhost:8080/api/v1/chat-policy" `
  -H "Content-Type: application/json" `
  -d "{\"message\":\"港澳通行证续签需要什么材料？\",\"conversation_id\":\"\",\"user_id\":\"demo-user-001\"}"
```

### macOS / Linux

```bash
curl -X POST "http://localhost:8080/api/v1/chat-policy" \
  -H "Content-Type: application/json" \
  -d '{
    "message": "港澳通行证续签需要什么材料？",
    "conversation_id": "",
    "user_id": "demo-user-001"
  }'
```

### 最小成功响应示例

```json
{
  "answer": "办理港澳通行证续签通常需要准备有效身份证件、原港澳通行证以及与签注类型相关的材料。具体以当地出入境窗口或官方平台要求为准。",
  "conversation_id": "97e020aa-9b1b-4000-8448-1f54ceca4d13",
  "sources": [],
  "usage": {}
}
```

说明：真实响应内容以 Dify 知识库和模型返回为准。`answer` 非空即可视为最小链路打通。

---

## 4. Android 联调地址检查

### 模拟器访问本机 FastAPI

在 Android 代码中使用：

```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

可先在模拟器浏览器中打开：

```text
http://10.0.2.2:8080/health
```

### 真机访问局域网 FastAPI

在 Android 代码中使用：

```kotlin
const val BASE_URL = "http://电脑局域网IP:8080/"
```

示例：

```kotlin
const val BASE_URL = "http://192.168.1.23:8080/"
```

可先在手机浏览器中打开：

```text
http://电脑局域网IP:8080/health
```

---

## 5. 常见失败排查

| 现象 | 可能原因 | 处理方式 |
|---|---|---|
| `curl` 访问 `/health` 失败 | FastAPI 未启动 | 确认后端进程已运行在 `8080` 端口 |
| `/health` 正常，`/api/v1/chat-policy` 失败 | Dify 地址或 Key 配置错误 | 检查 `backend/.env` 中的 `DIFY_BASE_URL` 和 `DIFY_API_KEY` |
| 返回 502 | 后端无法正常调用 Dify | 确认 Dify 服务可访问，应用已发布，Key 未失效 |
| 返回 504 或等待很久 | Dify、知识库或模型响应超时 | 检查 Dify 服务状态，适当增大 `DIFY_TIMEOUT_SECONDS` |
| Android 模拟器访问失败 | Base URL 写成了 `localhost` | 模拟器访问宿主机应使用 `10.0.2.2` |
| Android 真机访问失败 | 手机无法访问电脑局域网 IP | 确认手机和电脑在同一网络，防火墙放行 `8080` |
| App 显示技术错误 | Android 直接展示异常信息 | 改为统一友好提示，不展示堆栈或 HTTP 细节 |

---

## 6. 安全提醒

1. curl 示例中不要写真实 Dify API Key。
2. Dify API Key 只放在后端 `.env`。
3. 截图、录屏、日志中不要暴露 Key。
4. 如果 Key 曾经出现在聊天记录、截图或文档中，应在 Dify 后台删除旧 Key 并重新生成。
