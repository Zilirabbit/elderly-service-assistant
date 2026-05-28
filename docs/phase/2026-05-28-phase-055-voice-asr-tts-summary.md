# 5.5 语音输入、查询改写与语音播报完成记录

> 日期：2026-05-28
> 阶段：5.5 语音输入、查询改写与语音播报第一版闭环
> 范围：Android 语音录制与确认、FastAPI ASR、查询改写、Dify 问答、阿里云 DashScope TTS 播报与缓存

## 已完成

- Android 端已接入麦克风权限申请和真实录音流程，使用 `MediaRecorder` 录制 `.m4a` 音频文件。
- Android 端语音按钮已从固定模拟文本改为真实语音输入面板，支持普通话、方言免切换、英语三个入口。
- Android 端已通过 Retrofit multipart 上传录音到 FastAPI 后端 `POST /api/v1/asr/transcribe`。
- Android 端已恢复“我听到的是：……”确认流程，支持“正确，继续”“重新说一遍”“手动修改”。
- Android 端已保留文字输入 fallback，文字提问流程不依赖语音识别。
- Android 端已移除本地 `TextToSpeech` 朗读链路，改为使用 `MediaPlayer` 播放后端返回的阿里云 TTS 缓存音频。
- Android 端回答页、语音识别确认卡片、操作指南“朗读本步”均走后端 TTS，不再使用本地兜底。
- 后端已新增 ASR 路由 `POST /api/v1/asr/transcribe`，接收音频文件、语种参数和用户标识。
- 后端已新增 DashScope ASR 封装，将上传音频转为 Base64 data URL，并调用 `qwen3-asr-flash`。
- 后端已新增 Qwen 文本模型封装，使用 `qwen-plus` 生成检索问题 `search_query` 和播报文本 `tts.text`。
- 后端已新增 TTS 路由 `POST /api/v1/tts/synthesize`，调用阿里云 DashScope `SpeechSynthesizer` / `cosyvoice-v3-flash` 生成朗读音频。
- 后端已新增 TTS 音频缓存，缓存文件通过 `/static/tts/...` 暴露给 Android 播放。
- 后端 `/api/v1/chat-policy` 已扩展为“原始文本 -> 查询改写 -> Dify/RAG -> 展示文本 -> 播报文本”的链路。
- 后端 `/api/v1/chat-policy` 响应已兼容旧字段 `answer`，并新增 `original_text`、`search_query`、`display_text`、`tts`。
- 后端配置已补充 `DASHSCOPE_API_KEY`、`QWEN_BASE_URL`、`QWEN_MODEL`、`ASR_MODEL`、`ASR_TIMEOUT_SECONDS`、`TTS_MODEL`、`TTS_ENDPOINT`、`TTS_CACHE_ENABLED` 等环境变量。
- 后端依赖已补充 `python-multipart`，用于 FastAPI 接收 multipart 音频上传。
- Android 端不会保存或直接调用 DashScope、Qwen、Dify、ASR、TTS Key，模型 Key 仍只放在后端环境变量中。

## 已验证

- 已完成 Android Debug 构建验证：`.\gradlew.bat assembleDebug` 通过。
- 已完成后端 Python 语法检查：`python -m compileall backend\app` 通过。
- 已完成 FastAPI 路由导入检查，确认存在 `/health`、`/api/v1/chat-policy`、`/api/v1/asr/transcribe`。
- 已用 fake Qwen 与 fake Dify 服务完成 `/api/v1/chat-policy` 烟测，确认响应包含 `answer`、`original_text`、`search_query`、`display_text`、`tts`、`sources`、`usage`。
- 已验证 `/api/v1/asr/transcribe` 对空音频返回可理解错误：`400 录音文件为空，请重新录制`。
- 已在实际 App 流程中验证普通话语音可以识别为文本。
- 已在实际 App 流程中验证语音识别文本可以继续生成政策回答。
- 已在实际 App 流程中验证生成回答可以输出普通话播报文本；云端音频播放需在配置可用 DashScope TTS Key 后继续真机验收。
- 已确认后端请求中能够区分文字输入和语音输入，语音确认后以 `input_type = voice` 进入问答链路。
- 已确认文字输入流程仍可作为备用输入方式使用。

## 相关文件

- `backend/app/api/routes_asr.py`
- `backend/app/api/routes_chat.py`
- `backend/app/api/routes_tts.py`
- `backend/app/services/asr_service.py`
- `backend/app/services/qwen_text_service.py`
- `backend/app/services/tts_service.py`
- `backend/app/schemas/asr_schema.py`
- `backend/app/schemas/chat_schema.py`
- `backend/app/schemas/tts_schema.py`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/voice/VoiceRecorder.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
