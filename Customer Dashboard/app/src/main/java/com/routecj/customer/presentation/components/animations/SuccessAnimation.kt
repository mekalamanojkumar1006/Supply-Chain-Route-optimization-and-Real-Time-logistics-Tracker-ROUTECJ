package com.routecj.customer.presentation.components.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color

@Composable
fun SuccessAnimation(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    var startAnimation by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1.2f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.scale(scale),
            tint = color
        )
    }
}
