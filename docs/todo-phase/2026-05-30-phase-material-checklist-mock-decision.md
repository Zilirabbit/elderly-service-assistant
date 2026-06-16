# Phase：本地 Mock 材料清单闭环与办理判断打通

日期：2026-05-30  
项目：粤同心-湾区中老年助手  
阶段目标：在不接后端、不接 Dify/RAG、不接数据库的前提下，完成“办理判断 → 材料清单 → 保存 → 我的页继续核对”的本地闭环。

---

## 1. 当前背景

当前项目已经完成或部分完成以下功能：

1. 已有“我不知道该怎么办 / 办理情况判断”Demo。
   - 用户可以按步骤回答几个问题。
   - 页面可以生成一个办理建议结果。
   - “查看详细政策”已能跳转到智能问答页，并带入标准问题。

2. 材料清单功能还没有完整做完。
   - 现在有材料清单页面方向或雏形。
   - 但材料清单详情页还没有形成稳定可复用页面。
   - “查看材料清单”尚未真正跳转到对应详情页。
   - “保存清单”尚未形成状态闭环。
   - “我的”页虽然显示已保存清单区域，但已保存清单无法点击进入继续核对。

3. 未来可能接 RAG / Dify。
   - 但当前阶段暂时不接。
   - 当前只做本地 Mock，先把 UI、数据结构、页面跳转、保存状态跑通。
   - 未来再把 Mock 数据来源替换为后端静态 JSON 或 Dify/RAG 结构化 JSON。

---

## 2. 本阶段总目标

本阶段只做一个清晰、可演示、可运行的本地闭环：

```text
服务页
  ↓
我不知道该办哪种 / 办理情况判断
  ↓
判断结果页
  ↓
查看材料清单
  ↓
材料清单详情页
  ↓
勾选材料
  ↓
保存清单
  ↓
我的页显示已保存清单
  ↓
点击已保存清单
  ↓
回到材料清单详情页并恢复勾选状态
```

同时保留已经做好的：

```text
判断结果页
  ↓
查看详细政策
  ↓
智能问答页，并带入标准 policyQuery
```

---

## 3. 本阶段明确不做

请不要在本阶段引入额外复杂度。

本阶段不做：

```text
不接 Dify
不接 RAG
不接后端 API
不接数据库
不接 Room
不接 DataStore
不做真实政务接口
不做动态生成材料清单
不重做已有办理判断流程
不重做智能问答页
不扫描整个项目做大范围重构
```

当前只做本地 Mock 状态。

如果需要持久化，后续阶段再考虑 DataStore 或 Room。

---

## 4. 建议 Codex 修改范围

优先只阅读和修改以下文件：

```text
ElderCareScreens.kt
ElderCareComponents.kt
ElderResponsive.kt
导航相关文件，例如 AppNavHost / MainActivity 中的 NavHost
```

如果项目已有 ViewModel、AppState、Route、NavGraph 等文件，可以读取并小范围修改。

禁止无关修改：

```text
不要修改 Dify/RAG/API/语音相关代码
不要修改后端相关文件
不要修改 Gradle 配置，除非编译必须
不要重写现有页面视觉风格
不要删除已有 Demo 功能
不要把所有服务入口都改成真实跳转
```

---

## 5. 第一优先级：材料清单本地 Mock 闭环

### 5.1 新增或完善数据结构

建议新增统一数据结构，避免把材料文本写死在 UI 里。

```kotlin
data class MaterialChecklist(
    val id: String,
    val title: String,
    val reminder: String,
    val source: String = "本地 Mock 清单",
    val items: List<ChecklistItem>
)

data class ChecklistItem(
    val id: String,
    val title: String,
    val description: String,
    val required: Boolean,
    val note: String? = null
)

data class SavedChecklistState(
    val checklistId: String,
    val checkedItemIds: Set<String>,
    val updatedAtMillis: Long = System.currentTimeMillis()
)
```

如果项目中已有类似数据结构，请优先复用或轻量调整，不要重复创建一堆冲突类。

---

### 5.2 第一版支持 4 个 Mock 清单

本阶段只做 4 个清单。

```text
1. hk_macau_pass_apply
   办理港澳通行证

2. hk_macau_renewal
   港澳通行证续签

3. border_crossing_prepare
   过关材料准备

4. service_uncertain_valid_pass
   办理前核对清单
```

其中 `service_uncertain_valid_pass` 用于“我不知道该怎么办”判断结果中：

```text
用户已有有效港澳通行证，但不确定这次应办理签注、换证、补发还是其他业务。
```

---

### 5.3 Mock 清单内容建议

#### hk_macau_pass_apply：办理港澳通行证

标题：办理港澳通行证

办理提醒：

```text
首次办理一般需要本人到出入境窗口办理。办理前建议先确认当地出入境大厅预约要求。照片回执和申请表要求可能因城市略有差异。
```

材料项：

```text
居民身份证原件｜必带
用于核验本人身份，建议同时准备一份复印件。未满 16 周岁或特殊情况请按窗口要求补充监护材料。

出入境证件照片回执｜必带
到有资质照相点拍摄后取得照片回执。

中国公民出入境证件申请表｜必带
可现场填写，也可按当地平台要求提前填写。

预约记录或预约短信｜可选
如果当地要求预约，请带上预约成功记录。
```

---

#### hk_macau_renewal：港澳通行证续签

标题：港澳通行证续签

办理提醒：

```text
先确认港澳通行证仍在有效期内。部分地区可用智能签注设备办理，特殊情况需到窗口。如证件损坏、过期或信息变更，可能需要重新办理证件。
```

材料项：

```text
有效港澳通行证｜必带
请确认通行证未过期、未损坏，个人信息清晰可识别。

居民身份证原件｜必带
用于现场身份核验或智能设备核验。

原有签注信息｜可选
用于判断签注是否过期、是否仍有有效次数。

预约记录或预约短信｜可选
如果当地要求预约，请带上预约成功记录。
```

---

#### border_crossing_prepare：过关材料准备

标题：过关材料准备

办理提醒：

```text
出发前先检查证件和签注有效性。常用药品建议保留原包装，并避免携带不明药品。老人出行建议提前保存家属电话和紧急联系人。
```

材料项：

```text
港澳通行证｜必带
过关时需要出示，建议放在容易拿取的位置。

有效签注｜必带
确认目的地、次数和有效期是否满足本次出行。

居民身份证｜可选
部分交通、住宿或现场核验可能需要。

手机与紧急联系人信息｜可选
建议提前保存家属电话，方便需要时联系。

必要药品｜可选
如需携带常用药，建议保留原包装并按规定携带。
```

---

#### service_uncertain_valid_pass：办理前核对清单

标题：办理前核对清单

办理提醒：

```text
您已确认有有效港澳通行证，但还不确定这次具体要办签注、换证、补发还是其他业务。建议先带身份证和港澳通行证到窗口，请工作人员核对证件和签注状态。
```

材料项：

```text
居民身份证原件｜必带
用于窗口或设备核验本人身份。

有效港澳通行证｜必带
请带上现有证件，方便工作人员核对证件状态。

原有签注信息｜可选
用于判断签注是否过期、是否仍有有效次数。

预约记录或预约短信｜可选
如果当地要求预约，请带上预约成功记录。

其他补充材料｜可选
如有证件损坏、信息变更、未成年人办理等情况，请按窗口要求补充材料。
```

---

## 6. MaterialChecklistScreen 页面要求

新增或完善 `MaterialChecklistScreen(checklistId)`。

### 6.1 页面结构

Compact 手机下使用单列布局：

```text
顶部栏
  返回按钮 + 居中标题

办理提醒
  浅蓝说明卡

材料核对
  已核对 x / y 项
  材料卡片列表

底部固定按钮
  保存清单 / 更新清单
```

### 6.2 材料卡片结构

每个材料项建议结构：

```text
[ ] 材料标题                         必带/可选
    材料说明
    补充提示 note，可为空
```

要求：

```text
Checkbox 点击区域至少 48dp
标题最多允许两行
必带标签使用醒目的橙色系
可选标签使用绿色系
卡片保持现有政务蓝、浅色背景、圆角风格
```

### 6.3 底部按钮

底部按钮固定在页面底部。

状态：

```text
未保存：保存清单
已保存：更新清单
```

点击后：

```text
保存 checkedItemIds
Toast：已保存到我的材料清单
按钮文案变为：更新清单
```

底部容器需要避开系统手势区域：

```kotlin
.navigationBarsPadding()
.imePadding()
```

如果已有项目封装，请使用已有方式。

---

## 7. 保存状态要求

本阶段只需要本地内存状态。

建议在当前 App 顶层状态、ViewModel 或可提升状态的位置保存：

```kotlin
val savedChecklists: MutableMap<String, SavedChecklistState>
```

或 Compose 状态：

```kotlin
var savedChecklists by rememberSaveable { mutableStateOf(...) }
```

如果 `Set<String>` 不能直接 `rememberSaveable`，可以用 `List<String>` 存储，使用时转换为 Set。

要求：

```text
1. 勾选状态在当前 App 运行期间可恢复。
2. 从我的页进入某个清单时，要恢复对应 checkedItemIds。
3. 保存同一个 checklistId 时，应更新已有记录，不重复创建多条。
4. 计算进度：checkedItemIds.size / checklist.items.size。
```

---

## 8. 我的页要求

### 8.1 只显示已保存清单

本阶段“我的”页只需要显示已保存的清单列表。

如果没有已保存清单，可以显示简单空状态：

```text
还没有保存的材料清单
您可以在服务页或办理判断结果页查看并保存清单。
```

如果有已保存清单，显示：

```text
我的材料清单

港澳通行证续签
已核对 0 / 4 项
继续核对

过关材料准备
已核对 2 / 5 项
继续核对
```

### 8.2 点击行为

点击已保存清单卡片：

```text
navigate(MaterialChecklist.createRoute(checklistId))
```

进入详情页后恢复勾选状态。

### 8.3 暂不做复杂管理页

本阶段不要额外新增复杂的“清单管理页”。

可以直接在我的页展示已保存清单。

如果项目已有“我的材料清单”入口卡，当前可以：

```text
点击后 Toast：当前已在我的页展示已保存清单
```

或者隐藏该入口，避免重复。

---

## 9. 第二优先级：打通办理判断结果页

当前已有“我不知道该怎么办 / 办理情况判断”Demo，不要重做。

本阶段只需要把判断结果页的“查看材料清单”接起来。

---

### 9.1 建议结果数据结构

如果当前已有结果结构，可以在原有结构上补字段。

建议结构：

```kotlin
data class DecisionResult(
    val resultId: String,
    val title: String,
    val summary: String,
    val nextStep: String,
    val staffScript: String,
    val policyQuery: String,
    val checklistId: String
)
```

如果项目已有类似类，请不要重复创建冲突类。

---

### 9.2 当前结果示例

对于当前 Demo 中类似结果：

```text
办理事项还需要确认

您已经确认有有效港澳通行证，但还不确定这次具体要办签注、换证、补发还是其他业务。建议带身份证和港澳通行证到窗口，请工作人员先核对证件和签注状态。
```

建议生成：

```kotlin
DecisionResult(
    resultId = "service_uncertain_valid_pass",
    title = "办理事项还需要确认",
    summary = "您已经确认有有效港澳通行证，但还不确定这次具体要办签注、换证、补发还是其他业务。",
    nextStep = "建议带身份证和港澳通行证到窗口，请工作人员先核对证件和签注状态。",
    staffScript = "我有港澳通行证，但不确定这次应该办签注、换证还是其他业务。请帮我看一下证件和签注状态。",
    policyQuery = "用户已有有效港澳通行证，但不确定这次应办理签注、换证、补发还是其他出入境业务。请根据官方资料说明应核对哪些信息、到窗口如何说明、可能对应哪些办理事项。",
    checklistId = "service_uncertain_valid_pass"
)
```

---

### 9.3 结果页按钮行为

结果页按钮应为：

```text
查看详细政策
  → 已经做了，保留现有行为
  → 跳转到智能问答页，并带入 policyQuery

重新选择情况
  → 返回办理情况判断页
  → 清空已选答案

查看材料清单
  → 新增行为
  → 跳转到 MaterialChecklistScreen(checklistId)
```

注意：

```text
“查看材料清单”不要固定跳某一个清单。
必须根据当前 DecisionResult.checklistId 跳转。
```

---

## 10. 导航 Route 建议

如果项目已有路由系统，请按现有风格接入。

建议 route：

```kotlin
object MaterialChecklist : Screen("material_checklist/{checklistId}") {
    fun createRoute(checklistId: String) = "material_checklist/$checklistId"
}
```

调用：

```kotlin
navController.navigate(MaterialChecklist.createRoute(checklistId))
```

NavHost：

```kotlin
composable(
    route = "material_checklist/{checklistId}",
    arguments = listOf(navArgument("checklistId") { type = NavType.StringType })
) { backStackEntry ->
    val checklistId = backStackEntry.arguments?.getString("checklistId") ?: "hk_macau_pass_apply"
    MaterialChecklistScreen(
        checklistId = checklistId,
        ...
    )
}
```

如果当前项目没有使用 `Screen` sealed class，请按现有导航写法处理。

---

## 11. 服务页入口建议

当前服务页中的普通服务入口大多仍然可以 Toast。

但与材料清单直接相关的入口可以跳转：

```text
过关材料准备
  → MaterialChecklistScreen("border_crossing_prepare")

办理判断结果页的查看材料清单
  → MaterialChecklistScreen(result.checklistId)

问答页的查看材料清单按钮，如已有
  → MaterialChecklistScreen("hk_macau_pass_apply") 或根据上下文选择
```

注意：

```text
不要把所有服务页入口都强行改成跳转。
未实现的服务继续 Toast。
```

---

## 12. UI 与适老化要求

保持当前 App 的风格：

```text
政务蓝
浅蓝提示卡
大字体
大点击区域
卡片化
清晰图标
高对比度
```

同时注意不要让页面过度臃肿。

### 材料清单页建议

```text
标题清楚，不要过长
材料卡片单列展示
说明文字不要太小
必带 / 可选标签固定在右上角
底部按钮不要贴住系统手势条
```

### 我的页建议

```text
只展示已保存清单
每张卡片显示标题、已核对 x/y、继续核对
点击整张卡片可进入详情
```

---

## 13. 为未来 Dify/RAG 预留，但本阶段不接

当前 Mock 数据结构要为未来替换数据源做准备。

推荐思想：

```text
当前：LocalMockChecklistProvider
未来：BackendChecklistProvider
再未来：DifyChecklistProvider
```

UI 层只接收：

```kotlin
MaterialChecklist
```

不要关心数据来自哪里。

---

### 13.1 未来 Dify/RAG 推荐方式

后续如果要接 Dify/RAG，不建议让大模型自由生成材料清单。

推荐方式：

```text
固定办理类型 / 判断结果
  ↓
生成标准 policyQuery
  ↓
Dify / RAG 检索官方知识库
  ↓
大模型按固定 JSON Schema 整理
  ↓
App 渲染材料清单
```

未来 JSON 格式可以参考：

```json
{
  "id": "hk_macau_renewal",
  "title": "港澳通行证续签",
  "reminder": "请先确认港澳通行证仍在有效期内。部分地区可通过智能签注设备办理，特殊情况需到窗口。",
  "source": "粤同心政策知识库",
  "items": [
    {
      "id": "valid_pass",
      "title": "有效港澳通行证",
      "description": "请确认通行证未过期、未损坏，个人信息清晰可识别。",
      "required": true,
      "note": null
    }
  ]
}
```

但本阶段不要实现这些网络请求。

---

## 14. 完成标准

完成后至少可以演示以下 3 条流程。

---

### 流程 A：服务页进入材料清单

```text
服务页
  → 点击“过关材料准备”
  → 进入“过关材料准备”材料清单详情页
  → 勾选 2 项材料
  → 点击“保存清单”
  → Toast 显示“已保存到我的材料清单”
  → 到“我的”页
  → 看到“过关材料准备 已核对 2 / 5 项”
  → 点击该清单
  → 回到清单详情页，刚才勾选的 2 项仍然选中
```

---

### 流程 B：办理判断进入材料清单

```text
服务页
  → 点击“我不知道该办哪种”
  → 回答问题
  → 到结果页
  → 点击“查看材料清单”
  → 进入“办理前核对清单”
  → 勾选材料
  → 保存
  → 我的页显示该清单
  → 点击可继续核对
```

---

### 流程 C：办理判断进入智能问答

```text
结果页
  → 点击“查看详细政策”
  → 进入智能问答页
  → 保留现有已完成逻辑：自动带入标准 policyQuery
```

---

## 15. Codex 执行提示词

请按以下要求修改当前 Android 项目：

```text
当前项目已经有“我不知道该办哪种 / 办理情况判断”Demo，也已经实现“查看详细政策”跳转到智能问答页并带入标准问题。请不要重做这些功能。

本阶段只做“本地 Mock 材料清单闭环”和“办理判断结果页 → 材料清单”的打通。

重点修改范围：
1. ElderCareScreens.kt
2. ElderCareComponents.kt
3. ElderResponsive.kt
4. 导航相关文件，例如 AppNavHost / MainActivity 中的 NavHost
5. 如已有 ViewModel / AppState，可小范围修改以保存本地状态

目标：
1. 新增或完善 MaterialChecklistScreen。
2. 支持 checklistId 路由：material_checklist/{checklistId}。
3. 第一版支持 4 个本地 Mock 清单：
   - hk_macau_pass_apply：办理港澳通行证
   - hk_macau_renewal：港澳通行证续签
   - border_crossing_prepare：过关材料准备
   - service_uncertain_valid_pass：办理前核对清单
4. 材料清单页支持：
   - 显示标题
   - 显示办理提醒
   - 显示材料项
   - 必带 / 可选标签
   - 勾选材料项
   - 底部按钮“保存清单 / 更新清单”
5. 保存后：
   - 保存 checkedItemIds 到本地共享状态
   - Toast：“已保存到我的材料清单”
   - 同一个 checklistId 再保存时更新记录，不重复创建
6. 我的页只显示已保存清单：
   - 标题
   - 已核对 x / y 项
   - 继续核对
   - 点击清单卡片跳转回 MaterialChecklistScreen(checklistId)
   - 恢复之前的勾选状态
7. 办理判断结果页：
   - 保留“查看详细政策”现有行为
   - “查看材料清单”根据当前结果的 checklistId 跳转到对应材料清单
   - 当前不确定业务结果使用 checklistId = service_uncertain_valid_pass
   - “重新选择情况”保留或优化现有逻辑
8. 服务页：
   - “过关材料准备”入口可跳转到 border_crossing_prepare 清单
   - 其他未实现服务继续 Toast，不要全部改成跳转
9. UI 保持当前适老化风格：
   - 政务蓝
   - 大字体
   - 大点击区域
   - 卡片化
   - 浅蓝提示卡
   - 高对比度
10. 底部保存按钮要避开系统手势条，使用 navigationBarsPadding / imePadding 或项目已有等价写法。
11. 当前阶段不接 Dify、不接 RAG、不接后端、不接数据库、不接真实政务 API。
12. 不要扫描或修改无关文件。
13. 修改完成后检查未使用 import、导航 route、编译错误。
```

---

## 16. 验收重点

最终验收时请重点确认：

```text
1. MaterialChecklistScreen 能根据 checklistId 显示不同清单。
2. 勾选状态可以保存。
3. 我的页只显示已保存清单。
4. 我的页点击已保存清单可以跳回详情页。
5. 跳回详情页后勾选状态仍然存在。
6. 办理判断结果页“查看材料清单”可以跳转。
7. “查看详细政策”原有功能不被破坏。
8. 未实现服务仍然 Toast。
9. 底部按钮没有被系统手势条遮挡。
10. 不涉及 Dify/RAG/API/数据库改动。
```
