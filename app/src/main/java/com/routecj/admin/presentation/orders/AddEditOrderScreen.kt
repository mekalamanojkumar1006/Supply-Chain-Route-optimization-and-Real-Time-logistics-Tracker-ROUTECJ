package com.routecj.admin.presentation.orders

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.routecj.admin.core.util.Result
import com.routecj.admin.domain.model.Order
import com.routecj.admin.domain.model.OrderStatus
import com.routecj.admin.domain.model.Driver
import com.routecj.admin.domain.model.DriverStatus
import com.routecj.admin.domain.model.PaymentMethod
import com.routecj.admin.domain.model.PaymentStatus
import com.routecj.admin.domain.model.Vehicle
import com.routecj.admin.domain.model.VehicleStatus
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditOrderScreen(
    navController: NavController,
    editOrderId: String? = null,
    viewModel: OrdersViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val driversState by viewModel.driversState.collectAsStateWithLifecycle()
    val vehiclesState by viewModel.vehiclesState.collectAsStateWithLifecycle()

    var customerName by remember { mutableStateOf("") }
    var customerPhone by remember { mutableStateOf("") }
    var customerAddress by remember { mutableStateOf("") }
    var pickupLocation by remember { mutableStateOf("") }
    var pickupPincode by remember { mutableStateOf("") }
    var deliveryLocation by remember { mutableStateOf("") }
    var deliveryPincode by remember { mutableStateOf("") }
    var orderType by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Medium") }
    var assignedDriverId by remember { mutableStateOf<String?>(null) }
    var assignedVehicleId by remember { mutableStateOf<String?>(null) }
    var remarks by remember { mutableStateOf("") }

    // Payment Details State (Defaults to PENDING)
    var paymentStatus by remember { mutableStateOf(PaymentStatus.PENDING) }
    var paymentMethod by remember { mutableStateOf(PaymentMethod.CASH) }
    var paymentAmountText by remember { mutableStateOf("") }
    var transactionId by remember { mutableStateOf("") }
    var paymentNotes by remember { mutableStateOf("") }
    
    var existingOrder by remember { mutableStateOf<Order?>(null) }

    LaunchedEffect(editOrderId) {
        if (editOrderId != null) {
            val res = viewModel.getOrderById(editOrderId)
            if (res is Result.Success) {
                val o = res.data
                existingOrder = o
                customerName = o.customerName
                customerPhone = o.customerPhone
                customerAddress = o.customerAddress
                pickupLocation = o.pickupAddress.ifBlank { o.pickupLocation }
                pickupPincode = o.pickupPincode
                deliveryLocation = o.deliveryAddress.ifBlank { o.deliveryLocation }
                deliveryPincode = o.deliveryPincode
                orderType = o.orderType
                weight = o.weight.toString()
                quantity = o.quantity.toString()
                priority = o.priority
                assignedDriverId = o.assignedDriverId
                assignedVehicleId = o.assignedVehicleId
                remarks = o.remarks

                paymentStatus = o.effectivePaymentStatus
                paymentMethod = o.effectivePaymentMethod
                paymentAmountText = if (o.paymentAmount > 0.0) o.paymentAmount.toString() else if (o.totalAmount > 0.0) o.totalAmount.toString() else ""
                transactionId = o.transactionId
                paymentNotes = o.paymentNotes
            }
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Order saved successfully", Toast.LENGTH_SHORT).show()
            viewModel.clearActionState()
            navController.popBackStack()
        } else if (actionState is Result.Error) {
            Toast.makeText(context, (actionState as Result.Error).message, Toast.LENGTH_LONG).show()
            viewModel.clearActionState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (editOrderId == null) "Add New Order" else "Edit Order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 16.dp) {
                Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = {
                            val w = weight.toDoubleOrNull() ?: 0.0
                            val q = quantity.toIntOrNull() ?: 0
                            val amt = paymentAmountText.toDoubleOrNull() ?: 0.0

                            if (amt < 0.0) {
                                Toast.makeText(context, "Payment amount cannot be negative", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val isElectronic = paymentMethod == PaymentMethod.UPI || paymentMethod == PaymentMethod.CARD || paymentMethod == PaymentMethod.BANK_TRANSFER
                            val isPaidState = paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.PARTIALLY_PAID
                            if (isPaidState && isElectronic && transactionId.isBlank()) {
                                Toast.makeText(context, "Transaction ID required for ${paymentMethod.name.replace("_", " ")} payment", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            val paymentDate = existingOrder?.paymentTimestamp ?: (if (isPaidState) Date() else null)

                            val order = Order(
                                id = editOrderId ?: "",
                                orderNumber = existingOrder?.orderNumber ?: "ORD-${System.currentTimeMillis().toString().takeLast(6)}",
                                customerName = customerName,
                                customerPhone = customerPhone,
                                customerAddress = customerAddress,
                                pickupLocation = pickupLocation,
                                pickupAddress = pickupLocation,
                                pickupPincode = pickupPincode,
                                deliveryLocation = deliveryLocation,
                                deliveryAddress = deliveryLocation,
                                deliveryPincode = deliveryPincode,
                                orderType = orderType,
                                weight = w,
                                quantity = q,
                                priority = priority,
                                paymentStatus = paymentStatus.name,
                                paymentMethod = paymentMethod.name,
                                paymentAmount = amt,
                                transactionId = transactionId.trim(),
                                paymentTimestamp = paymentDate,
                                paymentNotes = paymentNotes.trim(),
                                totalAmount = amt,
                                assignedDriverId = assignedDriverId,
                                assignedVehicleId = assignedVehicleId,
                                remarks = remarks,
                                status = existingOrder?.status ?: OrderStatus.PENDING,
                                createdAt = existingOrder?.createdAt ?: Date(),
                                updatedAt = Date()
                            )
                            if (editOrderId == null) viewModel.createOrder(order) else viewModel.updateOrder(order)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = actionState !is Result.Loading
                    ) {
                        if (actionState is Result.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        else Text("Save Order")
                    }
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FormSectionTitle("Customer Details")
                OrderFormField(value = customerName, onValueChange = { customerName = it }, label = "Customer Name", icon = Icons.Default.Person)
                OrderFormField(value = customerPhone, onValueChange = { customerPhone = it }, label = "Customer Phone", icon = Icons.Default.Phone)
                OrderFormField(value = customerAddress, onValueChange = { customerAddress = it }, label = "Customer Address", icon = Icons.Default.Home)
            }
            item {
                FormSectionTitle("Logistics Info")
                OrderFormField(value = pickupLocation, onValueChange = { pickupLocation = it }, label = "Pickup Location / Address", icon = Icons.Default.LocationOn)
                OrderFormField(value = pickupPincode, onValueChange = { pickupPincode = it.filter { ch -> ch.isDigit() }.take(6) }, label = "Pickup PIN Code (6 Digits)", icon = Icons.Default.PinDrop)
                OrderFormField(value = deliveryLocation, onValueChange = { deliveryLocation = it }, label = "Delivery Location / Address", icon = Icons.Default.Flag)
                OrderFormField(value = deliveryPincode, onValueChange = { deliveryPincode = it.filter { ch -> ch.isDigit() }.take(6) }, label = "Delivery PIN Code (6 Digits)", icon = Icons.Default.PinDrop)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) {
                        OrderFormField(value = weight, onValueChange = { weight = it }, label = "Weight (kg)", icon = Icons.Default.Scale)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OrderFormField(value = quantity, onValueChange = { quantity = it }, label = "Quantity", icon = Icons.Default.Numbers)
                    }
                }
                OrderFormField(value = orderType, onValueChange = { orderType = it }, label = "Order Type", icon = Icons.Default.Category)
            }
            item {
                FormSectionTitle("Payment Details")
                
                // Status selection
                Text("Payment Status", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentStatus.PENDING, PaymentStatus.PAID, PaymentStatus.COD).forEach { status ->
                        val isSelected = paymentStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                paymentStatus = status
                                if (status == PaymentStatus.COD) paymentMethod = PaymentMethod.COD
                                else if (paymentMethod == PaymentMethod.COD) paymentMethod = PaymentMethod.CASH
                            },
                            label = { Text(status.name.replace("_", " "), fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentStatus.PARTIALLY_PAID, PaymentStatus.FAILED, PaymentStatus.REFUNDED).forEach { status ->
                        val isSelected = paymentStatus == status
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                paymentStatus = status
                                if (status == PaymentStatus.COD) paymentMethod = PaymentMethod.COD
                                else if (paymentMethod == PaymentMethod.COD) paymentMethod = PaymentMethod.CASH
                            },
                            label = { Text(status.name.replace("_", " "), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Method selection
                Text("Payment Method", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.CASH, PaymentMethod.UPI, PaymentMethod.CARD).forEach { method ->
                        val isSelected = paymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                paymentMethod = method
                                if (method == PaymentMethod.COD) paymentStatus = PaymentStatus.COD
                            },
                            label = { Text(method.name.replace("_", " "), fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(PaymentMethod.BANK_TRANSFER, PaymentMethod.COD, PaymentMethod.OTHER).forEach { method ->
                        val isSelected = paymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                paymentMethod = method
                                if (method == PaymentMethod.COD) paymentStatus = PaymentStatus.COD
                            },
                            label = { Text(method.name.replace("_", " "), fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                OrderFormField(
                    value = paymentAmountText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) paymentAmountText = input
                    },
                    label = "Amount (₹)",
                    icon = Icons.Default.CurrencyRupee
                )

                if (paymentMethod == PaymentMethod.UPI) {
                    Spacer(modifier = Modifier.height(6.dp))
                    com.routecj.admin.presentation.components.UpiPaymentQrCard(
                        amount = paymentAmountText.toDoubleOrNull() ?: 0.0,
                        orderNumber = existingOrder?.orderNumber ?: ""
                    )
                }

                val isElectronic = paymentMethod == PaymentMethod.UPI || paymentMethod == PaymentMethod.CARD || paymentMethod == PaymentMethod.BANK_TRANSFER
                val isPaidState = paymentStatus == PaymentStatus.PAID || paymentStatus == PaymentStatus.PARTIALLY_PAID
                OrderFormField(
                    value = transactionId,
                    onValueChange = { transactionId = it },
                    label = if (isElectronic && isPaidState) "Transaction / Reference ID *" else "Transaction / Reference ID (Optional)",
                    icon = Icons.Default.ReceiptLong
                )

                OrderFormField(
                    value = paymentNotes,
                    onValueChange = { paymentNotes = it },
                    label = "Payment Notes (Optional)",
                    icon = Icons.Default.Notes,
                    singleLine = false
                )
            }
            item {
                FormSectionTitle("Assignments")
                // Driver Selector
                AssignmentSelector(
                    label = "Assign Driver",
                    selectedId = assignedDriverId,
                    onSelected = { assignedDriverId = it },
                    options = (driversState as? Result.Success)?.data?.filter { it.status == DriverStatus.AVAILABLE }?.map { it.id to it.name } ?: emptyList(),
                    icon = Icons.Default.Badge
                )
                // Vehicle Selector
                AssignmentSelector(
                    label = "Assign Vehicle",
                    selectedId = assignedVehicleId,
                    onSelected = { assignedVehicleId = it },
                    options = (vehiclesState as? Result.Success)?.data?.filter { it.status == VehicleStatus.AVAILABLE }?.map { it.id to it.vehicleNumber } ?: emptyList(),
                    icon = Icons.Default.LocalShipping
                )
            }
            item {
                FormSectionTitle("Additional Info")
                OrderFormField(value = remarks, onValueChange = { remarks = it }, label = "Remarks", icon = Icons.Default.Edit, singleLine = false)
            }
        }
    }
}

@Composable
fun FormSectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun OrderFormField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = singleLine,
        maxLines = if (singleLine) 1 else 3,
        colors = com.routecj.admin.presentation.components.routeCJTextFieldColors(
            containerColor = Color.White,
            textColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )
}

@Composable
fun AssignmentSelector(label: String, selectedId: String?, onSelected: (String?) -> Unit, options: List<Pair<String, String>>, icon: ImageVector) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.find { it.first == selectedId }?.second ?: "Choose..."
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(12.dp)) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = displayText, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                DropdownMenuItem(text = { Text("None") }, onClick = { onSelected(null); expanded = false })
                options.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
                }
            }
        }
    }
}
