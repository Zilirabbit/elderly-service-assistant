# Elderly Service Assistant

智慧办事助手是一个面向老年用户的办事辅助 Demo 项目，目标是串联 Android App、FastAPI 后端网关、Dify/RAG 配置、部署文档、接口测试和比赛演示材料。

## Project Structure

当前仓库采用单仓库管理。现有 Android 工程目录保留为 `ElderCareApp/`，它对应开发文书推荐结构中的 `android-app/`。

```text
elderly-service-assistant/
├── ElderCareApp/        # Android 原生 App，对应推荐结构 android-app/
├── backend/             # FastAPI 后端网关占位
├── dify-config/         # Dify 应用、Workflow、知识库配置说明占位
├── deployment/          # 本地与云端部署说明占位
├── docs/                # 项目文档
├── api-tests/           # 接口测试材料占位
├── demo-assets/         # 比赛演示材料占位
└── scripts/             # 仓库级脚本占位
```

## Current Stage

本阶段只搭建项目大框架：

- 不新增后端接口实现。
- 不修改 Android 业务代码。
- 不调整 Gradle 依赖、Manifest 权限或包名。
- 空目录通过 README 或 `.gitkeep` 占位，便于后续分阶段补充实现。

