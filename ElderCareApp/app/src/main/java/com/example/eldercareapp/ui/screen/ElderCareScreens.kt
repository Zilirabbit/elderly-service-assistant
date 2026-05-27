package com.example.eldercareapp.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eldercareapp.model.MaterialChecklist
import com.example.eldercareapp.ui.component.ActionCard
import com.example.eldercareapp.ui.component.BottomNavItemSpec
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
import com.example.eldercareapp.ui.component.IconBadge
import com.example.eldercareapp.ui.component.InfoRow
import com.example.eldercareapp.ui.component.PrimaryActionButton
import com.example.eldercareapp.ui.component.ProgressDot
import com.example.eldercareapp.ui.component.SecondaryActionButton
import com.example.eldercareapp.ui.component.SegmentedControl
import com.example.eldercareapp.ui.component.SectionTitle
import com.example.eldercareapp.ui.component.SoftCard
import com.example.eldercareapp.ui.component.TopBarAction
import com.example.eldercareapp.ui.component.UnifiedBottomNav
import com.example.eldercareapp.ui.component.UnifiedTopBar
import com.example.eldercareapp.viewmodel.ChatViewModel
import com.example.eldercareapp.viewmodel.MaterialViewModel
import com.example.eldercareapp.viewmodel.SavedMaterialChecklist

private enum class MainTab {
    Home,
    Chat,
    Service,
    My
}

private enum class OverlayScreen {
    PortDetail,
    Guide,
    FontSize,
    MaterialList
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

private val fontChoices = listOf(
    FontChoice("小字", "小", 0.9f),
    FontChoice("中字", "中", 1.0f),
    FontChoice("大字", "大", 1.15f),
    FontChoice("超大字", "超大", 1.3f)
)

private val ports = listOf(
    PortInfo("深圳湾口岸", "6:30 - 24:00", "约 15 分钟", Icons.Filled.Place),
    PortInfo("福田口岸", "6:30 - 22:30", "约 10 分钟", Icons.Filled.Place)
)

@Composable
fun ElderCareAppRoot(modifier: Modifier = Modifier) {
    var currentTab by remember { mutableStateOf(MainTab.Home) }
    var overlayScreen by remember { mutableStateOf<OverlayScreen?>(null) }
    var selectedFont by remember { mutableStateOf(fontChoices[2]) }
    val chatViewModel: ChatViewModel = viewModel()
    val materialViewModel: MaterialViewModel = viewModel()
    val currentDensity = LocalDensity.current

    BackHandler(enabled = overlayScreen != null || currentTab != MainTab.Home) {
        if (overlayScreen != null) {
            overlayScreen = null
        } else {
            currentTab = MainTab.Home
        }
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = selectedFont.scale
        )
    ) {
        val openFont = { overlayScreen = OverlayScreen.FontSize }
        val closeOverlay = { overlayScreen = null }

        if (overlayScreen != null) {
            when (overlayScreen) {
                OverlayScreen.PortDetail -> PortDetailScreen(
                    modifier = modifier,
                    onBack = closeOverlay,
                    onAskAi = {
                        overlayScreen = null
                        currentTab = MainTab.Chat
                    },
                    onOpenMaterialList = { overlayScreen = OverlayScreen.MaterialList }
                )

                OverlayScreen.Guide -> GuideScreen(
                    modifier = modifier,
                    onBack = closeOverlay,
                    onDone = closeOverlay
                )

                OverlayScreen.FontSize -> FontSizeScreen(
                    modifier = modifier,
                    selected = selectedFont,
                    onSelected = { selectedFont = it },
                    onBack = closeOverlay
                )

                OverlayScreen.MaterialList -> MaterialListScreen(
                    modifier = modifier,
                    materialViewModel = materialViewModel,
                    onBack = closeOverlay
                )

                else -> Unit
            }
        } else {
            Scaffold(
                modifier = modifier.fillMaxSize(),
                containerColor = ElderBackground,
                bottomBar = {
                    UnifiedBottomNav(
                        items = listOf(
                            BottomNavItemSpec("首页", Icons.Filled.Home, currentTab == MainTab.Home) {
                                currentTab = MainTab.Home
                            },
                            BottomNavItemSpec("问答", Icons.Filled.Email, currentTab == MainTab.Chat) {
                                currentTab = MainTab.Chat
                            },
                            BottomNavItemSpec("服务", Icons.Filled.List, currentTab == MainTab.Service) {
                                currentTab = MainTab.Service
                            },
                            BottomNavItemSpec("我的", Icons.Filled.Person, currentTab == MainTab.My) {
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
                        onOpenMaterialList = { overlayScreen = OverlayScreen.MaterialList },
                        onFaqClick = { question ->
                            chatViewModel.updateInput(question)
                            chatViewModel.sendQuestion()
                            currentTab = MainTab.Chat
                        }
                    )

                    MainTab.Chat -> ChatScreen(
                        modifier = Modifier.padding(innerPadding),
                        chatViewModel = chatViewModel,
                        onOpenMaterialList = { overlayScreen = OverlayScreen.MaterialList },
                        onOpenFontSize = openFont
                    )

                    MainTab.Service -> ServiceScreen(
                        modifier = Modifier.padding(innerPadding)
                    )

                    MainTab.My -> MyScreen(
                        modifier = Modifier.padding(innerPadding),
                        onOpenFontSize = openFont,
                        onOpenGuide = { overlayScreen = OverlayScreen.Guide },
                        onOpenMaterialList = { overlayScreen = OverlayScreen.MaterialList },
                        materialViewModel = materialViewModel
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenFontSize: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenPortDetail: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenMaterialList: () -> Unit,
    onFaqClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var region by remember { mutableStateOf("香港") }
    var direction by remember { mutableStateOf("去往港澳") }
    var faqCategory by remember { mutableStateOf("全部") }

    ScreenColumn(
        modifier = modifier,
        topBar = {
            UnifiedTopBar(
                title = "粤同心",
                subtitle = "湾区中老年助手",
                gradient = true,
                actions = listOf(
                    TopBarAction("字体", Icons.Filled.Settings, onOpenFontSize),
                    TopBarAction("语言", Icons.Filled.Info) {
                        Toast.makeText(context, "语言切换暂未开放", Toast.LENGTH_SHORT).show()
                    }
                )
            )
        }
    ) {
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

        SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFBFD8FF)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconBadge(icon = Icons.Filled.Email, size = 70.dp)
                    Text(
                        text = "您好，我可以帮您查政策、准备材料、陪您办理",
                        color = ElderText,
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PrimaryActionButton(
                        text = "点击提问",
                        icon = Icons.Filled.Email,
                        onClick = onOpenChat,
                        modifier = Modifier.weight(1f)
                    )
                    SecondaryActionButton(
                        text = "操作指南",
                        icon = Icons.Filled.Info,
                        onClick = onOpenGuide,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        SectionTitle(text = "常用口岸")
        ports.forEach { port ->
            PortSummaryCard(port = port, onClick = onOpenPortDetail)
        }
        SecondaryActionButton(
            text = "查看全部口岸",
            icon = Icons.Filled.List,
            onClick = onOpenPortDetail,
            height = 52.dp
        )

        SectionTitle(text = "常见问题")
        FaqSection(
            selectedCategory = faqCategory,
            onCategorySelected = { faqCategory = it },
            onQuestionClick = onFaqClick
        )
        SecondaryActionButton(
            text = "展开更多",
            icon = Icons.Filled.KeyboardArrowDown,
            onClick = { onFaqClick("老人去香港过关要带什么？") },
            height = 52.dp
        )

        ActionCard(
            title = "我的材料清单",
            subtitle = "保存证件、表格和办事草稿",
            icon = Icons.Filled.List,
            onClick = onOpenMaterialList
        )
    }
}

@Composable
private fun ChatScreen(
    chatViewModel: ChatViewModel,
    onOpenMaterialList: () -> Unit,
    onOpenFontSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by chatViewModel.uiState.collectAsState()
    var voiceDraft by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElderBackground)
    ) {
        UnifiedTopBar(
            title = "智能问答",
            elevated = true,
            actions = listOf(
                TopBarAction("历史", Icons.Filled.DateRange) {
                    Toast.makeText(context, "历史记录暂未开放", Toast.LENGTH_SHORT).show()
                },
                TopBarAction("字体", Icons.Filled.Settings, onOpenFontSize)
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (uiState.answer.isBlank() && !uiState.isLoading && uiState.errorMessage.isNullOrBlank()) {
                AssistantAnswerCard(
                    title = "我帮您查到这些",
                    body = "您可以询问港澳通行证续签材料、过关流程、口岸开放时间和交通路线。我会尽量用简单的话说明。",
                    source = "资料来源：粤同心政策知识库",
                    onOpenMaterialList = onOpenMaterialList
                )
            } else {
                UserBubble(text = uiState.input.ifBlank { "港澳通行证续签需要什么材料？" })
            }

            if (uiState.isLoading) {
                LoadingCard()
            }

            uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                SoftCard(containerColor = Color.White, borderColor = Color(0xFFFFC9C2)) {
                    Row(
                        modifier = Modifier.padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        IconBadge(icon = Icons.Filled.Warning, tint = ElderRed, background = Color(0xFFFFE8E5), size = 52.dp)
                        Text(
                            text = message,
                            color = ElderRed,
                            fontSize = 19.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (uiState.answer.isNotBlank()) {
                AssistantAnswerCard(
                    title = "我帮您查到这些",
                    body = uiState.answer,
                    source = sourceText(uiState.sourceDocuments),
                    onOpenMaterialList = onOpenMaterialList
                )
            }

            voiceDraft?.let { draft ->
                VoiceConfirmCard(
                    draft = draft,
                    onConfirm = {
                        chatViewModel.updateInput(draft)
                        voiceDraft = null
                        chatViewModel.sendQuestion()
                    },
                    onRetry = { voiceDraft = "我想查港澳通行证续签材料" },
                    onEdit = {
                        chatViewModel.updateInput(draft)
                        voiceDraft = null
                    }
                )
            }
        }

        ChatInputBar(
            value = uiState.input,
            onValueChange = chatViewModel::updateInput,
            enabled = !uiState.isLoading,
            onVoice = { voiceDraft = "我想查港澳通行证续签材料" },
            onSend = chatViewModel::sendQuestion
        )
    }
}

@Composable
private fun ServiceScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val toast = {
        Toast.makeText(context, "该功能暂未开放", Toast.LENGTH_SHORT).show()
    }
    val travelServices = listOf(
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
        topBar = { UnifiedTopBar(title = "服务", gradient = true) }
    ) {
        SectionTitle(text = "出发/到达服务")
        ServiceGrid(items = travelServices, onClick = toast)
        SectionTitle(text = "其他服务")
        ServiceGrid(items = otherServices, onClick = toast)
        NoticeCard(text = "部分服务暂未开放，后续将逐步接入。")
    }
}

@Composable
private fun MyScreen(
    onOpenFontSize: () -> Unit,
    onOpenGuide: () -> Unit,
    onOpenMaterialList: () -> Unit,
    materialViewModel: MaterialViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val materialUiState by materialViewModel.uiState.collectAsState()
    val toast = {
        Toast.makeText(context, "该功能暂未开放", Toast.LENGTH_SHORT).show()
    }
    val items = listOf(
        ServiceItem("字体设置", "调整为适合阅读的大小", Icons.Filled.Settings, onOpenFontSize),
        ServiceItem("我的材料清单", "查看保存的办事材料", Icons.Filled.List, onOpenMaterialList),
        ServiceItem("表单草稿", "继续编辑未完成表单", Icons.Filled.Edit, toast),
        ServiceItem("家属确认", "请家人协助核对", Icons.Filled.Person, toast),
        ServiceItem("操作指南", "查看 App 使用步骤", Icons.Filled.Info, onOpenGuide),
        ServiceItem("关于项目", "演示版本说明", Icons.Filled.AccountCircle, toast)
    )

    ScreenColumn(
        modifier = modifier,
        topBar = { UnifiedTopBar(title = "我的", gradient = true) }
    ) {
        SoftCard {
            Row(
                modifier = Modifier.padding(22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                IconBadge(icon = Icons.Filled.AccountCircle, size = 76.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "您好，欢迎使用粤同心",
                        color = ElderText,
                        fontSize = 25.sp,
                        lineHeight = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "为您保存材料清单和办事草稿",
                        color = ElderTextMuted,
                        fontSize = 18.sp,
                        lineHeight = 25.sp
                    )
                }
            }
        }

        SavedMaterialsSection(savedChecklists = materialUiState.savedChecklists)

        items.forEach { item ->
            MyListItem(item = item)
        }
        NoticeCard(text = "当前为演示版本，部分功能暂未开放。")
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
            Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    IconBadge(icon = Icons.Filled.Place, size = 72.dp)
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "深圳湾口岸",
                                color = ElderText,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            StatusPill(text = "正常", color = ElderGreen, background = ElderGreenSoft)
                        }
                    }
                }
                InfoRow("开放时间", "6:30 - 24:00", Icons.Filled.DateRange)
                InfoRow("预计等待", "约 15 分钟", Icons.Filled.Person, valueColor = ElderGreen)
                InfoRow("更新时间", "今天 09:30", Icons.Filled.Refresh)
            }
        }

        SoftCard(containerColor = ElderOrangeSoft, borderColor = Color(0xFFFFCC8F)) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(text = "重要提醒", icon = Icons.Filled.Warning)
                ReminderRow("请确认港澳通行证和有效签注")
                ReminderRow("建议提前准备身份证件")
                ReminderRow("高峰时段可能排队，请预留时间")
            }
        }

        SoftCard {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SectionTitle(text = "过关步骤")
                TimelineRow(1, "到达口岸")
                TimelineRow(2, "准备证件")
                TimelineRow(3, "通过边检")
                TimelineRow(4, "前往香港侧交通接驳")
            }
        }

        SectionTitle(text = "相关服务")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("问 AI", "询问通关问题", Icons.Filled.Email, onAskAi, modifier = Modifier.weight(1f))
            ActionCard("查看材料", "核对必备证件", Icons.Filled.List, onOpenMaterialList, modifier = Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard("导航", "查看路线", Icons.Filled.Place, { Toast.makeText(context, "导航功能暂未开放", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f))
            ActionCard("找家人帮忙", "请家属协助", Icons.Filled.Person, { Toast.makeText(context, "家属协助暂未开放", Toast.LENGTH_SHORT).show() }, modifier = Modifier.weight(1f))
        }

        NoticeCard(text = "状态为演示数据，请以现场公告为准。")
    }
}

@Composable
private fun GuideScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var stepIndex by remember { mutableIntStateOf(0) }
    val steps = listOf(
        GuideStep(Icons.Filled.Email, "第 1 步：点击提问", "在首页点击“点击提问”，进入智能问答页面。"),
        GuideStep(Icons.Filled.Edit, "第 2 步：说出或输入问题", "您可以直接输入文字，也可以点击麦克风说话。"),
        GuideStep(Icons.Filled.Check, "第 3 步：确认识别文字", "如果使用语音提问，系统会先显示听到的文字，请确认无误后继续。"),
        GuideStep(Icons.Filled.Search, "第 4 步：查看 AI 回答", "系统会根据政策知识库整理回答，并尽量用简单的话说明。"),
        GuideStep(Icons.Filled.List, "第 5 步：保存或继续办理", "您可以查看材料清单、朗读回答，或请家人帮忙确认。")
    )
    val current = steps[stepIndex]

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = "操作指南",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = onBack,
                elevated = true
            )
        }
    ) {
        SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                IconBadge(icon = current.icon, size = 150.dp)
                Text(
                    text = current.title,
                    color = ElderText,
                    fontSize = 28.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = current.body,
                    color = ElderTextMuted,
                    fontSize = 21.sp,
                    lineHeight = 31.sp,
                    textAlign = TextAlign.Center
                )
                SecondaryActionButton(
                    text = "朗读本步",
                    icon = Icons.Filled.PlayArrow,
                    onClick = { Toast.makeText(context, "朗读功能暂未开放", Toast.LENGTH_SHORT).show() },
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
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        PrimaryActionButton(
            text = "上一步",
            icon = Icons.Filled.ArrowBack,
            onClick = { if (stepIndex > 0) stepIndex-- },
            enabled = stepIndex > 0,
            color = Color(0xFF8EA3C0)
        )
        PrimaryActionButton(
            text = if (stepIndex == steps.lastIndex) "完成" else "下一步",
            icon = if (stepIndex == steps.lastIndex) Icons.Filled.Check else Icons.Filled.KeyboardArrowRight,
            onClick = {
                if (stepIndex == steps.lastIndex) {
                    onDone()
                } else {
                    stepIndex++
                }
            }
        )
        SecondaryActionButton(
            text = "关闭",
            icon = Icons.Filled.Close,
            onClick = onDone
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
            fontSize = 21.sp,
            lineHeight = 30.sp
        )
        fontChoices.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                rowItems.forEach { choice ->
                    FontChoiceCard(
                        choice = choice,
                        selected = choice == selected,
                        onClick = { onSelected(choice) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
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
    materialViewModel: MaterialViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by materialViewModel.uiState.collectAsState()
    val selectedChecklist = uiState.selectedChecklist

    ScreenColumn(
        modifier = modifier.safeDrawingPadding(),
        topBar = {
            UnifiedTopBar(
                title = selectedChecklist?.title ?: "我的材料清单",
                showBack = true,
                leadingIcon = Icons.Filled.ArrowBack,
                onBack = {
                    if (selectedChecklist == null) {
                        onBack()
                    } else {
                        materialViewModel.closeChecklist()
                    }
                },
                elevated = true
            )
        }
    ) {
        if (uiState.isLoading && uiState.items.isEmpty() && selectedChecklist == null) {
            LoadingCard(text = "正在加载材料清单，请稍候...")
        }

        uiState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            MaterialErrorCard(
                message = message,
                onRetry = {
                    if (selectedChecklist == null) {
                        materialViewModel.loadItems()
                    } else {
                        materialViewModel.selectItem(selectedChecklist.code)
                    }
                }
            )
        }

        if (selectedChecklist == null) {
            SectionTitle(text = "请选择要办理的事项")
            uiState.items.forEach { item ->
                ActionCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = when (item.code) {
                        "hk_macau_pass_apply" -> Icons.Filled.AccountCircle
                        "hk_macau_endorsement_renew" -> Icons.Filled.Refresh
                        else -> Icons.Filled.List
                    },
                    onClick = { materialViewModel.selectItem(item.code) }
                )
            }
            NoticeCard(text = "清单来自后端演示数据，办理前仍请以现场和官方要求为准。")
        } else {
            MaterialChecklistDetail(
                checklist = selectedChecklist,
                checkedIds = uiState.checkedRequirementIds,
                isSaving = uiState.isLoading,
                saveMessage = uiState.saveMessage,
                onToggle = materialViewModel::toggleRequirement,
                onSave = materialViewModel::saveSelectedChecklist
            )
        }
    }
}

@Composable
private fun SavedMaterialsSection(savedChecklists: List<SavedMaterialChecklist>) {
    SectionTitle(text = "已保存材料清单", icon = Icons.Filled.Check)
    if (savedChecklists.isEmpty()) {
        NoticeCard(text = "保存后的材料清单会显示在这里，方便办理前再次核对。")
        return
    }

    savedChecklists.forEach { item ->
        SoftCard {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                IconBadge(icon = Icons.Filled.List, tint = ElderGreen, background = ElderGreenSoft, size = 54.dp)
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = item.title,
                        color = ElderText,
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "已勾选 ${item.checkedCount} / ${item.totalCount} 项",
                        color = ElderTextMuted,
                        fontSize = 17.sp,
                        lineHeight = 24.sp
                    )
                }
                StatusPill(text = "已保存", color = ElderGreen, background = ElderGreenSoft)
            }
        }
    }
}

@Composable
private fun MaterialErrorCard(message: String, onRetry: () -> Unit) {
    SoftCard(containerColor = Color.White, borderColor = Color(0xFFFFC9C2)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconBadge(icon = Icons.Filled.Warning, tint = ElderRed, background = Color(0xFFFFE8E5), size = 52.dp)
                Text(
                    text = message,
                    color = ElderRed,
                    fontSize = 19.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
            }
            SecondaryActionButton("重新加载", Icons.Filled.Refresh, onRetry, height = 52.dp)
        }
    }
}

@Composable
private fun MaterialChecklistDetail(
    checklist: MaterialChecklist,
    checkedIds: Set<String>,
    isSaving: Boolean,
    saveMessage: String?,
    onToggle: (String) -> Unit,
    onSave: () -> Unit
) {
    SectionTitle(text = "办理提醒", icon = Icons.Filled.Info)
    NoticeCard(text = checklist.tips.joinToString("\n"))

    SectionTitle(text = "材料核对", icon = Icons.Filled.List)
    checklist.requirements.forEach { requirement ->
        RequirementCheckRow(
            name = requirement.name,
            description = requirement.description,
            note = requirement.note,
            required = requirement.required,
            checked = requirement.id in checkedIds,
            onToggle = { onToggle(requirement.id) }
        )
    }

    saveMessage?.takeIf { it.isNotBlank() }?.let { message ->
        SoftCard(containerColor = ElderGreenSoft, borderColor = Color(0xFFBFE6CA), elevation = 0.dp) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = ElderGreen, modifier = Modifier.size(26.dp))
                Text(text = message, color = ElderGreen, fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
    }

    PrimaryActionButton(
        text = if (isSaving) "正在保存..." else "保存清单",
        icon = Icons.Filled.Check,
        onClick = onSave,
        enabled = !isSaving,
        height = 58.dp
    )
}

@Composable
private fun RequirementCheckRow(
    name: String,
    description: String,
    note: String?,
    required: Boolean,
    checked: Boolean,
    onToggle: () -> Unit
) {
    SoftCard(modifier = Modifier.clickable(onClick = onToggle), elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
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
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = name,
                        color = ElderText,
                        fontSize = 20.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    StatusPill(
                        text = if (required) "必带" else "可选",
                        color = if (required) ElderOrange else ElderGreen,
                        background = if (required) ElderOrangeSoft else ElderGreenSoft
                    )
                }
                Text(text = description, color = ElderTextMuted, fontSize = 17.sp, lineHeight = 24.sp)
                note?.takeIf { it.isNotBlank() }?.let {
                    Text(text = it, color = ElderBlueDark, fontSize = 16.sp, lineHeight = 23.sp)
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElderBackground)
            .verticalScroll(rememberScrollState())
    ) {
        topBar()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
private fun PortSummaryCard(port: PortInfo, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconBadge(icon = port.icon, size = 72.dp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(port.name, color = ElderText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                InfoLine("开放时间：${port.openTime}")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("当前：", color = ElderTextMuted, fontSize = 17.sp)
                    StatusPill(text = "正常", color = ElderGreen, background = ElderGreenSoft)
                }
                InfoLine("预计等待：${port.waitTime}", valueColor = ElderGreen)
            }
            SecondaryMiniButton(text = "查看详情", onClick = onClick)
        }
    }
}

@Composable
private fun FaqSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onQuestionClick: (String) -> Unit
) {
    val categories = listOf("全部", "证件办理", "签注续签", "通关流程", "交通出行")
    val questions = listOf(
        "港澳通行证续签需要什么材料？",
        "老人去香港过关要带什么？",
        "只有澳门签注可以去香港吗？"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        categories.forEach { category ->
            FilterChipLike(
                text = category,
                selected = category == selectedCategory,
                onClick = { onCategorySelected(category) }
            )
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        questions.forEach { question ->
            SoftCard(modifier = Modifier.clickable { onQuestionClick(question) }, elevation = 0.dp) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconBadge(icon = Icons.Filled.Info, size = 42.dp)
                    Text(
                        text = question,
                        color = ElderText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ElderTextMuted)
                }
            }
        }
    }
}

@Composable
private fun AssistantAnswerCard(
    title: String,
    body: String,
    source: String,
    onOpenMaterialList: () -> Unit
) {
    val context = LocalContext.current
    SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconBadge(icon = Icons.Filled.AccountCircle, size = 58.dp)
                Text(title, color = ElderText, fontSize = 25.sp, fontWeight = FontWeight.Bold)
            }
            Text(
                text = body,
                color = Color(0xFF25334A),
                fontSize = 20.sp,
                lineHeight = 31.sp
            )
            Text(text = source, color = ElderTextMuted, fontSize = 16.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SecondaryActionButton(
                    "朗读回答",
                    Icons.Filled.PlayArrow,
                    { Toast.makeText(context, "朗读功能暂未开放", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
                SecondaryActionButton(
                    "查看材料清单",
                    Icons.Filled.List,
                    onOpenMaterialList,
                    modifier = Modifier.weight(1f),
                    height = 52.dp
                )
            }
            SecondaryActionButton(
                "继续追问",
                Icons.Filled.Refresh,
                { Toast.makeText(context, "请在下方输入框继续提问", Toast.LENGTH_SHORT).show() },
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
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .background(ElderBlueSoft, RoundedCornerShape(18.dp))
                .border(1.dp, ElderLine, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Text(text = text, color = ElderText, fontSize = 20.sp, lineHeight = 30.sp)
        }
    }
}

@Composable
private fun LoadingCard(text: String = "正在查询官方资料，请稍候...") {
    SoftCard {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            CircularProgressIndicator(color = ElderBlue, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
            Text(
                text = text,
                color = ElderText,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun VoiceConfirmCard(
    draft: String,
    onConfirm: () -> Unit,
    onRetry: () -> Unit,
    onEdit: () -> Unit
) {
    SoftCard(containerColor = ElderBluePale, borderColor = Color(0xFFBFD8FF)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "我听到的是：$draft",
                color = ElderText,
                fontSize = 22.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryActionButton("正确，继续", Icons.Filled.Check, onConfirm, modifier = Modifier.weight(1f), height = 54.dp)
                SecondaryActionButton("重新说一遍", Icons.Filled.Refresh, onRetry, modifier = Modifier.weight(1f), height = 54.dp)
            }
            SecondaryActionButton("手动修改", Icons.Filled.Edit, onEdit, height = 54.dp)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .border(1.dp, ElderLine)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(ElderBlue, CircleShape)
                .clickable(onClick = onVoice),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Call, contentDescription = "语音输入", tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .height(56.dp)
                .background(Color(0xFFF7FAFE), RoundedCornerShape(28.dp))
                .border(1.dp, ElderLine, RoundedCornerShape(28.dp))
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                enabled = enabled,
                singleLine = true,
                textStyle = TextStyle(color = ElderText, fontSize = 19.sp),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text("请输入您的问题", color = Color(0xFFA0AABA), fontSize = 19.sp)
                    }
                    innerTextField()
                }
            )
        }
        PrimaryActionButton(
            text = "发送",
            icon = Icons.Filled.Send,
            onClick = onSend,
            enabled = enabled && value.isNotBlank(),
            modifier = Modifier.width(104.dp),
            height = 56.dp
        )
    }
}

@Composable
private fun ServiceGrid(items: List<ServiceItem>, onClick: () -> Unit) {
    items.chunked(2).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            row.forEach { item ->
                ActionCard(
                    title = item.title,
                    subtitle = item.subtitle,
                    icon = item.icon,
                    onClick = item.onClick ?: onClick,
                    modifier = Modifier.weight(1f)
                )
            }
            if (row.size == 1) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MyListItem(item: ServiceItem) {
    SoftCard(modifier = Modifier.clickable { item.onClick?.invoke() }, elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconBadge(icon = item.icon, size = 58.dp)
            Text(
                text = item.title,
                color = ElderText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = ElderTextMuted, modifier = Modifier.size(32.dp))
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
    SoftCard(
        modifier = modifier.clickable(onClick = onClick),
        containerColor = if (selected) ElderBlue else ElderCard,
        borderColor = if (selected) ElderBlue else ElderLine
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .background(if (selected) Color.White else ElderBlueSoft, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = choice.sample,
                    color = if (selected) ElderBlue else ElderText,
                    fontSize = if (choice.label == "超大字") 24.sp else 30.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = choice.label,
                color = if (selected) Color.White else ElderText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NoticeCard(text: String) {
    SoftCard(containerColor = ElderBlueSoft, borderColor = Color(0xFFD2E5FF), elevation = 0.dp) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Info, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(26.dp))
            Text(text = text, color = ElderBlueDark, fontSize = 18.sp, lineHeight = 26.sp)
        }
    }
}

@Composable
private fun StatusPill(text: String, color: Color, background: Color) {
    Box(
        modifier = Modifier
            .background(background, RoundedCornerShape(8.dp))
            .border(1.dp, color.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text = text, color = color, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun ReminderRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBadge(icon = Icons.Filled.Check, tint = ElderOrange, background = Color.White, size = 36.dp)
        Text(text = text, color = ElderText, fontSize = 19.sp, lineHeight = 28.sp)
    }
}

@Composable
private fun TimelineRow(index: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(ElderBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(index.toString(), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = text, color = ElderText, fontSize = 20.sp, lineHeight = 28.sp)
    }
}

@Composable
private fun FilterChipLike(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(46.dp)
            .background(if (selected) ElderBlue else Color.White, RoundedCornerShape(14.dp))
            .border(1.dp, if (selected) ElderBlue else ElderLine, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) Color.White else ElderText,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SecondaryMiniButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(44.dp)
            .border(1.5.dp, ElderBlue, RoundedCornerShape(22.dp))
            .background(Color.White, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = ElderBlue, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoLine(text: String, valueColor: Color = ElderTextMuted) {
    Text(text = text, color = valueColor, fontSize = 17.sp, lineHeight = 24.sp)
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
