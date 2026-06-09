package com.example.eldercareapp.ui.screen

import androidx.annotation.StringRes
import com.example.eldercareapp.R

data class FaqCategory(
    val id: String,
    @param:StringRes val titleResId: Int,
)

data class FaqItem(
    val id: String,
    val categoryId: String,
    @param:StringRes val titleResId: Int,
    val query: String,
)

const val FaqCategoryAll = "all"
private const val FaqCategoryDocument = "document"
private const val FaqCategoryBorder = "border"
private const val FaqCategoryTransport = "transport"
private const val FaqCategoryPolicy = "policy"
private const val FaqCategoryMedical = "medical"

val faqCategories = listOf(
    FaqCategory(FaqCategoryAll, R.string.faq_category_all),
    FaqCategory(FaqCategoryDocument, R.string.faq_category_document),
    FaqCategory(FaqCategoryBorder, R.string.faq_category_border),
    FaqCategory(FaqCategoryTransport, R.string.faq_category_transport),
    FaqCategory(FaqCategoryPolicy, R.string.faq_category_policy),
    FaqCategory(FaqCategoryMedical, R.string.faq_category_medical)
)

val faqItems = listOf(
    FaqItem(
        id = "apply_hk_macau_pass",
        categoryId = FaqCategoryDocument,
        titleResId = R.string.faq_q_apply_hk_macau_pass,
        query = "如何申请港澳通行证？"
    ),
    FaqItem(
        id = "expired_hk_macau_pass",
        categoryId = FaqCategoryDocument,
        titleResId = R.string.faq_q_expired_hk_macau_pass,
        query = "港澳通行证过期了怎么办？"
    ),
    FaqItem(
        id = "elder_pass_materials",
        categoryId = FaqCategoryDocument,
        titleResId = R.string.faq_q_elder_pass_materials,
        query = "老年人办理港澳通行证需要什么材料？"
    ),
    FaqItem(
        id = "elder_border_convenience",
        categoryId = FaqCategoryBorder,
        titleResId = R.string.faq_q_elder_border_convenience,
        query = "老年人通关有哪些便利措施？"
    ),
    FaqItem(
        id = "restricted_items",
        categoryId = FaqCategoryBorder,
        titleResId = R.string.faq_q_restricted_items,
        query = "哪些物品不能过关？"
    ),
    FaqItem(
        id = "border_wait_time",
        categoryId = FaqCategoryBorder,
        titleResId = R.string.faq_q_border_wait_time,
        query = "过关需要多长时间？"
    ),
    FaqItem(
        id = "border_notice",
        categoryId = FaqCategoryBorder,
        titleResId = R.string.faq_q_border_notice,
        query = "过关时需要注意什么？"
    ),
    FaqItem(
        id = "elder_transport_discount",
        categoryId = FaqCategoryTransport,
        titleResId = R.string.faq_q_elder_transport_discount,
        query = "老年人过关交通有哪些优惠？"
    ),
    FaqItem(
        id = "border_transport_options",
        categoryId = FaqCategoryTransport,
        titleResId = R.string.faq_q_border_transport_options,
        query = "过关有哪些交通方式？"
    ),
    FaqItem(
        id = "less_crowded_port",
        categoryId = FaqCategoryTransport,
        titleResId = R.string.faq_q_less_crowded_port,
        query = "哪个口岸人比较少？"
    ),
    FaqItem(
        id = "bay_area_elder_benefits",
        categoryId = FaqCategoryPolicy,
        titleResId = R.string.faq_q_bay_area_elder_benefits,
        query = "粤港澳大湾区有哪些养老福利？"
    ),
    FaqItem(
        id = "senior_allowance",
        categoryId = FaqCategoryPolicy,
        titleResId = R.string.faq_q_senior_allowance,
        query = "如何申请高龄津贴？"
    ),
    FaqItem(
        id = "social_medical_transfer",
        categoryId = FaqCategoryPolicy,
        titleResId = R.string.faq_q_social_medical_transfer,
        query = "社保医保如何转移？"
    ),
    FaqItem(
        id = "nearby_hospital",
        categoryId = FaqCategoryMedical,
        titleResId = R.string.faq_q_nearby_hospital,
        query = "查找口岸附近医院"
    ),
    FaqItem(
        id = "cross_region_medical_claim",
        categoryId = FaqCategoryMedical,
        titleResId = R.string.faq_q_cross_region_medical_claim,
        query = "医保异地就医怎么报销？"
    ),
    FaqItem(
        id = "bring_medicine",
        categoryId = FaqCategoryMedical,
        titleResId = R.string.faq_q_bring_medicine,
        query = "过去可以带药品吗？"
    ),
    FaqItem(
        id = "elder_medical_convenience",
        categoryId = FaqCategoryMedical,
        titleResId = R.string.faq_q_elder_medical_convenience,
        query = "老年人过关就医有哪些便利？"
    )
)

private val defaultHomeFaqIds = listOf(
    "apply_hk_macau_pass",
    "elder_border_convenience",
    "elder_transport_discount"
)

fun faqItemsForCategory(categoryId: String): List<FaqItem> {
    if (categoryId != FaqCategoryAll) {
        return faqItems.filter { it.categoryId == categoryId }
    }

    val defaultItems = defaultHomeFaqIds.mapNotNull { defaultId ->
        faqItems.firstOrNull { it.id == defaultId }
    }
    val defaultIdSet = defaultHomeFaqIds.toSet()
    return defaultItems + faqItems.filter { it.id !in defaultIdSet }
}
