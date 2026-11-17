package com.teczo.khatabill.util.money

/**
 * Converts what the user types into stored integers, using string maths only (never Float/Double):
 *  - money:    "123.45" -> 12345 paise
 *  - quantity: "1.250"  -> 1250 thousandths
 *  - percent:  "2.5"    -> 250 basis points
 */
object MoneyInput {
    private const val MAX_WHOLE_DIGITS = 12

    fun parsePaise(text: String): Long? = parseScaled(text, decimals = 2)

    fun parseQty(text: String): Long? = parseScaled(text, decimals = 3)

    fun parsePercentBp(text: String): Int? = parseScaled(text, decimals = 2)?.takeIf { it <= 10_000 }?.toInt()

    fun paiseToText(paise: Long): String = scaledToText(paise, 2)

    fun qtyToText(thousandths: Long): String = scaledToText(thousandths, 3)

    fun bpToText(basisPoints: Int): String = scaledToText(basisPoints.toLong(), 2)

    /** Blank counts as "nothing entered" and gives null; callers decide whether that is allowed. */
    private fun parseScaled(text: String, decimals: Int): Long? {
        val t = text.trim()
        if (t.isEmpty()) return null
        val parts = t.split('.')
        if (parts.size > 2) return null
        val whole = parts[0]
        val fraction = parts.getOrElse(1) { "" }
        if (whole.isEmpty() && fraction.isEmpty()) return null
        if (!whole.all(Char::isDigit) || !fraction.all(Char::isDigit)) return null
        if (whole.length > MAX_WHOLE_DIGITS || fraction.length > decimals) return null
        val scale = pow10(decimals)
        val wholeValue = if (whole.isEmpty()) 0L else whole.toLong()
        val fractionValue = fraction.padEnd(decimals, '0').ifEmpty { "0" }.toLong()
        return wholeValue * scale + fractionValue
    }

    /** 12345 -> "123.45", 12300 -> "123", 12350 -> "123.5" (no trailing zeros). */
    private fun scaledToText(value: Long, decimals: Int): String {
        val negative = value < 0
        val digits = value.toString().removePrefix("-").padStart(decimals + 1, '0')
        val whole = digits.dropLast(decimals)
        val fraction = digits.takeLast(decimals).trimEnd('0')
        return (if (negative) "-" else "") + whole + if (fraction.isEmpty()) "" else ".$fraction"
    }

    private fun pow10(n: Int): Long {
        var r = 1L
        repeat(n) { r *= 10 }
        return r
    }
}
