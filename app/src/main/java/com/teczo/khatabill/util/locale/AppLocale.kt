package com.teczo.khatabill.util.locale

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/** Switches the app language. The activity is recreated (not the process), so no manual restart. */
object AppLocale {
    fun apply(languageTag: String) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageTag))
    }
}
