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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val ElderBlue = Color(0xFF1F5D83)
val ElderBlueLight = Color(0xFF3396D1)
val ElderDeepText = Color(0xFF0F4F76)
val ElderBackground = Color(0xFFF5F7FA)
val ElderSoftCard = Color(0xFFF4F7FA)
val ElderGreen = Color(0xFF24A55A)
val ElderOrange = Color(0xFFF49312)
val ElderRed = Color(0xFFE34235)

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = text,
            color = ElderDeepText,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(76.dp)
                .height(4.dp)
                .background(ElderBlue, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
fun PillButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val background = if (selected) Color.White else Color.White.copy(alpha = 0.16f)
    val textColor = if (selected) ElderDeepText else Color.White
    Box(
        modifier = modifier
            .height(58.dp)
            .shadow(if (selected) 8.dp else 0.dp, RoundedCornerShape(28.dp))
            .background(background, RoundedCornerShape(28.dp))
            .border(
                BorderStroke(2.dp, Color.White.copy(alpha = if (selected) 0f else 0.45f)),
                RoundedCornerShape(28.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = textColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun PrimaryActionButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 76.dp
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(10.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        border = if (borderColor == Color.Transparent) null else BorderStroke(2.dp, borderColor),
        colors = CardDefaults.cardColors(containerColor = ElderSoftCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        content = content
    )
}

@Composable
fun InfoRow(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        color = Color(0xFF172431),
        fontSize = 21.sp
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFFD9DEE4))
    )
}

@Composable
fun TwoColumnRow(
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(modifier = Modifier.weight(1f)) { left() }
        Box(modifier = Modifier.weight(1f)) { right() }
    }
}

@Composable
fun ProgressDot(active: Boolean) {
    Box(
        modifier = Modifier
            .size(13.dp)
            .background(if (active) ElderBlue else Color(0xFFD9D9D9), CircleShape)
    )
}
