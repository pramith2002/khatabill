package com.teczo.khatabill

import android.content.Context
import android.content.res.Configuration
import com.teczo.khatabill.data.entities.BusinessEntity
import com.teczo.khatabill.domain.model.GstScheme
import java.util.Locale

/** A Context whose resources use [tag] ("en", "kn", "hi"), like the app does after the user picks a language. */
fun Context.localized(tag: String): Context {
    val config = Configuration(resources.configuration)
    config.setLocale(Locale.forLanguageTag(tag))
    return createConfigurationContext(config)
}

fun testBusiness(scheme: GstScheme = GstScheme.REGULAR) = BusinessEntity(
    id = "b1", businessId = "b1", createdAt = 1, updatedAt = 1,
    name = "ಶ್ರೀ ಗಣೇಶ ಅಗರಬತ್ತಿ Sri Ganesh Agarbatti",
    ownerName = "Ramesh", phone = "9845012345",
    address = "ದೇವರಾಜ ಮೊಹಲ್ಲಾ, 12th Cross", city = "Mysuru", state = "Karnataka", stateCode = "29", pincode = "570001",
    gstin = if (scheme == GstScheme.REGULAR) "29AAGCB7383J1Z4" else null,
    gstScheme = scheme, upiId = "ramesh@okhdfcbank", bankName = "State Bank of India",
    accountNo = "123456789012", ifsc = "SBIN0001234", invoicePrefix = "",
)
