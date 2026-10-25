package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

object WhatsAppUtil {

    /**
     * Builds check-in message for Boss notification.
     * Simple format requested: "[Employee Name] coming"
     * Example: "boms coming"
     */
    fun buildCheckInMessage(
        employeeName: String,
        checkInTime: String = "",
        date: String = "",
        isArabic: Boolean = false
    ): String {
        val cleanName = employeeName.trim().ifBlank { "Employee" }
        return "$cleanName coming"
    }

    /**
     * Builds check-out message for Boss notification.
     * Simple format requested: "[Employee Name] out"
     * Example: "boms out"
     */
    fun buildCheckOutMessage(
        employeeName: String,
        checkOutTime: String = "",
        date: String = "",
        isArabic: Boolean = false
    ): String {
        val cleanName = employeeName.trim().ifBlank { "Employee" }
        return "$cleanName out"
    }

    /**
     * Cleans phone number by removing spaces, dashes, parentheses, and optional plus sign.
     * Also normalizes Saudi Arabia mobile numbers (e.g., 05xxxxxxxx -> 9665xxxxxxxx).
     */
    fun cleanPhoneNumber(rawPhone: String): String {
        var digits = rawPhone
            .replace("+", "")
            .replace(" ", "")
            .replace("-", "")
            .replace("(", "")
            .replace(")", "")
            .replace(".", "")
            .trim()

        if (digits.startsWith("05") && digits.length == 10) {
            digits = "966" + digits.substring(1)
        } else if (digits.startsWith("5") && digits.length == 9) {
            digits = "966$digits"
        }
        return digits
    }

    /**
     * Constructs the WhatsApp URL for a given phone number and message.
     */
    fun buildWhatsAppUrl(phoneNumber: String, message: String): String {
        val cleanNumber = cleanPhoneNumber(phoneNumber)
        val encodedMessage = try {
            URLEncoder.encode(message, "UTF-8")
        } catch (_: Exception) {
            Uri.encode(message)
        }
        return if (cleanNumber.isNotBlank()) {
            "https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedMessage"
        } else {
            "https://api.whatsapp.com/send?text=$encodedMessage"
        }
    }

    /**
     * Opens WhatsApp with prefilled message to the boss's phone number.
     * Manual send: WhatsApp opens to the chat with the message populated,
     * allowing the admin to tap Send.
     */
    fun openWhatsApp(
        context: Context,
        phoneNumber: String,
        message: String
    ): Boolean {
        val url = buildWhatsAppUrl(phoneNumber, message)
        val uri = Uri.parse(url)

        val directAppIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.whatsapp")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val generalIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        return try {
            if (context.packageManager.resolveActivity(directAppIntent, 0) != null) {
                context.startActivity(directAppIntent)
                true
            } else {
                context.startActivity(generalIntent)
                true
            }
        } catch (e: Exception) {
            try {
                context.startActivity(generalIntent)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }
}
