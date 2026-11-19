package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.CustomerEntity
import com.example.data.DeliveryEntity
import com.example.ui.theme.WaterRiderTheme

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeliveryFormDialog(
    customers: List<CustomerEntity>,
    initialCustomer: CustomerEntity? = null,
    existingDelivery: DeliveryEntity? = null,
    defaultPurifiedPrice: Double = 35.0,
    defaultAlkalinePrice: Double = 45.0,
    purifiedPreset1: Double = 25.0,
    purifiedPreset2: Double = 35.0,
    alkalinePreset1: Double = 30.0,
    alkalinePreset2: Double = 45.0,
    onDismiss: () -> Unit,
    onSaveNew: (
        customerName: String,
        address: String,
        customerType: String,
        purifiedQty: Int,
        alkalineQty: Int,
        freePurifiedQty: Int,
        freeAlkalineQty: Int,
        purifiedPrice: Double,
        alkalinePrice: Double,
        customDiscount: Double,
        utangPaidAmount: Double,
        cashGiven: Double,
        paymentStatus: String,
        orderSource: String
    ) -> Unit,
    onUpdateExisting: ((
        deliveryId: Long,
        purifiedQty: Int,
        alkalineQty: Int,
        freePurifiedQty: Int,
        freeAlkalineQty: Int,
        purifiedPrice: Double,
        alkalinePrice: Double,
        customDiscount: Double,
        utangPaidAmount: Double,
        cashGiven: Double,
        paymentStatus: String,
        orderSource: String
    ) -> Unit)? = null,
    onDeleteExisting: ((Long) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    val purHeaderColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
    val alkHeaderColor = if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText
    val utangAlertTextColor = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
    val isEditing = existingDelivery != null

    val matchedCustomerForEdit = remember(existingDelivery, initialCustomer, customers) {
        if (existingDelivery != null) customers.find { it.id == existingDelivery.customerId } else initialCustomer
    }

    var currentStep by remember {
        mutableIntStateOf(if (isEditing || initialCustomer != null) 2 else 1)
    }

    var customerNameQuery by remember {
        mutableStateOf(existingDelivery?.customerName ?: initialCustomer?.name ?: "")
    }
    var selectedCustomer by remember {
        mutableStateOf<CustomerEntity?>(matchedCustomerForEdit)
    }
    var customerAddress by remember {
        mutableStateOf(existingDelivery?.customerAddress ?: initialCustomer?.address ?: "")
    }
    var customerType by remember {
        mutableStateOf(matchedCustomerForEdit?.customerType ?: "Residential")
    }

    // Gallon quantities start at "0" (unless editing) and tap-to-clear clears "0" -> "" on focus
    var purifiedQtyStr by remember {
        mutableStateOf(existingDelivery?.purifiedQty?.toString() ?: "0")
    }
    var alkalineQtyStr by remember {
        mutableStateOf(existingDelivery?.alkalineQty?.toString() ?: "0")
    }
    var freePurifiedStr by remember {
        mutableStateOf(existingDelivery?.freePurifiedQty?.toString() ?: "0")
    }
    var freeAlkalineStr by remember {
        mutableStateOf(existingDelivery?.freeAlkalineQty?.toString() ?: "0")
    }

    val initialPurifiedPrice = existingDelivery?.purifiedPrice
        ?: initialCustomer?.defaultPurifiedPrice
        ?: defaultPurifiedPrice
    val initialAlkalinePrice = existingDelivery?.alkalinePrice
        ?: initialCustomer?.defaultAlkalinePrice
        ?: defaultAlkalinePrice

    var purifiedPriceStr by remember(defaultPurifiedPrice, initialCustomer?.id) {
        mutableStateOf(initialPurifiedPrice.toString())
    }
    var alkalinePriceStr by remember(defaultAlkalinePrice, initialCustomer?.id) {
        mutableStateOf(initialAlkalinePrice.toString())
    }
    var purifiedCustomMode by remember(purifiedPreset1, purifiedPreset2, defaultPurifiedPrice, initialCustomer?.id) {
        mutableStateOf(
            kotlin.math.abs(initialPurifiedPrice - purifiedPreset1) >= 0.01 &&
                kotlin.math.abs(initialPurifiedPrice - purifiedPreset2) >= 0.01
        )
    }
    var alkalineCustomMode by remember(alkalinePreset1, alkalinePreset2, defaultAlkalinePrice, initialCustomer?.id) {
        mutableStateOf(
            kotlin.math.abs(initialAlkalinePrice - alkalinePreset1) >= 0.01 &&
                kotlin.math.abs(initialAlkalinePrice - alkalinePreset2) >= 0.01
        )
    }
    var discountStr by remember {
        mutableStateOf(
            if (existingDelivery != null && existingDelivery.customDiscount > 0.0) existingDelivery.customDiscount.toString() else ""
        )
    }

    var settleUtangTogether by remember {
        mutableStateOf(existingDelivery != null && existingDelivery.utangApplied > 0.0)
    }
    var utangPayStr by remember {
        mutableStateOf(
            if (existingDelivery != null && existingDelivery.utangApplied > 0.0) existingDelivery.utangApplied.toString() else ""
        )
    }
    var cashGivenStr by remember {
        mutableStateOf(
            if (existingDelivery != null && existingDelivery.amountPaid > 0.0) existingDelivery.amountPaid.toString() else ""
        )
    }

    var orderSource by remember {
        mutableStateOf(existingDelivery?.orderSource ?: "Route Delivery")
    }
    var paymentStatus by remember {
        mutableStateOf(
            when {
                existingDelivery != null -> when (existingDelivery.paymentStatus) {
                    "Partial" -> "Paid"
                    else -> existingDelivery.paymentStatus
                }
                initialCustomer?.isBankDirectAccount == true || initialCustomer?.customerType == "Bank / Direct" -> "Direct to Boss"
                else -> "Paid"
            }
        )
    }

    val isSelectedBankDirect by remember {
        derivedStateOf {
            selectedCustomer?.let {
                it.isBankDirectAccount || it.customerType == "Bank / Direct"
            } ?: (customerType == "Bank / Direct")
        }
    }

    val existingUtang by remember {
        derivedStateOf { selectedCustomer?.currentBalance ?: 0.0 }
    }
    LaunchedEffect(selectedCustomer?.id, existingUtang) {
        if (selectedCustomer != null && existingUtang > 0.0 && !isEditing) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val purifiedQty by remember {
        derivedStateOf { (purifiedQtyStr.toIntOrNull() ?: 0).coerceAtLeast(0) }
    }
    val alkalineQty by remember {
        derivedStateOf { (alkalineQtyStr.toIntOrNull() ?: 0).coerceAtLeast(0) }
    }
    val freePurifiedQty by remember {
        derivedStateOf { (freePurifiedStr.toIntOrNull() ?: 0).coerceIn(0, purifiedQty) }
    }
    val freeAlkalineQty by remember {
        derivedStateOf { (freeAlkalineStr.toIntOrNull() ?: 0).coerceIn(0, alkalineQty) }
    }

    val paidPurified by remember {
        derivedStateOf { maxOf(0, purifiedQty - freePurifiedQty) }
    }
    val paidAlkaline by remember {
        derivedStateOf { maxOf(0, alkalineQty - freeAlkalineQty) }
    }

    val currentPurifiedPrice by remember(defaultPurifiedPrice) {
        derivedStateOf { purifiedPriceStr.toDoubleOrNull() ?: defaultPurifiedPrice }
    }
    val currentAlkalinePrice by remember(defaultAlkalinePrice) {
        derivedStateOf { alkalinePriceStr.toDoubleOrNull() ?: defaultAlkalinePrice }
    }
    val discount by remember {
        derivedStateOf { discountStr.toDoubleOrNull() ?: 0.0 }
    }

    val orderSubtotal by remember(defaultPurifiedPrice, defaultAlkalinePrice) {
        derivedStateOf {
            maxOf(
                0.0,
                (paidPurified * currentPurifiedPrice) + (paidAlkaline * currentAlkalinePrice) - discount
            )
        }
    }
    val creditAvailable by remember {
        derivedStateOf { selectedCustomer?.storedCredit ?: 0.0 }
    }
    val afterCreditSubtotal by remember(defaultPurifiedPrice, defaultAlkalinePrice) {
        derivedStateOf { maxOf(0.0, orderSubtotal - creditAvailable) }
    }

    val utangPay by remember {
        derivedStateOf {
            if (settleUtangTogether) {
                (utangPayStr.toDoubleOrNull() ?: existingUtang).coerceIn(0.0, existingUtang)
            } else 0.0
        }
    }

    val totalDue by remember(defaultPurifiedPrice, defaultAlkalinePrice) {
        derivedStateOf { afterCreditSubtotal + utangPay }
    }
    val combinedPreviewTotal by remember(defaultPurifiedPrice, defaultAlkalinePrice) {
        derivedStateOf { afterCreditSubtotal + existingUtang }
    }
    val cashGiven by remember(defaultPurifiedPrice, defaultAlkalinePrice) {
        derivedStateOf { cashGivenStr.toDoubleOrNull() ?: totalDue }
    }

    val matchingCustomers = remember(customerNameQuery, customers) {
        if (customerNameQuery.isBlank()) customers.take(4)
        else customers.filter {
            it.name.contains(customerNameQuery, ignoreCase = true) ||
                it.address.contains(customerNameQuery, ignoreCase = true)
        }
    }

    val isNewTypedCustomer by remember(isEditing, customers) {
        derivedStateOf {
            !isEditing &&
                customerNameQuery.isNotBlank() &&
                selectedCustomer == null &&
                customers.none { it.name.equals(customerNameQuery.trim(), ignoreCase = true) }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 780.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Header + Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Delivery" else "New Water Delivery",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = when (currentStep) {
                                1 -> "Step 1 of 3 • Customer & Source"
                                2 -> "Step 2 of 3 • Purified & Alkaline"
                                else -> "Step 3 of 3 • Free Gallons & Payment"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("wizard_close_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3-Step Visual Progress Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (1..3).forEach { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(
                                    if (step <= currentStep) themeColors.primaryAccent
                                    else themeColors.border.copy(alpha = 0.5f)
                                )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Animated 3-Step Body
                AnimatedContent(
                    targetState = currentStep,
                    transitionSpec = {
                        val movingForward = targetState > initialState
                        (slideInHorizontally(animationSpec = tween(260)) { width ->
                            if (movingForward) width / 2 else -width / 2
                        } + fadeIn(animationSpec = tween(220))).togetherWith(
                            slideOutHorizontally(animationSpec = tween(220)) { width ->
                                if (movingForward) -width / 2 else width / 2
                            } + fadeOut(animationSpec = tween(180))
                        )
                    },
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth(),
                    label = "DeliveryWizardStepTransition"
                ) { step ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .animateContentSize()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (step) {
                            // ================= STEP 1: Customer Name & Order Source =================
                            1 -> {
                                OutlinedTextField(
                                    value = customerNameQuery,
                                    onValueChange = {
                                        if (!isEditing) {
                                            customerNameQuery = it
                                            val exactMatch = customers.find { c ->
                                                c.name.equals(it.trim(), ignoreCase = true)
                                            }
                                            selectedCustomer = exactMatch
                                            if (exactMatch != null) {
                                                customerAddress = exactMatch.address
                                                customerType = exactMatch.customerType
                                                purifiedPriceStr = exactMatch.defaultPurifiedPrice.toString()
                                                alkalinePriceStr = exactMatch.defaultAlkalinePrice.toString()
                                                purifiedCustomMode = kotlin.math.abs(exactMatch.defaultPurifiedPrice - purifiedPreset1) >= 0.01 &&
                                                    kotlin.math.abs(exactMatch.defaultPurifiedPrice - purifiedPreset2) >= 0.01
                                                alkalineCustomMode = kotlin.math.abs(exactMatch.defaultAlkalinePrice - alkalinePreset1) >= 0.01 &&
                                                    kotlin.math.abs(exactMatch.defaultAlkalinePrice - alkalinePreset2) >= 0.01
                                                if (exactMatch.currentBalance > 0.0) {
                                                    utangPayStr = exactMatch.currentBalance.toString()
                                                }
                                                if (exactMatch.isBankDirectAccount || exactMatch.customerType == "Bank / Direct") {
                                                    paymentStatus = "Direct to Boss"
                                                } else if (paymentStatus == "Direct to Boss") {
                                                    paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                }
                                            } else {
                                                settleUtangTogether = false
                                                utangPayStr = ""
                                                purifiedPriceStr = defaultPurifiedPrice.toString()
                                                alkalinePriceStr = defaultAlkalinePrice.toString()
                                                purifiedCustomMode = kotlin.math.abs(defaultPurifiedPrice - purifiedPreset1) >= 0.01 &&
                                                    kotlin.math.abs(defaultPurifiedPrice - purifiedPreset2) >= 0.01
                                                alkalineCustomMode = kotlin.math.abs(defaultAlkalinePrice - alkalinePreset1) >= 0.01 &&
                                                    kotlin.math.abs(defaultAlkalinePrice - alkalinePreset2) >= 0.01
                                                if (paymentStatus == "Direct to Boss") {
                                                    paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isEditing,
                                    label = { Text("Customer Name") },
                                    placeholder = { Text("Search saved or type new name...") },
                                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "Customer") },
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        keyboardType = KeyboardType.Text
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("wizard_customer_name_input"),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                AnimatedVisibility(visible = selectedCustomer != null) {
                                    selectedCustomer?.let { suki ->
                                        Surface(
                                            color = themeColors.paidContainer,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("suki_price_autofill_badge")
                                        ) {
                                            Text(
                                                text = "★ Suki Custom Price Auto-Filled: Purified ${formatPesoChip(suki.defaultPurifiedPrice)} • Alkaline ${formatPesoChip(suki.defaultAlkalinePrice)}",
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.paid
                                            )
                                        }
                                    }
                                }

                                // Live Customer Autocomplete Suggestions
                                if (!isEditing && matchingCustomers.isNotEmpty() && selectedCustomer == null) {
                                    BigCard(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(4.dp)) {
                                            Text(
                                                if (customerNameQuery.isBlank()) "Quick Select Recent Customers" else "Matching Customers",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = themeColors.textSecondary,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                            )
                                            matchingCustomers.take(4).forEach { match ->
                                                ListItem(
                                                    headlineContent = {
                                                        Text(match.name, fontWeight = FontWeight.Bold)
                                                    },
                                                    supportingContent = {
                                                        Text(
                                                            "${match.address} • P ₱${match.defaultPurifiedPrice.toInt()} / A ₱${match.defaultAlkalinePrice.toInt()}" +
                                                                (if (match.currentBalance > 0) " • Utang ₱%.2f".format(match.currentBalance) else "") +
                                                                (if (match.storedCredit > 0) " • Sukli ₱%.2f".format(match.storedCredit) else ""),
                                                            fontSize = 12.sp
                                                        )
                                                    },
                                                    modifier = Modifier.clickable {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        selectedCustomer = match
                                                        customerNameQuery = match.name
                                                        customerAddress = match.address
                                                        customerType = match.customerType
                                                        purifiedPriceStr = match.defaultPurifiedPrice.toString()
                                                        alkalinePriceStr = match.defaultAlkalinePrice.toString()
                                                        purifiedCustomMode = kotlin.math.abs(match.defaultPurifiedPrice - purifiedPreset1) >= 0.01 &&
                                                            kotlin.math.abs(match.defaultPurifiedPrice - purifiedPreset2) >= 0.01
                                                        alkalineCustomMode = kotlin.math.abs(match.defaultAlkalinePrice - alkalinePreset1) >= 0.01 &&
                                                            kotlin.math.abs(match.defaultAlkalinePrice - alkalinePreset2) >= 0.01
                                                        if (match.currentBalance > 0.0) {
                                                            utangPayStr = match.currentBalance.toString()
                                                        }
                                                        if (match.isBankDirectAccount || match.customerType == "Bank / Direct") {
                                                            paymentStatus = "Direct to Boss"
                                                        } else if (paymentStatus == "Direct to Boss") {
                                                            paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // New Customer Address & Type
                                if (isNewTypedCustomer) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customerAddress,
                                            onValueChange = { customerAddress = it },
                                            label = { Text("Address / Route (New Customer)") },
                                            placeholder = { Text("e.g. Purok 3, Highway") },
                                            keyboardOptions = KeyboardOptions(
                                                capitalization = KeyboardCapitalization.Words,
                                                keyboardType = KeyboardType.Text
                                            ),
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        FlowRow(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            FilterChip(
                                                selected = customerType == "Residential",
                                                onClick = {
                                                    customerType = "Residential"
                                                    purifiedPriceStr = purifiedPreset2.toString()
                                                    alkalinePriceStr = alkalinePreset2.toString()
                                                    purifiedCustomMode = false
                                                    alkalineCustomMode = false
                                                    if (paymentStatus == "Direct to Boss") {
                                                        paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                    }
                                                },
                                                label = { Text("Residential (${formatPesoChip(purifiedPreset2)}/${formatPesoChip(alkalinePreset2)})") },
                                                modifier = Modifier.height(48.dp)
                                            )
                                            FilterChip(
                                                selected = customerType == "Outlet",
                                                onClick = {
                                                    customerType = "Outlet"
                                                    purifiedPriceStr = purifiedPreset1.toString()
                                                    alkalinePriceStr = alkalinePreset1.toString()
                                                    purifiedCustomMode = false
                                                    alkalineCustomMode = false
                                                    if (paymentStatus == "Direct to Boss") {
                                                        paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                    }
                                                },
                                                label = { Text("Outlet (${formatPesoChip(purifiedPreset1)}/${formatPesoChip(alkalinePreset1)})") },
                                                modifier = Modifier.height(48.dp)
                                            )
                                        }
                                        // Bank / Direct-to-Boss Account Switch
                                        Surface(
                                            color = if (customerType == "Bank / Direct") themeColors.secondaryContainer.copy(alpha = 0.72f) else themeColors.surfaceVariant,
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
                                                        color = if (customerType == "Bank / Direct") alkHeaderColor else themeColors.textPrimary
                                                    )
                                                    Text(
                                                        "Earns commission immediately; excluded from cash remit.",
                                                        fontSize = 11.sp,
                                                        color = themeColors.textSecondary
                                                    )
                                                }
                                                Switch(
                                                    checked = customerType == "Bank / Direct",
                                                    onCheckedChange = { checked ->
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        if (checked) {
                                                            customerType = "Bank / Direct"
                                                            purifiedPriceStr = purifiedPreset2.toString()
                                                            alkalinePriceStr = alkalinePreset2.toString()
                                                            purifiedCustomMode = false
                                                            alkalineCustomMode = false
                                                            paymentStatus = "Direct to Boss"
                                                        } else {
                                                            customerType = "Residential"
                                                            if (paymentStatus == "Direct to Boss") {
                                                                paymentStatus = if (orderSource == "Route Delivery") "Paid" else "Paid at Station"
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.testTag("wizard_bank_direct_switch")
                                                )
                                            }
                                        }
                                    }
                                }

                                AnimatedVisibility(visible = !isNewTypedCustomer && isSelectedBankDirect) {
                                    BigCard(
                                        containerColor = themeColors.secondaryContainer.copy(alpha = 0.72f),
                                        borderColor = themeColors.secondaryAccent,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "🏦 Bank / Direct-to-Boss Account • Commission earned immediately, ₱0 physical cash remit.",
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = alkHeaderColor
                                        )
                                    }
                                }

                                // Order Source Toggle
                                Text("Order Source:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = orderSource == "Route Delivery",
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            orderSource = "Route Delivery"
                                            if (paymentStatus == "Paid at Station" || paymentStatus == "Unpaid / Collect on Delivery") {
                                                paymentStatus = "Paid"
                                            }
                                        },
                                        label = { Text("Route Delivery") },
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )
                                    FilterChip(
                                        selected = orderSource == "Station Walk-In",
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            orderSource = "Station Walk-In"
                                            if (paymentStatus !in listOf("Paid at Station", "Unpaid / Collect on Delivery", "Direct to Boss")) {
                                                paymentStatus = "Paid at Station"
                                            }
                                        },
                                        label = { Text("Station Walk-In") },
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = themeColors.expenseContainer,
                                            selectedLabelColor = themeColors.expense
                                        ),
                                        modifier = Modifier.weight(1f).height(48.dp)
                                    )
                                }

                                // Subtle Animated Red Warning Banner for Existing Utang
                                AnimatedVisibility(visible = existingUtang > 0.0) {
                                    BigCard(
                                        containerColor = themeColors.utangContainer,
                                        borderColor = themeColors.utang,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Icon(Icons.Default.Warning, contentDescription = "Utang Alert", tint = utangAlertTextColor)
                                            Column {
                                                Text(
                                                    "⚠️ MAY DATING UTANG: ₱%.2f".format(existingUtang),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = utangAlertTextColor,
                                                    fontSize = 14.sp
                                                )
                                                Text(
                                                    "You can settle this together on Step 3.",
                                                    fontSize = 12.sp,
                                                    color = themeColors.utangSubtext
                                                )
                                            }
                                        }
                                    }
                                }

                                // Stored Barya Credit Banner
                                AnimatedVisibility(visible = creditAvailable > 0.0) {
                                    BigCard(
                                        containerColor = themeColors.paidContainer,
                                        borderColor = themeColors.paid,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "💡 Stored Barya Credit: ₱%.2f (Auto-deducted from bill)".format(creditAvailable),
                                            modifier = Modifier.padding(12.dp),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = themeColors.paid
                                        )
                                    }
                                }
                            }

                            // ================= STEP 2: Purified & Alkaline Quantities =================
                            2 -> {
                                // Selected Customer Pill Summary
                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            customerNameQuery.ifBlank { "Customer" },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            orderSource,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = themeColors.primaryAccent
                                        )
                                    }
                                }

                                // PURIFIED WATER BIG CARD
                                BigCard(
                                    containerColor = themeColors.primaryContainer.copy(alpha = 0.55f),
                                    borderColor = themeColors.primaryAccent.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .animateContentSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Purified Water",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                color = purHeaderColor
                                            )
                                            Surface(
                                                color = themeColors.primaryContainer,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    "${formatPesoChip(currentPurifiedPrice)} / gal",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = purHeaderColor
                                                )
                                            }
                                        }

                                        // Quick-Tap Price Chips (Preset 1, Preset 2, Custom)
                                        val purifiedIsP1 by remember(purifiedPreset1, defaultPurifiedPrice) {
                                            derivedStateOf {
                                                !purifiedCustomMode && kotlin.math.abs(currentPurifiedPrice - purifiedPreset1) < 0.01
                                            }
                                        }
                                        val purifiedIsP2 by remember(purifiedPreset1, purifiedPreset2, defaultPurifiedPrice) {
                                            derivedStateOf {
                                                !purifiedCustomMode && kotlin.math.abs(currentPurifiedPrice - purifiedPreset2) < 0.01 && !purifiedIsP1
                                            }
                                        }
                                        val purifiedIsCustom by remember(purifiedPreset1, purifiedPreset2, defaultPurifiedPrice) {
                                            derivedStateOf {
                                                purifiedCustomMode || (!purifiedIsP1 && !purifiedIsP2)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SpringPriceChip(
                                                label = formatPesoChip(purifiedPreset1),
                                                selected = purifiedIsP1,
                                                accentColor = themeColors.primaryAccent,
                                                onClick = {
                                                    purifiedCustomMode = false
                                                    purifiedPriceStr = purifiedPreset1.toString()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("purified_price_preset1")
                                            )
                                            SpringPriceChip(
                                                label = formatPesoChip(purifiedPreset2),
                                                selected = purifiedIsP2,
                                                accentColor = themeColors.primaryAccent,
                                                onClick = {
                                                    purifiedCustomMode = false
                                                    purifiedPriceStr = purifiedPreset2.toString()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("purified_price_preset2")
                                            )
                                            SpringPriceChip(
                                                label = "Custom",
                                                selected = purifiedIsCustom,
                                                accentColor = themeColors.primaryAccent,
                                                onClick = {
                                                    purifiedCustomMode = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("purified_price_custom")
                                            )
                                        }

                                        AnimatedVisibility(visible = purifiedIsCustom) {
                                            OutlinedTextField(
                                                value = purifiedPriceStr,
                                                onValueChange = { purifiedPriceStr = it },
                                                label = { Text("Custom Purified Price / Gal (₱)") },
                                                placeholder = { Text("0.0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Gallons", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SpringBounceStepperButton(
                                                    icon = Icons.Default.Remove,
                                                    contentDescription = "Decrease Purified",
                                                    onClick = {
                                                        val next = (purifiedQty - 1).coerceAtLeast(0)
                                                        purifiedQtyStr = next.toString()
                                                        if (freePurifiedQty > next) {
                                                            freePurifiedStr = next.toString()
                                                        }
                                                    },
                                                    containerColor = themeColors.primaryContainer,
                                                    contentColor = themeColors.onPrimaryContainer,
                                                    buttonSize = 48.dp
                                                )
                                                OutlinedTextField(
                                                    value = purifiedQtyStr,
                                                    onValueChange = { input ->
                                                        val digits = input.filter { it.isDigit() }
                                                        purifiedQtyStr = digits
                                                        val newQty = digits.toIntOrNull() ?: 0
                                                        if (freePurifiedQty > newQty) {
                                                            freePurifiedStr = newQty.toString()
                                                        }
                                                    },
                                                    placeholder = {
                                                        Text(
                                                            "0",
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = LocalTextStyle.current.copy(
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 20.sp
                                                    ),
                                                    modifier = Modifier
                                                        .width(70.dp)
                                                        .testTag("purified_qty_input")
                                                        .onFocusChanged { focusState ->
                                                            if (focusState.isFocused && purifiedQtyStr == "0") {
                                                                purifiedQtyStr = ""
                                                            } else if (!focusState.isFocused && purifiedQtyStr.isEmpty()) {
                                                                purifiedQtyStr = "0"
                                                            }
                                                        }
                                                )
                                                SpringBounceStepperButton(
                                                    icon = Icons.Default.Add,
                                                    contentDescription = "Increase Purified",
                                                    onClick = {
                                                        purifiedQtyStr = (purifiedQty + 1).toString()
                                                    },
                                                    containerColor = themeColors.primaryAccent,
                                                    contentColor = themeColors.onPrimaryAccent,
                                                    buttonSize = 48.dp
                                                )
                                            }
                                        }
                                    }
                                }

                                // ALKALINE WATER BIG CARD
                                BigCard(
                                    containerColor = themeColors.secondaryContainer.copy(alpha = 0.55f),
                                    borderColor = themeColors.secondaryAccent.copy(alpha = 0.6f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .padding(14.dp)
                                            .animateContentSize(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Alkaline Water",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 17.sp,
                                                color = alkHeaderColor
                                            )
                                            Surface(
                                                color = themeColors.secondaryContainer,
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Text(
                                                    "${formatPesoChip(currentAlkalinePrice)} / gal",
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    color = alkHeaderColor
                                                )
                                            }
                                        }

                                        // Quick-Tap Price Chips (Preset 1, Preset 2, Custom)
                                        val alkalineIsP1 by remember(alkalinePreset1, defaultAlkalinePrice) {
                                            derivedStateOf {
                                                !alkalineCustomMode && kotlin.math.abs(currentAlkalinePrice - alkalinePreset1) < 0.01
                                            }
                                        }
                                        val alkalineIsP2 by remember(alkalinePreset1, alkalinePreset2, defaultAlkalinePrice) {
                                            derivedStateOf {
                                                !alkalineCustomMode && kotlin.math.abs(currentAlkalinePrice - alkalinePreset2) < 0.01 && !alkalineIsP1
                                            }
                                        }
                                        val alkalineIsCustom by remember(alkalinePreset1, alkalinePreset2, defaultAlkalinePrice) {
                                            derivedStateOf {
                                                alkalineCustomMode || (!alkalineIsP1 && !alkalineIsP2)
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            SpringPriceChip(
                                                label = formatPesoChip(alkalinePreset1),
                                                selected = alkalineIsP1,
                                                accentColor = themeColors.secondaryAccent,
                                                onClick = {
                                                    alkalineCustomMode = false
                                                    alkalinePriceStr = alkalinePreset1.toString()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("alkaline_price_preset1")
                                            )
                                            SpringPriceChip(
                                                label = formatPesoChip(alkalinePreset2),
                                                selected = alkalineIsP2,
                                                accentColor = themeColors.secondaryAccent,
                                                onClick = {
                                                    alkalineCustomMode = false
                                                    alkalinePriceStr = alkalinePreset2.toString()
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("alkaline_price_preset2")
                                            )
                                            SpringPriceChip(
                                                label = "Custom",
                                                selected = alkalineIsCustom,
                                                accentColor = themeColors.secondaryAccent,
                                                onClick = {
                                                    alkalineCustomMode = true
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("alkaline_price_custom")
                                            )
                                        }

                                        AnimatedVisibility(visible = alkalineIsCustom) {
                                            OutlinedTextField(
                                                value = alkalinePriceStr,
                                                onValueChange = { alkalinePriceStr = it },
                                                label = { Text("Custom Alkaline Price / Gal (₱)") },
                                                placeholder = { Text("0.0") },
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                modifier = Modifier.fillMaxWidth(),
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Gallons", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                SpringBounceStepperButton(
                                                    icon = Icons.Default.Remove,
                                                    contentDescription = "Decrease Alkaline",
                                                    onClick = {
                                                        val next = (alkalineQty - 1).coerceAtLeast(0)
                                                        alkalineQtyStr = next.toString()
                                                        if (freeAlkalineQty > next) {
                                                            freeAlkalineStr = next.toString()
                                                        }
                                                    },
                                                    containerColor = themeColors.secondaryContainer,
                                                    contentColor = themeColors.onSecondaryContainer,
                                                    buttonSize = 48.dp
                                                )
                                                OutlinedTextField(
                                                    value = alkalineQtyStr,
                                                    onValueChange = { input ->
                                                        val digits = input.filter { it.isDigit() }
                                                        alkalineQtyStr = digits
                                                        val newQty = digits.toIntOrNull() ?: 0
                                                        if (freeAlkalineQty > newQty) {
                                                            freeAlkalineStr = newQty.toString()
                                                        }
                                                    },
                                                    placeholder = {
                                                        Text(
                                                            "0",
                                                            modifier = Modifier.fillMaxWidth(),
                                                            textAlign = TextAlign.Center
                                                        )
                                                    },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                    singleLine = true,
                                                    textStyle = LocalTextStyle.current.copy(
                                                        textAlign = TextAlign.Center,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 20.sp
                                                    ),
                                                    modifier = Modifier
                                                        .width(70.dp)
                                                        .testTag("alkaline_qty_input")
                                                        .onFocusChanged { focusState ->
                                                            if (focusState.isFocused && alkalineQtyStr == "0") {
                                                                alkalineQtyStr = ""
                                                            } else if (!focusState.isFocused && alkalineQtyStr.isEmpty()) {
                                                                alkalineQtyStr = "0"
                                                            }
                                                        }
                                                )
                                                SpringBounceStepperButton(
                                                    icon = Icons.Default.Add,
                                                    contentDescription = "Increase Alkaline",
                                                    onClick = {
                                                        alkalineQtyStr = (alkalineQty + 1).toString()
                                                    },
                                                    containerColor = themeColors.secondaryAccent,
                                                    contentColor = themeColors.onSecondaryAccent,
                                                    buttonSize = 48.dp
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ================= STEP 3: Free/Damaged, Discount & Payment =================
                            else -> {
                                // Free / Damaged Gallons Card (only showing ordered types)
                                BigCard(
                                    containerColor = themeColors.surface,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Free / Damaged Gallons",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.sp,
                                            color = themeColors.primaryAccent
                                        )
                                        Text(
                                            "Free/Damaged gallons are deducted from the customer's bill and earn ₱0 rider commission.",
                                            fontSize = 12.sp,
                                            color = themeColors.textSecondary
                                        )

                                        if (purifiedQty > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Free Purified (max $purifiedQty)",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = themeColors.primaryAccent,
                                                    fontSize = 14.sp
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    SpringBounceStepperButton(
                                                        icon = Icons.Default.Remove,
                                                        contentDescription = "Decrease Free Purified",
                                                        onClick = {
                                                            freePurifiedStr = (freePurifiedQty - 1).coerceAtLeast(0).toString()
                                                        },
                                                        containerColor = themeColors.primaryContainer,
                                                        contentColor = themeColors.onPrimaryContainer,
                                                        buttonSize = 48.dp
                                                    )
                                                    OutlinedTextField(
                                                        value = freePurifiedStr,
                                                        onValueChange = { input ->
                                                            val digits = input.filter { it.isDigit() }
                                                            if (digits.isEmpty()) {
                                                                freePurifiedStr = ""
                                                            } else {
                                                                freePurifiedStr = (digits.toIntOrNull() ?: 0)
                                                                    .coerceIn(0, purifiedQty)
                                                                    .toString()
                                                            }
                                                        },
                                                        placeholder = {
                                                            Text(
                                                                "0",
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        textStyle = LocalTextStyle.current.copy(
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp
                                                        ),
                                                        modifier = Modifier
                                                            .width(60.dp)
                                                            .onFocusChanged { focusState ->
                                                                if (focusState.isFocused && freePurifiedStr == "0") {
                                                                    freePurifiedStr = ""
                                                                } else if (!focusState.isFocused && freePurifiedStr.isEmpty()) {
                                                                    freePurifiedStr = "0"
                                                                }
                                                            }
                                                    )
                                                    SpringBounceStepperButton(
                                                        icon = Icons.Default.Add,
                                                        contentDescription = "Increase Free Purified",
                                                        onClick = {
                                                            freePurifiedStr = (freePurifiedQty + 1).coerceAtMost(purifiedQty).toString()
                                                        },
                                                        containerColor = themeColors.primaryAccent,
                                                        contentColor = themeColors.onPrimaryAccent,
                                                        buttonSize = 48.dp,
                                                        enabled = freePurifiedQty < purifiedQty
                                                    )
                                                }
                                            }
                                        }

                                        if (alkalineQty > 0) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Free Alkaline (max $alkalineQty)",
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = themeColors.secondaryAccentText,
                                                    fontSize = 14.sp
                                                )
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    SpringBounceStepperButton(
                                                        icon = Icons.Default.Remove,
                                                        contentDescription = "Decrease Free Alkaline",
                                                        onClick = {
                                                            freeAlkalineStr = (freeAlkalineQty - 1).coerceAtLeast(0).toString()
                                                        },
                                                        containerColor = themeColors.secondaryContainer,
                                                        contentColor = themeColors.onSecondaryContainer,
                                                        buttonSize = 48.dp
                                                    )
                                                    OutlinedTextField(
                                                        value = freeAlkalineStr,
                                                        onValueChange = { input ->
                                                            val digits = input.filter { it.isDigit() }
                                                            if (digits.isEmpty()) {
                                                                freeAlkalineStr = ""
                                                            } else {
                                                                freeAlkalineStr = (digits.toIntOrNull() ?: 0)
                                                                    .coerceIn(0, alkalineQty)
                                                                    .toString()
                                                            }
                                                        },
                                                        placeholder = {
                                                            Text(
                                                                "0",
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Center
                                                            )
                                                        },
                                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                                        singleLine = true,
                                                        textStyle = LocalTextStyle.current.copy(
                                                            textAlign = TextAlign.Center,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 16.sp
                                                        ),
                                                        modifier = Modifier
                                                            .width(60.dp)
                                                            .onFocusChanged { focusState ->
                                                                if (focusState.isFocused && freeAlkalineStr == "0") {
                                                                    freeAlkalineStr = ""
                                                                } else if (!focusState.isFocused && freeAlkalineStr.isEmpty()) {
                                                                    freeAlkalineStr = "0"
                                                                }
                                                            }
                                                    )
                                                    SpringBounceStepperButton(
                                                        icon = Icons.Default.Add,
                                                        contentDescription = "Increase Free Alkaline",
                                                        onClick = {
                                                            freeAlkalineStr = (freeAlkalineQty + 1).coerceAtMost(alkalineQty).toString()
                                                        },
                                                        containerColor = themeColors.secondaryAccent,
                                                        contentColor = themeColors.onSecondaryAccent,
                                                        buttonSize = 48.dp,
                                                        enabled = freeAlkalineQty < alkalineQty
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Empty-start Discount input
                                OutlinedTextField(
                                    value = discountStr,
                                    onValueChange = { discountStr = it },
                                    label = { Text("Discount (₱)") },
                                    placeholder = { Text("0.0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp)
                                )

                                // Optional Settle Old Utang card if customer has Utang
                                if (existingUtang > 0.0) {
                                    BigCard(
                                        containerColor = themeColors.utangContainer,
                                        borderColor = themeColors.utang,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Old Utang: ₱%.2f".format(existingUtang),
                                                    fontWeight = FontWeight.Bold,
                                                    color = utangAlertTextColor
                                                )
                                                FilterChip(
                                                    selected = settleUtangTogether,
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        settleUtangTogether = !settleUtangTogether
                                                        if (settleUtangTogether && utangPayStr.isBlank()) {
                                                            utangPayStr = existingUtang.toString()
                                                        }
                                                    },
                                                    label = {
                                                        Text(
                                                            if (settleUtangTogether) "✓ Settling" else "Settle Utang",
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    },
                                                    modifier = Modifier.height(48.dp)
                                                )
                                            }
                                            if (settleUtangTogether) {
                                                OutlinedTextField(
                                                    value = utangPayStr,
                                                    onValueChange = { utangPayStr = it },
                                                    label = { Text("Old Utang Payment (₱)") },
                                                    placeholder = { Text("0.0") },
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                    modifier = Modifier.fillMaxWidth(),
                                                    singleLine = true
                                                )
                                            } else {
                                                Text(
                                                    "Combined w/ Old Utang: ₱%.2f".format(combinedPreviewTotal),
                                                    fontSize = 12.sp,
                                                    color = themeColors.utangSubtext
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right-Aligned Payment Status Chips
                                Text("Payment Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                val statuses = if (orderSource == "Route Delivery") {
                                    listOf("Paid", "Utang", "May Sukli", "Direct to Boss")
                                } else {
                                    listOf("Paid at Station", "Unpaid / Collect on Delivery", "Direct to Boss")
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    statuses.forEach { status ->
                                        FilterChip(
                                            selected = paymentStatus == status,
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                paymentStatus = status
                                            },
                                            label = { Text(status, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.height(48.dp)
                                        )
                                    }
                                }

                                if (paymentStatus == "Paid" || paymentStatus == "May Sukli" || paymentStatus == "Paid at Station") {
                                    OutlinedTextField(
                                        value = cashGivenStr,
                                        onValueChange = { cashGivenStr = it },
                                        label = { Text("Cash Given by Customer (₱)") },
                                        placeholder = { Text("%.2f".format(totalDue)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                }

                                // Live Summary Card
                                BigCard(
                                    containerColor = themeColors.primaryContainer.copy(alpha = 0.42f),
                                    borderColor = themeColors.primaryAccent.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Paid Gallons:", fontSize = 13.sp)
                                            Text(
                                                "${paidPurified + paidAlkaline} gal (Free: ${freePurifiedQty + freeAlkalineQty})",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                            Text("Order Subtotal:", fontSize = 13.sp)
                                            Text("₱%.2f".format(orderSubtotal), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        if (creditAvailable > 0.0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Barya Credit Used:", fontSize = 13.sp, color = themeColors.paid)
                                                Text("- ₱%.2f".format(minOf(creditAvailable, orderSubtotal)), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.paid)
                                            }
                                        }
                                        if (utangPay > 0.0) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Old Utang Settled:", fontSize = 13.sp, color = themeColors.utang)
                                                Text("₱%.2f".format(utangPay), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.utang)
                                            }
                                        }
                                        HorizontalDivider()
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Total Due:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            AnimatedRollingAmount(
                                                text = "₱%.2f".format(totalDue),
                                                style = MaterialTheme.typography.titleLarge,
                                                color = purHeaderColor
                                            )
                                        }
                                        if (isSelectedBankDirect || paymentStatus == "Direct to Boss") {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Remit Cash:", color = alkHeaderColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("₱0.00 (Direct to Boss - May Commission)", color = alkHeaderColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        } else if (paymentStatus == "Utang" || paymentStatus == "Unpaid / Collect on Delivery") {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Rider Commission:", color = themeColors.utang, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("₱0.00 Today (Pending until Bayad Utang)", color = themeColors.utang, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                        if ((paymentStatus == "Paid" || paymentStatus == "May Sukli" || paymentStatus == "Paid at Station") && cashGiven > totalDue) {
                                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Text("Barya / Sukli to Store:", color = themeColors.paid, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                Text("₱%.2f".format(cashGiven - totalDue), color = themeColors.paid, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Right-Anchored Wizard Navigation & Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentStep -= 1
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .height(60.dp)
                                .testTag("wizard_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back", fontWeight = FontWeight.Bold)
                        }
                    } else if (isEditing && onDeleteExisting != null && existingDelivery != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteExisting(existingDelivery.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.utang),
                            border = BorderStroke(1.dp, themeColors.utang),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(60.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Delivery")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete")
                        }
                    } else {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.height(60.dp)
                        ) {
                            Text("Cancel")
                        }
                    }

                    if (isEditing && currentStep == 3 && onDeleteExisting != null && existingDelivery != null) {
                        OutlinedButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDeleteExisting(existingDelivery.id)
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.utang),
                            border = BorderStroke(1.dp, themeColors.utang),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.height(60.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Delivery")
                        }
                    }

                    when (currentStep) {
                        1 -> {
                            SpringBounceButton(
                                onClick = { currentStep = 2 },
                                enabled = customerNameQuery.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .testTag("wizard_step1_next_button")
                            ) {
                                Text(
                                    if (isNewTypedCustomer) "SAVE & NEXT" else "NEXT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                            }
                        }
                        2 -> {
                            SpringBounceButton(
                                onClick = { currentStep = 3 },
                                enabled = (purifiedQty > 0 || alkalineQty > 0),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .testTag("wizard_step2_next_button")
                            ) {
                                Text(
                                    "NEXT",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next")
                            }
                        }
                        else -> {
                            SpringBounceButton(
                                onClick = {
                                    if (isEditing && onUpdateExisting != null && existingDelivery != null) {
                                        onUpdateExisting(
                                            existingDelivery.id,
                                            purifiedQty,
                                            alkalineQty,
                                            freePurifiedQty,
                                            freeAlkalineQty,
                                            currentPurifiedPrice,
                                            currentAlkalinePrice,
                                            discount,
                                            utangPay,
                                            cashGiven,
                                            paymentStatus,
                                            orderSource
                                        )
                                    } else {
                                        onSaveNew(
                                            customerNameQuery.trim(),
                                            customerAddress.ifBlank { "Default Route" },
                                            customerType,
                                            purifiedQty,
                                            alkalineQty,
                                            freePurifiedQty,
                                            freeAlkalineQty,
                                            currentPurifiedPrice,
                                            currentAlkalinePrice,
                                            discount,
                                            utangPay,
                                            cashGiven,
                                            paymentStatus,
                                            orderSource
                                        )
                                    }
                                },
                                enabled = customerNameQuery.isNotBlank() && (purifiedQty > 0 || alkalineQty > 0),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColors.paid,
                                    contentColor = themeColors.onPaid
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                                    .testTag("wizard_save_delivery_button")
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "Save")
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isEditing) "UPDATE DELIVERY" else "SAVE DELIVERY",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatPesoChip(price: Double): String {
    return if (price % 1.0 == 0.0) {
        "₱${price.toInt()}"
    } else {
        "₱%.2f".format(price)
    }
}
