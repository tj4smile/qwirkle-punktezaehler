package com.thomas.qwirkle.util

import org.junit.Assert.assertEquals
import org.junit.Test

class ScoreInputLogicTest {
    @Test fun firstDigitReplacesZero() = assertEquals("7", ScoreInputLogic.appendDigit("0", 7, false))
    @Test fun secondDigitAppends() = assertEquals("73", ScoreInputLogic.appendDigit("7", 3, false))
    @Test fun maxTwoDigits() = assertEquals("73", ScoreInputLogic.appendDigit("73", 5, false))
    @Test fun replaceIgnoresOldValue() = assertEquals("4", ScoreInputLogic.appendDigit("17", 4, true))
    @Test fun zeroStaysZero() = assertEquals("0", ScoreInputLogic.appendDigit("0", 0, false))
    @Test fun zeroAfterDigitAppends() = assertEquals("50", ScoreInputLogic.appendDigit("5", 0, false))
    @Test fun noLeadingZero() = assertEquals("0", ScoreInputLogic.appendDigit("5", 0, true))
    @Test fun deleteLastDigit() = assertEquals("1", ScoreInputLogic.deleteLast("17"))
    @Test fun deleteSingleGivesZero() = assertEquals("0", ScoreInputLogic.deleteLast("7"))
    @Test fun deleteZeroStaysZero() = assertEquals("0", ScoreInputLogic.deleteLast("0"))
    @Test fun cursorMovesOn() = assertEquals(2, ScoreInputLogic.nextCursor(1, 3))
    @Test fun cursorStaysAtLast() = assertEquals(3, ScoreInputLogic.nextCursor(3, 3))
    @Test fun cursorEmptyList() = assertEquals(0, ScoreInputLogic.nextCursor(0, -1))
}
