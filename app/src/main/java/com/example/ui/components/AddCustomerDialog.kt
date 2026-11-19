package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerEntity
import com.example.ui.theme.WaterRiderTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomerDialog(
    existingCustomer: CustomerEntity? = null,
    defaultPurifiedPrice: Double = 35.0,
    defaultAlkalinePrice: Double = 45.0,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Double, Double, Boolean) -> Unit
) {
    val themeColors = WaterRiderTheme.colors
    var name by remember { mutableStateOf(existingCustomer?.name ?: "") }
    var address by remember { mutableStateOf(existingCustomer?.address ?: "") }
    var customerType by remember { mutableStateOf(existingCustomer?.customerType ?: "Residential") }
    var isBankDirectAccount by remember {
        mutableStateOf(
            existingCustomer?.isBankDirectAccount == true ||
                existingCustomer?.customerType == "Bank / Direct"
        )
    }
    var purifiedPrice by remember { mutableStateOf((existingCustomer?.defaultPurifiedPrice ?: defaultPurifiedPrice).toString()) }
    var alkalinePrice by remember { mutableStateOf((existingCustomer?.defaultAlkalinePrice ?: defaultAlkalinePrice).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (existingCustomer == null) "Add New Customer" else "Edit Customer Profile",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Customer Name") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Address / Route / Landmark") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Text("Customer Type:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = customerType == "Residential",
                        onClick = {
                            customerType = "Residential"
                            isBankDirectAccount = false
                            if (existingCustomer == null) {
                                purifiedPrice = defaultPurifiedPrice.toString()
                                alkalinePrice = defaultAlkalinePrice.toString()
                            }
                        },
                        label = { Text("Residential", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1f).height(44.dp)
                    )
                    FilterChip(
                        selected = customerType == "Outlet",
                        onClick = {
                            customerType = "Outlet"
                            isBankDirectAccount = false
                            if (existingCustomer == null) {
                                purifiedPrice = "25.0"
                                alkalinePrice = "35.0"
                            }
                        },
                        label = { Text("Outlet", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(0.85f).height(44.dp)
                    )
                    FilterChip(
                        selected = customerType == "Bank / Direct",
                        onClick = {
                            customerType = "Bank / Direct"
                            isBankDirectAccount = true
                            if (existingCustomer == null) {
                                purifiedPrice = defaultPurifiedPrice.toString()
                                alkalinePrice = defaultAlkalinePrice.toString()
                            }
                        },
                        label = { Text("Bank / Direct", fontSize = 10.5.sp, maxLines = 1, softWrap = false) },
                        modifier = Modifier.weight(1.15f).height(44.dp)
                    )
                }

                // Bank / Direct-to-Boss Account Switch
                Surface(
                    color = if (isBankDirectAccount) themeColors.secondaryContainer.copy(alpha = 0.72f) else themeColors.surfaceVariant,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                            Text(
                                "Bank / Direct-to-Boss Account",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isBankDirectAccount) {
                                    if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText
                                } else themeColors.textPrimary
                            )
                            Text(
                                "Earns commission immediately; excluded from cash remit.",
                                fontSize = 11.sp,
                                color = if (isBankDirectAccount && themeColors.isDark) {
                                    themeColors.onSecondaryContainer.copy(alpha = 0.9f)
                                } else themeColors.textSecondary
                            )
                        }
                        Switch(
                            checked = isBankDirectAccount,
                            onCheckedChange = { checked ->
                                isBankDirectAccount = checked
                                if (checked) {
                                    customerType = "Bank / Direct"
                                } else if (customerType == "Bank / Direct") {
                                    customerType = "Residential"
                                }
                            },
                            modifier = Modifier.testTag("customer_bank_direct_switch")
                        )
                    }
                }

                // Quick Price Tier Presets
                Text("Quick Price Tier Presets:", fontSize = 12.sp, color = themeColors.textSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val purifiedTiers = if (customerType == "Outlet") listOf("25", "30") else listOf("35", "40", "45")
                    purifiedTiers.forEach { tier ->
                        AssistChip(
                            onClick = { purifiedPrice = "$tier.0" },
                            label = { Text("Pur ₱$tier", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val alkalineTiers = if (customerType == "Outlet") listOf("30", "35") else listOf("40", "45", "50")
                    alkalineTiers.forEach { tier ->
                        AssistChip(
                            onClick = { alkalinePrice = "$tier.0" },
                            label = { Text("Alk ₱$tier", fontSize = 11.sp, maxLines = 1, softWrap = false) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = purifiedPrice,
                        onValueChange = { purifiedPrice = it },
                        label = { Text("Purified ₱/Gal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_purified_price_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = alkalinePrice,
                        onValueChange = { alkalinePrice = it },
                        label = { Text("Alkaline ₱/Gal") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("customer_alkaline_price_input"),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            name.trim(),
                            address.trim(),
                            customerType,
                            purifiedPrice.toDoubleOrNull() ?: 35.0,
                            alkalinePrice.toDoubleOrNull() ?: 45.0,
                            isBankDirectAccount
                        )
                    }
                },
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Text(if (existingCustomer == null) "Save Customer" else "Update Customer")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}

