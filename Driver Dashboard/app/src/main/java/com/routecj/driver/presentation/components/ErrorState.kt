package com.routecj.driver.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ErrorState(
    message: String,
    modifier: Modifier = Modifier,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        
        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            RouteCJButton(
                text = actionText,
                onClick = onAction,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}
