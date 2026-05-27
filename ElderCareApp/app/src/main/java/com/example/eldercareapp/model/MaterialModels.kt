package com.example.eldercareapp.model

data class MaterialItem(
    val code: String,
    val title: String,
    val subtitle: String,
    val category: String,
)

data class MaterialRequirement(
    val id: String,
    val name: String,
    val description: String,
    val required: Boolean = true,
    val note: String? = null,
)

data class MaterialChecklist(
    val code: String,
    val title: String,
    val tips: List<String> = emptyList(),
    val requirements: List<MaterialRequirement> = emptyList(),
)

data class SaveMaterialRequest(
    val item_code: String,
    val checked_requirement_ids: List<String> = emptyList(),
)

data class SaveMaterialResponse(
    val item_code: String,
    val checked_requirement_ids: List<String> = emptyList(),
    val saved_count: Int,
    val message: String,
)
