package com.routecj.admin.presentation.profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Admin
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.PremiumActionCard
import com.routecj.admin.presentation.components.PremiumErrorState
import com.routecj.admin.presentation.components.PremiumLoadingState
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val isLoadingState by viewModel.isLoadingState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val adminProfile by viewModel.adminProfile.collectAsStateWithLifecycle()
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(actionState) {
        actionState?.let { result ->
            if (result is Result.Success) {
                Toast.makeText(context, "Profile synchronized successfully", Toast.LENGTH_SHORT).show()
                viewModel.clearActionState()
                showEditProfileDialog = false
            } else if (result is Result.Error) {
                Toast.makeText(context, result.message, Toast.LENGTH_LONG).show()
                viewModel.clearActionState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Profile", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            val profile = adminProfile
            when {
                isLoadingState -> PremiumLoadingState(message = "Loading profile details...")
                errorState != null -> PremiumErrorState(message = errorState!!, onRetry = { viewModel.retryLoadProfile() })
                profile != null -> {


                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // User Avatar & Title
                        item {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(vertical = 12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp)
                                        .shadow(8.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!profile.profileImage.isNullOrEmpty()) {
                                        AsyncImage(model = profile.profileImage, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    } else {
                                        Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(72.dp), tint = Primary)
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(text = profile.name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Surface(color = Primary.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
                                    Text(
                                        text = "ROLE: ${profile.role.displayName.uppercase()}", 
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), 
                                        color = Primary, 
                                        fontSize = 11.sp, 
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }
                        }

                        // Identity & Details
                        item {
                            BentoCard(modifier = Modifier.fillMaxWidth()) {
                                Text("ACCOUNT INFORMATION", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = Color.Gray, letterSpacing = 1.2.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                ProfileInfoEntry(Icons.Default.Email, "Email Address", profile.email)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                                ProfileInfoEntry(Icons.Default.Phone, "Phone Number", profile.phone)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                                ProfileInfoEntry(Icons.Default.AdminPanelSettings, "Role & Access", profile.role.displayName)
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = Color(0xFFF1F5F9))
                                ProfileInfoEntry(Icons.Default.AccessTime, "Last Login", profile.lastLogin.ifEmpty { "Active Session" })
                            }
                        }

                        // Quick Navigation to Settings & Logout
                        item {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                PremiumActionCard(
                                    title = "Application Settings",
                                    subtitle = "Security credentials, notifications & privacy",
                                    icon = Icons.Default.Settings,
                                    onClick = { navController.navigate(Constants.NavigationRoutes.SETTINGS) }
                                )
                                Button(
                                    onClick = {
                                        viewModel.logout()
                                        navController.navigate(Constants.NavigationRoutes.LOGIN) {
                                            popUpTo(0) { inclusive = true }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth().height(54.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.08f), contentColor = Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.2f)),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Logout", modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("LOGOUT", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, letterSpacing = 1.2.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditProfileDialog && adminProfile != null) {
        EditProfileDialog(
            admin = adminProfile!!,
            onDismiss = { showEditProfileDialog = false },
            onSave = { name, phone, image -> viewModel.updateProfile(name, phone, image) }
        )
    }
}

@Composable
fun ProfileInfoEntry(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier.size(36.dp).background(Primary.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column {
            Text(text = label.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Secondary)
        }
    }
}

@Composable
fun EditProfileDialog(admin: Admin, onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit) {
    var name by remember { mutableStateOf(admin.name) }
    var phone by remember { mutableStateOf(admin.phone) }
    var profileImage by remember { mutableStateOf(admin.profileImage ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile Details", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = com.routecj.admin.presentation.components.routeCJTextFieldColors()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = com.routecj.admin.presentation.components.routeCJTextFieldColors()
                )
                OutlinedTextField(
                    value = profileImage,
                    onValueChange = { profileImage = it },
                    label = { Text("Profile Photo URL") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = com.routecj.admin.presentation.components.routeCJTextFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, phone, if (profileImage.isBlank()) null else profileImage) },
                enabled = name.isNotBlank() && phone.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Primary)
            ) {
                Text("Save Profile", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) } }
    )
}
