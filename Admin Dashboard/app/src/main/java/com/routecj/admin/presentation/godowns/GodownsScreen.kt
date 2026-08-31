package com.routecj.admin.presentation.godowns

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.GodownStatus
import com.routecj.admin.presentation.components.*
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodownsScreen(
    navController: NavController,
    viewModel: GodownViewModel = hiltViewModel()
) {
    val godownsState by viewModel.filteredGodowns.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val statusFilter by viewModel.statusFilter.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Godown Management", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.NavigationRoutes.ADD_GODOWN) }) {
                        Icon(Icons.Default.AddBusiness, contentDescription = "Add Warehouse", tint = Primary, modifier = Modifier.size(28.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search Bar
            PremiumSearchBar(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = "Search facility name or location",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // Status Filter Chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    PremiumFilterChip(
                        selected = statusFilter == null,
                        onClick = { viewModel.setStatusFilter(null) },
                        label = "All Godowns"
                    )
                }
                items(GodownStatus.entries) { status ->
                    PremiumFilterChip(
                        selected = statusFilter == status.name,
                        onClick = { viewModel.setStatusFilter(status.name) },
                        label = status.name.lowercase().replaceFirstChar { it.uppercase() }
                    )
                }
            }

            when (val state = godownsState) {
                is Result.Loading -> PremiumLoadingState(message = "Analyzing warehouse capacity...")
                is Result.Error -> PremiumErrorState(message = state.message, onRetry = { })
                is Result.Success -> {
                    if (state.data.isEmpty()) {
                        PremiumEmptyState(message = "No godowns matching search criteria.", icon = Icons.Default.Warehouse)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.data) { godown ->
                                PremiumWarehouseCard(
                                    godown = godown,
                                    onClick = { 
                                        navController.navigate(Constants.NavigationRoutes.GODOWN_DETAILS.replace("{godownId}", godown.id))
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumWarehouseCard(godown: Godown, onClick: () -> Unit) {
    val occupancyPct = godown.occupancyPercentage.coerceIn(0, 100)
    val availableCapacity = (godown.capacity - godown.currentStock).coerceAtLeast(0.0)

    val (occupancyStateText, occupancyColor) = when {
        occupancyPct >= 100 -> "CRITICAL (100%)" to Color(0xFFEF4444)
        occupancyPct >= 90 -> "WARNING (>=90%)" to Color(0xFFF59E0B)
        else -> "NORMAL" to Primary
    }

    BentoCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = godown.name,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.titleMedium,
                    color = Secondary,
                    fontSize = 17.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                    Text(text = "${godown.city}, ${godown.state}", fontSize = 12.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                }
            }
            PremiumStatusChip(text = occupancyStateText, color = occupancyColor)
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Manager & Capacity overview
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "MANAGER", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = godown.managerName ?: "Unassigned", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "AVAILABLE CAPACITY", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text(text = "$availableCapacity Tons", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Primary)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Visual Occupancy Indicator
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Stock: ${godown.currentStock} / ${godown.capacity} Tons",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Secondary
                )
                Text(
                    text = "$occupancyPct% Occupied",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = occupancyColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Visual occupancy progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((occupancyPct / 100f).coerceIn(0.04f, 1f))
                        .clip(CircleShape)
                        .background(occupancyColor)
                )
            }
        }

        HorizontalDivider(modifier = Modifier.padding(top = 16.dp, bottom = 10.dp), color = Color(0xFFF1F5F9))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Warehouse ID: ${godown.id.take(8).uppercase()}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray
            )
            Icon(Icons.Default.ChevronRight, contentDescription = "Details", tint = Color.LightGray, modifier = Modifier.size(18.dp))
        }
    }
}
