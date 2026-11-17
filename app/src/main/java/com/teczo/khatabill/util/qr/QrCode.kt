package com.teczo.khatabill.util.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

object QrCode {
    /**
     * The QR code as a square grid of modules (true = dark), with no quiet zone: callers draw their own
     * white border. Error correction M handles a UPI link comfortably.
     */
    fun modules(content: String): Array<BooleanArray> {
        val hints = mapOf(
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to 0,
        )
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        return Array(matrix.height) { row -> BooleanArray(matrix.width) { col -> matrix[col, row] } }
    }
}
