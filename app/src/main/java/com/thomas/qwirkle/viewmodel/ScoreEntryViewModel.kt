package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.createSavedStateHandle
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.ScoreEntry
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.ScoreInputLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val roundId: Long? = null,
    private val savedState: SavedStateHandle = SavedStateHandle()
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

    /** Qwirkles aller gespeicherten Runden des Spiels (beim Bearbeiten ohne die bearbeitete Runde). */
    val savedQwirkles: StateFlow<Int> = repository.getRoundsWithScores(gameId)
        .map { rounds ->
            rounds.filter { it.round.id != roundId }.sumOf { r -> r.scores.sumOf { it.qwirkles } }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    // Laufende Runde: übersteht auch ein Beenden der App durch das System (SavedStateHandle)
    private val _inputs = MutableStateFlow<Map<Long, ScoreInput>>(
        if (roundId == null) restoreInputs() else emptyMap()
    )
    val inputs: StateFlow<Map<Long, ScoreInput>> = _inputs

    /** Position des Cursors (Index in der Spielerliste). */
    private val _activeIndex = MutableStateFlow(if (roundId == null) savedState.get<Int>("active") ?: 0 else 0)
    val activeIndex: StateFlow<Int> = _activeIndex

    /** Spieler, die in dieser Runde mit ✓ bestätigt wurden. */
    private val _confirmed = MutableStateFlow<Set<Long>>(
        if (roundId == null) (savedState.get<ArrayList<Long>>("confirmed") ?: arrayListOf()).toSet()
        else emptySet()
    )
    val confirmed: StateFlow<Set<Long>> = _confirmed

    // Spieler, dessen Punkte gerade getippt werden: die erste Ziffer ersetzt den alten Wert
    private var typingId: Long? = null

    /**
     * Solange in Runde 1 noch nichts eingegeben ist, darf die Reihenfolge geändert werden.
     * (Beim Bearbeiten einer Runde nie.)
     */
    val orderLocked: StateFlow<Boolean> = combine(roundCount, _inputs, _confirmed) { rounds, input, done ->
        isEditing || rounds > 0 || input.isNotEmpty() || done.isNotEmpty()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /** Verschiebt einen Spieler um [delta] Plätze (-1 = nach oben) und speichert die Reihenfolge. */
    fun movePlayer(playerId: Long, delta: Int) {
        if (orderLocked.value) return
        val ids = players.value.map { it.id }.toMutableList()
        val from = ids.indexOf(playerId)
        val to = from + delta
        if (from < 0 || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { repository.reorderPlayers(gameId, ids) }
    }

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
        updateInputs { map ->
            map + (playerId to (map[playerId] ?: ScoreInput()).copy(points = digits))
        }
    }

    fun changeQwirkles(playerId: Long, delta: Int) {
        updateInputs { map ->
            val current = map[playerId] ?: ScoreInput()
            map + (playerId to current.copy(qwirkles = (current.qwirkles + delta).coerceIn(0, MAX_QWIRKLES)))
        }
    }

    // ---------- Nummernpad ----------

    private fun activePlayerId(): Long? = players.value.getOrNull(_activeIndex.value)?.id

    /** Ziffer für den Spieler am Cursor. */
    fun pressDigit(digit: Int) {
        val id = activePlayerId() ?: return
        val replace = typingId != id
        typingId = id
        updateInputs { map ->
            val current = map[id] ?: ScoreInput()
            map + (id to current.copy(points = ScoreInputLogic.appendDigit(current.points, digit, replace)))
        }
    }

    /** ⌫: letzte Stelle des Spielers am Cursor löschen. */
    fun pressDelete() {
        val id = activePlayerId() ?: return
        typingId = id
        updateInputs { map ->
            val current = map[id] ?: ScoreInput()
            map + (id to current.copy(points = ScoreInputLogic.deleteLast(current.points)))
        }
    }

    /** Q: ein Qwirkle mehr für den Spieler am Cursor (weniger über Q−). */
    fun pressQwirkle() {
        val id = activePlayerId() ?: return
        changeQwirkles(id, +1)
    }

    /** Q−: ein Qwirkle weniger für den Spieler am Cursor. */
    fun pressQwirkleLess() {
        val id = activePlayerId() ?: return
        changeQwirkles(id, -1)
    }

    /** ✓: bestätigt den Spieler am Cursor und setzt den Cursor weiter. */
    fun pressConfirm() {
        val id = activePlayerId() ?: return
        // Ein bestätigter Spieler ohne Eingabe bekommt ausdrücklich 0 Punkte
        updateInputs { map -> if (id in map) map else map + (id to ScoreInput()) }
        _confirmed.value = _confirmed.value + id
        setCursor(ScoreInputLogic.nextCursor(_activeIndex.value, players.value.lastIndex))
        persistConfirmed()
    }

    /** Antippen einer Spielerkarte setzt den Cursor dorthin. */
    fun setCursor(index: Int) {
        _activeIndex.value = index.coerceIn(0, maxOf(players.value.lastIndex, 0))
        typingId = null
        if (roundId == null) savedState["active"] = _activeIndex.value
    }

    /** Anzahl der Spieler ohne ✓ in der laufenden Runde (beim Bearbeiten 0). */
    fun unconfirmedCount(): Int =
        if (isEditing) 0 else players.value.count { it.id !in _confirmed.value }

    private fun updateInputs(transform: (Map<Long, ScoreInput>) -> Map<Long, ScoreInput>) {
        _inputs.update(transform)
        if (roundId == null) {
            savedState["inputs"] = ArrayList(
                _inputs.value.map { (id, i) -> "$id|${i.points}|${i.qwirkles}" }
            )
        }
    }

    private fun persistConfirmed() {
        if (roundId == null) savedState["confirmed"] = ArrayList(_confirmed.value)
    }

    private fun restoreInputs(): Map<Long, ScoreInput> =
        (savedState.get<ArrayList<String>>("inputs") ?: arrayListOf()).mapNotNull { entry ->
            val parts = entry.split("|")
            val id = parts.getOrNull(0)?.toLongOrNull() ?: return@mapNotNull null
            id to ScoreInput(parts.getOrNull(1) ?: "0", parts.getOrNull(2)?.toIntOrNull() ?: 0)
        }.toMap()

    private fun resetRound() {
        _inputs.value = emptyMap()
        _confirmed.value = emptySet()
        typingId = null
        _activeIndex.value = 0
        savedState.remove<Any>("inputs")
        savedState.remove<Any>("confirmed")
        savedState["active"] = 0
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
                    // Die Erfassung bleibt auf dem Bildschirm: neue, leere Runde, Cursor beim ersten Spieler
                    resetRound()
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
            viewModelFactory {
                initializer { ScoreEntryViewModel(gameId, repository, roundId, createSavedStateHandle()) }
            }
    }
}
