package com.example.eldercareapp.ui.screen

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.eldercareapp.R
import com.example.eldercareapp.ui.component.ElderBackground
import com.example.eldercareapp.ui.component.ElderBlue
import com.example.eldercareapp.ui.component.ElderBlueLight
import com.example.eldercareapp.ui.component.ElderDeepText
import com.example.eldercareapp.ui.component.ElderGreen
import com.example.eldercareapp.ui.component.ElderOrange
import com.example.eldercareapp.ui.component.ElderRed
import com.example.eldercareapp.ui.component.InfoRow
import com.example.eldercareapp.ui.component.PillButton
import com.example.eldercareapp.ui.component.PrimaryActionButton
import com.example.eldercareapp.ui.component.ProgressDot
import com.example.eldercareapp.ui.component.SectionTitle
import com.example.eldercareapp.ui.component.SoftCard
import com.example.eldercareapp.ui.component.TwoColumnRow
import com.example.eldercareapp.viewmodel.ChatViewModel

private enum class AppScreen {
    Home,
    FontSize,
    MaterialList,
    OperationGuide
}

@Composable
fun ElderCareAppRoot(modifier: Modifier = Modifier) {
    var screen by remember { mutableStateOf(AppScreen.Home) }
    var selectedFontSize by remember { mutableStateOf("大字体") }
    val chatViewModel: ChatViewModel = viewModel()
    val currentDensity = LocalDensity.current
    val fontScale = when (selectedFontSize) {
        "小字体" -> 0.9f
        "中字体" -> 1.0f
        "超大字体" -> 1.3f
        else -> 1.15f
    }

    BackHandler(enabled = screen != AppScreen.Home) {
        screen = AppScreen.Home
    }

    CompositionLocalProvider(
        LocalDensity provides Density(
            density = currentDensity.density,
            fontScale = fontScale
        )
    ) {
        when (screen) {
            AppScreen.Home -> HomeScreen(
                modifier = modifier,
                chatViewModel = chatViewModel,
                onOpenFontSize = { screen = AppScreen.FontSize },
                onOpenMaterialList = { screen = AppScreen.MaterialList },
                onOpenGuide = { screen = AppScreen.OperationGuide }
            )

            AppScreen.FontSize -> FontSizeScreen(
                selected = selectedFontSize,
                onSelected = { selectedFontSize = it },
                onBack = { screen = AppScreen.Home },
                modifier = modifier
            )

            AppScreen.MaterialList -> MaterialListScreen(
                onBack = { screen = AppScreen.Home },
                modifier = modifier
            )

            AppScreen.OperationGuide -> OperationGuideScreen(
                modifier = modifier,
                onClose = { screen = AppScreen.Home }
            )
        }
    }
}

@Composable
private fun HomeScreen(
    chatViewModel: ChatViewModel,
    onOpenFontSize: () -> Unit,
    onOpenMaterialList: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val chatUiState by chatViewModel.uiState.collectAsState()
    var language by remember { mutableStateOf("普通话") }
    var place by remember { mutableStateOf("全部") }
    var direction by remember { mutableStateOf("全部") }
    var faqCategory by remember { mutableStateOf("全部") }
    val toast: (String) -> Unit = { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }

    Column(
        modifier = modifier
            .background(ElderBackground)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(
            language = language,
            place = place,
            direction = direction,
            onLanguageChange = { language = it },
            onPlaceChange = { place = it },
            onDirectionChange = { direction = it },
            onOpenFontSize = onOpenFontSize
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            AssistantIntroCard(onOpenGuide = onOpenGuide)

            TwoColumnRow(
                left = {
                    PrimaryActionButton(
                        text = "📋  材料清单",
                        color = ElderGreen,
                        onClick = onOpenMaterialList
                    )
                },
                right = {
                    PrimaryActionButton(
                        text = "🌐  翻译",
                        color = ElderBlueLight,
                        onClick = { toast("翻译功能为静态入口展示") }
                    )
                }
            )
            PrimaryActionButton(
                text = "🆘  紧急呼叫",
                color = ElderOrange,
                onClick = { toast("紧急呼叫功能为静态入口展示") },
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            FaqSection(
                selectedCategory = faqCategory,
                onCategorySelected = { faqCategory = it },
                onQuestionClick = {
                    chatViewModel.updateInput(it)
                    chatViewModel.sendQuestion()
                }
            )

            QuestionInputSection(
                value = chatUiState.input,
                onValueChange = chatViewModel::updateInput,
                onSend = chatViewModel::sendQuestion,
                onVoice = { toast("语音功能下一阶段接入，请先使用文字输入") },
                isLoading = chatUiState.isLoading,
                answer = chatUiState.answer,
                errorMessage = chatUiState.errorMessage
            )

            PrimaryActionButton(
                text = "📞  子女/客服帮手",
                color = ElderRed,
                onClick = { toast("子女/客服帮手为静态入口展示") }
            )
            PrimaryActionButton(
                text = "📋  我的材料清单",
                color = Color(0xFF2CC56E),
                onClick = { toast("我的材料清单为静态入口展示") }
            )
        }
    }
}

@Composable
private fun HeaderSection(
    language: String,
    place: String,
    direction: String,
    onLanguageChange: (String) -> Unit,
    onPlaceChange: (String) -> Unit,
    onDirectionChange: (String) -> Unit,
    onOpenFontSize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(ElderBlue, Color(0xFF2E89BE))))
            .padding(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 34.dp)
    ) {
        Button(
            onClick = onOpenFontSize,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(54.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White.copy(alpha = 0.17f),
                contentColor = Color.White
            )
        ) {
            Text(text = "abc  字体", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "粤同心-湾区中老年助手",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "您的智能生活帮手",
                color = Color.White,
                fontSize = 23.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))
            PreferenceRow(
                label = "播报语言：",
                options = listOf("普通话", "粤语"),
                selected = language,
                onSelected = onLanguageChange
            )
            PreferenceRow(
                label = "目的地/出发地：",
                options = listOf("香港", "澳门", "全部"),
                selected = place,
                onSelected = onPlaceChange
            )
            PreferenceRow(
                label = "方向：",
                options = listOf("去往港澳", "返回内地", "全部"),
                selected = direction,
                onSelected = onDirectionChange
            )
        }
    }
}

@Composable
private fun PreferenceRow(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = label, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            options.forEach { option ->
                PillButton(
                    text = option,
                    selected = option == selected,
                    onClick = { onSelected(option) }
                )
            }
        }
    }
}

@Composable
private fun AssistantIntroCard(onOpenGuide: () -> Unit) {
    SoftCard(borderColor = ElderBlue) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "👋 您好！我是粤同心-湾区中老年助手智能体",
                color = ElderDeepText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "我可以直接帮您：",
                color = ElderDeepText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))
            InfoRow("📋  准备过关材料")
            InfoRow("🌐  翻译粤语普通话")
            InfoRow("🆘  紧急联系家人")
            InfoRow("📝  输入问题或点击下方按钮语音提问")
            InfoRow("🔊  点击播放按钮听AI朗读回复")
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryActionButton(
                text = "📖  查看操作指南",
                color = ElderBlue,
                onClick = onOpenGuide,
                height = 70.dp
            )
        }
    }
}

@Composable
private fun FaqSection(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    onQuestionClick: (String) -> Unit
) {
    val categories = listOf("全部", "证件办理", "通关流程", "交通出行", "政策福利", "医疗健康")
    val sampleQuestion = "如何申请港澳通行证？"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        SectionTitle(text = "💡 常见问题")
        Spacer(modifier = Modifier.height(18.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            categories.forEach { category ->
                Box(
                    modifier = Modifier
                        .height(58.dp)
                        .shadow(4.dp, RoundedCornerShape(28.dp))
                        .background(
                            if (category == selectedCategory) ElderBlueLight else Color.White,
                            RoundedCornerShape(28.dp)
                        )
                        .border(1.dp, Color(0xFFD8DEE4), RoundedCornerShape(28.dp))
                        .clickable { onCategorySelected(category) }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = category,
                        color = if (category == selectedCategory) Color.White else Color(0xFF59636E),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(4.dp, RoundedCornerShape(12.dp))
                .background(Color.White, RoundedCornerShape(12.dp))
                .clickable { onQuestionClick(sampleQuestion) }
                .padding(22.dp)
        ) {
            Text(
                text = sampleQuestion,
                color = ElderDeepText,
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = "👆 点击上方问题快速提问，或使用语音输入其他问题",
            color = Color(0xFF5B6570),
            fontSize = 19.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun QuestionInputSection(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onVoice: () -> Unit,
    isLoading: Boolean,
    answer: String,
    errorMessage: String?
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(82.dp)
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .border(2.dp, ElderBlue, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = TextStyle(color = Color(0xFF172431), fontSize = 21.sp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isLoading,
                    decorationBox = { innerTextField ->
                        if (value.isEmpty()) {
                            Text(text = "请输入您的问题...", color = Color(0xFFA4AFB8), fontSize = 21.sp)
                        }
                        innerTextField()
                    }
                )
            }
            Button(
                onClick = onSend,
                enabled = !isLoading && value.isNotBlank(),
                modifier = Modifier
                    .width(104.dp)
                    .height(82.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (value.isBlank()) Color(0xFFB9C7D0) else ElderBlue,
                    disabledContainerColor = Color(0xFFB9C7D0)
                )
            ) {
                Text(text = if (isLoading) "查询中" else "发送", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(18.dp))
        PrimaryActionButton(text = "🎤  语音提问", color = ElderBlue, onClick = onVoice)
        if (isLoading) {
            Spacer(modifier = Modifier.height(18.dp))
            SoftCard(borderColor = ElderBlueLight) {
                Row(
                    modifier = Modifier.padding(22.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = ElderBlue,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "正在查询，请稍候...",
                        color = ElderDeepText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        if (!errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            SoftCard(borderColor = ElderRed) {
                Text(
                    text = errorMessage,
                    color = ElderRed,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(22.dp)
                )
            }
        }
        if (answer.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            SoftCard(borderColor = ElderGreen) {
                Column(modifier = Modifier.padding(22.dp)) {
                    Text(
                        text = "AI 回复",
                        color = ElderDeepText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = answer,
                        color = Color(0xFF172431),
                        fontSize = 20.sp,
                        lineHeight = 30.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun FontSizeScreen(
    selected: String,
    onSelected: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        "小字体" to "小",
        "中字体" to "中",
        "大字体" to "大",
        "超大字体" to "超大"
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(text = "选择字体大小")
        Spacer(modifier = Modifier.height(20.dp))
        Text(text = "请选择适合您的字体大小", color = Color(0xFF5B6570), fontSize = 22.sp)
        Spacer(modifier = Modifier.height(36.dp))
        options.chunked(2).forEach { rowItems ->
            TwoColumnRow(
                left = {
                    FontOptionCard(
                        title = rowItems[0].first,
                        sample = rowItems[0].second,
                        selected = selected == rowItems[0].first,
                        onClick = { onSelected(rowItems[0].first) }
                    )
                },
                right = {
                    FontOptionCard(
                        title = rowItems[1].first,
                        sample = rowItems[1].second,
                        selected = selected == rowItems[1].first,
                        onClick = { onSelected(rowItems[1].first) }
                    )
                }
            )
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FontOptionCard(
    title: String,
    sample: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .shadow(3.dp, RoundedCornerShape(16.dp))
            .background(if (selected) Color(0xFF2A7CAB) else Color(0xFFF8F9FA), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .shadow(5.dp, CircleShape)
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = sample,
                color = ElderDeepText,
                fontSize = if (sample == "超大") 31.sp else 35.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = title,
            color = if (selected) Color.White else ElderDeepText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MaterialListScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val items = listOf(
        "🛂" to "办理港澳通行证",
        "🔁" to "港澳通行证续签",
        "💼" to "社保医保转移",
        "👴" to "老年证办理",
        "💰" to "老年补贴申请",
        "🧳" to "过关材料准备"
    )

    Column(
        modifier = modifier
            .background(Color.White)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onBack)
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(text = "📋  请选择要办理的事项")
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "选择后，我会为您生成对应的材料清单",
            color = Color(0xFF6A6F76),
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(34.dp))
        items.chunked(2).forEach { rowItems ->
            TwoColumnRow(
                left = {
                    MaterialItemCard(rowItems[0].first, rowItems[0].second) {
                        Toast.makeText(context, "仅展示事项选择入口", Toast.LENGTH_SHORT).show()
                    }
                },
                right = {
                    MaterialItemCard(rowItems[1].first, rowItems[1].second) {
                        Toast.makeText(context, "仅展示事项选择入口", Toast.LENGTH_SHORT).show()
                    }
                }
            )
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun MaterialItemCard(
    icon: String,
    title: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(148.dp)
            .shadow(2.dp, RoundedCornerShape(16.dp))
            .background(Color(0xFFEFEFEF), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFDEDEDE), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = icon, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = title,
            color = Color(0xFFC6C6C6),
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun OperationGuideScreen(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .background(Color.White)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBackButton(onBack = onClose)
        Spacer(modifier = Modifier.height(16.dp))
        SectionTitle(text = "📖  操作指南")
        Spacer(modifier = Modifier.height(18.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, ElderBlue, RoundedCornerShape(8.dp))
                .background(Color(0xFFF4F8FB), RoundedCornerShape(8.dp))
                .padding(14.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.operation_guide_step1),
                contentDescription = "语音提问操作示意图",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.34f)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "第一步：点击语音提问",
            color = ElderDeepText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "点击底部蓝色“语音提问”按钮，说出您的问题",
            color = Color(0xFF172431),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(26.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(5) { index -> ProgressDot(active = index == 0) }
        }
        Spacer(modifier = Modifier.height(30.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GuideButton(
                text = "⬅  上一步",
                color = Color(0xFFCCD8DF),
                onClick = { Toast.makeText(context, "当前已是第一步", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "1 / 5",
                color = ElderDeepText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(72.dp)
            )
            GuideButton(
                text = "下一步  ➡",
                color = ElderBlue,
                onClick = { Toast.makeText(context, "后续步骤暂未展示", Toast.LENGTH_SHORT).show() },
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onClose,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7F7F7), contentColor = Color.Black)
        ) {
            Text(text = "关闭", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TopBackButton(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFEAF3F8),
                contentColor = ElderDeepText
            ),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
        ) {
            Text(text = "← 返回", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GuideButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(58.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    ) {
        Text(text = text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}
