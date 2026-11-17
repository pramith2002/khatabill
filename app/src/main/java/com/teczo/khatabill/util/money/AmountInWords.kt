package com.teczo.khatabill.util.money

/**
 * Indian-system amount in English words, for the printed bill:
 * 12345650 paise -> "Rupees One Lakh Twenty Three Thousand Four Hundred Fifty Six and Fifty Paise Only".
 * Kannada words are not included yet: they need a table checked by a native speaker.
 */
object AmountInWords {
    private val ones = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine", "Ten", "Eleven", "Twelve",
        "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen",
    )
    private val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")

    fun english(paise: Long): String {
        require(paise >= 0) { "Amount cannot be negative" }
        val rupees = paise / 100
        val fraction = (paise % 100).toInt()
        val rupeeWords = if (rupees == 0L) "Zero" else words(rupees)
        val sb = StringBuilder("Rupees ").append(rupeeWords)
        if (fraction > 0) sb.append(" and ").append(below100(fraction)).append(" Paise")
        return sb.append(" Only").toString()
    }

    private fun below100(n: Int): String =
        if (n < 20) ones[n] else tens[n / 10] + if (n % 10 == 0) "" else " " + ones[n % 10]

    private fun below1000(n: Int): String {
        val hundreds = n / 100
        val rest = n % 100
        val parts = mutableListOf<String>()
        if (hundreds > 0) parts.add(ones[hundreds] + " Hundred")
        if (rest > 0) parts.add(below100(rest))
        return parts.joinToString(" ")
    }

    /** Crore / Lakh / Thousand / Hundred grouping (the crore part may itself be larger than 99). */
    private fun words(n: Long): String {
        val parts = mutableListOf<String>()
        var rest = n
        val crore = rest / 10_000_000
        rest %= 10_000_000
        if (crore > 0) parts.add(words(crore) + " Crore")
        val lakh = (rest / 100_000).toInt()
        rest %= 100_000
        if (lakh > 0) parts.add(below100(lakh) + " Lakh")
        val thousand = (rest / 1_000).toInt()
        rest %= 1_000
        if (thousand > 0) parts.add(below100(thousand) + " Thousand")
        if (rest > 0) parts.add(below1000(rest.toInt()))
        return parts.joinToString(" ")
    }
}
