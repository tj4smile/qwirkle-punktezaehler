package com.thomas.qwirkle.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameSetupViewModelTest {

    @Test
    fun uniqueColors_noDuplicates_unchanged() {
        assertEquals(listOf(0, 3, 5), GameSetupViewModel.uniqueColors(listOf(0, 3, 5)))
    }

    @Test
    fun uniqueColors_duplicate_getsFreeColor_firstKeepsOwn() {
        assertEquals(listOf(2, 0, 1), GameSetupViewModel.uniqueColors(listOf(2, 2, 2)))
    }

    @Test
    fun uniqueColors_sevenPlayersSameColor_allDistinct() {
        val result = GameSetupViewModel.uniqueColors(List(7) { 4 })
        assertEquals(7, result.toSet().size)
        assertEquals(4, result[0])
        assertTrue(result.all { it in 0..6 })
    }

    @Test
    fun uniqueColors_empty() {
        assertEquals(emptyList<Int>(), GameSetupViewModel.uniqueColors(emptyList()))
    }
}
