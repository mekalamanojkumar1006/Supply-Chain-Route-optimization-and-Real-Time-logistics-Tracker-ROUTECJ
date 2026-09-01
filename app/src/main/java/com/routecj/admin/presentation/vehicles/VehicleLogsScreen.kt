package com.routecj.admin.presentation.vehicles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.VehicleLog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleLogsScreen(
    navController: NavController,
    vehicleId: String?,
    viewModel: VehiclesViewModel = hiltViewModel()
) {
    val logsState by viewModel.logsState.collectAsStateWithLifecycle()

    LaunchedEffect(vehicleId) {
        if (vehicleId != null) {
            viewModel.fetchVehicleLogs(vehicleId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vehicle Logs", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        when (val state = logsState) {
            is Result.Loading<*> -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is Result.Error<*> -> {
                Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { if (vehicleId != null) viewModel.fetchVehicleLogs(vehicleId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            is Result.Success<*> -> {
                val logs = (state as Result.Success<List<VehicleLog>>).data
                if (logs.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                        Text(text = "No logs found for this vehicle.", color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(logs) { log ->
                            LogTimelineItem(log)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LogTimelineItem(log: VehicleLog) {
    Row(modifier = Modifier.fillMaxWidth()) {
        // Timeline Indicator
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
            Box(
                modifier = Modifier
                    .width(2.dp)
                    .height(140.dp)
                    .background(Color.LightGray.copy(alpha = 0.5f))
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Log Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            val formatter = remember { java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ROOT) }
            val timeFormatter = remember { java.text.SimpleDateFormat("hh:mm a", java.util.Locale.ROOT) }
            
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.gateNumber,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Text(
                        text = formatter.format(log.date),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatusTimeBox(
                        label = "Time IN",
                        time = if (log.timeIn != null) timeFormatter.format(log.timeIn) else "--:--",
                        color = Color(0xFF22C55E),
                        modifier = Modifier.weight(1f)
                    )
                    StatusTimeBox(
                        label = "Time OUT",
                        time = if (log.timeOut != null) timeFormatter.format(log.timeOut) else "--:--",
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    )
                }
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = log.driverName, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Icon(Icons.Default.LocalShipping, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = log.vehicleNumber, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium))
                }
            }
        }
    }
}

@Composable
fun StatusTimeBox(label: String, time: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.05f),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold))
            Text(text = time, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}
