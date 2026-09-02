package com.routecj.admin.presentation.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.RouteCJPasswordTextField
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val admin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    
    var showPasswordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Preferences updated successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            showPasswordDialog = false
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. ACCOUNT & SECURITY Section
            item { 
                PremiumSettingsCard("ACCOUNT & SECURITY") {
                    SettingsItem(
                        title = "Change Password",
                        subtitle = "Rotate security credentials and passcodes",
                        icon = Icons.Default.Security,
                        onClick = { showPasswordDialog = true }
                    )
                }
            }
            
            // 2. NOTIFICATIONS Section
            item {
                PremiumSettingsCard("NOTIFICATIONS") {
                    PreferenceToggle(
                        title = "Global System Alerts",
                        checked = admin?.notificationsEnabled ?: true,
                        onCheckedChange = { viewModel.updateNotificationPreference(enabled = it) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    PreferenceToggle(
                        title = "Live Order Monitoring",
                        checked = admin?.orderAlertsEnabled ?: true,
                        onCheckedChange = { viewModel.updateNotificationPreference(orderAlerts = it) }
                    )
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    PreferenceToggle(
                        title = "Fleet Operations Alerts",
                        checked = admin?.dispatchAlertsEnabled ?: true,
                        onCheckedChange = { viewModel.updateNotificationPreference(dispatchAlerts = it) }
                    )
                }
            }

            // 3. PRIVACY & APPLICATION Section
            item {
                PremiumSettingsCard("PRIVACY & APPLICATION") {
                    SettingsItem(
                        title = "RouteCJ Privacy Policy",
                        subtitle = "Review official data protection and privacy policy",
                        icon = Icons.Default.PrivacyTip,
                        onClick = { 
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/routecj-privacy-policy-2026?usp=sharing"))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, "Browser interface unavailable", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("App Information", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = Secondary)
                            Text("RouteCJ Admin Enterprise Control", fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        }
                        Surface(color = Primary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                            Text(
                                text = "v${Constants.APP_VERSION}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = Primary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // 4. LOGOUT
            item {
                Button(
                    onClick = {
                        viewModel.logout()
                        navController.navigate(Constants.NavigationRoutes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("LOGOUT", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, letterSpacing = 1.sp)
                }
            }
        }
    }

    if (showPasswordDialog) {
        ChangePasswordDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { current, new -> viewModel.changePassword(current, new) }
        )
    }
}

@Composable
fun PremiumSettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    BentoCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Black,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 12.dp),
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
fun SettingsItem(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Primary.copy(alpha = 0.08f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Secondary)
                Text(subtitle, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
fun PreferenceToggle(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Secondary)
        Switch(
            checked = checked, 
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color.LightGray.copy(alpha = 0.3f),
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Password", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Enter your active passcode and specify a new password.", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                RouteCJPasswordTextField(value = current, onValueChange = { current = it }, label = "Current Password")
                RouteCJPasswordTextField(value = new, onValueChange = { new = it }, label = "New Password")
                RouteCJPasswordTextField(value = confirm, onValueChange = { confirm = it }, label = "Confirm New Password")
            }
        },
        confirmButton = {
            Button(
                onClick = { if (new == confirm) onConfirm(current, new) },
                enabled = current.isNotBlank() && new.isNotBlank() && new == confirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Update Password", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        }
    )
}
