package com.teczo.khatabill.ui.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.ExpenseRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.report.BookResult
import com.teczo.khatabill.domain.report.DayBook
import com.teczo.khatabill.domain.report.DayBookEntry
import com.teczo.khatabill.domain.report.MoneyBook
import com.teczo.khatabill.domain.report.MoneyMovement
import com.teczo.khatabill.ui.reports.RangeSelector
import com.teczo.khatabill.util.dates.DateFormat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Which book: "cash", "bank" or "day" (the route argument). */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class BookViewModel @Inject constructor(
    savedState: SavedStateHandle,
    business: BusinessRepository,
    payments: PaymentRepository,
    expenses: ExpenseRepository,
    parties: PartyRepository,
    invoices: InvoiceRepository,
) : ViewModel() {
    val book: String = savedState["book"] ?: "cash"

    val selector = RangeSelector()
    val day = MutableStateFlow(DateFormat.today())

    private val movements: Flow<List<MoneyMovement>> = business.active.flatMapLatest { b ->
        combine(payments.observeAll(b.id), expenses.observeAll(b.id), parties.observe(b.id)) { p, e, ps ->
            MoneyBook.movements(p, e, ps.associate { it.id to it.name })
        }
    }

    /** Cash book or bank book for the chosen dates. */
    val result: StateFlow<BookResult?> = combine(movements, selector.range) { m, range ->
        MoneyBook.compute(m, range, if (book == "bank") MoneyBook.BANK else MoneyBook.CASH)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Everything that happened on the chosen day. */
    val dayEntries: StateFlow<List<DayBookEntry>> = business.active.flatMapLatest { b ->
        combine(movements, invoices.observeAll(b.id), day) { m, inv, d -> DayBook.forDay(d, inv, m) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
