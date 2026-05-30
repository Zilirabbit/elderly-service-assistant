package com.example.eldercareapp.ui.screen

data class FaqItem(
    val category: String,
    val question: String
)

val faqCategories = listOf(
    "全部",
    "证件办理",
    "通关流程",
    "交通出行",
    "政策福利",
    "医疗健康"
)

val faqItems = listOf(
    FaqItem(
        category = "证件办理",
        question = "如何申请港澳通行证？"
    ),
    FaqItem(
        category = "证件办理",
        question = "港澳通行证过期了怎么办？"
    ),
    FaqItem(
        category = "证件办理",
        question = "老年人办理港澳通行证需要什么材料？"
    ),

    FaqItem(
        category = "通关流程",
        question = "老年人通关有哪些便利措施？"
    ),
    FaqItem(
        category = "通关流程",
        question = "哪些物品不能过关？"
    ),
    FaqItem(
        category = "通关流程",
        question = "过关需要多长时间？"
    ),
    FaqItem(
        category = "通关流程",
        question = "过关时需要注意什么？"
    ),

    FaqItem(
        category = "交通出行",
        question = "老年人过关交通有哪些优惠？"
    ),
    FaqItem(
        category = "交通出行",
        question = "过关有哪些交通方式？"
    ),
    FaqItem(
        category = "交通出行",
        question = "哪个口岸人比较少？"
    ),

    FaqItem(
        category = "政策福利",
        question = "粤港澳大湾区有哪些养老福利？"
    ),
    FaqItem(
        category = "政策福利",
        question = "如何申请高龄津贴？"
    ),
    FaqItem(
        category = "政策福利",
        question = "社保医保如何转移？"
    ),

    FaqItem(
        category = "医疗健康",
        question = "查找口岸附近医院"
    ),
    FaqItem(
        category = "医疗健康",
        question = "医保异地就医怎么报销？"
    ),
    FaqItem(
        category = "医疗健康",
        question = "过去可以带药品吗？"
    ),
    FaqItem(
        category = "医疗健康",
        question = "老年人过关就医有哪些便利？"
    )
)

private val defaultHomeFaqQuestions = listOf(
    "如何申请港澳通行证？",
    "老年人通关有哪些便利措施？",
    "老年人过关交通有哪些优惠？"
)

fun faqItemsForCategory(category: String): List<FaqItem> {
    if (category != "全部") {
        return faqItems.filter { it.category == category }
    }

    val defaultItems = defaultHomeFaqQuestions.mapNotNull { defaultQuestion ->
        faqItems.firstOrNull { it.question == defaultQuestion }
    }
    val defaultQuestionSet = defaultHomeFaqQuestions.toSet()
    return defaultItems + faqItems.filter { it.question !in defaultQuestionSet }
}
