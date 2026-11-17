package com.teczo.khatabill.ui.billing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.data.entities.ItemEntity
import com.teczo.khatabill.data.entities.PartyEntity
import com.teczo.khatabill.domain.gst.GstInvoiceResult
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.Choice
import com.teczo.khatabill.ui.components.ChoiceSheet
import com.teczo.khatabill.ui.components.PickerField
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.ui.util.unitLabel
import com.teczo.khatabill.util.dates.DateFormat
import com.teczo.khatabill.util.money.MoneyFormatter
import com.teczo.khatabill.util.money.MoneyInput
import com.teczo.khatabill.util.validators.GstStateCodes

class NewInvoiceActions(
    val onBack: () -> Unit,
    val onUpdate: ((InvoiceFormState) -> InvoiceFormState) -> Unit,
    val onSetParty: (PartyEntity?) -> Unit,
    val onAddItem: (ItemEntity) -> Unit,
    val onUpdateLine: (String, (LineState) -> LineState) -> Unit,
    val onRemoveLine: (String) -> Unit,
    val onSave: () -> Unit,
)

@Composable
private fun rupees(paise: Long) = stringResource(R.string.amount_rupees, MoneyFormatter.format(paise))

/** The whole New bill screen, driven only by the values passed in (so it can be tested without Hilt). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewInvoiceContent(
    form: InvoiceFormState,
    business: BusinessEntity?,
    calc: CalcState,
    parties: List<PartyEntity>,
    items: List<ItemEntity>,
    error: Int?,
    actions: NewInvoiceActions,
) {
    var showParty by remember { mutableStateOf(false) }
    var showItems by remember { mutableStateOf(false) }
    var showPlace by remember { mutableStateOf(false) }
    var showDate by remember { mutableStateOf(false) }
    val isSale = form.type == DocumentType.SALE
    val title = stringResource(R.string.invoice_new) + " · " + stringResource(Labels.docType(form.type))

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(title, actions.onBack)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PickerField(
                stringResource(R.string.field_party),
                form.party?.name ?: stringResource(R.string.party_walk_in_option),
                { showParty = true },
            )
            PickerField(stringResource(R.string.field_date), DateFormat.format(form.date), { showDate = true })
            if (business?.gstScheme == GstScheme.REGULAR) {
                PickerField(
                    stringResource(if (form.type == DocumentType.PURCHASE) R.string.field_supplier_state else R.string.field_place_of_supply),
                    Labels.state(form.placeOfSupply)?.let { stringResource(it) } ?: form.placeOfSupply,
                    { showPlace = true },
                )
            }

            val lineResults = (calc as? CalcState.Ok)?.result?.lines
            form.lines.forEachIndexed { index, line ->
                LineCard(
                    line = line,
                    total = lineResults?.getOrNull(index)?.lineTotal,
                    onChange = { change -> actions.onUpdateLine(line.key, change) },
                    onRemove = { actions.onRemoveLine(line.key) },
                )
            }
            OutlinedButton(onClick = { showItems = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.add_item))
            }

            if (form.lines.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(
                        form.overallDiscount, { v -> actions.onUpdate { it.copy(overallDiscount = v) } },
                        stringResource(R.string.field_overall_discount), modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal,
                        error = if (form.overallDiscount.isNotBlank() && calc == CalcState.Invalid) R.string.err_number else null,
                    )
                    DiscountModeChips(form.overallIsPercent) { p -> actions.onUpdate { it.copy(overallIsPercent = p) } }
                }
                AppTextField(
                    form.otherCharges, { v -> actions.onUpdate { it.copy(otherCharges = v) } },
                    stringResource(R.string.field_other_charges), keyboardType = KeyboardType.Decimal,
                    error = if (form.otherCharges.isNotBlank() && MoneyInput.parsePaise(form.otherCharges) == null) R.string.err_number else null,
                )
                if (isSale) {
                    AppTextField(
                        form.paid, { v -> actions.onUpdate { it.copy(paid = v) } }, stringResource(R.string.field_paid_now),
                        keyboardType = KeyboardType.Decimal,
                        error = if (form.paid.isNotBlank() && MoneyInput.parsePaise(form.paid) == null) R.string.err_number else null,
                    )
                }
                AppTextField(form.notes, { v -> actions.onUpdate { it.copy(notes = v) } }, stringResource(R.string.field_invoice_notes), singleLine = false)
            }

            if (calc is CalcState.Ok) TotalsCard(calc.result, form.paid.takeIf { isSale }?.let { MoneyInput.parsePaise(it) } ?: 0L)
            val message = when {
                error != null -> error
                calc == CalcState.Invalid -> R.string.err_check_numbers
                else -> null
            }
            if (message != null) Text(stringResource(message), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }
        Button(
            onClick = actions.onSave,
            enabled = calc is CalcState.Ok,
            modifier = Modifier.fillMaxWidth().padding(16.dp).heightIn(min = 56.dp),
        ) { Text(stringResource(R.string.btn_save_bill)) }
    }

    if (showParty) {
        ChoiceSheet(
            title = stringResource(R.string.party_choose),
            choices = listOf(Choice("", stringResource(R.string.party_walk_in_option))) +
                parties.map { Choice(it.id, listOfNotNull(it.name, it.nameKn).joinToString(" · ")) },
            selectedKey = form.party?.id ?: "",
            onSelect = { id -> actions.onSetParty(parties.firstOrNull { it.id == id }); showParty = false },
            onDismiss = { showParty = false },
            searchable = true,
        )
    }
    if (showItems) {
        ChoiceSheet(
            title = stringResource(R.string.picker_choose_item),
            choices = items.map { Choice(it.id, listOfNotNull(it.name, it.nameKn).joinToString(" · ")) },
            selectedKey = null,
            onSelect = { id -> items.firstOrNull { it.id == id }?.let(actions.onAddItem); showItems = false },
            onDismiss = { showItems = false },
            searchable = true,
        )
    }
    if (showPlace) {
        ChoiceSheet(
            title = stringResource(R.string.picker_choose_state),
            choices = GstStateCodes.codes.sorted().map { Choice(it, Labels.state(it)?.let { r -> stringResource(r) } ?: it) },
            selectedKey = form.placeOfSupply,
            onSelect = { code -> actions.onUpdate { it.copy(placeOfSupply = code) }; showPlace = false },
            onDismiss = { showPlace = false },
            searchable = true,
        )
    }
    if (showDate) {
        val state = rememberDatePickerState(initialSelectedDateMillis = form.date * 86_400_000L)
        DatePickerDialog(
            onDismissRequest = { showDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { ms -> actions.onUpdate { it.copy(date = ms / 86_400_000L) } }
                    showDate = false
                }) { Text(stringResource(R.string.btn_ok)) }
            },
            dismissButton = { TextButton(onClick = { showDate = false }) { Text(stringResource(R.string.btn_cancel)) } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DiscountModeChips(isPercent: Boolean, onChange: (Boolean) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        FilterChip(selected = isPercent, onClick = { onChange(true) }, label = { Text(stringResource(R.string.discount_percent_short)) })
        FilterChip(selected = !isPercent, onClick = { onChange(false) }, label = { Text(stringResource(R.string.discount_amount_short)) })
    }
}

@Composable
private fun LineCard(line: LineState, total: Long?, onChange: ((LineState) -> LineState) -> Unit, onRemove: () -> Unit) {
    val qtyBad = line.qty.isNotBlank() && MoneyInput.parseQty(line.qty)?.takeIf { it > 0 } == null
    val rateBad = line.rate.isNotBlank() && MoneyInput.parsePaise(line.rate) == null
    val discountBad = line.discount.isNotBlank() &&
        (if (line.discountIsPercent) MoneyInput.parsePercentBp(line.discount) else MoneyInput.parsePaise(line.discount)) == null
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(line.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onRemove, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.line_remove))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppTextField(
                    line.qty, { v -> onChange { it.copy(qty = v) } },
                    stringResource(R.string.line_qty) + " (" + unitLabel(line.unit) + ")",
                    modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal,
                    error = if (qtyBad) R.string.err_number else null,
                )
                AppTextField(
                    line.rate, { v -> onChange { it.copy(rate = v) } }, stringResource(R.string.line_rate),
                    modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal,
                    error = if (rateBad) R.string.err_number else null,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppTextField(
                    line.discount, { v -> onChange { it.copy(discount = v) } }, stringResource(R.string.line_discount),
                    modifier = Modifier.weight(1f), keyboardType = KeyboardType.Decimal,
                    error = if (discountBad) R.string.err_number else null,
                )
                DiscountModeChips(line.discountIsPercent) { p -> onChange { it.copy(discountIsPercent = p) } }
            }
            if (total != null) {
                Text(rupees(total), style = MaterialTheme.typography.titleMedium, modifier = Modifier.align(Alignment.End))
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

/** Totals as they will be saved. CGST/SGST/IGST rows appear only when there is tax. */
@Composable
fun TotalsCard(r: GstInvoiceResult, received: Long) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            TotalRow(stringResource(R.string.total_subtotal), rupees(r.subtotal))
            if (r.discountTotal > 0) TotalRow(stringResource(R.string.total_discount), "- " + rupees(r.discountTotal))
            TotalRow(stringResource(R.string.total_taxable), rupees(r.taxableTotal))
            if (r.cgst > 0) TotalRow(stringResource(R.string.total_cgst), rupees(r.cgst))
            if (r.sgst > 0) TotalRow(stringResource(R.string.total_sgst), rupees(r.sgst))
            if (r.igst > 0) TotalRow(stringResource(R.string.total_igst), rupees(r.igst))
            if (r.otherCharges > 0) TotalRow(stringResource(R.string.total_other), rupees(r.otherCharges))
            if (r.roundOff != 0L) TotalRow(stringResource(R.string.total_roundoff), (if (r.roundOff < 0) "- " else "+ ") + rupees(kotlin.math.abs(r.roundOff)))
            HorizontalDivider(Modifier.padding(vertical = 6.dp))
            TotalRow(stringResource(R.string.total_grand), rupees(r.grandTotal), bold = true)
            if (received > 0) {
                TotalRow(stringResource(R.string.total_paid), rupees(received))
                TotalRow(stringResource(R.string.total_due), rupees((r.grandTotal - received).coerceAtLeast(0)))
            }
        }
    }
}
