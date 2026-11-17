package com.teczo.khatabill.util.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * v1 WhatsApp sharing: open WhatsApp with the message (and the PDF attached) and let the user tap send.
 * No WhatsApp API and no automatic sending; the customer is only messaged when the shopkeeper presses send.
 */
object WhatsAppShare {
    private val packages = listOf("com.whatsapp", "com.whatsapp.w4b")

    /** Digits only; a 10-digit Indian mobile gets the 91 country code. Returns null when there is nothing usable. */
    fun normalize(phone: String?): String? {
        val digits = phone.orEmpty().filter(Char::isDigit)
        return when {
            digits.isEmpty() -> null
            digits.length == 10 -> "91$digits"
            digits.length == 11 && digits.startsWith("0") -> "91" + digits.drop(1)
            else -> digits
        }
    }

    fun installedPackage(context: Context): String? = packages.firstOrNull {
        try {
            context.packageManager.getPackageInfo(it, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun send(context: Context, phone: String?, message: String, pdf: File? = null) {
        val number = normalize(phone)
        val pkg = installedPackage(context)
        if (pdf != null) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdf)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, message)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (pkg != null) {
                    setPackage(pkg)
                    if (number != null) putExtra("jid", "$number@s.whatsapp.net")
                }
            }
            context.startActivity(if (pkg != null) intent else Intent.createChooser(intent, null))
        } else {
            val url = "https://wa.me/${number.orEmpty()}?text=" + Uri.encode(message)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { pkg?.let { setPackage(it) } }
            context.startActivity(intent)
        }
    }

    fun sendSms(context: Context, phone: String, message: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + phone.filter { it.isDigit() || it == '+' })).apply {
            putExtra("sms_body", message)
        }
        context.startActivity(intent)
    }
}
