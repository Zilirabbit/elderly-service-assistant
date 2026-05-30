package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.model.MaterialItem
import com.example.eldercareapp.model.MaterialRequirement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SavedMaterialChecklist(
    val itemCode: String,
    val title: String,
    val checkedCount: Int,
    val totalCount: Int,
    val checkedRequirementIds: Set<String> = emptySet(),
    val updatedAtMillis: Long = System.currentTimeMillis(),
)

data class MaterialUiState(
    val items: List<MaterialItem> = emptyList(),
    val selectedChecklist: MaterialChecklist? = null,
    val checkedRequirementIds: Set<String> = emptySet(),
    val savedChecklists: List<SavedMaterialChecklist> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveMessage: String? = null,
)

class MaterialViewModel : ViewModel() {
    private val mockChecklists = listOf(
        MaterialChecklist(
            code = "hk_macau_pass_apply",
            title = "办理港澳通行证",
            tips = listOf(
                "首次办理一般需要本人到出入境窗口办理。办理前建议先确认当地出入境大厅预约要求。",
                "照片回执和申请表要求可能因城市略有差异，请以现场和官方平台为准。"
            ),
            requirements = listOf(
                MaterialRequirement(
                    id = "resident_id_card",
                    name = "居民身份证原件",
                    description = "用于核验本人身份，建议同时准备一份复印件。",
                    required = true,
                    note = "未满 16 周岁或特殊情况请按窗口要求补充监护材料。"
                ),
                MaterialRequirement(
                    id = "entry_exit_photo_receipt",
                    name = "出入境证件照片回执",
                    description = "到有资质照相点拍摄后取得照片回执。",
                    required = true
                ),
                MaterialRequirement(
                    id = "application_form",
                    name = "中国公民出入境证件申请表",
                    description = "可现场填写，也可按当地平台要求提前填写。",
                    required = true
                ),
                MaterialRequirement(
                    id = "appointment_record",
                    name = "预约记录或预约短信",
                    description = "如果当地要求预约，请带上预约成功记录。",
                    required = false
                )
            )
        ),
        MaterialChecklist(
            code = "hk_macau_renewal",
            title = "港澳通行证续签",
            tips = listOf(
                "先确认港澳通行证仍在有效期内。部分地区可用智能签注设备办理，特殊情况需到窗口。",
                "如证件损坏、过期或信息变更，可能需要重新办理证件。"
            ),
            requirements = listOf(
                MaterialRequirement(
                    id = "valid_hk_macau_pass",
                    name = "有效港澳通行证",
                    description = "请确认通行证未过期、未损坏，个人信息清晰可识别。",
                    required = true
                ),
                MaterialRequirement(
                    id = "resident_id_card",
                    name = "居民身份证原件",
                    description = "用于现场身份核验或智能设备核验。",
                    required = true
                ),
                MaterialRequirement(
                    id = "previous_endorsement",
                    name = "原有签注信息",
                    description = "用于判断签注是否过期、是否仍有有效次数。",
                    required = false
                ),
                MaterialRequirement(
                    id = "appointment_record",
                    name = "预约记录或预约短信",
                    description = "如果当地要求预约，请带上预约成功记录。",
                    required = false
                )
            )
        ),
        MaterialChecklist(
            code = "border_crossing_prepare",
            title = "过关材料准备",
            tips = listOf(
                "出发前先检查证件和签注有效性。常用药品建议保留原包装，并避免携带不明药品。",
                "老人出行建议提前保存家属电话和紧急联系人。"
            ),
            requirements = listOf(
                MaterialRequirement(
                    id = "hk_macau_pass",
                    name = "港澳通行证",
                    description = "过关时需要出示，建议放在容易拿取的位置。",
                    required = true,
                    linkedChecklistId = "hk_macau_pass_apply",
                    linkedActionLabel = "还没有？查看办理材料"
                ),
                MaterialRequirement(
                    id = "valid_endorsement",
                    name = "有效签注",
                    description = "确认目的地、次数和有效期是否满足本次出行。",
                    required = true,
                    linkedChecklistId = "hk_macau_renewal",
                    linkedActionLabel = "不确定？查看续签材料"
                ),
                MaterialRequirement(
                    id = "resident_id_card",
                    name = "居民身份证",
                    description = "部分交通、住宿或现场核验可能需要。",
                    required = false
                ),
                MaterialRequirement(
                    id = "phone_emergency_contact",
                    name = "手机与紧急联系人信息",
                    description = "建议提前保存家属电话，方便需要时联系。",
                    required = false
                ),
                MaterialRequirement(
                    id = "necessary_medicine",
                    name = "必要药品",
                    description = "如需携带常用药，建议保留原包装并按规定携带。",
                    required = false
                )
            )
        ),
        MaterialChecklist(
            code = "service_uncertain_valid_pass",
            title = "办理前核对清单",
            tips = listOf(
                "您已确认有有效港澳通行证，但还不确定这次具体要办签注、换证、补发还是其他业务。",
                "建议先带身份证和港澳通行证到窗口，请工作人员核对证件和签注状态。"
            ),
            requirements = listOf(
                MaterialRequirement(
                    id = "resident_id_card",
                    name = "居民身份证原件",
                    description = "用于窗口或设备核验本人身份。",
                    required = true
                ),
                MaterialRequirement(
                    id = "valid_hk_macau_pass",
                    name = "有效港澳通行证",
                    description = "请带上现有证件，方便工作人员核对证件状态。",
                    required = true
                ),
                MaterialRequirement(
                    id = "previous_endorsement",
                    name = "原有签注信息",
                    description = "用于判断签注是否过期、是否仍有有效次数。",
                    required = false
                ),
                MaterialRequirement(
                    id = "appointment_record",
                    name = "预约记录或预约短信",
                    description = "如果当地要求预约，请带上预约成功记录。",
                    required = false
                ),
                MaterialRequirement(
                    id = "extra_documents",
                    name = "其他补充材料",
                    description = "如有证件损坏、信息变更、未成年人办理等情况，请按窗口要求补充材料。",
                    required = false
                )
            )
        )
    )

    private val _uiState = MutableStateFlow(MaterialUiState())
    val uiState: StateFlow<MaterialUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        _uiState.value = _uiState.value.copy(
            items = mockChecklists.map { checklist ->
                MaterialItem(
                    code = checklist.code,
                    title = checklist.title,
                    subtitle = checklist.tips.firstOrNull().orEmpty(),
                    category = "本地 Mock 清单"
                )
            },
            isLoading = false,
            errorMessage = null,
        )
    }

    fun selectItem(itemCode: String) {
        val checklist = mockChecklists.firstOrNull { it.code == itemCode }
        if (checklist == null) {
            _uiState.value = _uiState.value.copy(
                selectedChecklist = null,
                checkedRequirementIds = emptySet(),
                isLoading = false,
                errorMessage = "暂时没有找到这个事项的材料清单。",
                saveMessage = null,
            )
            return
        }

        val saved = _uiState.value.savedChecklists.firstOrNull { it.itemCode == itemCode }
        _uiState.value = _uiState.value.copy(
            selectedChecklist = checklist,
            checkedRequirementIds = saved?.checkedRequirementIds ?: emptySet(),
            isLoading = false,
            errorMessage = null,
            saveMessage = null,
        )
    }

    fun closeChecklist() {
        _uiState.value = _uiState.value.copy(
            selectedChecklist = null,
            checkedRequirementIds = emptySet(),
            saveMessage = null,
            errorMessage = null,
        )
    }

    fun toggleRequirement(requirementId: String) {
        val current = _uiState.value.checkedRequirementIds
        _uiState.value = _uiState.value.copy(
            checkedRequirementIds = if (requirementId in current) {
                current - requirementId
            } else {
                current + requirementId
            },
            saveMessage = null,
        )
    }

    fun saveSelectedChecklist() {
        val checklist = _uiState.value.selectedChecklist ?: return
        val checkedIds = _uiState.value.checkedRequirementIds
        val saved = SavedMaterialChecklist(
            itemCode = checklist.code,
            title = checklist.title,
            checkedCount = checkedIds.size,
            totalCount = checklist.requirements.size,
            checkedRequirementIds = checkedIds,
        )

        _uiState.value = _uiState.value.copy(
            savedChecklists = (_uiState.value.savedChecklists
                .filterNot { it.itemCode == checklist.code } + saved)
                .sortedByDescending { it.updatedAtMillis },
            isLoading = false,
            errorMessage = null,
            saveMessage = "已保存到我的材料清单",
        )
    }
}
