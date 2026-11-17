package com.teczo.khatabill.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.BusinessType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.ui.components.AppTextField
import com.teczo.khatabill.ui.components.Choice
import com.teczo.khatabill.ui.components.ChoiceSheet
import com.teczo.khatabill.ui.components.PickerField
import com.teczo.khatabill.ui.components.RadioRow
import com.teczo.khatabill.ui.util.Labels
import com.teczo.khatabill.util.validators.GstStateCodes
import com.teczo.khatabill.util.validators.GstinValidator

private fun FieldError.res(): Int = when (this) {
    FieldError.REQUIRED -> R.string.err_required
    FieldError.MOBILE -> R.string.err_mobile
    FieldError.PINCODE -> R.string.err_pincode
    FieldError.GSTIN_LENGTH -> R.string.err_gstin_length
    FieldError.GSTIN_FORMAT -> R.string.err_gstin_format
    FieldError.GSTIN_STATE -> R.string.err_gstin_state
    FieldError.GSTIN_CHECKSUM -> R.string.err_gstin_checksum
    FieldError.PAN -> R.string.err_pan
    FieldError.UPI -> R.string.err_upi
    FieldError.ACCOUNT_NO -> R.string.err_account_no
    FieldError.IFSC -> R.string.err_ifsc
    FieldError.PREFIX -> R.string.err_prefix
}

@StringRes
private fun Map<SetupField, FieldError>.res(field: SetupField): Int? = this[field]?.res()

@Composable
fun StepShop(form: SetupForm, errors: Map<SetupField, FieldError>, update: ((SetupForm) -> SetupForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(form.shopName, { v -> update { it.copy(shopName = v) } }, stringResource(R.string.field_shop_name), error = errors.res(SetupField.SHOP_NAME))
        AppTextField(form.ownerName, { v -> update { it.copy(ownerName = v) } }, stringResource(R.string.field_owner_name), error = errors.res(SetupField.OWNER_NAME))
        AppTextField(
            form.mobile, { v -> update { it.copy(mobile = v.filter(Char::isDigit).take(10)) } },
            stringResource(R.string.field_mobile), error = errors.res(SetupField.MOBILE), keyboardType = KeyboardType.Phone,
        )
        AppTextField(form.address, { v -> update { it.copy(address = v) } }, stringResource(R.string.field_address), error = errors.res(SetupField.ADDRESS))
        AppTextField(form.city, { v -> update { it.copy(city = v) } }, stringResource(R.string.field_city), error = errors.res(SetupField.CITY))
        AppTextField(
            form.pincode, { v -> update { it.copy(pincode = v.filter(Char::isDigit).take(6)) } },
            stringResource(R.string.field_pincode), error = errors.res(SetupField.PINCODE), keyboardType = KeyboardType.Number,
        )
    }
}

@Composable
fun StepGstAndType(form: SetupForm, errors: Map<SetupField, FieldError>, update: ((SetupForm) -> SetupForm) -> Unit) {
    var showState by remember { mutableStateOf(false) }
    var showType by remember { mutableStateOf(false) }
    val gstinValid = GstinValidator.validate(form.gstin) is GstinValidator.Result.Valid

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.gst_choice_title), style = MaterialTheme.typography.titleMedium)
        GstScheme.entries.forEach { scheme ->
            RadioRow(
                label = stringResource(Labels.gstScheme(scheme)),
                selected = form.gstScheme == scheme,
                onClick = { update { it.copy(gstScheme = scheme) } },
            )
        }
        if (form.chargesGst) {
            AppTextField(
                form.gstin, { v -> update { it.copy(gstin = v.take(15)) } }, stringResource(R.string.field_gstin),
                error = errors.res(SetupField.GSTIN), capitalization = KeyboardCapitalization.Characters,
            )
        } else {
            AppTextField(
                form.pan, { v -> update { it.copy(pan = v.take(10).uppercase()) } }, stringResource(R.string.field_pan),
                error = errors.res(SetupField.PAN), capitalization = KeyboardCapitalization.Characters,
            )
        }
        PickerField(
            label = stringResource(R.string.field_state),
            value = Labels.state(form.stateCode)?.let { stringResource(it) } ?: "",
            onClick = { showState = true },
            error = errors.res(SetupField.STATE),
            enabled = !gstinValid,
            supportingText = if (gstinValid) stringResource(R.string.state_from_gstin) else null,
        )
        PickerField(
            label = stringResource(R.string.field_business_type),
            value = stringResource(Labels.businessType(form.businessType)),
            onClick = { showType = true },
        )
    }

    if (showState) {
        val choices = GstStateCodes.codes.sorted().map { Choice(it, Labels.state(it)?.let { r -> stringResource(r) } ?: it) }
        ChoiceSheet(
            title = stringResource(R.string.picker_choose_state),
            choices = choices,
            selectedKey = form.stateCode.ifBlank { null },
            onSelect = { code -> update { it.copy(stateCode = code) }; showState = false },
            onDismiss = { showState = false },
            searchable = true,
        )
    }
    if (showType) {
        val choices = BusinessType.entries.map { Choice(it.name, stringResource(Labels.businessType(it))) }
        ChoiceSheet(
            title = stringResource(R.string.field_business_type),
            choices = choices,
            selectedKey = form.businessType.name,
            onSelect = { key -> update { it.copy(businessType = BusinessType.valueOf(key)) }; showType = false },
            onDismiss = { showType = false },
        )
    }
}

@Composable
fun StepPayments(form: SetupForm, errors: Map<SetupField, FieldError>, update: ((SetupForm) -> SetupForm) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AppTextField(
            form.upiId, { v -> update { it.copy(upiId = v.trim()) } }, stringResource(R.string.field_upi),
            error = errors.res(SetupField.UPI), capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Email,
        )
        AppTextField(form.bankName, { v -> update { it.copy(bankName = v) } }, stringResource(R.string.field_bank_name))
        AppTextField(
            form.accountNo, { v -> update { it.copy(accountNo = v.filter(Char::isDigit).take(20)) } },
            stringResource(R.string.field_account_no), error = errors.res(SetupField.ACCOUNT_NO), keyboardType = KeyboardType.Number,
        )
        AppTextField(
            form.ifsc, { v -> update { it.copy(ifsc = v.take(11).uppercase()) } }, stringResource(R.string.field_ifsc),
            error = errors.res(SetupField.IFSC), capitalization = KeyboardCapitalization.Characters,
        )
        AppTextField(
            form.prefix, { v -> update { it.copy(prefix = v.take(2).uppercase()) } }, stringResource(R.string.field_prefix),
            error = errors.res(SetupField.PREFIX), capitalization = KeyboardCapitalization.Characters,
        )
    }
}
