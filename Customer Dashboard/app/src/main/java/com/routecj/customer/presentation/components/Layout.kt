package com.routecj.customer.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.routecj.customer.ui.theme.RouteCJShapes
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder

@Composable
fun RouteCJCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    border: BorderStroke? = BorderStroke(1.dp, StitchTonalBorder),
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = shape,
            color = containerColor,
            border = border
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun RouteCJStepProgressHeader(
    currentStep: Int,
    totalSteps: Int = 4,
    stepTitle: String,
    modifier: Modifier = Modifier
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 400),
        label = "step_progress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Step $currentStep of $totalSteps",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stepTitle,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteCJTopBar(
    title: String,
    onBackClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { 
            Text(
                text = title, 
                style = MaterialTheme.typography.titleLarge
            ) 
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun RouteCJSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

