package com.teczo.khatabill.ui.items

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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.GstRates
import com.teczo.khatabill.domain.model.ItemType
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.Choice
import com.teczo.khatabill.ui.components.ChoiceSheet
import com.teczo.khatabill.ui.components.PickerField
import com.teczo.khatabill.ui.components.RadioRow
import com.teczo.khatabill.ui.components.ScreenHeader
import com.teczo.khatabill.ui.util.unitLabel
import com.teczo.khatabill.util.money.MoneyInput

@Composable
fun ItemEditScreen(onDone: () -> Unit, viewModel: ItemEditViewModel = hiltViewModel()) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val errors by viewModel.errors.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val business by viewModel.business.collectAsStateWithLifecycle()
    var showUnit by remember { mutableStateOf(false) }
    var showRate by remember { mutableStateOf(false) }
    var showCategory by remember { mutableStateOf(false) }

    LaunchedEffect(done) { if (done) onDone() }
    val chargesGst = business != null && viewModel.chargesGst
    val isProduct = form.type == ItemType.PRODUCT

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        ScreenHeader(stringResource(if (viewModel.isNew) R.string.item_title_new else R.string.item_title_edit), onDone)
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AppTextField(form.name, { v -> viewModel.update { it.copy(name = v) } }, stringResource(R.string.field_item_name), error = errors[ItemField.NAME])
            AppTextField(form.nameKn, { v -> viewModel.update { it.copy(nameKn = v) } }, stringResource(R.string.field_name_kn))

            Text(stringResource(R.string.field_item_type), style = MaterialTheme.typography.titleMedium)
            Row {
                RadioRow(stringResource(R.string.itype_product), isProduct, { viewModel.update { it.copy(type = ItemType.PRODUCT) } }, Modifier.weight(1f))
                RadioRow(stringResource(R.string.itype_service), !isProduct, { viewModel.update { it.copy(type = ItemType.SERVICE) } }, Modifier.weight(1f))
            }

            PickerField(stringResource(R.string.field_unit), if (form.unit.isBlank()) "" else unitLabel(form.unit), { showUnit = true }, error = errors[ItemField.UNIT])
            AppTextField(
                form.salePrice, { v -> viewModel.update { it.copy(salePrice = v) } }, stringResource(R.string.field_sale_price),
                error = errors[ItemField.SALE], keyboardType = KeyboardType.Decimal,
            )
            AppTextField(
                form.purchasePrice, { v -> viewModel.update { it.copy(purchasePrice = v) } }, stringResource(R.string.field_purchase_price),
                error = errors[ItemField.PURCHASE], keyboardType = KeyboardType.Decimal,
            )
            AppTextField(
                form.mrp, { v -> viewModel.update { it.copy(mrp = v) } }, stringResource(R.string.field_mrp),
                error = errors[ItemField.MRP], keyboardType = KeyboardType.Decimal,
            )

            if (chargesGst) {
                PickerField(
                    stringResource(R.string.field_gst_rate),
                    stringResource(R.string.percent_value, MoneyInput.bpToText(form.gstRateBp)),
                    { showRate = true },
                )
                Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.field_tax_inclusive), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = form.taxInclusive, onCheckedChange = { c -> viewModel.update { it.copy(taxInclusive = c) } })
                }
                AppTextField(form.hsn, { v -> viewModel.update { it.copy(hsn = v.filter(Char::isDigit).take(8)) } }, stringResource(R.string.field_hsn), keyboardType = KeyboardType.Number)
            }

            if (isProduct) {
                if (viewModel.isNew) {
                    AppTextField(
                        form.openingStock, { v -> viewModel.update { it.copy(openingStock = v) } }, stringResource(R.string.field_opening_stock),
                        error = errors[ItemField.OPENING], keyboardType = KeyboardType.Decimal,
                    )
                }
                AppTextField(
                    form.minStock, { v -> viewModel.update { it.copy(minStock = v) } }, stringResource(R.string.field_min_stock),
                    error = errors[ItemField.MIN_STOCK], keyboardType = KeyboardType.Decimal,
                )
                AppTextField(
                    form.barcode, { v -> viewModel.update { it.copy(barcode = v.trim()) } }, stringResource(R.string.field_barcode),
                    error = errors[ItemField.BARCODE], capitalization = KeyboardCapitalization.None,
                )
            }
            AppTextField(form.sku, { v -> viewModel.update { it.copy(sku = v) } }, stringResource(R.string.field_sku), capitalization = KeyboardCapitalization.Characters)
            PickerField(
                stringResource(R.string.field_category),
                categories.firstOrNull { it.id == form.categoryId }?.name ?: stringResource(R.string.category_none),
                { showCategory = true },
            )
        }
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onDone, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(R.string.btn_cancel)) }
            Button(onClick = viewModel::save, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) { Text(stringResource(R.string.btn_save)) }
        }
    }

    if (showUnit) {
        ChoiceSheet(
            title = stringResource(R.string.field_unit),
            choices = viewModel.unitChoices().map { Choice(it, unitLabel(it)) },
            selectedKey = form.unit,
            onSelect = { u -> viewModel.update { it.copy(unit = u) }; showUnit = false },
            onDismiss = { showUnit = false },
        )
    }
    if (showRate) {
        ChoiceSheet(
            title = stringResource(R.string.field_gst_rate),
            choices = GstRates.options.map { Choice(it.toString(), stringResource(R.string.percent_value, MoneyInput.bpToText(it))) },
            selectedKey = form.gstRateBp.toString(),
            onSelect = { bp -> viewModel.update { it.copy(gstRateBp = bp.toInt()) }; showRate = false },
            onDismiss = { showRate = false },
        )
    }
    if (showCategory) {
        var newName by remember { mutableStateOf("") }
        ChoiceSheet(
            title = stringResource(R.string.field_category),
            choices = listOf(Choice("", stringResource(R.string.category_none))) + categories.map { Choice(it.id, it.name) },
            selectedKey = form.categoryId ?: "",
            onSelect = { id -> viewModel.update { it.copy(categoryId = id.ifBlank { null }) }; showCategory = false },
            onDismiss = { showCategory = false },
            footer = {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppTextField(newName, { newName = it }, stringResource(R.string.category_new_hint), modifier = Modifier.weight(1f))
                    Button(
                        onClick = { viewModel.addCategory(newName); newName = ""; showCategory = false },
                        modifier = Modifier.heightIn(min = 56.dp),
                    ) { Text(stringResource(R.string.btn_add)) }
                }
            },
        )
    }
}
