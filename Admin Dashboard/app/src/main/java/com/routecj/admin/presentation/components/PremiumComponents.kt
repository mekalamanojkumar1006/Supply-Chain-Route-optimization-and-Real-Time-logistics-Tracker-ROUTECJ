package com.routecj.admin.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

/**
 * Premium Bento Card for Dashboard, Grids, and Lists.
 * Features large 24dp corner radius, subtle shadow, and press scale animation.
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    backgroundColor: Color = Color.White,
    borderColor: Color = Color(0xFFF1F5F9),
    content: @Composable ColumnScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.98f else 1f, label = "card_scale")

    Card(
        modifier = modifier
            .scale(scale)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = androidx.compose.foundation.LocalIndication.current,
                        onClick = onClick
                    )
                } else Modifier
            )
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(24.dp),
                spotColor = Color(0x0D000000)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

/**
 * Spatial Surface for floating map overlays.
 */
@Composable
fun SpatialSurface(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.White.copy(alpha = 0.96f),
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(16.dp, RoundedCornerShape(24.dp), spotColor = Color.Black.copy(alpha = 0.12f))
            .border(0.5.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(24.dp)),
        color = backgroundColor,
        shape = RoundedCornerShape(24.dp),
        content = { Box(modifier = Modifier.padding(20.dp), content = content) }
    )
}

/**
 * Bento Stat Card for 2x2 Command Center metrics.
 */
@Composable
fun PremiumStatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    onClick: () -> Unit = {}
) {
    BentoCard(
        modifier = modifier.defaultMinSize(minHeight = 130.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(iconColor.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = iconColor, modifier = Modifier.size(20.dp))
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate to $title",
                tint = Color.LightGray.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.displaySmall.copy(
                fontWeight = FontWeight.ExtraBold,
                fontSize = 30.sp,
                color = MaterialTheme.colorScheme.onSurface,
                letterSpacing = (-0.5).sp
            )
        )

        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )

        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = iconColor,
                    fontWeight = FontWeight.SemiBold
                ),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * High-density Quick Operations Action Card.
 */
@Composable
fun PremiumActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BentoCard(
        modifier = modifier.defaultMinSize(minHeight = 88.dp), // Ensure touch target and visibility
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), // Min touch target height
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Primary.copy(alpha = 0.12f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = Primary, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 15.sp
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Action arrow",
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * Compact Operational Card for Active Shipments in Live Operations.
 */
@Composable
fun PremiumShipmentCard(
    orderNumber: String,
    origin: String,
    destination: String,
    driverName: String,
    vehicleRegistration: String,
    statusText: String,
    statusColor: Color,
    etaText: String,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    BentoCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "ACTIVE SHIPMENT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Primary,
                        letterSpacing = 1.sp
                    )
                )
                Text(
                    text = "• $orderNumber",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = Secondary
                    )
                )
            }
            PremiumStatusChip(text = statusText, color = statusColor)
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Route details
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = origin,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "to",
                modifier = Modifier.size(16.dp),
                tint = Primary
            )
            Text(
                text = destination,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 16.sp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Driver & Vehicle details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(text = "Driver: $driverName", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.Gray)
                Text(text = "Vehicle: $vehicleRegistration", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Route Progress Visual Bar: ●────●────🚚────○
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "ETA: $etaText", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Primary)
                Text(text = "${(progressFraction * 100).toInt()}% COMPLETED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progressFraction.coerceIn(0.05f, 1f))
                        .clip(CircleShape)
                        .background(Primary)
                )
            }
        }
    }
}

/**
 * Minimalist Status Chip with indicator dot.
 */
@Composable
fun PremiumStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    // Determine a higher contrast text color if the primary color is too light
    // For this theme, we'll use a slightly darkened version for text on pale backgrounds
    val textColor = if (color == Primary) Color(0xFF008B86) else color

    Surface(
        color = color.copy(alpha = 0.12f), // Increased alpha for better background visibility
        shape = CircleShape,
        modifier = modifier.border(0.5.dp, color.copy(alpha = 0.3f), CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), // Increased vertical padding
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Black, // Extra bold
                    color = textColor,
                    letterSpacing = 0.5.sp,
                    fontSize = 11.sp // Slightly larger
                )
            )
        }
    }
}

/**
 * Global High-Contrast Color Palette for all Input TextFields in RouteCJ Admin.
 * Ensures dark, readable navy/black text on light surfaces and high visibility in all states.
 */
@Composable
fun routeCJTextFieldColors(
    containerColor: Color = Color.White,
    textColor: Color = Color(0xFF0F172A),
    unfocusedBorderColor: Color = Color(0xFFCBD5E1)
) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = textColor,
    unfocusedTextColor = textColor,
    disabledTextColor = Color(0xFF94A3B8),
    errorTextColor = Color(0xFFEF4444),
    focusedContainerColor = containerColor,
    unfocusedContainerColor = containerColor,
    disabledContainerColor = containerColor.copy(alpha = 0.6f),
    errorContainerColor = containerColor,
    cursorColor = Primary,
    errorCursorColor = Color(0xFFEF4444),
    focusedBorderColor = Primary,
    unfocusedBorderColor = unfocusedBorderColor,
    disabledBorderColor = Color(0xFFE2E8F0),
    errorBorderColor = Color(0xFFEF4444),
    focusedLabelColor = Primary,
    unfocusedLabelColor = Color(0xFF64748B),
    disabledLabelColor = Color(0xFF94A3B8),
    errorLabelColor = Color(0xFFEF4444),
    focusedPlaceholderColor = Color(0xFF94A3B8),
    unfocusedPlaceholderColor = Color(0xFF94A3B8),
    disabledPlaceholderColor = Color(0xFFCBD5E1),
    focusedLeadingIconColor = Primary,
    unfocusedLeadingIconColor = Color(0xFF64748B),
    focusedTrailingIconColor = Primary,
    unfocusedTrailingIconColor = Color(0xFF64748B)
)

/**
 * Premium Search Bar with bento styling and high-contrast typography.
 */
@Composable
fun PremiumSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF94A3B8)) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF64748B)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color(0xFF64748B))
                }
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x0D000000)),
        shape = RoundedCornerShape(16.dp),
        singleLine = true,
        colors = routeCJTextFieldColors(
            containerColor = Color.White,
            textColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        )
    )
}

/**
 * Premium Section Header.
 */
@Composable
fun PremiumSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 0.2.sp,
                fontSize = 18.sp
            )
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = Primary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

/**
 * Filter Chip Component.
 */
@Composable
fun PremiumFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
            )
        },
        shape = RoundedCornerShape(12.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Primary.copy(alpha = 0.12f),
            selectedLabelColor = Primary,
            containerColor = Color.White,
            labelColor = Color.DarkGray
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) Primary.copy(alpha = 0.3f) else Color(0xFFE2E8F0)
        ),
        modifier = modifier
    )
}

/**
 * Premium Empty State with illustrations/icons.
 */
@Composable
fun PremiumEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Default.Inbox
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp), // Reduced from 48dp
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant, // Better contrast
                fontWeight = FontWeight.Bold, // Bolder
                textAlign = TextAlign.Center,
                lineHeight = 24.sp
            )
        )
    }
}

/**
 * Skeleton / Progress Loading State.
 */
@Composable
fun PremiumLoadingState(
    message: String = "Synchronizing logistics network...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Primary, strokeWidth = 3.dp)
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

/**
 * Error State with Retry Button.
 */
@Composable
fun PremiumErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Retry Connection", fontWeight = FontWeight.Bold)
            }
        }
    }
}
