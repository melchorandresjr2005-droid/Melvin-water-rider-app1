package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DeliveryFormDialog
import com.example.ui.components.WaterDropletRiderBadge
import com.example.ui.customers.CustomersScreen
import com.example.ui.expenses.ExpensesScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.today.TodayScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val defaultPriceConfig by viewModel.defaultPriceConfig.collectAsState()
    val showAddDeliveryDialog by viewModel.showAddDeliveryDialog.collectAsState()
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = selectedTab != 0) {
        viewModel.setSelectedTab(0)
    }

    var fabPressed by remember { mutableStateOf(false) }
    val fabScale by animateFloatAsState(
        targetValue = if (fabPressed) 0.93f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "FabSpringScale"
    )

    val themeColors = com.example.ui.theme.WaterRiderTheme.colors

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        WaterDropletRiderBadge(size = 38.dp)
                        Column {
                            Text(
                                text = "Melvin WaterRider",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeColors.topBarTitle
                            )
                            Text(
                                text = "Offline Water Delivery & Remit",
                                style = MaterialTheme.typography.labelMedium,
                                color = themeColors.topBarSubtitle,
                                fontSize = 11.sp
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = themeColors.topBarContainer,
                    titleContentColor = themeColors.topBarTitle
                )
            )
        },
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                selectedIconColor = themeColors.navBarSelectedIcon,
                selectedTextColor = themeColors.navBarSelectedText,
                indicatorColor = themeColors.navBarIndicator,
                unselectedIconColor = themeColors.navBarUnselected,
                unselectedTextColor = themeColors.navBarUnselected
            )
            NavigationBar(
                containerColor = themeColors.navBarContainer,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedTab(0)
                    },
                    icon = { Icon(Icons.Default.Today, contentDescription = "Today") },
                    label = { Text("Today", fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_today")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedTab(1)
                    },
                    icon = { Icon(Icons.Default.Group, contentDescription = "Customers") },
                    label = { Text("Customers", fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_customers")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedTab(2)
                    },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Expenses & Vale") },
                    label = { Text("Expenses", fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_expenses")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.setSelectedTab(3)
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Sahod & Settings") },
                    label = { Text("Sahod", fontWeight = FontWeight.Bold) },
                    colors = navColors,
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.openAddDeliveryWizard()
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .heightIn(min = 60.dp)
                    .scale(fabScale)
                    .testTag("add_delivery_fab")
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                fabPressed = true
                                tryAwaitRelease()
                                fabPressed = false
                            }
                        )
                    }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Delivery")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Delivery", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TodayScreen(viewModel = viewModel)
                1 -> CustomersScreen(viewModel = viewModel)
                2 -> ExpensesScreen(viewModel = viewModel)
                3 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    if (showAddDeliveryDialog) {
        DeliveryFormDialog(
            customers = customers,
            defaultPurifiedPrice = defaultPriceConfig.defaultPurifiedPrice,
            defaultAlkalinePrice = defaultPriceConfig.defaultAlkalinePrice,
            purifiedPreset1 = settings?.purifiedPreset1 ?: 25.0,
            purifiedPreset2 = settings?.purifiedPreset2 ?: defaultPriceConfig.defaultPurifiedPrice,
            alkalinePreset1 = settings?.alkalinePreset1 ?: 30.0,
            alkalinePreset2 = settings?.alkalinePreset2 ?: defaultPriceConfig.defaultAlkalinePrice,
            onDismiss = { viewModel.closeAddDeliveryWizard() },
            onSaveNew = { customerName, address, customerType, purifiedQty, alkalineQty, freePurifiedQty, freeAlkalineQty, purifiedPrice, alkalinePrice, customDiscount, utangPaidAmount, cashGiven, paymentStatus, orderSource ->
                viewModel.recordDeliveryByName(
                    customerName = customerName,
                    address = address,
                    customerType = customerType,
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
                viewModel.closeAddDeliveryWizard()
            }
        )
    }
}
