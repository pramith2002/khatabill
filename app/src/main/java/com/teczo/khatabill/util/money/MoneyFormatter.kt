package com.teczo.khatabill.util.money

/**
 * Formats integer paise using Indian digit grouping: 12345600 -> "1,23,456.00".
 * Pure string maths on the Long, so there is no Float/Double rounding anywhere.
 * Negative amounts keep a leading minus: -5 -> "-0.05".
 */
object MoneyFormatter {

    fun format(paise: Long): String {
        val negative = paise < 0
        // Strip the sign from the decimal string so Long.MIN_VALUE needs no special case.
        val digits = paise.toString().removePrefix("-").padStart(3, '0')
        val rupees = digits.dropLast(2)
        val fraction = digits.takeLast(2)
        return (if (negative) "-" else "") + groupIndian(rupees) + "." + fraction
    }

    /** Last three digits form one group, every group before that has two digits. */
    private fun groupIndian(rupees: String): String {
        if (rupees.length <= 3) return rupees
        val head = rupees.dropLast(3)
        val tail = rupees.takeLast(3)
        val groups = head.reversed().chunked(2).map { it.reversed() }.reversed()
        return groups.joinToString(",") + "," + tail
    }
}
