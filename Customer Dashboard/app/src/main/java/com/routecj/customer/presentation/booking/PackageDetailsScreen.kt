package com.routecj.customer.presentation.booking

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.routecj.customer.presentation.components.*
import com.routecj.customer.ui.theme.RouteCJSpacing
import com.routecj.customer.ui.theme.StitchTonalBorder

data class PackageTypeOption(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val isFragile: Boolean = false
)

@Composable
fun PackageDetailsScreen(
    viewModel: BookingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateNext: () -> Unit
) {
    val pkgState by viewModel.packageState.collectAsState()

    val packageTypes = listOf(
        PackageTypeOption("Document", "Document", Icons.Default.Description),
        PackageTypeOption("Small Parcel", "Small Parcel", Icons.Default.Widgets),
        PackageTypeOption("Medium Parcel", "Medium Parcel", Icons.Default.ViewInAr),
        PackageTypeOption("Large Parcel", "Large Parcel", Icons.Default.Inventory2)
    )

    var selectedType by remember { mutableStateOf(pkgState.packageType ?: "Small Parcel") }
    var itemDesc by remember { mutableStateOf(pkgState.itemDescription ?: "") }
    var count by remember { mutableStateOf((pkgState.packageCount ?: 1).coerceAtLeast(1)) }
    var weight by remember { mutableStateOf((pkgState.weight ?: 2.0).coerceAtLeast(0.5)) }
    var isFragile by remember { mutableStateOf(pkgState.packageType == "Fragile Items") }

    Scaffold(
        topBar = {
            RouteCJTopBar(
                title = "Create Delivery",
                onBackClick = onNavigateBack
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                border = BorderStroke(1.dp, StitchTonalBorder)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .navigationBarsPadding()
                ) {
                    RouteCJButton(
                        text = "Continue",
                        onClick = {
                            viewModel.updatePackage(
                                packageType = if (isFragile) "Fragile Items" else selectedType,
                                itemDescription = itemDesc,
                                packageCount = count.toString(),
                                weight = "%.1f".format(weight),
                                specialInstructions = pkgState.specialInstructions ?: ""
                            )
                            onNavigateNext()
                        }
                    )
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = RouteCJSpacing.Default)
                .verticalScroll(rememberScrollState())
        ) {
            // Step Progress Header
            RouteCJStepProgressHeader(
                currentStep = 3,
                totalSteps = 4,
                stepTitle = "Package Details"
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Package Type Section Header
            Text(
                text = "Package Type",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2x2 Grid of Package Types
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    packageTypes.take(2).forEach { pkg ->
                        val isSelected = !isFragile && selectedType == pkg.id
                        PackageTypeCard(
                            option = pkg,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isFragile = false
                                selectedType = pkg.id
                            }
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    packageTypes.drop(2).forEach { pkg ->
                        val isSelected = !isFragile && selectedType == pkg.id
                        PackageTypeCard(
                            option = pkg,
                            isSelected = isSelected,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                isFragile = false
                                selectedType = pkg.id
                            }
                        )
                    }
                }

                // Fragile Card (Full Width)
                val fragileBg by animateColorAsState(
                    targetValue = if (isFragile) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer,
                    label = "fragileBg"
                )
                val fragileBorder by animateColorAsState(
                    targetValue = if (isFragile) MaterialTheme.colorScheme.primary else StitchTonalBorder,
                    label = "fragileBorder"
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            isFragile = !isFragile
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = fragileBg,
                    border = BorderStroke(1.dp, fragileBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrokenImage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fragile Items",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Glass, electronics, delicate cargo",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isFragile) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .then(
                                    if (!isFragile) Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHighest) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isFragile) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Weight Section with Stepper
            Text(
                text = "Weight",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            RouteCJCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (weight > 0.5) weight -= 0.5 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "%.1f".format(weight),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "kg",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { weight += 0.5 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Item Count Section with Stepper
            Text(
                text = "Item Count",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            RouteCJCard(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (count > 1) count -= 1 },
                        enabled = count > 1,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Decrease",
                            tint = if (count > 1) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (count == 1) "item" else "items",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    IconButton(
                        onClick = { count += 1 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Increase",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Item Description (Optional)
            RouteCJTextField(
                value = itemDesc,
                onValueChange = { itemDesc = it },
                label = "Item Description (Optional)",
                placeholder = "e.g. Electronics, Spare Parts, Clothes",
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PackageTypeCard(
    option: PackageTypeOption,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceContainer,
        label = "pkg_card_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.primary else StitchTonalBorder,
        label = "pkg_card_border"
    )

    Surface(
        modifier = modifier
            .height(110.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = bgColor,
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = option.name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
