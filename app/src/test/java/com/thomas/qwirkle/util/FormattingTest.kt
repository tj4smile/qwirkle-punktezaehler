package com.thomas.qwirkle.util

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FormattingTest {

    private lateinit var oldLocale: Locale
    private lateinit var oldZone: TimeZone

    @Before
    fun setUp() {
        oldLocale = Locale.getDefault()
        oldZone = TimeZone.getDefault()
        Locale.setDefault(Locale.GERMANY)
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(oldLocale)
        TimeZone.setDefault(oldZone)
    }

    private fun october26() = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.OCTOBER, 26, 12, 0, 0)
    }.timeInMillis

    @Test
    fun date_formatsAsDayMonthYear() {
        assertEquals("26.10.2026", Formatting.date(october26()))
    }

    @Test
    fun date_epochZero() {
        assertEquals("01.01.1970", Formatting.date(0L))
    }

    @Test
    fun date_inEnglish_usesLocalizedFormat() {
        Locale.setDefault(Locale.US)

        val text = Formatting.date(october26())

        assertTrue(text.contains("2026"))
        assertFalse(text.contains("26.10.2026"))
    }

    @Test
    fun percent_roundsToWholeNumbers() {
        val text = Formatting.percent(0.6)

        assertTrue(text.contains("60"))
        assertTrue(text.contains("%"))
        assertTrue(Formatting.percent(1.0 / 3).contains("33"))
        assertFalse(Formatting.percent(1.0 / 3).contains(","))
    }

    @Test
    fun decimal_usesCommaInGerman() {
        assertEquals("344,2", Formatting.decimal(344.2))
    }

    @Test
    fun decimal_alwaysOneDecimalPlace() {
        assertEquals("2,0", Formatting.decimal(2.0))
        assertEquals("0,0", Formatting.decimal(0.0))
    }

    @Test
    fun decimal_roundsToOneDecimalPlace() {
        assertEquals("2,8", Formatting.decimal(2.75))
    }

    @Test
    fun decimal_usesDotInEnglish() {
        Locale.setDefault(Locale.US)

        assertEquals("344.2", Formatting.decimal(344.2))
    }
}
