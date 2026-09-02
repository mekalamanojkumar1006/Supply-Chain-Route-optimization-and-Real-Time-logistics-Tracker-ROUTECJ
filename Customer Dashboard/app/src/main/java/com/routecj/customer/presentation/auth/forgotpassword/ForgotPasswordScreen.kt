package com.routecj.customer.presentation.auth.forgotpassword

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.presentation.components.animations.AnimatedEntrance

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Reset Password",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(RouteCJSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))
            
            AnimatedEntrance(index = 0) {
                Column {
                    Text(
                        text = "Forgot your password?",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                    Text(
                        text = "Enter your email address to receive a password reset link.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Start)
                    )
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraExtraLarge))

            AnimatedEntrance(index = 1) {
                Column {
                    RouteCJTextField(
                        value = email,
                        onValueChange = viewModel::onEmailChange,
                        label = "Email Address",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )

                    Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                    successMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = com.routecj.customer.ui.theme.SuccessGreen,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))

            AnimatedEntrance(index = 2) {
                RouteCJButton(
                    text = "Send Reset Link",
                    onClick = viewModel::resetPassword,
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

            AnimatedEntrance(index = 3) {
                RouteCJTextButton(text = "Back to Login", onClick = onNavigateBack)
            }
        }
    }
}
