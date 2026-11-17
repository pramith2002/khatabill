package com.teczo.khatabill.domain.gst

import com.teczo.khatabill.domain.model.GstScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class GstCalculatorTest {

    private val karnataka = "29"
    private val maharashtra = "27"

    private fun line(
        rate: Long,
        bp: Int,
        qty: Long = 1000,
        inclusive: Boolean = false,
        treatment: TaxTreatment = TaxTreatment.TAXABLE,
        discount: Discount = Discount.None,
    ) = GstLineInput(qty, rate, inclusive, bp, treatment, discount)

    private fun invoice(
        vararg lines: GstLineInput,
        place: String = karnataka,
        scheme: GstScheme = GstScheme.REGULAR,
        overall: Discount = Discount.None,
        charges: Long = 0,
        round: Boolean = true,
    ) = GstCalculator.calculate(
        GstInvoiceInput(lines.toList(), karnataka, place, scheme, overall, charges, round),
    )

    // ---- exclusive prices ----

    @Test fun exclusiveIntraState() {
        val r = invoice(line(10_000, 1800))
        val l = r.lines.single()
        assertEquals(10_000, l.taxableValue)
        assertEquals(900, l.cgst)
        assertEquals(900, l.sgst)
        assertEquals(0, l.igst)
        assertEquals(11_800, l.lineTotal)
        assertEquals(11_800, r.grandTotal)
        assertEquals(0, r.roundOff)
        assertEquals(SupplyType.INTRA_STATE, r.supply)
        assertTrue(!r.billOfSupply)
    }

    @Test fun exclusiveInterState() {
        val r = invoice(line(10_000, 1800), place = maharashtra)
        val l = r.lines.single()
        assertEquals(1_800, l.igst)
        assertEquals(0, l.cgst)
        assertEquals(0, l.sgst)
        assertEquals(11_800, r.grandTotal)
        assertEquals(SupplyType.INTER_STATE, r.supply)
    }

    @Test fun blankPlaceOfSupplyIsIntraState() {
        val r = invoice(line(10_000, 1800), place = "")
        assertEquals(SupplyType.INTRA_STATE, r.supply)
        assertEquals(900, r.cgst)
    }

    @Test fun weightQuantityInThousandths() {
        // 1.250 kg at 200.00 per kg, 5%: gross 250.00, tax 12.50 -> 262.50 rounds up to 263.00
        val r = invoice(line(rate = 20_000, bp = 500, qty = 1_250))
        assertEquals(25_000, r.taxableTotal)
        assertEquals(625, r.cgst)
        assertEquals(625, r.sgst)
        assertEquals(50, r.roundOff)
        assertEquals(26_300, r.grandTotal)
    }

    @Test fun roundOffDownwards() {
        val r = invoice(line(10_040, 0, treatment = TaxTreatment.EXEMPT))
        assertEquals(-40, r.roundOff)
        assertEquals(10_000, r.grandTotal)
    }

    @Test fun roundOffCanBeDisabled() {
        val r = invoice(line(10_040, 0, treatment = TaxTreatment.EXEMPT), round = false)
        assertEquals(0, r.roundOff)
        assertEquals(10_040, r.grandTotal)
    }

    @Test fun oddPaiseTaxRoundsHalfUp() {
        // taxable 3.33 at 5%: cgst = 0.08325 rupees -> 8 paise each, total 3.49 -> 3.00
        val r = invoice(line(333, 500))
        assertEquals(8, r.cgst)
        assertEquals(8, r.sgst)
        assertEquals(-49, r.roundOff)
        assertEquals(300, r.grandTotal)
    }

    @Test fun zeroRateItemIsAllowed() {
        val r = invoice(line(0, 1800))
        assertEquals(0, r.grandTotal)
    }

    // ---- inclusive prices ----

    @Test fun inclusiveExactBackOut() {
        val l = invoice(line(11_800, 1800, inclusive = true)).lines.single()
        assertEquals(10_000, l.taxableValue)
        assertEquals(900, l.cgst)
        assertEquals(900, l.sgst)
        assertEquals(11_800, l.lineTotal)
    }

    @Test fun inclusiveOddTaxPaisaKeepsCgstEqualToSgstAndTotalUnchanged() {
        // 100.00 inclusive at 18%: tax 15.25 is odd, so taxable absorbs the extra paisa
        val l = invoice(line(10_000, 1800, inclusive = true)).lines.single()
        assertEquals(762, l.cgst)
        assertEquals(762, l.sgst)
        assertEquals(8_476, l.taxableValue)
        assertEquals(10_000, l.lineTotal)
    }

    @Test fun inclusiveInterState() {
        val l = invoice(line(10_000, 1800, inclusive = true), place = maharashtra).lines.single()
        assertEquals(1_525, l.igst)
        assertEquals(8_475, l.taxableValue)
        assertEquals(10_000, l.lineTotal)
    }

    @Test fun inclusiveWithPercentDiscount() {
        // 118.00 inclusive, 10% off -> 106.20 payable; taxable 90.00 + 16.20 tax
        val r = invoice(line(11_800, 1800, inclusive = true, discount = Discount.Percent(1000)))
        val l = r.lines.single()
        assertEquals(9_000, l.taxableValue)
        assertEquals(810, l.cgst)
        assertEquals(10_000, l.subtotal)
        assertEquals(1_000, l.discountAmount)
        assertEquals(10_620, l.lineTotal)
        assertEquals(-20, r.roundOff)
        assertEquals(10_600, r.grandTotal)
    }

    // ---- discounts ----

    @Test fun linePercentDiscountComesBeforeTax() {
        // 2 x 100.00 = 200.00, 10% off = 180.00, tax 18% = 32.40
        val r = invoice(line(10_000, 1800, qty = 2_000, discount = Discount.Percent(1000)))
        val l = r.lines.single()
        assertEquals(20_000, l.subtotal)
        assertEquals(2_000, l.discountAmount)
        assertEquals(18_000, l.taxableValue)
        assertEquals(1_620, l.cgst)
        assertEquals(21_240, l.lineTotal)
        assertEquals(21_200, r.grandTotal)
    }

    @Test fun lineAmountDiscountInterState() {
        val r = invoice(line(10_000, 1200, discount = Discount.Amount(500)), place = maharashtra)
        val l = r.lines.single()
        assertEquals(9_500, l.taxableValue)
        assertEquals(1_140, l.igst)
        assertEquals(10_640, l.lineTotal)
        assertEquals(10_600, r.grandTotal)
    }

    @Test fun fullDiscountGivesZero() {
        val r = invoice(line(10_000, 1800, discount = Discount.Percent(10_000)))
        assertEquals(0, r.taxableTotal)
        assertEquals(0, r.grandTotal)
    }

    @Test fun overallPercentDiscountAcrossMixedRates() {
        val r = invoice(
            line(10_000, 500),
            line(30_000, 1800),
            overall = Discount.Percent(1000),
        )
        assertEquals(40_000, r.subtotal)
        assertEquals(4_000, r.discountTotal)
        assertEquals(9_000, r.lines[0].taxableValue)
        assertEquals(27_000, r.lines[1].taxableValue)
        assertEquals(36_000, r.taxableTotal)
        assertEquals(2_655, r.cgst)
        assertEquals(2_655, r.sgst)
        assertEquals(-10, r.roundOff)
        assertEquals(41_300, r.grandTotal)
    }

    @Test fun overallAmountDiscountSharesAddUpExactly() {
        val exempt = TaxTreatment.EXEMPT
        val r = invoice(
            line(1_000, 0, treatment = exempt),
            line(1_000, 0, treatment = exempt),
            line(1_000, 0, treatment = exempt),
            overall = Discount.Amount(100),
        )
        assertEquals(listOf(966L, 967L, 967L), r.lines.map { it.taxableValue })
        assertEquals(100, r.discountTotal)
        assertEquals(2_900, r.grandTotal)
    }

    @Test fun discountMoreThanLineAmountIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            invoice(line(10_000, 1800, discount = Discount.Amount(10_001)))
        }
    }

    @Test fun overallDiscountMoreThanTotalIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            invoice(line(10_000, 1800), overall = Discount.Amount(10_001))
        }
    }

    // ---- schemes and tax treatments ----

    @Test fun compositionSchemeChargesNoTax() {
        val r = invoice(line(10_000, 1800), scheme = GstScheme.COMPOSITION)
        assertEquals(10_000, r.taxableTotal)
        assertEquals(0, r.cgst + r.sgst + r.igst)
        assertEquals(10_000, r.grandTotal)
        assertEquals(SupplyType.NONE, r.supply)
        assertTrue(r.billOfSupply)
    }

    @Test fun compositionKeepsInclusivePriceAsEntered() {
        val r = invoice(line(11_800, 1800, inclusive = true), scheme = GstScheme.COMPOSITION)
        assertEquals(11_800, r.lines.single().taxableValue)
        assertEquals(11_800, r.grandTotal)
    }

    @Test fun unregisteredBusinessIssuesBillOfSupply() {
        val r = invoice(line(10_000, 1800), scheme = GstScheme.UNREGISTERED, place = maharashtra)
        assertTrue(r.billOfSupply)
        assertEquals(0, r.igst)
        assertEquals(10_000, r.grandTotal)
    }

    @Test fun exemptAndZeroRatedItemsChargeNoTax() {
        val r = invoice(
            line(10_000, 1800, treatment = TaxTreatment.EXEMPT),
            line(5_000, 1800, treatment = TaxTreatment.ZERO_RATED),
        )
        assertEquals(15_000, r.grandTotal)
        assertEquals(0, r.cgst + r.sgst + r.igst)
        assertEquals(TaxTreatment.EXEMPT, r.lines[0].treatment)
        assertEquals(TaxTreatment.ZERO_RATED, r.lines[1].treatment)
    }

    @Test fun taxableAndExemptLinesMixOnOneInvoice() {
        val r = invoice(line(10_000, 1800), line(5_000, 0, treatment = TaxTreatment.EXEMPT))
        assertEquals(15_000, r.taxableTotal)
        assertEquals(900, r.cgst)
        assertEquals(900, r.sgst)
        assertEquals(16_800, r.grandTotal)
    }

    @Test fun otherChargesAreAddedWithoutTax() {
        val r = invoice(line(10_000, 0, treatment = TaxTreatment.EXEMPT), charges = 5_000)
        assertEquals(5_000, r.otherCharges)
        assertEquals(15_000, r.grandTotal)
    }

    // ---- Phase 1 acceptance: 3 items at mixed rates, hand-calculated ----

    private fun threeItems() = arrayOf(
        line(10_000, 500),   // 100.00 at 5%  -> tax 5.00
        line(20_000, 1200),  // 200.00 at 12% -> tax 24.00
        line(30_000, 1800),  // 300.00 at 18% -> tax 54.00
    )

    @Test fun threeItemsMixedRatesIntraStateGiveCgstAndSgst() {
        val r = invoice(*threeItems())
        assertEquals(60_000, r.taxableTotal)
        assertEquals(listOf(250L, 1_200L, 2_700L), r.lines.map { it.cgst })
        assertEquals(4_150, r.cgst)
        assertEquals(4_150, r.sgst)
        assertEquals(0, r.igst)
        assertEquals(68_300, r.grandTotal)
        assertEquals(0, r.roundOff)
    }

    @Test fun threeItemsMixedRatesInterStateGiveIgst() {
        val r = invoice(*threeItems(), place = maharashtra)
        assertEquals(listOf(500L, 2_400L, 5_400L), r.lines.map { it.igst })
        assertEquals(8_300, r.igst)
        assertEquals(0, r.cgst + r.sgst)
        assertEquals(68_300, r.grandTotal)
    }

    // ---- invalid input ----

    @Test fun emptyInvoiceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            GstCalculator.calculate(GstInvoiceInput(emptyList(), karnataka, karnataka, GstScheme.REGULAR))
        }
    }

    @Test fun zeroQuantityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { invoice(line(10_000, 1800, qty = 0)) }
    }

    @Test fun gstRateAbove100PercentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) { invoice(line(10_000, 10_001)) }
    }

    // ---- invariants over many random invoices ----

    @Test fun invariantsHoldForRandomInvoices() {
        val random = Random(42)
        val rates = listOf(0, 250, 500, 1200, 1800, 2800)
        repeat(500) {
            val lines = (1..(1 + random.nextInt(5))).map {
                val qty = 1L + random.nextInt(20_000)
                val rate = random.nextInt(500_000).toLong()
                val gross = GstCalculator.mulDiv(qty, rate, 1000)
                val discount = when (random.nextInt(3)) {
                    0 -> Discount.None
                    1 -> Discount.Percent(random.nextInt(5_001))
                    else -> Discount.Amount(if (gross == 0L) 0 else random.nextInt(gross.toInt().coerceAtLeast(1)).toLong())
                }
                GstLineInput(qty, rate, random.nextBoolean(), rates[random.nextInt(rates.size)], discount = discount)
            }
            val overall = if (random.nextBoolean()) Discount.None else Discount.Percent(random.nextInt(3_001))
            val place = if (random.nextBoolean()) karnataka else maharashtra
            val r = invoice(*lines.toTypedArray(), place = place, overall = overall, charges = random.nextInt(10_000).toLong())

            assertEquals(r.subtotal - r.discountTotal, r.taxableTotal)
            assertEquals(r.taxableTotal + r.cgst + r.sgst + r.igst + r.otherCharges + r.roundOff, r.grandTotal)
            assertEquals(0, r.grandTotal % 100)
            assertEquals(r.cgst, r.sgst)
            assertEquals(r.taxableTotal, r.lines.sumOf { it.taxableValue })
            r.lines.forEach { l ->
                assertEquals(l.subtotal - l.discountAmount, l.taxableValue)
                assertEquals(l.taxableValue + l.cgst + l.sgst + l.igst, l.lineTotal)
                assertTrue(l.taxableValue >= 0)
            }
        }
    }
}
