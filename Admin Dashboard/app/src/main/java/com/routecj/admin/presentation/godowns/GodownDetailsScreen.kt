package com.routecj.admin.presentation.godowns

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.ui.LoadingIndicator
import com.routecj.admin.core.util.Constants
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.GodownStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GodownDetailsScreen(
    navController: NavController,
    godownId: String,
    viewModel: GodownViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var godown by remember { mutableStateOf<Godown?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()

    LaunchedEffect(godownId) {
        val res = viewModel.getGodownById(godownId)
        if (res is Result.Success) godown = res.data
        isLoading = false
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Action Successful", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            val res = viewModel.getGodownById(godownId)
            if (res is Result.Success) godown = res.data
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Godown Details", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Constants.NavigationRoutes.ADD_GODOWN + "?editGodownId=$godownId") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { 
                        viewModel.deleteGodown(godownId)
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) LoadingIndicator()
        else if (godown == null) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Godown not found") }
        else {
            val g = godown!!
            LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    GodownStatusHeader(status = g.status)
                }
                item {
                    CapacityCard(current = g.currentStock, total = g.capacity)
                }
                item {
                    GodownInfoSection(godown = g)
                }
            }
        }
    }
}

@Composable
fun GodownStatusHeader(status: GodownStatus) {
    val statusColor = when (status) {
        GodownStatus.ACTIVE -> Color(0xFF22C55E)
        GodownStatus.INACTIVE -> Color(0xFFEF4444)
        GodownStatus.MAINTENANCE -> Color(0xFFF59E0B)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = statusColor.copy(alpha = 0.1f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(12.dp).background(statusColor, CircleShape))
            Text(text = "Warehouse Status: ${status.name}", fontWeight = FontWeight.Bold, color = statusColor)
        }
    }
}

@Composable
fun CapacityCard(current: Double, total: Double) {
    val available = (total - current).coerceAtLeast(0.0)
    val percentage = if (total > 0) (current / total) * 100 else 0.0
    
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Storage Utilization", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Occupied", fontSize = 11.sp, color = Color.Gray)
                    Text("$current T", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Available", fontSize = 11.sp, color = Color.Gray)
                    Text("$available T", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF22C55E))
                }
            }
            
            LinearProgressIndicator(
                progress = { (percentage / 100).toFloat() },
                modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(6.dp)),
                color = if (percentage > 90) Color.Red else MaterialTheme.colorScheme.primary,
                trackColor = Color.LightGray.copy(alpha = 0.2f)
            )
            
            Text(text = "Total Capacity: $total Tons", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
fun GodownInfoSection(godown: Godown) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DetailItem(Icons.Default.Store, "Warehouse Name", godown.name)
        DetailItem(Icons.Default.LocationOn, "Location", "${godown.address}, ${godown.city}, ${godown.state} - ${godown.pincode}")
        DetailItem(Icons.Default.Person, "Assigned Manager", godown.managerName ?: "Not Assigned")
        DetailItem(Icons.Default.Phone, "Contact Phone", godown.phone)
        DetailItem(Icons.Default.Schedule, "Created On", godown.createdAt.toString())
    }
}

@Composable
fun DetailItem(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}
