package com.teczo.khatabill.util.scan

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

/**
 * Reads a product barcode with Google's code scanner. It opens its own scanning screen, so the app needs no
 * camera permission and nothing is sent anywhere. It needs Google Play services on the phone; when that is
 * missing (or the scanner module is not downloaded yet) [onError] is called and the user can type the code.
 */
object BarcodeScan {
    private val formats = intArrayOf(
        Barcode.FORMAT_EAN_13,
        Barcode.FORMAT_EAN_8,
        Barcode.FORMAT_UPC_A,
        Barcode.FORMAT_UPC_E,
        Barcode.FORMAT_CODE_128,
        Barcode.FORMAT_CODE_39,
        Barcode.FORMAT_QR_CODE,
    )

    fun start(context: Context, onResult: (String) -> Unit, onError: () -> Unit) {
        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(formats.first(), *formats.drop(1).toIntArray())
            .build()
        GmsBarcodeScanning.getClient(context, options).startScan()
            .addOnSuccessListener { barcode ->
                val code = barcode.rawValue?.trim().orEmpty()
                if (code.isEmpty()) onError() else onResult(code)
            }
            .addOnCanceledListener { /* the user closed the scanner: nothing to do */ }
            .addOnFailureListener { onError() }
    }
}
