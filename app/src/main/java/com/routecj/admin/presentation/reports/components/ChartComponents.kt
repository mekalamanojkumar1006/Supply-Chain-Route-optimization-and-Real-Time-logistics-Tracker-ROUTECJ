package com.routecj.admin.presentation.reports.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.admin.ui.theme.Primary

/**
 * A Donut Chart to visualize proportions (e.g. Godown Occupancy).
 */

@Composable
fun DonutChart(
    percentage: Float,
    centerText: String,
    color: Color = Primary,
    modifier: Modifier = Modifier.size(130.dp)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = percentage.coerceIn(0f, 100f),
        animationSpec = tween(durationMillis = 800),
        label = "donut_anim"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            val strokeWidth = 10.dp.toPx()
            
            // Background Circle
            drawArc(
                color = Color(0xFF334155).copy(alpha = 0.3f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
            
            // Foreground Arc
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 3.6f * animatedProgress,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
        
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${percentage.toInt()}%",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
            )
            Text(
                text = centerText,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}

/**
 * Bar Chart for time-series or categorical order/delivery trends.
 */
@Composable
fun BarChart(
    data: List<Pair<String, Int>>,
    modifier: Modifier = Modifier.fillMaxWidth().height(160.dp),
    barColor: Color = Primary
) {
    if (data.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Text("No activity data for selected period", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        }
        return
    }

    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1).coerceAtLeast(1)
    
    Column(modifier = modifier) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val totalBars = data.size
                val barWidth = (size.width / (totalBars * 1.8f)).coerceIn(12.dp.toPx(), 36.dp.toPx())
                val availableWidth = size.width
                val spacing = if (totalBars > 1) (availableWidth - (barWidth * totalBars)) / (totalBars + 1) else (availableWidth - barWidth) / 2
                
                // Horizontal reference grid line
                drawLine(
                    color = Color(0xFF334155).copy(alpha = 0.5f),
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )

                data.forEachIndexed { index, pair ->
                    val fraction = (pair.second.toFloat() / maxVal).coerceIn(0.05f, 1f)
                    val barHeight = fraction * (size.height - 10.dp.toPx())
                    val x = spacing + index * (barWidth + spacing)
                    val y = size.height - barHeight
                    
                    // Bar background
                    drawRoundRect(
                        color = Color(0xFF334155).copy(alpha = 0.2f),
                        topLeft = Offset(x, 0f),
                        size = Size(barWidth, size.height),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Active Bar
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                }
            }
        }
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            data.forEach {
                Text(
                    text = it.first,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 10.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Status Segmented Progress Bar for status distribution breakdowns.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatusDistributionBar(
    segments: List<Pair<String, Int>>,
    colorMap: (String) -> Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val total = segments.sumOf { it.second }.coerceAtLeast(1)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Multi-color segmented bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFF334155))
        ) {
            segments.filter { it.second > 0 }.forEach { (name, count) ->
                val weight = count.toFloat() / total
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .weight(weight)
                        .background(colorMap(name))
                )
            }
        }

        // Legend tags
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            segments.filter { it.second > 0 }.forEach { (name, count) ->
                val pct = ((count.toDouble() / total) * 100).toInt()
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(8.dp).background(colorMap(name), CircleShape))
                    Text(
                        text = "${name.replace("_", " ")}: $count ($pct%)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.LightGray
                    )
                }
            }
        }
    }
}

/**
 * Color indicators for legends.
 */
@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
        Box(modifier = Modifier.size(10.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium, fontSize = 12.sp)
    }
}

