package com.teczo.khatabill.domain.invoice

import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.util.dates.FinancialYear

/**
 * Display number such as "INV/26-27/0001". GST allows at most 16 characters in an invoice number,
 * so the business prefix must be at most 2 characters (3-letter series + "/26-27/" + 4 digits = 14).
 * The business setup screen enforces this limit.
 */
object InvoiceNumber {

    fun format(prefix: String, series: String, financialYear: String, sequenceNo: Int): String =
        "$prefix$series/${FinancialYear.shortLabel(financialYear)}/${sequenceNo.toString().padStart(4, '0')}"

    /** Default series per document type. A SALE by a business that charges no tax is a bill of supply. */
    fun defaultSeries(type: DocumentType, chargesTax: Boolean): String = when (type) {
        DocumentType.SALE -> if (chargesTax) "INV" else "BOS"
        DocumentType.PURCHASE -> "PUR"
        DocumentType.QUOTATION -> "QUO"
        DocumentType.SALE_RETURN -> "CN"
        DocumentType.PURCHASE_RETURN -> "DN"
        DocumentType.DELIVERY_CHALLAN -> "DC"
        DocumentType.PROFORMA -> "PI"
    }
}
