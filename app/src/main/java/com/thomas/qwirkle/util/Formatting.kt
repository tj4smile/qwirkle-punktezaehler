package com.thomas.qwirkle.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Formatting {

    /** Deutsch: 26.10.2026. Andere Sprachen: landesübliches Datum (z. B. Oct 26, 2026). */
    fun date(millis: Long): String {
        val locale = Locale.getDefault()
        val format = if (locale.language == "de") {
            SimpleDateFormat("dd.MM.yyyy", locale)
        } else {
            DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        }
        return format.format(Date(millis))
    }

    /** Anteil 0.0 bis 1.0 als Prozent, landesüblich (z. B. "60 %" bzw. "60%"). */
    fun percent(fraction: Double): String =
        java.text.NumberFormat.getPercentInstance(Locale.getDefault()).apply {
            maximumFractionDigits = 0
        }.format(fraction)

    /** Eine Nachkommastelle, passend zur Geräte-Sprache (z. B. 344,2 bzw. 344.2). */
    fun decimal(value: Double): String =
        String.format(Locale.getDefault(), "%.1f", value)
}
