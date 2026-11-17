package com.teczo.khatabill.ui.payments

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.InvoiceEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.LedgerRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.domain.model.PaymentType
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

const val NO_PARTY = "none"

data class PaymentForm(
    val partyId: String? = null,
    val type: PaymentType = PaymentType.RECEIVED,
    val amount: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val date: Long = DateFormat.today(),
    val reference: String = "",
    val notes: String = "",
    /** null = oldest bills first. */
    val invoiceId: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordPaymentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val businessRepository: BusinessRepository,
    partyRepository: PartyRepository,
    private val invoiceRepository: InvoiceRepository,
    private val paymentRepository: PaymentRepository,
    ledger: LedgerRepository,
) : ViewModel() {
    private val presetParty: String? = savedState.get<String>("partyId")?.takeIf { it != NO_PARTY }
    val partyIsFixed = presetParty != null

    private val _form = MutableStateFlow(PaymentForm(partyId = presetParty))
    val form: StateFlow<PaymentForm> = _form.asStateFlow()

    private val _business = MutableStateFlow<BusinessEntity?>(null)
    val business: StateFlow<BusinessEntity?> = _business.asStateFlow()

    val parties: StateFlow<List<PartyEntity>> = businessRepository.active
        .flatMapLatest { partyRepository.observe(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Unpaid bills of the chosen party that this payment could settle. */
    val openInvoices: StateFlow<List<InvoiceEntity>> = _form
        .map { it.partyId to it.type }
        .flatMapLatest { (partyId, type) ->
            if (partyId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else invoiceRepository.observeByParty(partyId).map { list ->
                val docType = if (type == PaymentType.RECEIVED) DocumentType.SALE else DocumentType.PURCHASE
                list.filter { it.type == docType && (it.status == InvoiceStatus.UNPAID || it.status == InvoiceStatus.PARTIAL) }.sortedBy { it.date }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The party's overall balance (positive = they owe us), for the "Outstanding" hint. */
    val balance: StateFlow<Long> = combine(_business, _form.map { it.partyId }) { b, p -> b to p }
        .flatMapLatest { (b, p) -> if (b == null || p == null) kotlinx.coroutines.flow.flowOf(0L) else ledger.observeBalances(b.id).map { it.byParty[p] ?: 0L } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    init {
        viewModelScope.launch {
            _business.value = businessRepository.active.first()
            presetParty?.let { id -> partyRepository.get(id)?.let(::applyPartyDefaults) }
        }
    }

    private fun applyPartyDefaults(p: PartyEntity) {
        _form.update { it.copy(partyId = p.id, type = if (p.type == PartyType.SUPPLIER) PaymentType.PAID else PaymentType.RECEIVED, invoiceId = null) }
    }

    fun setParty(p: PartyEntity) {
        applyPartyDefaults(p)
        _error.value = null
    }

    fun update(change: (PaymentForm) -> PaymentForm) {
        _form.update(change)
        _error.value = null
    }

    /** True while a save is running, so a double tap cannot record the payment twice. */
    private var saving = false

    fun save() {
        val f = _form.value
        val biz = _business.value ?: return
        val partyId = f.partyId ?: run { _error.value = R.string.err_choose_party; return }
        val amount = MoneyInput.parsePaise(f.amount)?.takeIf { it > 0 } ?: run { _error.value = R.string.err_amount; return }
        if (saving) return
        saving = true
        viewModelScope.launch {
            try {
                paymentRepository.record(
                    businessId = biz.id, type = f.type, partyId = partyId, date = f.date, amount = amount, mode = f.mode,
                    reference = f.reference, notes = f.notes, preferredInvoiceId = f.invoiceId,
                )
                _done.value = true
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _error.value = R.string.err_save_failed
            } finally {
                saving = false
            }
        }
    }
}
