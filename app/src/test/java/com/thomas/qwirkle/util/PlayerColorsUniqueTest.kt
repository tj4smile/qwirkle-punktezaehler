package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerColorsUniqueTest {

    @Test
    fun uniqueColors_noDuplicates_unchanged() {
        assertEquals(listOf(0, 3, 5), PlayerColors.uniqueColors(listOf(0, 3, 5)))
    }

    @Test
    fun uniqueColors_duplicate_getsFreeColor_firstKeepsOwn() {
        assertEquals(listOf(2, 0, 1), PlayerColors.uniqueColors(listOf(2, 2, 2)))
    }

    @Test
    fun uniqueColors_sevenPlayersSameColor_allDistinct() {
        val result = PlayerColors.uniqueColors(List(7) { 4 })
        assertEquals(7, result.toSet().size)
        assertEquals(4, result[0])
        assertTrue(result.all { it in 0..6 })
    }

    @Test
    fun uniqueColors_empty() {
        assertEquals(emptyList<Int>(), PlayerColors.uniqueColors(emptyList()))
    }
}
