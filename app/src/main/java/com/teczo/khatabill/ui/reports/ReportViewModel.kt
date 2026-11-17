package com.teczo.khatabill.ui.reports

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.ExpenseRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.ItemRepository
import com.teczo.khatabill.domain.report.DateRange
import com.teczo.khatabill.domain.report.ReportTable
import com.teczo.khatabill.domain.report.Reports
import com.teczo.khatabill.util.locale.LocalizedContext
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ReportKind(val titleRes: Int, val usesRange: Boolean = true, val isGst: Boolean = false) {
    SALES(R.string.report_sales),
    PURCHASES(R.string.report_purchases),
    ITEM_WISE(R.string.report_item_wise),
    PARTY_WISE(R.string.report_party_wise),
    PROFIT_LOSS(R.string.report_pl),
    STOCK(R.string.report_stock, usesRange = false),
    LOW_STOCK(R.string.report_low_stock, usesRange = false),
    EXPENSES(R.string.report_expenses),
    GSTR1(R.string.report_gstr1, isGst = true),
    GSTR3B(R.string.report_gstr3b, isGst = true),
}

data class ReportState(val tables: List<ReportTable> = emptyList(), val range: DateRange? = null)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReportViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val businessRepository: BusinessRepository,
    private val invoices: InvoiceRepository,
    expenses: ExpenseRepository,
    items: ItemRepository,
) : ViewModel() {
    val kind: ReportKind = ReportKind.valueOf(savedState["kind"] ?: ReportKind.SALES.name)

    val selector = RangeSelector()

    private val _business = MutableStateFlow<BusinessEntity?>(null)
    val business: StateFlow<BusinessEntity?> = _business.asStateFlow()

    init {
        viewModelScope.launch { _business.value = businessRepository.active.first() }
    }

    val state: StateFlow<ReportState> = businessRepository.active.flatMapLatest { b ->
        val walkIn = LocalizedContext.forApp(appContext).getString(R.string.walk_in_customer)
        combine(
            selector.range, invoices.observeAll(b.id), expenses.observeAll(b.id), items.observe(b.id), items.observeAllStock(b.id),
        ) { range, allInvoices, allExpenses, allItems, stocks -> Inputs(range, allInvoices, allExpenses, allItems, stocks.associate { it.itemId to it.qty }) }
            .flatMapLatest { i ->
                invoices.observeLinesBetween(b.id, i.range.from, i.range.to).map { l -> build(i, l, walkIn) }
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReportState())

    private class Inputs(
        val range: DateRange,
        val invoices: List<com.teczo.khatabill.data.entities.InvoiceEntity>,
        val expenses: List<com.teczo.khatabill.data.entities.ExpenseEntity>,
        val items: List<com.teczo.khatabill.data.entities.ItemEntity>,
        val stocks: Map<String, Long>,
    )

    private fun build(i: Inputs, lines: List<com.teczo.khatabill.data.entities.InvoiceLineEntity>, walkIn: String): ReportState {
        val inRange = i.invoices.filter { it.date in i.range }
        val spent = i.expenses.filter { it.date in i.range }
        val tables = when (kind) {
            ReportKind.SALES -> listOf(Reports.sales(inRange, walkIn))
            ReportKind.PURCHASES -> listOf(Reports.purchases(inRange, walkIn))
            ReportKind.ITEM_WISE -> listOf(Reports.itemWise(inRange, lines))
            ReportKind.PARTY_WISE -> listOf(Reports.partyWise(inRange, walkIn))
            ReportKind.PROFIT_LOSS -> listOf(Reports.profitAndLoss(inRange, spent))
            ReportKind.STOCK -> listOf(Reports.stock(i.items, i.stocks))
            ReportKind.LOW_STOCK -> listOf(Reports.lowStock(i.items, i.stocks))
            ReportKind.EXPENSES -> listOf(Reports.expenses(spent))
            ReportKind.GSTR1 -> listOf(Reports.gstr1B2b(inRange), Reports.gstr1B2c(inRange, lines), Reports.gstr1Hsn(inRange, lines))
            ReportKind.GSTR3B -> listOf(Reports.gstr3b(inRange))
        }
        return ReportState(tables, i.range)
    }
}
