package com.teczo.khatabill.util.upi

/**
 * Builds the standard UPI payment link: upi://pay?pa=<upiId>&pn=<name>&am=<amount>&cu=INR&tn=<note>.
 * The app only builds links; it never sees a PIN or card number. Payment confirmation is manual ("Mark as paid").
 */
object UpiLink {
    fun build(upiId: String, payeeName: String, amountPaise: Long, note: String): String {
        require(upiId.isNotBlank()) { "UPI ID is required" }
        require(amountPaise > 0) { "Amount must be positive" }
        return "upi://pay?pa=" + encode(upiId.trim(), keep = "@") +
            "&pn=" + encode(payeeName.trim()) +
            "&am=" + amountText(amountPaise) +
            "&cu=INR" +
            "&tn=" + encode(note.trim())
    }

    /** "1157.00": always two decimals, from integer paise. */
    fun amountText(paise: Long): String {
        val whole = paise / 100
        val fraction = (paise % 100).toString().padStart(2, '0')
        return "$whole.$fraction"
    }

    /**
     * Percent-encodes everything except the RFC 3986 unreserved characters (and anything in [keep]).
     * Works on UTF-8 bytes, so Kannada names are safe. The UPI id keeps its '@' as UPI apps expect.
     */
    fun encode(text: String, keep: String = ""): String {
        val sb = StringBuilder()
        for (b in text.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            val ch = c.toChar()
            if (ch in 'A'..'Z' || ch in 'a'..'z' || ch in '0'..'9' || ch == '-' || ch == '.' || ch == '_' || ch == '~' || ch in keep) {
                sb.append(ch)
            } else {
                sb.append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 0x0F])
            }
        }
        return sb.toString()
    }
}
