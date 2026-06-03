package com.example.eldercareapp.ui.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eldercareapp.R
import com.example.eldercareapp.api.ApiClient
import com.example.eldercareapp.core.i18n.AppLanguageResolver
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.viewmodel.ChatMessageRole
import com.example.eldercareapp.model.QaAnswerUiModel
import com.example.eldercareapp.model.QaScenarioOption
import com.example.eldercareapp.ui.component.ActionCard
import com.example.eldercareapp.ui.component.BottomNavItemSpec
import com.example.eldercareapp.ui.component.CardLayoutMode
import com.example.eldercareapp.ui.component.ElderBackground
import com.example.eldercareapp.ui.component.ElderBlue
import com.example.eldercareapp.ui.component.ElderBlueDark
import com.example.eldercareapp.ui.component.ElderBluePale
import com.example.eldercareapp.ui.component.ElderBlueSoft
import com.example.eldercareapp.ui.component.ElderCard
import com.example.eldercareapp.ui.component.ElderGreen
import com.example.eldercareapp.ui.component.ElderGreenSoft
import com.example.eldercareapp.ui.component.ElderLine
import com.example.eldercareapp.ui.component.ElderOrange
import com.example.eldercareapp.ui.component.ElderOrangeSoft
import com.example.eldercareapp.ui.component.ElderRed
import com.example.eldercareapp.ui.component.ElderText
import com.example.eldercareapp.ui.component.ElderTextMuted
import com.example.eldercareapp.ui.component.ElderWindowSizeClass
import com.example.eldercareapp.ui.component.IconBadge
import com.example.eldercareapp.ui.component.InfoRow
import com.example.eldercareapp.ui.component.LocalElderResponsive
import com.example.eldercareapp.ui.component.PrimaryActionButton
import com.example.eldercareapp.ui.component.ProgressDot
import com.example.eldercareapp.ui.component.SecondaryActionButton
import com.example.eldercareapp.ui.component.SegmentedControl
import com.example.eldercareapp.ui.component.SectionTitle
import com.example.eldercareapp.ui.component.SoftCard
import com.example.eldercareapp.ui.component.TopBarAction
import com.example.eldercareapp.ui.component.UnifiedBottomNav
import com.example.eldercareapp.ui.component.UnifiedTopBar
import com.example.eldercareapp.ui.component.elderResponsiveSpec
import com.example.eldercareapp.viewmodel.ChatViewModel
import com.example.eldercareapp.viewmodel.ChatUiStrings
import com.example.eldercareapp.viewmodel.MaterialViewModel
import com.example.eldercareapp.viewmodel.SavedMaterialChecklist
import com.example.eldercareapp.voice.VoiceRecorder
import java.io.File
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class MainTab {
    Home,
    Chat,
    Service,
    My
}

private enum class OverlayScreen {
    PortDetail,
    Guide,
    Guidance,
    CrossBorderPreparePicker,
    FontSize,
    MaterialList
}

private val voiceLanguageOptions = listOf("普通话", "方言", "英语")
private const val SpeechTargetAnswer = "answer"
private const val SpeechTargetVoiceDraft = "voice_draft"
private const val SpeechTargetGuide = "guide"
private const val SpeechTargetGuidance = "guidance"
private const val MAX_RECORD_SECONDS = 30

@Composable
private fun currentDisplayLanguage(): AppLanguageResolver.DisplayLanguage {
    val configuration = LocalConfiguration.current
    val locale = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        configuration.locales[0]
    } else {
        @Suppress("DEPRECATION")
        configuration.locale
    }
    return AppLanguageResolver.resolveDisplayLanguage(locale)
}

private data class QaQuickQuestion(
    val labelResId: Int,
    val question: String,
    val openGuidance: Boolean = false,
)

private data class QaEmptyExample(
    val labelResId: Int,
    val question: String,
)

private val qaQuickQuestions = listOf(
    QaQuickQuestion(
        labelResId = R.string.qa_quick_first_permit,
        question = "第一次办理港澳通行证需要怎么做？"
    ),
    QaQuickQuestion(
        labelResId = R.string.qa_quick_endorsement,
        question = "已有港澳通行证，签注过期或用完了怎么办？"
    ),
    QaQuickQuestion(
        labelResId = R.string.qa_quick_border_materials,
        question = "去香港澳门口岸过关需要准备什么材料？"
    ),
    QaQuickQuestion(
        labelResId = R.string.qa_quick_unsure,
        question = "我不确定自己属于哪种港澳办理情况，应该怎么判断？",
        openGuidance = true
    )
)

private val qaEmptyExamples = listOf(
    QaEmptyExample(
        labelResId = R.string.qa_example_first_permit,
        question = "第一次办港澳通行证要带什么？"
    ),
    QaEmptyExample(
        labelResId = R.string.qa_example_endorsement_expired,
        question = "签注过期了怎么办？"
    ),
    QaEmptyExample(
        labelResId = R.string.qa_example_border_materials,
        question = "去香港过关要准备什么？"
    )
)

private fun voiceLanguageCode(label: String): String {
    return when (label) {
        "普通话" -> "zh"
        "英语" -> "en"
        else -> "auto"
    }
}

private fun ttsLanguageCode(label: String): String {
    return when (label) {
        "英语" -> "en"
        "方言" -> "yue"
        else -> "zh-CN"
    }
}

private fun speechPreferenceLabelRes(preference: AppLanguageResolver.SpeechLanguagePreference): Int {
    return when (preference) {
        AppLanguageResolver.SpeechLanguagePreference.AUTO -> R.string.speech_auto
        AppLanguageResolver.SpeechLanguagePreference.ZH_CN -> R.string.speech_mandarin
        AppLanguageResolver.SpeechLanguagePreference.YUE -> R.string.speech_cantonese
        AppLanguageResolver.SpeechLanguagePreference.EN -> R.string.speech_english
    }
}

@Composable
private fun chatUiStrings(): ChatUiStrings {
    return ChatUiStrings(
        emptyQuestionError = stringResource(R.string.chat_error_empty_question),
        noKnowledgeAnswer = stringResource(R.string.chat_error_no_knowledge),
        voiceNotClear = stringResource(R.string.chat_error_voice_not_clear),
        voiceNotClearConfirm = stringResource(R.string.chat_error_voice_not_clear_confirm),
        queryFailed = stringResource(R.string.chat_error_query_failed),
        voiceTimeout = stringResource(R.string.chat_error_voice_timeout),
        recordingTooLarge = stringResource(R.string.chat_error_recording_too_large),
        voiceServiceBusy = stringResource(R.string.chat_error_voice_service_busy),
        backendUnavailable = stringResource(R.string.chat_error_backend_unavailable),
        voiceFailed = stringResource(R.string.chat_error_voice_failed),
        networkUnstable = stringResource(R.string.chat_error_network_unstable),
        voiceNoResponse = stringResource(R.string.chat_error_voice_no_response),
        fallbackWarning = stringResource(R.string.qa_fallback_warning),
        fallbackTitle = stringResource(R.string.chat_answer_title),
        fallbackSubtitle = stringResource(R.string.qa_fallback_subtitle),
        sourceKnowledgeBase = stringResource(R.string.chat_source_kb),
        sourcePermitGuide = stringResource(R.string.qa_source_permit_guide),
        scenarioFirstPermit = stringResource(R.string.qa_scenario_first_permit),
        scenarioRenewal = stringResource(R.string.qa_scenario_renewal),
        scenarioExpiredOrLost = stringResource(R.string.qa_scenario_expired_lost),
        scenarioUnsure = stringResource(R.string.qa_scenario_unsure),
    )
}

private fun isVoiceNotClearMessage(message: String, uiStrings: ChatUiStrings): Boolean {
    return message == uiStrings.voiceNotClear || message == uiStrings.voiceNotClearConfirm
}

private class CloudSpeechController(
    val activeTarget: String?,
    val isPreparing: Boolean,
    val isSpeaking: Boolean,
    val speak: (text: String, target: String, audioUrl: String?, language: String) -> Unit,
    val stop: () -> Unit,
) {
    fun isPreparingTarget(target: String): Boolean = isPreparing && activeTarget == target
    fun isSpeakingTarget(target: String): Boolean = isSpeaking && activeTarget == target
    fun isActiveTarget(target: String): Boolean = activeTarget == target && (isPreparing || isSpeaking)
}

@Composable
private fun rememberCloudSpeechController(
    chatViewModel: ChatViewModel,
    language: String = "zh-CN",
): CloudSpeechController {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var activeTarget by remember { mutableStateOf<String?>(null) }
    var isPreparing by remember { mutableStateOf(false) }
    var isSpeaking by remember { mutableStateOf(false) }
    var speechRequestId by remember { mutableIntStateOf(0) }

    fun clearSpeechState() {
        mediaPlayer?.release()
        mediaPlayer = null
        activeTarget = null
        isPreparing = false
        isSpeaking = false
    }

    fun stopSpeaking() {
        speechRequestId += 1
        clearSpeechState()
    }

    fun showUnavailableToast() {
        Toast.makeText(context, "朗读服务暂时不可用，请稍后再试", Toast.LENGTH_SHORT).show()
    }

    fun playAudioUrl(audioUrl: String, target: String, requestId: Int) {
        val playableUrl = ApiClient.absoluteUrl(audioUrl)
        mediaPlayer?.release()

        val player = MediaPlayer()
        mediaPlayer = player
        activeTarget = target
        isPreparing = true
        isSpeaking = false

        try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            player.setDataSource(playableUrl)
            player.setOnPreparedListener { prepared ->
                mainHandler.post {
                    if (mediaPlayer == prepared && speechRequestId == requestId && activeTarget == target) {
                        isPreparing = false
                        isSpeaking = true
                        prepared.start()
                    } else {
                        prepared.release()
                    }
                }
            }
            player.setOnCompletionListener { completed ->
                mainHandler.post {
                    if (mediaPlayer == completed && speechRequestId == requestId) {
                        clearSpeechState()
                    }
                }
            }
            player.setOnErrorListener { failed, _, _ ->
                mainHandler.post {
                    if (mediaPlayer == failed && speechRequestId == requestId) {
                        clearSpeechState()
                        showUnavailableToast()
                    }
                }
                true
            }
            player.prepareAsync()
        } catch (_: Exception) {
            clearSpeechState()
            showUnavailableToast()
        }
    }

    fun speak(text: String, target: String, audioUrl: String?, requestedLanguage: String) {
        val content = text.trim()
        if (content.isBlank()) {
            Toast.makeText(context, "暂无可朗读内容", Toast.LENGTH_SHORT).show()
            return
        }

        stopSpeaking()
        speechRequestId += 1
        val requestId = speechRequestId
        activeTarget = target

        val existingUrl = audioUrl?.takeIf { it.isNotBlank() }
        if (existingUrl != null) {
            playAudioUrl(existingUrl, target, requestId)
            return
        }

        isPreparing = true
        isSpeaking = false
        coroutineScope.launch {
            try {
                val generatedUrl = chatViewModel.synthesizeSpeech(
                    text = content,
                    language = safeSpeechLanguageForAdHocText(content, requestedLanguage.ifBlank { language }, existingUrl),
                )
                if (speechRequestId != requestId || activeTarget != target) {
                    return@launch
                }
                if (generatedUrl.isNullOrBlank()) {
                    clearSpeechState()
                    showUnavailableToast()
                    return@launch
                }
                playAudioUrl(generatedUrl, target, requestId)
            } catch (_: Exception) {
                if (speechRequestId == requestId && activeTarget == target) {
                    clearSpeechState()
                    showUnavailableToast()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            clearSpeechState()
        }
    }

    return CloudSpeechController(
        activeTarget = activeTarget,
        isPreparing = isPreparing,
        isSpeaking = isSpeaking,
        speak = { text, target, audioUrl, requestedLanguage ->
            speak(text, target, audioUrl, requestedLanguage)
        },
        stop = { stopSpeaking() },
    )
}

private data class FontChoice(
    val label: String,
    val sample: String,
    val scale: Float
)

private data class PortInfo(
    val name: String,
    val openTime: String,
    val waitTime: String,
    val icon: ImageVector
)

private enum class GuidanceField {
    HasPass,
    PassValid,
    ApplyType,
    Destination,
    Purpose
}

private data class GuidanceProfile(
    val hasPass: String = "",
    val passValid: String = "",
    val applyType: String = "",
    val destination: String = "",
    val purpose: String = "",
)

private data class GuidanceQuestion(
    val field: GuidanceField,
    val title: String,
    val options: List<String>,
)

private data class GuidanceResult(
    val caseType: String,
    val recommendedAction: String,
    val standardQuestion: String,
    val windowScript: String,
    val notice: String,
    val checklistId: String = "service_uncertain_valid_pass",
)

private val fontChoices = listOf(
    FontChoice("小字", "小", 0.95f),
    FontChoice("中字", "中", 1.0f),
    FontChoice("大字", "大", 1.12f),
    FontChoice("超大字", "超大", 1.25f)
)

private val ports = listOf(
    PortInfo("深圳湾口岸", "6:30 - 24:00", "约 15 分钟", Icons.Filled.Place),
    PortInfo("福田口岸", "6:30 - 22:30", "约 10 分钟", Icons.Filled.Place)
)

private val guidanceQuestions = listOf(
    GuidanceQuestion(
        field = GuidanceField.HasPass,
        title = "您现在有没有港澳通行证？",
        options = listOf("有", "没有", "不清楚"),
    ),
    GuidanceQuestion(
        field = GuidanceField.PassValid,
        title = "您的港澳通行证还在有效期内吗？",
        options = listOf("在有效期内", "已经过期", "不知道怎么看"),
    ),
    GuidanceQuestion(
        field = GuidanceField.ApplyType,
        title = "您这次想办理什么？",
        options = listOf("第一次办理", "签注过期或用完", "通行证过期", "通行证丢了或坏了", "不确定"),
    ),
    GuidanceQuestion(
        field = GuidanceField.Destination,
        title = "您想去哪里？",
        options = listOf("香港", "澳门", "香港和澳门都去", "还没确定"),
    ),
    GuidanceQuestion(
        field = GuidanceField.Purpose,
        title = "您去港澳的目的是什么？",
        options = listOf("旅游", "探亲", "商务", "学习工作逗留", "不确定"),
    ),
)

private fun GuidanceProfile.answerFor(field: GuidanceField): String {
    return when (field) {
        GuidanceField.HasPass -> hasPass
        GuidanceField.PassValid -> passValid
        GuidanceField.ApplyType -> applyType
        GuidanceField.Destination -> destination
        GuidanceField.Purpose -> purpose
    }
}

private fun GuidanceProfile.withAnswer(field: GuidanceField, answer: String): GuidanceProfile {
    val updated = when (field) {
        GuidanceField.HasPass -> copy(hasPass = answer)
        GuidanceField.PassValid -> copy(passValid = answer)
        GuidanceField.ApplyType -> copy(applyType = answer)
        GuidanceField.Destination -> copy(destination = answer)
        GuidanceField.Purpose -> copy(purpose = answer)
    }
    return updated.normalizedForGuidanceFlow()
}

private fun GuidanceProfile.normalizedForGuidanceFlow(): GuidanceProfile {
    return when {
        hasPass.isBlank() || hasPass == "不清楚" -> copy(
            passValid = "",
            applyType = "",
            destination = "",
            purpose = "",
        )

        hasPass == "没有" -> copy(
            passValid = "",
            applyType = "",
            purpose = if (destination == "还没确定") "" else purpose,
        )

        passValid.isBlank() || passValid == "不知道怎么看" -> copy(
            applyType = "",
            destination = "",
            purpose = "",
        )

        passValid == "已经过期" -> copy(
            applyType = "",
            purpose = if (destination == "还没确定") "" else purpose,
        )

        applyType.isBlank() || applyType in setOf("不确定", "其他情况") -> copy(
            destination = "",
            purpose = "",
        )

        destination == "还没确定" -> copy(purpose = "")

        else -> this
    }
}

private fun GuidanceProfile.visibleGuidanceQuestions(): List<GuidanceQuestion> {
    val hasPassQuestion = guidanceQuestions.first { it.field == GuidanceField.HasPass }
    val passValidQuestion = guidanceQuestions.first { it.field == GuidanceField.PassValid }
    val applyTypeQuestion = guidanceQuestions.first { it.field == GuidanceField.ApplyType }
    val destinationQuestion = guidanceQuestions.first { it.field == GuidanceField.Destination }
    val purposeQuestion = guidanceQuestions.first { it.field == GuidanceField.Purpose }

    val visible = mutableListOf(hasPassQuestion)
    when (hasPass) {
        "" -> return visible
        "不清楚" -> return visible
        "没有" -> {
            visible += destinationQuestion
            if (destination.isNotBlank() && destination != "还没确定") {
                visible += purposeQuestion
            }
            return visible
        }
    }

    visible += passValidQuestion
    when (passValid) {
        "" -> return visible
        "不知道怎么看" -> return visible
        "已经过期" -> {
            visible += destinationQuestion
            if (destination.isNotBlank() && destination != "还没确定") {
                visible += purposeQuestion
            }
            return visible
        }
    }

    visible += applyTypeQuestion.copy(
        options = listOf("签注过期或用完", "通行证丢了或坏了", "其他情况", "不确定")
    )
    if (applyType.isBlank() || applyType in setOf("不确定", "其他情况")) {
        return visible
    }

    visible += destinationQuestion
    if (destination.isNotBlank() && destination != "还没确定") {
        visible += purposeQuestion
    }
    return visible
}

private fun GuidanceProfile.missingCount(questions: List<GuidanceQuestion>): Int {
    return questions.count { answerFor(it.field).isBlank() }
}

private fun buildGuidanceResult(profile: GuidanceProfile): GuidanceResult {
    val destinationText = when (profile.destination) {
        "香港和澳门都去" -> "香港和澳门"
        "还没确定" -> "港澳"
        else -> profile.destination.ifBlank { "港澳" }
    }
    val purposeText = profile.purpose.ifBlank { "出行" }
    val destinationUnclear = profile.destination == "还没确定" || profile.destination.isBlank()
    val purposeUnclear = profile.purpose == "不确定" || profile.purpose.isBlank()
    val firstApplyCaseType = when {
        destinationUnclear -> "首次办理往来港澳通行证，目的地和签注类型还需确认"
        purposeUnclear -> "首次办理往来港澳通行证，签注类型还需确认"
        else -> "首次办理往来港澳通行证及${destinationText}签注"
    }
    val firstApplyAction = when {
        destinationUnclear -> "您还没有港澳通行证，通常需要先本人到出入境窗口办理通行证。由于目的地还没确定，建议到窗口先说明出行计划，请工作人员帮您确认应申请香港、澳门还是两地签注。"
        purposeUnclear -> "您还没有港澳通行证，通常需要先本人到出入境窗口办理通行证。由于出行目的还不确定，建议请工作人员帮您确认应申请旅游、探亲、商务或其他签注类型。"
        else -> "首次办理通常需要本人到出入境窗口办理，建议提前准备身份证、照片回执，并确认当地是否需要预约。"
    }
    val firstApplyQuestion = when {
        destinationUnclear -> "用户没有港澳通行证，暂时还没确定去香港还是澳门。请根据官方资料说明首次办理往来港澳通行证时应如何确认目的地和签注类型、需要携带什么材料、是否需要本人到窗口办理。"
        purposeUnclear -> "用户没有港澳通行证，想去$destinationText，但还不确定出行目的。请根据官方资料说明首次办理往来港澳通行证时如何确认签注类型、需要携带什么材料、是否需要本人到窗口办理。"
        else -> "用户没有港澳通行证，想去$destinationText，出行目的是$purposeText。请根据官方资料说明首次办理往来港澳通行证及签注需要携带什么材料、是否需要本人到窗口、现场应该怎么办理。"
    }
    val firstApplyScript = when {
        destinationUnclear -> "我现在还没有港澳通行证，想先了解第一次办理需要准备什么，也想请您帮我确认应该申请香港、澳门还是两地签注。"
        purposeUnclear -> "我现在还没有港澳通行证，想去$destinationText，但还不确定这次出行属于旅游、探亲还是其他目的。请帮我确认首次办理时应该一起申请哪种签注。"
        else -> "我想第一次办理往来港澳通行证和${destinationText}签注，出行目的是$purposeText。请问我需要先取号还是先拍照填表？"
    }
    val notice = "以上结果为办事辅助建议，具体要求以当地出入境管理部门或现场窗口为准。"

    return when {
        profile.hasPass == "不清楚" -> GuidanceResult(
            caseType = "信息还不够，需要先确认是否已有港澳通行证",
            recommendedAction = "建议先找一找是否已经办理过港澳通行证，或带身份证到出入境窗口请工作人员查询确认。确认后再判断是首次办理、换发还是办理签注。",
            standardQuestion = "用户不确定自己是否已有港澳通行证。请根据官方资料说明应该如何确认办证状态、到窗口需要携带什么证件、确认后可能分别办理哪些业务。",
            windowScript = "我不确定自己以前有没有办过港澳通行证，想请您帮我查一下现在应该办哪一种业务。",
            notice = notice,
        )

        profile.hasPass == "没有" || profile.applyType == "第一次办理" -> GuidanceResult(
            caseType = firstApplyCaseType,
            recommendedAction = firstApplyAction,
            standardQuestion = firstApplyQuestion,
            windowScript = firstApplyScript,
            notice = notice,
            checklistId = "hk_macau_pass_apply",
        )

        profile.passValid == "不知道怎么看" -> GuidanceResult(
            caseType = "信息还不够，需要先确认通行证有效期",
            recommendedAction = "建议查看通行证个人信息页上的有效期，以及签注页上的目的地、次数和有效期；如果看不清楚，带身份证和通行证到出入境窗口请工作人员确认。",
            standardQuestion = "用户已有港澳通行证，但不知道怎么看证件是否仍在有效期内。请根据官方资料说明应查看哪些位置、需要确认哪些信息、是否建议到窗口咨询。",
            windowScript = "我有港澳通行证，但不会看是不是还有效，也不知道签注能不能用。请帮我看一下应该办哪一种业务。",
            notice = notice,
        )

        profile.passValid == "已经过期" || profile.applyType == "通行证过期" -> GuidanceResult(
            caseType = "换发港澳通行证",
            recommendedAction = if (purposeUnclear) {
                "您的通行证已经过期，主流程应先按换证办理。出行目的还不确定时，建议换证时请工作人员一并确认后续应办理哪类签注。"
            } else {
                "建议按换证流程办理，通常需要带身份证、原港澳通行证和证件照片材料，换证后再按出行需要办理签注。"
            },
            standardQuestion = "用户的港澳通行证已经过期，想去$destinationText，出行目的是$purposeText。请根据官方资料说明如何办理换发港澳通行证、需要携带什么材料、是否还需要重新办理签注。",
            windowScript = "我的港澳通行证已经过期了，想去${destinationText}${purposeText}。请问我应该先办理换证，还是可以一起办理签注？",
            notice = notice,
        )

        profile.applyType == "通行证丢了或坏了" -> GuidanceResult(
            caseType = "补发 / 换发港澳通行证",
            recommendedAction = "证件丢失、损坏或信息不清时，建议直接到出入境窗口办理补发或换发，并按窗口要求补充说明材料。",
            standardQuestion = "用户的港澳通行证丢了或坏了，想去$destinationText，出行目的是$purposeText。请根据官方资料说明补发或换发港澳通行证需要什么材料、是否必须到窗口办理、需要注意哪些事项。",
            windowScript = "我的港澳通行证丢了或坏了，想重新办理。请问我需要补发还是换发，需要准备哪些材料？",
            notice = notice,
        )

        profile.applyType in setOf("不确定", "其他情况") -> GuidanceResult(
            caseType = "办理事项还需要确认",
            recommendedAction = "您已经确认有有效港澳通行证，但还不确定这次具体要办签注、换证、补发还是其他业务。建议带身份证和港澳通行证到窗口，请工作人员先核对证件和签注状态。",
            standardQuestion = "用户已有有效港澳通行证，但不确定这次应该办理哪种港澳通行证或签注业务。请根据官方资料说明应先核对哪些信息、到窗口如何说明、可能对应哪些办理事项。",
            windowScript = "我有港澳通行证，但不确定这次应该办签注、换证还是其他业务。请帮我看一下证件和签注状态。",
            notice = notice,
        )

        profile.purpose in setOf("探亲", "商务", "学习工作逗留") -> GuidanceResult(
            caseType = "非旅游类签注",
            recommendedAction = "非旅游类签注可能需要邀请、亲属关系、商务或学习工作等额外证明，建议优先到窗口咨询后再准备材料。",
            standardQuestion = "用户已有港澳通行证，想去$destinationText，出行目的是${profile.purpose}。请根据官方资料说明办理非旅游类签注可能需要哪些额外材料、哪些情况需要到窗口咨询、现场应该怎么说明。",
            windowScript = "我想办理去${destinationText}的${profile.purpose}签注。请问这种签注需要额外证明材料吗？",
            notice = notice,
        )

        profile.passValid == "在有效期内" &&
            profile.applyType == "签注过期或用完" &&
            destinationUnclear -> GuidanceResult(
            caseType = "再次办理签注，目的地和签注类型还需确认",
            recommendedAction = "您的通行证仍在有效期内，主方向是再次办理签注。由于还没确定去香港还是澳门，建议先确认目的地，再选择对应签注。",
            standardQuestion = "用户已有有效港澳通行证，签注已经过期或用完，但还没确定去香港还是澳门。请根据官方资料说明再次办理签注前应确认哪些信息、需要携带什么材料、是否可以使用自助签注机。",
            windowScript = "我的港澳通行证还在有效期内，但签注过期或用完了。我还没确定去香港还是澳门，请问应该怎么确认并办理签注？",
            notice = notice,
            checklistId = "hk_macau_renewal",
        )

        profile.passValid == "在有效期内" &&
            profile.applyType == "签注过期或用完" &&
            profile.purpose == "旅游" -> GuidanceResult(
            caseType = "再次办理${destinationText}旅游签注",
            recommendedAction = "通行证仍有效且只是旅游签注过期或用完时，可优先了解当地自助签注机或线上申请后打印签注；特殊情况仍需到窗口确认。",
            standardQuestion = "用户已有有效港澳通行证，但${destinationText}旅游签注已经过期或用完。请根据官方资料说明是否可以通过自助签注机办理、需要携带什么材料、哪些情况需要去窗口、到现场应该怎么和工作人员说明。",
            windowScript = "我想办理${destinationText}旅游签注。我的港澳通行证还在有效期内，只是签注过期或用完了。请问可以在自助机办理吗？",
            notice = notice,
            checklistId = "hk_macau_renewal",
        )

        profile.passValid == "在有效期内" &&
            profile.applyType == "签注过期或用完" &&
            purposeUnclear -> GuidanceResult(
            caseType = "再次办理${destinationText}签注，签注类型还需确认",
            recommendedAction = "您的通行证仍在有效期内，主方向是再次办理签注。由于出行目的还不确定，建议先请工作人员确认应办理旅游、探亲、商务或其他签注类型。",
            standardQuestion = "用户已有有效港澳通行证，${destinationText}签注已经过期或用完，但还不确定出行目的。请根据官方资料说明再次办理签注时如何确认签注类型、需要携带什么材料、哪些情况需要去窗口。",
            windowScript = "我的港澳通行证还在有效期内，${destinationText}签注过期或用完了，但我还不确定应该办哪种签注类型。请帮我确认一下。",
            notice = notice,
            checklistId = "hk_macau_renewal",
        )

        else -> GuidanceResult(
            caseType = "港澳通行证 / 签注办理咨询",
            recommendedAction = "建议带上身份证和港澳通行证，先确认通行证有效期、签注目的地和签注次数，再按窗口或自助设备提示办理。",
            standardQuestion = "用户想办理港澳通行证或签注相关业务，目的地是$destinationText，出行目的是$purposeText。请根据官方资料说明应该先确认哪些信息、需要准备哪些材料、哪些情况适合窗口办理。",
            windowScript = "我想办理港澳通行证或签注相关业务，想请您帮我确认应该办哪一种。",
            notice = notice,
        )
    }
}

@Composable
fun ElderCareAppRoot(modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    var overlayScreen by remember { mutableStateOf<OverlayScreen?>(null) }
    var activeChecklistId by remember { mutableStateOf<String?>(null) }
    var checklistBackTarget by remember { mutableStateOf<OverlayScreen?>(null) }
    var selectedFont by remember { mutableStateOf(fontChoices[2]) }
    var speechPreference by remember { mutableStateOf(AppLanguageResolver.SpeechLanguagePreference.AUTO) }
    val chatViewModel: ChatViewModel = viewModel()
    val materialViewModel: MaterialViewModel = viewModel()
    val currentDensity = LocalDensity.current
    val displayLanguage = currentDisplayLanguage()
    val displayLanguageCode = displayLanguage.apiCode
    val resolvedSpeechLanguage = AppLanguageResolver.resolveSpeechLanguage(speechPreference, displayLanguage)

    BackHandler(enabled = overlayScreen != null || currentTab != MainTab.Home) {
        if (overlayScreen == OverlayScreen.MaterialList && activeChecklistId != null && checklistBackTarget != null) {
            overlayScreen = checklistBackTarget
            activeChecklistId = null
            checklistBackTarget = null
            materialViewModel.closeChecklist()
        } else if (overlayScreen != null) {
            overlayScreen = null
            activeChecklistId = null
            checklistBackTarget = null
            materialViewModel.closeChecklist()
        } else {
            currentTab = MainTab.Home
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val responsive = remember(maxWidth, selectedFont.scale) {
            elderResponsiveSpec(maxWidth, selectedFont.scale)
        }
        CompositionLocalProvider(
            LocalDensity provides Density(
                density = currentDensity.density,
                fontScale = selectedFont.scale
            ),
            LocalElderResponsive provides responsive
        ) {
            val openFont = { overlayScreen = OverlayScreen.FontSize }
            val closeOverlay = {
                overlayScreen = null
                activeChecklistId = null
                checklistBackTarget = null
                materialViewModel.closeChecklist()
            }
            val openMaterialList = {
                activeChecklistId = null
                checklistBackTarget = null
                materialViewModel.closeChecklist()
                overlayScreen = OverlayScreen.MaterialList
            }
            val openMaterialChecklist = { checklistId: String, backTarget: OverlayScreen? ->
                if (backTarget != null || overlayScreen != OverlayScreen.MaterialList) {
                    checklistBackTarget = backTarget
                }
                activeChecklistId = checklistId
                materialViewModel.selectItem(checklistId)
                overlayScreen = OverlayScreen.MaterialList
            }
            val backFromMaterialChecklist = {
                val target = checklistBackTarget
                if (target != null) {
                    overlayScreen = target
                    activeChecklistId = null
                    checklistBackTarget = null
                    materialViewModel.closeChecklist()
                } else {
                    closeOverlay()
                }
            }
            val openCrossBorderPreparePicker = {
                activeChecklistId = null
                checklistBackTarget = null
                materialViewModel.closeChecklist()
                overlayScreen = OverlayScreen.CrossBorderPreparePicker
            }
            val rootModifier = Modifier.fillMaxSize()

            if (overlayScreen != null) {
                when (overlayScreen) {
                    OverlayScreen.PortDetail -> PortDetailScreen(
                        modifier = rootModifier,
                        onBack = closeOverlay,
                        onAskAi = {
                            overlayScreen = null
                            activeChecklistId = null
                            checklistBackTarget = null
                            currentTab = MainTab.Chat
                        },
                        onOpenMaterialList = { openMaterialChecklist("border_crossing_prepare", null) }
                    )

                    OverlayScreen.Guide -> GuideScreen(
                        modifier = rootModifier,
                        chatViewModel = chatViewModel,
                        speechLanguage = resolvedSpeechLanguage,
                        onBack = closeOverlay,
                        onDone = closeOverlay
                    )

                    OverlayScreen.Guidance -> GuidanceScreen(
                        modifier = rootModifier,
                        chatViewModel = chatViewModel,
                        speechLanguage = resolvedSpeechLanguage,
                        onBack = closeOverlay,
                        onOpenMaterialList = { checklistId -> openMaterialChecklist(checklistId, null) },
                        onOpenDetailedPolicy = { standardQuestion ->
                            overlayScreen = null
                            activeChecklistId = null
                            checklistBackTarget = null
                            currentTab = MainTab.Chat
                            chatViewModel.submitPrefilledQuestion(
                                standardQuestion,
                                inputType = "guidance",
                                displayLanguage = displayLanguageCode,
                                speechLanguage = resolvedSpeechLanguage
                            )
                        }
                    )

                    OverlayScreen.CrossBorderPreparePicker -> CrossBorderPreparePickerScreen(
                        modifier = rootModifier,
                        onBack = closeOverlay,
                        onOpenChecklist = { checklistId -> openMaterialChecklist(checklistId, OverlayScreen.CrossBorderPreparePicker) },
                        onOpenGuidance = {
                            activeChecklistId = null
                            checklistBackTarget = null
                            materialViewModel.closeChecklist()
                            overlayScreen = OverlayScreen.Guidance
                        }
                    )

                    OverlayScreen.FontSize -> FontSizeScreen(
                        modifier = rootModifier,
                        selected = selectedFont,
                        onSelected = { selectedFont = it },
                        onBack = closeOverlay
                    )

                    OverlayScreen.MaterialList -> MaterialListScreen(
                        modifier = rootModifier,
                        activeChecklistId = activeChecklistId,
                        materialViewModel = materialViewModel,
                        onBack = closeOverlay,
                        onChecklistBack = backFromMaterialChecklist,
                        onOpenChecklist = { checklistId -> openMaterialChecklist(checklistId, null) }
                    )

                    else -> Unit
                }
            } else {
                Scaffold(
                    modifier = rootModifier,
                    containerColor = ElderBackground,
                    bottomBar = {
                        UnifiedBottomNav(
                            items = listOf(
                                BottomNavItemSpec(stringResource(R.string.nav_home), Icons.Filled.Home, currentTab == MainTab.Home) {
                                    currentTab = MainTab.Home
                                },
                                BottomNavItemSpec(stringResource(R.string.nav_chat), Icons.Filled.Email, currentTab == MainTab.Chat) {
                                    currentTab = MainTab.Chat
                                },
                                BottomNavItemSpec(stringResource(R.string.nav_service), Icons.Filled.List, currentTab == MainTab.Service) {
                                    currentTab = MainTab.Service
                                },
                                BottomNavItemSpec(stringResource(R.string.nav_my), Icons.Filled.Person, currentTab == MainTab.My) {
                                    currentTab = MainTab.My
                                }
                            )
                        )
                    }
                ) { innerPadding ->
                    when (currentTab) {
                        MainTab.Home -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenFontSize = openFont,
                            onOpenGuide = { overlayScreen = OverlayScreen.Guide },
                            onOpenPortDetail = { overlayScreen = OverlayScreen.PortDetail },
                            onOpenChat = { currentTab = MainTab.Chat },
                            onOpenMaterialList = openMaterialList,
                            speechPreference = speechPreference,
                            onSpeechPreferenceChange = { speechPreference = it },
                            onFaqClick = { question ->
                                chatViewModel.submitPrefilledQuestion(
                                    question,
                                    inputType = "text",
                                    displayLanguage = displayLanguageCode,
                                    speechLanguage = resolvedSpeechLanguage
                                )
                                currentTab = MainTab.Chat
                            }
                        )

                        MainTab.Chat -> ChatScreen(
                            modifier = Modifier.padding(innerPadding),
                            chatViewModel = chatViewModel,
                            displayLanguage = displayLanguageCode,
                            speechLanguage = resolvedSpeechLanguage,
                            onOpenMaterialList = { openMaterialChecklist("hk_macau_pass_apply", null) },
                            onOpenGuidance = { overlayScreen = OverlayScreen.Guidance },
                            onOpenFontSize = openFont
                        )

                        MainTab.Service -> ServiceScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenGuidance = { overlayScreen = OverlayScreen.Guidance },
                            onOpenCrossBorderPreparePicker = openCrossBorderPreparePicker
                        )

                        MainTab.My -> MyScreen(
                            modifier = Modifier.padding(innerPadding),
                            onOpenSavedChecklist = { checklistId -> openMaterialChecklist(checklistId, null) },
                            materialViewModel = materialViewModel
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    onOpenFontSize: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenPortDetail: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenMaterialList: () -> Unit,
    speechPreference: AppLanguageResolver.SpeechLanguagePreference,
    onSpeechPreferenceChange: (AppLanguageResolver.SpeechLanguagePreference) -> Unit,
    onFaqClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    var region by remember { mutableStateOf("香港") }
    var direction by remember { mutableStateOf("去往港澳") }
    var faqCategory by remember { mutableStateOf("全部") }
    var showVoiceSheet by remember { mutableStateOf(false) }
    val voiceOptions = listOf(
        AppLanguageResolver.SpeechLanguagePreference.AUTO,
        AppLanguageResolver.SpeechLanguagePreference.ZH_CN,
        AppLanguageResolver.SpeechLanguagePreference.YUE,
        AppLanguageResolver.SpeechLanguagePreference.EN
    )

    if (showVoiceSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVoiceSheet = false },
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = responsive.pagePadding, vertical = responsive.cardSpacing),
                verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
            ) {
                Text(
                    text = stringResource(R.string.speech_settings_title),
                    color = ElderText,
                    fontSize = responsive.cardTitle,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.speech_settings_desc),
                    color = ElderTextMuted,
                    fontSize = responsive.body
                )
                voiceOptions.forEach { option ->
                    val optionLabel = stringResource(speechPreferenceLabelRes(option))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .background(if (option == speechPreference) ElderBlueSoft else Color.White, RoundedCornerShape(14.dp))
                            .border(1.dp, if (option == speechPreference) ElderBlue else ElderLine, RoundedCornerShape(14.dp))
                            .clickable {
                                onSpeechPreferenceChange(option)
                                showVoiceSheet = false
                            }
                            .padding(horizontal = responsive.cardSpacing, vertical = responsive.rowSpacing),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VolumeUp,
                            contentDescription = null,
                            tint = ElderBlue,
                            modifier = Modifier.size(responsive.iconSmall)
                        )
                        Text(
                            text = optionLabel,
                            color = ElderText,
                            fontSize = responsive.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        if (option == speechPreference) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = ElderBlue,
                                modifier = Modifier.size(responsive.iconSmall)
                            )
                        }
                    }
                }
            }
        }
    }

    ScreenColumn(
        modifier = modifier,
        topBar = {
            UnifiedTopBar(
                title = stringResource(R.string.app_name),
                subtitle = stringResource(R.string.app_subtitle),
                gradient = true,
                actions = listOf(
                    TopBarAction(
                        label = stringResource(R.string.speech_label_prefix) +
                            stringResource(speechPreferenceLabelRes(speechPreference)),
                        icon = Icons.Filled.VolumeUp,
                        onClick = { showVoiceSheet = true },
                        alwaysShowText = true
                    ),
                    TopBarAction(stringResource(R.string.common_font), Icons.Filled.Settings, onClick = onOpenFontSize)
                )
            )
        }
    ) {
        SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFBFD8FF)) {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Email, size = responsive.iconLarge)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Text(
                            text = stringResource(R.string.home_hero_title),
                            color = ElderText,
                            fontSize = responsive.topBarTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.home_hero_desc),
                            color = ElderTextMuted,
                            fontSize = responsive.body
                        )
                    }
                }
                AdaptivePairRow(
                    first = { itemModifier ->
                        PrimaryActionButton(
                            text = stringResource(R.string.home_ask),
                            icon = Icons.Filled.Email,
                            onClick = onOpenChat,
                            modifier = itemModifier
                        )
                    },
                    second = { itemModifier ->
                        SecondaryActionButton(
                            text = stringResource(R.string.home_guide),
                            icon = Icons.Filled.Info,
                            onClick = onOpenGuide,
                            modifier = itemModifier
                        )
                    }
                )
            }
        }

        SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
            Column(
                modifier = Modifier.padding(responsive.cardPadding),
                verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Call, size = responsive.iconMedium)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Text(
                            text = stringResource(R.string.home_help_title),
                            color = ElderText,
                            fontSize = responsive.cardTitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.home_help_desc),
                            color = ElderTextMuted,
                            fontSize = responsive.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                if (responsive.stackActionRows) {
                    PrimaryActionButton(
                        text = stringResource(R.string.home_contact_service),
                        icon = Icons.Filled.Call,
                        onClick = { Toast.makeText(context, "客服功能暂未接入", Toast.LENGTH_SHORT).show() },
                        height = 52.dp
                    )
                    SecondaryActionButton(
                        text = stringResource(R.string.home_volunteer_help),
                        icon = Icons.Filled.Favorite,
                        onClick = { Toast.makeText(context, "志愿者协助暂未接入", Toast.LENGTH_SHORT).show() },
                        height = 52.dp
                    )
                    SecondaryActionButton(
                        text = stringResource(R.string.home_video_guide),
                        icon = Icons.Filled.PlayArrow,
                        onClick = { Toast.makeText(context, "视频讲解暂未接入", Toast.LENGTH_SHORT).show() },
                        height = 52.dp
                    )
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)) {
                        PrimaryActionButton(
                            text = stringResource(R.string.home_contact_service),
                            icon = Icons.Filled.Call,
                            onClick = { Toast.makeText(context, "客服功能暂未接入", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f),
                            height = 52.dp
                        )
                        SecondaryActionButton(
                            text = stringResource(R.string.home_volunteer_help),
                            icon = Icons.Filled.Favorite,
                            onClick = { Toast.makeText(context, "志愿者协助暂未接入", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f),
                            height = 52.dp
                        )
                        SecondaryActionButton(
                            text = stringResource(R.string.home_video_guide),
                            icon = Icons.Filled.PlayArrow,
                            onClick = { Toast.makeText(context, "视频讲解暂未接入", Toast.LENGTH_SHORT).show() },
                            modifier = Modifier.weight(1f),
                            height = 52.dp
                        )
                    }
                }
            }
        }

        if (!responsive.isExtraLargeText) {
            SegmentedControl(
                options = listOf("香港", "澳门", "全部"),
                selected = region,
                onSelected = { region = it }
            )
            SegmentedControl(
                options = listOf("去往港澳", "返回内地", "全部"),
                selected = direction,
                onSelected = { direction = it }
            )
        }

        SectionTitle(text = stringResource(R.string.home_ports))
        ports.forEach { port ->
            PortSummaryCard(port = port, onClick = onOpenPortDetail)
        }
        SecondaryActionButton(
            text = stringResource(R.string.home_all_ports),
            icon = Icons.Filled.List,
            onClick = onOpenPortDetail,
            height = 52.dp
        )

        SectionTitle(text = stringResource(R.string.home_faq))
        FaqSection(
            selectedCategory = faqCategory,
            onCategorySelected = { faqCategory = it },
            onQuestionClick = onFaqClick
        )

        ActionCard(
            title = stringResource(R.string.home_saved_materials),
            subtitle = stringResource(R.string.home_saved_materials_desc),
            icon = Icons.Filled.List,
            onClick = onOpenMaterialList
        )
    }
}

@Composable
private fun ChatScreen(
    chatViewModel: ChatViewModel,
    displayLanguage: String,
    speechLanguage: String,
    onOpenMaterialList: () -> Unit,
    onOpenGuidance: () -> Unit,
    onOpenFontSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by chatViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val uiStrings = chatUiStrings()
    val voiceRecorder = remember { VoiceRecorder() }
    var showVoicePanel by remember { mutableStateOf(false) }
    var selectedVoiceLanguage by remember { mutableStateOf("普通话") }
    var isRecording by remember { mutableStateOf(false) }
    var voicePanelMessage by remember { mutableStateOf<String?>(null) }
    val chatScrollState = rememberScrollState()
    val speech = rememberCloudSpeechController(chatViewModel, speechLanguage)

    DisposableEffect(Unit) {
        onDispose {
            voiceRecorder.cancel()
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            delay(MAX_RECORD_SECONDS * 1000L)
            if (isRecording) {
                voicePanelMessage = context.getString(R.string.voice_recording_auto_stopped)
                val file = voiceRecorder.stop()
                isRecording = false
                showVoicePanel = false
                if (file == null) {
                    Toast.makeText(context, context.getString(R.string.voice_recording_too_short), Toast.LENGTH_SHORT).show()
                } else {
                    chatViewModel.transcribeVoice(
                        file,
                        voiceLanguageCode(selectedVoiceLanguage),
                        context.getString(R.string.voice_sample_live_recording),
                        uiStrings
                    )
                }
            }
        }
    }

    LaunchedEffect(
        uiState.messages.size,
        uiState.isLoading,
        uiState.isTranscribing,
        uiState.voiceDraft,
        uiState.errorMessage
    ) {
        chatScrollState.animateScrollTo(chatScrollState.maxValue)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showVoicePanel = true
        } else {
            Toast.makeText(context, context.getString(R.string.voice_permission_required), Toast.LENGTH_SHORT).show()
        }
    }
    val audioSampleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult

        val sampleFile = copyAudioSampleToCache(context, uri)
        if (sampleFile == null) {
            Toast.makeText(context, context.getString(R.string.voice_sample_read_failed), Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }

        voicePanelMessage = null
        showVoicePanel = false
        chatViewModel.transcribeVoice(
            file = sampleFile,
            language = voiceLanguageCode(selectedVoiceLanguage),
            sampleName = getDisplayNameForUri(context, uri),
            uiStrings = uiStrings
        )
    }

    fun openVoicePanel() {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            voicePanelMessage = null
            showVoicePanel = true
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun startRecording() {
        if (isRecording || uiState.isTranscribing) return
        try {
            voiceRecorder.start(context)
            voicePanelMessage = null
            isRecording = true
        } catch (exc: Exception) {
            isRecording = false
            Toast.makeText(context, context.getString(R.string.voice_recording_start_failed), Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingAndUpload() {
        if (!isRecording) return
        val file = voiceRecorder.stop()
        isRecording = false
        showVoicePanel = false
        if (file == null) {
            Toast.makeText(context, context.getString(R.string.voice_recording_too_short), Toast.LENGTH_SHORT).show()
            return
        }
        chatViewModel.transcribeVoice(
            file,
            voiceLanguageCode(selectedVoiceLanguage),
            context.getString(R.string.voice_sample_live_recording),
            uiStrings
        )
    }

    fun stopRecording() {
        voiceRecorder.cancel()
        isRecording = false
        voicePanelMessage = null
        showVoicePanel = false
    }
    val chatWidthModifier = if (responsive.windowSizeClass == ElderWindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier
            .fillMaxWidth()
            .widthIn(max = responsive.chatContentMaxWidth)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElderBackground)
    ) {
        UnifiedTopBar(
            title = stringResource(R.string.chat_title),
            elevated = true,
            actions = listOf(
                TopBarAction(stringResource(R.string.common_history), Icons.Filled.DateRange) {
                    Toast.makeText(context, context.getString(R.string.chat_history_toast), Toast.LENGTH_SHORT).show()
                },
                TopBarAction(stringResource(R.string.common_font), Icons.Filled.Settings, onClick = onOpenFontSize)
            )
        )

        QaQuickQuestionChips(
            items = qaQuickQuestions,
            onClick = { item ->
                if (item.openGuidance) {
                    onOpenGuidance()
                } else {
                    speech.stop()
                    chatViewModel.submitPrefilledQuestion(
                        item.question,
                        inputType = "quick",
                        displayLanguage = displayLanguage,
                        speechLanguage = speechLanguage,
                        uiStrings = uiStrings
                    )
                }
            },
            modifier = chatWidthModifier.align(Alignment.CenterHorizontally)
        )

        Column(
            modifier = chatWidthModifier
                .align(Alignment.CenterHorizontally)
                .weight(1f)
                .verticalScroll(chatScrollState)
                .padding(
                    start = responsive.pagePadding,
                    end = responsive.pagePadding,
                    top = responsive.pagePadding,
                    bottom = responsive.pagePadding + 112.dp
                ),
            verticalArrangement = Arrangement.spacedBy(responsive.pageSpacing)
        ) {
            if (uiState.messages.isEmpty() && !uiState.isLoading && !uiState.isTranscribing && uiState.errorMessage.isNullOrBlank()) {
                QaEmptyState(
                    examples = qaEmptyExamples,
                    onExampleClick = { question ->
                        speech.stop()
                        chatViewModel.submitPrefilledQuestion(
                            question,
                            inputType = "example",
                            displayLanguage = displayLanguage,
                            speechLanguage = speechLanguage,
                            uiStrings = uiStrings
                        )
                    },
                    onGuidanceClick = onOpenGuidance
                )
            }

            uiState.messages.forEach { message ->
                when (message.role) {
                    ChatMessageRole.User -> UserBubble(text = message.text)
                    ChatMessageRole.Assistant -> {
                        val answer = message.answerUiModel
                        if (answer != null) {
                            val target = "$SpeechTargetAnswer-${message.id}"
                            val spokenText = message.ttsText.ifBlank { answer.readableText() }
                            AssistantStructuredAnswerCard(
                                answer = answer,
                                onScenarioClick = { option ->
                                    speech.stop()
                                    chatViewModel.submitPrefilledQuestion(
                                        option.standardQuestion,
                                        inputType = "scenario",
                                        displayLanguage = displayLanguage,
                                        speechLanguage = speechLanguage,
                                        uiStrings = uiStrings
                                    )
                                },
                                onOpenMaterialList = onOpenMaterialList,
                                onReadAnswer = {
                                    speech.speak(
                                        spokenText,
                                        target,
                                        message.ttsAudioUrl,
                                        speechLanguage
                                    )
                                },
                                onStopReading = { speech.stop() },
                                onContinueClick = {
                                    Toast.makeText(context, context.getString(R.string.chat_continue_toast), Toast.LENGTH_SHORT).show()
                                },
                                isPreparing = speech.isPreparingTarget(target),
                                isSpeaking = speech.isSpeakingTarget(target)
                            )
                        } else if (message.text.isNotBlank()) {
                            val spokenText = message.ttsText.ifBlank { message.text }
                            AssistantAnswerCard(
                                title = stringResource(R.string.chat_answer_title),
                                body = message.text,
                                source = stringResource(R.string.chat_source_kb),
                                onOpenMaterialList = onOpenMaterialList,
                                onReadAnswer = {
                                    speech.speak(spokenText, "$SpeechTargetAnswer-${message.id}", message.ttsAudioUrl, speechLanguage)
                                },
                                onStopReading = { speech.stop() },
                                isPreparing = speech.isPreparingTarget("$SpeechTargetAnswer-${message.id}"),
                                isSpeaking = speech.isSpeakingTarget("$SpeechTargetAnswer-${message.id}")
                            )
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                QaLoadingCard()
            }

            if (uiState.isTranscribing) {
                QaLoadingCard(text = voicePanelMessage ?: stringResource(R.string.chat_transcribing))
            }

            if (uiState.lastVoiceSampleName.isNotBlank()) {
                VoiceSampleInfoCard(
                    name = uiState.lastVoiceSampleName,
                    sizeBytes = uiState.lastVoiceSampleSizeBytes
                )
            }

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                if (isVoiceNotClearMessage(message, uiStrings)) {
                    VoiceErrorCard(
                        message = uiStrings.voiceNotClearConfirm,
                        onRetry = {
                            chatViewModel.clearVoiceDraft()
                            showVoicePanel = true
                        },
                        onManualInput = {
                            chatViewModel.clearVoiceErrorForManualInput()
                        }
                    )
                } else {
                    QaErrorCard(
                        message = message,
                        onRetry = {
                            val retryQuestion = uiState.input.ifBlank { uiState.lastQuestion }
                            if (retryQuestion.isBlank()) {
                                Toast.makeText(context, context.getString(R.string.chat_retry_rephrase_toast), Toast.LENGTH_SHORT).show()
                            } else {
                                chatViewModel.submitPrefilledQuestion(
                                    retryQuestion,
                                    inputType = "retry",
                                    displayLanguage = displayLanguage,
                                    speechLanguage = speechLanguage,
                                    uiStrings = uiStrings
                                )
                            }
                        }
                    )
                }
            }

            uiState.voiceDraft?.let { draft ->
                val liveRecordingLabel = stringResource(R.string.voice_sample_live_recording)
                val isSampleDraft = uiState.lastVoiceSampleName.isNotBlank() && uiState.lastVoiceSampleName != liveRecordingLabel
                VoiceConfirmCard(
                    draft = draft,
                    retryText = if (isSampleDraft) stringResource(R.string.voice_retry_pick_sample) else stringResource(R.string.voice_retry_record),
                    onReadDraft = {
                        speech.speak(
                            draft,
                            SpeechTargetVoiceDraft,
                            null,
                            speechLanguage
                        )
                    },
                    onStopReading = { speech.stop() },
                    isPreparing = speech.isPreparingTarget(SpeechTargetVoiceDraft),
                    isSpeaking = speech.isSpeakingTarget(SpeechTargetVoiceDraft),
                    onConfirm = {
                        speech.stop()
                        chatViewModel.confirmVoiceDraft(
                            displayLanguage = displayLanguage,
                            speechLanguage = speechLanguage,
                            uiStrings = uiStrings
                        )
                    },
                    onRetry = {
                        speech.stop()
                        chatViewModel.clearVoiceDraft()
                        if (isSampleDraft) {
                            audioSampleLauncher.launch("audio/*")
                        } else {
                            showVoicePanel = true
                        }
                    },
                    onEdit = {
                        speech.stop()
                        chatViewModel.editVoiceDraft()
                    }
                )
            }

        }

        if (showVoicePanel) {
            Box(
                modifier = chatWidthModifier
                    .align(Alignment.CenterHorizontally)
                    .background(ElderBackground)
                    .padding(horizontal = responsive.pagePadding, vertical = responsive.smallSpacing)
            ) {
                VoiceInputPanel(
                    selectedLanguage = selectedVoiceLanguage,
                    onLanguageSelected = { selectedVoiceLanguage = it },
                    isRecording = isRecording,
                    enabled = !uiState.isLoading && !uiState.isTranscribing,
                    statusMessage = voicePanelMessage,
                    onStartRecording = { startRecording() },
                    onPickSample = { audioSampleLauncher.launch("audio/*") },
                    onStopRecording = { stopRecordingAndUpload() },
                    onCancel = { stopRecording() }
                )
            }
        }

        Box(modifier = chatWidthModifier.align(Alignment.CenterHorizontally)) {
            ChatInputBar(
                value = uiState.input,
                onValueChange = chatViewModel::updateInput,
                enabled = !uiState.isLoading && !uiState.isTranscribing,
                onVoice = { openVoicePanel() },
                onSend = {
                    chatViewModel.sendQuestion(
                        displayLanguage = displayLanguage,
                        speechLanguage = speechLanguage,
                        uiStrings = uiStrings
                    )
                }
            )
        }
    }
}

@Composable
private fun ServiceScreen(
    onOpenGuidance: () -> Unit,
    onOpenCrossBorderPreparePicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val toast = {
        Toast.makeText(context, "该功能暂未开放", Toast.LENGTH_SHORT).show()
    }
    val travelServices = listOf(
        ServiceItem("过关材料准备", "按当前情况选择材料清单", Icons.Filled.List, onOpenCrossBorderPreparePicker),
        ServiceItem("通关流程", "查看过关步骤", Icons.Filled.List),
        ServiceItem("预约停车", "提前安排停车", Icons.Filled.Place),
        ServiceItem("交通出行", "查询接驳与路线", Icons.Filled.Home),
        ServiceItem("特殊人群预约", "老人等关怀服务", Icons.Filled.Favorite),
        ServiceItem("志愿者呼叫", "一键寻求协助", Icons.Filled.Star),
        ServiceItem("客服电话", "快速联系人工", Icons.Filled.Call),
        ServiceItem("视频通关", "视频了解流程", Icons.Filled.PlayArrow)
    )
    val otherServices = listOf(
        ServiceItem("养老资源", "周边养老服务", Icons.Filled.Person),
        ServiceItem("医疗资源", "医院与便民医疗", Icons.Filled.Favorite),
        ServiceItem("旅游资源", "湾区出行与游玩", Icons.Filled.Place)
    )

    ScreenColumn(
        modifier = modifier,
        topBar = { UnifiedTopBar(title = stringResource(R.string.nav_service), gradient = true) }
    ) {
        SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFBFD8FF)) {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Search, size = responsive.iconLarge)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Text(
                            text = stringResource(R.string.service_guidance_title),
                            color = ElderText,
                            fontSize = responsive.cardTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.service_guidance_desc),
                            color = ElderTextMuted,
                            fontSize = responsive.body
                        )
                    }
                }
                PrimaryActionButton(
                    text = stringResource(R.string.service_guidance_action),
                    icon = Icons.Filled.KeyboardArrowRight,
                    onClick = onOpenGuidance,
                    height = 58.dp
                )
            }
        }
        SectionTitle(text = stringResource(R.string.service_departure))
        ServiceGrid(items = travelServices, onClick = toast)
        SectionTitle(text = stringResource(R.string.service_other))
        ServiceGrid(items = otherServices, onClick = toast)
        NoticeCard(text = stringResource(R.string.service_not_ready))
    }
}

@Composable
private fun CrossBorderPreparePickerScreen(
    onBack: () -> Unit,
    onOpenChecklist: (String) -> Unit,
    onOpenGuidance: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    val choices = listOf(
        ServiceItem(
            title = "已办好证件，只核对过关材料",
            subtitle = "查看通行证、签注、身份证等是否带齐。",
            icon = Icons.Filled.Check,
            onClick = { onOpenChecklist("border_crossing_prepare") }
        ),
        ServiceItem(
            title = "还没有港澳通行证",
            subtitle = "查看首次办理港澳通行证需要准备的材料。",
            icon = Icons.Filled.AccountCircle,
            onClick = { onOpenChecklist("hk_macau_pass_apply") }
        ),
        ServiceItem(
            title = "有通行证，但签注不确定",
            subtitle = "查看续签或签注核对材料。",
            icon = Icons.Filled.Refresh,
            onClick = { onOpenChecklist("hk_macau_renewal") }
        ),
        ServiceItem(
            title = "我不清楚该办哪种",
            subtitle = "回答几个问题，先判断自己该办哪一种。",
            icon = Icons.Filled.Search,
            onClick = onOpenGuidance
        )
    )

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = "过关材料准备",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        }
    ) {
        SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFBFD8FF)) {
            Row(
                modifier = Modifier.padding(responsive.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.List, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                    Text(
                        text = "请选择您现在的情况",
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "我会帮您打开对应的材料清单。不确定时，可以先选择“我不清楚该办哪种”。",
                        color = ElderTextMuted,
                        fontSize = responsive.body
                    )
                }
            }
        }

        choices.forEach { choice ->
            ActionCard(
                title = choice.title,
                subtitle = choice.subtitle,
                icon = choice.icon,
                onClick = choice.onClick ?: {},
                layoutMode = CardLayoutMode.List
            )
        }
    }
}

@Composable
private fun GuidanceScreen(
    chatViewModel: ChatViewModel,
    speechLanguage: String,
    onBack: () -> Unit,
    onOpenMaterialList: (String) -> Unit,
    onOpenDetailedPolicy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val speech = rememberCloudSpeechController(chatViewModel, speechLanguage)
    var profile by remember { mutableStateOf(GuidanceProfile()) }
    var result by remember { mutableStateOf<GuidanceResult?>(null) }
    val visibleQuestions = profile.visibleGuidanceQuestions()
    val readAllText = buildString {
        append("办理情况问卷。请按当前页面的问题回答，我们帮您判断该怎么办。")
        visibleQuestions.forEachIndexed { index, question ->
            append("第${index + 1}题。${question.title}。")
            append("选项有：${question.options.joinToString("，")}。")
        }
    }
    val readAllActive = speech.isActiveTarget(SpeechTargetGuidance)

    ScreenColumn(
        modifier = modifier,
        topBar = {
            UnifiedTopBar(
                title = "办理情况判断",
                showBack = true,
                onBack = {
                    speech.stop()
                    onBack()
                },
                leadingIcon = Icons.Filled.ArrowBack,
                elevated = true
            )
        }
    ) {
        SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFBFD8FF)) {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Search, size = responsive.iconMedium)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Text(
                            text = "按步骤回答几个问题",
                            color = ElderText,
                            fontSize = responsive.cardTitle,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "后面的问题会根据您的选择自动出现",
                            color = ElderTextMuted,
                            fontSize = responsive.body
                        )
                    }
                }
                SecondaryActionButton(
                    text = if (readAllActive) "停止朗读" else "朗读全部",
                    icon = if (readAllActive) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                    onClick = {
                        if (readAllActive) {
                            speech.stop()
                        } else {
                            speech.speak(readAllText, SpeechTargetGuidance, null, speechLanguage)
                        }
                    },
                    height = 52.dp
                )
            }
        }

        visibleQuestions.forEachIndexed { index, question ->
            val target = "$SpeechTargetGuidance-$index"
            GuidanceQuestionCard(
                index = index + 1,
                question = question,
                selected = profile.answerFor(question.field),
                isReading = speech.isActiveTarget(target),
                onRead = {
                    if (speech.isActiveTarget(target)) {
                        speech.stop()
                    } else {
                        val text = "第${index + 1}题。${question.title}。选项有：${question.options.joinToString("，")}。"
                        speech.speak(text, target, null, speechLanguage)
                    }
                },
                onSelected = { answer ->
                    profile = profile.withAnswer(question.field, answer)
                    result = null
                }
            )
        }

        PrimaryActionButton(
            text = "看看我该怎么办",
            icon = Icons.Filled.Check,
            onClick = {
                val missingCount = profile.missingCount(visibleQuestions)
                if (missingCount > 0) {
                    Toast.makeText(context, "还有 $missingCount 个问题没选", Toast.LENGTH_SHORT).show()
                } else {
                    result = buildGuidanceResult(profile)
                }
            },
            height = 60.dp
        )

        result?.let { guidanceResult ->
            GuidanceResultCard(
                result = guidanceResult,
                onOpenDetailedPolicy = { onOpenDetailedPolicy(guidanceResult.standardQuestion) },
                onRestart = {
                    speech.stop()
                    profile = GuidanceProfile()
                    result = null
                },
                onOpenMaterialList = { onOpenMaterialList(guidanceResult.checklistId) }
            )
        }
    }
}

@Composable
private fun GuidanceQuestionCard(
    index: Int,
    question: GuidanceQuestion,
    selected: String,
    isReading: Boolean,
    onRead: () -> Unit,
    onSelected: (String) -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(ElderBlue, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(index.toString(), color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = question.title,
                    color = ElderText,
                    fontSize = responsive.cardTitle,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                GuidanceSpeechButton(
                    active = isReading,
                    onClick = onRead
                )
            }

            val optionColumns = if (responsive.useSingleColumnCards) 1 else 2
            question.options.chunked(optionColumns).forEach { rowOptions ->
                Row(horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)) {
                    rowOptions.forEach { option ->
                        GuidanceOptionButton(
                            text = option,
                            selected = option == selected,
                            onClick = { onSelected(option) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (optionColumns > 1 && rowOptions.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GuidanceSpeechButton(
    active: Boolean,
    onClick: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = Modifier
            .heightIn(min = responsive.compactButtonMinHeight)
            .background(Color.White, RoundedCornerShape(22.dp))
            .border(1.5.dp, ElderBlue, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = responsive.rowSpacing),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
            Icon(
                imageVector = if (active) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                contentDescription = if (active) "停止朗读" else "朗读题目",
                tint = ElderBlue,
                modifier = Modifier.size(responsive.iconSmall)
            )
            Text(
                text = if (active) "停止" else "朗读",
                color = ElderBlue,
                fontSize = responsive.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun GuidanceOptionButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = modifier
            .heightIn(min = responsive.buttonMinHeight)
            .background(if (selected) ElderBlue else ElderBluePale, RoundedCornerShape(14.dp))
            .border(1.5.dp, if (selected) ElderBlue else ElderLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = responsive.rowSpacing, vertical = responsive.smallSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else ElderText,
            fontSize = responsive.body,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun GuidanceResultCard(
    result: GuidanceResult,
    onOpenDetailedPolicy: () -> Unit,
    onRestart: () -> Unit,
    onOpenMaterialList: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = ElderGreenSoft, borderColor = Color(0xFFBFE6CA)) {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Check, tint = ElderGreen, background = Color.White, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                    Text(
                        text = "您的情况可能是",
                        color = ElderTextMuted,
                        fontSize = responsive.label
                    )
                    Text(
                        text = result.caseType,
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            GuidanceTextBlock(title = "推荐下一步", body = result.recommendedAction)
            GuidanceTextBlock(title = "给工作人员看的说明", body = result.windowScript)
            GuidanceNoticeBlock(text = result.notice)

            PrimaryActionButton(
                text = "查看详细政策",
                icon = Icons.Filled.Search,
                onClick = onOpenDetailedPolicy,
                height = 58.dp
            )
            AdaptivePairRow(
                first = { itemModifier ->
                    SecondaryActionButton(
                        text = "重新判断",
                        icon = Icons.Filled.Refresh,
                        onClick = onRestart,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                },
                second = { itemModifier ->
                    SecondaryActionButton(
                        text = "查看材料清单",
                        icon = Icons.Filled.List,
                        onClick = onOpenMaterialList,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                }
            )
        }
    }
}

@Composable
private fun GuidanceNoticeBlock(text: String) {
    val responsive = LocalElderResponsive.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, Color(0xFFBFE6CA), RoundedCornerShape(14.dp))
            .padding(responsive.cardSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = ElderGreen, modifier = Modifier.size(responsive.iconSmall))
        Text(text = text, color = ElderText, fontSize = responsive.label, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun GuidanceTextBlock(
    title: String,
    body: String
) {
    val responsive = LocalElderResponsive.current
    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        Text(
            text = title,
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(14.dp))
                .border(1.dp, ElderLine, RoundedCornerShape(14.dp))
                .padding(responsive.cardSpacing)
        ) {
            Text(
                text = body,
                color = ElderText,
                fontSize = responsive.body
            )
        }
    }
}

@Composable
private fun MyScreen(
    onOpenSavedChecklist: (String) -> Unit,
    materialViewModel: MaterialViewModel,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    val materialUiState by materialViewModel.uiState.collectAsState()

    ScreenColumn(
        modifier = modifier,
        topBar = { UnifiedTopBar(title = "我的", gradient = true) }
    ) {
        SoftCard {
            Row(
                modifier = Modifier.padding(responsive.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.AccountCircle, size = responsive.iconLarge)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "您好，欢迎使用粤同心",
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(responsive.smallSpacing))
                    Text(
                        text = "为您保存材料清单和办事草稿",
                        color = ElderTextMuted,
                        fontSize = responsive.body
                    )
                }
            }
        }

        SavedMaterialsSection(
            savedChecklists = materialUiState.savedChecklists,
            onChecklistClick = onOpenSavedChecklist
        )
    }
}

@Composable
private fun PortDetailScreen(
    onBack: () -> Unit,
    onAskAi: () -> Unit,
    onOpenMaterialList: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val relatedServices = listOf(
        ServiceItem("问 AI", "询问通关问题", Icons.Filled.Email, onAskAi),
        ServiceItem("查看材料", "核对必备证件", Icons.Filled.List, onOpenMaterialList),
        ServiceItem(
            "导航",
            "查看路线",
            Icons.Filled.Place
        ) { Toast.makeText(context, "导航功能暂未开放", Toast.LENGTH_SHORT).show() },
        ServiceItem(
            "找家人帮忙",
            "请家属协助",
            Icons.Filled.Person
        ) { Toast.makeText(context, "家属协助暂未开放", Toast.LENGTH_SHORT).show() }
    )

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = "口岸详情",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        }
    ) {
        SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Place, size = responsive.iconLarge)
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
                        ) {
                            Text(
                                text = "深圳湾口岸",
                                color = ElderText,
                                fontSize = responsive.sectionTitle,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(text = "正常", color = ElderGreen, background = ElderGreenSoft)
                        }
                        Text(
                            text = "演示数据，仅供办事前参考",
                            color = ElderTextMuted,
                            fontSize = responsive.label
                        )
                    }
                }
                InfoRow("开放时间", "6:30 - 24:00", Icons.Filled.DateRange)
                InfoRow("预计等待", "约 15 分钟", Icons.Filled.Person, valueColor = ElderGreen)
                InfoRow("更新时间", "今天 09:30", Icons.Filled.Refresh)
            }
        }

        SoftCard(containerColor = ElderOrangeSoft, borderColor = Color(0xFFFFCC8F)) {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                SectionTitle(text = "重要提醒", icon = Icons.Filled.Warning)
                ReminderRow("请确认港澳通行证和有效签注")
                ReminderRow("建议提前准备身份证件")
                ReminderRow("高峰时段可能排队，请预留时间")
            }
        }

        SoftCard {
            Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
                SectionTitle(text = "过关步骤")
                TimelineRow(1, "到达口岸")
                TimelineRow(2, "准备证件")
                TimelineRow(3, "通过边检")
                TimelineRow(4, "前往香港侧交通接驳")
            }
        }

        SectionTitle(text = "相关服务")
        ServiceGrid(items = relatedServices, onClick = {})

        NoticeCard(text = "状态为演示数据，请以现场公告为准。")
    }
}

@Composable
private fun GuideScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    chatViewModel: ChatViewModel,
    speechLanguage: String,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    val speech = rememberCloudSpeechController(chatViewModel, speechLanguage)
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = listOf(
        GuideStep(Icons.Filled.Email, "第 1 步：点击提问", "在首页点击“点击提问”，进入智能问答页面。"),
        GuideStep(Icons.Filled.Edit, "第 2 步：说出或输入问题", "您可以直接输入文字，也可以点击麦克风说话。"),
        GuideStep(Icons.Filled.Check, "第 3 步：确认识别文字", "如果使用语音提问，系统会先显示听到的文字，请确认无误后继续。"),
        GuideStep(Icons.Filled.Search, "第 4 步：查看 AI 回答", "系统会根据政策知识库整理回答，并尽量用简单的话说明。"),
        GuideStep(Icons.Filled.List, "第 5 步：保存或继续办理", "您可以查看材料清单、朗读回答，或请家人帮忙确认。")
    )
    val current = steps[stepIndex]
    val guideText = "${current.title}。${current.body}"
    val guideIsPreparing = speech.isPreparingTarget(SpeechTargetGuide)
    val guideIsSpeaking = speech.isSpeakingTarget(SpeechTargetGuide)
    val guideIsActive = speech.isActiveTarget(SpeechTargetGuide)

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = "操作指南",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = {
                    speech.stop()
                    onBack()
                },
                elevated = true
            )
        }
    ) {
        SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
            Column(
                modifier = Modifier.padding(responsive.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
            ) {
                IconBadge(icon = current.icon, size = responsive.guideHeroIconSize)
                Text(
                    text = current.title,
                    color = ElderText,
                    fontSize = responsive.sectionTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = current.body,
                    color = ElderTextMuted,
                    fontSize = responsive.bodyLarge,
                    textAlign = TextAlign.Center
                )
                SecondaryActionButton(
                    text = when {
                        guideIsPreparing -> "准备朗读..."
                        guideIsSpeaking -> "停止朗读"
                        else -> "朗读本步"
                    },
                    icon = if (guideIsActive) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    onClick = {
                        if (guideIsActive) {
                            speech.stop()
                        } else {
                            speech.speak(guideText, SpeechTargetGuide, null, speechLanguage)
                        }
                    },
                    height = 54.dp
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                steps.indices.forEach { index ->
                    ProgressDot(active = index == stepIndex)
                }
            }
        }
        Text(
            text = "${stepIndex + 1} / ${steps.size}",
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        AdaptivePairRow(
            first = { itemModifier ->
                SecondaryActionButton(
                    text = "上一步",
                    icon = Icons.Filled.ArrowBack,
                    onClick = { if (stepIndex > 0) stepIndex-- },
                    modifier = itemModifier,
                    height = 48.dp
                )
            },
            second = { itemModifier ->
                PrimaryActionButton(
                    text = if (stepIndex == steps.lastIndex) "完成" else "下一步",
                    icon = if (stepIndex == steps.lastIndex) Icons.Filled.Check else Icons.Filled.KeyboardArrowRight,
                    onClick = {
                        if (stepIndex == steps.lastIndex) {
                            speech.stop()
                            onDone()
                        } else {
                            speech.stop()
                            stepIndex++
                        }
                    },
                    modifier = itemModifier,
                    height = 48.dp
                )
            }
        )
        SecondaryActionButton(
            text = "关闭",
            icon = Icons.Filled.Close,
            onClick = {
                speech.stop()
                onDone()
            }
        )
    }
}

@Composable
private fun FontSizeScreen(
    selected: FontChoice,
    onSelected: (FontChoice) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = "字体设置",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        }
    ) {
        Text(
            text = "请选择适合您的字体大小",
            color = ElderTextMuted,
            fontSize = responsive.bodyLarge
        )
        val columns = if (responsive.useSingleColumnCards) 1 else 2
        fontChoices.chunked(columns).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)) {
                rowItems.forEach { choice ->
                    FontChoiceCard(
                        choice = choice,
                        selected = choice == selected,
                        onClick = { onSelected(choice) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (columns > 1 && rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        NoticeCard(text = "返回后，页面文字会保持您选择的大小。")
    }
}

@Composable
private fun MaterialListScreen(
    onBack: () -> Unit,
    activeChecklistId: String?,
    materialViewModel: MaterialViewModel,
    onChecklistBack: () -> Unit,
    onOpenChecklist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by materialViewModel.uiState.collectAsState()

    if (activeChecklistId != null) {
        MaterialChecklistScreen(
            checklistId = activeChecklistId,
            materialViewModel = materialViewModel,
            onBack = onChecklistBack,
            onOpenChecklist = onOpenChecklist,
            modifier = modifier
        )
        return
    }

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = stringResource(R.string.common_material_list),
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        }
    ) {
        if (uiState.isLoading && uiState.items.isEmpty()) {
            LoadingCard(text = stringResource(R.string.common_loading_materials))
        }

        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            MaterialErrorCard(
                message = message,
                onRetry = { materialViewModel.loadItems() }
            )
        }

        SectionTitle(text = stringResource(R.string.material_choose_item))
        uiState.items.forEach { item ->
            ActionCard(
                title = item.title,
                subtitle = item.subtitle,
                icon = when (item.code) {
                    "hk_macau_pass_apply" -> Icons.Filled.AccountCircle
                    "hk_macau_renewal" -> Icons.Filled.Refresh
                    "border_crossing_prepare" -> Icons.Filled.Place
                    else -> Icons.Filled.List
                },
                onClick = { onOpenChecklist(item.code) }
            )
        }
        NoticeCard(text = stringResource(R.string.material_mock_notice))
    }
}

@Composable
private fun MaterialChecklistScreen(
    checklistId: String,
    materialViewModel: MaterialViewModel,
    onBack: () -> Unit,
    onOpenChecklist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val uiState by materialViewModel.uiState.collectAsState()
    val selectedChecklist = uiState.selectedChecklist

    LaunchedEffect(checklistId) {
        materialViewModel.selectItem(checklistId)
    }

    val currentChecklist = selectedChecklist?.takeIf { it.code == checklistId }
    val isSaved = uiState.savedChecklists.any { it.itemCode == checklistId }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(ElderBackground),
        containerColor = ElderBackground,
        topBar = {
            UnifiedTopBar(
                title = currentChecklist?.title ?: stringResource(R.string.common_material_list),
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(1.dp, ElderLine)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = responsive.pagePadding, vertical = responsive.rowSpacing)
            ) {
                PrimaryActionButton(
                    text = if (isSaved) stringResource(R.string.material_update) else stringResource(R.string.material_save),
                    icon = Icons.Filled.Check,
                    onClick = {
                        materialViewModel.saveSelectedChecklist()
                        Toast.makeText(context, context.getString(R.string.material_saved_toast), Toast.LENGTH_SHORT).show()
                    },
                    enabled = currentChecklist != null && !uiState.isLoading,
                    height = 58.dp
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            val contentModifier = if (responsive.windowSizeClass == ElderWindowSizeClass.Compact) {
                Modifier.fillMaxWidth()
            } else {
                Modifier
                    .fillMaxWidth()
                    .widthIn(max = responsive.contentMaxWidth)
            }

            Column(
                modifier = contentModifier
                    .align(Alignment.CenterHorizontally)
                    .padding(responsive.pagePadding),
                verticalArrangement = Arrangement.spacedBy(responsive.pageSpacing)
            ) {
                when {
                    uiState.isLoading && currentChecklist == null -> LoadingCard(text = stringResource(R.string.common_loading_materials))
                    uiState.errorMessage != null && currentChecklist == null -> MaterialErrorCard(
                        message = uiState.errorMessage.orEmpty(),
                        onRetry = { materialViewModel.selectItem(checklistId) }
                    )

                    currentChecklist != null -> MaterialChecklistDetail(
                        checklist = currentChecklist,
                        checkedIds = uiState.checkedRequirementIds,
                        saveMessage = uiState.saveMessage,
                        onToggle = materialViewModel::toggleRequirement,
                        onOpenLinkedChecklist = onOpenChecklist
                    )

                    else -> MaterialErrorCard(
                        message = stringResource(R.string.material_not_found),
                        onRetry = { materialViewModel.selectItem(checklistId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SavedMaterialsSection(
    savedChecklists: List<SavedMaterialChecklist>,
    onChecklistClick: (String) -> Unit
) {
    val responsive = LocalElderResponsive.current
    SectionTitle(text = stringResource(R.string.home_saved_materials), icon = Icons.Filled.Check)
    if (savedChecklists.isEmpty()) {
        NoticeCard(text = stringResource(R.string.material_empty_saved))
        return
    }

    savedChecklists.forEach { item ->
        SoftCard(modifier = Modifier.clickable { onChecklistClick(item.itemCode) }) {
            Row(
                modifier = Modifier.padding(responsive.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.List, tint = ElderGreen, background = ElderGreenSoft, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                    Text(
                        text = item.title,
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "已核对 ${item.checkedCount} / ${item.totalCount} 项",
                        color = ElderTextMuted,
                        fontSize = responsive.label
                    )
                }
                StatusPill(text = stringResource(R.string.material_continue_check), color = ElderGreen, background = ElderGreenSoft)
            }
        }
    }
}

@Composable
private fun MaterialErrorCard(message: String, onRetry: () -> Unit) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFFFC9C2)) {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Warning, tint = ElderRed, background = Color(0xFFFFE8E5), size = responsive.iconMedium)
                Text(
                    text = message,
                    color = ElderRed,
                    fontSize = responsive.body,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            SecondaryActionButton(stringResource(R.string.common_retry_reload), Icons.Filled.Refresh, onRetry, height = 52.dp)
        }
    }
}

@Composable
private fun MaterialChecklistDetail(
    checklist: MaterialChecklist,
    checkedIds: Set<String>,
    saveMessage: String?,
    onToggle: (String) -> Unit,
    onOpenLinkedChecklist: (String) -> Unit
) {
    val responsive = LocalElderResponsive.current
    SectionTitle(text = stringResource(R.string.material_reminder), icon = Icons.Filled.Info)
    NoticeCard(text = checklist.tips.joinToString("\n"))

    SectionTitle(text = stringResource(R.string.material_check), icon = Icons.Filled.List)
    Text(
        text = "已核对 ${checkedIds.size} / ${checklist.requirements.size} 项",
        color = ElderBlueDark,
        fontSize = responsive.bodyLarge,
        fontWeight = FontWeight.Bold
    )
    checklist.requirements.forEach { requirement ->
        RequirementCheckRow(
            name = requirement.name,
            description = requirement.description,
            note = requirement.note,
            required = requirement.required,
            checked = requirement.id in checkedIds,
            linkedChecklistId = requirement.linkedChecklistId,
            linkedActionLabel = requirement.linkedActionLabel,
            onOpenLinkedChecklist = onOpenLinkedChecklist,
            onToggle = { onToggle(requirement.id) }
        )
    }

    saveMessage?.takeIf { it.isNotBlank() }?.let { message ->
        SoftCard(containerColor = ElderGreenSoft, borderColor = Color(0xFFBFE6CA), elevation = 0.dp) {
            Row(
                modifier = Modifier.padding(responsive.cardSpacing),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = ElderGreen, modifier = Modifier.size(responsive.iconSmall))
                Text(text = message, color = ElderGreen, fontSize = responsive.body, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RequirementCheckRow(
    name: String,
    description: String,
    note: String?,
    required: Boolean,
    checked: Boolean,
    linkedChecklistId: String?,
    linkedActionLabel: String?,
    onOpenLinkedChecklist: (String) -> Unit,
    onToggle: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard(elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(responsive.cardSpacing),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = checked,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ElderBlue,
                        uncheckedColor = ElderTextMuted,
                        checkmarkColor = Color.White
                    )
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                    Text(
                        text = name,
                        color = ElderText,
                        fontSize = responsive.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (required) "必带" else "可选",
                        color = if (required) ElderOrange else ElderGreen,
                        background = if (required) ElderOrangeSoft else ElderGreenSoft
                    )
                }
                Text(text = description, color = ElderTextMuted, fontSize = responsive.label)
                note?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = ElderBlueDark, fontSize = responsive.labelSmall)
                }
                if (!linkedChecklistId.isNullOrBlank() && !linkedActionLabel.isNullOrBlank()) {
                    SecondaryActionButton(
                        text = linkedActionLabel,
                        icon = Icons.Filled.KeyboardArrowRight,
                        onClick = { onOpenLinkedChecklist(linkedChecklistId) },
                        height = 48.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenColumn(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val responsive = LocalElderResponsive.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElderBackground)
            .verticalScroll(rememberScrollState())
    ) {
        topBar()
        val contentModifier = if (responsive.windowSizeClass == ElderWindowSizeClass.Compact) {
            Modifier.fillMaxWidth()
        } else {
            Modifier
                .fillMaxWidth()
                .widthIn(max = responsive.contentMaxWidth)
        }
        Column(
            modifier = contentModifier
                .align(Alignment.CenterHorizontally)
                .padding(responsive.pagePadding),
            verticalArrangement = Arrangement.spacedBy(responsive.pageSpacing),
            content = content
        )
    }
}

@Composable
private fun AdaptivePairRow(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    if (responsive.stackActionRows) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            first(Modifier.fillMaxWidth())
            second(Modifier.fillMaxWidth())
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            first(Modifier.weight(1f))
            second(Modifier.weight(1f))
        }
    }
}

@Composable
private fun PortSummaryCard(port: PortInfo, onClick: () -> Unit) {
    val responsive = LocalElderResponsive.current
    SoftCard(modifier = Modifier.clickable(onClick = onClick)) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = port.icon, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
                        Text(
                            port.name,
                            color = ElderText,
                            fontSize = responsive.cardTitle,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        StatusPill(text = "正常", color = ElderGreen, background = ElderGreenSoft)
                    }
                    InfoLine("开放：${port.openTime}")
                    InfoLine("预计等待：${port.waitTime}", valueColor = ElderGreen)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                SecondaryMiniButton(text = "查看详情", onClick = onClick)
            }
        }
    }
}

@Composable
private fun FaqSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onQuestionClick: (String) -> Unit
) {
    val responsive = LocalElderResponsive.current
    var isExpanded by remember(selectedCategory) { mutableStateOf(false) }
    val filteredItems = faqItemsForCategory(selectedCategory)
    val visibleItems = if (isExpanded) filteredItems else filteredItems.take(3)
    val hasMoreItems = filteredItems.size > 3

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        faqCategories.forEach { category ->
            FilterChipLike(
                text = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        visibleItems.forEach { item ->
            SoftCard(modifier = Modifier.clickable { onQuestionClick(item.question) }, elevation = 0.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = responsive.cardSpacing, vertical = responsive.rowSpacing),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
                ) {
                    IconBadge(icon = Icons.Filled.Info, size = responsive.iconSmall + 18.dp)
                    Text(
                        text = item.question,
                        color = ElderText,
                        fontSize = responsive.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ElderTextMuted, modifier = Modifier.size(responsive.iconSmall))
                }
            }
        }
    }
    if (hasMoreItems) {
        SecondaryActionButton(
            text = if (isExpanded) "收起" else "展开更多",
            icon = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
            onClick = { isExpanded = !isExpanded },
            height = 52.dp
        )
    }
}

@Composable
private fun QaQuickQuestionChips(
    items: List<QaQuickQuestion>,
    onClick: (QaQuickQuestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, ElderLine)
            .padding(horizontal = responsive.pagePadding, vertical = responsive.smallSpacing),
        verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
    ) {
        Text(
            text = stringResource(R.string.qa_common_questions),
            color = ElderText,
            fontSize = responsive.label,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                QaChip(
                    text = stringResource(item.labelResId),
                    onClick = { onClick(item) }
                )
            }
        }
    }
}

@Composable
private fun QaChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 88.dp)
            .background(ElderBlueSoft, RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFBFD8FF), RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = responsive.cardSpacing, vertical = responsive.smallSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = ElderBlueDark,
            fontSize = responsive.body,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QaEmptyState(
    examples: List<QaEmptyExample>,
    onExampleClick: (String) -> Unit,
    onGuidanceClick: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFBFD8FF)) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Search, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.chat_empty_title),
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.chat_empty_hint),
                        color = ElderTextMuted,
                        fontSize = responsive.body
                    )
                }
            }

            examples.forEach { example ->
                SecondaryActionButton(
                    text = stringResource(example.labelResId),
                    icon = Icons.Filled.KeyboardArrowRight,
                    onClick = { onExampleClick(example.question) },
                    height = 52.dp
                )
            }

            Text(
                text = stringResource(R.string.chat_unsure_prompt),
                color = ElderTextMuted,
                fontSize = responsive.label,
                fontWeight = FontWeight.Bold
            )
            PrimaryActionButton(
                text = stringResource(R.string.chat_unsure_action),
                icon = Icons.Filled.Search,
                onClick = onGuidanceClick,
                height = 56.dp
            )
        }
    }
}

@Composable
private fun AssistantStructuredAnswerCard(
    answer: QaAnswerUiModel,
    onScenarioClick: (QaScenarioOption) -> Unit,
    onOpenMaterialList: () -> Unit,
    onReadAnswer: () -> Unit,
    onStopReading: () -> Unit,
    onContinueClick: () -> Unit,
    isPreparing: Boolean,
    isSpeaking: Boolean
) {
    val responsive = LocalElderResponsive.current
    val details = answer.originalAnswer.orEmpty()
    SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.AccountCircle, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = answer.title,
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = answer.subtitle,
                        color = ElderTextMuted,
                        fontSize = responsive.label
                    )
                }
            }

            QaConclusionBox(answer.conclusion)
            QaScenarioChips(answer.scenarioOptions, onScenarioClick)

            if (answer.steps.isNotEmpty()) {
                QaInfoSection(
                    title = stringResource(R.string.qa_how_to),
                    items = answer.steps,
                    numbered = true
                )
            } else {
                QaInfoSection(
                    title = stringResource(R.string.qa_details),
                    items = listOf(details.ifBlank { answer.conclusion }),
                    numbered = false
                )
            }

            QaMaterialSummarySection(
                requiredMaterials = answer.requiredMaterials,
                optionalMaterials = answer.optionalMaterials
            )
            QaInfoSection(
                title = stringResource(R.string.qa_warnings),
                items = answer.warnings,
                numbered = false
            )
            if (details.isNotBlank() && details != answer.conclusion) {
                QaDetailSection(details)
            }
            QaSourceNotice(answer.sourceTitle ?: stringResource(R.string.chat_source_kb))
            QaAnswerActions(
                isPreparing = isPreparing,
                isSpeaking = isSpeaking,
                onOpenMaterialList = onOpenMaterialList,
                onReadAnswer = onReadAnswer,
                onStopReading = onStopReading,
                onContinueClick = onContinueClick
            )
        }
    }
}

@Composable
private fun QaConclusionBox(conclusion: String) {
    val responsive = LocalElderResponsive.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFBFD8FF), RoundedCornerShape(16.dp))
            .padding(responsive.cardSpacing),
        verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
    ) {
        Text(
            text = stringResource(R.string.qa_conclusion),
            color = ElderBlueDark,
            fontSize = responsive.label,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = conclusion,
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun QaScenarioChips(
    options: List<QaScenarioOption>,
    onScenarioClick: (QaScenarioOption) -> Unit
) {
    val responsive = LocalElderResponsive.current
    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        Text(
            text = stringResource(R.string.qa_scenario_question),
            color = ElderText,
            fontSize = responsive.label,
            fontWeight = FontWeight.Bold
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
        ) {
            options.forEach { option ->
                QaChip(
                    text = option.label,
                    onClick = { onScenarioClick(option) }
                )
            }
        }
    }
}

@Composable
private fun QaInfoSection(
    title: String,
    items: List<String>,
    numbered: Boolean
) {
    val responsive = LocalElderResponsive.current
    val visibleItems = items.filter { it.isNotBlank() }
    if (visibleItems.isEmpty()) return

    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        Text(
            text = title,
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        visibleItems.forEachIndexed { index, item ->
            Text(
                text = if (numbered) "${index + 1}. $item" else "- $item",
                color = Color(0xFF25334A),
                fontSize = responsive.body
            )
        }
    }
}

@Composable
private fun QaMaterialSummarySection(
    requiredMaterials: List<String>,
    optionalMaterials: List<String>
) {
    val responsive = LocalElderResponsive.current
    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        Text(
            text = stringResource(R.string.qa_materials),
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        if (requiredMaterials.isEmpty() && optionalMaterials.isEmpty()) {
            Text(
                text = stringResource(R.string.qa_no_materials),
                color = ElderTextMuted,
                fontSize = responsive.body
            )
        } else {
            if (requiredMaterials.isNotEmpty()) {
                Text(stringResource(R.string.qa_required_materials), color = ElderTextMuted, fontSize = responsive.label, fontWeight = FontWeight.Bold)
                requiredMaterials.forEach { item ->
                    Text(text = "- $item", color = Color(0xFF25334A), fontSize = responsive.body)
                }
            }
            if (optionalMaterials.isNotEmpty()) {
                Text(stringResource(R.string.qa_optional_materials), color = ElderTextMuted, fontSize = responsive.label, fontWeight = FontWeight.Bold)
                optionalMaterials.forEach { item ->
                    Text(text = "- $item", color = Color(0xFF25334A), fontSize = responsive.body)
                }
            }
        }
    }
}

@Composable
private fun QaDetailSection(detailText: String) {
    val responsive = LocalElderResponsive.current
    var expanded by remember(detailText) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
        Text(
            text = stringResource(R.string.qa_details),
            color = ElderText,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = detailText,
            color = Color(0xFF25334A),
            fontSize = responsive.body,
            maxLines = if (expanded) Int.MAX_VALUE else 6,
            overflow = TextOverflow.Ellipsis
        )
        if (detailText.length > 90) {
            Text(
                text = if (expanded) stringResource(R.string.qa_collapse) else stringResource(R.string.qa_expand),
                color = ElderBlue,
                fontSize = responsive.label,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun QaSourceNotice(sourceTitle: String) {
    val responsive = LocalElderResponsive.current
    Text(
        text = stringResource(R.string.qa_source_prefix, sourceTitle),
        color = ElderTextMuted,
        fontSize = responsive.labelSmall
    )
}

@Composable
private fun QaAnswerActions(
    isPreparing: Boolean,
    isSpeaking: Boolean,
    onOpenMaterialList: () -> Unit,
    onReadAnswer: () -> Unit,
    onStopReading: () -> Unit,
    onContinueClick: () -> Unit
) {
    val isReadingActive = isPreparing || isSpeaking
    PrimaryActionButton(
        text = stringResource(R.string.qa_open_materials),
        icon = Icons.Filled.List,
        onClick = onOpenMaterialList,
        height = 56.dp
    )
    AdaptivePairRow(
        first = { itemModifier ->
            SecondaryActionButton(
                text = when {
                    isPreparing -> stringResource(R.string.qa_read_prepare)
                    isSpeaking -> stringResource(R.string.qa_read_stop)
                    else -> stringResource(R.string.qa_read_answer)
                },
                icon = if (isReadingActive) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                onClick = if (isReadingActive) onStopReading else onReadAnswer,
                modifier = itemModifier,
                height = 52.dp
            )
        },
        second = { itemModifier ->
            SecondaryActionButton(
                text = stringResource(R.string.qa_continue),
                icon = Icons.Filled.Refresh,
                onClick = onContinueClick,
                modifier = itemModifier,
                height = 52.dp
            )
        }
    )
}

@Composable
private fun QaLoadingCard(text: String? = null) {
    LoadingCard(text = text ?: stringResource(R.string.chat_loading))
}

@Composable
private fun QaErrorCard(message: String, onRetry: () -> Unit) {
    val responsive = LocalElderResponsive.current
    val noKnowledgeAnswer = stringResource(R.string.chat_error_no_knowledge)
    val retryText = if (message == noKnowledgeAnswer) {
        stringResource(R.string.chat_retry_rephrase)
    } else {
        stringResource(R.string.chat_retry_query)
    }
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFFFC9C2)) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Warning, tint = ElderRed, background = Color(0xFFFFE8E5), size = responsive.iconMedium)
                Text(
                    text = message,
                    color = ElderRed,
                    fontSize = responsive.body,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            SecondaryActionButton(
                text = retryText,
                icon = Icons.Filled.Refresh,
                onClick = onRetry,
                height = 52.dp
            )
        }
    }
}

private fun QaAnswerUiModel.readableText(): String {
    return buildString {
        append("结论。")
        append(conclusion)
        if (steps.isNotEmpty()) {
            append(" 怎么办。")
            append(steps.joinToString("。"))
        }
        if (!originalAnswer.isNullOrBlank()) {
            append(" 详细说明。")
            append(originalAnswer.orEmpty())
        }
        if (requiredMaterials.isNotEmpty() || optionalMaterials.isNotEmpty()) {
            append(" 需要什么材料。")
            append((requiredMaterials + optionalMaterials).joinToString("。"))
        }
        if (warnings.isNotEmpty()) {
            append(" 注意事项。")
            append(warnings.joinToString("。"))
        }
    }
}

private fun safeSpeechLanguageForAdHocText(text: String, requestedLanguage: String, audioUrl: String?): String {
    if (!audioUrl.isNullOrBlank()) return requestedLanguage
    if (requestedLanguage != "yue") return requestedLanguage
    return if (looksLikeLongMandarinWrittenText(text)) "zh-CN" else requestedLanguage
}

private fun looksLikeLongMandarinWrittenText(text: String): Boolean {
    val normalized = text.replace(Regex("""\s+"""), "")
    if (normalized.length < 80) return false
    val hasChinese = normalized.any { it in '\u4e00'..'\u9fff' }
    if (!hasChinese) return false
    val cantoneseMarkers = listOf("点搞", "點搞", "唔", "咁", "嘅", "啲", "冇", "使唔使", "边度", "邊度")
    return cantoneseMarkers.none { normalized.contains(it) }
}

@Composable
private fun AssistantAnswerCard(
    title: String,
    body: String,
    source: String,
    onOpenMaterialList: () -> Unit,
    onReadAnswer: () -> Unit,
    onStopReading: () -> Unit,
    isPreparing: Boolean,
    isSpeaking: Boolean
) {
    val context = LocalContext.current
    val responsive = LocalElderResponsive.current
    val isReadingActive = isPreparing || isSpeaking
    SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.AccountCircle, size = responsive.iconMedium)
                Text(
                    title,
                    color = ElderText,
                    fontSize = responsive.cardTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = body,
                color = Color(0xFF25334A),
                fontSize = responsive.bodyLarge
            )
            Text(text = source, color = ElderTextMuted, fontSize = responsive.labelSmall)
            AdaptivePairRow(
                first = { itemModifier ->
                    SecondaryActionButton(
                        when {
                            isPreparing -> stringResource(R.string.qa_read_prepare)
                            isSpeaking -> stringResource(R.string.qa_read_stop)
                            else -> stringResource(R.string.qa_read_answer)
                        },
                        if (isReadingActive) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                        if (isReadingActive) onStopReading else onReadAnswer,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                },
                second = { itemModifier ->
                    SecondaryActionButton(
                        stringResource(R.string.qa_open_materials),
                        Icons.Filled.List,
                        onOpenMaterialList,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                }
            )
            SecondaryActionButton(
                stringResource(R.string.qa_continue),
                Icons.Filled.Refresh,
                { Toast.makeText(context, context.getString(R.string.chat_continue_toast), Toast.LENGTH_SHORT).show() },
                height = 52.dp
            )
        }
    }
}

private fun sourceText(documents: List<String>): String {
    if (documents.isEmpty()) {
        return "资料来源：粤同心政策知识库"
    }

    val shownDocuments = documents.take(2).joinToString("、")
    val suffix = if (documents.size > 2) "等" else ""
    return "资料来源：$shownDocuments$suffix"
}

@Composable
private fun UserBubble(text: String) {
    val responsive = LocalElderResponsive.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .fillMaxWidth(if (responsive.stackActionRows) 0.92f else 0.78f)
                .background(ElderBlueSoft, RoundedCornerShape(18.dp))
                .border(1.dp, ElderLine, RoundedCornerShape(18.dp))
                .padding(responsive.cardPadding)
        ) {
            Text(text = text, color = ElderText, fontSize = responsive.bodyLarge)
        }
    }
}

@Composable
private fun LoadingCard(text: String? = null) {
    val responsive = LocalElderResponsive.current
    SoftCard {
        Row(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            CircularProgressIndicator(color = ElderBlue, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text(
                text = text ?: stringResource(R.string.chat_loading_official),
                color = ElderText,
                fontSize = responsive.body,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VoiceSampleInfoCard(name: String, sizeBytes: Long) {
    val sizeText = formatVoiceDebugSize(sizeBytes)
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFD9E8FF), elevation = 0.dp) {
        Column(modifier = Modifier.padding(responsive.cardSpacing), verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Mic, size = responsive.iconSmall + 18.dp)
                Text(
                    text = stringResource(R.string.voice_sample_prefix, sizeText),
                    color = ElderText,
                    fontSize = responsive.body,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = name,
                color = ElderTextMuted,
                fontSize = responsive.labelSmall
            )
        }
    }
}

private fun formatVoiceDebugSize(sizeBytes: Long): String {
    if (sizeBytes <= 0L) return "0 B"
    if (sizeBytes < 1024L) return "$sizeBytes B"
    val kb = sizeBytes / 1024.0
    if (kb < 1024.0) return String.format(Locale.US, "%.1f KB", kb)
    return String.format(Locale.US, "%.1f MB", kb / 1024.0)
}

private fun copyAudioSampleToCache(context: Context, uri: Uri): File? {
    val displayName = getDisplayNameForUri(context, uri)
    val extension = displayName.substringAfterLast('.', missingDelimiterValue = "m4a")
        .lowercase()
        .takeIf { it.length in 2..5 }
        ?: "m4a"
    val sampleDir = File(context.cacheDir, "voice_samples")
    sampleDir.mkdirs()
    val sampleFile = File.createTempFile("voice_sample_", ".$extension", sampleDir)

    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            sampleFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        sampleFile.takeIf { it.exists() && it.length() > 0L }
    } catch (_: Exception) {
        sampleFile.delete()
        null
    }
}

private fun getDisplayNameForUri(context: Context, uri: Uri): String {
    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index).orEmpty().ifBlank { uri.lastPathSegment.orEmpty() }
        }
    }
    return uri.lastPathSegment.orEmpty().ifBlank { context.getString(R.string.voice_sample_default_name) }
}

@Composable
private fun VoiceConfirmCard(
    draft: String,
    retryText: String,
    onReadDraft: () -> Unit,
    onStopReading: () -> Unit,
    isPreparing: Boolean,
    isSpeaking: Boolean,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit
) {
    val isReadingActive = isPreparing || isSpeaking
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Mic, size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.voice_confirm_title),
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.voice_confirm_subtitle),
                        color = ElderTextMuted,
                        fontSize = responsive.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(14.dp))
                    .border(1.dp, ElderLine, RoundedCornerShape(14.dp))
                    .padding(horizontal = responsive.cardSpacing, vertical = responsive.rowSpacing)
            ) {
                Text(
                    text = draft,
                    color = ElderText,
                    fontSize = responsive.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            if (responsive.stackActionRows) {
                SecondaryActionButton(
                    when {
                        isPreparing -> stringResource(R.string.voice_read_prepare)
                        isSpeaking -> stringResource(R.string.voice_read_stop)
                        else -> stringResource(R.string.voice_read)
                    },
                    if (isReadingActive) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                    if (isReadingActive) onStopReading else onReadDraft,
                    height = 52.dp
                )
                AdaptivePairRow(
                    first = { itemModifier ->
                        SecondaryActionButton(retryText, Icons.Filled.Refresh, onRetry, modifier = itemModifier, height = 52.dp)
                    },
                    second = { itemModifier ->
                        SecondaryActionButton(stringResource(R.string.voice_manual_edit), Icons.Filled.Edit, onEdit, modifier = itemModifier, height = 52.dp)
                    }
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)) {
                    SecondaryActionButton(
                        when {
                            isPreparing -> stringResource(R.string.voice_read_prepare)
                            isSpeaking -> stringResource(R.string.voice_read_stop)
                            else -> stringResource(R.string.voice_read)
                        },
                        if (isReadingActive) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                        if (isReadingActive) onStopReading else onReadDraft,
                        modifier = Modifier.weight(1f),
                        height = 52.dp
                    )
                    SecondaryActionButton(retryText, Icons.Filled.Refresh, onRetry, modifier = Modifier.weight(1f), height = 52.dp)
                    SecondaryActionButton(stringResource(R.string.voice_manual_edit), Icons.Filled.Edit, onEdit, modifier = Modifier.weight(1f), height = 52.dp)
                }
            }
            PrimaryActionButton(stringResource(R.string.voice_send_query), Icons.Filled.Check, onConfirm, height = 54.dp)
        }
    }
}

@Composable
private fun VoiceErrorCard(
    message: String,
    onRetry: () -> Unit,
    onManualInput: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFFFC9C2)) {
        Column(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = Icons.Filled.Mic, tint = ElderRed, background = Color(0xFFFFE8E5), size = responsive.iconMedium)
                Text(
                    text = message,
                    color = ElderText,
                    fontSize = responsive.body,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            AdaptivePairRow(
                first = { itemModifier ->
                    SecondaryActionButton(
                        text = stringResource(R.string.voice_retry_record),
                        icon = Icons.Filled.Refresh,
                        onClick = onRetry,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                },
                second = { itemModifier ->
                    SecondaryActionButton(
                        text = stringResource(R.string.voice_manual_input),
                        icon = Icons.Filled.Edit,
                        onClick = onManualInput,
                        modifier = itemModifier,
                        height = 52.dp
                    )
                }
            )
        }
    }
}

@Composable
private fun VoiceInputPanel(
    selectedLanguage: String,
    onLanguageSelected: (String) -> Unit,
    isRecording: Boolean,
    enabled: Boolean,
    statusMessage: String?,
    onStartRecording: () -> Unit,
    onPickSample: () -> Unit,
    onStopRecording: () -> Unit,
    onCancel: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFBFD8FF)) {
        Column(modifier = Modifier.padding(responsive.cardPadding), verticalArrangement = Arrangement.spacedBy(responsive.cardSpacing)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(
                    icon = Icons.Filled.Mic,
                    tint = if (isRecording) ElderRed else ElderBlue,
                    background = if (isRecording) Color(0xFFFFE8E5) else ElderBlueSoft,
                    size = responsive.iconMedium
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isRecording) stringResource(R.string.voice_recording_title) else stringResource(R.string.voice_panel_title),
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isRecording) stringResource(R.string.voice_recording_subtitle) else stringResource(R.string.voice_panel_subtitle),
                        color = ElderTextMuted,
                        fontSize = responsive.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(responsive.iconButtonSize)
                        .background(ElderBlueSoft, CircleShape)
                        .clickable(onClick = onCancel),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.voice_close_panel), tint = ElderBlue)
                }
            }

            SegmentedControl(
                options = voiceLanguageOptions,
                selected = selectedLanguage,
                onSelected = onLanguageSelected
            )

            if (!statusMessage.isNullOrBlank()) {
                Text(
                    text = statusMessage,
                    color = ElderGreen,
                    fontSize = responsive.label,
                    fontWeight = FontWeight.Bold
                )
            }

            PrimaryActionButton(
                text = if (isRecording) stringResource(R.string.voice_stop_and_recognize) else stringResource(R.string.voice_start_speaking),
                icon = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                onClick = if (isRecording) onStopRecording else onStartRecording,
                enabled = enabled && !statusMessage.orEmpty().contains("正在识别"),
                height = 58.dp,
                color = if (isRecording) ElderGreen else ElderBlue
            )
            if (isRecording) {
                SecondaryActionButton(
                    text = stringResource(R.string.common_cancel),
                    icon = Icons.Filled.Close,
                    onClick = onCancel,
                    height = 52.dp
                )
            } else {
                SecondaryActionButton(
                    text = stringResource(R.string.voice_pick_sample),
                    icon = Icons.Filled.Search,
                    onClick = onPickSample,
                    height = 52.dp
                )
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    onVoice: () -> Unit,
    onSend: () -> Unit
) {
    val responsive = LocalElderResponsive.current
    val sendEnabled = enabled && value.isNotBlank()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, ElderLine)
            .padding(horizontal = responsive.pagePadding, vertical = responsive.rowSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(responsive.inputMinHeight)
                .background(ElderBlue, CircleShape)
                .clickable(enabled = enabled, onClick = onVoice),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Mic, contentDescription = stringResource(R.string.voice_input_content_description), tint = Color.White, modifier = Modifier.size(responsive.iconSmall))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = responsive.inputMinHeight, max = responsive.inputMinHeight * 2)
                .background(Color(0xFFF7FAFE), RoundedCornerShape(28.dp))
                .border(1.dp, ElderLine, RoundedCornerShape(28.dp))
                .padding(horizontal = responsive.cardSpacing, vertical = responsive.smallSpacing),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = false,
                maxLines = 2,
                textStyle = TextStyle(color = ElderText, fontSize = responsive.body),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(stringResource(R.string.chat_input_placeholder), color = Color(0xFFA0AABA), fontSize = responsive.body)
                    }
                    innerTextField()
                }
            )
        }
        Box(
            modifier = Modifier
                .size(responsive.inputMinHeight)
                .background(if (sendEnabled) ElderBlue else Color(0xFFE6ECF4), CircleShape)
                .clickable(enabled = sendEnabled, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Send,
                contentDescription = stringResource(R.string.chat_send_content_description),
                tint = if (sendEnabled) Color.White else Color(0xFF9AA6B6),
                modifier = Modifier.size(responsive.iconSmall)
            )
        }
    }
}

@Composable
private fun ServiceGrid(items: List<ServiceItem>, onClick: () -> Unit) {
    val responsive = LocalElderResponsive.current
    val columns = responsive.serviceColumns
    items.chunked(columns).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)) {
            row.forEach { item ->
                ActionCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = item.onClick ?: onClick,
                    modifier = Modifier.weight(1f),
                    layoutMode = if (columns == 1) CardLayoutMode.List else CardLayoutMode.Grid
                )
            }
            if (columns > 1 && row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MyListItem(item: ServiceItem) {
    val responsive = LocalElderResponsive.current
    SoftCard(modifier = Modifier.clickable { item.onClick?.invoke() }, elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(responsive.cardPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            IconBadge(icon = item.icon, size = responsive.iconMedium)
            Text(
                text = item.title,
                color = ElderText,
                fontSize = responsive.cardTitle,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ElderTextMuted, modifier = Modifier.size(responsive.iconSmall))
        }
    }
}

@Composable
private fun FontChoiceCard(
    choice: FontChoice,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val responsive = LocalElderResponsive.current
    SoftCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = if (selected) ElderBlue else ElderCard,
        borderColor = if (selected) ElderBlue else ElderLine
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 142.dp)
                .padding(responsive.cardSpacing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(responsive.iconLarge)
                    .background(if (selected) Color.White else ElderBlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = choice.sample,
                    color = if (selected) ElderBlue else ElderText,
                    fontSize = if (choice.label == "超大字") responsive.cardTitle else 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(responsive.rowSpacing))
            Text(
                text = choice.label,
                color = if (selected) Color.White else ElderText,
                fontSize = responsive.cardTitle,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    val responsive = LocalElderResponsive.current
    SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFD2E5FF), elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(responsive.cardSpacing),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(responsive.iconSmall))
            Text(text = text, color = ElderBlueDark, fontSize = responsive.body, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, background: Color) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = responsive.rowSpacing, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontSize = responsive.label, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReminderRow(text: String) {
    val responsive = LocalElderResponsive.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        IconBadge(icon = Icons.Filled.Check, tint = ElderOrange, background = Color.White, size = responsive.iconSmall + 14.dp)
        Text(
            text = text,
            color = ElderText,
            fontSize = responsive.body,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TimelineRow(index: Int, text: String) {
    val responsive = LocalElderResponsive.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        Box(
            modifier = Modifier
                .size(responsive.iconSmall + 12.dp)
                .background(ElderBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = Color.White, fontSize = responsive.label, fontWeight = FontWeight.Bold)
        }
        Text(
            text = text,
            color = ElderText,
            fontSize = responsive.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FilterChipLike(text: String, selected: Boolean, onClick: () -> Unit) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = Modifier
            .heightIn(min = responsive.segmentedMinHeight)
            .background(if (selected) ElderBlue else Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) ElderBlue else ElderLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = responsive.cardSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else ElderText,
            fontSize = responsive.label,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryMiniButton(text: String, onClick: () -> Unit) {
    val responsive = LocalElderResponsive.current
    Box(
        modifier = Modifier
            .heightIn(min = responsive.compactButtonMinHeight)
            .border(1.5.dp, ElderBlue, RoundedCornerShape(22.dp))
            .background(Color.White, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = responsive.cardSpacing),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = ElderBlue, fontSize = responsive.labelSmall, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoLine(text: String, valueColor: Color = ElderTextMuted) {
    val responsive = LocalElderResponsive.current
    Text(text = text, color = valueColor, fontSize = responsive.label)
}

private data class ServiceItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val onClick: (() -> Unit)? = null
)

private data class GuideStep(
    val icon: ImageVector,
    val title: String,
    val body: String
)
