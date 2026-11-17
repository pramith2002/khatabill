package com.teczo.khatabill.util.qr

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.teczo.khatabill.util.upi.UpiLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Proves the QR actually scans: render the module grid like the screen/PDF does, then decode it with a QR reader. */
class QrCodeTest {
    /** Draws the grid as black/white pixels with a 4-module quiet zone, like the white card on screen. */
    private fun decode(content: String, scale: Int = 6): String {
        val modules = QrCode.modules(content)
        val quiet = 4
        val n = modules.size + 2 * quiet
        val size = n * scale
        val pixels = IntArray(size * size) { -1 } // white
        for (r in modules.indices) for (c in modules.indices) {
            if (!modules[r][c]) continue
            for (dy in 0 until scale) for (dx in 0 until scale) {
                pixels[((r + quiet) * scale + dy) * size + (c + quiet) * scale + dx] = 0xFF000000.toInt()
            }
        }
        val source = RGBLuminanceSource(size, size, pixels)
        return QRCodeReader().decode(BinaryBitmap(HybridBinarizer(source))).text
    }

    @Test fun theUpiLinkRoundTripsThroughTheQr() {
        val link = UpiLink.build("ramesh@okhdfcbank", "Sri Ganesh Agarbatti", 115_700, "SGINV/26-27/0001")
        assertEquals(link, decode(link))
    }

    @Test fun kannadaPayeeNamesSurviveTheQr() {
        val link = UpiLink.build("ramesh@okhdfcbank", "ಶ್ರೀ ಗಣೇಶ ಅಗರಬತ್ತಿ", 50_050, "ಬಿಲ್ 1")
        assertEquals(link, decode(link))
    }

    @Test fun theGridIsSquareAndHasDarkModules() {
        val m = QrCode.modules("upi://pay?pa=a@b&am=1.00")
        assertEquals(m.size, m[0].size)
        assertTrue(m.sumOf { row -> row.count { it } } > 50)
    }

    @Test fun finderPatternsSitInTheThreeCorners() {
        val m = QrCode.modules("hello")
        val last = m.size - 1
        // each finder pattern has a dark module in its outer corner
        assertTrue(m[0][0] && m[0][last] && m[last][0])
    }
}
