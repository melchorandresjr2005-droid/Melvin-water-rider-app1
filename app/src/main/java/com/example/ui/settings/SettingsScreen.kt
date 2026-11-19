package com.example.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.AnimatedRollingAmount
import com.example.ui.components.BigCard
import com.example.ui.components.WaterDropletRiderBadge
import com.example.ui.theme.AppThemeId
import com.example.ui.theme.WaterRiderTheme
import com.example.ui.theme.paletteForTheme
import com.example.util.MessengerUtils
import com.example.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val backupConfig by viewModel.backupConfig.collectAsState()
    val defaultPriceConfig by viewModel.defaultPriceConfig.collectAsState()
    val bossContactConfig by viewModel.bossContactConfig.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val deliveries by viewModel.deliveries.collectAsState()
    val payments by viewModel.payments.collectAsState()
    val activeVales by viewModel.activeVales.collectAsState()
    val weeklyPayouts by viewModel.weeklyPayouts.collectAsState()
    val backupReminderDismissed by viewModel.backupReminderDismissed.collectAsState()
    val updateStatus by viewModel.updateStatus.collectAsState()
    val updateDownloadUrl by viewModel.updateDownloadUrl.collectAsState()

    var initializedFromSettings by remember { mutableStateOf(false) }
    var initializedFromDataStore by remember { mutableStateOf(false) }
    var defaultPurifiedStr by remember {
        mutableStateOf(defaultPriceConfig.defaultPurifiedPrice.toString())
    }
    var defaultAlkalineStr by remember {
        mutableStateOf(defaultPriceConfig.defaultAlkalinePrice.toString())
    }
    var bossMessengerStr by remember {
        mutableStateOf(bossContactConfig.messengerUsername)
    }
    var bossPhoneStr by remember {
        mutableStateOf(bossContactConfig.phoneNumber)
    }
    var commissionRateStr by remember {
        mutableStateOf((settings?.commissionPerGallon ?: 5.0).toString())
    }
    var purifiedPreset1Str by remember {
        mutableStateOf((settings?.purifiedPreset1 ?: 25.0).toString())
    }
    var purifiedPreset2Str by remember {
        mutableStateOf((settings?.purifiedPreset2 ?: settings?.defaultPurifiedPrice ?: 35.0).toString())
    }
    var alkalinePreset1Str by remember {
        mutableStateOf((settings?.alkalinePreset1 ?: 30.0).toString())
    }
    var alkalinePreset2Str by remember {
        mutableStateOf((settings?.alkalinePreset2 ?: settings?.defaultAlkalinePrice ?: 45.0).toString())
    }

    LaunchedEffect(defaultPriceConfig, bossContactConfig) {
        if (!initializedFromDataStore) {
            defaultPurifiedStr = defaultPriceConfig.defaultPurifiedPrice.toString()
            defaultAlkalineStr = defaultPriceConfig.defaultAlkalinePrice.toString()
            bossMessengerStr = bossContactConfig.messengerUsername
            bossPhoneStr = bossContactConfig.phoneNumber
            if (defaultPriceConfig.defaultPurifiedPrice > 0 || bossContactConfig.messengerUsername.isNotEmpty() || bossContactConfig.phoneNumber.isNotEmpty()) {
                initializedFromDataStore = true
            }
        }
    }

    LaunchedEffect(settings) {
        val s = settings ?: return@LaunchedEffect
        if (!initializedFromSettings) {
            commissionRateStr = s.commissionPerGallon.toString()
            purifiedPreset1Str = s.purifiedPreset1.toString()
            purifiedPreset2Str = s.purifiedPreset2.toString()
            alkalinePreset1Str = s.alkalinePreset1.toString()
            alkalinePreset2Str = s.alkalinePreset2.toString()
            initializedFromSettings = true
        }
    }

    var backupStatusMsg by remember { mutableStateOf<String?>(null) }

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val todayStr = remember(dateFormat) { dateFormat.format(Date()) }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDatabase(context, uri) { success ->
                backupStatusMsg = if (success) "Backup exported successfully!" else "Export failed."
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importDatabase(context, uri) { success ->
                backupStatusMsg = if (success) "Database restored successfully!" else "Restore failed."
            }
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            viewModel.saveBackupFolderSelection(context, uri)
            Toast.makeText(context, "Google Drive backup folder connected!", Toast.LENGTH_SHORT).show()
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.exportDeliveriesCsv(context, uri) { success ->
                val msg = if (success) {
                    "Deliveries exported to CSV!"
                } else {
                    "CSV export failed."
                }
                backupStatusMsg = msg
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Current Monday-Saturday workweek range
    val (monDateStr, satDateStr, weekRangeLabel) = remember(dateFormat) {
        val cal = Calendar.getInstance()
        cal.firstDayOfWeek = Calendar.MONDAY
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val mon = dateFormat.format(cal.time)
        cal.add(Calendar.DAY_OF_MONTH, 5)
        val sat = dateFormat.format(cal.time)
        Triple(mon, sat, "$mon to $sat")
    }

    val showBackupReminder = remember(backupReminderDismissed, settings?.lastBackupTimestamp) {
        val isSaturday = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        val lastBackup = settings?.lastBackupTimestamp ?: 0L
        val daysSinceBackup = (System.currentTimeMillis() - lastBackup) / (1000 * 60 * 60 * 24)
        !backupReminderDismissed && (isSaturday || daysSinceBackup >= 7)
    }

    // Filter deliveries & Utang collections within Mon-Sat of current workweek
    val weekDeliveries = remember(deliveries, monDateStr, satDateStr) {
        deliveries.filter { it.dateString in monDateStr..satDateStr }
    }
    val weekPayments = remember(payments, monDateStr, satDateStr) {
        payments.filter { it.dateString in monDateStr..satDateStr }
    }
    val earnedGallonsDouble = remember(weekDeliveries, weekPayments) {
        weekDeliveries.sumOf { it.commissionGallons + it.utangCommissionGallons } +
            weekPayments.sumOf { it.commissionGallonsEarned }
    }
    val pendingGallonsDouble = remember(weekDeliveries) {
        weekDeliveries.sumOf { it.pendingCommissionAdded }
    }
    val paidGallons = remember(earnedGallonsDouble) { kotlin.math.round(earnedGallonsDouble).toInt() }
    val freeGallons = remember(weekDeliveries) {
        weekDeliveries.sumOf { it.freePurifiedQty + it.freeAlkalineQty }
    }

    val commissionPerGallon = settings?.commissionPerGallon ?: 5.0
    val grossCommission = remember(earnedGallonsDouble, commissionPerGallon) {
        earnedGallonsDouble * commissionPerGallon
    }
    val totalVale = remember(activeVales) { activeVales.sumOf { it.amount } }
    val netPay = remember(grossCommission, totalVale) { maxOf(0.0, grossCommission - totalVale) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // App Branding / About Section
        BigCard(
            containerColor = themeColors.primaryContainer.copy(alpha = 0.65f),
            borderColor = themeColors.primaryAccent.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WaterDropletRiderBadge(size = 52.dp)
                Column {
                    Text(
                        "Melvin WaterRider",
                        style = MaterialTheme.typography.titleLarge,
                        color = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
                    )
                    Text(
                        "Offline-First Delivery & Remittance Companion",
                        fontSize = 12.sp,
                        color = themeColors.textSecondary
                    )
                    Text(
                        "Workweek: Mon–Sat (Sunday Rest Day)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.paid
                    )
                }
            }
        }

        // App Theme (Kulay ng App) Instant Switcher Card
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "App Theme",
                        tint = themeColors.primaryAccent
                    )
                    Column {
                        Text(
                            "App Theme (Kulay ng App)",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            "1-tap high-contrast themes • Saved offline",
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppThemeId.entries.forEach { theme ->
                        val isSelected = selectedTheme == theme
                        val previewPalette = remember(theme) { paletteForTheme(theme) }
                        val cardBg by animateColorAsState(
                            targetValue = if (isSelected) {
                                themeColors.primaryContainer.copy(alpha = if (themeColors.isDark) 0.55f else 0.75f)
                            } else {
                                themeColors.surfaceVariant.copy(alpha = 0.55f)
                            },
                            label = "ThemeOptionBg_${theme.id}"
                        )
                        val cardBorderColor by animateColorAsState(
                            targetValue = if (isSelected) themeColors.primaryAccent else themeColors.border,
                            label = "ThemeOptionBorder_${theme.id}"
                        )

                        Surface(
                            color = cardBg,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(if (isSelected) 2.dp else 1.dp, cardBorderColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.selectTheme(theme)
                                }
                                .testTag("theme_option_${theme.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    // 3-Dot Palette Swatch Pill
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy((-4).dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.background)
                                                .border(1.dp, previewPalette.border, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.primaryAccent)
                                                .border(1.dp, previewPalette.surface, CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .clip(CircleShape)
                                                .background(previewPalette.secondaryAccent)
                                                .border(1.dp, previewPalette.surface, CircleShape)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = theme.title,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected && themeColors.isDark) {
                                                themeColors.onPrimaryContainer
                                            } else {
                                                themeColors.textPrimary
                                            }
                                        )
                                        Text(
                                            text = theme.subtitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isSelected && themeColors.isDark) {
                                                themeColors.onPrimaryContainer.copy(alpha = 0.9f)
                                            } else {
                                                themeColors.textSecondary
                                            }
                                        )
                                    }
                                }

                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Selected Theme",
                                        tint = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dismissible Saturday Backup Reminder
        if (showBackupReminder) {
            BigCard(
                containerColor = themeColors.utangContainer,
                borderColor = themeColors.utang,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠️ Weekly Saturday Backup Reminder", fontWeight = FontWeight.Bold, color = themeColors.utangSubtext)
                        TextButton(onClick = { viewModel.dismissBackupReminder() }) {
                            Text("Dismiss", color = themeColors.utangSubtext, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        "Protect your delivery records, utang balances, and payroll history by backing up to Google Drive.",
                        fontSize = 13.sp,
                        color = themeColors.utangSubtext
                    )
                    Button(
                        onClick = { exportLauncher.launch("MelvinWaterRider_Backup_$todayStr.json") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryAccent,
                            contentColor = themeColors.onPrimaryAccent
                        ),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = "Backup")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Backup to Google Drive", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Default Price List Card (Persisted via DataStore)
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Payments,
                        contentDescription = "Default Price List",
                        tint = themeColors.primaryAccent
                    )
                    Column {
                        Text(
                            "Default Price List (Per Gallon)",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            "Saved offline via DataStore • Auto-fills + Add Delivery (overridden by Suki custom prices)",
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = defaultPurifiedStr,
                        onValueChange = { input ->
                            defaultPurifiedStr = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updateDefaultPriceList(
                                    purifiedPrice = parsed,
                                    alkalinePrice = defaultAlkalineStr.toDoubleOrNull()
                                        ?: defaultPriceConfig.defaultAlkalinePrice
                                )
                            }
                        },
                        label = { Text("Purified Default (₱)") },
                        placeholder = { Text("35.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("datastore_default_purified_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = defaultAlkalineStr,
                        onValueChange = { input ->
                            defaultAlkalineStr = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updateDefaultPriceList(
                                    purifiedPrice = defaultPurifiedStr.toDoubleOrNull()
                                        ?: defaultPriceConfig.defaultPurifiedPrice,
                                    alkalinePrice = parsed
                                )
                            }
                        },
                        label = { Text("Alkaline Default (₱)") },
                        placeholder = { Text("45.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("datastore_default_alkaline_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Boss Contact Setup Card (1-Tap Messenger)
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Boss Contact",
                        tint = themeColors.primaryAccent
                    )
                    Column {
                        Text(
                            "Boss Contact (1-Tap Messenger)",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            "Save Boss Messenger username/m.me link & phone for instant dashboard chat",
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
                OutlinedTextField(
                    value = bossMessengerStr,
                    onValueChange = { input ->
                        bossMessengerStr = input
                        viewModel.updateBossContact(input, bossPhoneStr)
                    },
                    label = { Text("Boss Messenger Username / Link") },
                    placeholder = { Text("e.g., boss.waterstation or m.me/boss.waterstation") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boss_messenger_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                OutlinedTextField(
                    value = bossPhoneStr,
                    onValueChange = { input ->
                        bossPhoneStr = input
                        viewModel.updateBossContact(bossMessengerStr, input)
                    },
                    label = { Text("Boss Phone Number (Optional)") },
                    placeholder = { Text("e.g., 09171234567") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("boss_phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        MessengerUtils.openBossMessengerChat(
                            context = context,
                            messengerUsername = bossMessengerStr,
                            phoneNumber = bossPhoneStr
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.primaryAccent,
                        contentColor = themeColors.onPrimaryAccent
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("settings_chat_boss_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Chat kay Boss", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Chat kay Boss (Messenger)", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Quick-Tap Price Presets Card
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Quick-Tap Price Presets (Per Gallon)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Set 2 quick-tap price buttons each for Purified and Alkaline water. Preset 2 acts as the default price for new customers.",
                    fontSize = 12.sp,
                    color = themeColors.textSecondary
                )

                Text(
                    "Purified Water Presets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.primaryAccent
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = purifiedPreset1Str,
                        onValueChange = { input ->
                            purifiedPreset1Str = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updatePurifiedPreset1(parsed)
                            }
                        },
                        label = { Text("Preset 1 (₱)") },
                        placeholder = { Text("25.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("purified_preset1_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = purifiedPreset2Str,
                        onValueChange = { input ->
                            purifiedPreset2Str = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updatePurifiedPreset2(parsed)
                            }
                        },
                        label = { Text("Preset 2 / Default (₱)") },
                        placeholder = { Text("35.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("purified_preset2_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                Text(
                    "Alkaline Water Presets",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeColors.secondaryAccentText
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = alkalinePreset1Str,
                        onValueChange = { input ->
                            alkalinePreset1Str = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updateAlkalinePreset1(parsed)
                            }
                        },
                        label = { Text("Preset 1 (₱)") },
                        placeholder = { Text("30.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alkaline_preset1_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                    OutlinedTextField(
                        value = alkalinePreset2Str,
                        onValueChange = { input ->
                            alkalinePreset2Str = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0.0) {
                                viewModel.updateAlkalinePreset2(parsed)
                            }
                        },
                        label = { Text("Preset 2 / Default (₱)") },
                        placeholder = { Text("45.0") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("alkaline_preset2_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )
                }
            }
        }

        // Saturday Payday Summary & Commission Calculator Card
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Mon–Sat Payroll Summary", style = MaterialTheme.typography.titleMedium)
                Text("Current Week: $weekRangeLabel", fontSize = 12.sp, color = themeColors.textSecondary)

                OutlinedTextField(
                    value = commissionRateStr,
                    onValueChange = {
                        commissionRateStr = it
                        val rate = it.toDoubleOrNull()
                        if (rate != null) {
                            viewModel.updateCommissionRate(rate)
                        }
                    },
                    label = { Text("Commission Per Paid Gallon (₱)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                HorizontalDivider()

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Commission-Earned Gallons:")
                    Text(
                        text = if (earnedGallonsDouble % 1.0 == 0.0) "${earnedGallonsDouble.toInt()} gal" else String.format(Locale.US, "%.1f gal", earnedGallonsDouble),
                        fontWeight = FontWeight.Bold
                    )
                }
                if (pendingGallonsDouble > 0.0) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Pending Utang Gallons (₱0 until paid):", fontSize = 13.sp, color = themeColors.utang)
                        Text(
                            text = if (pendingGallonsDouble % 1.0 == 0.0) "${pendingGallonsDouble.toInt()} gal" else String.format(Locale.US, "%.1f gal", pendingGallonsDouble),
                            color = themeColors.utang,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Free/Damaged Gallons (₱0 comm):")
                    Text("$freeGallons gal", color = themeColors.textSecondary)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Gross Commission Earned:")
                    Text("₱%.2f".format(grossCommission), fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Less Cash Advances (Vale):")
                    Text("- ₱%.2f".format(totalVale), color = themeColors.utang, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Net Take-Home Pay:", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    AnimatedRollingAmount(
                        text = "₱%.2f".format(netPay),
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp),
                        color = themeColors.paid
                    )
                }

                Button(
                    onClick = {
                        viewModel.archiveWeeklyPayout(
                            weekLabel = weekRangeLabel,
                            paidGallons = paidGallons,
                            freeGallons = freeGallons,
                            commissionRate = commissionPerGallon,
                            grossCommission = grossCommission,
                            totalVale = totalVale,
                            netTakeHomePay = netPay
                        )
                        Toast.makeText(context, "Weekly payout archived!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.paid,
                        contentColor = themeColors.onPaid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Archive Payout")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Close Week & Archive Payout", fontWeight = FontWeight.Bold)
                }

                if (weeklyPayouts.isNotEmpty()) {
                    HorizontalDivider()
                    Text("Past Weekly Payouts Archive", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    weeklyPayouts.take(6).forEach { payout ->
                        Surface(
                            color = themeColors.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(payout.weekLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("${payout.paidGallons} paid gal • Vale ₱%.0f".format(payout.totalVale), fontSize = 11.sp, color = themeColors.textSecondary)
                                }
                                Text("₱%.2f".format(payout.netTakeHomePay), fontWeight = FontWeight.ExtraBold, color = themeColors.paid)
                            }
                        }
                    }
                }
            }
        }

        // Interactive Monthly Delivery Calendar History
        DeliveryCalendarHistoryCard(
            deliveries = deliveries,
            payments = payments,
            commissionPerGallon = commissionPerGallon
        )

        // Google Drive Auto-Backup Card (SAF OpenDocumentTree)
        BigCard(modifier = Modifier.fillMaxWidth()) {
            val autoBackupTimeFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Google Drive Auto-Backup",
                        tint = themeColors.primaryAccent
                    )
                    Column {
                        Text(
                            "Google Drive Auto-Backup",
                            style = MaterialTheme.typography.titleMedium,
                            color = themeColors.textPrimary
                        )
                        Text(
                            "Auto-saves MelvinWaterRider_Backup.json on every Remit / Copy Report",
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                Surface(
                    color = if (backupConfig.isConnected) themeColors.paidContainer else themeColors.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("auto_backup_status_banner")
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = if (backupConfig.isConnected) Icons.Default.CheckCircle else Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = if (backupConfig.isConnected) themeColors.paid else themeColors.textSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (backupConfig.isConnected) {
                                    "Connected: ${backupConfig.folderName ?: "Google Drive Folder"}"
                                } else {
                                    "Local Fail-Safe Active (No Drive Folder Linked)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (backupConfig.isConnected) themeColors.paid else themeColors.textPrimary
                            )
                        }
                        Text(
                            text = if (backupConfig.lastAutoBackupTimestamp > 0L) {
                                "Last auto-backup: ${autoBackupTimeFormat.format(Date(backupConfig.lastAutoBackupTimestamp))}"
                            } else if (backupConfig.isConnected) {
                                "Ready — next Remit or Copy Report will auto-save to this folder."
                            } else {
                                "Tap Select Backup Folder to pick your Google Drive folder."
                            },
                            fontSize = 12.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColors.primaryAccent,
                            contentColor = themeColors.onPrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 54.dp)
                            .testTag("select_backup_folder_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = "Select Backup Folder", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Select Backup Folder",
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 54.dp)
                            .testTag("restore_auto_backup_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Restore Backup", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Restore Backup",
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }
            }
        }

        // Data Safety & Google Drive Backup / Restore Card
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Data Safety & Google Drive Backup", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Export or restore all Room records (customers, deliveries, utang/sukli, expenses, remittances, payroll) as JSON.",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { exportLauncher.launch("MelvinWaterRider_Backup_$todayStr.json") },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = "Export", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Backup JSON",
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 54.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Import", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Restore JSON",
                                fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                softWrap = true,
                                overflow = TextOverflow.Visible
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { csvExportLauncher.launch("MelvinWaterRider_Deliveries_$todayStr.csv") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("export_deliveries_csv_button")
                ) {
                    Icon(
                        Icons.Default.TableChart,
                        contentDescription = "Export Deliveries CSV",
                        tint = themeColors.primaryAccent
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Export Deliveries to CSV (Spreadsheet)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeColors.primaryAccent
                    )
                }

                if (backupStatusMsg != null) {
                    Surface(color = themeColors.paidContainer, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            backupStatusMsg!!,
                            modifier = Modifier.padding(8.dp),
                            color = if (themeColors.isDark) themeColors.paid else themeColors.paid,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // GitHub Actions APK Update Checker Card
        BigCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("App Updates (GitHub Releases)", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Check for latest APK builds generated via GitHub Actions workflow.",
                    fontSize = 13.sp,
                    color = themeColors.textSecondary
                )

                Button(
                    onClick = { viewModel.checkForAppUpdates() },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Check")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Check for App Updates", fontWeight = FontWeight.Bold)
                }

                if (updateStatus != null) {
                    Text(updateStatus!!, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                if (updateDownloadUrl != null && updateDownloadUrl!!.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.downloadAndInstallUpdate(context, updateDownloadUrl!!)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.paid,
                                contentColor = themeColors.onPaid
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("download_latest_apk_button")
                        ) {
                            Icon(Icons.Default.CloudDownload, contentDescription = "Download APK", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download & Install Latest APK", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                viewModel.installDownloadedApk(context)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("install_downloaded_apk_button")
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Install APK", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Install Downloaded APK", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(96.dp))
    }
}
