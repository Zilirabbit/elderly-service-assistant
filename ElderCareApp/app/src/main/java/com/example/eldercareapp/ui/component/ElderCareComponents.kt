package com.example.eldercareapp.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ElderBlue = Color(0xFF176BEF)
val ElderBlueDark = Color(0xFF06245A)
val ElderBlueSoft = Color(0xFFEAF3FF)
val ElderBluePale = Color(0xFFF5F9FF)
val ElderText = Color(0xFF071E49)
val ElderTextMuted = Color(0xFF637083)
val ElderLine = Color(0xFFD6E4F7)
val ElderBackground = Color(0xFFF7FAFE)
val ElderCard = Color(0xFFFFFFFF)
val ElderGreen = Color(0xFF139B3A)
val ElderGreenSoft = Color(0xFFE9F8EE)
val ElderOrange = Color(0xFFF57C00)
val ElderOrangeSoft = Color(0xFFFFF3E4)
val ElderRed = Color(0xFFD93025)

data class TopBarAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

data class BottomNavItemSpec(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

@Composable
fun UnifiedTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    showBack: Boolean = false,
    onBack: (() -> Unit)? = null,
    actions: List<TopBarAction> = emptyList(),
    elevated: Boolean = false,
    leadingIcon: ImageVector? = null,
    gradient: Boolean = false
) {
    val backgroundModifier = if (gradient) {
        Modifier.background(Brush.verticalGradient(listOf(Color(0xFFEAF4FF), Color.White)))
    } else {
        Modifier.background(Color.White)
    }

    Column(
        modifier = modifier
            .then(backgroundModifier)
            .fillMaxWidth()
            .border(
                width = if (elevated) 1.dp else 0.dp,
                color = if (elevated) ElderLine else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (showBack) {
                RoundIconButton(
                    icon = requireNotNull(leadingIcon),
                    contentDescription = "返回",
                    onClick = { onBack?.invoke() }
                )
            } else {
                leadingIcon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = ElderBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ElderText,
                    fontSize = 26.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (showBack) TextAlign.Center else TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = ElderText,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            actions.forEach { action ->
                PillIconButton(
                    text = action.label,
                    icon = action.icon,
                    onClick = action.onClick
                )
            }
        }
    }
}

@Composable
fun UnifiedBottomNav(items: List<BottomNavItemSpec>) {
    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 0.dp,
        modifier = Modifier.border(1.dp, ElderLine)
    ) {
        items.forEach { item ->
            NavigationBarItem(
                selected = item.selected,
                onClick = item.onClick,
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(30.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ElderBlue,
                    selectedTextColor = ElderBlue,
                    indicatorColor = ElderBlueSoft,
                    unselectedIconColor = Color(0xFF7F8898),
                    unselectedTextColor = Color(0xFF7F8898)
                )
            )
        }
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ElderText
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(52.dp)
            .background(Color.White, CircleShape)
            .border(1.dp, ElderLine, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(30.dp)
        )
    }
}

@Composable
fun PillIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.5.dp, ElderBlue),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = ElderBlue
        )
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(32.dp)
                .background(ElderBlue, RoundedCornerShape(3.dp))
        )
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(28.dp))
        }
        Text(
            text = text,
            color = ElderText,
            fontSize = 25.sp,
            lineHeight = 31.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ElderCard,
    borderColor: Color = ElderLine,
    elevation: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        content = content
    )
}

@Composable
fun IconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = ElderBlue,
    background: Color = ElderBlueSoft,
    size: Dp = 64.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .background(background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.48f)
        )
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(1.dp, ElderLine, RoundedCornerShape(16.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(
                        if (option == selected) ElderBlue else Color.Transparent,
                        RoundedCornerShape(13.dp)
                    )
                    .clickable { onSelected(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (option == selected) Color.White else ElderText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 60.dp,
    color: Color = ElderBlue
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE6ECF4),
            disabledContentColor = Color(0xFF9AA6B6)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = text, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SecondaryActionButton(
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.5.dp, ElderBlue),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = ElderBlue
        )
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = text, modifier = Modifier.size(23.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ElderBlue
) {
    SoftCard(
        modifier = modifier.clickable(onClick = onClick),
        elevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconBadge(icon = icon, tint = tint, background = tint.copy(alpha = 0.11f), size = 58.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = ElderText,
                    fontSize = 21.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = subtitle,
                    color = ElderTextMuted,
                    fontSize = 17.sp,
                    lineHeight = 23.sp
                )
            }
        }
    }
}

@Composable
fun InfoRow(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    valueColor: Color = ElderText
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(24.dp))
        Text(
            text = label,
            color = ElderText,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun ProgressDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(if (active) 14.dp else 12.dp)
            .background(if (active) ElderBlue else Color(0xFFC9DAF4), CircleShape)
    )
}
