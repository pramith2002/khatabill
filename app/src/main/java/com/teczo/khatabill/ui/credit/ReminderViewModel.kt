package com.teczo.khatabill.ui.credit

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.dao.ReminderDao
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.entities.ReminderEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.InvoiceRepository
import com.teczo.khatabill.data.repositories.LedgerRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.data.repositories.PaymentRepository
import com.teczo.khatabill.domain.ledger.BalanceCalculator
import com.teczo.khatabill.domain.ledger.InvoiceFlow
import com.teczo.khatabill.domain.ledger.PaymentFlow
import com.teczo.khatabill.domain.ledger.Statement
import com.teczo.khatabill.domain.ledger.StatementBill
import com.teczo.khatabill.domain.ledger.StatementBuilder
import com.teczo.khatabill.domain.ledger.StatementPayment
import com.teczo.khatabill.domain.model.InvoiceStatus
import com.teczo.khatabill.domain.model.ReminderChannel
import com.teczo.khatabill.domain.model.ReminderStatus
import com.teczo.khatabill.util.locale.LocalizedContext
import com.teczo.khatabill.util.money.MoneyFormatter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

enum class ReminderStyle { GENTLE, FIRM }

data class ReminderState(
    val party: PartyEntity? = null,
    val business: BusinessEntity? = null,
    /** What the party owes us (paise); the reminder is about this amount. */
    val owed: Long = 0,
    val language: String = "en",
    val style: ReminderStyle = ReminderStyle.GENTLE,
    val message: String = "",
    val attachStatement: Boolean = true,
    val statement: Statement? = null,
)

@HiltViewModel
class ReminderViewModel @Inject constructor(
    savedState: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val businessRepository: BusinessRepository,
    parties: PartyRepository,
    invoices: InvoiceRepository,
    payments: PaymentRepository,
    ledger: LedgerRepository,
    private val reminders: ReminderDao,
) : ViewModel() {
    private val partyId: String = checkNotNull(savedState["partyId"])

    private val _state = MutableStateFlow(ReminderState(language = LocalizedContext.appLanguageTag().takeIf { it in setOf("kn", "en", "hi") } ?: "en"))
    val state: StateFlow<ReminderState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val biz = businessRepository.active.first()
            combine(parties.observeById(partyId), invoices.observeByParty(partyId), payments.observeByParty(partyId), ledger.observeBalances(biz.id)) { p, inv, pay, balances ->
                if (p == null) return@combine
                val live = inv.filter { it.status != InvoiceStatus.CANCELLED && it.status != InvoiceStatus.DRAFT }
                val owed = BalanceCalculator.balance(
                    p.openingBalance, p.openingBalanceType, live.map { InvoiceFlow(it.type, it.grandTotal) }, pay.map { PaymentFlow(it.type, it.amount) },
                ).coerceAtLeast(0)
                val statement = StatementBuilder.build(
                    p.openingBalance, p.openingBalanceType,
                    live.map { StatementBill(it.date, it.createdAt, it.number, it.type, it.grandTotal) },
                    pay.map { StatementPayment(it.date, it.createdAt, it.type, it.amount, it.mode) },
                )
                val overdue = p.id in balances.overdueParties
                _state.update { s ->
                    val first = s.party == null
                    val style = if (first) (if (overdue) ReminderStyle.FIRM else ReminderStyle.GENTLE) else s.style
                    val next = s.copy(party = p, business = biz, owed = owed, style = style, statement = statement)
                    if (first) next.copy(message = buildMessage(next)) else next.copy(message = if (s.message.isBlank()) buildMessage(next) else s.message)
                }
            }.collect {}
        }
    }

    /** The message text for the current language, style and amount, using the app's own translated templates. */
    private fun buildMessage(s: ReminderState): String {
        val p = s.party ?: return ""
        val ctx = LocalizedContext.of(appContext, s.language)
        val name = if (s.language == "kn") (p.nameKn?.takeIf { it.isNotBlank() } ?: p.name) else p.name
        val amount = "₹" + MoneyFormatter.format(s.owed)
        val template = if (s.style == ReminderStyle.GENTLE) R.string.reminder_msg_gentle else R.string.reminder_msg_firm
        return ctx.getString(template, name, s.business?.name.orEmpty(), amount)
    }

    fun setLanguage(tag: String) = _state.update { it.copy(language = tag).let { n -> n.copy(message = buildMessage(n)) } }

    fun setStyle(style: ReminderStyle) = _state.update { it.copy(style = style).let { n -> n.copy(message = buildMessage(n)) } }

    fun setMessage(text: String) = _state.update { it.copy(message = text) }

    fun setAttach(on: Boolean) = _state.update { it.copy(attachStatement = on) }

    /** Remembers that a reminder was sent (the shopkeeper still presses send in WhatsApp or the SMS app). */
    fun recordSent(channel: ReminderChannel) {
        val s = _state.value
        val biz = s.business ?: return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            reminders.insert(
                ReminderEntity(
                    id = UUID.randomUUID().toString(), businessId = biz.id, createdAt = now, updatedAt = now,
                    partyId = partyId, scheduledAt = now, channel = channel, status = ReminderStatus.SENT, message = s.message,
                ),
            )
        }
    }
}
