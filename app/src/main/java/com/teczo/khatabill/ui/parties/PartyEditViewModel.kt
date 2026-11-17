package com.teczo.khatabill.ui.parties

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.PartyRepository
import com.teczo.khatabill.domain.model.BalanceType
import com.teczo.khatabill.domain.model.PartyType
import com.teczo.khatabill.util.money.MoneyInput
import com.teczo.khatabill.util.validators.FieldValidators
import com.teczo.khatabill.util.validators.GstinValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val NEW_ID = "new"

enum class PartyField { NAME, PHONE, WHATSAPP, GSTIN, OPENING, CREDIT }

data class PartyForm(
    val name: String = "",
    val nameKn: String = "",
    val type: PartyType = PartyType.CUSTOMER,
    val phone: String = "",
    val whatsapp: String = "",
    val gstin: String = "",
    val billingAddress: String = "",
    val shippingAddress: String = "",
    val stateCode: String = "",
    val openingBalance: String = "",
    val balanceType: BalanceType = BalanceType.TO_COLLECT,
    val creditLimit: String = "",
    val notes: String = "",
)

@HiltViewModel
class PartyEditViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val business: BusinessRepository,
    private val parties: PartyRepository,
) : ViewModel() {
    private val partyId: String = savedState["partyId"] ?: NEW_ID
    val isNew = partyId == NEW_ID

    private val _form = MutableStateFlow(PartyForm())
    val form: StateFlow<PartyForm> = _form.asStateFlow()
    private val _errors = MutableStateFlow<Map<PartyField, Int>>(emptyMap())
    val errors: StateFlow<Map<PartyField, Int>> = _errors.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    private var original: PartyEntity? = null

    init {
        if (!isNew) {
            viewModelScope.launch {
                parties.get(partyId)?.let { p ->
                    original = p
                    _form.value = PartyForm(
                        name = p.name, nameKn = p.nameKn.orEmpty(), type = p.type, phone = p.phone.orEmpty(),
                        whatsapp = p.whatsapp.orEmpty(), gstin = p.gstin.orEmpty(),
                        billingAddress = p.billingAddress.orEmpty(), shippingAddress = p.shippingAddress.orEmpty(),
                        stateCode = p.stateCode.orEmpty(),
                        openingBalance = if (p.openingBalance == 0L) "" else MoneyInput.paiseToText(p.openingBalance),
                        balanceType = p.openingBalanceType,
                        creditLimit = p.creditLimit?.let { MoneyInput.paiseToText(it) }.orEmpty(),
                        notes = p.notes.orEmpty(),
                    )
                }
            }
        }
    }

    fun update(change: (PartyForm) -> PartyForm) {
        _form.update { old ->
            var new = change(old)
            if (new.gstin != old.gstin) {
                new = new.copy(gstin = new.gstin.trim().uppercase())
                val r = GstinValidator.validate(new.gstin)
                if (r is GstinValidator.Result.Valid) new = new.copy(stateCode = r.stateCode)
            }
            new
        }
        _errors.value = emptyMap()
    }

    fun save() {
        val f = _form.value
        val errors = buildMap<PartyField, Int> {
            if (f.name.isBlank()) put(PartyField.NAME, R.string.err_required)
            if (f.phone.isNotBlank() && !FieldValidators.isValidMobile(f.phone.trim())) put(PartyField.PHONE, R.string.err_mobile)
            if (f.whatsapp.isNotBlank() && !FieldValidators.isValidMobile(f.whatsapp.trim())) put(PartyField.WHATSAPP, R.string.err_mobile)
            if (f.gstin.isNotBlank()) gstinError(f.gstin)?.let { put(PartyField.GSTIN, it) }
            if (f.openingBalance.isNotBlank() && MoneyInput.parsePaise(f.openingBalance) == null) put(PartyField.OPENING, R.string.err_number)
            if (f.creditLimit.isNotBlank() && MoneyInput.parsePaise(f.creditLimit) == null) put(PartyField.CREDIT, R.string.err_number)
        }
        if (errors.isNotEmpty()) {
            _errors.value = errors
            return
        }
        viewModelScope.launch {
            val biz = business.active.first()
            val now = System.currentTimeMillis()
            val base = original ?: PartyEntity(
                id = UUID.randomUUID().toString(), businessId = biz.id, createdAt = now, updatedAt = now, name = "",
            )
            parties.save(
                base.copy(
                    name = f.name.trim(),
                    nameKn = f.nameKn.trim().ifBlank { null },
                    type = f.type,
                    phone = f.phone.trim().ifBlank { null },
                    whatsapp = f.whatsapp.trim().ifBlank { null },
                    gstin = f.gstin.trim().ifBlank { null },
                    billingAddress = f.billingAddress.trim().ifBlank { null },
                    shippingAddress = f.shippingAddress.trim().ifBlank { null },
                    stateCode = f.stateCode.ifBlank { null },
                    openingBalance = MoneyInput.parsePaise(f.openingBalance) ?: 0L,
                    openingBalanceType = f.balanceType,
                    creditLimit = MoneyInput.parsePaise(f.creditLimit),
                    notes = f.notes.trim().ifBlank { null },
                ),
            )
            _done.value = true
        }
    }

    fun delete() {
        if (isNew) return
        viewModelScope.launch {
            parties.delete(partyId)
            _done.value = true
        }
    }

    @StringRes
    private fun gstinError(gstin: String): Int? = when ((GstinValidator.validate(gstin) as? GstinValidator.Result.Invalid)?.reason) {
        null -> null
        GstinValidator.Reason.BAD_LENGTH -> R.string.err_gstin_length
        GstinValidator.Reason.BAD_FORMAT -> R.string.err_gstin_format
        GstinValidator.Reason.BAD_STATE -> R.string.err_gstin_state
        GstinValidator.Reason.BAD_CHECKSUM -> R.string.err_gstin_checksum
    }
}
