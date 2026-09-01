package com.routecj.driver.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.ui.theme.RouteCJCyan
import com.routecj.driver.ui.theme.RouteCJError
import com.routecj.driver.ui.theme.RouteCJNavyDark
import com.routecj.driver.ui.theme.RouteCJSuccess
import com.routecj.driver.ui.theme.RouteCJWarning
import com.routecj.driver.ui.theme.RouteCJWhite

enum class BadgeType {
    SUCCESS, WARNING, ERROR, NEUTRAL, INFO
}

@Composable
fun StatusBadge(
    text: String,
    type: BadgeType,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (type) {
        BadgeType.SUCCESS -> RouteCJSuccess to RouteCJWhite
        BadgeType.WARNING -> RouteCJWarning to RouteCJNavyDark
        BadgeType.ERROR -> RouteCJError to RouteCJWhite
        BadgeType.INFO -> RouteCJCyan to RouteCJNavyDark
        BadgeType.NEUTRAL -> Color(0xFF64748B) to RouteCJWhite
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text.uppercase(),
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
