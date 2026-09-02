package com.routecj.customer.presentation.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.StitchTonalBorder

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToEditProfile: (Customer) -> Unit,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Profile & Settings",
                onBackClick = onNavigateBack
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val currentState = state) {
                is ProfileState.Loading -> {
                    RouteCJLoading(modifier = Modifier.padding(16.dp))
                }
                is ProfileState.Error -> {
                    RouteCJErrorState(
                        message = currentState.message,
                        onRetry = viewModel::loadProfile,
                        modifier = Modifier.padding(16.dp)
                    )
                }
                is ProfileState.Success -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Avatar with Edit Badge
                        Box(contentAlignment = Alignment.BottomEnd) {
                            RouteCJAvatar(
                                name = currentState.customer.name,
                                size = 96.dp
                            )
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable { onNavigateToEditProfile(currentState.customer) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = currentState.customer.name ?: "Customer",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = currentState.customer.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val phone = currentState.customer.phoneNumber
                        if (!phone.isNullOrBlank()) {
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // 2-Column Stats Summary
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, StitchTonalBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Deliveries",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "24",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Surface(
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = BorderStroke(1.dp, StitchTonalBorder)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "Rating",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "5.0 ★",
                                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Group 1: Account Settings
                        Text(
                            text = "Account Settings",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, StitchTonalBorder)
                        ) {
                            Column {
                                ProfileMenuItem(
                                    icon = Icons.Default.Person,
                                    title = "Edit Profile",
                                    onClick = { onNavigateToEditProfile(currentState.customer) }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ProfileMenuItem(
                                    icon = Icons.Default.LocationOn,
                                    title = "Saved Addresses",
                                    onClick = { /* Saved addresses */ }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ProfileMenuItem(
                                    icon = Icons.Default.CreditCard,
                                    title = "Payment Methods",
                                    onClick = { /* Payment methods */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Group 2: Preferences
                        Text(
                            text = "Preferences",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, StitchTonalBorder)
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.DarkMode,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Text(
                                            text = "Dark Mode",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Switch(
                                        checked = isDarkMode,
                                        onCheckedChange = { viewModel.toggleDarkMode() }
                                    )
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ProfileMenuItem(
                                    icon = Icons.Default.NotificationsNone,
                                    title = "Push Notifications",
                                    onClick = { /* Notifications toggle */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Group 3: Support & Legal
                        Text(
                            text = "Support & Legal",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp)
                        )

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            border = BorderStroke(1.dp, StitchTonalBorder)
                        ) {
                            Column {
                                ProfileMenuItem(
                                    icon = Icons.AutoMirrored.Filled.HelpOutline,
                                    title = "Help & Support",
                                    onClick = { /* Support */ }
                                )
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                                ProfileMenuItem(
                                    icon = Icons.Default.Description,
                                    title = "Terms of Service",
                                    onClick = { /* Terms */ }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Sign Out Button
                        Surface(
                            onClick = { viewModel.signOut() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "Sign Out",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Sign Out",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp)
        )
    }
}
