package com.routecj.admin.presentation.login

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    val emailState by viewModel.emailState.collectAsStateWithLifecycle()
    val isLoadingState by viewModel.isLoadingState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val successMessageState by viewModel.successMessageState.collectAsStateWithLifecycle()
    val showResetSuccessDialog by viewModel.showResetSuccessDialog.collectAsStateWithLifecycle()

    if (showResetSuccessDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissResetSuccessDialog() },
            title = { Text("Success") },
            text = { Text("Password reset link has been sent to your email.") },
            confirmButton = {
                TextButton(onClick = { 
                    viewModel.dismissResetSuccessDialog()
                    navController.popBackStack() 
                }) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Reset Your Password",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Enter your registered email address and we will send you a link to reset your password.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = emailState,
                onValueChange = { viewModel.setEmail(it) },
                label = { Text("Email Address") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoadingState,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                colors = com.routecj.admin.presentation.components.routeCJTextFieldColors(
                    containerColor = androidx.compose.ui.graphics.Color.White,
                    textColor = androidx.compose.ui.graphics.Color(0xFF0F172A),
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color(0xFFCBD5E1)
                )
            )
            
            if (errorState != null) {
                Text(
                    text = errorState!!,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            if (successMessageState != null) {
                Text(
                    text = successMessageState!!,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { viewModel.forgotPassword() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoadingState && emailState.isNotBlank(),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            ) {
                if (isLoadingState) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).padding(end = 8.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                Text("Send Reset Link")
            }
        }
    }
}
