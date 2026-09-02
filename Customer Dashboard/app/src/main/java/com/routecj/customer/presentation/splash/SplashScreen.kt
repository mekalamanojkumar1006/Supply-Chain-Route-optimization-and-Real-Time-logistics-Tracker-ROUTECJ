package com.routecj.customer.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.routecj.customer.presentation.navigation.Screen
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: () -> Unit,
    viewModel: SplashViewModel = hiltViewModel(),
) {
    val isInitialized by viewModel.isInitialized.collectAsStateWithLifecycle()
    val startDestination by viewModel.startDestination.collectAsStateWithLifecycle()

    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val vanWidthDp = 180.dp
    val vanWidthPx = with(density) { vanWidthDp.toPx() }

    // Animation States
    val vanX = remember { Animatable(-vanWidthPx * 1.5f) }
    val vanScale = remember { Animatable(0.85f) }
    val suspensionBounce = remember { Animatable(0f) }
    val roadAlpha = remember { Animatable(0f) }
    val speedLinesAlpha = remember { Animatable(0f) }

    // Parcel Physics
    val parcelDropY = remember { Animatable(-50f) }
    val parcelAlpha = remember { Animatable(0f) }
    val parcelScale = remember { Animatable(0.6f) }

    // Branding & Logo Reveal
    val brandAlpha = remember { Animatable(0f) }
    val brandOffsetY = remember { Animatable(25f) }
    val brandScale = remember { Animatable(0.95f) }
    val glowPulse = remember { Animatable(0.5f) }

    // Road motion
    val roadAnim = rememberInfiniteTransition(label = "roadTransition")
    val roadOffset by roadAnim.animateFloat(
        initialValue = 0f,
        targetValue = -200f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "roadOffset"
    )

    // Wheel rotation
    val wheelAnim = rememberInfiniteTransition(label = "wheelRotation")
    val wheelRotation by wheelAnim.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(450, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wheelRotation"
    )

    var isVanMoving by remember { mutableStateOf(value = true) }

    // Main Animation Choreography
    LaunchedEffect(Unit) {
        delay(200.milliseconds)

        launch { roadAlpha.animateTo(1f, tween(400)) }
        launch { speedLinesAlpha.animateTo(1f, tween(300)) }
        launch { vanScale.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }

        vanX.animateTo(
            targetValue = (screenWidthPx - vanWidthPx) / 2,
            animationSpec = tween(1700, easing = FastOutSlowInEasing)
        )
        isVanMoving = false
        launch { speedLinesAlpha.animateTo(0f, tween(200)) }
        launch { roadAlpha.animateTo(0.35f, tween(600)) }

        launch {
            suspensionBounce.animateTo(3f, tween(120, easing = LinearOutSlowInEasing))
            suspensionBounce.animateTo(-2f, tween(120, easing = FastOutSlowInEasing))
            suspensionBounce.animateTo(0f, tween(150, easing = FastOutSlowInEasing))
        }

        launch { parcelAlpha.animateTo(1f, tween(300)) }
        launch { parcelScale.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMedium)) }
        parcelDropY.animateTo(0f, tween(500, easing = BounceEnrichedEasing))

        delay(150.milliseconds)
        launch { brandAlpha.animateTo(1f, tween(600, easing = LinearOutSlowInEasing)) }
        launch { brandOffsetY.animateTo(0f, tween(600, easing = FastOutSlowInEasing)) }
        launch { brandScale.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
        launch {
            glowPulse.animateTo(
                1f,
                infiniteRepeatable(
                    animation = tween(900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        }
    }

    // Navigation Handling
    LaunchedEffect(isInitialized, startDestination) {
        if (isInitialized && startDestination != null) {
            delay(300.milliseconds)
            if (startDestination == Screen.Home.route) {
                onNavigateToHome()
            } else {
                onNavigateToLogin()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A),
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        RoadTrack(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .align(Alignment.Center)
                .offset(y = 52.dp)
                .alpha(roadAlpha.value),
            roadOffset = if (isVanMoving) roadOffset else 0f
        )

        if (speedLinesAlpha.value > 0f) {
            SpeedWindLines(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset { IntOffset((vanX.value - 60.dp.toPx()).toInt(), -10.dp.toPx().toInt()) }
                    .size(width = 90.dp, height = 70.dp)
                    .alpha(speedLinesAlpha.value)
            )
        }

        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = vanX.value.toInt(),
                        y = (-30.dp.toPx() + suspensionBounce.value.dp.toPx()).toInt()
                    )
                }
                .size(width = vanWidthDp, height = 130.dp)
                .scale(vanScale.value)
        ) {
            ModernLogisticsVan(
                modifier = Modifier.fillMaxSize(),
                parcelDropY = parcelDropY.value,
                parcelAlpha = parcelAlpha.value,
                parcelScale = parcelScale.value,
                wheelRotation = if (isVanMoving) wheelRotation else 0f
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 90.dp + brandOffsetY.value.dp)
                .alpha(brandAlpha.value)
                .scale(brandScale.value),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Route",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "CJ",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Color(0xFF60A5FA),
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 6.dp)
                        .size(8.dp)
                        .background(
                            color = Color(0xFF60A5FA).copy(alpha = glowPulse.value),
                            shape = androidx.compose.foundation.shape.CircleShape
                        )
                )
            }

            Text(
                text = "LOGISTICS & EXPRESS DELIVERY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF94A3B8),
                letterSpacing = 2.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private val BounceEnrichedEasing = Easing { fraction ->
    val t = 1f - fraction
    1f - (t * t * (2.70158f * t - 1.70158f))
}

@Composable
fun RoadTrack(
    modifier: Modifier = Modifier,
    roadOffset: Float
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 2.5.dp.toPx()
        val primaryRoadColor = Color(0xFF475569)
        val asphaltBaseColor = Color(0xFF334155)
        val dashWidth = 50.dp.toPx()
        val gapWidth = 30.dp.toPx()

        drawLine(
            color = asphaltBaseColor,
            start = Offset(0f, size.height * 0.65f),
            end = Offset(size.width, size.height * 0.65f),
            strokeWidth = 1.dp.toPx()
        )

        var x = (roadOffset % (dashWidth + gapWidth))
        while (x < size.width + dashWidth) {
            drawLine(
                color = primaryRoadColor,
                start = Offset(x, size.height * 0.65f),
                end = Offset(x + dashWidth, size.height * 0.65f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            x += dashWidth + gapWidth
        }
    }
}

@Composable
fun SpeedWindLines(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val windColor = Color(0xFF60A5FA).copy(alpha = 0.4f)
        val stroke = 1.5.dp.toPx()

        drawLine(
            color = windColor,
            start = Offset(0f, size.height * 0.3f),
            end = Offset(size.width * 0.7f, size.height * 0.3f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = windColor.copy(alpha = 0.25f),
            start = Offset(size.width * 0.2f, size.height * 0.5f),
            end = Offset(size.width * 0.9f, size.height * 0.5f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
        drawLine(
            color = windColor.copy(alpha = 0.35f),
            start = Offset(size.width * 0.1f, size.height * 0.7f),
            end = Offset(size.width * 0.8f, size.height * 0.7f),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )
    }
}

@Composable
fun ModernLogisticsVan(
    modifier: Modifier = Modifier,
    parcelDropY: Float,
    parcelAlpha: Float,
    parcelScale: Float,
    wheelRotation: Float
) {
    Canvas(modifier = modifier) {
        val darkNavy = Color(0xFF0F172A)
        val cyanAccent = Color(0xFF60A5FA)
        val bodySilver = Color(0xFF1E293B)
        val glassTint = Color(0xFF38BDF8).copy(alpha = 0.35f)
        val parcelColor = Color(0xFFD97706)

        val w = size.width
        val h = size.height
        val vanGroundY = h * 0.82f

        val cargoLeft = w * 0.05f
        val cargoTop = h * 0.22f
        val cargoWidth = w * 0.62f
        val cargoHeight = vanGroundY - cargoTop - 12.dp.toPx()

        drawRoundRect(
            color = bodySilver,
            topLeft = Offset(cargoLeft, cargoTop),
            size = Size(cargoWidth, cargoHeight),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Fill
        )
        drawRoundRect(
            color = cyanAccent.copy(alpha = 0.6f),
            topLeft = Offset(cargoLeft, cargoTop),
            size = Size(cargoWidth, cargoHeight),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 2.2.dp.toPx())
        )

        val stripeY = cargoTop + cargoHeight * 0.52f
        drawRect(
            color = cyanAccent,
            topLeft = Offset(cargoLeft + 2.dp.toPx(), stripeY),
            size = Size(cargoWidth - 4.dp.toPx(), 6.dp.toPx())
        )

        val cabinLeft = cargoLeft + cargoWidth - 1.dp.toPx()
        val cabinWidth = w * 0.28f
        val cabinTop = cargoTop + 8.dp.toPx()
        val cabinHeight = cargoHeight - 8.dp.toPx()

        val cabinPath = Path().apply {
            moveTo(cabinLeft, cabinTop)
            lineTo(cabinLeft + cabinWidth * 0.45f, cabinTop)
            cubicTo(
                cabinLeft + cabinWidth * 0.75f, cabinTop + 2.dp.toPx(),
                cabinLeft + cabinWidth * 0.95f, cabinTop + cabinHeight * 0.4f,
                cabinLeft + cabinWidth, cabinTop + cabinHeight * 0.65f
            )
            lineTo(cabinLeft + cabinWidth, cabinTop + cabinHeight)
            lineTo(cabinLeft, cabinTop + cabinHeight)
            close()
        }

        drawPath(cabinPath, color = bodySilver, style = Fill)
        drawPath(cabinPath, color = cyanAccent.copy(alpha = 0.6f), style = Stroke(width = 2.2.dp.toPx()))

        val windowPath = Path().apply {
            moveTo(cabinLeft + 6.dp.toPx(), cabinTop + 5.dp.toPx())
            lineTo(cabinLeft + cabinWidth * 0.38f, cabinTop + 5.dp.toPx())
            cubicTo(
                cabinLeft + cabinWidth * 0.62f, cabinTop + 7.dp.toPx(),
                cabinLeft + cabinWidth * 0.76f, cabinTop + cabinHeight * 0.35f,
                cabinLeft + cabinWidth * 0.8f, cabinTop + cabinHeight * 0.5f
            )
            lineTo(cabinLeft + 6.dp.toPx(), cabinTop + cabinHeight * 0.5f)
            close()
        }
        drawPath(windowPath, color = glassTint, style = Fill)

        if (parcelAlpha > 0f) {
            val parcelW = 24.dp.toPx() * parcelScale
            val parcelH = 20.dp.toPx() * parcelScale
            val parcelX = cargoLeft + 18.dp.toPx()
            val parcelY = (cargoTop + cargoHeight - parcelH - 4.dp.toPx()) + (parcelDropY * parcelScale)

            drawRoundRect(
                color = parcelColor.copy(alpha = parcelAlpha),
                topLeft = Offset(parcelX, parcelY),
                size = Size(parcelW, parcelH),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
            )
        }

        val wheelRadius = 13.dp.toPx()
        val wheelY = vanGroundY - wheelRadius + 2.dp.toPx()

        drawWheelWithSpokes(
            center = Offset(cargoLeft + cargoWidth * 0.28f, wheelY),
            radius = wheelRadius,
            rotationAngle = wheelRotation,
            darkNavy = darkNavy,
            cyanAccent = cyanAccent
        )

        drawWheelWithSpokes(
            center = Offset(cabinLeft + cabinWidth * 0.65f, wheelY),
            radius = wheelRadius,
            rotationAngle = wheelRotation,
            darkNavy = darkNavy,
            cyanAccent = cyanAccent
        )
    }
}

private fun DrawScope.drawWheelWithSpokes(
    center: Offset,
    radius: Float,
    rotationAngle: Float,
    darkNavy: Color,
    cyanAccent: Color
) {
    drawCircle(
        color = darkNavy,
        radius = radius,
        center = center,
        style = Fill
    )

    val rimRadius = radius * 0.62f
    drawCircle(
        color = Color(0xFF475569),
        radius = rimRadius,
        center = center,
        style = Fill
    )

    val spokeCount = 4
    val angleStep = (2 * PI / spokeCount).toFloat()
    val radOffset = (rotationAngle * PI / 180f).toFloat()

    for (i in 0 until spokeCount) {
        val angle = radOffset + (i * angleStep)
        val endX = center.x + cos(angle) * (rimRadius - 1.dp.toPx())
        val endY = center.y + sin(angle) * (rimRadius - 1.dp.toPx())

        drawLine(
            color = Color.White,
            start = center,
            end = Offset(endX, endY),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round
        )
    }

    drawCircle(
        color = cyanAccent,
        radius = radius * 0.22f,
        center = center
    )
}
