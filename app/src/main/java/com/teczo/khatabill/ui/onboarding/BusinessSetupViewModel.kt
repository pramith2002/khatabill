package com.teczo.khatabill.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.util.validators.GstStateCodes
import com.teczo.khatabill.util.validators.GstinValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

const val SETUP_STEPS = 3

@HiltViewModel
class BusinessSetupViewModel @Inject constructor(
    private val repository: BusinessRepository,
) : ViewModel() {

    private val _form = MutableStateFlow(SetupForm())
    val form: StateFlow<SetupForm> = _form.asStateFlow()

    private val _step = MutableStateFlow(0)
    val step: StateFlow<Int> = _step.asStateFlow()

    private val _errors = MutableStateFlow<Map<SetupField, FieldError>>(emptyMap())
    val errors: StateFlow<Map<SetupField, FieldError>> = _errors.asStateFlow()

    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    fun update(change: (SetupForm) -> SetupForm) {
        _form.update { old ->
            var new = change(old)
            if (new.gstin != old.gstin) new = BusinessSetupRules.applyGstin(new.copy(gstin = new.gstin.trim().uppercase()))
            if (new.gstScheme == GstScheme.UNREGISTERED && old.gstScheme != GstScheme.UNREGISTERED) {
                new = new.copy(gstin = "", pan = "")
            }
            new
        }
        _errors.value = emptyMap()
    }

    fun next() {
        val errors = BusinessSetupRules.validateStep(_step.value, _form.value)
        if (errors.isNotEmpty()) {
            _errors.value = errors
            return
        }
        if (_step.value < SETUP_STEPS - 1) _step.value += 1 else save()
    }

    fun back() {
        if (_step.value > 0) {
            _step.value -= 1
            _errors.value = emptyMap()
        }
    }

    private fun save() {
        val f = _form.value
        val now = System.currentTimeMillis()
        val id = UUID.randomUUID().toString()
        val gstinResult = GstinValidator.validate(f.gstin)
        val pan = (gstinResult as? GstinValidator.Result.Valid)?.pan ?: f.pan.trim().uppercase().ifBlank { null }
        val entity = BusinessEntity(
            id = id,
            businessId = id,
            createdAt = now,
            updatedAt = now,
            name = f.shopName.trim(),
            ownerName = f.ownerName.trim(),
            phone = f.mobile.trim(),
            address = f.address.trim(),
            city = f.city.trim(),
            state = GstStateCodes.englishName(f.stateCode) ?: f.stateCode,
            stateCode = f.stateCode,
            pincode = f.pincode.trim(),
            gstin = (gstinResult as? GstinValidator.Result.Valid)?.gstin,
            pan = pan,
            gstScheme = f.gstScheme,
            businessType = f.businessType,
            upiId = f.upiId.trim().ifBlank { null },
            bankName = f.bankName.trim().ifBlank { null },
            accountNo = f.accountNo.trim().ifBlank { null },
            ifsc = f.ifsc.trim().uppercase().ifBlank { null },
            invoicePrefix = f.prefix.trim().uppercase(),
        )
        viewModelScope.launch {
            repository.save(entity)
            _saved.value = true
        }
    }
}
