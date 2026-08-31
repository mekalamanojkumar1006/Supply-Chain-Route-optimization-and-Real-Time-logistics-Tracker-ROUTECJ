package com.routecj.admin.presentation.godowns

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
import com.routecj.admin.domain.model.Godown
import com.routecj.admin.domain.model.GodownStatus
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditGodownScreen(
    navController: NavController,
    editGodownId: String? = null,
    viewModel: GodownViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val actionState by viewModel.actionState.collectAsStateWithLifecycle()
    val managers by viewModel.eligibleManagers.collectAsStateWithLifecycle()

    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var pincode by remember { mutableStateOf("") }
    var capacity by remember { mutableStateOf("") }
    var currentStock by remember { mutableStateOf("0") }
    var managerId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf(GodownStatus.ACTIVE) }
    var phone by remember { mutableStateOf("") }
    
    var existingGodown by remember { mutableStateOf<Godown?>(null) }

    LaunchedEffect(editGodownId) {
        if (editGodownId != null) {
            val res = viewModel.getGodownById(editGodownId)
            if (res is Result.Success) {
                val g = res.data
                existingGodown = g
                name = g.name
                address = g.address
                city = g.city
                state = g.state
                pincode = g.pincode
                capacity = g.capacity.toString()
                currentStock = g.currentStock.toString()
                managerId = g.managerId
                status = g.status
                phone = g.phone
            }
        }
    }

    LaunchedEffect(actionState) {
        if (actionState is Result.Success) {
            Toast.makeText(context, "Godown saved successfully", Toast.LENGTH_SHORT).show()
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
                title = { Text(if (editGodownId == null) "Add Godown" else "Edit Godown", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 8.dp, shadowElevation = 16.dp) {
                Button(
                    onClick = {
                        val cap = capacity.toDoubleOrNull() ?: 0.0
                        val stock = currentStock.toDoubleOrNull() ?: 0.0
                        val selectedManager = managers.find { it.uid == managerId }
                        
                        val godown = Godown(
                            id = editGodownId ?: "",
                            name = name,
                            address = address,
                            city = city,
                            state = state,
                            pincode = pincode,
                            capacity = cap,
                            currentStock = stock,
                            managerId = managerId,
                            managerName = selectedManager?.name,
                            phone = phone,
                            status = status,
                            createdAt = existingGodown?.createdAt ?: Date(),
                            updatedAt = Date()
                        )
                        if (editGodownId == null) viewModel.createGodown(godown) else viewModel.updateGodown(godown)
                    },
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = actionState !is Result.Loading
                ) {
                    if (actionState is Result.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text("Save Godown")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                FormSectionTitle("General Information")
                GodownFormField(value = name, onValueChange = { name = it }, label = "Godown Name", icon = Icons.Default.Store)
                GodownFormField(value = phone, onValueChange = { phone = it }, label = "Contact Phone", icon = Icons.Default.Phone)
            }
            item {
                FormSectionTitle("Location Details")
                GodownFormField(value = address, onValueChange = { address = it }, label = "Address", icon = Icons.Default.Home)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GodownFormField(value = city, onValueChange = { city = it }, label = "City", icon = Icons.Default.LocationCity) }
                    Box(modifier = Modifier.weight(1f)) { GodownFormField(value = state, onValueChange = { state = it }, label = "State", icon = Icons.Default.Map) }
                }
                GodownFormField(value = pincode, onValueChange = { pincode = it }, label = "Pincode", icon = Icons.Default.PinDrop)
            }
            item {
                FormSectionTitle("Capacity & Management")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(modifier = Modifier.weight(1f)) { GodownFormField(value = capacity, onValueChange = { capacity = it }, label = "Capacity (T)", icon = Icons.Default.Scale, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)) }
                    Box(modifier = Modifier.weight(1f)) { GodownFormField(value = currentStock, onValueChange = { currentStock = it }, label = "Stock (T)", icon = Icons.Default.Inventory, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal)) }
                }
                
                // Manager Selector
                ManagerSelector(
                    selectedId = managerId,
                    onSelected = { managerId = it },
                    options = managers.map { it.uid to it.name }
                )

                // Status Selector
                var showStatusMenu by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    OutlinedTextField(
                        value = status.name,
                        onValueChange = {},
                        label = { Text("Status") },
                        readOnly = true,
                        leadingIcon = { Icon(Icons.Default.Info, null, tint = MaterialTheme.colorScheme.primary) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { IconButton(onClick = { showStatusMenu = true }) { Icon(Icons.Default.ArrowDropDown, null) } }
                    )
                    DropdownMenu(expanded = showStatusMenu, onDismissRequest = { showStatusMenu = false }) {
                        GodownStatus.entries.forEach { st ->
                            DropdownMenuItem(text = { Text(st.name) }, onClick = { status = st; showStatusMenu = false })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FormSectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
fun GodownFormField(value: String, onValueChange: (String) -> Unit, label: String, icon: ImageVector, keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        keyboardOptions = keyboardOptions,
        colors = com.routecj.admin.presentation.components.routeCJTextFieldColors(
            containerColor = Color.White,
            textColor = Color(0xFF0F172A),
            unfocusedBorderColor = Color(0xFFCBD5E1)
        )
    )
}

@Composable
fun ManagerSelector(selectedId: String?, onSelected: (String?) -> Unit, options: List<Pair<String, String>>) {
    var expanded by remember { mutableStateOf(false) }
    val displayText = options.find { it.first == selectedId }?.second ?: "Select Manager"
    
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("Godown Manager", style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.padding(start = 4.dp, bottom = 4.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Badge, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(12.dp))
                Text(displayText, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface)
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { (id, name) ->
                    DropdownMenuItem(text = { Text(name) }, onClick = { onSelected(id); expanded = false })
                }
            }
        }
    }
}
