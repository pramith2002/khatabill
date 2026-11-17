package com.teczo.khatabill.util.dates

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Indian date format, dd/MM/yyyy, with Latin digits (the default for amounts and dates). */
object DateFormat {
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)

    fun format(epochDay: Long): String = LocalDate.ofEpochDay(epochDay).format(formatter)

    fun today(): Long = LocalDate.now().toEpochDay()
}
