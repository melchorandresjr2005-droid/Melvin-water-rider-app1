package com.example.ui.customers

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CustomerEntity
import com.example.data.DeliveryEntity
import com.example.data.PaymentEntity
import com.example.ui.components.AddCustomerDialog
import com.example.ui.components.BayadUtangDialog
import com.example.ui.components.BigCard
import com.example.ui.components.DeliveryFormDialog
import com.example.ui.theme.WaterRiderTheme
import com.example.util.MessengerUtils
import com.example.viewmodel.CustomerFilter
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CustomersScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val themeColors = WaterRiderTheme.colors
    val customers by viewModel.customers.collectAsState()
    val allDeliveries by viewModel.deliveries.collectAsState()
    val allPayments by viewModel.payments.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val defaultPriceConfig by viewModel.defaultPriceConfig.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val activeFilter by viewModel.customerFilter.collectAsState()

    var showAddCustomerDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerToDeliver by remember { mutableStateOf<CustomerEntity?>(null) }
    var customerForBayadUtang by remember { mutableStateOf<CustomerEntity?>(null) }
    var deliveryToEdit by remember { mutableStateOf<DeliveryEntity?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember(dateFormat) { dateFormat.format(Date()) }

    val totalRemainingUtang = remember(customers) {
        computeTotalRegularUtang(customers)
    }

    val deliveriesByCustomer = remember(allDeliveries) {
        allDeliveries.groupBy { it.customerId }.mapValues { it.value.take(5) }
    }
    val paymentsByCustomer = remember(allPayments) {
        allPayments.groupBy { it.customerId }.mapValues { it.value.take(5) }
    }

    val filteredCustomers = remember(customers, searchQuery, activeFilter, todayStr) {
        filterAndSortCustomers(
            customers = customers,
            searchQuery = searchQuery,
            activeFilter = activeFilter,
            todayStr = todayStr
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Customer Directory", style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { showAddCustomerDialog = true },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Customer")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Customer", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Instant QWERTY Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Search by customer name or route...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                keyboardType = KeyboardType.Text
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Right-Thumb Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(key = "filter_all") {
                FilterChip(
                    selected = activeFilter == CustomerFilter.ALL,
                    onClick = { viewModel.setCustomerFilter(CustomerFilter.ALL) },
                    label = { Text("All (${customers.size})") },
                    modifier = Modifier.height(48.dp)
                )
            }
            item(key = "filter_with_utang") {
                val utangChipLabel = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
                FilterChip(
                    selected = activeFilter == CustomerFilter.WITH_UTANG,
                    onClick = { viewModel.setCustomerFilter(CustomerFilter.WITH_UTANG) },
                    label = {
                        Text(
                            text = String.format(Locale.US, "May Utang (₱%,.2f)", totalRemainingUtang),
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = themeColors.utangContainer,
                        selectedLabelColor = utangChipLabel
                    ),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("filter_chip_may_utang")
                )
            }
            item(key = "filter_with_sukli") {
                FilterChip(
                    selected = activeFilter == CustomerFilter.WITH_SUKLI,
                    onClick = { viewModel.setCustomerFilter(CustomerFilter.WITH_SUKLI) },
                    label = { Text("May Sukli") },
                    modifier = Modifier.height(48.dp)
                )
            }
            item(key = "filter_not_delivered") {
                FilterChip(
                    selected = activeFilter == CustomerFilter.NOT_DELIVERED_TODAY,
                    onClick = { viewModel.setCustomerFilter(CustomerFilter.NOT_DELIVERED_TODAY) },
                    label = { Text("Not Delivered Today") },
                    modifier = Modifier.height(48.dp)
                )
            }
        }

        if (activeFilter == CustomerFilter.WITH_UTANG) {
            val bannerTextColor = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = themeColors.utangContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Singilan (${filteredCustomers.size} customers • Highest first)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = bannerTextColor
                    )
                    Text(
                        String.format(Locale.US, "₱%,.2f", totalRemainingUtang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = bannerTextColor
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (filteredCustomers.isEmpty()) {
                item(key = "empty_customers", contentType = "empty_state") {
                    BigCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.PersonOutline,
                                contentDescription = null,
                                tint = themeColors.primaryAccent.copy(alpha = 0.65f),
                                modifier = Modifier.size(44.dp)
                            )
                            Text("No matching customers", fontWeight = FontWeight.Bold)
                            Text(
                                "Tap 'Add Customer' above or log a delivery directly via + Add Delivery.",
                                fontSize = 13.sp,
                                color = themeColors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(
                    items = filteredCustomers,
                    key = { it.id },
                    contentType = { "customer_card" }
                ) { customer ->
                    val customerDeliveries = deliveriesByCustomer[customer.id].orEmpty()
                    val customerPayments = paymentsByCustomer[customer.id].orEmpty()
                    CustomerCard(
                        customer = customer,
                        recentDeliveries = customerDeliveries,
                        recentPayments = customerPayments,
                        onDeliverClick = { customerToDeliver = customer },
                        onBayadUtangClick = { customerForBayadUtang = customer },
                        onMessengerReminderClick = {
                            MessengerUtils.openCustomerUtangMessenger(
                                context = context,
                                customerName = customer.name,
                                remainingBalance = customer.currentBalance
                            )
                        },
                        onEditCustomerClick = { customerToEdit = customer },
                        onDeliveryClick = { deliveryToEdit = it }
                    )
                }
            }
        }
    }

    if (showAddCustomerDialog || customerToEdit != null) {
        AddCustomerDialog(
            existingCustomer = customerToEdit,
            defaultPurifiedPrice = defaultPriceConfig.defaultPurifiedPrice,
            defaultAlkalinePrice = defaultPriceConfig.defaultAlkalinePrice,
            onDismiss = {
                showAddCustomerDialog = false
                customerToEdit = null
            },
            onSave = { name, address, type, purifiedPrice, alkalinePrice, isBankDirect ->
                if (customerToEdit != null) {
                    viewModel.updateCustomer(
                        customerToEdit!!.copy(
                            name = name,
                            address = address,
                            customerType = type,
                            isBankDirectAccount = isBankDirect,
                            defaultPurifiedPrice = purifiedPrice,
                            defaultAlkalinePrice = alkalinePrice
                        )
                    )
                } else {
                    viewModel.addCustomer(
                        name = name,
                        address = address,
                        type = type,
                        purifiedPrice = purifiedPrice,
                        alkalinePrice = alkalinePrice,
                        isBankDirectAccount = isBankDirect
                    )
                }
                showAddCustomerDialog = false
                customerToEdit = null
            }
        )
    }

    if (customerToDeliver != null) {
        DeliveryFormDialog(
            customers = customers,
            initialCustomer = customerToDeliver,
            defaultPurifiedPrice = defaultPriceConfig.defaultPurifiedPrice,
            defaultAlkalinePrice = defaultPriceConfig.defaultAlkalinePrice,
            purifiedPreset1 = settings?.purifiedPreset1 ?: 25.0,
            purifiedPreset2 = settings?.purifiedPreset2 ?: defaultPriceConfig.defaultPurifiedPrice,
            alkalinePreset1 = settings?.alkalinePreset1 ?: 30.0,
            alkalinePreset2 = settings?.alkalinePreset2 ?: defaultPriceConfig.defaultAlkalinePrice,
            onDismiss = { customerToDeliver = null },
            onSaveNew = { _, _, _, purifiedQty, alkalineQty, freePurifiedQty, freeAlkalineQty, purifiedPrice, alkalinePrice, customDiscount, utangPaidAmount, cashGiven, paymentStatus, orderSource ->
                viewModel.recordDelivery(
                    customerId = customerToDeliver!!.id,
                    purifiedQty = purifiedQty,
                    alkalineQty = alkalineQty,
                    freePurifiedQty = freePurifiedQty,
                    freeAlkalineQty = freeAlkalineQty,
                    purifiedPrice = purifiedPrice,
                    alkalinePrice = alkalinePrice,
                    customDiscount = customDiscount,
                    utangPaidAmount = utangPaidAmount,
                    cashGiven = cashGiven,
                    paymentStatus = paymentStatus,
                    orderSource = orderSource
                )
                customerToDeliver = null
            }
        )
    }

    if (deliveryToEdit != null) {
        DeliveryFormDialog(
            customers = customers,
            existingDelivery = deliveryToEdit,
            defaultPurifiedPrice = defaultPriceConfig.defaultPurifiedPrice,
            defaultAlkalinePrice = defaultPriceConfig.defaultAlkalinePrice,
            purifiedPreset1 = settings?.purifiedPreset1 ?: 25.0,
            purifiedPreset2 = settings?.purifiedPreset2 ?: defaultPriceConfig.defaultPurifiedPrice,
            alkalinePreset1 = settings?.alkalinePreset1 ?: 30.0,
            alkalinePreset2 = settings?.alkalinePreset2 ?: defaultPriceConfig.defaultAlkalinePrice,
            onDismiss = { deliveryToEdit = null },
            onSaveNew = { _, _, _, _, _, _, _, _, _, _, _, _, _, _ -> },
            onUpdateExisting = { deliveryId, purifiedQty, alkalineQty, freePurifiedQty, freeAlkalineQty, purifiedPrice, alkalinePrice, customDiscount, utangPaidAmount, cashGiven, paymentStatus, orderSource ->
                viewModel.updateDelivery(
                    deliveryId = deliveryId,
                    purifiedQty = purifiedQty,
                    alkalineQty = alkalineQty,
                    freePurifiedQty = freePurifiedQty,
                    freeAlkalineQty = freeAlkalineQty,
                    purifiedPrice = purifiedPrice,
                    alkalinePrice = alkalinePrice,
                    customDiscount = customDiscount,
                    utangPaidAmount = utangPaidAmount,
                    cashGiven = cashGiven,
                    paymentStatus = paymentStatus,
                    orderSource = orderSource
                )
                deliveryToEdit = null
            },
            onDeleteExisting = { deliveryId ->
                viewModel.deleteDelivery(deliveryId)
                deliveryToEdit = null
            }
        )
    }

    if (customerForBayadUtang != null) {
        BayadUtangDialog(
            customer = customerForBayadUtang!!,
            onDismiss = { customerForBayadUtang = null },
            onSavePayment = { amount, notes ->
                viewModel.recordUtangPayment(customerForBayadUtang!!.id, amount, notes)
            }
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    recentDeliveries: List<DeliveryEntity>,
    recentPayments: List<PaymentEntity>,
    onDeliverClick: () -> Unit,
    onBayadUtangClick: () -> Unit,
    onMessengerReminderClick: () -> Unit,
    onEditCustomerClick: () -> Unit,
    onDeliveryClick: (DeliveryEntity) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    var expanded by remember { mutableStateOf(false) }

    val typeBadgeText = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
    val utangBadgeText = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang

    BigCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(customer.name, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = themeColors.textPrimary)
                        Surface(
                            color = themeColors.primaryContainer,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                customer.customerType,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = typeBadgeText
                            )
                        }
                    }
                    Text(
                        "${customer.address} • P ₱%.0f / A ₱%.0f".format(
                            customer.defaultPurifiedPrice,
                            customer.defaultAlkalinePrice
                        ),
                        fontSize = 13.sp,
                        color = themeColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (customer.currentBalance > 0.0) {
                            Surface(color = themeColors.utangContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    String.format(Locale.US, "Natitirang Utang: ₱%,.2f", customer.currentBalance),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = utangBadgeText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (customer.storedCredit > 0.0) {
                            Surface(color = themeColors.paidContainer, shape = RoundedCornerShape(6.dp)) {
                                Text(
                                    String.format(Locale.US, "Sukli: ₱%,.2f", customer.storedCredit),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    color = themeColors.paid,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDeliverClick()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryAccent,
                            contentColor = themeColors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.LocalShipping, contentDescription = "Deliver", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deliver", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    if (customer.currentBalance > 0.0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FilledTonalIconButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onMessengerReminderClick()
                                },
                                colors = IconButtonDefaults.filledTonalIconButtonColors(
                                    containerColor = themeColors.primaryContainer,
                                    contentColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
                                ),
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("customer_messenger_utang_${customer.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Chat,
                                    contentDescription = "Messenger Utang Reminder",
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onBayadUtangClick()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColors.utang,
                                    contentColor = themeColors.onUtang
                                ),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(48.dp)
                                    .testTag("bayad_utang_button_${customer.id}")
                            ) {
                                Icon(Icons.Default.Payments, contentDescription = "Bayad Utang", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Bayad Utang", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            if (expanded) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Purified: ₱%.0f/gal  •  Alkaline: ₱%.0f/gal".format(customer.defaultPurifiedPrice, customer.defaultAlkalinePrice),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.secondaryAccentText
                    )
                    TextButton(onClick = onEditCustomerClick, modifier = Modifier.height(48.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Customer", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Profile")
                    }
                }
                if (recentPayments.isNotEmpty()) {
                    Text(
                        "Bayad Utang History:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val payDateFormat = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }
                    recentPayments.forEach { pay ->
                        val dateLabel = payDateFormat.format(Date(pay.timestamp))
                        val balText = if (pay.sukliAdded > 0.0) {
                            String.format(Locale.US, "Bal: ₱0.00 (+Sukli ₱%.2f)", pay.sukliAdded)
                        } else {
                            String.format(Locale.US, "Bal: ₱%.2f", pay.remainingBalance)
                        }
                        Surface(
                            color = themeColors.paidContainer.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = String.format(Locale.US, "%s: Nagbayad ₱%.2f", dateLabel, pay.amount),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = themeColors.paid
                                )
                                Text(
                                    text = balText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pay.remainingBalance > 0.0) utangBadgeText else themeColors.paid
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
                if (recentDeliveries.isNotEmpty()) {
                    Text("Recent Deliveries (tap to edit/delete):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = themeColors.textSecondary)
                    Spacer(modifier = Modifier.height(4.dp))
                    recentDeliveries.forEach { del ->
                        Surface(
                            color = themeColors.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clickable { onDeliveryClick(del) }
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${del.dateString} • P:${del.purifiedQty} A:${del.alkalineQty}", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                Text("₱%.2f (${del.paymentStatus})".format(del.totalDue), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

internal fun computeTotalRegularUtang(customers: List<CustomerEntity>): Double {
    return customers
        .filter { !it.isBankDirectAccount && it.customerType != "Bank / Direct" && it.currentBalance > 0.0 }
        .sumOf { it.currentBalance }
}

internal fun filterAndSortCustomers(
    customers: List<CustomerEntity>,
    searchQuery: String,
    activeFilter: CustomerFilter,
    todayStr: String
): List<CustomerEntity> {
    val base = customers.filter { customer ->
        val matchesSearch = customer.name.contains(searchQuery, ignoreCase = true) ||
            customer.address.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (activeFilter) {
            CustomerFilter.ALL -> true
            CustomerFilter.WITH_UTANG ->
                !customer.isBankDirectAccount &&
                    customer.customerType != "Bank / Direct" &&
                    customer.currentBalance > 0.0
            CustomerFilter.WITH_SUKLI -> customer.storedCredit > 0.0
            CustomerFilter.NOT_DELIVERED_TODAY -> customer.lastDeliveredDate != todayStr
        }

        matchesSearch && matchesFilter
    }
    return if (activeFilter == CustomerFilter.WITH_UTANG) {
        base.sortedWith(
            compareByDescending<CustomerEntity> { it.currentBalance }
                .thenBy { it.lastDeliveredDate ?: "" }
                .thenBy { it.id }
        )
    } else {
        base
    }
}
