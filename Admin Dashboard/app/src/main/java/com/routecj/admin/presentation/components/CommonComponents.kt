package com.routecj.admin.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Reusable composable components for the application.
 * This file contains commonly used UI components.
 *
 * Benefits of centralizing components:
 * - Consistency across the app
 * - Easy to maintain and update
 * - Reduces code duplication
 * - Single source of truth for component design
 */

/**
 * Professional Password TextField with Show/Hide functionality.
 */
@Composable
fun RouteCJPasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null
) {
    var passwordVisible by rememberSaveable { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (passwordVisible) "Hide Password" else "Show Password"

            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(imageVector = image, contentDescription = description, tint = Color(0xFF64748B))
            }
        },
        supportingText = error?.let { { Text(text = it, color = Color(0xFFEF4444)) } },
        shape = RoundedCornerShape(12.dp),
        colors = routeCJTextFieldColors(
            containerColor = Color.White,
            textColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )
}

/**
 * Order Card Component.
 * Displays order information in a card format.
 */
@Composable
fun OrderCard(
    orderNumber: String,
    customerName: String,
    status: String,
    totalAmount: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = orderNumber,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                StatusBadge(status = status)
            }

            Text(
                text = customerName,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "₹$totalAmount",
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Status Badge Component.
 * Displays status with appropriate color coding.
 */
@Composable
fun StatusBadge(
    status: String,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, textColor) = when (status.uppercase()) {
        "PENDING" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        "ASSIGNED" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        "IN_TRANSIT" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        "DELIVERED" -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        "CANCELLED" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Text(
        text = status,
        modifier = modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = textColor
    )
}

/**
 * Info Row Component.
 * Displays label and value pairs.
 */
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * Empty state Component for sections with no data.
 */
@Composable
fun SectionEmptyState(
    message: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📭",
            fontSize = 48.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

