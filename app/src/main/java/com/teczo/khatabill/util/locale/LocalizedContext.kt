package com.teczo.khatabill.util.locale

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

object LocalizedContext {
    /** A context whose string resources use [tag] ("kn", "en", "hi"), independent of the app language. */
    fun of(context: Context, tag: String): Context {
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        return context.createConfigurationContext(config)
    }

    /** The language tag the user chose for the app (falls back to the phone's language). Safe to call from a background worker. */
    fun appLanguageTag(): String =
        AppCompatDelegate.getApplicationLocales().takeIf { !it.isEmpty }?.get(0)?.language ?: Locale.getDefault().language

    fun forApp(context: Context): Context = of(context, appLanguageTag())
}
