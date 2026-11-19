package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AdvanceRemitEntity
import com.example.ui.theme.WaterRiderTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AdvanceRemitDialog(
    existingAdvanceRemit: AdvanceRemitEntity? = null,
    onDismiss: () -> Unit,
    onSave: (amount: Double, notes: String) -> Unit,
    onDelete: ((AdvanceRemitEntity) -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    val themeColors = WaterRiderTheme.colors
    var amountStr by remember(existingAdvanceRemit?.id) {
        mutableStateOf(existingAdvanceRemit?.amount?.toString() ?: "")
    }
    var notes by remember(existingAdvanceRemit?.id) {
        mutableStateOf(existingAdvanceRemit?.notes ?: "")
    }

    val timePreview = remember(existingAdvanceRemit?.timestamp) {
        SimpleDateFormat("h:mm a", Locale.getDefault()).format(
            Date(existingAdvanceRemit?.timestamp ?: System.currentTimeMillis())
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = if (existingAdvanceRemit == null) "Advance Remit (Pangtanghali)" else "Edit Advance Remit",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Oras: $timePreview • Ibawas sa Natitirang Cash to Remit",
                    fontSize = 12.sp,
                    color = themeColors.textSecondary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Advance Remit Amount (₱)") },
                    placeholder = { Text("0.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_remit_amount_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (e.g., Pangtanghali, Kay Boss)") },
                    placeholder = { Text("Optional note") },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        keyboardType = KeyboardType.Text
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("advance_remit_notes_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                if (existingAdvanceRemit != null && onDelete != null) {
                    OutlinedButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onDelete(existingAdvanceRemit)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = themeColors.utang),
                        border = BorderStroke(1.dp, themeColors.utang),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_advance_remit_button")
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete Advance Remit", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete Advance Remit", fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        confirmButton = {
            val parsedAmount = amountStr.toDoubleOrNull() ?: 0.0
            Button(
                onClick = {
                    if (parsedAmount > 0.0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(parsedAmount, notes.trim())
                    }
                },
                enabled = parsedAmount > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (existingAdvanceRemit == null) themeColors.primaryAccent else themeColors.paid,
                    contentColor = if (existingAdvanceRemit == null) themeColors.onPrimaryAccent else themeColors.onPaid
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .height(48.dp)
                    .testTag("save_advance_remit_button")
            ) {
                Text(if (existingAdvanceRemit == null) "SAVE ADVANCE" else "UPDATE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.height(48.dp)) {
                Text("Cancel")
            }
        }
    )
}
