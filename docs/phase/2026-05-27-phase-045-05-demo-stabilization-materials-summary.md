# Phase 04.5 + Phase 05 问答稳定化与材料清单闭环总结

> 日期：2026-05-27
> 阶段：问答 Demo 稳定化 + 材料清单真实生成
> 目标：在已跑通 Android -> FastAPI -> Dify 的最小问答闭环基础上，增强演示稳定性，并把“材料清单”从静态入口升级为可选择、可勾选、可保存的最小业务闭环。

## 1. 本阶段完成内容

本阶段完成两条主线：

1. **Phase 04.5：问答体验打磨**
   - Android 端对 Dify 返回文本做基础 Markdown 清理，减少 `**标题**`、裸 `*` 等符号直接显示。
   - Android 端错误提示细分为网络不可达、后端异常、超时、空回答等适老化文案。
   - AI 回答卡片改为展示后端返回的真实 `sources.document_name`。
   - Android `BASE_URL` 改为 `BuildConfig.ELDERCARE_BASE_URL`，默认适配官方模拟器：

```text
http://10.0.2.2:8080/
```

   - 真机联调时可通过 Gradle 参数临时覆盖：

```cmd
gradlew.bat assembleDebug -PELDERCARE_BASE_URL=http://电脑局域网IP:8080/
```

   - 后端 `chat-policy` 路由补充结构化日志，记录耗时、状态、错误类型、来源数量等信息，不打印 Dify API Key。

2. **Phase 05：材料清单真实生成**
   - 后端新增材料清单接口：

```http
GET /api/v1/materials/items
GET /api/v1/materials/{item_code}
POST /api/v1/materials/save
```

   - 后端第一版使用人工整理的稳定演示数据，不依赖模型即时生成，降低演示不确定性。
   - Android 新增材料清单模型和 `MaterialViewModel`。
   - Android “材料清单”页支持：
     - 从后端加载事项列表；
     - 选择事项；
     - 展示办理提醒；
     - 勾选必带/可选材料；
     - 保存清单到本机演示状态。
   - Android “我的”页新增“已保存材料清单”展示区域。

## 2. 已支持事项

当前材料清单第一版支持 3 个演示事项：

1. 办理港澳通行证
2. 港澳通行证续签
3. 过关材料准备

每个事项包含：

- `code`
- `title`
- `subtitle`
- `category`
- `tips`
- `requirements`
- `required`
- `note`

## 3. 验证结果

已验证通过：

```cmd
cd /d C:\Users\86136\Desktop\work\elderly-service-assistant\ElderCareApp
gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

后端验证：

- `python -m compileall backend\app` 通过。
- 使用 FastAPI `TestClient` 和 Fake Dify 服务完成烟测：
  - `GET /health` 返回 200。
  - `GET /api/v1/materials/items` 返回 200，包含 3 个事项。
  - `GET /api/v1/materials/{item_code}` 返回 200，包含材料要求。
  - `POST /api/v1/materials/save` 返回 200。
  - `POST /api/v1/chat-policy` 在 Fake Dify 下返回 200，并保留 sources 结构。

安全检查：

- Android 源码中未检出 `DIFY_API_KEY`、`Bearer`、`dify_api_key`。
- Dify API Key 仍只应保存在后端 `.env` 中。

## 4. 模拟器联调方式

后端 CMD 启动：

```cmd
cd /d C:\Users\86136\Desktop\work\elderly-service-assistant\backend
conda activate elderly-backend
uvicorn app.main:app --reload --host 0.0.0.0 --port 8080
```

Android 官方模拟器访问电脑本机 FastAPI 使用：

```text
http://10.0.2.2:8080/
```

因此模拟器测试时不需要额外传 `BASE_URL`。

## 5. 后续建议

- 在模拟器中完成一次完整手测：
  - FAQ 自动发送问题并展示 AI 回答。
  - AI 回答来源显示真实文档名。
  - 后端关闭时显示网络/服务错误提示。
  - 材料清单可选择、可勾选、可保存，并可在“我的”页看到。
- 真机测试时再通过 `-PELDERCARE_BASE_URL=http://电脑局域网IP:8080/` 切换地址。
- Phase 06 再接语音输入、ASR 和 TTS，不建议在当前材料清单闭环未稳定前抢先接入。
