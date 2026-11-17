package com.teczo.khatabill.domain.report

import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.InvoiceLineEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.ExpenseCategories
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.ItemType

/**
 * Builds report tables from the data. Every figure is paise, summed exactly. Cancelled and draft bills are
 * left out everywhere. Returns count negatively (a sale return reduces sales).
 */
object Reports {
    private fun live(invoices: List<InvoiceEntity>) = invoices.filter { it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }

    /** +1 for sales and purchases, -1 for their returns, 0 for documents that move no money. */
    private fun sign(type: DocumentType): Int = when (type) {
        DocumentType.SALE, DocumentType.PURCHASE -> 1
        DocumentType.SALE_RETURN, DocumentType.PURCHASE_RETURN -> -1
        else -> 0
    }

    private fun isSales(t: DocumentType) = t == DocumentType.SALE || t == DocumentType.SALE_RETURN
    private fun isPurchases(t: DocumentType) = t == DocumentType.PURCHASE || t == DocumentType.PURCHASE_RETURN

    private fun taxOf(i: InvoiceEntity) = i.cgst + i.sgst + i.igst

    // ---- sales and purchase reports ----

    private fun billList(invoices: List<InvoiceEntity>, pick: (DocumentType) -> Boolean, title: Int, walkIn: String): ReportTable {
        val list = live(invoices).filter { pick(it.type) }.sortedWith(compareBy({ it.date }, { it.sequenceNo }))
        val rows = list.map {
            val s = sign(it.type)
            listOf(Cells.date(it.date), Cells.text(it.number), Cells.text(it.partyName ?: walkIn), Cells.money(s * it.taxableTotal), Cells.money(s * taxOf(it)), Cells.money(s * it.grandTotal))
        }
        val total = listOf(
            Cells.label(R.string.col_total), Cells.text(""), Cells.text(""),
            Cells.money(list.sumOf { sign(it.type) * it.taxableTotal }),
            Cells.money(list.sumOf { sign(it.type) * taxOf(it) }),
            Cells.money(list.sumOf { sign(it.type) * it.grandTotal }),
        )
        return ReportTable(
            title,
            listOf(Col(R.string.col_date), Col(R.string.col_number), Col(R.string.col_party), Col(R.string.col_taxable, true), Col(R.string.col_tax, true), Col(R.string.col_total, true)),
            rows, total,
        )
    }

    fun sales(invoices: List<InvoiceEntity>, walkIn: String) = billList(invoices, ::isSales, R.string.report_sales, walkIn)

    fun purchases(invoices: List<InvoiceEntity>, walkIn: String) = billList(invoices, ::isPurchases, R.string.report_purchases, walkIn)

    fun partyWise(invoices: List<InvoiceEntity>, walkIn: String): ReportTable {
        val groups = live(invoices).filter { isSales(it.type) }.groupBy { it.partyName ?: walkIn }
        val rows = groups.map { (name, list) ->
            Triple(name, list.count { sign(it.type) > 0 }, list.sumOf { sign(it.type) * it.grandTotal })
        }.sortedByDescending { it.third }
        return ReportTable(
            R.string.report_party_wise,
            listOf(Col(R.string.col_party), Col(R.string.col_count, true), Col(R.string.col_total, true)),
            rows.map { listOf(Cells.text(it.first), Cells.count(it.second), Cells.money(it.third)) },
            listOf(Cells.label(R.string.col_total), Cells.count(rows.sumOf { it.second }), Cells.money(rows.sumOf { it.third })),
        )
    }

    fun itemWise(invoices: List<InvoiceEntity>, lines: List<InvoiceLineEntity>): ReportTable {
        val signs = live(invoices).filter { isSales(it.type) }.associate { it.id to sign(it.type) }
        val groups = lines.filter { it.invoiceId in signs }.groupBy { it.nameSnapshot to it.unit }
        val rows = groups.map { (key, ls) ->
            val qty = ls.sumOf { signs.getValue(it.invoiceId) * it.qty }
            val taxable = ls.sumOf { signs.getValue(it.invoiceId) * it.taxableValue }
            val total = ls.sumOf { signs.getValue(it.invoiceId) * it.lineTotal }
            listOf(Cells.text(key.first), Cells.text(key.second), Cells.qty(qty), Cells.money(taxable), Cells.money(total)) to total
        }.sortedByDescending { it.second }
        return ReportTable(
            R.string.report_item_wise,
            listOf(Col(R.string.col_item), Col(R.string.col_unit), Col(R.string.col_qty, true), Col(R.string.col_taxable, true), Col(R.string.col_total, true)),
            rows.map { it.first },
            listOf(Cells.label(R.string.col_total), Cells.text(""), Cells.text(""), Cells.text(""), Cells.money(rows.sumOf { it.second })),
        )
    }

    // ---- profit and loss, expenses, stock ----

    /** Simple profit and loss: taxable sales less taxable purchases less expenses (tax is not income or cost). */
    fun profitAndLoss(invoices: List<InvoiceEntity>, expenses: List<ExpenseEntity>): ReportTable {
        val bills = live(invoices)
        val sales = bills.filter { it.type == DocumentType.SALE }.sumOf { it.taxableTotal }
        val saleReturns = bills.filter { it.type == DocumentType.SALE_RETURN }.sumOf { it.taxableTotal }
        val purchases = bills.filter { it.type == DocumentType.PURCHASE }.sumOf { it.taxableTotal }
        val purchaseReturns = bills.filter { it.type == DocumentType.PURCHASE_RETURN }.sumOf { it.taxableTotal }
        val spent = expenses.sumOf { it.amount }
        val profit = sales - saleReturns - purchases + purchaseReturns - spent
        fun row(res: Int, amount: Long) = listOf(Cells.label(res), Cells.money(amount))
        return ReportTable(
            R.string.report_pl,
            listOf(Col(R.string.col_details), Col(R.string.col_amount, true)),
            listOf(
                row(R.string.pl_sales, sales), row(R.string.pl_sales_returns, -saleReturns),
                row(R.string.pl_purchases, -purchases), row(R.string.pl_purchase_returns, purchaseReturns),
                row(R.string.pl_expenses, -spent),
            ),
            row(R.string.pl_profit, profit),
        )
    }

    fun expenses(expenses: List<ExpenseEntity>): ReportTable {
        val groups = expenses.groupBy { it.category }
        val rows = groups.map { (key, list) -> Triple(key, list.size, list.sumOf { it.amount }) }.sortedByDescending { it.third }
        return ReportTable(
            R.string.report_expenses,
            listOf(Col(R.string.col_category), Col(R.string.col_count, true), Col(R.string.col_amount, true)),
            rows.map { (key, n, amount) ->
                listOf(ExpenseCategories.labelRes(key)?.let { Cells.label(it) } ?: Cells.text(key), Cells.count(n), Cells.money(amount))
            },
            listOf(Cells.label(R.string.col_total), Cells.count(rows.sumOf { it.second }), Cells.money(rows.sumOf { it.third })),
        )
    }

    fun stock(items: List<ItemEntity>, stocks: Map<String, Long>): ReportTable {
        val products = items.filter { it.type == ItemType.PRODUCT }.sortedBy { it.name.lowercase() }
        val rows = products.map {
            val qty = stocks[it.id] ?: 0L
            listOf(Cells.text(it.name), Cells.text(it.unit), Cells.qty(qty), Cells.money(it.purchasePrice), Cells.money(valueOf(qty, it.purchasePrice)))
        }
        return ReportTable(
            R.string.report_stock,
            listOf(Col(R.string.col_item), Col(R.string.col_unit), Col(R.string.col_stock, true), Col(R.string.col_rate, true), Col(R.string.col_value, true)),
            rows,
            listOf(Cells.label(R.string.col_total), Cells.text(""), Cells.text(""), Cells.text(""), Cells.money(products.sumOf { valueOf(stocks[it.id] ?: 0L, it.purchasePrice) })),
        )
    }

    /** Stock value in paise: quantity (thousandths) x price per unit, rounded half up. */
    fun valueOf(qtyThousandths: Long, pricePaise: Long): Long =
        java.math.BigInteger.valueOf(qtyThousandths).multiply(java.math.BigInteger.valueOf(pricePaise)).shiftLeft(1)
            .add(java.math.BigInteger.valueOf(1000)).divide(java.math.BigInteger.valueOf(2000)).toLong()

    fun lowStock(items: List<ItemEntity>, stocks: Map<String, Long>): ReportTable {
        val low = items.filter { it.type == ItemType.PRODUCT && it.minStock > 0 && (stocks[it.id] ?: 0L) <= it.minStock }.sortedBy { it.name.lowercase() }
        return ReportTable(
            R.string.report_low_stock,
            listOf(Col(R.string.col_item), Col(R.string.col_unit), Col(R.string.col_stock, true), Col(R.string.col_min_stock, true)),
            low.map { listOf(Cells.text(it.name), Cells.text(it.unit), Cells.qty(stocks[it.id] ?: 0L), Cells.qty(it.minStock)) },
        )
    }

    // ---- GST ----

    /** B2B: sales to parties that have a GSTIN, one row per bill. */
    fun gstr1B2b(invoices: List<InvoiceEntity>): ReportTable {
        val list = live(invoices).filter { isSales(it.type) && !it.partyGstin.isNullOrBlank() }.sortedWith(compareBy({ it.date }, { it.sequenceNo }))
        return ReportTable(
            R.string.tbl_b2b,
            listOf(
                Col(R.string.col_gstin), Col(R.string.col_party), Col(R.string.col_number), Col(R.string.col_date), Col(R.string.col_taxable, true),
                Col(R.string.col_cgst, true), Col(R.string.col_sgst, true), Col(R.string.col_igst, true), Col(R.string.col_total, true),
            ),
            list.map {
                val s = sign(it.type)
                listOf(
                    Cells.text(it.partyGstin), Cells.text(it.partyName), Cells.text(it.number), Cells.date(it.date), Cells.money(s * it.taxableTotal),
                    Cells.money(s * it.cgst), Cells.money(s * it.sgst), Cells.money(s * it.igst), Cells.money(s * it.grandTotal),
                )
            },
            gstTotals(list, 4),
        )
    }

    /** B2C: sales without a GSTIN, summed by state and tax rate (from the bill lines). */
    fun gstr1B2c(invoices: List<InvoiceEntity>, lines: List<InvoiceLineEntity>): ReportTable {
        val bills = live(invoices).filter { isSales(it.type) && it.partyGstin.isNullOrBlank() }.associateBy { it.id }
        val groups = lines.filter { it.invoiceId in bills }.groupBy { bills.getValue(it.invoiceId).placeOfSupplyStateCode to it.gstRateBp }
        val rows = groups.map { (key, ls) ->
            fun sum(f: (InvoiceLineEntity) -> Long) = ls.sumOf { sign(bills.getValue(it.invoiceId).type) * f(it) }
            listOf(Cells.text(key.first), Cells.percent(key.second), Cells.money(sum { it.taxableValue }), Cells.money(sum { it.cgst }), Cells.money(sum { it.sgst }), Cells.money(sum { it.igst }))
        }.sortedWith(compareBy({ it[0].text }, { it[1].raw.toDoubleOrNull() ?: 0.0 }))
        return ReportTable(
            R.string.tbl_b2c,
            listOf(Col(R.string.col_place), Col(R.string.col_gst_rate, true), Col(R.string.col_taxable, true), Col(R.string.col_cgst, true), Col(R.string.col_sgst, true), Col(R.string.col_igst, true)),
            rows,
            listOf(Cells.label(R.string.col_total), Cells.text("")) + (2..5).map { c -> Cells.money(rows.sumOf { plainToPaise(it[c].raw) }) },
        )
    }

    /** HSN summary of all sales: quantity, taxable value and tax per HSN code and rate. */
    fun gstr1Hsn(invoices: List<InvoiceEntity>, lines: List<InvoiceLineEntity>): ReportTable {
        val bills = live(invoices).filter { isSales(it.type) }.associateBy { it.id }
        val groups = lines.filter { it.invoiceId in bills }.groupBy { Triple(it.hsnSac.orEmpty(), it.gstRateBp, it.unit) }
        val rows = groups.map { (key, ls) ->
            fun sum(f: (InvoiceLineEntity) -> Long) = ls.sumOf { sign(bills.getValue(it.invoiceId).type) * f(it) }
            listOf(
                Cells.text(key.first), Cells.percent(key.second), Cells.text(key.third), Cells.qty(sum { it.qty }),
                Cells.money(sum { it.taxableValue }), Cells.money(sum { it.cgst }), Cells.money(sum { it.sgst }), Cells.money(sum { it.igst }),
            )
        }.sortedBy { it[0].text }
        return ReportTable(
            R.string.tbl_hsn,
            listOf(
                Col(R.string.col_hsn), Col(R.string.col_gst_rate, true), Col(R.string.col_unit), Col(R.string.col_qty, true), Col(R.string.col_taxable, true),
                Col(R.string.col_cgst, true), Col(R.string.col_sgst, true), Col(R.string.col_igst, true),
            ),
            rows,
            listOf(Cells.label(R.string.col_total), Cells.text(""), Cells.text(""), Cells.text("")) + (4..7).map { c -> Cells.money(rows.sumOf { plainToPaise(it[c].raw) }) },
        )
    }

    /** Tax collected on sales, tax paid on purchases (input credit) and the net payable, per tax head. */
    fun gstr3b(invoices: List<InvoiceEntity>): ReportTable {
        val bills = live(invoices)
        fun side(pick: (DocumentType) -> Boolean) = bills.filter { pick(it.type) }.let { list ->
            longArrayOf(
                list.sumOf { sign(it.type) * it.taxableTotal }, list.sumOf { sign(it.type) * it.igst },
                list.sumOf { sign(it.type) * it.cgst }, list.sumOf { sign(it.type) * it.sgst },
            )
        }
        val out = side(::isSales)
        val input = side(::isPurchases)
        val net = longArrayOf(0, out[1] - input[1], out[2] - input[2], out[3] - input[3])
        fun row(res: Int, v: LongArray, withTaxable: Boolean = true) =
            listOf(Cells.label(res), if (withTaxable) Cells.money(v[0]) else Cells.text(""), Cells.money(v[1]), Cells.money(v[2]), Cells.money(v[3]))
        return ReportTable(
            R.string.report_gstr3b,
            listOf(Col(R.string.col_details), Col(R.string.col_taxable, true), Col(R.string.col_igst, true), Col(R.string.col_cgst, true), Col(R.string.col_sgst, true)),
            listOf(row(R.string.gst_outward, out), row(R.string.gst_inward, input)),
            row(R.string.gst_net, net, withTaxable = false),
        )
    }

    private fun gstTotals(list: List<InvoiceEntity>, spacers: Int): List<Cell> =
        listOf(Cells.label(R.string.col_total)) + List(spacers - 1) { Cells.text("") } + listOf(
            Cells.money(list.sumOf { sign(it.type) * it.taxableTotal }), Cells.money(list.sumOf { sign(it.type) * it.cgst }),
            Cells.money(list.sumOf { sign(it.type) * it.sgst }), Cells.money(list.sumOf { sign(it.type) * it.igst }), Cells.money(list.sumOf { sign(it.type) * it.grandTotal }),
        )

    /** Reads back the plain "123.45" CSV text of a money cell as paise (used to total grouped rows). */
    private fun plainToPaise(raw: String): Long {
        val negative = raw.startsWith("-")
        val digits = raw.removePrefix("-").replace(".", "")
        return (if (negative) -1 else 1) * (digits.toLongOrNull() ?: 0L)
    }
}
