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
    val shortAnswer: String = "",
    val shortAnswerEn: String = "",
    val answerSections: List<FaqAnswerSection> = emptyList(),
    val tips: List<String> = emptyList(),
    val tipsEn: List<String> = emptyList(),
    val sourceNote: String = "请以当地出入境窗口或官方平台最新要求为准。",
    val sourceNoteEn: String = "Please follow the latest requirements from your local exit-entry service window or official platform.",
    val updatedAt: String = "2026-06",
    val followUpQuestion: String = query,
    val followUpQuestionEn: String = "",
)

data class FaqAnswerSection(
    val title: String,
    val content: String,
    val titleEn: String = "",
    val contentEn: String = "",
)

private fun useEnglish(language: String): Boolean = language.equals("en", ignoreCase = true)

fun FaqItem.shortAnswerFor(language: String): String {
    return if (useEnglish(language)) shortAnswerEn.ifBlank { shortAnswer } else shortAnswer
}

fun FaqItem.tipsFor(language: String): List<String> {
    return if (useEnglish(language)) tipsEn.ifEmpty { tips } else tips
}

fun FaqItem.sourceNoteFor(language: String): String {
    return if (useEnglish(language)) sourceNoteEn.ifBlank { sourceNote } else sourceNote
}

fun FaqItem.followUpQuestionFor(language: String): String {
    return if (useEnglish(language)) followUpQuestionEn.ifBlank { followUpQuestion.ifBlank { query } } else followUpQuestion.ifBlank { query }
}

fun FaqAnswerSection.titleFor(language: String): String {
    return if (useEnglish(language)) titleEn.ifBlank { title } else title
}

fun FaqAnswerSection.contentFor(language: String): String {
    return if (useEnglish(language)) contentEn.ifBlank { content } else content
}

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
        query = "如何申请港澳通行证？",
        shortAnswer = "首次申请港澳通行证通常需要本人到出入境窗口办理，完成身份核验、拍照或信息采集后按要求提交申请。",
        shortAnswerEn = "For a first Hong Kong and Macao Travel Permit, you usually need to visit an exit-entry service window in person for identity checks, photo or information collection, and application submission.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "常见材料",
                content = "一般需要携带本人有效居民身份证，并按现场要求提交照片、申请表或其他补充材料。老年人可由家属陪同前往。",
                titleEn = "Common materials",
                contentEn = "Usually bring your valid resident ID card. Photos, an application form, or extra materials may be requested on site. Older adults may go with a family member."
            ),
            FaqAnswerSection(
                title = "办理方式",
                content = "首次办理通常以窗口办理为主。部分城市支持提前线上预约，也可以到现场咨询工作人员是否有适老服务或绿色通道。",
                titleEn = "How to apply",
                contentEn = "First-time applications are usually handled at a service window. Some cities support online appointments. You can also ask staff whether elder-friendly assistance or a priority channel is available."
            ),
            FaqAnswerSection(
                title = "下一步建议",
                content = "出发前先确认当地出入境大厅地址、办公时间和是否需要预约，避免老人反复跑动。",
                titleEn = "Next step",
                contentEn = "Before leaving, confirm the local exit-entry hall address, office hours, and whether an appointment is needed so older adults do not need to make repeated trips."
            )
        ),
        tips = listOf(
            "不同城市办理要求可能略有差异。",
            "建议同时携带身份证原件和手机，方便现场核验或接收通知。",
            "如老人行动不便，可先电话咨询是否支持陪办或便民服务。"
        ),
        tipsEn = listOf(
            "Requirements may differ slightly by city.",
            "Bring the original ID card and a mobile phone for verification or notices.",
            "If the older adult has mobility difficulty, call ahead to ask about companion or convenience services."
        ),
        followUpQuestion = "请根据我的情况一步步说明首次申请港澳通行证需要准备什么、是否必须预约、到窗口后怎么办理。",
        followUpQuestionEn = "Please walk me through what I need for a first Hong Kong and Macao Travel Permit, whether I must make an appointment, and what to do at the service window."
    ),
    FaqItem(
        id = "expired_hk_macau_pass",
        categoryId = FaqCategoryDocument,
        titleResId = R.string.faq_q_expired_hk_macau_pass,
        query = "港澳通行证过期了怎么办？",
        shortAnswer = "港澳通行证已过期时，一般需要办理换发或重新申请，是否能线上预约、是否必须到窗口，以当地出入境规定为准。",
        shortAnswerEn = "If the permit itself has expired, you usually need renewal or re-application. Online appointment availability and whether an in-person visit is required depend on local exit-entry rules.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "先确认过期情况",
                content = "请区分是通行证证件本身过期，还是签注过期或次数用完。证件本身过期通常需要换发；签注问题可能只需办理签注。",
                titleEn = "Confirm what expired",
                contentEn = "First check whether the permit card itself expired, or only the endorsement expired or ran out. An expired permit usually needs replacement; an endorsement issue may only need endorsement renewal."
            ),
            FaqAnswerSection(
                title = "常见材料",
                content = "通常建议携带本人有效身份证、原港澳通行证，以及当地窗口要求的照片、申请表或其他证明材料。",
                titleEn = "Common materials",
                contentEn = "Bring your valid ID card, the original permit, and any photos, forms, or supporting documents required by the local service window."
            ),
            FaqAnswerSection(
                title = "老人办理建议",
                content = "如果老人不会线上预约，可由家属协助预约，或先到窗口咨询现场取号、适老服务和陪办要求。",
                titleEn = "For older adults",
                contentEn = "If the older adult cannot make an online appointment, a family member can help, or you can ask the service window about on-site ticketing, elder-friendly services, and companion rules."
            )
        ),
        tips = listOf(
            "出行前务必同时确认通行证有效期和签注有效期。",
            "不要等到临近出发当天才办理，换发可能需要制证时间。",
            "证件遗失、损坏、信息变更等情况可能需要额外材料。"
        ),
        tipsEn = listOf(
            "Check both the permit validity and endorsement validity before travel.",
            "Do not wait until the travel day; replacement may require production time.",
            "Lost, damaged, or changed-information cases may require extra materials."
        ),
        followUpQuestion = "我的港澳通行证快过期或已经过期，请帮我判断是换证、续签还是重新办理，并说明要准备哪些材料。",
        followUpQuestionEn = "My Hong Kong and Macao permit is expiring or has expired. Please help me decide whether I need replacement, endorsement renewal, or re-application, and what materials to prepare."
    ),
    FaqItem(
        id = "elder_pass_materials",
        categoryId = FaqCategoryDocument,
        titleResId = R.string.faq_q_elder_pass_materials,
        query = "老年人办理港澳通行证需要什么材料？",
        shortAnswer = "老年人办理港澳通行证一般需要本人有效身份证件，并根据首次办理、换证、补办或签注类型准备相应材料。",
        shortAnswerEn = "Older adults usually need a valid personal ID document, plus materials that match the case: first application, replacement, reissue, or endorsement renewal.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "常见材料",
                content = "通常包括本人有效居民身份证；如已有港澳通行证，建议一并携带原证件。部分业务还可能需要照片、申请表或相关证明。",
                titleEn = "Common materials",
                contentEn = "Usually bring the valid resident ID card. If the person already has a Hong Kong and Macao permit, bring the original permit too. Some cases may also need photos, forms, or supporting documents."
            ),
            FaqAnswerSection(
                title = "办理场景",
                content = "首次办理、证件过期换发、证件遗失补办、签注过期续签的材料要求不同，窗口会按具体场景核验。",
                titleEn = "Application scenarios",
                contentEn = "First application, expired permit replacement, lost permit reissue, and endorsement renewal can require different materials. The service window will check based on the exact case."
            ),
            FaqAnswerSection(
                title = "陪同办理",
                content = "老人不熟悉流程时，家属可陪同前往，协助取号、填写信息、确认办理类型和保存回执。",
                titleEn = "Companion support",
                contentEn = "If the older adult is unfamiliar with the process, a family member can accompany them to take a queue number, fill in information, confirm the application type, and keep the receipt."
            )
        ),
        tips = listOf(
            "不同地区对照片、预约和表格的要求可能不同。",
            "建议把身份证、原通行证、手机和必要现金或银行卡一起带上。",
            "行动不便老人可提前咨询当地窗口是否有绿色通道。"
        ),
        tipsEn = listOf(
            "Photo, appointment, and form requirements may differ by region.",
            "Bring ID, the original permit if any, a mobile phone, and necessary cash or bank card.",
            "For mobility difficulties, ask the local window in advance whether a priority channel is available."
        ),
        followUpQuestion = "请根据老人的具体情况，帮我判断办理港澳通行证需要准备哪些材料，以及哪些情况必须到窗口办理。",
        followUpQuestionEn = "Please help me decide what materials an older adult needs for a Hong Kong and Macao permit, and which cases must be handled at a service window."
    ),
    FaqItem(
        id = "elder_border_convenience",
        categoryId = FaqCategoryBorder,
        titleResId = R.string.faq_q_elder_border_convenience,
        query = "老年人通关有哪些便利措施？",
        shortAnswer = "不少口岸会为老年人、行动不便人士提供现场引导、人工通道咨询或优先协助，具体安排以口岸现场为准。",
        shortAnswerEn = "Many ports provide on-site guidance, manual-channel consultation, or priority assistance for older adults and people with mobility difficulties. The exact arrangement depends on the port.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "现场可咨询的帮助",
                content = "到达口岸后，可向现场工作人员说明老人年龄、行动能力、是否需要陪同或人工协助，工作人员会按现场秩序引导。",
                titleEn = "Help to ask for on site",
                contentEn = "After arriving at the port, tell staff the older adult's age, mobility condition, and whether companion or manual assistance is needed. Staff will guide you based on site rules."
            ),
            FaqAnswerSection(
                title = "证件准备",
                content = "通关前请准备好身份证件、港澳通行证和有效签注，避免在队伍中临时翻找。",
                titleEn = "Prepare documents",
                contentEn = "Before inspection, keep the ID document, Hong Kong and Macao permit, and valid endorsement ready so you do not need to search for them in the queue."
            ),
            FaqAnswerSection(
                title = "出行安排",
                content = "尽量避开节假日、早晚高峰和大型活动时段，给老人预留休息、如厕和换乘时间。",
                titleEn = "Travel planning",
                contentEn = "Try to avoid holidays, rush hours, and large event periods. Leave enough time for rest, restroom use, and transfers."
            )
        ),
        tips = listOf(
            "便利措施会受口岸客流和现场管理影响。",
            "老人如需轮椅、陪同或人工查验，建议提前到现场咨询。",
            "请不要携带禁止或限制进出境物品。"
        ),
        tipsEn = listOf(
            "Convenience measures depend on port traffic and on-site management.",
            "If wheelchair, companion, or manual inspection help is needed, ask staff at the port.",
            "Do not carry prohibited or restricted items across the border."
        ),
        followUpQuestion = "老人准备过关，请帮我说明可以提前做哪些准备、到口岸后怎样向工作人员寻求帮助。",
        followUpQuestionEn = "An older adult is preparing to cross the border. Please explain what to prepare in advance and how to ask port staff for help."
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
        query = "过关时需要注意什么？",
        shortAnswer = "过关前请确认通行证和签注有效，提前准备证件，遵守口岸现场指引，并注意随身物品是否符合进出境规定。",
        shortAnswerEn = "Before crossing, confirm that the permit and endorsement are valid, keep documents ready, follow port instructions, and check whether carried items comply with entry-exit rules.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "证件核对",
                content = "出发前检查港澳通行证、签注目的地、签注次数和有效期。证件临近过期或签注不匹配时，不建议贸然出行。",
                titleEn = "Check documents",
                contentEn = "Before departure, check the permit, endorsement destination, number of entries, and validity. If the document is near expiry or the endorsement does not match, do not travel without confirming first."
            ),
            FaqAnswerSection(
                title = "现场流程",
                content = "到口岸后按指引排队查验，提前取出证件。老人不清楚走哪条通道时，可直接询问工作人员。",
                titleEn = "On-site process",
                contentEn = "At the port, queue according to signs and have documents ready. If an older adult is unsure which channel to use, ask staff directly."
            ),
            FaqAnswerSection(
                title = "物品提醒",
                content = "药品、食品、现金、电子产品等物品可能有携带限制，建议出发前查看口岸或海关提示。",
                titleEn = "Item reminders",
                contentEn = "Medicines, food, cash, electronics, and other items may have carrying limits. Check port or customs reminders before departure."
            )
        ),
        tips = listOf(
            "节假日口岸客流较大，建议预留更充足时间。",
            "请保管好证件、手机和随身药品。",
            "如老人身体不适，优先寻求现场工作人员帮助。"
        ),
        tipsEn = listOf(
            "Ports can be crowded during holidays, so leave extra time.",
            "Keep documents, phone, and necessary medicines safe.",
            "If an older adult feels unwell, ask port staff for help first."
        ),
        followUpQuestion = "我准备带老人过关，请帮我检查出发前需要确认哪些证件、物品和现场注意事项。",
        followUpQuestionEn = "I am taking an older adult across the border. Please help me check the documents, items, and on-site reminders before departure."
    ),
    FaqItem(
        id = "elder_transport_discount",
        categoryId = FaqCategoryTransport,
        titleResId = R.string.faq_q_elder_transport_discount,
        query = "老年人过关交通有哪些优惠？",
        shortAnswer = "老年人交通优惠通常与城市、交通工具、年龄和证件类型有关，跨境出行前建议分别确认内地段、香港或澳门段的规则。",
        shortAnswerEn = "Transport discounts for older adults depend on the city, transport type, age, and accepted documents. For cross-border trips, check mainland, Hong Kong, or Macao rules separately.",
        answerSections = listOf(
            FaqAnswerSection(
                title = "先区分路段",
                content = "内地公交地铁、口岸接驳、香港或澳门本地交通可能执行不同优惠政策，不能默认通用。",
                titleEn = "Separate the route segments",
                contentEn = "Mainland buses or metro, port shuttles, and Hong Kong or Macao local transport may use different discount rules. Do not assume one rule applies everywhere."
            ),
            FaqAnswerSection(
                title = "常见凭证",
                content = "可能需要身份证、老人优待证、当地交通卡或目的地认可的长者卡。具体以交通运营方要求为准。",
                titleEn = "Common proof",
                contentEn = "You may need an ID card, senior card, local transport card, or an older-person card accepted by the destination. Follow the transport operator's rules."
            ),
            FaqAnswerSection(
                title = "出行建议",
                content = "陪同老人出行时，建议提前规划换乘路线，优先选择步行少、电梯方便、换乘次数少的方案。",
                titleEn = "Travel suggestion",
                contentEn = "When accompanying an older adult, plan transfers in advance and prefer routes with less walking, easier elevator access, and fewer transfers."
            )
        ),
        tips = listOf(
            "跨境巴士、高铁、地铁和公交优惠规则不同。",
            "部分优惠需要提前办卡或实名登记。",
            "不要只看票价，也要考虑老人步行距离和候车时间。"
        ),
        tipsEn = listOf(
            "Cross-border buses, high-speed rail, metro, and buses have different discount rules.",
            "Some discounts require a card or real-name registration in advance.",
            "Do not only compare ticket price; also consider walking distance and waiting time."
        ),
        followUpQuestion = "请根据我的出发地、目的地和老人年龄，帮我规划过关交通方式并说明可能有哪些优惠。",
        followUpQuestionEn = "Please plan a border-crossing transport option based on my departure place, destination, and the older adult's age, and explain possible discounts."
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
