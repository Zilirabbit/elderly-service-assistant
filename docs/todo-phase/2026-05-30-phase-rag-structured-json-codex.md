# 2026-05-30 Phase：RAG 结构化 JSON 回答接入与问答页数据稳定化

## 1. 背景

当前问答页 UI 已经改成“结构化办事卡片”方向，包括：

- 结论卡片
- 情况选择按钮
- 详细说明
- 材料清单入口
- 注意事项
- 资料来源
- 朗读回答 / 查看材料清单 / 继续追问按钮

但当前后端 `ChatPolicyResponse` 仍主要返回纯文本字段：

```python
answer: str
display_text: str
sources: list[SourceItem]
tts: TtsInfo
```

这会导致 Android 端仍然需要从自然语言 / Markdown 中解析“结论、材料、步骤、注意事项”，容易出现：

- 结论区混入材料或步骤；
- 明明回答文本里有材料，但材料模块显示“没有单独列出材料项”；
- `detailText` 太长，前端不好折叠；
- Markdown 小标题解析不稳定。

本阶段目标是把 RAG 输出从 Markdown 文本改为结构化 JSON，并让 FastAPI 负责解析、兜底和统一返回，Android 只按字段渲染，不再硬拆长文本。

---

## 2. 当前项目结构相关文件

本次修改只涉及 RAG 问答链路，不改知识库流水线，不改材料清单真实数据库，不重做 UI。

预计涉及文件：

```text
elderly-service-assistant/
│
├── dify-config/
│   └── apps/
│       └── policy_qa_prompt_structured_json.md        # 新增或更新：Dify JSON Prompt 文档
│
├── backend/
│   └── app/
│       ├── schemas/
│       │   └── chat_schema.py                         # 修改：新增 StructuredAnswer 等 schema
│       │
│       ├── services/
│       │   └── dify_service.py                        # 修改：解析 Dify JSON + fallback
│       │
│       └── api/
│           └── routes_chat.py                         # 修改：返回 structured_answer
│
└── ElderCareApp/
    └── app/src/main/java/com/example/eldercareapp/
        ├── model/
        │   └── ChatModels.kt                          # 修改：新增 structuredAnswer 对应 data class
        │
        ├── viewmodel/
        │   └── ChatViewModel.kt                       # 修改：优先保存 structuredAnswer
        │
        └── ui/
            ├── screen/
            │   └── ElderCareScreens.kt                # 修改：问答页优先渲染 structuredAnswer
            └── component/
                └── ElderCareComponents.kt             # 修改：结构化回答卡片组件
```

如当前仓库实际文件名不同，请以现有路径为准，优先搜索：

```text
ChatPolicyResponse
ChatPolicyRequest
DifyService
chat-policy
StructuredAnswer
AnswerCard
```

---

## 3. 本阶段目标

### 3.1 必须完成

1. Dify Prompt 已经要求输出合法 JSON，字段名统一使用 `snake_case`。
2. FastAPI 新增结构化问答 schema：`StructuredAnswer`、`MaterialBlock`。
3. `/api/v1/chat-policy` 响应中新增 `structured_answer` 字段。
4. 保留旧字段 `answer`、`display_text`、`tts`、`sources`，避免 Android 旧逻辑崩溃。
5. `dify_service.py` 尝试解析 Dify 返回的 JSON。
6. 如果 Dify 返回不是合法 JSON，要 fallback 成结构化对象，不要让接口报 500。
7. Android 优先使用 `structuredAnswer` 渲染问答卡片。
8. 若 `structuredAnswer.summary` 为空，则回退显示 `displayText` / `answer`。
9. 不转换 Dify 知识流水线，不点击 Dify 的“转换为知识流水线”。

### 3.2 不做

本阶段不要做以下内容：

- 不重做问答页整体 UI；
- 不接真实预约；
- 不做历史记录云同步；
- 不改材料清单数据库；
- 不做 Dify 知识库“流水线转换”；
- 不改 ASR / TTS 主链路；
- 不大改项目架构；
- 不删除旧字段；
- 不把 API Key 放到 Android。

---

## 4. Dify Prompt

在 Dify 应用中使用以下结构化 JSON Prompt。

建议同步保存到：

```text
dify-config/apps/policy_qa_prompt_structured_json.md
```

Prompt 内容：

```text
你是“粤同心-湾区中老年助手”，面向中老年用户和家属提供港澳通行证、签注办理、换发补发和过关材料准备说明。

你的任务：
根据知识库内容，回答用户关于港澳通行证、签注办理、换发补发、过关材料准备等问题，并输出适合 Android 前端展示的 JSON。

重要规则：
1. 必须优先依据知识库内容回答，不要编造政策。
2. 如果知识库没有明确依据，必须在 summary 或 warnings 中说明：“目前资料中没有找到明确说明，建议咨询当地公安出入境窗口或口岸工作人员”。
3. 回答要适合中老年人阅读，语言口语化、清楚、礼貌，但不能过度简化政策。
4. 不要使用绝对化表述，例如“肯定可以”“一定可以”“绝对不能”。应使用“一般可以”“通常需要”“部分情况可能需要人工窗口办理”等表达。
5. summary 只能写结论和最关键去向，最多 2 句话；不要写材料清单、预约、指纹采集、费用、时限、有效期等细节。
6. summary 第一句应尽量直接回答“能不能办”，例如“一般可以办理”或“目前资料中没有找到明确说明”。
7. 如果用户问题可能涉及多种办理情形，例如首次办理、签注续签、换发、补发、过关材料，应在 scenario_options 中给出可选情形。
8. steps 最多 5 条，每条不超过 30 个字。
9. materials.required 和 materials.optional 必须是数组。没有明确材料时返回空数组，不要编造。
10. materials.required 中尽量使用正式材料名称，不要只写“申请表”“照片”这类过短名称。
11. 如果知识库提到未成年人、异地办理、监护人陪同、特殊人群、补充证明等情况，应放入 materials.optional 或 warnings。
12. warnings 最多 4 条，用于放注意事项、人工核实提醒和政策不确定提醒。
13. 涉及签注、有效期、办理时限、费用、全国通办、自助机办理时，必须在 warnings 中提醒：“以当地出入境管理部门最新要求为准”。
14. 有效期、费用、办理时限等具体数字，只有知识库明确召回时才允许写入；否则只做原则性提醒。
15. warnings 优先放风险提醒和人工核实提醒；有效期、费用、办理时限等数字信息如确有依据，可放入 detail_text，不要占用多条 warnings。
16. detail_text 可以放较完整说明，但控制在 300 字以内；不要重复完整 steps 和 materials。
17. source_note 固定说明资料依据，例如“资料依据：知识库中的相关官方指南/政策说明”。
18. confidence 只能是 high、medium、low。
19. need_human_reminder 涉及政策办理时一般为 true。
20. 只输出合法 JSON，不要输出 Markdown，不要输出代码块，不要在 JSON 前后添加任何解释文字。
21. 字段名必须使用 snake_case，不要使用 camelCase。

必须严格输出以下 JSON 结构：

{
  "title": "",
  "summary": "",
  "scenario_options": [],
  "steps": [],
  "materials": {
    "required": [],
    "optional": []
  },
  "warnings": [],
  "detail_text": "",
  "source_note": "资料依据：知识库中的相关官方指南/政策说明",
  "confidence": "medium",
  "need_human_reminder": true
}

字段说明：
- title：本次回答的简短标题，例如“首次办理港澳通行证”
- summary：最重要的结论，1 到 2 句话
- scenario_options：用户可能需要继续确认的情况，例如“首次办理”“已有证件续签”“证件过期”“不确定”
- steps：办理步骤，最多 5 条
- materials.required：知识库明确提到的必备材料
- materials.optional：可能需要、建议携带或特殊情况材料
- warnings：注意事项、政策不确定提醒和人工核实提醒
- detail_text：补充说明，适合前端“展开查看”
- source_note：资料依据说明
- confidence：只能是 high、medium、low
- need_human_reminder：涉及政策办理时一般为 true

如果知识库没有明确答案，仍然输出 JSON，例如：

{
  "title": "暂未找到明确说明",
  "summary": "目前资料中没有找到明确说明，建议咨询当地公安出入境窗口或口岸工作人员。",
  "scenario_options": ["重新提问", "查看常见问题", "咨询窗口"],
  "steps": [],
  "materials": {
    "required": [],
    "optional": []
  },
  "warnings": [
    "不要仅凭 AI 回答办理证件业务",
    "具体要求以当地公安出入境窗口或口岸工作人员说明为准"
  ],
  "detail_text": "当前知识库没有检索到足够明确的政策依据。建议换一种说法重新提问，或直接咨询当地公安出入境窗口。",
  "source_note": "资料依据：知识库中的相关官方指南/政策说明",
  "confidence": "low",
  "need_human_reminder": true
}

用户问题：
{{query}}
```

注意：

- 如果 Dify Prompt 使用 `{{query}}` 变量，则 API 调用时必须同时传：
  - 顶层 `query`
  - `inputs.query`
- Dify 调试页面右侧的 `query` 输入框也必须填写，否则会出现 `query 必填`。

---

## 5. FastAPI Schema 修改方案

当前 `backend/app/schemas/chat_schema.py` 只有文本型响应。请兼容式新增结构化字段，不删除旧字段。

建议修改为：

```python
from typing import Any

from pydantic import BaseModel, Field


class ChatPolicyRequest(BaseModel):
    message: str = Field(..., min_length=1, description="用户输入的问题")
    conversation_id: str | None = ""
    user_id: str | None = None
    input_type: str | None = "text"
    tts_language: str | None = "zh-CN"


class SourceItem(BaseModel):
    document_name: str | None = None
    title: str | None = None
    score: float | None = None
    content: str | None = None
    source_type: str | None = "knowledge_base"


class TtsInfo(BaseModel):
    language: str = "zh-CN"
    voice: str = "longxiaochun_v3"
    text: str = ""
    audio_url: str | None = None
    cached: bool = False


class MaterialBlock(BaseModel):
    required: list[str] = Field(default_factory=list)
    optional: list[str] = Field(default_factory=list)


class StructuredAnswer(BaseModel):
    title: str = ""
    summary: str = ""
    scenario_options: list[str] = Field(default_factory=list)
    steps: list[str] = Field(default_factory=list)
    materials: MaterialBlock = Field(default_factory=MaterialBlock)
    warnings: list[str] = Field(default_factory=list)
    detail_text: str = ""
    source_note: str = "资料依据：知识库中的相关官方指南/政策说明"
    confidence: str = "medium"
    need_human_reminder: bool = True


class ChatPolicyResponse(BaseModel):
    answer: str = ""
    conversation_id: str = ""
    original_text: str = ""
    search_query: str = ""
    display_text: str = ""
    structured_answer: StructuredAnswer = Field(default_factory=StructuredAnswer)
    tts: TtsInfo = Field(default_factory=TtsInfo)
    sources: list[SourceItem] = Field(default_factory=list)
    usage: dict[str, Any] = Field(default_factory=dict)
```

兼容原则：

- `answer` 继续保留，用于旧 Android 或调试；
- `display_text` 继续保留，用于 TTS 改写或纯文本兜底；
- 新增 `structured_answer`，供新版问答页渲染；
- `SourceItem` 增加 `title` 和 `source_type`，但保留 `document_name`。

---

## 6. Dify Service 修改方案

在 `backend/app/services/dify_service.py` 中增加 JSON 解析和 fallback。

### 6.1 Dify 请求 payload

如果 Prompt 使用了 `{{query}}`，payload 需要这样构造：

```python
payload = {
    "inputs": {
        "query": message
    },
    "query": message,
    "response_mode": "blocking",
    "conversation_id": conversation_id or "",
    "user": user_id or "eldercare-user"
}
```

重点：

- `inputs.query` 给 Prompt 变量 `{{query}}` 使用；
- 顶层 `query` 给 Dify chat message 使用；
- 两个都传，减少调试和 API 调用不一致。

### 6.2 解析 Dify 返回文本

新增函数，例如：

```python
import json
import re

from app.schemas.chat_schema import StructuredAnswer, MaterialBlock


def _strip_json_text(text: str) -> str:
    value = (text or "").strip()

    # 兼容模型偶尔输出 ```json ... ```
    if value.startswith("```"):
        value = re.sub(r"^```(?:json)?", "", value, flags=re.IGNORECASE).strip()
        value = re.sub(r"```$", "", value).strip()

    # 兼容 JSON 前后偶尔带解释文字，尽量截取首个对象
    start = value.find("{")
    end = value.rfind("}")
    if start >= 0 and end > start:
        value = value[start:end + 1]

    return value


def parse_structured_answer(raw_answer: str) -> StructuredAnswer:
    try:
        data = json.loads(_strip_json_text(raw_answer))
        if not isinstance(data, dict):
            raise ValueError("Dify answer JSON is not an object")

        return StructuredAnswer(
            title=data.get("title", "") or "",
            summary=data.get("summary", "") or "",
            scenario_options=data.get("scenario_options") or [],
            steps=data.get("steps") or [],
            materials=MaterialBlock(
                required=(data.get("materials") or {}).get("required") or [],
                optional=(data.get("materials") or {}).get("optional") or [],
            ),
            warnings=data.get("warnings") or [],
            detail_text=data.get("detail_text", "") or "",
            source_note=data.get("source_note", "资料依据：知识库中的相关官方指南/政策说明"),
            confidence=data.get("confidence", "medium") or "medium",
            need_human_reminder=bool(data.get("need_human_reminder", True)),
        )
    except Exception:
        return fallback_structured_answer(raw_answer)


def fallback_structured_answer(raw_answer: str) -> StructuredAnswer:
    text = (raw_answer or "").strip()
    return StructuredAnswer(
        title="查询结果",
        summary="我帮您查到以下说明，具体要求请以当地窗口为准。",
        scenario_options=["继续追问", "查看材料清单", "咨询窗口"],
        steps=[],
        materials=MaterialBlock(required=[], optional=[]),
        warnings=[
            "当前回答未能稳定解析为结构化内容",
            "具体要求以当地出入境管理部门最新要求为准"
        ],
        detail_text=text,
        source_note="资料依据：知识库中的相关官方指南/政策说明",
        confidence="low",
        need_human_reminder=True,
    )
```

### 6.3 返回内容映射

Dify 返回的原始回答假设为 `raw_answer`，解析后：

```python
structured_answer = parse_structured_answer(raw_answer)

display_text = structured_answer.detail_text or structured_answer.summary or raw_answer
answer = raw_answer
```

返回 `ChatPolicyResponse` 时：

```python
return ChatPolicyResponse(
    answer=raw_answer,
    conversation_id=conversation_id,
    original_text=original_text or message,
    search_query=search_query or message,
    display_text=display_text,
    structured_answer=structured_answer,
    tts=tts_info,
    sources=sources,
    usage=usage,
)
```

注意：

- 不要因为 JSON 解析失败而让接口失败；
- JSON 解析失败时 `structured_answer.confidence = "low"`；
- `detail_text` 可以用作 TTS 文本来源，但后续 TTS 仍可继续使用原有逻辑。

---

## 7. routes_chat.py 修改方案

`routes_chat.py` 中保持接口路径不变：

```text
POST /api/v1/chat-policy
```

只调整内部调用和返回结构：

1. 接收 `ChatPolicyRequest`。
2. 调用 `dify_service`。
3. 返回 `ChatPolicyResponse`。
4. 如果服务异常，返回可理解错误，不暴露堆栈和密钥。

错误兜底建议：

```python
except Exception:
    fallback = StructuredAnswer(
        title="查询失败",
        summary="系统暂时没有查询成功，请稍后再试。",
        scenario_options=["重新查询", "查看常见问题"],
        steps=[],
        materials=MaterialBlock(),
        warnings=["如果多次失败，建议直接咨询当地窗口"],
        detail_text="当前问答服务暂时不可用，您可以稍后重试或使用常见问题入口。",
        confidence="low",
        need_human_reminder=True,
    )
    return ChatPolicyResponse(
        answer=fallback.detail_text,
        display_text=fallback.detail_text,
        structured_answer=fallback,
    )
```

---

## 8. Android 修改方案

### 8.1 ChatModels.kt

新增与后端对应的数据类。Kotlin 可以继续使用 camelCase 属性，但需要根据当前 JSON 库决定是否加映射注解。

如果使用 Gson：

```kotlin
import com.google.gson.annotations.SerializedName

data class ChatPolicyResponse(
    val answer: String = "",
    @SerializedName("conversation_id")
    val conversationId: String = "",
    @SerializedName("original_text")
    val originalText: String = "",
    @SerializedName("search_query")
    val searchQuery: String = "",
    @SerializedName("display_text")
    val displayText: String = "",
    @SerializedName("structured_answer")
    val structuredAnswer: StructuredAnswer = StructuredAnswer(),
    val tts: TtsInfo = TtsInfo(),
    val sources: List<SourceItem> = emptyList()
)

data class StructuredAnswer(
    val title: String = "",
    val summary: String = "",
    @SerializedName("scenario_options")
    val scenarioOptions: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val materials: MaterialBlock = MaterialBlock(),
    val warnings: List<String> = emptyList(),
    @SerializedName("detail_text")
    val detailText: String = "",
    @SerializedName("source_note")
    val sourceNote: String = "资料依据：知识库中的相关官方指南/政策说明",
    val confidence: String = "medium",
    @SerializedName("need_human_reminder")
    val needHumanReminder: Boolean = true
)

data class MaterialBlock(
    val required: List<String> = emptyList(),
    val optional: List<String> = emptyList()
)
```

如果项目使用 kotlinx.serialization，请改用 `@SerialName`。

### 8.2 UI 渲染规则

问答页渲染时：

```kotlin
val structured = response.structuredAnswer

if (structured.summary.isNotBlank()) {
    StructuredAnswerCard(answer = structured)
} else {
    PlainAnswerCard(text = response.displayText.ifBlank { response.answer })
}
```

结构化卡片建议展示顺序：

```text
1. 标题：我帮您查到这些
2. 结论：structured.summary
3. 您是哪种情况：structured.scenarioOptions
4. 怎么办：structured.steps
5. 需要什么材料：
   - 必备材料：structured.materials.required
   - 可能需要：structured.materials.optional
6. 注意事项：structured.warnings
7. 详细说明：structured.detailText，默认折叠或限制行数
8. 来源：structured.sourceNote
9. 操作按钮：
   - 查看材料清单
   - 朗读回答
   - 继续追问
```

### 8.3 UI 细节要求

- `summary` 卡片最多展示 2 到 3 行。
- `detailText` 默认最多 6 行，提供“展开完整说明”。
- 如果 `materials.required` 和 `materials.optional` 都为空，显示：
  ```text
  本次回答没有明确列出材料项，请打开材料清单或以窗口要求为准。
  ```
- 如果 `warnings` 为空，仍显示：
  ```text
  具体要求以当地出入境管理部门最新要求为准。
  ```
- 顶部快捷问题按钮不要截断：
  ```text
  首次办证 / 签注续签 / 过关材料 / 不确定
  ```
- 对话列表底部 padding 需要包含输入栏和底部导航高度，避免内容被遮挡。

---

## 9. TTS 处理建议

本阶段不重做 TTS。保留现有 `TtsInfo` 和 `tts_schema.py`。

朗读文本优先级建议：

```text
structured_answer.summary
+ structured_answer.detail_text
+ structured_answer.warnings
```

最小实现可以继续读：

```text
response.display_text
```

但建议后续改为：

```python
tts_text = structured_answer.detail_text or structured_answer.summary or display_text
```

注意：

- 不要把完整 JSON 传给 TTS；
- 不要朗读 source_note；
- 不要朗读太长的材料清单；
- TTS 失败不影响文字回答。

---

## 10. 测试问题

先用以下问题做 Dify 和后端联调：

```text
第一次办理港澳通行证需要怎么做？
签注过期了怎么办？
港澳通行证过期了怎么办？
已有港澳通行证，只是签注用完了，还要重新办证吗？
去香港过关要带什么？
老人不会网上预约怎么办？
未满16周岁办理港澳通行证需要什么？
港澳通行证丢了怎么办？
我不知道自己是办证还是续签怎么办？
去澳门旅游需要准备什么材料？
```

每条测试检查：

```text
[ ] Dify 原始输出是合法 JSON
[ ] 字段名是 snake_case
[ ] summary 不超过 2 句话
[ ] materials.required 是数组
[ ] materials.optional 是数组
[ ] warnings 不超过 4 条
[ ] detail_text 不超过 300 字
[ ] FastAPI 返回 structured_answer
[ ] Android 能展示结构化卡片
[ ] JSON 解析失败时不会崩溃
```

---

## 11. 验收标准

### 后端验收

```text
[ ] chat_schema.py 中存在 StructuredAnswer、MaterialBlock
[ ] ChatPolicyResponse 保留旧字段并新增 structured_answer
[ ] dify_service.py 能解析 Dify JSON
[ ] Dify 输出 ```json 代码块时也能清洗解析
[ ] Dify 输出前后有多余文字时尽量截取 JSON 对象
[ ] JSON 解析失败时 fallback，不返回 500
[ ] /api/v1/chat-policy 返回 answer、display_text、structured_answer
[ ] response 中 structured_answer.materials.required 可正常被 Android 使用
```

### Android 验收

```text
[ ] ChatModels.kt 能接收 structured_answer
[ ] 问答页优先渲染 structuredAnswer
[ ] 结论卡片只展示 summary
[ ] 材料卡片展示 required / optional
[ ] detailText 默认折叠或限制行数
[ ] warnings 正常展示
[ ] sourceNote 正常展示
[ ] structuredAnswer 为空时回退显示 displayText / answer
[ ] 底部输入栏不遮挡最后一个按钮
```

### 展示验收

```text
[ ] 用户问“第一次办理港澳通行证需要怎么做？”
[ ] 页面显示“结论：一般可以办理……”
[ ] 页面显示“怎么办”步骤列表
[ ] 页面显示“必备材料”
[ ] 页面显示“可能需要”
[ ] 页面显示“注意事项”
[ ] 页面底部有“查看材料清单 / 朗读回答 / 继续追问”
```

---

## 12. 重要限制

1. 本阶段不要点击 Dify 知识库页面的“转换为知识流水线”。
2. 本阶段不修改知识库文档切片策略。
3. 本阶段不要求召回质量最终定版，只解决返回结构和前端渲染稳定性。
4. 本阶段不删除旧文本字段，避免影响现有功能。
5. 本阶段不要把材料清单页改成完全依赖 RAG，材料页仍可保留原有静态 / mock 数据。
6. 本阶段不要扩大到语音输入、语音识别或方言翻译改造。

---

## 13. 推荐提交信息

```text
feat: add structured RAG answer schema and JSON parsing fallback
```

或：

```text
phase: stabilize QA structured response rendering
```
