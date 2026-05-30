# 2026-05-30 阶段记录：过关材料选择页进入清单后的返回修复

日期：2026-05-30  
项目：粤同心-湾区中老年助手  
阶段类型：本地 Mock 材料清单闭环的追加修复  
当前版本：返回行为修复版

---

## 1. 本轮新增工作

本轮修复了一个材料清单详情页返回行为问题。

原问题：

```text
服务页
  -> 过关材料准备
  -> 过关材料选择页
  -> 任意材料清单
  -> 点击左上角返回
  -> 直接回到服务页
```

期望行为：

```text
服务页
  -> 过关材料准备
  -> 过关材料选择页
  -> 任意材料清单
  -> 点击左上角返回
  -> 回到过关材料选择页
```

---

## 2. 实现方式

本轮没有重构导航系统，也没有引入正式 `NavHost` route。

继续沿用当前项目的：

```text
overlayScreen
activeChecklistId
```

并新增一个最小状态：

```kotlin
checklistBackTarget
```

作用：

- 记录当前材料清单详情页是否有指定返回目标。
- 如果有，则材料清单返回时回到该 overlay。
- 如果没有，则继续走原来的返回逻辑。

当前主要使用场景：

```text
从 CrossBorderPreparePicker 进入材料清单
  -> checklistBackTarget = CrossBorderPreparePicker
```

---

## 3. 已完成的行为

### 3.1 从过关材料选择页进入清单

已修复：

```text
过关材料选择页
  -> 已办好证件，只核对过关材料
  -> border_crossing_prepare
  -> 返回
  -> 过关材料选择页
```

已修复：

```text
过关材料选择页
  -> 还没有港澳通行证
  -> hk_macau_pass_apply
  -> 返回
  -> 过关材料选择页
```

已修复：

```text
过关材料选择页
  -> 有通行证，但签注不确定
  -> hk_macau_renewal
  -> 返回
  -> 过关材料选择页
```

---

### 3.2 清单内辅助跳转后返回

已保持返回目标不丢失。

例如：

```text
过关材料选择页
  -> 过关材料准备清单
  -> 点击“还没有？查看办理材料”
  -> 办理港澳通行证清单
  -> 返回
  -> 过关材料选择页
```

例如：

```text
过关材料选择页
  -> 过关材料准备清单
  -> 点击“不确定？查看续签材料”
  -> 港澳通行证续签清单
  -> 返回
  -> 过关材料选择页
```

---

### 3.3 系统返回键

本轮同时让系统返回键和左上角返回按钮保持一致。

也就是说：

```text
如果当前材料清单有 checklistBackTarget
  -> 系统返回键也回到 checklistBackTarget
```

---

## 4. 修改文件

本轮只修改了必要页面文件：

```text
ElderCareApp/app/src/main/java/com/example/eldercareapp/ui/screen/ElderCareScreens.kt
```

关键改动：

- 新增 `checklistBackTarget` 状态。
- 从 `CrossBorderPreparePickerScreen` 进入材料清单时设置返回目标。
- `MaterialChecklistScreen` 返回按钮改为优先走 `checklistBackTarget`。
- 清单内辅助跳转到其他清单时保留返回目标。
- 系统返回键同步支持该逻辑。

---

## 5. 未做内容

本轮刻意没有做以下事情：

1. 没有重构导航系统
2. 没有引入正式 `NavHost` route
3. 没有修改 ViewModel 保存逻辑
4. 没有修改材料 Mock 数据
5. 没有修改 Dify / RAG / 后端 / 数据库相关代码
6. 没有修改语音、ASR、TTS 相关逻辑

---

## 6. 验证结果

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
- 仍有项目既有的 `Icons.Filled.*` deprecation warning，不影响当前功能。

---

## 7. 当前验收重点

建议重点验收：

```text
服务页
  -> 过关材料准备
  -> 选择页
  -> 任意清单
  -> 返回
  -> 回到选择页
```

以及：

```text
过关材料准备清单
  -> 材料项辅助按钮跳转到其他清单
  -> 返回
  -> 仍回到过关材料选择页
```
