package com.example.eldercareapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.model.ChatPolicyRequest
import com.example.eldercareapp.model.ChatPolicyResponse
import com.example.eldercareapp.model.QaAnswerUiModel
import com.example.eldercareapp.model.QaScenarioOption
import com.example.eldercareapp.model.TtsSynthesizeRequest
import com.example.eldercareapp.model.defaultQaScenarioOptions
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

enum class ChatMessageRole {
    User,
    Assistant
}

data class ChatMessageUi(
    val id: Long,
    val role: ChatMessageRole,
    val text: String = "",
    val answerUiModel: QaAnswerUiModel? = null,
    val ttsText: String = "",
    val ttsAudioUrl: String? = null,
)

data class ChatUiState(
    val input: String = "",
    val answer: String = "",
    val answerUiModel: QaAnswerUiModel? = null,
    val messages: List<ChatMessageUi> = emptyList(),
    val conversationId: String = "",
    val lastQuestion: String = "",
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
    private var nextMessageId = 1L

    fun updateInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value, errorMessage = null)
    }

    fun submitPrefilledQuestion(
        question: String,
        inputType: String = "guidance",
        displayLanguage: String = "zh-CN",
        speechLanguage: String = "zh-CN",
    ) {
        val cleanedQuestion = question.trim()
        if (cleanedQuestion.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先输入您想咨询的问题")
            return
        }

        sendQuestion(
            questionOverride = cleanedQuestion,
            inputType = inputType,
            displayLanguage = displayLanguage,
            speechLanguage = speechLanguage
        )
    }

    fun sendQuestion(
        inputType: String = "text",
        displayLanguage: String = "zh-CN",
        speechLanguage: String = "zh-CN",
        questionOverride: String? = null
    ) {
        val question = questionOverride?.trim() ?: _uiState.value.input.trim()
        if (question.isEmpty()) {
            _uiState.value = _uiState.value.copy(errorMessage = "请先输入您想咨询的问题")
            return
        }

        val userMessage = ChatMessageUi(
            id = nextMessageId(),
            role = ChatMessageRole.User,
            text = question
        )
        val messagesWithUser = _uiState.value.messages + userMessage

        _uiState.value = _uiState.value.copy(
            input = "",
            answer = "",
            answerUiModel = null,
            messages = messagesWithUser,
            lastQuestion = question,
            sourceDocuments = emptyList(),
            voiceDraft = null,
            lastVoiceSampleName = "",
            lastVoiceSampleSizeBytes = 0L,
            ttsText = "",
            ttsAudioUrl = null,
            isLoading = true,
            errorMessage = null
        )

        viewModelScope.launch {
            try {
                val current = _uiState.value
                val response = ApiClient.assistantApi.chatPolicy(
                    ChatPolicyRequest(
                        message = question,
                        conversation_id = current.conversationId,
                        user_id = "demo-user-001",
                        input_type = inputType,
                        language = displayLanguage,
                        tts_language = speechLanguage,
                    )
                )
                val displayText = response.display_text?.takeIf { it.isNotBlank() } ?: response.answer
                val cleanedAnswer = cleanMarkdownAnswer(displayText)
                val shouldUseStructuredAnswer = response.hasUsableStructuredAnswer()

                if (cleanedAnswer.isBlank()) {
                    _uiState.value = current.copy(
                        input = "",
                        answer = "",
                        answerUiModel = null,
                        lastQuestion = question,
                        sourceDocuments = emptyList(),
                        voiceDraft = null,
                        ttsText = "",
                        ttsAudioUrl = null,
                        isLoading = false,
                        errorMessage = "暂时没有在知识库中找到明确说明。建议咨询当地出入境窗口或官方渠道。",
                    )
                    return@launch
                }

                val sourceDocuments = response.sources
                    .mapNotNull { source ->
                        source.title?.takeIf { it.isNotBlank() }
                            ?: source.document_name?.takeIf { it.isNotBlank() }
                    }
                    .distinct()
                val answerUiModel = response.toQaAnswerUiModel(cleanedAnswer, sourceDocuments)
                    .takeIf { shouldUseStructuredAnswer }

                _uiState.value = current.copy(
                    input = "",
                    answer = cleanedAnswer,
                    answerUiModel = answerUiModel,
                    messages = current.messages + ChatMessageUi(
                        id = nextMessageId(),
                        role = ChatMessageRole.Assistant,
                        text = cleanedAnswer,
                        answerUiModel = answerUiModel,
                        ttsText = cleanMarkdownAnswer(response.tts?.text?.takeIf { it.isNotBlank() } ?: cleanedAnswer),
                        ttsAudioUrl = response.tts?.audio_url
                    ),
                    conversationId = response.conversation_id.orEmpty(),
                    lastQuestion = question,
                    sourceDocuments = sourceDocuments,
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

    fun confirmVoiceDraft(
        displayLanguage: String = "zh-CN",
        speechLanguage: String = "zh-CN"
    ) {
        val draft = _uiState.value.voiceDraft?.trim().orEmpty()
        if (draft.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "没有听清，请重新说一遍，或改用文字输入。")
            return
        }

        sendQuestion(
            inputType = "voice",
            displayLanguage = displayLanguage,
            speechLanguage = speechLanguage,
            questionOverride = draft
        )
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

    fun clearVoiceErrorForManualInput() {
        _uiState.value = _uiState.value.copy(errorMessage = null, voiceDraft = null)
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

    private fun ChatPolicyResponse.toQaAnswerUiModel(
        cleanedAnswer: String,
        sourceDocuments: List<String>
    ): QaAnswerUiModel {
        val structured = structured_answer
        val structuredConclusion = listOfNotNull(
            structured?.summary.cleanTextOrNull(),
            conclusion.cleanTextOrNull(),
            summary.cleanTextOrNull()
        ).firstOrNull()
        val stepsFromResponse = structured?.steps.orEmpty().cleanedItems().ifEmpty {
            steps.cleanedItems()
        }
        val requiredFromResponse = structured?.materials?.required.orEmpty().cleanedItems().ifEmpty {
            (materials?.required.orEmpty() + required_materials).cleanedItems()
        }
        val optionalFromResponse = structured?.materials?.optional.orEmpty().cleanedItems().ifEmpty {
            (
                materials?.possible_extra.orEmpty() +
                    materials?.optional.orEmpty() +
                    optional_materials
                ).cleanedItems()
        }
        val warningsFromResponse = structured?.warnings.orEmpty().cleanedItems().ifEmpty {
            warnings.cleanedItems()
        }.ifEmpty {
            listOf("具体要求以当地出入境管理部门或现场窗口为准。")
        }
        val sourceTitle = structured?.source_note.cleanTextOrNull()
            ?: source_title.cleanTextOrNull()
            ?: sourceDocuments.mapNotNull { friendlySourceTitle(it) }.firstOrNull()
            ?: "知识库资料"
        val scenarioOptions = structured?.scenario_options.orEmpty()
            .cleanedItems()
            .toQaScenarioOptions()
            .ifEmpty { defaultQaScenarioOptions() }
        val details = structured?.detail_text.cleanTextOrNull() ?: cleanedAnswer

        return QaAnswerUiModel(
            title = structured?.title.cleanTextOrNull() ?: "我帮您查到这些",
            subtitle = "根据办事资料整理，办理前请以当地窗口要求为准",
            conclusion = structuredConclusion ?: conciseConclusion(cleanedAnswer),
            scenarioOptions = scenarioOptions,
            steps = stepsFromResponse,
            requiredMaterials = requiredFromResponse,
            optionalMaterials = optionalFromResponse,
            warnings = warningsFromResponse,
            sourceTitle = sourceTitle,
            originalAnswer = details,
            confidence = structured?.confidence.cleanTextOrNull() ?: confidence.cleanTextOrNull(),
            needHumanReminder = structured?.need_human_reminder ?: true
        )
    }

    private fun ChatPolicyResponse.hasUsableStructuredAnswer(): Boolean {
        val structured = structured_answer ?: return false
        if (structured.confidence.equals("low", ignoreCase = true)) return false
        return listOf(
            structured.title,
            structured.summary,
            structured.detail_text
        ).any { it.isNotBlank() } ||
            structured.steps.any { it.isNotBlank() } ||
            structured.scenario_options.any { it.isNotBlank() } ||
            structured.materials?.required.orEmpty().any { it.isNotBlank() } ||
            structured.materials?.optional.orEmpty().any { it.isNotBlank() } ||
            structured.warnings.any { it.isNotBlank() }
    }

    private fun List<String>.cleanedItems(): List<String> {
        return mapNotNull { it.cleanTextOrNull() }.distinct()
    }

    private fun nextMessageId(): Long = nextMessageId++

    private fun List<String>.toQaScenarioOptions(): List<QaScenarioOption> {
        return map { label ->
            QaScenarioOption(
                label = label,
                standardQuestion = scenarioQuestionFor(label)
            )
        }
    }

    private fun scenarioQuestionFor(label: String): String {
        return when {
            label.contains("首次") || label.contains("办证") -> "第一次办理港澳通行证需要怎么做？"
            label.contains("续签") || label.contains("签注") -> "已有港澳通行证，签注过期或用完了怎么办？"
            label.contains("过期") || label.contains("遗失") || label.contains("丢") -> "港澳通行证过期、遗失或损坏了应该怎么办？"
            label.contains("材料") || label.contains("过关") -> "去香港或澳门过关要带什么材料？"
            label.contains("不确定") || label.contains("不清楚") -> "我不确定自己属于哪种港澳办理情况，应该怎么判断？"
            label.endsWith("？") || label.endsWith("?") -> label
            else -> "$label 应该怎么办？"
        }
    }

    private fun String?.cleanTextOrNull(): String? {
        return this
            ?.replace("\r\n", "\n")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun conciseConclusion(answer: String): String {
        val normalized = answer.replace("\n", " ").replace(Regex("""\s+"""), " ").trim()
        if (normalized.length <= 80) return normalized

        val sentenceEnd = listOf("。", "！", "？", ".", "!", "?")
            .mapNotNull { mark ->
                normalized.indexOf(mark).takeIf { index -> index in 16..79 }?.let { it + mark.length }
            }
            .minOrNull()

        return if (sentenceEnd != null) {
            normalized.take(sentenceEnd)
        } else {
            normalized.take(80).trimEnd() + "..."
        }
    }

    private fun friendlySourceTitle(raw: String): String? {
        val fileName = raw.substringAfterLast('/').substringAfterLast('\\').trim()
        val withoutExtension = fileName.replace(Regex("""\.(md|pdf|docx?|txt)$""", RegexOption.IGNORE_CASE), "")
        val withoutDate = withoutExtension.replace(Regex("""^\d{4}[-_]\d{2}[-_]\d{2}[-_]?"""), "")
        val readable = withoutDate
            .replace('_', ' ')
            .replace('-', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()

        if (readable.isBlank()) return null
        if (readable.contains("港澳") || readable.contains("通行证")) {
            return "港澳通行证办理指南"
        }
        if (readable.length > 28 || readable.contains("Phase", ignoreCase = true)) {
            return "知识库资料"
        }
        return readable
    }

    private fun classifyError(exc: Exception): String {
        return when (exc) {
            is SocketTimeoutException -> "查询失败，请稍后再试。"
            is HttpException -> when (exc.code()) {
                504 -> "查询失败，请稍后再试。"
                502 -> "查询失败，请稍后再试。"
                in 500..599 -> "查询失败，请稍后再试。"
                else -> "查询失败，请稍后再试。"
            }
            is IOException -> "查询失败，请稍后再试。"
            else -> "查询失败，请稍后再试。"
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
