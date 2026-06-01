package com.example.eldercareapp.model

data class ChatPolicyRequest(
    val message: String,
    val conversation_id: String? = "",
    val user_id: String? = "demo-user-001",
    val input_type: String? = "text",
    val language: String? = "zh-CN",
    val tts_language: String? = "zh-CN",
)

data class SourceItem(
    val document_name: String? = null,
    val title: String? = null,
    val score: Double? = null,
    val content: String? = null,
)

data class ChatPolicyResponse(
    val answer: String = "",
    val conversation_id: String? = "",
    val original_text: String? = "",
    val search_query: String? = "",
    val display_text: String? = "",
    val structured_answer: StructuredAnswer? = null,
    val summary: String? = null,
    val conclusion: String? = null,
    val steps: List<String> = emptyList(),
    val materials: ChatMaterialsSection? = null,
    val required_materials: List<String> = emptyList(),
    val optional_materials: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val source_title: String? = null,
    val confidence: String? = null,
    val tts: TtsInfo? = null,
    val sources: List<SourceItem> = emptyList(),
    val usage: Map<String, Any> = emptyMap(),
)

data class StructuredAnswer(
    val title: String = "",
    val summary: String = "",
    val scenario_options: List<String> = emptyList(),
    val steps: List<String> = emptyList(),
    val materials: MaterialBlock? = MaterialBlock(),
    val warnings: List<String> = emptyList(),
    val detail_text: String = "",
    val source_note: String = "资料依据：知识库中的相关官方指南/政策说明",
    val confidence: String = "medium",
    val need_human_reminder: Boolean = true,
)

data class MaterialBlock(
    val required: List<String> = emptyList(),
    val optional: List<String> = emptyList(),
)

data class ChatMaterialsSection(
    val required: List<String> = emptyList(),
    val possible_extra: List<String> = emptyList(),
    val optional: List<String> = emptyList(),
)

data class QaAnswerUiModel(
    val title: String = "我帮您查到这些",
    val subtitle: String = "根据办事资料整理，办理前请以当地窗口要求为准",
    val conclusion: String = "",
    val scenarioOptions: List<QaScenarioOption> = defaultQaScenarioOptions(),
    val steps: List<String> = emptyList(),
    val requiredMaterials: List<String> = emptyList(),
    val optionalMaterials: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val sourceTitle: String? = null,
    val originalAnswer: String? = null,
    val confidence: String? = null,
    val needHumanReminder: Boolean = true,
)

data class QaScenarioOption(
    val label: String,
    val standardQuestion: String,
)

fun defaultQaScenarioOptions(): List<QaScenarioOption> = listOf(
    QaScenarioOption(
        label = "首次办理",
        standardQuestion = "第一次办理港澳通行证需要怎么做？"
    ),
    QaScenarioOption(
        label = "已有证件续签",
        standardQuestion = "已有港澳通行证，签注过期或用完了怎么办？"
    ),
    QaScenarioOption(
        label = "证件过期/遗失",
        standardQuestion = "港澳通行证过期、遗失或损坏了应该怎么办？"
    ),
    QaScenarioOption(
        label = "我不确定",
        standardQuestion = "我不确定自己属于哪种港澳办理情况，应该怎么判断？"
    )
)

data class TtsInfo(
    val language: String? = "zh-CN",
    val voice: String? = "longxiaochun_v3",
    val text: String? = "",
    val audio_url: String? = null,
    val cached: Boolean = false,
)

data class TtsSynthesizeRequest(
    val text: String,
    val language: String = "zh-CN",
    val voice: String? = null,
)

data class TtsSynthesizeResponse(
    val text: String = "",
    val language: String? = "zh-CN",
    val voice: String? = "longxiaochun_v3",
    val audio_url: String? = null,
    val cached: Boolean = false,
)

data class VoiceTranscribeResponse(
    val original_text: String,
    val language: String? = "auto",
    val usage: Map<String, Any> = emptyMap(),
    val request_id: String? = "",
)
