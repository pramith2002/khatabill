package com.teczo.khatabill.domain.report

import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.util.export.CsvExport
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The "GST summary matches a hand-calculated sample of 20 invoices" check.
 *
 * Invoice i (1..20) has ONE line: taxable = 100.00 x i, qty 1.
 *   rate  : i%3==1 -> 5%, i%3==2 -> 12%, otherwise 18%
 *   place : odd i -> Karnataka (29, CGST+SGST), even i -> Maharashtra (27, IGST)
 *   GSTIN : invoices 1..10 are B2B, 11..20 are B2C
 * All tax amounts are exact whole paise, so the hand totals below have no rounding in them.
 */
class ReportsTest {
    private val walkIn = "Walk-in"

    private fun rate(i: Int) = when (i % 3) { 1 -> 500; 2 -> 1200; else -> 1800 }

    private fun taxable(i: Int) = 10_000L * i

    private fun halfTax(i: Int) = taxable(i) * rate(i) / 20_000 // cgst = sgst (odd i)
    private fun igst(i: Int) = taxable(i) * rate(i) / 10_000 // even i

    private fun bill(i: Int, type: DocumentType = DocumentType.SALE, status: InvoiceStatus = InvoiceStatus.UNPAID, gstin: String? = if (i <= 10) "29AAGCB7383J1Z4" else null): InvoiceEntity {
        val intra = i % 2 == 1
        val cgst = if (intra) halfTax(i) else 0L
        val igst = if (intra) 0L else igst(i)
        return InvoiceEntity(
            id = "inv$i", businessId = "b", createdAt = i.toLong(), updatedAt = 0, type = type, series = "INV", financialYear = "2026-27",
            sequenceNo = i, number = "INV/$i", date = 100L + i, partyId = "p$i", partyName = "Party $i", partyGstin = gstin,
            placeOfSupplyStateCode = if (intra) "29" else "27", subtotal = taxable(i), discountTotal = 0, taxableTotal = taxable(i),
            cgst = cgst, sgst = cgst, igst = igst, grandTotal = taxable(i) + 2 * cgst + igst, status = status,
        )
    }

    private fun line(i: Int, invoiceId: String = "inv$i"): InvoiceLineEntity {
        val intra = i % 2 == 1
        val cgst = if (intra) halfTax(i) else 0L
        val igst = if (intra) 0L else igst(i)
        return InvoiceLineEntity(
            id = "l$i", businessId = "b", createdAt = 0, updatedAt = 0, invoiceId = invoiceId, lineNo = 1, nameSnapshot = "Agarbatti",
            hsnSac = "3307", qty = 1_000, unit = "kg", rate = taxable(i), taxableValue = taxable(i), gstRateBp = rate(i),
            cgst = cgst, sgst = cgst, igst = igst, lineTotal = taxable(i) + 2 * cgst + igst,
        )
    }

    private val invoices = (1..20).map { bill(it) }
    private val lines = (1..20).map { line(it) }

    private fun money(paise: Long) = Cells.plain(paise)
    private fun List<Cell>.raws() = map { it.raw }

    // ---- hand-calculated totals for the 20 invoices ----
    // taxable 2,100,000 | CGST 54,100 | SGST 54,100 | IGST 132,600 | total tax 240,800 | grand total 2,340,800 (paise)

    @Test fun salesReportTotalsMatchTheHandCalculation() {
        val t = Reports.sales(invoices, walkIn)
        assertEquals(20, t.rows.size)
        assertEquals(money(2_100_000), t.total!![3].raw)
        assertEquals(money(240_800), t.total!![4].raw)
        assertEquals(money(2_340_800), t.total!![5].raw)
    }

    @Test fun b2bSummaryMatchesTheHandCalculation() {
        val t = Reports.gstr1B2b(invoices)
        assertEquals(10, t.rows.size)
        // taxable 550,000 | CGST 15,800 | SGST 15,800 | IGST 29,800 | total 611,400
        assertEquals(listOf("", "", "", "", money(550_000), money(15_800), money(15_800), money(29_800), money(611_400)).drop(4), t.total!!.raws().drop(4))
    }

    @Test fun b2cSummaryIsGroupedByStateAndRate() {
        val t = Reports.gstr1B2c(invoices, lines)
        val rows = t.rows.associate { (it[0].text + "@" + it[1].raw) to it.drop(2).raws() }
        assertEquals(6, rows.size)
        assertEquals(listOf(money(320_000), money(8_000), money(8_000), money(0)), rows["29@5"])
        assertEquals(listOf(money(280_000), money(16_800), money(16_800), money(0)), rows["29@12"])
        assertEquals(listOf(money(150_000), money(13_500), money(13_500), money(0)), rows["29@18"])
        assertEquals(listOf(money(160_000), money(0), money(0), money(8_000)), rows["27@5"])
        assertEquals(listOf(money(340_000), money(0), money(0), money(40_800)), rows["27@12"])
        assertEquals(listOf(money(300_000), money(0), money(0), money(54_000)), rows["27@18"])
        // taxable 1,550,000 | CGST 38,300 | SGST 38,300 | IGST 102,800
        assertEquals(listOf(money(1_550_000), money(38_300), money(38_300), money(102_800)), t.total!!.drop(2).raws())
    }

    @Test fun b2bAndB2cTogetherAreEverySale() {
        val b2b = Reports.gstr1B2b(invoices).total!!
        val b2c = Reports.gstr1B2c(invoices, lines).total!!
        // taxable: B2B col 4, B2C col 2
        assertEquals(money(2_100_000), money(plain(b2b[4]) + plain(b2c[2])))
    }

    private fun plain(c: Cell): Long = c.raw.replace(".", "").toLong()

    @Test fun hsnSummaryGroupsByRate() {
        val t = Reports.gstr1Hsn(invoices, lines)
        val rows = t.rows.associate { it[1].raw to it.drop(3).raws() } // rate -> qty, taxable, cgst, sgst, igst
        assertEquals(3, rows.size)
        assertEquals(listOf("7", money(700_000), money(10_000), money(10_000), money(15_000)), rows["5"]!!.let { listOf(it[0], it[1], it[2], it[3], it[4]) })
        assertEquals(listOf("7", money(770_000), money(19_800), money(19_800), money(52_800)), rows["12"]!!.let { listOf(it[0], it[1], it[2], it[3], it[4]) })
        assertEquals(listOf("6", money(630_000), money(24_300), money(24_300), money(64_800)), rows["18"]!!.let { listOf(it[0], it[1], it[2], it[3], it[4]) })
    }

    @Test fun taxSummaryNetsOutInputTaxFromPurchases() {
        // purchases: 400,000 taxable intra-state at 18% (CGST 36,000 + SGST 36,000) and 100,000 inter-state at 12% (IGST 12,000)
        val p1 = bill(1, DocumentType.PURCHASE).copy(id = "pur1", taxableTotal = 400_000, subtotal = 400_000, cgst = 36_000, sgst = 36_000, igst = 0, grandTotal = 472_000)
        val p2 = bill(2, DocumentType.PURCHASE).copy(id = "pur2", taxableTotal = 100_000, subtotal = 100_000, cgst = 0, sgst = 0, igst = 12_000, grandTotal = 112_000)
        val t = Reports.gstr3b(invoices + p1 + p2)
        val out = t.rows[0].raws()
        val input = t.rows[1].raws()
        val net = t.total!!.raws()
        assertEquals(listOf(money(2_100_000), money(132_600), money(54_100), money(54_100)), out.drop(1))
        assertEquals(listOf(money(500_000), money(12_000), money(36_000), money(36_000)), input.drop(1))
        assertEquals(listOf(money(120_600), money(18_100), money(18_100)), net.drop(2))
    }

    @Test fun cancelledBillsAreLeftOutOfEverySummary() {
        val withCancelled = invoices + bill(20).copy(id = "dead", status = InvoiceStatus.CANCELLED, taxableTotal = 9_999_999, grandTotal = 9_999_999)
        assertEquals(money(2_100_000), Reports.sales(withCancelled, walkIn).total!![3].raw)
        assertEquals(20, Reports.sales(withCancelled, walkIn).rows.size)
    }

    @Test fun aSaleReturnReducesTheSalesAndTheTaxSummary() {
        val ret = bill(3, DocumentType.SALE_RETURN, gstin = null).copy(id = "ret")
        val t = Reports.gstr3b(invoices + ret)
        // return of bill 3: taxable 30,000, CGST 2,700, SGST 2,700 comes off the outward figures
        assertEquals(listOf(money(2_070_000), money(132_600), money(51_400), money(51_400)), t.rows[0].raws().drop(1))
        assertEquals(money(2_340_800 - 35_400), Reports.sales(invoices + ret, walkIn).total!![5].raw)
    }

    // ---- other reports ----

    @Test fun partyWiseListsEachPartyBiggestFirst() {
        val t = Reports.partyWise(invoices, walkIn)
        assertEquals("Party 20", t.rows.first()[0].text)
        assertEquals(20, t.rows.size)
        assertEquals(money(2_340_800), t.total!![2].raw)
    }

    @Test fun walkInSalesAreGroupedUnderTheWalkInLabel() {
        val a = bill(1).copy(partyName = null)
        val b = bill(2).copy(partyName = null)
        val t = Reports.partyWise(listOf(a, b), walkIn)
        assertEquals(1, t.rows.size)
        assertEquals(walkIn, t.rows.single()[0].text)
        assertEquals("2", t.rows.single()[1].raw)
    }

    @Test fun itemWiseAddsUpQuantitiesAndValues() {
        val t = Reports.itemWise(invoices, lines)
        assertEquals(1, t.rows.size) // every line is "Agarbatti" / kg
        assertEquals("20", t.rows.single()[2].raw)
        assertEquals(money(2_100_000), t.rows.single()[3].raw)
        assertEquals(money(2_340_800), t.total!![4].raw)
    }

    private fun expense(id: String, category: String, amount: Long) =
        ExpenseEntity(id = id, businessId = "b", createdAt = 0, updatedAt = 0, date = 1, category = category, amount = amount, mode = PaymentMode.CASH)

    @Test fun profitAndLossIsTaxableSalesMinusPurchasesMinusExpenses() {
        val purchase = bill(1, DocumentType.PURCHASE).copy(taxableTotal = 400_000)
        val t = Reports.profitAndLoss(invoices + purchase, listOf(expense("e1", "rent", 150_000), expense("e2", "salary", 250_000)))
        // 2,100,000 - 400,000 - 400,000 = 1,300,000
        assertEquals(money(1_300_000), t.total!![1].raw)
        assertEquals(R.string.pl_profit, t.total!![0].res)
    }

    @Test fun profitCanBeNegative() {
        val t = Reports.profitAndLoss(emptyList(), listOf(expense("e", "rent", 500)))
        assertEquals(money(-500), t.total!![1].raw)
    }

    @Test fun expenseReportGroupsByCategory() {
        val t = Reports.expenses(listOf(expense("1", "rent", 100), expense("2", "rent", 50), expense("3", "weird", 10)))
        assertEquals(2, t.rows.size)
        assertEquals(R.string.exp_rent, t.rows.first()[0].res)
        assertEquals(money(150), t.rows.first()[2].raw)
        assertEquals("weird", t.rows.last()[0].text) // unknown categories show as typed
        assertEquals(money(160), t.total!![2].raw)
    }

    private fun item(id: String, name: String, price: Long, min: Long = 0, type: ItemType = ItemType.PRODUCT) =
        ItemEntity(id = id, businessId = "b", createdAt = 0, updatedAt = 0, name = name, unit = "kg", purchasePrice = price, minStock = min, type = type)

    @Test fun stockValueUsesPurchasePriceAndRoundsHalfUp() {
        // 1.250 kg x 99.90 = 124.875 -> 124.88
        assertEquals(12_488, Reports.valueOf(1_250, 9_990))
        val t = Reports.stock(listOf(item("a", "Rose", 9_990), item("s", "Delivery", 100, type = ItemType.SERVICE)), mapOf("a" to 1_250L))
        assertEquals(1, t.rows.size) // services have no stock
        assertEquals(money(12_488), t.total!![4].raw)
    }

    @Test fun lowStockListsItemsAtOrBelowTheirMinimum() {
        val items = listOf(item("a", "Low", 1, min = 10_000), item("b", "Fine", 1, min = 10_000), item("c", "NoLimit", 1, min = 0), item("d", "AtLimit", 1, min = 5_000))
        val t = Reports.lowStock(items, mapOf("a" to 2_000L, "b" to 50_000L, "c" to 0L, "d" to 5_000L))
        assertEquals(listOf("AtLimit", "Low"), t.rows.map { it[0].text })
    }

    // ---- CSV ----

    @Test fun csvEscapesCommasQuotesAndNewlines() {
        assertEquals("plain", CsvExport.escape("plain"))
        assertEquals("\"a,b\"", CsvExport.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", CsvExport.escape("say \"hi\""))
        assertEquals("\"line1\nline2\"", CsvExport.escape("line1\nline2"))
    }

    @Test fun csvHasHeaderRowsAndTotalWithPlainNumbers() {
        val csv = CsvExport.render(listOf(Reports.sales(listOf(bill(1)), walkIn)), { "H$it" })
        val lines = csv.trim().split("\r\n")
        assertEquals(4, lines.size) // title, header, 1 row, total
        assertTrue(lines[2].contains("100.00") && !lines[2].contains(",000")) // 10,000 paise = 100.00, no grouping
        assertTrue(lines[2].endsWith(money(10_000 + 500)))
    }

    @Test fun csvKeepsKannadaText() {
        val inv = bill(1).copy(partyName = "ರಮೇಶ್, ಮೈಸೂರು")
        val csv = CsvExport.render(listOf(Reports.sales(listOf(inv), walkIn)), { "H$it" })
        assertTrue(csv.contains("\"ರಮೇಶ್, ಮೈಸೂರು\""))
    }
}
