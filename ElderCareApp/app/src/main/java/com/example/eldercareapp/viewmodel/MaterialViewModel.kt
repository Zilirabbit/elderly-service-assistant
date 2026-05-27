package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.model.MaterialItem
import com.example.eldercareapp.model.SaveMaterialRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

data class SavedMaterialChecklist(
    val itemCode: String,
    val title: String,
    val checkedCount: Int,
    val totalCount: Int,
    val checkedRequirementIds: Set<String> = emptySet(),
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
    private val _uiState = MutableStateFlow(MaterialUiState())
    val uiState: StateFlow<MaterialUiState> = _uiState.asStateFlow()

    init {
        loadItems()
    }

    fun loadItems() {
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val items = ApiClient.assistantApi.materialItems()
                _uiState.value = _uiState.value.copy(
                    items = items,
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (exc: CancellationException) {
                throw exc
            } catch (exc: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = classifyError(exc),
                )
            }
        }
    }

    fun selectItem(itemCode: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            errorMessage = null,
            saveMessage = null,
        )

        viewModelScope.launch {
            try {
                val checklist = ApiClient.assistantApi.materialChecklist(itemCode)
                val saved = _uiState.value.savedChecklists.firstOrNull { it.itemCode == itemCode }
                _uiState.value = _uiState.value.copy(
                    selectedChecklist = checklist,
                    checkedRequirementIds = saved?.checkedRequirementIds ?: emptySet(),
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (exc: CancellationException) {
                throw exc
            } catch (exc: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = classifyError(exc),
                )
            }
        }
    }

    fun closeChecklist() {
        _uiState.value = _uiState.value.copy(
            selectedChecklist = null,
            checkedRequirementIds = emptySet(),
            saveMessage = null,
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
        val checkedIds = _uiState.value.checkedRequirementIds.toList()

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null, saveMessage = null)

        viewModelScope.launch {
            try {
                val response = ApiClient.assistantApi.saveMaterialChecklist(
                    SaveMaterialRequest(
                        item_code = checklist.code,
                        checked_requirement_ids = checkedIds,
                    )
                )
                val saved = SavedMaterialChecklist(
                    itemCode = checklist.code,
                    title = checklist.title,
                    checkedCount = response.saved_count,
                    totalCount = checklist.requirements.size,
                    checkedRequirementIds = response.checked_requirement_ids.toSet(),
                )
                _uiState.value = _uiState.value.copy(
                    savedChecklists = (_uiState.value.savedChecklists
                        .filterNot { it.itemCode == checklist.code } + saved),
                    isLoading = false,
                    errorMessage = null,
                    saveMessage = response.message,
                )
            } catch (exc: CancellationException) {
                throw exc
            } catch (exc: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = classifyError(exc),
                )
            }
        }
    }

    private fun classifyError(exc: Exception): String {
        return when (exc) {
            is SocketTimeoutException -> "材料清单加载时间有点久，请稍后再试。"
            is HttpException -> when (exc.code()) {
                404 -> "暂时没有找到这个事项的材料清单。"
                in 500..599 -> "材料服务暂时不可用，请稍后再试。"
                else -> "材料清单请求没有成功，请稍后再试。"
            }
            is IOException -> "网络好像不太稳定，请确认后端服务已启动。"
            else -> "材料清单暂时加载失败，请稍后再试。"
        }
    }
}
