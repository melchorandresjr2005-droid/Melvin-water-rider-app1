package com.example.ui.today

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdvanceRemitEntity
import com.example.data.DeliveryEntity
import com.example.data.ExpenseEntity
import com.example.data.PaymentEntity
import com.example.data.RemittanceLogEntity
import com.example.ui.components.AdvanceRemitDialog
import com.example.ui.components.AnimatedRollingAmount
import com.example.ui.components.BigCard
import com.example.ui.components.DeliveryFormDialog
import com.example.ui.components.WaterWaveBackground
import com.example.ui.theme.WaterRiderTheme
import com.example.util.MessengerUtils
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun TodayScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors

    val customers by viewModel.customers.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val unremittedDeliveries by viewModel.unremittedDeliveries.collectAsState()
    val unremittedExpenses by viewModel.unremittedExpenses.collectAsState()
    val allExpenses by viewModel.expenses.collectAsState()
    val allPayments by viewModel.payments.collectAsState()
    val unremittedPayments by viewModel.unremittedPayments.collectAsState()
    val allAdvanceRemits by viewModel.advanceRemits.collectAsState()
    val unremittedAdvanceRemits by viewModel.unremittedAdvanceRemits.collectAsState()
    val remittances by viewModel.remittances.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val defaultPriceConfig by viewModel.defaultPriceConfig.collectAsState()
    val bossContactConfig by viewModel.bossContactConfig.collectAsState()
    val backupReminderDismissed by viewModel.backupReminderDismissed.collectAsState()

    val scope = rememberCoroutineScope()
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val headerDateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val historyDateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val todayStr = remember(dateFormat) { dateFormat.format(Date()) }
    val headerDateText = remember(headerDateFormat) { headerDateFormat.format(Date()) }

    val todayDeliveries by remember(todayStr) {
        derivedStateOf { deliveries.filter { it.dateString == todayStr } }
    }
    val todayPayments by remember(todayStr) {
        derivedStateOf { allPayments.filter { it.dateString == todayStr } }
    }

    val totalPurifiedToday = remember(todayDeliveries) { todayDeliveries.sumOf { it.purifiedQty } }
    val totalAlkalineToday = remember(todayDeliveries) { todayDeliveries.sumOf { it.alkalineQty } }
    val earnedGallonsToday = remember(todayDeliveries, todayPayments) {
        todayDeliveries.sumOf { it.commissionGallons + it.utangCommissionGallons } +
            todayPayments.sumOf { it.commissionGallonsEarned }
    }
    val pendingGallonsToday = remember(todayDeliveries) {
        todayDeliveries.sumOf { it.pendingCommissionAdded }
    }
    val commissionRate = settings?.commissionPerGallon ?: 5.0
    val dailyCommission = remember(earnedGallonsToday, commissionRate) {
        earnedGallonsToday * commissionRate
    }

    val totalCashCollected = remember(unremittedDeliveries, unremittedPayments) {
        unremittedDeliveries.sumOf { it.amountPaid } + unremittedPayments.sumOf { it.amount }
    }
    val totalExpenses = remember(unremittedExpenses) { unremittedExpenses.sumOf { it.amount } }
    val totalAdvanceRemits = remember(unremittedAdvanceRemits) { unremittedAdvanceRemits.sumOf { it.amount } }
    val netRemit = remember(totalCashCollected, totalExpenses, totalAdvanceRemits) {
        maxOf(0.0, totalCashCollected - totalExpenses - totalAdvanceRemits)
    }

    // Saturday / 7+ days backup check
    val showBackupReminder = remember(backupReminderDismissed, settings?.lastBackupTimestamp) {
        val calendar = Calendar.getInstance()
        val isSaturday = calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val lastBackup = settings?.lastBackupTimestamp ?: 0L
        val daysSinceBackup = (System.currentTimeMillis() - lastBackup) / (1000 * 60 * 60 * 24)
        !backupReminderDismissed && (isSaturday || daysSinceBackup >= 7)
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDatabase(context, uri) { success ->
                Toast.makeText(
                    context,
                    if (success) "Backup saved to Google Drive / Storage!" else "Backup failed.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    var showRemitDialog by remember { mutableStateOf(false) }
    var showAdvanceRemitDialog by remember { mutableStateOf(false) }
    var selectedAdvanceRemitForEdit by remember { mutableStateOf<AdvanceRemitEntity?>(null) }
    var remitNotes by remember { mutableStateOf("") }
    var latestRemitLog by remember { mutableStateOf<RemittanceLogEntity?>(null) }
    var showHistory by remember { mutableStateOf(false) }
    var selectedDeliveryForEdit by remember { mutableStateOf<DeliveryEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Dismissible Saturday Backup Reminder
        if (showBackupReminder) {
            item(key = "backup_reminder", contentType = "banner") {
                BigCard(
                    containerColor = themeColors.utangContainer,
                    borderColor = themeColors.utang,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⚠️ Backup Reminder",
                                fontWeight = FontWeight.ExtraBold,
                                color = themeColors.utangSubtext,
                                fontSize = 15.sp
                            )
                            TextButton(onClick = { viewModel.dismissBackupReminder() }) {
                                Text("Dismiss", color = themeColors.utangSubtext, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text(
                            "Protect your customer utang balances, deliveries, and payroll by backing up to Google Drive.",
                            fontSize = 13.sp,
                            color = themeColors.utangSubtext
                        )
                        Button(
                            onClick = {
                                exportLauncher.launch("MelvinWaterRider_Backup_$todayStr.json")
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primaryAccent,
                                contentColor = themeColors.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Backup")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Backup to Google Drive", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Animated Water Wave Header Banner
        item(key = "header_banner", contentType = "header") {
            BigCard(
                containerColor = themeColors.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    WaterWaveBackground(modifier = Modifier.matchParentSize())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "Today's Route & Shift",
                                style = MaterialTheme.typography.headlineSmall,
                                color = themeColors.textPrimary
                            )
                            Text(
                                headerDateText,
                                fontSize = 14.sp,
                                color = themeColors.textSecondary
                            )
                        }
                        FilledTonalIconButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                MessengerUtils.openBossMessengerChat(
                                    context = context,
                                    messengerUsername = bossContactConfig.messengerUsername,
                                    phoneNumber = bossContactConfig.phoneNumber
                                )
                            },
                            colors = IconButtonDefaults.filledTonalIconButtonColors(
                                containerColor = themeColors.primaryContainer,
                                contentColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
                            ),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("dashboard_chat_boss_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = "Chat kay Boss (Messenger)"
                            )
                        }
                    }
                }
            }
        }

        // 2x2 Grid Summary Tiles with Oversized Bold Numbers (24sp-28sp)
        item(key = "summary_tiles", contentType = "summary") {
            val purTileColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
            val alkTileColor = if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BigCard(
                        containerColor = themeColors.primaryContainer.copy(alpha = 0.72f),
                        borderColor = themeColors.primaryAccent.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Purified Gallons", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = purTileColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$totalPurifiedToday",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = purTileColor
                            )
                        }
                    }
                    BigCard(
                        containerColor = themeColors.secondaryContainer.copy(alpha = 0.72f),
                        borderColor = themeColors.secondaryAccent.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Alkaline Gallons", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = alkTileColor)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "$totalAlkalineToday",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = alkTileColor
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BigCard(
                        containerColor = themeColors.paidContainer.copy(alpha = 0.72f),
                        borderColor = themeColors.paid.copy(alpha = 0.55f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Remit Cash", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = themeColors.paid)
                            Spacer(modifier = Modifier.height(4.dp))
                            AnimatedRollingAmount(
                                text = "₱%.0f".format(netRemit),
                                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp),
                                color = themeColors.paid
                            )
                        }
                    }
                    BigCard(
                        containerColor = themeColors.surface,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Daily Commission", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = themeColors.textSecondary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "₱%.0f".format(dailyCommission),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = themeColors.primaryAccent
                            )
                            val earnedLabel = if (earnedGallonsToday % 1.0 == 0.0) "${earnedGallonsToday.toInt()}" else String.format(Locale.US, "%.1f", earnedGallonsToday)
                            val pendingPart = if (pendingGallonsToday > 0.0) {
                                val pLabel = if (pendingGallonsToday % 1.0 == 0.0) "${pendingGallonsToday.toInt()}" else String.format(Locale.US, "%.1f", pendingGallonsToday)
                                " • $pLabel pending"
                            } else ""
                            Text("$earnedLabel gal earned$pendingPart", fontSize = 11.sp, color = themeColors.textSecondary)
                        }
                    }
                }
            }
        }

        // Afternoon Remittance Card ("Panghapong Remit")
        item(key = "panghapong_remit_card", contentType = "remit_card") {
            BigCard(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column {
                            Text("Panghapong Remit", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text("Afternoon Remittance & Advance Remits", fontSize = 12.sp, color = themeColors.textSecondary)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showAdvanceRemitDialog = true
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("advance_remit_button")
                            ) {
                                Text("Advance Remit", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showRemitDialog = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = themeColors.paid,
                                    contentColor = themeColors.onPaid
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("remit_now_button")
                            ) {
                                Text("Remit Now", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }

                    HorizontalDivider()

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Unremitted Cash Collected:")
                        Text("₱%.2f".format(totalCashCollected), fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Unremitted Route Expenses:")
                        Text("- ₱%.2f".format(totalExpenses), color = themeColors.expense, fontWeight = FontWeight.Bold)
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Advance Remits (${unremittedAdvanceRemits.size}):")
                        Text("- ₱%.2f".format(totalAdvanceRemits), color = themeColors.primaryAccent, fontWeight = FontWeight.Bold)
                    }
                    if (unremittedAdvanceRemits.isNotEmpty()) {
                        val advTimeFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
                        val advTextColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                "Active Advance Remits (tap to edit/delete):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = themeColors.textSecondary
                            )
                            unremittedAdvanceRemits.forEach { adv ->
                                val timeStr = advTimeFormat.format(Date(adv.timestamp))
                                val label = if (adv.notes.isNotBlank()) "$timeStr • ${adv.notes}" else timeStr
                                Surface(
                                    color = themeColors.primaryContainer.copy(alpha = 0.65f),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAdvanceRemitForEdit = adv }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = advTextColor)
                                        Text("- ₱%.2f".format(adv.amount), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = advTextColor)
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Natitirang Cash to Remit:", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                        AnimatedRollingAmount(
                            text = "₱%.2f".format(netRemit),
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                            color = themeColors.paid
                        )
                    }

                    // Latest Remit Confirmation + Copy / Share
                    AnimatedVisibility(visible = latestRemitLog != null) {
                        latestRemitLog?.let { log ->
                            val reportText by produceState(
                                initialValue = "",
                                key1 = log,
                                key2 = deliveries to allExpenses,
                                key3 = allPayments to allAdvanceRemits
                            ) {
                                value = withContext(Dispatchers.IO) {
                                    buildRemitReport(log, deliveries, allExpenses, allPayments, allAdvanceRemits)
                                }
                            }
                            BigCard(
                                containerColor = themeColors.paidContainer,
                                borderColor = themeColors.paid,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "✓ Shift Closed: Remitted ₱%.2f".format(log.netRemit),
                                        color = themeColors.paid,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                scope.launch {
                                                    val text = reportText.ifEmpty {
                                                        withContext(Dispatchers.IO) {
                                                            buildRemitReport(log, deliveries, allExpenses, allPayments, allAdvanceRemits)
                                                        }
                                                    }
                                                    copyToClipboard(context, text, showToast = false)
                                                    viewModel.triggerAutoBackup(context) { res ->
                                                        val msg = if (res.driveSaved) {
                                                            "Remit copied & Auto-saved to Google Drive backup!"
                                                        } else {
                                                            "Report copied & saved to local backup!"
                                                        }
                                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Report", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                scope.launch {
                                                    val text = reportText.ifEmpty {
                                                        withContext(Dispatchers.IO) {
                                                            buildRemitReport(log, deliveries, allExpenses, allPayments, allAdvanceRemits)
                                                        }
                                                    }
                                                    viewModel.triggerAutoBackup(context)
                                                    shareReport(context, text)
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = themeColors.primaryAccent,
                                                contentColor = themeColors.onPrimaryAccent
                                            ),
                                            modifier = Modifier.weight(1f).height(48.dp)
                                        ) {
                                            Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Share", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Expandable Remittance History Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showHistory = !showHistory }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Remittance History Log (${remittances.size})",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = themeColors.primaryAccent
                        )
                        Icon(
                            if (showHistory) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = "Toggle History",
                            tint = themeColors.primaryAccent
                        )
                    }

                    if (showHistory) {
                        if (remittances.isEmpty()) {
                            Text("No past remittances recorded yet.", fontSize = 13.sp, color = themeColors.textSecondary)
                        } else {
                            val recentRemits = remember(remittances) { remittances.take(10) }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recentRemits.forEach { log ->
                                    key(log.id) {
                                        Surface(
                                            color = themeColors.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        historyDateFormat.format(Date(log.timestamp)),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp
                                                    )
                                                    Text("₱%.2f".format(log.netRemit), fontWeight = FontWeight.ExtraBold, color = themeColors.paid)
                                                }
                                                Text(
                                                    "Purified: ${log.purifiedGallons} • Alkaline: ${log.alkalineGallons} • Cash: ₱%.0f • Exp: ₱%.0f".format(log.totalCashCollected, log.totalExpenses),
                                                    fontSize = 12.sp,
                                                    color = themeColors.textSecondary
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(onClick = {
                                                        scope.launch {
                                                            val report = withContext(Dispatchers.IO) {
                                                                buildRemitReport(log, deliveries, allExpenses, allPayments, allAdvanceRemits)
                                                            }
                                                            copyToClipboard(context, report, showToast = false)
                                                            viewModel.triggerAutoBackup(context) { res ->
                                                                val msg = if (res.driveSaved) {
                                                                    "Remit copied & Auto-saved to Google Drive backup!"
                                                                } else {
                                                                    "Report copied & saved to local backup!"
                                                                }
                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Copy", fontSize = 12.sp)
                                                    }
                                                    TextButton(onClick = {
                                                        scope.launch {
                                                            val report = withContext(Dispatchers.IO) {
                                                                buildRemitReport(log, deliveries, allExpenses, allPayments, allAdvanceRemits)
                                                            }
                                                            viewModel.triggerAutoBackup(context)
                                                            shareReport(context, report)
                                                        }
                                                    }) {
                                                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Share", fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Today's Deliveries Header
        item(key = "today_deliveries_header", contentType = "section_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Deliveries Logged Today (${todayDeliveries.size})", fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                Text("Tap to edit/delete", fontSize = 12.sp, color = themeColors.textSecondary)
            }
        }

        if (todayDeliveries.isEmpty()) {
            item(key = "empty_deliveries", contentType = "empty_state") {
                BigCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.LocalDrink,
                            contentDescription = null,
                            tint = themeColors.primaryAccent.copy(alpha = 0.65f),
                            modifier = Modifier.size(44.dp)
                        )
                        Text("No deliveries logged yet today", fontWeight = FontWeight.Bold)
                        Text(
                            "Tap + Add Delivery at the bottom-right to log your first stop!",
                            fontSize = 13.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }
        } else {
            items(
                items = todayDeliveries,
                key = { it.id },
                contentType = { "delivery_card" }
            ) { delivery ->
                DeliveryCard(
                    delivery = delivery,
                    onClick = { selectedDeliveryForEdit = delivery }
                )
            }
        }
    }

    if (showRemitDialog) {
        AlertDialog(
            onDismissRequest = { showRemitDialog = false },
            title = { Text("Close Afternoon Shift & Remit", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Net Cash to Remit: ₱%.2f".format(netRemit), fontWeight = FontWeight.ExtraBold, color = themeColors.paid, fontSize = 18.sp)
                    OutlinedTextField(
                        value = remitNotes,
                        onValueChange = { remitNotes = it },
                        label = { Text("Remittance Notes / Boss Receipt") },
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            keyboardType = KeyboardType.Text
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.closeAfternoonShift(remitNotes.trim()) { log ->
                            if (log != null) {
                                latestRemitLog = log
                                viewModel.triggerAutoBackup(context) { res ->
                                    if (res.driveSaved) {
                                        Toast.makeText(
                                            context,
                                            "Shift closed & Auto-saved to Google Drive backup!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "No unremitted cash or expenses to close.", Toast.LENGTH_SHORT).show()
                            }
                            showRemitDialog = false
                            remitNotes = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.paid,
                        contentColor = themeColors.onPaid
                    ),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text("Confirm Remittance", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemitDialog = false }, modifier = Modifier.height(48.dp)) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAdvanceRemitDialog || selectedAdvanceRemitForEdit != null) {
        AdvanceRemitDialog(
            existingAdvanceRemit = selectedAdvanceRemitForEdit,
            onDismiss = {
                showAdvanceRemitDialog = false
                selectedAdvanceRemitForEdit = null
            },
            onSave = { amount, notes ->
                val editing = selectedAdvanceRemitForEdit
                if (editing != null) {
                    viewModel.updateAdvanceRemit(editing.copy(amount = amount, notes = notes))
                } else {
                    viewModel.addAdvanceRemit(amount, notes)
                }
                showAdvanceRemitDialog = false
                selectedAdvanceRemitForEdit = null
            },
            onDelete = { adv ->
                viewModel.deleteAdvanceRemit(adv)
                showAdvanceRemitDialog = false
                selectedAdvanceRemitForEdit = null
            }
        )
    }

    if (selectedDeliveryForEdit != null) {
        DeliveryFormDialog(
            customers = customers,
            existingDelivery = selectedDeliveryForEdit,
            defaultPurifiedPrice = defaultPriceConfig.defaultPurifiedPrice,
            defaultAlkalinePrice = defaultPriceConfig.defaultAlkalinePrice,
            purifiedPreset1 = settings?.purifiedPreset1 ?: 25.0,
            purifiedPreset2 = settings?.purifiedPreset2 ?: defaultPriceConfig.defaultPurifiedPrice,
            alkalinePreset1 = settings?.alkalinePreset1 ?: 30.0,
            alkalinePreset2 = settings?.alkalinePreset2 ?: defaultPriceConfig.defaultAlkalinePrice,
            onDismiss = { selectedDeliveryForEdit = null },
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
                selectedDeliveryForEdit = null
            },
            onDeleteExisting = { deliveryId ->
                viewModel.deleteDelivery(deliveryId)
                selectedDeliveryForEdit = null
            }
        )
    }
}

internal fun buildRemitReport(
    log: RemittanceLogEntity,
    allDeliveries: List<DeliveryEntity> = emptyList(),
    allExpenses: List<ExpenseEntity> = emptyList(),
    allPayments: List<PaymentEntity> = emptyList(),
    allAdvanceRemits: List<AdvanceRemitEntity> = emptyList()
): String {
    val reportDate = Date(log.timestamp)
    val tagalogDay = getTagalogDayName(reportDate)
    val formattedDate = SimpleDateFormat("MMM d, yyyy", Locale.US).format(reportDate)
    val dateHeader = "$tagalogDay — $formattedDate" // e.g. Huwebes — Nov 19, 2026

    val dateLabel = SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(reportDate)
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val matchedDeliveries = if (log.id != 0L) allDeliveries.filter { it.remittanceId == log.id } else emptyList()
    val matchedPayments = if (log.id != 0L) allPayments.filter { it.remittanceId == log.id } else emptyList()
    val matchedAdvances = if (log.id != 0L) {
        allAdvanceRemits.filter { it.remittanceId == log.id }.sortedBy { it.timestamp }
    } else emptyList()

    val cashPaidDeliveries = matchedDeliveries.filter {
        !it.isBankDirect &&
            it.paymentStatus != "Direct to Boss" &&
            it.paymentStatus != "Utang" &&
            it.paymentStatus != "Unpaid / Collect on Delivery"
    }

    val useStoredCashCounts = log.cashPaidPurifiedGallons > 0 ||
        log.cashPaidAlkalineGallons > 0 ||
        log.cashDeliverySales > 0.0 ||
        log.bankDirectGallons > 0 ||
        log.totalCommissionGallons > 0.0 ||
        matchedDeliveries.isEmpty()

    val cashPaidPurified = if (useStoredCashCounts) {
        log.cashPaidPurifiedGallons
    } else {
        cashPaidDeliveries.sumOf { del ->
            val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
            if (del.paymentStatus == "Partial") {
                val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
                val totalP = maxOf(1, paidP + paidA)
                kotlin.math.round(paidP * (del.commissionGallons / totalP.toDouble())).toInt()
            } else paidP
        }
    }
    val cashPaidAlkaline = if (useStoredCashCounts) {
        log.cashPaidAlkalineGallons
    } else {
        cashPaidDeliveries.sumOf { del ->
            val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
            if (del.paymentStatus == "Partial") {
                val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
                val totalP = maxOf(1, paidP + paidA)
                kotlin.math.round(paidA * (del.commissionGallons / totalP.toDouble())).toInt()
            } else paidA
        }
    }
    val totalCashPaidGallons = cashPaidPurified + cashPaidAlkaline

    val totalUtangCollected = if (log.totalUtangCollected > 0.0) {
        log.totalUtangCollected
    } else {
        matchedPayments.sumOf { it.amount }
    }
    val cashDeliverySales = when {
        log.cashDeliverySales > 0.0 -> log.cashDeliverySales
        matchedDeliveries.isNotEmpty() -> matchedDeliveries.sumOf { it.amountPaid }
        else -> maxOf(0.0, log.totalCashCollected - totalUtangCollected)
    }

    val bankDirectDeliveries = matchedDeliveries.filter { it.isBankDirect || it.paymentStatus == "Direct to Boss" }
    val bankDirectLines = when {
        log.bankDirectDetails.isNotBlank() ->
            log.bankDirectDetails.lines().filter { it.isNotBlank() }
        bankDirectDeliveries.isNotEmpty() ->
            bankDirectDeliveries.map { del ->
                val paidP = maxOf(0, del.purifiedQty - del.freePurifiedQty.coerceIn(0, del.purifiedQty))
                val paidA = maxOf(0, del.alkalineQty - del.freeAlkalineQty.coerceIn(0, del.alkalineQty))
                val waterDesc = formatReportWaterSummary(paidP, paidA)
                String.format(Locale.US, "• %s (%s): ₱%,.2f", del.customerName, waterDesc, del.totalDue)
            }
        else -> emptyList()
    }
    val bankDirectGallons = if (log.bankDirectGallons > 0) {
        log.bankDirectGallons
    } else {
        bankDirectDeliveries.sumOf {
            maxOf(0, it.purifiedQty - it.freePurifiedQty.coerceIn(0, it.purifiedQty)) +
                maxOf(0, it.alkalineQty - it.freeAlkalineQty.coerceIn(0, it.alkalineQty))
        }
    }
    val bankDirectAmount = if (log.bankDirectAmount > 0.0) {
        log.bankDirectAmount
    } else {
        bankDirectDeliveries.sumOf { it.totalDue }
    }

    val bayadUtangLines = when {
        log.utangPaymentDetails.isNotBlank() ->
            log.utangPaymentDetails.lines().filter { it.isNotBlank() }
        matchedPayments.isNotEmpty() ->
            matchedPayments.map { pay ->
                val statusTag = if (pay.remainingBalance > 0.0) {
                    String.format(Locale.US, "Hulog lang | Balanse: ₱%,.2f", pay.remainingBalance)
                } else if (pay.commissionGallonsEarned > 0.0) {
                    "${formatReportGallonCount(pay.commissionGallonsEarned)} gal - FULLY PAID"
                } else {
                    "FULLY PAID"
                }
                String.format(Locale.US, "• %s (%s): +₱%,.2f", pay.customerName, statusTag, pay.amount)
            }
        else -> emptyList()
    }

    val paidPautangHeaderLines = if (matchedPayments.isNotEmpty()) {
        matchedPayments
            .filter { it.remainingBalance == 0.0 && it.commissionGallonsEarned > 0.0 }
            .map { pay ->
                String.format(Locale.US, "+%s gal - %s (Paid Pautang)", formatReportGallonCount(pay.commissionGallonsEarned), pay.customerName)
            }
    } else if (log.utangPaymentDetails.isNotBlank()) {
        val fullyPaidRegex = Regex("""•\s*(.+?)\s*\(([\d.]+)\s*gal\s*-\s*FULLY PAID\):""")
        log.utangPaymentDetails.lines().mapNotNull { line ->
            val match = fullyPaidRegex.find(line)
            if (match != null) {
                val name = match.groupValues[1]
                val gal = match.groupValues[2]
                "+$gal gal - $name (Paid Pautang)"
            } else null
        }
    } else emptyList()

    val expenseLines = when {
        log.expenseDetails.isNotBlank() ->
            log.expenseDetails.lines().filter { it.isNotBlank() }
        log.id != 0L ->
            allExpenses.filter { it.remittanceId == log.id }.map { exp ->
                val label = if (exp.notes.isNotBlank()) "${exp.category} (${exp.notes.trim()})" else exp.category
                String.format(Locale.US, "• %s: -₱%,.2f", label, exp.amount)
            }
        else -> emptyList()
    }

    val advanceLines = when {
        log.advanceRemitDetails.isNotBlank() ->
            log.advanceRemitDetails.lines().filter { it.isNotBlank() }
        matchedAdvances.isNotEmpty() ->
            matchedAdvances.map { adv ->
                val timeStr = timeFormat.format(Date(adv.timestamp))
                val label = if (adv.notes.isNotBlank()) "${adv.notes.trim()} ($timeStr)" else timeStr
                String.format(Locale.US, "• %s: -₱%,.2f", label, adv.amount)
            }
        else -> emptyList()
    }
    val totalAdvanceRemits = if (log.totalAdvanceRemits > 0.0) {
        log.totalAdvanceRemits
    } else {
        matchedAdvances.sumOf { it.amount }
    }
    val totalDayRemitted = if (log.totalDayRemitted > 0.0) {
        log.totalDayRemitted
    } else {
        totalAdvanceRemits + log.netRemit
    }
    val totalCommissionGallons = when {
        log.totalCommissionGallons > 0.0 -> log.totalCommissionGallons
        matchedDeliveries.isNotEmpty() || matchedPayments.isNotEmpty() ->
            matchedDeliveries.sumOf { it.commissionGallons + it.utangCommissionGallons } +
                matchedPayments.sumOf { it.commissionGallonsEarned }
        else -> totalCashPaidGallons.toDouble()
    }

    return buildString {
        appendLine(dateHeader)
        appendLine("Melvin WaterRider - Remit Report ($dateLabel)")
        appendLine()
        appendLine("$totalCashPaidGallons gal = Delivered (Paid)")
        paidPautangHeaderLines.forEach { appendLine(it) }
        appendLine("GALON NA MAY BAYAD (Ngayong Araw): $totalCashPaidGallons gal ($cashPaidPurified Purified | $cashPaidAlkaline Alkaline)")
        appendLine(String.format(Locale.US, "Benta sa Cash Delivered: ₱%,.2f", cashDeliverySales))
        if (bankDirectGallons > 0 || bankDirectLines.isNotEmpty()) {
            appendLine(String.format(Locale.US, "Bank / Direct (Diretso kay Boss): %d gal — ₱%,.2f", bankDirectGallons, bankDirectAmount))
            bankDirectLines.forEach { appendLine(it) }
        }
        if (bayadUtangLines.isNotEmpty()) {
            appendLine()
            appendLine("BAYAD UTANG (Dagdag sa Galon & Cash):")
            bayadUtangLines.forEach { appendLine(it) }
        }
        if (expenseLines.isNotEmpty() || log.totalExpenses > 0.0) {
            appendLine()
            appendLine("GASTOS SA ROTA:")
            if (expenseLines.isNotEmpty()) {
                expenseLines.forEach { appendLine(it) }
            } else {
                appendLine(String.format(Locale.US, "• Gastos: -₱%,.2f", log.totalExpenses))
            }
        }
        if (advanceLines.isNotEmpty() || totalAdvanceRemits > 0.0) {
            appendLine()
            appendLine("ADVANCE REMIT:")
            if (advanceLines.isNotEmpty()) {
                advanceLines.forEach { appendLine(it) }
            } else {
                appendLine(String.format(Locale.US, "• Advance Remit: -₱%,.2f", totalAdvanceRemits))
            }
        }
        appendLine("-------------------------")
        appendLine("TOTAL GALON (Para sa Commission): ${formatReportGallonCount(totalCommissionGallons)} gal")
        appendLine(String.format(Locale.US, "NATITIRANG CASH NA IRE-REMIT: ₱%,.2f", log.netRemit))
        append(String.format(Locale.US, "(Kabuuang Remit Buong Araw: ₱%,.2f)", totalDayRemitted))
        if (log.notes.isNotBlank()) {
            appendLine()
            appendLine()
            append("Mga Tala: ${log.notes}")
        }
    }
}

private fun getTagalogDayName(date: Date): String {
    val cal = Calendar.getInstance().apply { time = date }
    return when (cal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> "Lunes"
        Calendar.TUESDAY -> "Martes"
        Calendar.WEDNESDAY -> "Miyerkules"
        Calendar.THURSDAY -> "Huwebes"
        Calendar.FRIDAY -> "Biyernes"
        Calendar.SATURDAY -> "Sabado"
        Calendar.SUNDAY -> "Linggo"
        else -> "Lunes"
    }
}

private fun formatReportGallonCount(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else String.format(Locale.US, "%.1f", rounded)
}

private fun formatReportWaterSummary(purifiedQty: Int, alkalineQty: Int): String {
    val parts = mutableListOf<String>()
    if (purifiedQty > 0) parts.add("$purifiedQty Purified")
    if (alkalineQty > 0) parts.add("$alkalineQty Alkaline")
    return parts.joinToString(", ").ifEmpty { "0 gal" }
}

private fun copyToClipboard(context: Context, text: String, showToast: Boolean = true) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Remittance Report", text))
    if (showToast) {
        Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
    }
}

private fun shareReport(context: Context, text: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share Remittance Report"))
}

@Composable
fun DeliveryCard(
    delivery: DeliveryEntity,
    onClick: () -> Unit
) {
    val themeColors = WaterRiderTheme.colors
    val purBadgeTextColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
    val alkBadgeTextColor = if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText

    BigCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(delivery.customerName, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = themeColors.textPrimary)
                Text(delivery.customerAddress, fontSize = 12.sp, color = themeColors.textSecondary)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (delivery.purifiedQty > 0) {
                        Surface(color = themeColors.primaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "Purified: ${delivery.purifiedQty}" + if (delivery.freePurifiedQty > 0) " (${delivery.freePurifiedQty} free)" else "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = purBadgeTextColor
                            )
                        }
                    }
                    if (delivery.alkalineQty > 0) {
                        Surface(color = themeColors.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                "Alkaline: ${delivery.alkalineQty}" + if (delivery.freeAlkalineQty > 0) " (${delivery.freeAlkalineQty} free)" else "",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = alkBadgeTextColor
                            )
                        }
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text("₱%.2f".format(delivery.totalDue), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = themeColors.textPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                val (badgeBg, badgeText) = when (delivery.paymentStatus) {
                    "Paid", "Paid at Station", "May Sukli" -> themeColors.paidContainer to themeColors.paid
                    "Direct to Boss" -> themeColors.secondaryContainer to alkBadgeTextColor
                    else -> themeColors.utangContainer to if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
                }
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        delivery.paymentStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        color = badgeText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
