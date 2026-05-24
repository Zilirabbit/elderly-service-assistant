# 最小接口联调 API 文档

> 适用阶段：Phase 04 后端网关与 Android 前端通信打通阶段
> 文档目标：对齐 Android 与 FastAPI 的最小联调接口
> 安全提醒：本文档不记录真实 Dify API Key、真实公网 IP 或敏感日志

---

## 1. 联调目标

本阶段只打通一条最小真实通信闭环：

```text
Android App
  -> FastAPI 后端网关
  -> Dify /v1/chat-messages
  -> Dify 知识库 + Qwen
  -> FastAPI 后端网关
  -> Android App 展示回答
```

当前仅覆盖：

1. 健康检查接口：`GET /health`
2. 政策问答接口：`POST /api/v1/chat-policy`

暂不覆盖材料清单生成、语音识别、语音播报、文件上传、表单预填、用户登录等功能。

---

## 2. 环境约定

### 2.1 后端服务

开发阶段 FastAPI 默认运行在：

```text
http://localhost:8080
```

启动示例：

```powershell
cd backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

### 2.2 Android Base URL

Android 端 `BASE_URL` 需要按运行环境选择。

#### Android 模拟器访问电脑本机后端

```kotlin
const val BASE_URL = "http://10.0.2.2:8080/"
```

说明：Android 模拟器里的 `10.0.2.2` 代表宿主电脑。

#### Android 真机访问局域网后端

```kotlin
const val BASE_URL = "http://电脑局域网IP:8080/"
```

示例：

```kotlin
const val BASE_URL = "http://192.168.1.23:8080/"
```

要求：手机和电脑必须在同一个 Wi-Fi 或可互通的局域网内。

### 2.3 Dify 配置

Dify API Key 只允许放在后端 `.env` 中，不允许写入 Android 代码、提交到仓库或出现在截图里。

后端环境变量示例：

```env
DIFY_BASE_URL=http://localhost
DIFY_API_KEY=app-替换为新的DifyAppKey
DIFY_CHAT_PATH=/v1/chat-messages
DIFY_TIMEOUT_SECONDS=60
DEFAULT_USER_ID=demo-user-001
```

---

## 3. 健康检查接口

### 3.1 请求

```http
GET /health
```

### 3.2 响应

```json
{
  "status": "ok",
  "service": "yue-tongxin-backend"
}
```

### 3.3 用途

用于确认 Android、浏览器或 curl 能访问 FastAPI 后端。
在 Android 接入政策问答接口前，应先确认该接口可访问。

---

## 4. 政策问答接口

### 4.1 请求

```http
POST /api/v1/chat-policy
Content-Type: application/json
```

### 4.2 请求体

```json
{
  "message": "港澳通行证续签需要什么材料？",
  "conversation_id": "",
  "user_id": "demo-user-001"
}
```

### 4.3 请求字段

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `message` | string | 是 | 用户输入的问题，不能为空 |
| `conversation_id` | string | 否 | Dify 返回的多轮会话 ID，首轮传空字符串 |
| `user_id` | string | 否 | 用户标识，Demo 阶段可固定为 `demo-user-001` |

### 4.4 成功响应

```json
{
  "answer": "办理港澳通行证续签通常需要准备有效身份证件、原港澳通行证以及与签注类型相关的材料。具体以当地出入境窗口或官方平台要求为准。",
  "conversation_id": "97e020aa-9b1b-4000-8448-1f54ceca4d13",
  "sources": [
    {
      "document_name": "02_港澳签注办理与续签说明.md",
      "score": 0.65,
      "content": "相关知识库片段摘要..."
    }
  ],
  "usage": {
    "total_tokens": 1283,
    "latency": 18.07
  }
}
```

### 4.5 响应字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `answer` | string | 给 Android 展示的政策问答结果 |
| `conversation_id` | string | Dify 会话 ID，后续多轮对话可继续传回 |
| `sources` | array | 知识库命中文档信息，当前阶段可不展示 |
| `usage` | object | Token、耗时等元信息，当前阶段可只用于调试 |

Android 当前最少只需要使用：

```json
{
  "answer": "...",
  "conversation_id": "..."
}
```

---

## 5. 错误处理约定

### 5.1 后端错误

| 场景 | 建议 HTTP 状态码 | 说明 |
|---|---:|---|
| `message` 为空 | 422 | 请求参数校验失败 |
| Dify 地址不可达 | 502 | 后端无法连接 Dify |
| Dify API Key 无效 | 502 | Dify 鉴权失败，日志不打印完整 Key |
| Dify 响应超时 | 504 | 模型或知识库响应过慢 |
| Dify 返回非预期格式 | 502 | 后端无法解析上游响应 |

### 5.2 Android 展示文案

Android 不直接展示技术错误，例如 `HTTP 502`、`SocketTimeoutException`。
建议统一展示：

```text
系统暂时没有响应，请稍后再试，或换个问题重新发送。
```

网络不可达时可展示：

```text
网络好像不太稳定，请检查网络后再试一次。
```

---

## 6. 最小验收标准

输入问题：

```text
港澳通行证续签需要什么材料？
```

期望结果：

1. Android 点击发送后出现 loading 状态。
2. Android 调用 `POST /api/v1/chat-policy`。
3. FastAPI 调用 Dify `/v1/chat-messages`。
4. FastAPI 返回非空 `answer`。
5. Android 展示真实回答。
6. 失败时 Android 展示友好错误提示。

---

## 7. 安全约束

1. Dify API Key 不写入 Android。
2. Dify API Key 不写入文档、截图、提交记录或 curl 示例。
3. 后端日志可以记录请求成功或失败，但不能打印完整 Key。
4. 文档中的 Key、IP、会话 ID 均使用占位符或示例值。
