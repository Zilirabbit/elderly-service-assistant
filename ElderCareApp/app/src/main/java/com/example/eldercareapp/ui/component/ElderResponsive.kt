package com.example.eldercareapp.ui.component

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class ElderWindowSizeClass {
    Compact,
    Medium,
    Expanded
}

data class ElderResponsiveSpec(
    val windowSizeClass: ElderWindowSizeClass,
    val fontScale: Float,
    val effectiveWidth: Dp,
    val isLargeText: Boolean,
    val isExtraLargeText: Boolean,
    val contentMaxWidth: Dp,
    val chatContentMaxWidth: Dp,
    val pagePadding: Dp,
    val pageSpacing: Dp,
    val cardPadding: Dp,
    val cardSpacing: Dp,
    val rowSpacing: Dp,
    val smallSpacing: Dp,
    val cardCorner: Dp,
    val controlCorner: Dp,
    val buttonMinHeight: Dp,
    val compactButtonMinHeight: Dp,
    val segmentedMinHeight: Dp,
    val inputMinHeight: Dp,
    val iconButtonSize: Dp,
    val iconLarge: Dp,
    val iconMedium: Dp,
    val iconSmall: Dp,
    val topBarTitle: TextUnit,
    val topBarSubtitle: TextUnit,
    val sectionTitle: TextUnit,
    val cardTitle: TextUnit,
    val body: TextUnit,
    val bodyLarge: TextUnit,
    val label: TextUnit,
    val labelSmall: TextUnit,
    val serviceColumns: Int,
    val guideHeroIconSize: Dp,
    val showTopBarActionText: Boolean,
    val useCenterTitleTopBar: Boolean,
    val useSingleColumnCards: Boolean,
    val stackActionRows: Boolean,
    val compactTopBarActions: Boolean
)

val LocalElderResponsive = compositionLocalOf { elderResponsiveSpec(390.dp, 1f) }

fun elderResponsiveSpec(width: Dp, fontScale: Float): ElderResponsiveSpec {
    val effectiveWidth = width / fontScale.coerceAtLeast(1f)
    val sizeClass = when {
        effectiveWidth < 600.dp -> ElderWindowSizeClass.Compact
        effectiveWidth < 840.dp -> ElderWindowSizeClass.Medium
        else -> ElderWindowSizeClass.Expanded
    }
    val largeText = fontScale >= 1.12f
    val extraLargeText = fontScale >= 1.25f
    val tightWidth = sizeClass == ElderWindowSizeClass.Compact
    val roomyWidth = sizeClass == ElderWindowSizeClass.Expanded
    val contentMaxWidth = when (sizeClass) {
        ElderWindowSizeClass.Compact -> Dp.Unspecified
        ElderWindowSizeClass.Medium -> 720.dp
        ElderWindowSizeClass.Expanded -> 1100.dp
    }
    val chatContentMaxWidth = when (sizeClass) {
        ElderWindowSizeClass.Compact -> Dp.Unspecified
        else -> 760.dp
    }
    val serviceColumns = when {
        extraLargeText && effectiveWidth < 840.dp -> 1
        largeText && effectiveWidth < 600.dp -> 1
        sizeClass == ElderWindowSizeClass.Compact -> 1
        sizeClass == ElderWindowSizeClass.Medium -> 3
        else -> 4
    }

    return ElderResponsiveSpec(
        windowSizeClass = sizeClass,
        fontScale = fontScale,
        effectiveWidth = effectiveWidth,
        isLargeText = largeText,
        isExtraLargeText = extraLargeText,
        contentMaxWidth = contentMaxWidth,
        chatContentMaxWidth = chatContentMaxWidth,
        pagePadding = when {
            tightWidth -> 14.dp
            roomyWidth -> 28.dp
            else -> 20.dp
        },
        pageSpacing = when {
            tightWidth -> 14.dp
            roomyWidth -> 22.dp
            else -> 18.dp
        },
        cardPadding = when {
            tightWidth -> 16.dp
            roomyWidth -> 22.dp
            else -> 20.dp
        },
        cardSpacing = when {
            tightWidth -> 12.dp
            roomyWidth -> 18.dp
            else -> 14.dp
        },
        rowSpacing = if (tightWidth) 10.dp else 12.dp,
        smallSpacing = if (tightWidth) 6.dp else 8.dp,
        cardCorner = 16.dp,
        controlCorner = 14.dp,
        buttonMinHeight = when {
            extraLargeText -> 62.dp
            largeText -> 58.dp
            else -> 54.dp
        },
        compactButtonMinHeight = when {
            extraLargeText -> 56.dp
            largeText -> 52.dp
            else -> 48.dp
        },
        segmentedMinHeight = when {
            extraLargeText -> 56.dp
            largeText -> 52.dp
            else -> 46.dp
        },
        inputMinHeight = when {
            extraLargeText -> 60.dp
            largeText -> 56.dp
            else -> 52.dp
        },
        iconButtonSize = if (extraLargeText) 58.dp else 52.dp,
        iconLarge = if (tightWidth) 58.dp else 64.dp,
        iconMedium = if (tightWidth) 50.dp else 56.dp,
        iconSmall = if (tightWidth) 22.dp else 24.dp,
        topBarTitle = if (tightWidth) 21.sp else 22.sp,
        topBarSubtitle = if (tightWidth) 16.sp else 17.sp,
        sectionTitle = if (tightWidth) 20.sp else 21.sp,
        cardTitle = if (tightWidth) 19.sp else 20.sp,
        body = if (tightWidth) 17.sp else 18.sp,
        bodyLarge = if (tightWidth) 18.sp else 19.sp,
        label = if (tightWidth) 15.sp else 16.sp,
        labelSmall = if (tightWidth) 13.sp else 14.sp,
        serviceColumns = serviceColumns,
        guideHeroIconSize = when {
            extraLargeText -> 96.dp
            tightWidth -> 104.dp
            else -> 120.dp
        },
        showTopBarActionText = !largeText && sizeClass != ElderWindowSizeClass.Compact,
        useCenterTitleTopBar = true,
        useSingleColumnCards = serviceColumns == 1,
        stackActionRows = tightWidth || extraLargeText,
        compactTopBarActions = largeText || tightWidth
    )
}
