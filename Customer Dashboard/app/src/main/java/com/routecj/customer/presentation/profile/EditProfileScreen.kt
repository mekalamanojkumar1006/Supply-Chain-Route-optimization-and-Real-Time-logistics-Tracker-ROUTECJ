package com.routecj.customer.presentation.profile

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.routecj.customer.domain.model.Customer
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    customer: Customer,
    viewModel: EditProfileViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val name by viewModel.name.collectAsState()
    val phone by viewModel.phone.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initProfile(customer)
    }

    LaunchedEffect(state) {
        if (state is EditProfileState.Success) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Edit Profile",
                onBackClick = onNavigateBack
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(RouteCJSpacing.ExtraLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RouteCJTextField(
                value = name,
                onValueChange = viewModel::onNameChange,
                label = "Full Name"
            )
            Spacer(modifier = Modifier.height(RouteCJSpacing.Default))

            RouteCJTextField(
                value = phone,
                onValueChange = viewModel::onPhoneChange,
                label = "Phone Number"
            )
            Spacer(modifier = Modifier.height(RouteCJSpacing.Small))

            if (state is EditProfileState.Error) {
                Text(
                    text = (state as EditProfileState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.Start)
                )
            }

            Spacer(modifier = Modifier.height(RouteCJSpacing.ExtraLarge))

            RouteCJButton(
                text = "Save Changes",
                onClick = viewModel::saveProfile,
                isLoading = state is EditProfileState.Updating
            )
        }
    }
}
