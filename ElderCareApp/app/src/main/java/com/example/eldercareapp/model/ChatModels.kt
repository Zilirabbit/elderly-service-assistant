package com.example.eldercareapp.model

data class ChatPolicyRequest(
    val message: String,
    val conversation_id: String? = "",
    val user_id: String? = "demo-user-001",
)

data class SourceItem(
    val document_name: String? = null,
    val score: Double? = null,
    val content: String? = null,
)

data class ChatPolicyResponse(
    val answer: String,
    val conversation_id: String? = "",
    val sources: List<SourceItem> = emptyList(),
    val usage: Map<String, Any> = emptyMap(),
)
