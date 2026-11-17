package com.teczo.khatabill.work

import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity

/** One notification: a party who owes us money on bills that have reached their due date. */
data class DueNotice(
    val partyId: String,
    val partyName: String,
    val partyNameKn: String?,
    /** Total unpaid balance of the due bills (paise). */
    val amount: Long,
    /** True when at least one of the bills is past its due date (not just due today). */
    val overdue: Boolean,
    val invoiceIds: List<String>,
)

object DueReminders {
    /**
     * Groups unpaid bills that are due today or earlier by party. Bills already notified today
     * ([alreadyNotified] invoice ids) are skipped, so a party is never nagged twice in one day.
     */
    fun plan(
        dueInvoices: List<InvoiceEntity>,
        parties: Map<String, PartyEntity>,
        alreadyNotified: Set<String>,
        today: Long,
    ): List<DueNotice> = dueInvoices
        .filter { it.partyId != null && it.dueDate != null && it.dueDate <= today && it.grandTotal > it.paidAmount && it.id !in alreadyNotified }
        .groupBy { it.partyId!! }
        .mapNotNull { (partyId, list) ->
            val party = parties[partyId] ?: return@mapNotNull null
            DueNotice(
                partyId = partyId,
                partyName = party.name,
                partyNameKn = party.nameKn,
                amount = list.sumOf { it.grandTotal - it.paidAmount },
                overdue = list.any { it.dueDate!! < today },
                invoiceIds = list.map { it.id },
            )
        }
        .sortedByDescending { it.amount }
}
