package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.model.ChatPolicyRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ChatUiState(
    val input: String = "",
    val answer: String = "",
    val conversationId: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value, errorMessage = null)
    }

    fun sendQuestion() {
        val question = _uiState.value.input.trim()
        if (question.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先输入您想咨询的问题")
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)

        viewModelScope.launch {
            try {
                val current = _uiState.value
                val response = ApiClient.assistantApi.chatPolicy(
                    ChatPolicyRequest(
                        message = question,
                        conversation_id = current.conversationId,
                        user_id = "demo-user-001",
                    )
                )

                _uiState.value = current.copy(
                    input = question,
                    answer = response.answer,
                    conversationId = response.conversation_id.orEmpty(),
                    isLoading = false,
                    errorMessage = null,
                )
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "系统暂时没有响应，请稍后再试，或换个问题重新发送。",
                )
            }
        }
    }
}
