package com.routecj.customer.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.routecj.customer.ui.theme.*

@Composable
fun RouteCJStatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, showPulse) = when (status.uppercase()) {
        "IN_TRANSIT", "IN TRANSIT" -> Triple(TertiaryDark.copy(alpha = 0.15f), TertiaryDark, true)
        "DELIVERED", "COMPLETED" -> Triple(SuccessGreen.copy(alpha = 0.15f), SuccessGreen, false)
        "BOOKED", "CONFIRMED", "DRIVER_ASSIGNED", "PICKED_UP" -> Triple(PrimaryDark.copy(alpha = 0.15f), PrimaryDark, true)
        "PENDING", "PENDING_APPROVAL" -> Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, false)
        "CANCELLED", "DELAYED", "FAILED" -> Triple(ErrorDark.copy(alpha = 0.15f), ErrorDark, false)
        else -> Triple(MaterialTheme.colorScheme.surfaceContainerHighest, MaterialTheme.colorScheme.onSurfaceVariant, false)
    }

    val displayStatus = when (status.uppercase()) {
        "IN_TRANSIT" -> "In Transit"
        "DRIVER_ASSIGNED" -> "Driver Assigned"
        "PICKED_UP" -> "Picked Up"
        "PENDING_APPROVAL" -> "Pending Approval"
        else -> status.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (showPulse) {
                val infiniteTransition = rememberInfiniteTransition(label = "badge_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(textColor)
                )
            }
            Text(
                text = displayStatus,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = textColor
            )
        }
    }
}

@Composable
fun RouteCJStatusChip(
    text: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(percent = 50),
        color = containerColor,
        contentColor = contentColor
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun RouteCJAvatar(
    name: String?,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    listOf(BrandPrimaryBlue, TertiaryDark)
                )
            )
            .padding(2.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        val initial = name?.takeIf { it.isNotBlank() }?.first()?.uppercase() ?: "A"
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

