package com.routecj.customer.presentation.components.animations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay

/**
 * Standard spring physics for RouteCJ animations.
 * Provides a smooth, premium bounce without excessive rubber-banding.
 */
fun <T> routeCJSpring(): SpringSpec<T> = spring(
    dampingRatio = 0.8f,
    stiffness = 400f
)

/**
 * Modifier that scales a component down slightly when pressed.
 * Ideal for Cards and List Items.
 */
fun Modifier.animatedPress(
    scaleDown: Float = 0.98f,
    interactionSource: InteractionSource? = null
): Modifier = composed {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = routeCJSpring(),
        label = "press_scale"
    )

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * Reusable animated entrance container for staggered list items or screen content.
 * Fades in and slides up smoothly.
 */
@Composable
fun AnimatedEntrance(
    index: Int = 0,
    delayMillis: Int = 50,
    durationMillis: Int = 400,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay((index * delayMillis).toLong())
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis)) + 
                slideInVertically(
                    initialOffsetY = { 40 },
                    animationSpec = tween(durationMillis, easing = FastOutSlowInEasing)
                ),
        modifier = modifier
    ) {
        Box(content = content)
    }
}

/**
 * Crossfade animation for small state changes (like icons).
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun <T> AnimatedIconTransition(
    targetState: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = targetState,
        transitionSpec = {
            fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
        },
        modifier = modifier,
        label = "icon_transition"
    ) { state ->
        content(state)
    }
}
