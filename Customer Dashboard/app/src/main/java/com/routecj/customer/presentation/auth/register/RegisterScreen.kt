package com.routecj.customer.presentation.auth.register

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.R
import com.routecj.customer.presentation.components.RouteCJButton
import com.routecj.customer.presentation.components.RouteCJTextButton
import com.routecj.customer.presentation.components.RouteCJTextField
import com.routecj.customer.presentation.components.RouteCJTopBar
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.presentation.components.animations.AnimatedEntrance
import com.routecj.customer.presentation.components.animations.AnimatedIconTransition

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val isLoading by viewModel.isLoading.collectAsState()
    val isSuccess by viewModel.isSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val email by viewModel.email.collectAsState()
    val fullName by viewModel.fullName.collectAsState()
    val phone by viewModel.phone.collectAsState()
    val password by viewModel.password.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()

    val authViewModel: com.routecj.customer.presentation.auth.AuthViewModel = hiltViewModel()

    if (isSuccess) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { /* Prevent dismiss */ }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = com.routecj.customer.ui.theme.RouteCJShapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    com.routecj.customer.presentation.components.animations.SuccessAnimation(
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Account Created Successfully",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Your RouteCJ customer account has been created.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    RouteCJButton(
                        text = "Continue",
                        onClick = { authViewModel.checkAuthStatus() }
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Create Account",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(RouteCJSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            AnimatedEntrance(index = 0) {
                Text(
                    text = "Join RouteCJ",
                    style = MaterialTheme.typography.displayLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Start
                )
            }
            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraExtraLarge))

            AnimatedEntrance(index = 1) {
                RouteCJTextField(
                    value = fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = "Full Name"
                )
            }
            Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

            AnimatedEntrance(index = 2) {
                RouteCJTextField(
                    value = email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email Address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }
            Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

            AnimatedEntrance(index = 3) {
                RouteCJTextField(
                    value = phone,
                    onValueChange = viewModel::onPhoneChange,
                    label = "Phone Number",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
            }
            Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

            var passwordVisible by remember { mutableStateOf(false) }
            var confirmPasswordVisible by remember { mutableStateOf(false) }

            AnimatedEntrance(index = 4) {
                Column {
                    RouteCJTextField(
                        value = password,
                        onValueChange = viewModel::onPasswordChange,
                        label = "Password",
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                AnimatedIconTransition(targetState = passwordVisible) { isVisible ->
                                    Icon(
                                        painter = painterResource(id = if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                        contentDescription = if (isVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

                    RouteCJTextField(
                        value = confirmPassword,
                        onValueChange = viewModel::onConfirmPasswordChange,
                        label = "Confirm Password",
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                                AnimatedIconTransition(targetState = confirmPasswordVisible) { isVisible ->
                                    Icon(
                                        painter = painterResource(id = if (isVisible) R.drawable.ic_visibility_off else R.drawable.ic_visibility),
                                        contentDescription = if (isVisible) "Hide password" else "Show password"
                                    )
                                }
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(RouteCJSpacing.Small))

                    errorMessage?.let { msg ->
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))

            AnimatedEntrance(index = 5) {
                RouteCJButton(
                    text = if (isLoading) "Creating account..." else "Register",
                    onClick = viewModel::register,
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

            AnimatedEntrance(index = 6) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Already have an account?", style = MaterialTheme.typography.bodyMedium)
                    RouteCJTextButton(text = "Sign In", onClick = onNavigateBack)
                }
            }
        }
    }
}
