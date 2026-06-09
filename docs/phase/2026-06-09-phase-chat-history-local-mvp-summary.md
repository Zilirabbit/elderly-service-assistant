# Phase 6.5 问答页本地历史记录 MVP 总结

日期：2026-06-09

## 本轮目标

- 在问答页补齐“历史”按钮的真实功能，不再停留在 Toast 占位。
- 实现前端本地持久化历史记录 MVP。
- 不改后端接口、不改 Dify 参数、不新增登录或云同步。
- 保持现有问答、语音输入、TTS 朗读、快捷问题、结构化回答卡片和材料清单入口可用。

## 修改范围

- `ElderCareApp/app/src/main/java/com/example/eldercareapp/model/ChatModels.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/data/local/ChatHistoryStorage.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/ChatViewModel.kt`
- `ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt`
- `ElderCareApp/app/src/main/res/values/strings.xml`
- `ElderCareApp/app/src/main/res/values-zh-rCN/strings.xml`
- `ElderCareApp/app/src/main/res/values-zh-rHK/strings.xml`
- `ElderCareApp/app/src/main/res/values-zh-rMO/strings.xml`
- `ElderCareApp/app/src/main/res/values-zh-rTW/strings.xml`
- `ElderCareApp/app/src/main/res/values-en/strings.xml`
- `.gitignore`

本次提交同时包含工作区中已存在的前序阶段改动与阶段文档，包括语音面板正式化、回答展示本地化、Me 页/服务页资源化等内容。

## 核心实现

### 历史数据模型

新增前端历史模型：

- `ChatHistoryItem`
- `ChatHistoryMessage`

历史消息使用字符串 role（`user` / `assistant`），避免 model 层依赖 ViewModel 中的 `ChatMessageRole`。

### 本地持久化

新增 `ChatHistoryStorage`：

- 使用 `SharedPreferences` 保存历史 JSON。
- 使用 Android 自带 `org.json` 序列化，不新增 Room/DataStore/Gson 依赖。
- 最多保留最近 30 条。
- 解析失败时返回空列表，避免历史数据损坏导致 App 崩溃。
- 保存结构化回答 `QaAnswerUiModel`，恢复历史时仍可展示结构化回答卡片。

### ViewModel 状态与动作

`ChatUiState` 新增：

- `historyItems`

`ChatViewModel` 新增：

- `attachHistoryStorage`
- `restoreHistory`
- `deleteHistory`
- `clearHistory`
- `resendHistoryQuestion`

成功完成一次问答后自动保存历史；失败、loading、空回答不保存。

恢复历史时：

- 不触发网络请求。
- 恢复完整 `messages`。
- 恢复 `conversationId`，方便继续追问沿用旧会话。
- 更新 `nextMessageId`，避免后续新消息 ID 冲突。

重新提问时：

- 使用历史问题重新走现有 `sendQuestion` 流程。
- `inputType` 标记为 `history`。

### 问答页 UI

问答页右上角历史按钮从 Toast 改为 `ModalBottomSheet`：

- 空状态显示“暂无历史记录”。
- 历史卡片显示问题、回答摘要、时间和输入来源标签。
- 点击卡片可恢复完整问答。
- 支持“查看完整问答”“重新提问”“删除”。
- 支持清空全部历史。
- 复用现有适老化颜色、卡片、按钮和响应式间距。

### 多语言文案

补齐历史功能文案：

- 简中默认资源
- `zh-rCN`
- `zh-rHK`
- `zh-rMO`
- `zh-rTW`
- 英文

## 不做内容

- 不改后端 FastAPI 接口。
- 不改 Dify / RAG 参数。
- 不新增登录、账号体系或云同步。
- 不保存语音音频文件。
- 不新增底部 Tab。
- 不新增独立一级历史页面。
- 不引入 Room/DataStore。

## 验证

已在 `ElderCareApp` 目录运行：

```powershell
.\gradlew.bat assembleDebug
```

结果：构建通过。

## 提交注意

- `backend/app/data/asr_debug/` 是 ASR 调试音频缓存，已加入 `.gitignore`，不纳入提交。
- 当前工作区包含若干前序阶段未提交改动，本次按用户要求统一提交工作区代码。

## 剩余风险

- 尚未做真实设备上的历史弹窗视觉 QA。
- 清空历史目前为直接操作，未加二次确认；如面向正式用户，可后续补确认弹窗。
- 本地 JSON 方案适合 MVP；如果后续要支持复杂查询、多设备同步或大规模历史，应升级为 Room 或后端同步。
