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
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.text.style.TextOverflow
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
    val alwaysShowText: Boolean = false,
    val onClick: () -> Unit
)

data class BottomNavItemSpec(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val onClick: () -> Unit
)

enum class CardLayoutMode {
    List,
    Grid
}

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
    val responsive = LocalElderResponsive.current
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
            .padding(horizontal = responsive.pagePadding, vertical = responsive.cardSpacing)
    ) {
        if (showBack && responsive.useCenterTitleTopBar) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterStart),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoundIconButton(
                        icon = requireNotNull(leadingIcon),
                        contentDescription = "返回",
                        onClick = { onBack?.invoke() }
                    )
                }
                Text(
                    text = title,
                    color = ElderText,
                    fontSize = responsive.topBarTitle,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 72.dp)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
                ) {
                    actions.forEach { action ->
                        PillIconButton(
                            text = action.label,
                            icon = action.icon,
                            onClick = action.onClick,
                            alwaysShowText = action.alwaysShowText
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
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
                            modifier = Modifier.size(responsive.iconSmall)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = ElderText,
                        fontSize = responsive.topBarTitle,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Start,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(responsive.smallSpacing))
                        Text(
                            text = subtitle,
                            color = ElderText,
                            fontSize = responsive.topBarSubtitle,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                actions.forEach { action ->
                    PillIconButton(
                        text = action.label,
                        icon = action.icon,
                        onClick = action.onClick,
                        alwaysShowText = action.alwaysShowText
                    )
                }
            }
        }
    }
}

@Composable
fun UnifiedBottomNav(items: List<BottomNavItemSpec>) {
    val responsive = LocalElderResponsive.current
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
                        modifier = Modifier.size(responsive.iconSmall)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = responsive.labelSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
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
    val responsive = LocalElderResponsive.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(responsive.iconButtonSize)
            .background(Color.White, CircleShape)
            .border(1.dp, ElderLine, CircleShape)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(responsive.iconSmall)
        )
    }
}

@Composable
fun PillIconButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    alwaysShowText: Boolean = false
) {
    val responsive = LocalElderResponsive.current
    val showText = alwaysShowText || responsive.showTopBarActionText
    if (!showText) {
        RoundIconButton(
            icon = icon,
            contentDescription = text,
            onClick = onClick,
            modifier = modifier,
            tint = ElderBlue
        )
        return
    }
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = responsive.compactButtonMinHeight),
        shape = RoundedCornerShape(responsive.controlCorner),
        border = BorderStroke(1.5.dp, ElderBlue),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = ElderBlue
        )
    ) {
        Icon(imageVector = icon, contentDescription = text, modifier = Modifier.size(responsive.iconSmall))
        if (showText) {
            Spacer(modifier = Modifier.width(responsive.smallSpacing))
            Text(
                text = text,
                fontSize = responsive.label,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val responsive = LocalElderResponsive.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        Box(
            modifier = Modifier
                .width(6.dp)
                .height(30.dp)
                .background(ElderBlue, RoundedCornerShape(3.dp))
        )
        icon?.let {
            Icon(imageVector = it, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(responsive.iconSmall))
        }
        Text(
            text = text,
            color = ElderText,
            fontSize = responsive.sectionTitle,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
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
    val responsive = LocalElderResponsive.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(responsive.cardCorner),
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
    val responsive = LocalElderResponsive.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(responsive.controlCorner))
            .border(1.dp, ElderLine, RoundedCornerShape(responsive.controlCorner))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { option ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = responsive.segmentedMinHeight)
                    .background(
                        if (option == selected) ElderBlue else Color.Transparent,
                        RoundedCornerShape(responsive.controlCorner)
                    )
                    .clickable { onSelected(option) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    color = if (option == selected) Color.White else ElderText,
                    fontSize = responsive.label,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
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
    val responsive = LocalElderResponsive.current
    val minHeight = if (height > responsive.buttonMinHeight) height else responsive.buttonMinHeight
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(responsive.controlCorner),
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE6ECF4),
            disabledContentColor = Color(0xFF9AA6B6)
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = text, modifier = Modifier.size(responsive.iconSmall))
            Spacer(modifier = Modifier.width(responsive.smallSpacing))
        }
        Text(
            text = text,
            fontSize = responsive.bodyLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
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
    val responsive = LocalElderResponsive.current
    val minHeight = if (height > responsive.compactButtonMinHeight) height else responsive.compactButtonMinHeight
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = minHeight),
        shape = RoundedCornerShape(responsive.controlCorner),
        border = BorderStroke(1.5.dp, ElderBlue),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = ElderBlue
        )
    ) {
        icon?.let {
            Icon(imageVector = it, contentDescription = text, modifier = Modifier.size(responsive.iconSmall))
            Spacer(modifier = Modifier.width(responsive.smallSpacing))
        }
        Text(
            text = text,
            fontSize = responsive.body,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun ActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = ElderBlue,
    layoutMode: CardLayoutMode = CardLayoutMode.List
) {
    val responsive = LocalElderResponsive.current
    SoftCard(
        modifier = modifier.clickable(onClick = onClick),
        elevation = 1.dp
    ) {
        if (layoutMode == CardLayoutMode.Grid) {
            Column(
                modifier = Modifier.padding(responsive.cardPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(responsive.smallSpacing)
            ) {
                IconBadge(icon = icon, tint = tint, background = tint.copy(alpha = 0.11f), size = responsive.iconMedium)
                Text(
                    text = title,
                    color = ElderText,
                    fontSize = responsive.cardTitle,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    color = ElderTextMuted,
                    fontSize = responsive.label,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Row(
                modifier = Modifier.padding(responsive.cardPadding),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
            ) {
                IconBadge(icon = icon, tint = tint, background = tint.copy(alpha = 0.11f), size = responsive.iconMedium)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        color = ElderText,
                        fontSize = responsive.cardTitle,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(responsive.smallSpacing))
                    Text(
                        text = subtitle,
                        color = ElderTextMuted,
                        fontSize = responsive.label,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
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
    val responsive = LocalElderResponsive.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = responsive.smallSpacing),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(responsive.rowSpacing)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = ElderBlue, modifier = Modifier.size(responsive.iconSmall))
        Text(
            text = label,
            color = ElderText,
            fontSize = responsive.label,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = responsive.body,
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
