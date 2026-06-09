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
    val title: String = "",
    val subtitle: String = "",
    val conclusion: String = "",
    val scenarioOptions: List<QaScenarioOption> = emptyList(),
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

data class ChatHistoryItem(
    val id: String = "",
    val question: String = "",
    val answerPreview: String = "",
    val messages: List<ChatHistoryMessage> = emptyList(),
    val conversationId: String = "",
    val inputType: String = "text",
    val displayLanguage: String = "zh-CN",
    val speechLanguage: String = "zh-CN",
    val createdAtMillis: Long = 0L,
    val updatedAtMillis: Long = 0L,
)

data class ChatHistoryMessage(
    val id: Long = 0L,
    val role: String = "user",
    val text: String = "",
    val answerUiModel: QaAnswerUiModel? = null,
    val ttsText: String = "",
    val ttsAudioUrl: String? = null,
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
