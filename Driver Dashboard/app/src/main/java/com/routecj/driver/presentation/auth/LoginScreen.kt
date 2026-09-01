package com.routecj.driver.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.driver.presentation.components.LogoVariant
import com.routecj.driver.presentation.components.RouteCJButton
import com.routecj.driver.presentation.components.RouteCJLogo
import com.routecj.driver.ui.theme.RouteCJCyan
import com.routecj.driver.ui.theme.RouteCJCyanLight
import com.routecj.driver.ui.theme.RouteCJError
import com.routecj.driver.ui.theme.RouteCJNavyCard
import com.routecj.driver.ui.theme.RouteCJNavyDark
import com.routecj.driver.ui.theme.RouteCJNavySurface
import com.routecj.driver.ui.theme.RouteCJTextPrimaryDark
import com.routecj.driver.ui.theme.RouteCJTextSecondaryDark

@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by authViewModel.uiState.collectAsState()
    val formState by authViewModel.formState.collectAsState()
    val focusManager = LocalFocusManager.current

    val isLoading = uiState is AuthUiState.Loading || uiState is AuthUiState.CheckingSession

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(RouteCJNavyDark)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Header Branding with Master RouteCJ Identity
            RouteCJLogo(
                variant = LogoVariant.DARK_BG,
                height = 120.dp
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Login Card Container
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = RouteCJNavySurface
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Driver Portal Login",
                        color = RouteCJTextPrimaryDark,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Sign in to access assigned delivery routes",
                        color = RouteCJTextSecondaryDark,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Email Input Field
                    OutlinedTextField(
                        value = formState.email,
                        onValueChange = { authViewModel.onEmailChanged(it) },
                        label = { Text("Email Address", color = RouteCJTextSecondaryDark) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Email,
                                contentDescription = "Email Icon",
                                tint = RouteCJCyan
                            )
                        },
                        isError = formState.emailError != null,
                        supportingText = formState.emailError?.let { err ->
                            { Text(text = err, color = RouteCJError) }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RouteCJCyan,
                            unfocusedBorderColor = RouteCJNavyCard,
                            focusedTextColor = RouteCJTextPrimaryDark,
                            unfocusedTextColor = RouteCJTextPrimaryDark,
                            cursorColor = RouteCJCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password Input Field
                    OutlinedTextField(
                        value = formState.password,
                        onValueChange = { authViewModel.onPasswordChanged(it) },
                        label = { Text("Password", color = RouteCJTextSecondaryDark) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Password Icon",
                                tint = RouteCJCyan
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { authViewModel.onTogglePasswordVisibility() }) {
                                Icon(
                                    imageVector = if (formState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (formState.isPasswordVisible) "Hide password" else "Show password",
                                    tint = RouteCJTextSecondaryDark
                                )
                            }
                        },
                        isError = formState.passwordError != null,
                        supportingText = formState.passwordError?.let { err ->
                            { Text(text = err, color = RouteCJError) }
                        },
                        visualTransformation = if (formState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                authViewModel.login()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RouteCJCyan,
                            unfocusedBorderColor = RouteCJNavyCard,
                            focusedTextColor = RouteCJTextPrimaryDark,
                            unfocusedTextColor = RouteCJTextPrimaryDark,
                            cursorColor = RouteCJCyan
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { authViewModel.onOpenForgotPassword(true) }
                        ) {
                            Text(
                                text = "Forgot Password?",
                                color = RouteCJCyanLight,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Display Error Message if any
                    when (val state = uiState) {
                        is AuthUiState.Error -> {
                            Text(
                                text = state.message,
                                color = RouteCJError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )
                        }
                        is AuthUiState.DriverNotFound -> {
                            Text(
                                text = state.message,
                                color = RouteCJError,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            )
                        }
                        else -> {}
                    }

                    // Login Action Button
                    RouteCJButton(
                        text = "SIGN IN",
                        onClick = {
                            focusManager.clearFocus()
                            authViewModel.login()
                        },
                        isLoading = isLoading,
                        enabled = !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Footer Slogan
            Text(
                text = "SMART ROUTES • CONNECTED LOGISTICS • REAL-TIME TRACKING",
                color = RouteCJTextSecondaryDark,
                fontSize = 10.sp,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // Forgot Password Dialog
        if (formState.isForgotPasswordOpen) {
            AlertDialog(
                onDismissRequest = { authViewModel.onOpenForgotPassword(false) },
                title = {
                    Text(
                        text = "Reset Password",
                        color = RouteCJTextPrimaryDark,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Enter your driver email address to receive password reset instructions.",
                            color = RouteCJTextSecondaryDark,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = formState.resetEmail,
                            onValueChange = { authViewModel.onResetEmailChanged(it) },
                            label = { Text("Reset Email", color = RouteCJTextSecondaryDark) },
                            isError = formState.resetEmailError != null,
                            supportingText = formState.resetEmailError?.let { err ->
                                { Text(text = err, color = RouteCJError) }
                            },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RouteCJCyan,
                                unfocusedBorderColor = RouteCJNavyCard,
                                focusedTextColor = RouteCJTextPrimaryDark,
                                unfocusedTextColor = RouteCJTextPrimaryDark
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { authViewModel.sendPasswordReset() }
                    ) {
                        Text("Send Link", color = RouteCJCyan)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { authViewModel.onOpenForgotPassword(false) }
                    ) {
                        Text("Cancel", color = RouteCJTextSecondaryDark)
                    }
                },
                containerColor = RouteCJNavySurface
            )
        }
    }
}
