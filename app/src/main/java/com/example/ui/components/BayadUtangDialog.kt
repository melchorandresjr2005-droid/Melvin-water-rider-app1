package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
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
import com.example.ui.theme.WaterRiderTheme
import java.util.Locale

@Composable
fun BayadUtangDialog(
    customer: CustomerEntity,
    onDismiss: () -> Unit,
    onSavePayment: (Double, String) -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    val utangTextColor = if (themeColors.isDark) themeColors.utangSubtext else themeColors.utang
    val previousUtang = remember(customer.id) { customer.currentBalance }

    var amountStr by remember { mutableStateOf("") } // Empty-start with "0.0" placeholder
    var notes by remember { mutableStateOf("") }
    var paymentSaved by remember { mutableStateOf(false) }
    var savedPaidAmount by remember { mutableStateOf(0.0) }
    var savedRemainingUtang by remember { mutableStateOf(previousUtang) }
    var savedExtraSukli by remember { mutableStateOf(0.0) }

    val enteredAmount = amountStr.toDoubleOrNull() ?: 0.0
    val remainingUtang = maxOf(0.0, previousUtang - enteredAmount)
    val extraSukli = maxOf(0.0, enteredAmount - previousUtang)

    val receiptText = remember(paymentSaved, savedPaidAmount, savedRemainingUtang, savedExtraSukli, customer.name) {
        if (savedExtraSukli > 0.0) {
            String.format(
                Locale.US,
                "Hi %s, Bayad Utang: ₱%.2f. Natitirang Balance: ₱0.00 (May Sukli Credit: ₱%.2f). Salamat!",
                customer.name,
                savedPaidAmount,
                savedExtraSukli
            )
        } else {
            String.format(
                Locale.US,
                "Hi %s, Bayad Utang: ₱%.2f. Natitirang Balance: ₱%.2f. Salamat!",
                customer.name,
                savedPaidAmount,
                savedRemainingUtang
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (paymentSaved) "Payment Recorded" else "Bayad Utang: ${customer.name}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format(Locale.US, "Dating Utang: ₱%.2f", previousUtang),
                    fontSize = 14.sp,
                    color = themeColors.utang,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            AnimatedContent(targetState = paymentSaved, label = "bayad_utang_content") { isSaved ->
                if (!isSaved) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            label = { Text("Payment Amount (₱)") },
                            placeholder = { Text("0.0") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bayad_utang_amount_input"),
                            singleLine = true
                        )

                        // Quick 1-tap Full Payment Chip
                        AssistChip(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                amountStr = String.format(Locale.US, "%.2f", previousUtang)
                            },
                            label = {
                                Text(
                                    String.format(Locale.US, "Pay Full Balance (₱%.2f)", previousUtang),
                                    fontWeight = FontWeight.SemiBold
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("pay_full_balance_chip")
                        )

                        // Live Remaining Utang / Sukli Preview Card
                        val isFullyPaid = enteredAmount > 0.0 && remainingUtang <= 0.0
                        Surface(
                            color = if (isFullyPaid) themeColors.paidContainer else themeColors.utangContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = if (isFullyPaid) {
                                        "Natitirang Utang: Fully Paid (₱0.00)"
                                    } else {
                                        String.format(Locale.US, "Natitirang Utang: ₱%.2f", remainingUtang)
                                    },
                                    color = if (isFullyPaid) themeColors.paid else utangTextColor,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                                if (extraSukli > 0.0) {
                                    Text(
                                        text = String.format(Locale.US, "May Sukli / Advance Credit: +₱%.2f", extraSukli),
                                        color = themeColors.paid,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes / Receipt No. (Optional)") },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                keyboardType = KeyboardType.Text
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            color = themeColors.paidContainer,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = "Payment Saved",
                                        tint = themeColors.paid
                                    )
                                    Text(
                                        text = String.format(Locale.US, "Nagbayad: ₱%.2f", savedPaidAmount),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = themeColors.paid
                                    )
                                }
                                Text(
                                    text = if (savedRemainingUtang <= 0.0) {
                                        "Natitirang Utang: Fully Paid (₱0.00)"
                                    } else {
                                        String.format(Locale.US, "Natitirang Utang: ₱%.2f", savedRemainingUtang)
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (savedRemainingUtang <= 0.0) themeColors.paid else utangTextColor
                                )
                                if (savedExtraSukli > 0.0) {
                                    Text(
                                        text = String.format(Locale.US, "Stored Sukli Credit: +₱%.2f", savedExtraSukli),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = themeColors.paid
                                    )
                                }
                            }
                        }

                        Surface(
                            color = themeColors.surfaceVariant,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = receiptText,
                                modifier = Modifier.padding(10.dp),
                                fontSize = 13.sp,
                                color = themeColors.textSecondary
                            )
                        }

                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Resibo ng Bayad", receiptText))
                                Toast.makeText(context, "Resibo copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColors.primaryAccent,
                                contentColor = themeColors.onPrimaryAccent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("copy_resibo_button")
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy Resibo", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Resibo ng Bayad", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!paymentSaved) {
                Button(
                    onClick = {
                        val amt = amountStr.toDoubleOrNull() ?: 0.0
                        if (amt > 0.0) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val remainingAfter = maxOf(0.0, previousUtang - amt)
                            val defaultNote = if (remainingAfter > 0.0) "Hulog" else "Full"
                            onSavePayment(amt, notes.trim().ifBlank { defaultNote })
                            savedPaidAmount = amt
                            savedRemainingUtang = remainingAfter
                            savedExtraSukli = maxOf(0.0, amt - previousUtang)
                            paymentSaved = true
                        }
                    },
                    enabled = (amountStr.toDoubleOrNull() ?: 0.0) > 0.0,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.paid,
                        contentColor = themeColors.onPaid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("record_payment_button")
                ) {
                    Text("RECORD PAYMENT", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColors.paid,
                        contentColor = themeColors.onPaid
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("bayad_utang_done_button")
                ) {
                    Text("DONE", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (!paymentSaved) {
                TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                    Text("Cancel")
                }
            }
        }
    )
}

