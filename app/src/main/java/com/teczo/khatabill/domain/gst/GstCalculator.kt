package com.teczo.khatabill.domain.gst

import com.teczo.khatabill.domain.model.GstScheme
import java.math.BigInteger

/**
 * The single place where invoice tax is calculated. Pure Kotlin, integer maths only.
 *
 * Rules:
 *  1. Per line: gross = qty * rate; line discount comes off first (tax is always on the discounted value).
 *  2. An overall discount is spread across lines pro-rata (largest remainder, so shares add up exactly).
 *  3. Intra-state: CGST = SGST = half the rate. Inter-state: IGST. Blank place of supply counts as intra-state.
 *  4. Tax-inclusive lines: tax is backed out of the amount. If that tax is an odd number of paise, the taxable
 *     value absorbs the odd paisa so CGST always equals SGST and the line still totals what was entered.
 *  5. COMPOSITION and UNREGISTERED businesses charge no tax: amounts are used as entered (bill of supply).
 *  6. EXEMPT and ZERO_RATED items charge no tax.
 *  7. Round-off brings the grand total to the nearest rupee (half rounds up).
 */
object GstCalculator {

    fun calculate(input: GstInvoiceInput): GstInvoiceResult {
        require(input.lines.isNotEmpty()) { "An invoice needs at least one line" }
        require(input.otherCharges >= 0) { "Other charges cannot be negative" }
        input.lines.forEach(::validateLine)

        val chargesTax = input.scheme == GstScheme.REGULAR
        val intra = input.placeOfSupplyStateCode.isBlank() ||
            input.placeOfSupplyStateCode == input.businessStateCode
        val supply = when {
            !chargesTax -> SupplyType.NONE
            intra -> SupplyType.INTRA_STATE
            else -> SupplyType.INTER_STATE
        }

        val gross = input.lines.map { mulDiv(it.qty, it.rate, 1000) }
        val lineDiscounts = input.lines.mapIndexed { i, line -> discountOf(line.discount, gross[i]) }
        val afterLineDiscount = gross.indices.map { gross[it] - lineDiscounts[it] }
        val overall = discountOf(input.overallDiscount, afterLineDiscount.sum())
        val shares = apportion(overall, afterLineDiscount)

        val results = input.lines.mapIndexed { i, line ->
            calculateLine(
                line = line,
                gross = gross[i],
                enteredDiscount = lineDiscounts[i] + shares[i],
                amount = afterLineDiscount[i] - shares[i],
                chargesTax = chargesTax,
                intra = intra,
            )
        }

        val taxableTotal = results.sumOf { it.taxableValue }
        val cgst = results.sumOf { it.cgst }
        val sgst = results.sumOf { it.sgst }
        val igst = results.sumOf { it.igst }
        val beforeRounding = taxableTotal + cgst + sgst + igst + input.otherCharges
        val grandTotal = if (input.roundToRupee) ((beforeRounding + 50) / 100) * 100 else beforeRounding

        return GstInvoiceResult(
            lines = results,
            supply = supply,
            billOfSupply = !chargesTax,
            subtotal = results.sumOf { it.subtotal },
            discountTotal = results.sumOf { it.discountAmount },
            taxableTotal = taxableTotal,
            cgst = cgst,
            sgst = sgst,
            igst = igst,
            otherCharges = input.otherCharges,
            roundOff = grandTotal - beforeRounding,
            grandTotal = grandTotal,
        )
    }

    private fun calculateLine(
        line: GstLineInput,
        gross: Long,
        enteredDiscount: Long,
        amount: Long,
        chargesTax: Boolean,
        intra: Boolean,
    ): GstLineResult {
        val taxed = chargesTax && line.treatment == TaxTreatment.TAXABLE && line.gstRateBp > 0
        if (!taxed) return lineResult(line, gross, gross, enteredDiscount, amount, 0, 0, 0)

        val bp = line.gstRateBp.toLong()
        return if (!line.taxInclusive) {
            if (intra) {
                val half = mulDiv(amount, bp, 20_000)
                lineResult(line, gross, gross, enteredDiscount, amount, half, half, 0)
            } else {
                lineResult(line, gross, gross, enteredDiscount, amount, 0, 0, mulDiv(amount, bp, 10_000))
            }
        } else {
            val tax = amount - mulDiv(amount, 10_000, 10_000 + bp)
            val discountOnTaxable = mulDiv(enteredDiscount, 10_000, 10_000 + bp)
            if (intra) {
                val half = tax / 2
                val taxable = amount - 2 * half
                lineResult(line, gross, taxable + discountOnTaxable, discountOnTaxable, taxable, half, half, 0)
            } else {
                val taxable = amount - tax
                lineResult(line, gross, taxable + discountOnTaxable, discountOnTaxable, taxable, 0, 0, tax)
            }
        }
    }

    private fun lineResult(
        line: GstLineInput,
        gross: Long,
        subtotal: Long,
        discount: Long,
        taxable: Long,
        cgst: Long,
        sgst: Long,
        igst: Long,
    ) = GstLineResult(
        grossAmount = gross,
        subtotal = subtotal,
        discountAmount = discount,
        taxableValue = taxable,
        gstRateBp = line.gstRateBp,
        treatment = line.treatment,
        cgst = cgst,
        sgst = sgst,
        igst = igst,
        lineTotal = taxable + cgst + sgst + igst,
    )

    private fun validateLine(line: GstLineInput) {
        require(line.qty > 0) { "Quantity must be positive" }
        require(line.rate >= 0) { "Rate cannot be negative" }
        require(line.gstRateBp in 0..10_000) { "GST rate must be between 0% and 100%" }
    }

    /** Discount amount for a base value. Never more than the base, never negative. */
    private fun discountOf(discount: Discount, base: Long): Long = when (discount) {
        Discount.None -> 0
        is Discount.Percent -> {
            require(discount.basisPoints in 0..10_000) { "Discount percent must be between 0 and 100" }
            mulDiv(base, discount.basisPoints.toLong(), 10_000)
        }
        is Discount.Amount -> {
            require(discount.paise in 0..base) { "Discount cannot be negative or more than the amount" }
            discount.paise
        }
    }

    /** Splits [total] across [weights] pro-rata so the shares add up exactly (largest remainder method). */
    private fun apportion(total: Long, weights: List<Long>): List<Long> {
        if (total == 0L) return weights.map { 0L }
        val sum = BigInteger.valueOf(weights.sum())
        val bigTotal = BigInteger.valueOf(total)
        val floors = ArrayList<Long>(weights.size)
        val remainders = ArrayList<BigInteger>(weights.size)
        weights.forEach { w ->
            val parts = bigTotal.multiply(BigInteger.valueOf(w)).divideAndRemainder(sum)
            floors.add(parts[0].toLong())
            remainders.add(parts[1])
        }
        var leftover = total - floors.sum()
        val order = weights.indices.sortedWith(
            compareByDescending<Int> { remainders[it] }.thenBy { it },
        )
        for (i in order) {
            if (leftover == 0L) break
            floors[i] = floors[i] + 1
            leftover--
        }
        return floors
    }

    /** Rounded (half up) a * b / c for non-negative values, without overflow. */
    internal fun mulDiv(a: Long, b: Long, c: Long): Long {
        require(c > 0)
        val twice = BigInteger.valueOf(a).multiply(BigInteger.valueOf(b)).shiftLeft(1)
        return twice.add(BigInteger.valueOf(c)).divide(BigInteger.valueOf(c).shiftLeft(1)).longValueExact()
    }
}
