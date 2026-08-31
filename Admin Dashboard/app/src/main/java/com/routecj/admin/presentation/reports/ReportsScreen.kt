package com.routecj.admin.presentation.reports

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.*
import com.routecj.admin.presentation.components.*
import com.routecj.admin.presentation.reports.components.BarChart
import com.routecj.admin.presentation.reports.components.DonutChart
import com.routecj.admin.presentation.reports.components.StatusDistributionBar
import com.routecj.admin.ui.theme.Primary
import com.routecj.admin.ui.theme.Secondary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
    navController: NavController,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val ordersReport by viewModel.ordersReport.collectAsStateWithLifecycle()
    val deliveryReport by viewModel.deliveryPerformanceReport.collectAsStateWithLifecycle()
    val driverReport by viewModel.driverReport.collectAsStateWithLifecycle()
    val vehicleReport by viewModel.vehicleReport.collectAsStateWithLifecycle()
    val godownReport by viewModel.godownReport.collectAsStateWithLifecycle()
    val selectedPeriod by viewModel.selectedPeriod.collectAsStateWithLifecycle()
    
    val admin by viewModel.currentAdmin.collectAsStateWithLifecycle()
    val role = admin?.role

    fun shareCSV(content: String) {
        if (content.isBlank()) {
            Toast.makeText(context, "No report data available to export", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val file = File(context.cacheDir, "routecj_logistics_intelligence.csv")
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export Logistics Intelligence Report"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Reports & Analytics", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        Text("Logistics Intelligence Control", fontSize = 11.sp, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareCSV(viewModel.exportOrdersToCSV()) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export CSV", tint = Primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. DATE FILTER CHIP ROW
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(ReportPeriod.entries) { period ->
                        val isSelected = selectedPeriod == period
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                if (period == ReportPeriod.CUSTOM) {
                                    val cal = Calendar.getInstance()
                                    DatePickerDialog(
                                        context,
                                        { _, y, m, d ->
                                            val startCal = Calendar.getInstance().apply { set(y, m, d, 0, 0, 0) }
                                            DatePickerDialog(
                                                context,
                                                { _, endY, endM, endD ->
                                                    val endCal = Calendar.getInstance().apply { set(endY, endM, endD, 23, 59, 59) }
                                                    viewModel.setCustomRange(startCal.time, endCal.time)
                                                },
                                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                            ).apply { setTitle("Select End Date") }.show()
                                        },
                                        cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                    ).apply { setTitle("Select Start Date") }.show()
                                } else {
                                    viewModel.setPeriod(period)
                                }
                            },
                            label = {
                                Text(
                                    text = when (period) {
                                        ReportPeriod.TODAY -> "Today"
                                        ReportPeriod.LAST_7_DAYS -> "Last 7 Days"
                                        ReportPeriod.LAST_30_DAYS -> "Last 30 Days"
                                        ReportPeriod.THIS_MONTH -> "This Month"
                                        ReportPeriod.CUSTOM -> "Custom Range"
                                    },
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Primary,
                                selectedLabelColor = Color.Black,
                                containerColor = Color(0xFF1E293B),
                                labelColor = Color.LightGray
                            ),
                            border = null,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }

            // 2. OVERVIEW ANALYTICS (TOTAL ORDERS, DELIVERED, IN TRANSIT, PENDING, CANCELLED, FAILED, ACTIVE TRIPS)
            item {
                PremiumSectionHeader(title = "Operational Overview")
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    when (val state = ordersReport) {
                        is Result.Loading -> PremiumLoadingState(message = "Aggregating operational analytics...")
                        is Result.Error -> PremiumErrorState(message = state.message, onRetry = { })
                        is Result.Success -> {
                            val data = state.data
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                // 2x2 Bento Cards
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ReportKPICard(
                                        title = "TOTAL ORDERS",
                                        value = "${data.total}",
                                        icon = Icons.Default.Inventory2,
                                        color = Secondary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReportKPICard(
                                        title = "DELIVERED",
                                        value = "${data.delivered}",
                                        icon = Icons.Default.CheckCircle,
                                        color = Color(0xFF10B981),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ReportKPICard(
                                        title = "IN TRANSIT",
                                        value = "${data.inTransit}",
                                        icon = Icons.Default.LocalShipping,
                                        color = Primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReportKPICard(
                                        title = "ACTIVE TRIPS",
                                        value = "${data.activeTrips}",
                                        icon = Icons.Default.Navigation,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    ReportKPICard(
                                        title = "PENDING",
                                        value = "${data.pending}",
                                        icon = Icons.Default.WatchLater,
                                        color = Color(0xFFF59E0B),
                                        modifier = Modifier.weight(1f)
                                    )
                                    ReportKPICard(
                                        title = "CANCELLED / FAILED",
                                        value = "${data.cancelled + data.failed}",
                                        icon = Icons.Default.Cancel,
                                        color = Color(0xFFEF4444),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. ORDER STATUS DISTRIBUTION (Exact enum breakdown)
            item {
                PremiumSectionHeader(title = "Order Lifecycle Distribution")
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    BentoCard(modifier = Modifier.fillMaxWidth()) {
                        when (val state = ordersReport) {
                            is Result.Success -> {
                                val data = state.data
                                val colorMap: (String) -> Color = { name ->
                                    when (name) {
                                        OrderStatus.DELIVERED.name -> Color(0xFF10B981)
                                        OrderStatus.IN_TRANSIT.name, OrderStatus.DISPATCHED.name -> Primary
                                        OrderStatus.READY_FOR_DISPATCH.name, OrderStatus.QR_GENERATED.name -> Color(0xFF38BDF8)
                                        OrderStatus.PENDING_GODOWN_REVIEW.name, OrderStatus.PICKED_UP.name -> Color(0xFF818CF8)
                                        OrderStatus.ASSIGNED.name -> Color(0xFFA78BFA)
                                        OrderStatus.PENDING.name -> Color(0xFFF59E0B)
                                        OrderStatus.CANCELLED.name, OrderStatus.FAILED.name -> Color(0xFFEF4444)
                                        else -> Color.Gray
                                    }
                                }

                                val statusPairs = data.statusBreakdown.map { it.first.name to it.second }
                                StatusDistributionBar(segments = statusPairs, colorMap = colorMap)

                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider(color = Color(0xFF334155))
                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "DAILY ORDER VOLUME TREND",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Primary,
                                    letterSpacing = 1.sp
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                BarChart(data = data.ordersByDay, barColor = Primary)
                            }
                            else -> {}
                        }
                    }
                }
            }

            // 4. DELIVERY PERFORMANCE ANALYTICS
            if (role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN || role == AdminRole.DISPATCH_MANAGER) {
                item {
                    PremiumSectionHeader(title = "Delivery Performance & Timing")
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            when (val state = deliveryReport) {
                                is Result.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                                is Result.Success -> {
                                    val d = state.data
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "SUCCESSFUL DELIVERY RATE",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "${d.deliverySuccessRate.toInt()}%",
                                                    fontSize = 28.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color(0xFF10B981)
                                                )
                                            }
                                            DonutChart(
                                                percentage = d.deliverySuccessRate.toFloat(),
                                                centerText = "Fulfilled",
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.size(90.dp)
                                            )
                                        }

                                        HorizontalDivider(color = Color(0xFF334155))

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            DeliveryMetricItem("Today", "${d.deliveredToday}", Color.White)
                                            DeliveryMetricItem("This Week", "${d.deliveredThisWeek}", Color.White)
                                            DeliveryMetricItem("This Month", "${d.deliveredThisMonth}", Primary)
                                        }

                                        if (d.avgDeliveryDurationMinutes > 0) {
                                            HorizontalDivider(color = Color(0xFF334155))
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Icon(Icons.Default.Timer, contentDescription = null, tint = Primary, modifier = Modifier.size(18.dp))
                                                val hours = (d.avgDeliveryDurationMinutes / 60).toInt()
                                                val mins = (d.avgDeliveryDurationMinutes % 60).toInt()
                                                val durationText = if (hours > 0) "${hours}h ${mins}m" else "${mins} mins"
                                                Text(text = "Average Fulfillment Duration: $durationText", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                                            }
                                        }

                                        if (d.deliveryTrendByDay.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "COMPLETED DELIVERIES TIMELINE",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF10B981),
                                                letterSpacing = 1.sp
                                            )
                                            BarChart(data = d.deliveryTrendByDay, barColor = Color(0xFF10B981))
                                        }
                                    }
                                }
                                is Result.Error -> Text("Failed to load delivery analytics", color = Color.Red)
                            }
                        }
                    }
                }
            }

            // 5. DRIVER PERFORMANCE RANKING
            if (role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN || role == AdminRole.DISPATCH_MANAGER) {
                item {
                    PremiumSectionHeader(title = "Driver Fleet Performance")
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            when (val state = driverReport) {
                                is Result.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                                is Result.Success -> {
                                    val d = state.data
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Total: ${d.totalDrivers}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                                            Text("${d.availableCount} Avail", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            Text("${d.onTripCount + d.busyCount} On Duty", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                            Text("${d.offDutyCount} Off", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                                        }
                                        HorizontalDivider(color = Color(0xFF334155))

                                        Text(
                                            text = "DRIVER ACTIVITY RANKING",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Gray,
                                            letterSpacing = 0.5.sp
                                        )

                                        if (d.topPerformers.isEmpty()) {
                                            Text("No driver records found.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            d.topPerformers.take(5).forEach { driver ->
                                                DriverPerformanceRow(driver = driver)
                                            }
                                        }
                                    }
                                }
                                is Result.Error -> Text("Failed to load drivers", color = Color.Red)
                            }
                        }
                    }
                }
            }

            // 6. VEHICLE ASSET UTILIZATION
            if (role == AdminRole.SUPER_ADMIN || role == AdminRole.ADMIN || role == AdminRole.DISPATCH_MANAGER) {
                item {
                    PremiumSectionHeader(title = "Vehicle Asset Utilization")
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            when (val state = vehicleReport) {
                                is Result.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                                is Result.Success -> {
                                    val v = state.data
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Total: ${v.totalVehicles}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Secondary)
                                            Text("${v.availableCount} Avail", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                            Text("${v.assignedCount + v.inTransitCount} In Use", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Primary)
                                            Text("${v.maintenanceCount} Maint", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFFEF4444))
                                        }
                                        HorizontalDivider(color = Color(0xFF334155))

                                        Text(
                                            text = "ACTIVE ASSETS UTILIZATION",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Gray,
                                            letterSpacing = 0.5.sp
                                        )

                                        if (v.utilizationStats.isEmpty()) {
                                            Text("No vehicle records found.", fontSize = 12.sp, color = Color.Gray)
                                        } else {
                                            v.utilizationStats.take(5).forEach { vehicle ->
                                                VehicleUtilizationRow(vehicle = vehicle)
                                            }
                                        }
                                    }
                                }
                                is Result.Error -> Text("Failed to load vehicles", color = Color.Red)
                            }
                        }
                    }
                }
            }

            // 7. GODOWN OPERATIONS & INTAKE CAPACITY
            if (role == AdminRole.SUPER_ADMIN || role == AdminRole.GODOWN_MANAGER) {
                item {
                    PremiumSectionHeader(title = "Godown Network & Warehouse Intake")
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        BentoCard(modifier = Modifier.fillMaxWidth()) {
                            when (val state = godownReport) {
                                is Result.Loading -> LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = Primary)
                                is Result.Success -> {
                                    val g = state.data
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            DonutChart(
                                                percentage = g.avgOccupancy.toFloat(),
                                                centerText = "Occupancy",
                                                color = if (g.criticalGodowns > 0) Color(0xFFEF4444) else Primary,
                                                modifier = Modifier.size(100.dp)
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Text(
                                                    text = "${g.avgOccupancy.toInt()}% Avg Utilization",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = Secondary
                                                )
                                                Text(
                                                    text = "Stock: ${g.currentStock.toInt()} / ${g.totalCapacity.toInt()} Tons",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.Gray
                                                )
                                                Text(
                                                    text = "Available: ${g.availableCapacity.toInt()} Tons",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFF10B981),
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                        }

                                        HorizontalDivider(color = Color(0xFF334155))

                                        Text(
                                            text = "PARCEL INTAKE PIPELINE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Black,
                                            color = Color.Gray
                                        )

                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            DeliveryMetricItem("Incoming", "${g.incomingParcels}", Color.White)
                                            DeliveryMetricItem("Pending Review", "${g.pendingReview}", Color(0xFFF59E0B))
                                            DeliveryMetricItem("Ready for Dispatch", "${g.readyForDispatch}", Color(0xFF38BDF8))
                                            DeliveryMetricItem("Dispatched", "${g.dispatched}", Color(0xFF10B981))
                                        }
                                    }
                                }
                                is Result.Error -> Text("Failed to load warehouse analytics", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportKPICard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    BentoCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = Color.Gray,
                    letterSpacing = 0.5.sp
                )
                Box(
                    modifier = Modifier.size(24.dp).background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
        }
    }
}

@Composable
fun DeliveryMetricItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Black, color = valueColor)
        Text(text = label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DriverPerformanceRow(driver: DriverStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = driver.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
            Text(
                text = "Trips: ${driver.totalTrips} • Active: ${driver.active} • Status: ${driver.currentStatus.name}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Text(
            text = "${driver.completed} Completed (${driver.completionRate.toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (driver.completionRate >= 80) Color(0xFF10B981) else Primary
        )
    }
}

@Composable
fun VehicleUtilizationRow(vehicle: VehicleStat) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = vehicle.registrationNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Secondary)
            Text(
                text = "${vehicle.vehicleType} • Active Trips: ${vehicle.activeTrips} • Status: ${vehicle.currentStatus.name}",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
        Text(
            text = "${vehicle.completedTrips} Trips (${vehicle.utilizationRate.toInt()}%)",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Primary
        )
    }
}

