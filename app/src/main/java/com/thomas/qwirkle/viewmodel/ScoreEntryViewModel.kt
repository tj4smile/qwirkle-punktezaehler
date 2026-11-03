package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.ScoreEntry
import com.thomas.qwirkle.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Eingabe eines Spielers in der aktuellen Runde. points bleibt Text, bis er gespeichert wird. Start: "0". */
data class ScoreInput(
    val points: String = "0",
    val qwirkles: Int = 0
)

/**
 * Punkteerfassung. Ohne roundId wird eine neue Runde erfasst, mit roundId werden die
 * Werte einer bereits gespeicherten Runde geladen und beim Speichern geändert.
 */
class ScoreEntryViewModel(
    private val gameId: Long,
    private val repository: GameRepository,
    private val roundId: Long? = null
) : ViewModel() {

    val isEditing: Boolean = roundId != null

    /** Im Bearbeiten-Modus: true, bis die gespeicherten Werte geladen sind (vorher nicht speichern). */
    private val _isLoading = MutableStateFlow(roundId != null)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** Nummer der bearbeiteten Runde (nur im Bearbeiten-Modus, nach dem Laden). */
    private val _editedRoundNumber = MutableStateFlow<Int?>(null)
    val editedRoundNumber: StateFlow<Int?> = _editedRoundNumber

    val players: StateFlow<List<Player>> = repository.getGamePlayers(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Anzahl bereits gespeicherter Runden (aktuelle Runde = roundCount + 1). */
    val roundCount: StateFlow<Int> = repository.getRoundsWithScores(gameId)
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _inputs = MutableStateFlow<Map<Long, ScoreInput>>(emptyMap())
    val inputs: StateFlow<Map<Long, ScoreInput>> = _inputs

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving

    init {
        if (roundId != null) {
            viewModelScope.launch {
                try {
                    repository.getRoundWithScoresOnce(roundId)?.let { loaded ->
                        _editedRoundNumber.value = loaded.round.roundNumber
                        _inputs.value = loaded.scores.associate {
                            it.playerId to ScoreInput(it.points.toString(), it.qwirkles)
                        }
                    }
                } finally {
                    _isLoading.value = false
                }
            }
        }
    }

    fun setPoints(playerId: Long, text: String) {
        val digits = text.filter { it.isDigit() }.take(MAX_POINT_DIGITS)
        _inputs.update { map ->
            map + (playerId to (map[playerId] ?: ScoreInput()).copy(points = digits))
        }
    }

    fun changeQwirkles(playerId: Long, delta: Int) {
        _inputs.update { map ->
            val current = map[playerId] ?: ScoreInput()
            map + (playerId to current.copy(qwirkles = (current.qwirkles + delta).coerceIn(0, MAX_QWIRKLES)))
        }
    }

    /** Speichert die Runde. Spieler ohne Eingabe zählen mit 0 Punkten und 0 Qwirkles. */
    fun save(onSaved: () -> Unit) {
        if (_isSaving.value || _isLoading.value) return
        val entries = players.value.map { player ->
            val input = _inputs.value[player.id] ?: ScoreInput()
            val points = input.points.toIntOrNull() ?: return
            ScoreEntry(player.id, points, input.qwirkles)
        }
        if (entries.isEmpty()) return

        _isSaving.value = true
        viewModelScope.launch {
            try {
                if (roundId == null) {
                    repository.saveRound(gameId, entries)
                } else {
                    try {
                        repository.updateRound(roundId, entries)
                    } catch (e: IllegalStateException) {
                        // Spiel wurde inzwischen abgeschlossen: nichts ändern, zurück zum Verlauf
                    } catch (e: IllegalArgumentException) {
                        // Runde gibt es nicht mehr: ebenfalls zurück zum Verlauf
                    }
                }
                onSaved()
            } finally {
                _isSaving.value = false
            }
        }
    }

    companion object {
        const val MAX_POINT_DIGITS = 2
        const val MAX_QWIRKLES = 3

        fun factory(
            gameId: Long,
            repository: GameRepository,
            roundId: Long? = null
        ): ViewModelProvider.Factory =
            viewModelFactory { initializer { ScoreEntryViewModel(gameId, repository, roundId) } }
    }
}
