package com.routecj.admin.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Loading indicator composable.
 * Displayed during asynchronous operations.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier,
    message: String? = null
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        if (!message.isNullOrEmpty()) {
            Text(
                text = message,
                modifier = Modifier,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Error state composable.
 * Displayed when an error occurs during an operation.
 */
@Composable
fun ErrorScreen(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit = {}
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "⚠️ Error",
            fontSize = 20.sp,
            modifier = Modifier
        )
        Text(
            text = message,
            fontSize = 14.sp,
            modifier = Modifier,
            textAlign = TextAlign.Center,
            color = Color.Red
        )
    }
}

/**
 * Empty state composable.
 * Displayed when there's no data to show.
 */
@Composable
fun EmptyState(
    modifier: Modifier = Modifier,
    message: String = "No data available"
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "📭",
            fontSize = 48.sp,
            modifier = Modifier
        )
        Text(
            text = message,
            fontSize = 14.sp,
            modifier = Modifier,
            textAlign = TextAlign.Center
        )
    }
}

