package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ValidationTest {

    @Test
    fun maxNameLength_isTwenty() {
        assertEquals(20, Validation.MAX_NAME_LENGTH)
    }

    @Test
    fun checkFormat_normalName_isValid() {
        assertEquals(NameValidation.VALID, Validation.checkFormat("Anna"))
    }

    @Test
    fun checkFormat_emptyString_isEmpty() {
        assertEquals(NameValidation.EMPTY, Validation.checkFormat(""))
    }

    @Test
    fun checkFormat_onlyWhitespace_isEmpty() {
        assertEquals(NameValidation.EMPTY, Validation.checkFormat("   "))
    }

    @Test
    fun checkFormat_exactlyMaxLength_isValid() {
        assertEquals(NameValidation.VALID, Validation.checkFormat("a".repeat(20)))
    }

    @Test
    fun checkFormat_oneOverMaxLength_isTooLong() {
        assertEquals(NameValidation.TOO_LONG, Validation.checkFormat("a".repeat(21)))
    }

    @Test
    fun checkFormat_umlautsAndSpaces_areValid() {
        assertEquals(NameValidation.VALID, Validation.checkFormat("Jörg Müller"))
    }
}
