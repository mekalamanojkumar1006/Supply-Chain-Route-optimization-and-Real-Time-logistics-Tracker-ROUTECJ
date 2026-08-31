package com.routecj.admin.presentation.login

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants
import com.routecj.admin.domain.model.AdminRole
import com.routecj.admin.presentation.components.BentoCard
import com.routecj.admin.presentation.components.RouteCJPasswordTextField
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

/**
 * Login Screen Composable.
 * Redesigned for a modern, high-end logistics control center experience.
 */
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: LoginViewModel = hiltViewModel()
) {
    // Collect UI states using lifecycle-aware collectors
    val emailState by viewModel.emailState.collectAsStateWithLifecycle()
    val passwordState by viewModel.passwordState.collectAsStateWithLifecycle()
    val rememberMeState by viewModel.rememberMeState.collectAsStateWithLifecycle()
    val isLoadingState by viewModel.isLoadingState.collectAsStateWithLifecycle()
    val errorState by viewModel.errorState.collectAsStateWithLifecycle()
    val successMessageState by viewModel.successMessageState.collectAsStateWithLifecycle()
    val loginSuccessState by viewModel.loginSuccessState.collectAsStateWithLifecycle()
    val currentAdmin by viewModel.currentAdmin.collectAsStateWithLifecycle()

    // Diagnostic log before LoginScreen is shown
    LaunchedEffect(Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        timber.log.Timber.tag("AUTH_DEBUG").d("currentUser before LoginScreen = ${user != null}")
        if (user != null) {
            timber.log.Timber.tag("AUTH_DEBUG").d("before LoginScreen UID = ${user.uid}")
        }
    }

    // Role-based navigation logic
    LaunchedEffect(loginSuccessState, currentAdmin) {
        if (loginSuccessState && currentAdmin != null) {
            val role = currentAdmin!!.role
            
            val route = when (role) {
                AdminRole.SUPER_ADMIN, 
                AdminRole.ADMIN,
                AdminRole.DISPATCH_MANAGER,
                AdminRole.GODOWN_MANAGER -> Constants.NavigationRoutes.DASHBOARD
                else -> null
            }

            Log.d("ROLE_DEBUG", "Matched Role: $role")
            Log.d("ROLE_DEBUG", "Navigation Target: $route")

            if (route != null) {
                navController.navigate(route) {
                    popUpTo(Constants.NavigationRoutes.LOGIN) { inclusive = true }
                }
            } else {
                Log.e("ROLE_DEBUG", "Navigation FAILED: Unknown or unauthorized role")
                viewModel.setError("Unauthorized user role.")
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFF8FAFC),
                        Color(0xFFF1F5F9)
                    )
                )
            )
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Brand Logo Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Route",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Secondary,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "CJ",
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.SansSerif,
                    color = Primary,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier
                        .padding(start = 4.dp, top = 6.dp)
                        .size(8.dp)
                        .background(color = Primary, shape = CircleShape)
                )
            }

            Text(
                text = "LOGISTICS INTELLIGENCE & CONTROL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFF64748B),
                letterSpacing = 1.8.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Login Bento Card Container
            BentoCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color.White
            ) {
                Text(
                    text = "Admin Sign In",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Secondary,
                        fontSize = 20.sp
                    )
                )
                Text(
                    text = "Enter your verified administrator credentials to access the logistics control center.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFF64748B),
                        lineHeight = 18.sp
                    ),
                    modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                )

                // Email Field
                OutlinedTextField(
                    value = emailState,
                    onValueChange = { viewModel.setEmail(it) },
                    label = { Text("Admin Email", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = "Email",
                            tint = if (emailState.isNotEmpty()) Primary else Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    singleLine = true,
                    enabled = !isLoadingState,
                    shape = RoundedCornerShape(14.dp),
                    colors = com.routecj.admin.presentation.components.routeCJTextFieldColors(
                        containerColor = Color.White,
                        textColor = Color(0xFF0F172A),
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                // Password Field
                RouteCJPasswordTextField(
                    value = passwordState,
                    onValueChange = { viewModel.setPassword(it) },
                    label = "Password",
                    enabled = !isLoadingState
                )

                // Remember Me & Forgot Password
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 2.dp)
                    ) {
                        Checkbox(
                            checked = rememberMeState,
                            onCheckedChange = { viewModel.setRememberMe(it) },
                            enabled = !isLoadingState,
                            colors = CheckboxDefaults.colors(checkedColor = Primary)
                        )
                        Text(
                            text = "Remember Me",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )
                    }
                    TextButton(
                        onClick = { navController.navigate(Constants.NavigationRoutes.FORGOT_PASSWORD) },
                        enabled = !isLoadingState
                    ) {
                        Text(
                            text = "Forgot Password?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Primary
                        )
                    }
                }

                // Error / Success Messages
                if (errorState != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = errorState!!,
                            color = Color(0xFFEF4444),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
                if (successMessageState != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 14.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = Primary.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = successMessageState!!,
                            color = Primary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }

                // Login Button
                Button(
                    onClick = { viewModel.login() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = !isLoadingState && emailState.isNotBlank() && passwordState.isNotBlank(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Primary,
                        disabledContainerColor = Color(0xFFE2E8F0)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    if (isLoadingState) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Text(
                            text = "Authenticating...",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    } else {
                        Text(
                            text = "AUTHENTICATE & ENTER",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


