package com.thomas.qwirkle.util

enum class NameValidation { VALID, EMPTY, TOO_LONG, DUPLICATE }

object Validation {
    const val MAX_NAME_LENGTH = 20

    /** Prüft nur das Format; Duplikate prüft das Repository (braucht die Datenbank). */
    fun checkFormat(name: String): NameValidation = when {
        name.isBlank() -> NameValidation.EMPTY
        name.length > MAX_NAME_LENGTH -> NameValidation.TOO_LONG
        else -> NameValidation.VALID
    }
}
