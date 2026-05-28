package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.model.ChatPolicyRequest
import com.example.eldercareapp.model.TtsSynthesizeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException
import java.net.SocketTimeoutException

data class ChatUiState(
    val input: String = "",
    val answer: String = "",
    val conversationId: String = "",
    val sourceDocuments: List<String> = emptyList(),
    val voiceDraft: String? = null,
    val ttsText: String = "",
    val ttsAudioUrl: String? = null,
    val lastVoiceSampleName: String = "",
    val lastVoiceSampleSizeBytes: Long = 0L,
    val isLoading: Boolean = false,
    val isTranscribing: Boolean = false,
    val errorMessage: String? = null,
)

class ChatViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value, errorMessage = null)
    }

    fun sendQuestion(inputType: String = "text", ttsLanguage: String = "zh-CN") {
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
                        input_type = inputType,
                        tts_language = ttsLanguage,
                    )
                )
                val displayText = response.display_text?.takeIf { it.isNotBlank() } ?: response.answer
                val cleanedAnswer = cleanMarkdownAnswer(displayText)

                if (cleanedAnswer.isBlank()) {
                    _uiState.value = current.copy(
                        input = question,
                        answer = "",
                        sourceDocuments = emptyList(),
                        voiceDraft = null,
                        ttsText = "",
                        ttsAudioUrl = null,
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
                    voiceDraft = null,
                    ttsText = cleanMarkdownAnswer(response.tts?.text?.takeIf { it.isNotBlank() } ?: cleanedAnswer),
                    ttsAudioUrl = response.tts?.audio_url,
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

    fun transcribeVoice(file: File, language: String, sampleName: String = "") {
        if (_uiState.value.isTranscribing) return

        _uiState.value = _uiState.value.copy(
            isTranscribing = true,
            errorMessage = null,
            voiceDraft = null,
            lastVoiceSampleName = sampleName.ifBlank { file.name },
            lastVoiceSampleSizeBytes = file.length(),
        )

        viewModelScope.launch {
            try {
                val audioBody = file.asRequestBody(mediaTypeForAudioFile(file).toMediaTypeOrNull())
                val audioPart = MultipartBody.Part.createFormData("file", file.name, audioBody)
                val languageBody = language.toRequestBody("text/plain".toMediaTypeOrNull())
                val userIdBody = "demo-user-001".toRequestBody("text/plain".toMediaTypeOrNull())
                val response = ApiClient.assistantApi.transcribeVoice(
                    file = audioPart,
                    language = languageBody,
                    userId = userIdBody,
                )
                val recognizedText = response.original_text.trim()
                if (!isMeaningfulVoiceText(recognizedText)) {
                    _uiState.value = _uiState.value.copy(
                        isTranscribing = false,
                        errorMessage = "没有听清，请重新说一遍或手动输入。",
                    )
                    return@launch
                }

                _uiState.value = _uiState.value.copy(
                    voiceDraft = recognizedText,
                    isTranscribing = false,
                    errorMessage = null,
                )
            } catch (exc: CancellationException) {
                throw exc
            } catch (exc: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTranscribing = false,
                    errorMessage = classifyVoiceError(exc),
                )
            } finally {
                file.delete()
            }
        }
    }

    fun confirmVoiceDraft(ttsLanguage: String = "zh-CN") {
        val draft = _uiState.value.voiceDraft?.trim().orEmpty()
        if (draft.isBlank()) return

        _uiState.value = _uiState.value.copy(
            input = draft,
            voiceDraft = null,
            errorMessage = null,
        )
        sendQuestion(inputType = "voice", ttsLanguage = ttsLanguage)
    }

    fun clearVoiceDraft() {
        _uiState.value = _uiState.value.copy(voiceDraft = null, errorMessage = null)
    }

    fun editVoiceDraft() {
        val draft = _uiState.value.voiceDraft.orEmpty()
        _uiState.value = _uiState.value.copy(
            input = draft,
            voiceDraft = null,
            errorMessage = null,
        )
    }

    suspend fun synthesizeSpeech(text: String, language: String = "zh-CN"): String? {
        val response = ApiClient.assistantApi.synthesizeTts(
            TtsSynthesizeRequest(
                text = text,
                language = language,
            )
        )
        return response.audio_url
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

    private fun classifyVoiceError(exc: Exception): String {
        return when (exc) {
            is SocketTimeoutException -> "语音识别时间有点久，请稍后再试一次。"
            is HttpException -> when (exc.code()) {
                400, 422 -> "没有听清，请重新说一遍，或改用文字输入。"
                413 -> "录音文件太大，请缩短录音后重试。"
                504 -> "语音识别时间有点久，请稍后再试一次。"
                502 -> "语音识别服务暂时繁忙，请稍后再试。"
                in 500..599 -> "后端服务暂时不可用，请稍后再试。"
                else -> "语音识别没有成功，请重新说一遍。"
            }
            is IOException -> "网络好像不太稳定，请检查手机和电脑是否在同一网络。"
            else -> "语音识别暂时没有响应，请重新说一遍或手动输入。"
        }
    }

    private fun mediaTypeForAudioFile(file: File): String {
        return when (file.extension.lowercase()) {
            "wav" -> "audio/wav"
            "mp3" -> "audio/mpeg"
            "aac" -> "audio/aac"
            "webm" -> "audio/webm"
            "mp4" -> "audio/mp4"
            else -> "audio/mp4"
        }
    }

    private fun isMeaningfulVoiceText(value: String): Boolean {
        val normalized = value
            .lowercase()
            .replace(Regex("""[\s,，.。!！?？、~～…]+"""), "")
        if (normalized.isBlank()) return false
        if (normalized.length <= 1) return false

        val fillerWords = setOf(
            "嗯",
            "嗯嗯",
            "嗯哼",
            "呃",
            "呃呃",
            "啊",
            "啊啊",
            "哦",
            "喔",
            "额",
            "唔",
            "唔唔",
            "hm",
            "hmm",
            "uh",
            "um",
            "er",
        )
        if (normalized in fillerWords) return false

        val fillerChars = setOf('嗯', '呃', '啊', '哦', '喔', '额', '唔')
        return normalized.any { it !in fillerChars }
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
