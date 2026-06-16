# 2026-05-30 追加修改建议：过关材料准备入口细化与材料项跳转

项目：粤同心-湾区中老年助手  
阶段类型：本地 Mock 材料清单闭环的追加优化  
目标读者：Codex / Android UI 与页面逻辑修改  
当前策略：只做本地 Mock，不接 Dify、不接 RAG、不接后端、不接数据库

---

## 1. 当前背景

当前项目已经完成第一版本地 Mock 材料清单闭环：

1. 已支持 4 个 Mock 清单：
   - `hk_macau_pass_apply`：办理港澳通行证
   - `hk_macau_renewal`：港澳通行证续签
   - `border_crossing_prepare`：过关材料准备
   - `service_uncertain_valid_pass`：办理前核对清单

2. 已完成材料清单详情页：
   - 可根据 `checklistId` 展示不同清单
   - 可勾选材料
   - 可保存 / 更新清单
   - 底部按钮已处理系统手势区域避让

3. 已完成“我的”页已保存清单闭环：
   - 保存后在“我的”页展示
   - 显示 `已核对 x / y 项`
   - 点击“继续核对”可回到对应清单详情页
   - 可恢复之前的勾选状态

4. 已完成办理判断结果页到材料清单的接入：
   - 判断结果已增加 `checklistId`
   - “查看材料清单”可根据判断结果进入对应 Mock 清单
   - “查看详细政策”仍保留原有智能问答跳转

---

## 2. 当前发现的问题

当前“服务页 → 过关材料准备”的逻辑仍然偏简单。

现在大致是：

```text
服务页
  → 过关材料准备
  → border_crossing_prepare
```

这个流程可以核对“过关当天要带什么”，但还没有解决用户的进一步问题：

```text
我还没有港澳通行证怎么办？
我有港澳通行证，但不确定签注是否有效怎么办？
我想从过关材料准备页跳到港澳通行证具体办理材料，应该点哪里？
```

也就是说，`过关材料准备` 目前是一个过于泛化的入口。

对适老化用户来说，点击“过关材料准备”后，最好先让用户选择自己的情况，而不是直接进入一个通用清单。

---

## 3. 本阶段修改目标

本阶段目标是把“过关材料准备”从一个单一清单入口，升级为一个可理解的选择入口。

目标流程改为：

```text
服务页
  → 过关材料准备
  → 过关材料选择页
  → 根据用户情况进入对应清单或办理判断流程
```

同时，在 `border_crossing_prepare` 清单内部，对关键材料项增加辅助跳转：

```text
港澳通行证
  → 还没有？查看办理材料
  → hk_macau_pass_apply

有效签注
  → 不确定？查看续签材料
  → hk_macau_renewal
```

这样用户可以从“过关要带什么”自然跳到“我还缺什么、该怎么办”。

---

## 4. 新增页面：CrossBorderPreparePickerScreen

建议新增页面：

```kotlin
CrossBorderPreparePickerScreen
```

也可以根据当前项目命名习惯使用等价名称，例如：

```kotlin
CrossBorderMaterialPickerScreen
BorderPrepareChoiceScreen
```

但页面职责要明确：  
**它不是清单详情页，而是过关材料准备的分流选择页。**

---

## 5. 过关材料选择页内容

### 5.1 顶部栏

标题：

```text
过关材料准备
```

左侧返回按钮。  
标题保持居中，沿用当前项目的二级页面顶栏样式。

---

### 5.2 说明卡片

文案建议：

```text
请选择您现在的情况，我会帮您打开对应的材料清单。
```

可以加一句更适老化的补充说明：

```text
如果不确定该选哪一项，可以先选择“我不清楚该办哪种”。
```

---

### 5.3 四个入口卡片

#### 入口 1：已办好证件，只核对过关材料

标题：

```text
已办好证件，只核对过关材料
```

说明：

```text
查看通行证、签注、身份证等是否带齐。
```

点击后进入：

```kotlin
MaterialChecklistScreen("border_crossing_prepare")
```

或当前项目等价的 overlay / activeChecklistId 切换逻辑。

---

#### 入口 2：还没有港澳通行证

标题：

```text
还没有港澳通行证
```

说明：

```text
查看首次办理港澳通行证需要准备的材料。
```

点击后进入：

```kotlin
MaterialChecklistScreen("hk_macau_pass_apply")
```

---

#### 入口 3：有通行证，但签注不确定

标题：

```text
有通行证，但签注不确定
```

说明：

```text
查看续签或签注核对材料。
```

点击后进入：

```kotlin
MaterialChecklistScreen("hk_macau_renewal")
```

---

#### 入口 4：我不清楚该办哪种

标题：

```text
我不清楚该办哪种
```

说明：

```text
回答几个问题，先判断自己该办哪一种。
```

点击后进入现有办理情况判断页。

注意：

```text
不要重做办理情况判断流程，只复用已有的“我不知道该办哪种 / 办理情况判断”入口。
```

---

## 6. 修改服务页入口

当前服务页里的“过关材料准备”不要再直接跳转到 `border_crossing_prepare`。

需要改为：

```text
服务页
  → 点击“过关材料准备”
  → CrossBorderPreparePickerScreen
```

其他服务入口保持现状：

```text
通关流程
预约停车
交通出行
特殊人群
```

如果这些入口当前仍是 Toast，就继续保持 Toast，不要在本阶段扩大改动范围。

---

## 7. 材料项辅助跳转

### 7.1 数据结构建议

在材料项数据结构中增加可选字段。

建议字段：

```kotlin
val linkedChecklistId: String? = null
val linkedActionLabel: String? = null
```

如果项目现有 `MaterialRequirement` / `MaterialItem` 已经有类似字段，请复用现有字段，不要强行新增冲突模型。

目标是让某些材料项可以显示一个辅助按钮。

---

### 7.2 港澳通行证材料项

在 `border_crossing_prepare` 清单中，找到材料项：

```text
港澳通行证
```

增加辅助操作：

```text
还没有？查看办理材料
```

点击后进入：

```kotlin
MaterialChecklistScreen("hk_macau_pass_apply")
```

---

### 7.3 有效签注材料项

在 `border_crossing_prepare` 清单中，找到材料项：

```text
有效签注
```

增加辅助操作：

```text
不确定？查看续签材料
```

点击后进入：

```kotlin
MaterialChecklistScreen("hk_macau_renewal")
```

---

### 7.4 其他材料项

以下材料项暂时不需要跳转：

```text
居民身份证
手机号与紧急联系人信息
必要药品
```

它们保持普通 checkbox 和说明即可。

---

## 8. 材料项 UI 建议

不要把整张材料卡片做成隐式跳转。  
Checkbox 和辅助跳转要分开，避免用户误触。

建议结构：

```text
[ ] 港澳通行证                      必带
    过关时需要出示，建议放在容易拿取的位置。

    [还没有？查看办理材料]
```

要求：

1. Checkbox 只负责勾选材料。
2. 辅助按钮只负责跳转到相关清单。
3. 辅助按钮要明确显示文案。
4. 辅助按钮点击区域足够大，适合中老年用户。
5. Compact 手机下保持单列。
6. 保持政务蓝、卡片化、大字体、高对比度。
7. 不要让辅助按钮抢走“保存清单”的主操作地位。

辅助按钮样式建议：

```text
描边按钮 / 浅蓝底按钮
高度 44dp - 48dp
文字 15sp - 17sp
```

---

## 9. 保存逻辑要求

不改变现有保存逻辑。

如果用户从“过关材料准备”跳转到其他清单，并保存对应清单，那么“我的”页应显示多条已保存清单。

示例：

```text
我的材料清单

过关材料准备
已核对 2 / 5 项
继续核对

办理港澳通行证
已核对 1 / 4 项
继续核对
```

要求：

1. 同一个 `checklistId` 重复保存时更新原记录，不重复创建。
2. 从“我的”页点击任意已保存清单，都能回到对应详情页。
3. 勾选状态需要恢复。
4. 本阶段继续使用 ViewModel 内存状态即可，不做 DataStore / Room。

---

## 10. 页面跳转状态建议

当前项目如果仍然使用 `overlayScreen` / `activeChecklistId` 模式，可以继续沿用，不必强行迁移到正式 NavHost route。

建议新增一个 overlay 状态，例如：

```kotlin
CrossBorderPreparePicker
```

或者使用当前项目已有页面枚举方式。

推荐等价逻辑：

```text
activeOverlay = CrossBorderPreparePicker
activeChecklistId = null
```

点击选择项后：

```text
activeOverlay = MaterialChecklist
activeChecklistId = "border_crossing_prepare"
```

或：

```text
activeOverlay = MaterialChecklist
activeChecklistId = "hk_macau_pass_apply"
```

不要为了这个小阶段大改导航架构。

---

## 11. 修改范围建议

优先修改：

```text
ElderCareScreens.kt
MaterialViewModel.kt
ElderCareComponents.kt
```

如果当前服务页入口、overlay 状态、页面枚举在其他文件中，也可以小范围读取并修改。

不要扫描整个项目。  
不要修改无关文件。

---

## 12. 不要做的事情

本阶段明确不要做：

1. 不接 Dify
2. 不接 RAG
3. 不接后端
4. 不接数据库
5. 不接 Room
6. 不接 DataStore
7. 不改语音、ASR、TTS
8. 不改智能问答的 API 逻辑
9. 不重做办理情况判断流程
10. 不接真实政务 API
11. 不做真实动态材料清单生成
12. 不重构整个导航系统
13. 不扫描整个项目

---

## 13. 验收流程

### 流程 A：服务页进入选择页

```text
服务页
  → 点击“过关材料准备”
  → 进入“过关材料准备”选择页
```

验收重点：

```text
不再直接进入 border_crossing_prepare 清单。
```

---

### 流程 B：选择过关核对

```text
过关材料准备选择页
  → 点击“已办好证件，只核对过关材料”
  → 进入 border_crossing_prepare 清单
```

---

### 流程 C：选择港澳通行证办理

```text
过关材料准备选择页
  → 点击“还没有港澳通行证”
  → 进入 hk_macau_pass_apply 清单
```

---

### 流程 D：选择签注核对

```text
过关材料准备选择页
  → 点击“有通行证，但签注不确定”
  → 进入 hk_macau_renewal 清单
```

---

### 流程 E：进入办理判断

```text
过关材料准备选择页
  → 点击“我不清楚该办哪种”
  → 进入现有办理情况判断页
```

要求：

```text
不要重做判断页，只复用已有页面。
```

---

### 流程 F：材料项辅助跳转

```text
border_crossing_prepare 清单
  → 找到“港澳通行证”材料项
  → 点击“还没有？查看办理材料”
  → 进入 hk_macau_pass_apply 清单
```

```text
border_crossing_prepare 清单
  → 找到“有效签注”材料项
  → 点击“不确定？查看续签材料”
  → 进入 hk_macau_renewal 清单
```

---

### 流程 G：保存后我的页展示

```text
进入任意清单
  → 勾选材料
  → 保存清单
  → 我的页显示该清单
  → 点击“继续核对”
  → 回到对应清单并恢复勾选状态
```

---

## 14. 本阶段完成标准

完成后，用户从“过关材料准备”可以清楚选择：

```text
我只是出发前核对材料
我还没有港澳通行证
我有通行证但签注不确定
我完全不知道该办哪种
```

并且可以从过关材料清单中的关键材料项继续跳转到具体办理清单。

最终 Demo 链路应变成：

```text
服务页
  → 过关材料准备
  → 选择当前情况
  → 查看对应材料清单
  → 勾选
  → 保存
  → 我的页继续核对
```

这一步不改变底层技术架构，只增强材料清单入口的可理解性和可用性。
