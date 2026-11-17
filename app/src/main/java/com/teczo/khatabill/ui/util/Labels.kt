package com.teczo.khatabill.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.teczo.khatabill.R
import com.teczo.khatabill.domain.model.BusinessType
import com.teczo.khatabill.domain.model.DocumentType
import com.teczo.khatabill.domain.model.GstScheme
import com.teczo.khatabill.domain.model.InvoiceStatus

/** Maps stored codes (units, GST state codes, enums) to localized string resources. */
object Labels {
    private val units = mapOf(
        "kg" to R.string.unit_kg, "g" to R.string.unit_g, "pcs" to R.string.unit_pcs,
        "box" to R.string.unit_box, "dozen" to R.string.unit_dozen, "metre" to R.string.unit_metre,
        "litre" to R.string.unit_litre, "bundle" to R.string.unit_bundle,
        "carton" to R.string.unit_carton, "pack" to R.string.unit_pack,
    )

    private val states = mapOf(
        "01" to R.string.state_01, "02" to R.string.state_02, "03" to R.string.state_03,
        "04" to R.string.state_04, "05" to R.string.state_05, "06" to R.string.state_06,
        "07" to R.string.state_07, "08" to R.string.state_08, "09" to R.string.state_09,
        "10" to R.string.state_10, "11" to R.string.state_11, "12" to R.string.state_12,
        "13" to R.string.state_13, "14" to R.string.state_14, "15" to R.string.state_15,
        "16" to R.string.state_16, "17" to R.string.state_17, "18" to R.string.state_18,
        "19" to R.string.state_19, "20" to R.string.state_20, "21" to R.string.state_21,
        "22" to R.string.state_22, "23" to R.string.state_23, "24" to R.string.state_24,
        "26" to R.string.state_26, "27" to R.string.state_27, "28" to R.string.state_28,
        "29" to R.string.state_29, "30" to R.string.state_30, "31" to R.string.state_31,
        "32" to R.string.state_32, "33" to R.string.state_33, "34" to R.string.state_34,
        "35" to R.string.state_35, "36" to R.string.state_36, "37" to R.string.state_37,
        "38" to R.string.state_38, "97" to R.string.state_97, "99" to R.string.state_99,
    )

    @StringRes fun unit(code: String): Int? = units[code]

    @StringRes fun state(code: String): Int? = states[code]

    @StringRes fun businessType(type: BusinessType): Int = when (type) {
        BusinessType.AGARBATTI -> R.string.btype_agarbatti
        BusinessType.SILK_HANDICRAFTS -> R.string.btype_silk
        BusinessType.FOOD_SWEETS -> R.string.btype_food
        BusinessType.GARMENTS -> R.string.btype_garments
        BusinessType.GENERAL_TRADER -> R.string.btype_general
        BusinessType.MANUFACTURER -> R.string.btype_manufacturer
    }

    /** Tab / list label for a document type. */
    @StringRes fun docType(type: DocumentType): Int = when (type) {
        DocumentType.SALE -> R.string.doc_sale
        DocumentType.PURCHASE -> R.string.doc_purchase
        DocumentType.QUOTATION -> R.string.doc_quotation
        else -> R.string.doc_sale
    }

    /** The big title on a bill. A SALE with series "BOS" was issued by a business that charges no tax. */
    @StringRes fun docTitle(type: DocumentType, series: String): Int = when (type) {
        DocumentType.SALE -> if (series == "BOS") R.string.doc_title_bill_of_supply else R.string.doc_title_tax_invoice
        DocumentType.QUOTATION -> R.string.doc_title_quotation
        DocumentType.PURCHASE -> R.string.doc_title_purchase
        else -> R.string.doc_title_tax_invoice
    }

    @StringRes fun status(status: InvoiceStatus): Int = when (status) {
        InvoiceStatus.DRAFT -> R.string.status_draft
        InvoiceStatus.UNPAID -> R.string.status_unpaid
        InvoiceStatus.PARTIAL -> R.string.status_partial
        InvoiceStatus.PAID -> R.string.status_paid
        InvoiceStatus.CANCELLED -> R.string.status_cancelled
    }

    @StringRes fun gstScheme(scheme: GstScheme): Int = when (scheme) {
        GstScheme.REGULAR -> R.string.gst_regular
        GstScheme.COMPOSITION -> R.string.gst_composition
        GstScheme.UNREGISTERED -> R.string.gst_none
    }
}

@Composable
fun unitLabel(code: String): String = Labels.unit(code)?.let { stringResource(it) } ?: code

@Composable
fun stateLabel(code: String): String = Labels.state(code)?.let { stringResource(it) } ?: code
