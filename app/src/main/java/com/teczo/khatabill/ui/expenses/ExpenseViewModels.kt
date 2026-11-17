package com.teczo.khatabill.ui.expenses

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.ExpenseEntity
import com.teczo.khatabill.data.repositories.BusinessRepository
import com.teczo.khatabill.data.repositories.ExpenseRepository
import com.teczo.khatabill.domain.model.ExpenseCategories
import com.teczo.khatabill.domain.model.PaymentMode
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyInput
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    business: BusinessRepository,
    private val expenses: ExpenseRepository,
) : ViewModel() {
    val items: StateFlow<List<ExpenseEntity>> = business.active
        .flatMapLatest { expenses.observeAll(it.id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(id: String) {
        viewModelScope.launch { expenses.delete(id) }
    }
}

data class ExpenseForm(
    val category: String = ExpenseCategories.keys.first(),
    val amount: String = "",
    val mode: PaymentMode = PaymentMode.CASH,
    val date: Long = DateFormat.today(),
    val notes: String = "",
    val receiptPath: String? = null,
)

@HiltViewModel
class ExpenseEditViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val business: BusinessRepository,
    private val expenses: ExpenseRepository,
) : ViewModel() {
    private val _form = MutableStateFlow(ExpenseForm())
    val form: StateFlow<ExpenseForm> = _form.asStateFlow()
    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error.asStateFlow()
    private val _done = MutableStateFlow(false)
    val done: StateFlow<Boolean> = _done.asStateFlow()

    fun update(change: (ExpenseForm) -> ExpenseForm) {
        _form.update(change)
        _error.value = null
    }

    /** A fresh file in the app's private storage for the camera app to write the receipt photo into. */
    fun newReceiptFile(): File = File(File(appContext.filesDir, "receipts").apply { mkdirs() }, UUID.randomUUID().toString() + ".jpg")

    fun save() {
        val f = _form.value
        val amount = MoneyInput.parsePaise(f.amount)?.takeIf { it > 0 } ?: run { _error.value = R.string.err_amount; return }
        viewModelScope.launch {
            val biz = business.active.first()
            val now = System.currentTimeMillis()
            expenses.save(
                ExpenseEntity(
                    id = UUID.randomUUID().toString(), businessId = biz.id, createdAt = now, updatedAt = now, date = f.date,
                    category = f.category, amount = amount, mode = f.mode, notes = f.notes.trim().ifBlank { null }, receiptUri = f.receiptPath,
                ),
            )
            _done.value = true
        }
    }
}
