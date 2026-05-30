# 2026-05-30 阶段记录：本地 Mock 材料清单闭环与办理判断接入（第一版）

日期：2026-05-30  
项目：粤同心-湾区中老年助手  
对应方案：`docs/2026-05-30-phase-material-checklist-mock-decision.md`  
当前版本：第一版，可根据后续验收反馈继续修改

---

## 1. 本轮已完成

### 1.1 本地 Mock 材料清单数据

已将材料清单能力切换为本地内存 Mock，不再依赖后端材料接口。

当前支持 4 个清单：

1. `hk_macau_pass_apply`：办理港澳通行证
2. `hk_macau_renewal`：港澳通行证续签
3. `border_crossing_prepare`：过关材料准备
4. `service_uncertain_valid_pass`：办理前核对清单

实现位置：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/MaterialViewModel.kt
```

说明：

- 复用了项目已有的 `MaterialChecklist` / `MaterialRequirement` / `MaterialItem` 数据结构。
- 没有新增一套冲突模型。
- Mock 数据包含标题、办理提醒、材料项、必带/可选、补充说明。
- 当前数据只保存在 App 运行期内存中，退出 App 后不持久化。

---

### 1.2 材料清单详情页

已完成 `MaterialChecklistScreen(checklistId)` 形态的详情页能力。

页面包含：

- 顶部返回栏
- 清单标题
- 办理提醒
- 材料核对进度：`已核对 x / y 项`
- 材料卡片列表
- 必带 / 可选标签
- Checkbox 勾选
- 底部固定按钮

底部按钮状态：

- 未保存：`保存清单`
- 已保存：`更新清单`

保存后：

- 保存当前 `checkedRequirementIds`
- Toast：`已保存到我的材料清单`
- 同一个 `checklistId` 再次保存会更新原记录，不重复创建

已处理底部安全区域：

```kotlin
.navigationBarsPadding()
.imePadding()
```

实现位置：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
```

---

### 1.3 我的页已保存清单闭环

已将“我的”页调整为当前阶段需要的材料清单展示。

当前行为：

- 没有保存清单时显示空状态提示
- 有保存清单时显示：
  - 清单标题
  - `已核对 x / y 项`
  - `继续核对`
- 点击已保存清单可回到对应材料清单详情页
- 回到详情页后会恢复之前勾选的材料项

实现位置：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
```

---

### 1.4 办理判断结果页接入材料清单

已在办理判断结果数据中增加 `checklistId`。

当前映射：

- 首次办理相关结果：`hk_macau_pass_apply`
- 续签相关结果：`hk_macau_renewal`
- 不确定具体业务、但已有有效通行证：`service_uncertain_valid_pass`
- 其他未细分结果默认：`service_uncertain_valid_pass`

已保留原有行为：

- `查看详细政策` 仍然跳转到智能问答页
- 仍然带入原来的标准政策问题 `standardQuestion`

新增行为：

- `查看材料清单` 会根据当前结果的 `checklistId` 打开对应材料清单

实现位置：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
```

---

### 1.5 服务页入口

已在服务页“出发/到达服务”中新增/接入：

```text
过关材料准备 -> border_crossing_prepare
```

其他暂未实现的服务入口仍保持 Toast，不强行全部改成跳转。

---

## 2. 本轮修改的文件

### 2.1 主要修改

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/viewmodel/MaterialViewModel.kt
```

修改内容：

- 移除材料清单对后端接口的依赖
- 增加 4 个本地 Mock 清单
- 保存已勾选材料状态
- 支持同一清单重复保存时更新
- 支持从“我的”页进入后恢复勾选状态

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
```

修改内容：

- 增加 `GuidanceResult.checklistId`
- 办理判断结果页的“查看材料清单”按结果跳转
- 增加 `activeChecklistId` 作为当前本地页面状态
- 完成材料清单详情页
- 调整材料清单列表页
- 调整“我的”页只展示已保存清单
- 服务页接入“过关材料准备”
- 底部保存按钮加入系统手势区避让

---

## 3. 当前没有做 / 刻意不做

以下内容按方案要求，本阶段没有做：

1. 没有接 Dify
2. 没有接 RAG
3. 没有接后端材料接口
4. 没有接数据库
5. 没有接 Room
6. 没有接 DataStore
7. 没有接真实政务 API
8. 没有做真实动态生成材料清单
9. 没有重做智能问答页
10. 没有重做办理判断问卷流程
11. 没有改语音、ASR、TTS 相关逻辑
12. 没有改后端代码

另外有两个当前实现选择需要说明：

1. 当前项目没有使用独立 `NavHost` route 来管理这些页面，本轮按现有 `overlayScreen` 页面模式接入，通过 `activeChecklistId` 达到 `material_checklist/{checklistId}` 的等价效果。后续如果项目迁移到正式 Navigation route，可再补 `MaterialChecklist.createRoute(checklistId)`。
2. 当前保存状态是 ViewModel 内存状态，满足 App 运行期恢复；退出 App 后不保留。后续如需要跨启动保存，再接 DataStore 或 Room。

---

## 4. 已验证

已执行 Android Debug 编译：

```powershell
.\gradlew.bat assembleDebug
```

结果：

```text
BUILD SUCCESSFUL
```

说明：

- Kotlin / Compose 编译通过。
- 仍有若干 `Icons.Filled.*` deprecation warning，是现有 Material Icons API 警告，不影响当前功能运行。

---

## 5. 当前可验收流程

### 流程 A：服务页进入材料清单

```text
服务页
-> 点击“过关材料准备”
-> 进入“过关材料准备”材料清单
-> 勾选材料
-> 保存清单
-> 我的页显示该清单
-> 点击继续核对
-> 回到详情页并恢复勾选状态
```

### 流程 B：办理判断结果进入材料清单

```text
服务页
-> 点击“我不知道该办哪种”
-> 回答问题
-> 生成判断结果
-> 点击“查看材料清单”
-> 根据当前结果进入对应 Mock 清单
-> 勾选并保存
-> 我的页可继续核对
```

### 流程 C：办理判断结果进入智能问答

```text
判断结果页
-> 点击“查看详细政策”
-> 进入智能问答页
-> 自动带入 standardQuestion
```

该流程为原有能力，本轮仅确认没有破坏。

---

## 6. 后续可继续调整点

如果第一版验收后需要优化，可优先看：

1. 是否要把 `activeChecklistId` 改成正式 `NavHost` route。
2. “我的”页是否要恢复字体设置、操作指南等入口，或继续保持本阶段只展示材料清单。
3. 办理判断结果到 `checklistId` 的映射是否需要更细。
4. Mock 清单文字是否需要按最终政策口径再润色。
5. 是否需要增加页面截图验收或真机点击验收记录。
