package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.model.ChatPolicyRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

data class ChatUiState(
    val input: String = "",
    val answer: String = "",
    val conversationId: String = "",
    val sourceDocuments: List<String> = emptyList(),
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
                val cleanedAnswer = cleanMarkdownAnswer(response.answer)

                if (cleanedAnswer.isBlank()) {
                    _uiState.value = current.copy(
                        input = question,
                        answer = "",
                        sourceDocuments = emptyList(),
                        isLoading = false,
                        errorMessage = "资料中暂时没有明确答案，建议咨询人工窗口。",
                    )
                    return@launch
                }

                _uiState.value = current.copy(
                    input = question,
                    answer = cleanedAnswer,
                    conversationId = response.conversation_id.orEmpty(),
                    sourceDocuments = response.sources
                        .mapNotNull { it.document_name?.takeIf { name -> name.isNotBlank() } }
                        .distinct(),
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

    private fun classifyError(exc: Exception): String {
        return when (exc) {
            is SocketTimeoutException -> "查询时间有点久，请稍后再试一次。"
            is HttpException -> when (exc.code()) {
                504 -> "查询时间有点久，请稍后再试一次。"
                502 -> "资料服务暂时繁忙，请稍后再试。"
                in 500..599 -> "后端服务暂时不可用，请稍后再试。"
                else -> "请求没有成功，请稍后再试。"
            }
            is IOException -> "网络好像不太稳定，请检查手机和电脑是否在同一网络。"
            else -> "系统暂时没有响应，请稍后再试，或换个问题重新发送。"
        }
    }

    private fun cleanMarkdownAnswer(raw: String): String {
        return raw
            .replace("\r\n", "\n")
            .replace(Regex("""(?m)^\s{0,3}#{1,6}\s*"""), "")
            .replace(Regex("""\*\*(.*?)\*\*"""), "$1")
            .replace(Regex("""__(.*?)__"""), "$1")
            .replace(Regex("""`([^`]*)`"""), "$1")
            .replace(Regex("""(?m)^\s*[-*]\s+"""), "• ")
            .replace(Regex("""\n{3,}"""), "\n\n")
            .trim()
    }
}
