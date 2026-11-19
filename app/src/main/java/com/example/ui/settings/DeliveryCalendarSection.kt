package com.example.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DeliveryEntity
import com.example.data.PaymentEntity
import com.example.ui.components.BigCard
import com.example.ui.theme.WaterRiderTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryCalendarHistoryCard(
    deliveries: List<DeliveryEntity>,
    payments: List<PaymentEntity>,
    commissionPerGallon: Double
) {
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    val isoFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { isoFormat.format(Date()) }

    var monthOffset by remember { mutableIntStateOf(0) }
    var selectedDateStr by remember { mutableStateOf<String?>(todayStr) }

    val monthCal = remember(monthOffset) {
        Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.MONTH, monthOffset)
        }
    }
    val monthTitle = remember(monthCal) {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(monthCal.time)
    }
    val displayedYear = monthCal.get(Calendar.YEAR)
    val displayedMonth = monthCal.get(Calendar.MONTH)
    val daysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingEmptyDays = monthCal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY

    val deliveriesByDate = remember(deliveries) { deliveries.groupBy { it.dateString } }
    val paymentsByDate = remember(payments) { payments.groupBy { it.dateString } }

    BigCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header + Month Navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = themeColors.primaryAccent
                    )
                    Column {
                        Text(
                            "Delivery Calendar History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Tap a date to inspect deliveries & collections",
                            fontSize = 11.sp,
                            color = themeColors.textSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        monthOffset -= 1
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_prev_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous Month"
                    )
                }

                Text(
                    text = monthTitle,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = themeColors.primaryAccent,
                    modifier = Modifier.testTag("calendar_month_label")
                )

                FilledTonalIconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        monthOffset += 1
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("calendar_next_month")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next Month"
                    )
                }
            }

            // Weekday Header Row
            val weekDays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekDays.forEach { dayName ->
                    Text(
                        text = dayName,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (dayName == "Sun") themeColors.utang else themeColors.textSecondary
                    )
                }
            }

            // Monthly Grid Cells
            val totalCells = leadingEmptyDays + daysInMonth
            val rows = (totalCells + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0 until 7) {
                            val cellIndex = row * 7 + col
                            val dayNum = cellIndex - leadingEmptyDays + 1
                            if (dayNum in 1..daysInMonth) {
                                val dateStr = String.format(
                                    Locale.US,
                                    "%04d-%02d-%02d",
                                    displayedYear,
                                    displayedMonth + 1,
                                    dayNum
                                )
                                val dayDeliveries = deliveriesByDate[dateStr].orEmpty()
                                val dayPayments = paymentsByDate[dateStr].orEmpty()
                                val hasDelivery = dayDeliveries.isNotEmpty()
                                val hasPayment = dayPayments.isNotEmpty()
                                val isSelected = selectedDateStr == dateStr
                                val isToday = dateStr == todayStr

                                val cellBg = when {
                                    isSelected -> themeColors.primaryAccent
                                    hasDelivery || hasPayment -> themeColors.primaryContainer.copy(alpha = 0.55f)
                                    isToday -> themeColors.surfaceVariant
                                    else -> Color.Transparent
                                }
                                val cellBorder = when {
                                    isToday && !isSelected -> BorderStroke(1.dp, themeColors.primaryAccent)
                                    else -> null
                                }

                                Surface(
                                    color = cellBg,
                                    shape = RoundedCornerShape(12.dp),
                                    border = cellBorder,
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedDateStr = dateStr
                                        }
                                        .testTag("calendar_day_$dateStr")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "$dayNum",
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected || isToday || hasDelivery || hasPayment) FontWeight.ExtraBold else FontWeight.Medium,
                                            color = when {
                                                isSelected -> themeColors.onPrimaryAccent
                                                hasDelivery || hasPayment -> themeColors.onPrimaryContainer
                                                col == 0 -> themeColors.utang
                                                else -> themeColors.textPrimary
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.height(10.dp)
                                        ) {
                                            if (hasDelivery) {
                                                Icon(
                                                    imageVector = Icons.Default.WaterDrop,
                                                    contentDescription = "Has Delivery",
                                                    tint = if (isSelected) themeColors.onPrimaryAccent else if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                            }
                                            if (hasPayment) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(if (isSelected) themeColors.onPrimaryAccent else themeColors.paid)
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f).height(48.dp))
                            }
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = themeColors.primaryAccent,
                        modifier = Modifier.size(12.dp)
                    )
                    Text("May Delivery", fontSize = 11.sp, color = themeColors.textSecondary)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(themeColors.paid)
                    )
                    Text("May Bayad Utang", fontSize = 11.sp, color = themeColors.textSecondary)
                }
            }

            // Smoothly Animated Daily History Breakdown Card
            AnimatedVisibility(
                visible = selectedDateStr != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                selectedDateStr?.let { dateKey ->
                    AnimatedContent(
                        targetState = dateKey,
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "DailyHistoryBreakdown"
                    ) { activeDate ->
                        val dayDeliveries = deliveriesByDate[activeDate].orEmpty()
                        val dayPayments = paymentsByDate[activeDate].orEmpty()
                        DailyHistoryBreakdownCard(
                            dateStr = activeDate,
                            deliveries = dayDeliveries,
                            payments = dayPayments,
                            commissionPerGallon = commissionPerGallon
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyHistoryBreakdownCard(
    dateStr: String,
    deliveries: List<DeliveryEntity>,
    payments: List<PaymentEntity>,
    commissionPerGallon: Double
) {
    val themeColors = WaterRiderTheme.colors
    val displayDate = remember(dateStr) {
        try {
            val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)
            if (parsed != null) SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()).format(parsed) else dateStr
        } catch (_: Exception) {
            dateStr
        }
    }

    val totalPurified = deliveries.sumOf { it.purifiedQty }
    val totalAlkaline = deliveries.sumOf { it.alkalineQty }
    val totalFree = deliveries.sumOf { it.freePurifiedQty + it.freeAlkalineQty }
    val totalCashCollected = deliveries.sumOf { it.amountPaid } + payments.sumOf { it.amount }
    val totalPautang = deliveries
        .filter { !it.isBankDirect && it.paymentStatus in listOf("Utang", "Partial", "Unpaid / Collect on Delivery") }
        .sumOf { maxOf(0.0, it.totalDue - it.amountPaid) }
    val earnedGallons = deliveries.sumOf { it.commissionGallons + it.utangCommissionGallons } +
        payments.sumOf { it.commissionGallonsEarned }
    val totalCommission = earnedGallons * commissionPerGallon

    Surface(
        color = themeColors.surfaceVariant.copy(alpha = 0.65f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, themeColors.border),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_history_breakdown_card")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = displayDate,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = themeColors.primaryAccent
            )

            if (deliveries.isEmpty() && payments.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = themeColors.primaryAccent.copy(alpha = 0.6f),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "Walang delivery record sa petsang ito",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = themeColors.textSecondary
                    )
                }
            } else {
                // 1. Daily Totals Summary
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val purTextColor = if (themeColors.isDark) themeColors.onPrimaryContainer else themeColors.primaryAccent
                        Surface(
                            color = themeColors.primaryContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Gallons Delivered", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = purTextColor)
                                Text(
                                    "$totalPurified Pur • $totalAlkaline Alk",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = purTextColor
                                )
                                if (totalFree > 0) {
                                    Text("Free/Sira: $totalFree gal", fontSize = 10.sp, color = themeColors.expense, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        Surface(
                            color = themeColors.paidContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Cash Collected", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = themeColors.paid)
                                Text(
                                    "₱%,.2f".format(totalCashCollected),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = themeColors.paid
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val utangTextColor = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
                        Surface(
                            color = themeColors.utangContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Total Pautang", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = utangTextColor)
                                Text(
                                    "₱%,.2f".format(totalPautang),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = utangTextColor
                                )
                            }
                        }
                        val commTextColor = if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText
                        Surface(
                            color = themeColors.secondaryContainer.copy(alpha = 0.75f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("Commission Earned", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = commTextColor)
                                val galStr = if (earnedGallons % 1.0 == 0.0) "${earnedGallons.toInt()}" else String.format(Locale.US, "%.1f", earnedGallons)
                                Text(
                                    "₱%,.2f ($galStr gal)".format(totalCommission),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = commTextColor
                                )
                            }
                        }
                    }
                }

                // 2. Customer Delivery List
                if (deliveries.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        "Customer Deliveries (${deliveries.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        deliveries.forEach { del ->
                            val freeCount = del.freePurifiedQty + del.freeAlkalineQty
                            val (badgeLabel, badgeBg, badgeFg) = when {
                                del.isBankDirect || del.paymentStatus == "Direct to Boss" ->
                                    Triple(
                                        "Bank/Direct",
                                        themeColors.secondaryContainer,
                                        if (themeColors.isDark) themeColors.onSecondaryContainer else themeColors.secondaryAccentText
                                    )
                                del.paymentStatus == "Paid" || del.paymentStatus == "Paid at Station" ->
                                    Triple("Paid", themeColors.paidContainer, themeColors.paid)
                                del.paymentStatus == "May Sukli" ->
                                    Triple("May Sukli", themeColors.paidContainer, themeColors.paid)
                                del.paymentStatus == "Partial" ->
                                    Triple("Partial", themeColors.expenseContainer, themeColors.expense)
                                else ->
                                    Triple("Utang", themeColors.utangContainer, if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang)
                            }

                            Surface(
                                color = themeColors.surface,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(del.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.textPrimary)
                                        val qtyParts = buildList {
                                            if (del.purifiedQty > 0) add("Purified: ${del.purifiedQty}")
                                            if (del.alkalineQty > 0) add("Alkaline: ${del.alkalineQty}")
                                            if (freeCount > 0) add("Free/Sira: $freeCount")
                                        }.joinToString(" • ")
                                        if (qtyParts.isNotEmpty()) {
                                            Text(
                                                qtyParts,
                                                fontSize = 11.sp,
                                                color = themeColors.textSecondary
                                            )
                                        }
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text("₱%,.2f".format(del.totalDue), fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = themeColors.textPrimary)
                                        Surface(color = badgeBg, shape = RoundedCornerShape(6.dp)) {
                                            Text(
                                                badgeLabel,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeFg
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Bayad Utang Collected
                if (payments.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        "Bayad Utang Collected (${payments.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = themeColors.paid
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        payments.forEach { pay ->
                            val galNote = when {
                                pay.remainingBalance > 0.0 -> "Hulog lang (₱0 comm)"
                                pay.commissionGallonsEarned > 0.0 -> {
                                    val g = if (pay.commissionGallonsEarned % 1.0 == 0.0) "${pay.commissionGallonsEarned.toInt()}" else String.format(Locale.US, "%.1f", pay.commissionGallonsEarned)
                                    "$g gal unlocked (FULLY PAID)"
                                }
                                else -> "FULLY PAID"
                            }
                            val balNote = if (pay.remainingBalance > 0.0) " • Bal: ₱%,.2f".format(pay.remainingBalance) else " • Fully Paid"
                            Surface(
                                color = themeColors.paidContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pay.customerName, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = themeColors.textPrimary)
                                        Text(
                                            "$galNote$balNote",
                                            fontSize = 11.sp,
                                            color = themeColors.textSecondary
                                        )
                                    }
                                    Text(
                                        "+₱%,.2f".format(pay.amount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = themeColors.paid
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
