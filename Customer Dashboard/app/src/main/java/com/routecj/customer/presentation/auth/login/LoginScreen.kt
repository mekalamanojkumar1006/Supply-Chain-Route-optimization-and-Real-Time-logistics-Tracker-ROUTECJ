package com.routecj.customer.presentation.auth.login

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import com.routecj.customer.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.CustomCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.presentation.components.animations.AnimatedEntrance
import com.routecj.customer.presentation.components.animations.AnimatedIconTransition
import com.routecj.customer.presentation.components.animations.animatedPress

import androidx.compose.ui.res.stringResource

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var passwordVisible by remember { mutableStateOf(false) }

    val webClientId = stringResource(id = R.string.default_web_client_id)

    val handleGoogleLogin: () -> Unit = {
        val credentialManager = CredentialManager.create(context)
        val googleIdOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(webClientId)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        coroutineScope.launch {
            try {
                timber.log.Timber.tag("AUTH_GOOGLE").d("Google account picker started")
                val result = credentialManager.getCredential(context, request)
                val credential = result.credential
                if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    timber.log.Timber.tag("AUTH_GOOGLE").d("Credential obtained successfully")
                    viewModel.loginWithGoogle(googleIdTokenCredential.idToken)
                } else {
                    timber.log.Timber.tag("AUTH_GOOGLE").w("Received unexpected credential type: ${credential.type}")
                }
            } catch (e: androidx.credentials.exceptions.GetCredentialCancellationException) {
                timber.log.Timber.tag("AUTH_GOOGLE").d("Google sign-in was cancelled.")
            } catch (e: Exception) {
                timber.log.Timber.tag("AUTH_GOOGLE").e(e, "Google sign-in configuration or execution failure")
                viewModel.onEmailChange(email) // Just to trigger a UI update if needed, but really we want to show an error
                // In a real app, we'd have a specific error channel for Google Login in the ViewModel
            }
        }
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(RouteCJSpacing.Large),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedEntrance(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.displayLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                    Text(
                        text = "Sign in to manage your shipments",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraExtraLarge))

            AnimatedEntrance(index = 1) {
                RouteCJTextField(
                    value = email,
                    onValueChange = viewModel::onEmailChange,
                    label = "Email Address",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

            AnimatedEntrance(index = 2) {
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

                    errorMessage?.let { msg ->
                        Spacer(modifier = Modifier.height(RouteCJSpacing.Small))
                        Text(
                            text = msg,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.align(Alignment.Start)
                        )
                    }

                    Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        RouteCJTextButton(
                            text = "Forgot Password?",
                            onClick = onNavigateToForgotPassword
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

            AnimatedEntrance(index = 3) {
                RouteCJButton(
                    text = "Sign In",
                    onClick = viewModel::loginWithEmail,
                    isLoading = isLoading
                )
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

            AnimatedEntrance(index = 4) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        text = "OR",
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.Large))

            AnimatedEntrance(index = 5) {
                OutlinedButton(
                    onClick = handleGoogleLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .animatedPress(),
                    shape = com.routecj.customer.ui.theme.RouteCJShapes.small,
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Signing in with Google...",
                            style = MaterialTheme.typography.labelLarge
                        )
                    } else {
                        // If we had a real Google icon we'd use it here, but a text "G" works as a premium styled placeholder
                        Text(
                            text = "G",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Text(
                            text = "Continue with Google",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))

            AnimatedEntrance(index = 6) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Don't have an account?", style = MaterialTheme.typography.bodyMedium)
                    RouteCJTextButton(text = "Register", onClick = onNavigateToRegister)
                }
            }
        }
    }
}
