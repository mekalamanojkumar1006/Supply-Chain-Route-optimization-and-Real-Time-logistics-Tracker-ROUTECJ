package com.routecj.admin.presentation.vehicles.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun VehicleCard(
    vehicle: Vehicle,
    onEdit: () -> Unit,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
    onViewLogs: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Vehicle Number and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        var imageLoadFailed by remember { mutableStateOf(false) }
                        if (!vehicle.imageUrl.isNullOrBlank() && !imageLoadFailed) {
                            coil.compose.AsyncImage(
                                model = vehicle.imageUrl,
                                contentDescription = "Vehicle Photo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                onError = { imageLoadFailed = true }
                            )
                        } else {
                            Icon(
                                Icons.Default.LocalShipping,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = vehicle.vehicleNumber.ifBlank { vehicle.registrationNumber },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${vehicle.brand} ${vehicle.model}",
                            style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray)
                        )
                    }
                }
                
                StatusBadge(status = vehicle.status)
            }
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            
            // Details Grid
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Person,
                    label = "Driver",
                    value = if (vehicle.driverName.isNotBlank()) vehicle.driverName else (vehicle.driverId ?: "Unassigned")
                )
                DetailItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = vehicle.location.ifBlank { "Not Specified" }
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(modifier = Modifier.fillMaxWidth()) {
                DetailItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.GasMeter,
                    label = "Fuel Level",
                    value = "${vehicle.fuelLevel.toInt()}%",
                    valueColor = if (vehicle.fuelLevel < 20) Color.Red else Color(0xFF22C55E)
                )
                DetailItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.CalendarToday,
                    label = "Last Service",
                    value = SimpleDateFormat("dd MMM yyyy", Locale.ROOT).format(vehicle.lastServiceDate)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
            
            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionButton(Icons.Default.Edit, "Edit", onEdit)
                    ActionButton(Icons.Default.Visibility, "View", onViewDetails)
                    ActionButton(Icons.Default.History, "Logs", onViewLogs)
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = Color.Red)
                }
            }
        }
    }
}

@Composable
fun StatusBadge(status: VehicleStatus) {
    val (color, text) = when (status) {
        VehicleStatus.AVAILABLE -> Color(0xFF22C55E) to "Available"
        VehicleStatus.ASSIGNED -> Color(0xFF3B82F6) to "Assigned"
        VehicleStatus.IN_TRANSIT -> Color(0xFF8B5CF6) to "In Transit"
        VehicleStatus.MAINTENANCE -> Color(0xFFF59E0B) to "Maintenance"
        VehicleStatus.INACTIVE -> Color(0xFF64748B) to "Inactive"
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall.copy(color = color, fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun DetailItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = Color.Unspecified
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray, fontSize = 10.sp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Medium,
                    color = valueColor
                ),
                maxLines = 1
            )
        }
    }
}

@Composable
fun ActionButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        modifier = Modifier.height(36.dp)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}
