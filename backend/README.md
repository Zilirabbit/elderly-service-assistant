# Backend

FastAPI 后端网关目录。

后续计划在这里统一封装 Dify 应用 API、阿里云百炼 / DashScope Qwen 文本模型、DashScope ASR、云端 TTS、字段抽取 Workflow、表单 Schema、申请草稿和统一错误处理。

Android 不直接保存或调用 DashScope、Dify、ASR、TTS 的 API Key；所有模型调用都必须经过 FastAPI 后端。
