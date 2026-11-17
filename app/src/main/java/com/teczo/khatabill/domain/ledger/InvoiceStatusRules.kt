package com.teczo.khatabill.domain.ledger

import com.teczo.khatabill.domain.model.InvoiceStatus

object InvoiceStatusRules {
    /** Status of a live invoice from its total and the money allocated to it. */
    fun statusFor(grandTotal: Long, paid: Long): InvoiceStatus = when {
        paid >= grandTotal -> InvoiceStatus.PAID
        paid > 0 -> InvoiceStatus.PARTIAL
        else -> InvoiceStatus.UNPAID
    }
}
