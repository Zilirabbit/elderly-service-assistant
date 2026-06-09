package com.example.eldercareapp.model

import androidx.annotation.StringRes

data class MaterialItem(
    val code: String,
    val title: String,
    val subtitle: String,
    val category: String,
    @param:StringRes val titleResId: Int? = null,
    @param:StringRes val subtitleResId: Int? = null,
    @param:StringRes val categoryResId: Int? = null,
)

data class MaterialRequirement(
    val id: String,
    val name: String,
    val description: String,
    val required: Boolean = true,
    val note: String? = null,
    val linkedChecklistId: String? = null,
    val linkedActionLabel: String? = null,
    @param:StringRes val nameResId: Int? = null,
    @param:StringRes val descriptionResId: Int? = null,
    @param:StringRes val noteResId: Int? = null,
    @param:StringRes val linkedActionLabelResId: Int? = null,
)

data class MaterialChecklist(
    val code: String,
    val title: String,
    val tips: List<String> = emptyList(),
    val requirements: List<MaterialRequirement> = emptyList(),
    @param:StringRes val titleResId: Int? = null,
    val tipResIds: List<Int> = emptyList(),
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
