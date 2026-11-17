package com.teczo.khatabill.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import com.teczo.khatabill.data.repositories.LedgerRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.report.DateRanges
import com.teczo.khatabill.domain.report.RangeKind
import com.teczo.khatabill.domain.report.Reports
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.locale.LocalizedContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** A label with a money (or quantity) figure already formatted for display. */
data class DashRow(val label: String, val value: String)

data class HomeState(
    val ownerName: String = "",
    val summary: HomeSummary = HomeSummary(),
    val topItems: List<DashRow> = emptyList(),
    val topCustomers: List<DashRow> = emptyList(),
    val lowStock: List<DashRow> = emptyList(),
    val recent: List<InvoiceEntity> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    business: BusinessRepository,
    private val invoices: InvoiceRepository,
    payments: PaymentRepository,
    ledger: LedgerRepository,
    items: ItemRepository,
) : ViewModel() {
    val state: StateFlow<HomeState> = business.active.flatMapLatest { b ->
        val today = DateFormat.today()
        val month = DateRanges.of(RangeKind.THIS_MONTH, today, b.financialYearStartMonth)
        val walkIn = LocalizedContext.forApp(appContext).getString(R.string.walk_in_customer)
        combine(
            invoices.observeAll(b.id), payments.observeAll(b.id), ledger.observeBalances(b.id),
            invoices.observeLinesBetween(b.id, month.from, month.to), items.observe(b.id), items.observeAllStock(b.id),
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val inv = values[0] as List<InvoiceEntity>
            @Suppress("UNCHECKED_CAST")
            val pay = values[1] as List<com.teczo.khatabill.data.entities.PaymentEntity>
            val balances = values[2] as com.teczo.khatabill.data.repositories.PartyBalances
            @Suppress("UNCHECKED_CAST")
            val lines = values[3] as List<com.teczo.khatabill.data.entities.InvoiceLineEntity>
            @Suppress("UNCHECKED_CAST")
            val allItems = values[4] as List<com.teczo.khatabill.data.entities.ItemEntity>
            @Suppress("UNCHECKED_CAST")
            val stocks = (values[5] as List<com.teczo.khatabill.data.dao.ItemStock>).associate { it.itemId to it.qty }

            val monthInvoices = inv.filter { it.date in month }
            val topItems = Reports.itemWise(monthInvoices, lines).rows.take(3).map { DashRow(it[0].text, it[4].text) }
            val topCustomers = Reports.partyWise(monthInvoices, walkIn).rows.take(3).map { DashRow(it[0].text, it[2].text) }
            val low = Reports.lowStock(allItems, stocks).rows.take(3).map { DashRow(it[0].text, it[2].text + " " + it[1].text) }
            HomeState(
                ownerName = b.ownerName,
                summary = HomeSummary.compute(balances, inv, pay, today),
                topItems = topItems,
                topCustomers = topCustomers,
                lowStock = low,
                recent = inv.filter { it.status != InvoiceStatus.CANCELLED }.sortedByDescending { it.createdAt }.take(5),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}
