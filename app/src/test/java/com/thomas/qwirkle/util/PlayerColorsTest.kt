package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerColorsTest {

    @Test
    fun count_isSeven() {
        assertEquals(7, PlayerColors.COUNT)
    }

    @Test
    fun allSevenColors_areDistinct() {
        val colors = (0 until PlayerColors.COUNT).map { PlayerColors.argb(it) }.toSet()

        assertEquals(7, colors.size)
    }

    @Test
    fun argb_wrapsAroundAtCount() {
        assertEquals(PlayerColors.argb(0), PlayerColors.argb(7))
        assertEquals(PlayerColors.argb(3), PlayerColors.argb(10))
    }

    @Test
    fun argb_negativeIndex_wrapsFromTheEnd() {
        assertEquals(PlayerColors.argb(6), PlayerColors.argb(-1))
    }

    @Test
    fun nextFreeIndex_nothingUsed_isZero() {
        assertEquals(0, PlayerColors.nextFreeIndex(emptySet()))
    }

    @Test
    fun nextFreeIndex_picksFirstUnusedColor() {
        assertEquals(2, PlayerColors.nextFreeIndex(setOf(0, 1)))
    }

    @Test
    fun nextFreeIndex_fillsGaps() {
        assertEquals(0, PlayerColors.nextFreeIndex(setOf(1, 2, 3)))
        assertEquals(1, PlayerColors.nextFreeIndex(setOf(0, 2)))
    }

    @Test
    fun nextFreeIndex_allUsed_startsOver() {
        assertEquals(0, PlayerColors.nextFreeIndex((0 until PlayerColors.COUNT).toSet()))
    }
}
