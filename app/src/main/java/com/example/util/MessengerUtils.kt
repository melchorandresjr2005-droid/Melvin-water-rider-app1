package com.example.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale

object MessengerUtils {
    private const val MESSENGER_PACKAGE = "com.facebook.orca"

    fun sanitizeMessengerUsername(rawInput: String): String {
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) return ""
        return trimmed
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("m.me/")
            .removePrefix("messenger.com/t/")
            .removePrefix("@")
            .trim('/')
    }

    fun buildBossMessengerUri(rawInput: String): Uri? {
        val handle = sanitizeMessengerUsername(rawInput)
        return if (handle.isNotBlank()) Uri.parse("https://m.me/$handle") else null
    }

    fun buildUtangMessengerReminder(customerName: String, remainingBalance: Double): String {
        val safeName = customerName.trim().ifBlank { "Ma'am/Sir" }
        return String.format(
            Locale.US,
            "Magandang araw po %s! Paalala lang po sana sa natitirang balanse ninyo sa tubig na ₱%,.2f. Maraming salamat po!",
            safeName,
            remainingBalance
        )
    }

    fun openBossMessengerChat(
        context: Context,
        messengerUsername: String,
        phoneNumber: String
    ) {
        val messengerUri = buildBossMessengerUri(messengerUsername)
        if (messengerUri != null) {
            val orcaIntent = Intent(Intent.ACTION_VIEW, messengerUri).apply {
                setPackage(MESSENGER_PACKAGE)
            }
            try {
                context.startActivity(orcaIntent)
                return
            } catch (_: ActivityNotFoundException) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, messengerUri))
                    return
                } catch (_: ActivityNotFoundException) {
                    Toast.makeText(
                        context,
                        "No Messenger or browser app available to open $messengerUri",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }
            }
        }

        val cleanPhone = phoneNumber.trim()
        if (cleanPhone.isNotBlank()) {
            val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$cleanPhone"))
            try {
                context.startActivity(smsIntent)
                return
            } catch (_: ActivityNotFoundException) {
            }
        }

        Toast.makeText(
            context,
            "I-set muna ang Boss Messenger username sa Sahod & Settings.",
            Toast.LENGTH_SHORT
        ).show()
    }

    fun openCustomerUtangMessenger(
        context: Context,
        customerName: String,
        remainingBalance: Double
    ) {
        val reminderText = buildUtangMessengerReminder(customerName, remainingBalance)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("Utang Reminder", reminderText))
        Toast.makeText(context, "Reminder copied! Opening Messenger...", Toast.LENGTH_SHORT).show()

        val orcaSendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, reminderText)
            setPackage(MESSENGER_PACKAGE)
        }
        try {
            context.startActivity(orcaSendIntent)
        } catch (_: ActivityNotFoundException) {
            val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, reminderText)
            }
            try {
                context.startActivity(
                    Intent.createChooser(fallbackIntent, "Send Utang Reminder via Messenger")
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(
                    context,
                    "Reminder copied to clipboard!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
