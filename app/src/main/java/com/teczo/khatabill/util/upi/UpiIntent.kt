package com.teczo.khatabill.util.upi

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.teczo.khatabill.R

object UpiIntent {
    /**
     * "Pay with any UPI app": opens the system chooser for the upi://pay link. The result is not read back
     * (UPI intents do not return a reliable status), so the shopkeeper confirms with "Mark as paid".
     */
    fun open(context: Context, link: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            context.startActivity(Intent.createChooser(intent, null))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, R.string.upi_no_app, Toast.LENGTH_LONG).show()
        }
    }
}
