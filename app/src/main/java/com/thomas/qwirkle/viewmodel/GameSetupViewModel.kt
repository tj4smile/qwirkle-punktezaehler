package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.GameRules
import com.thomas.qwirkle.util.PlayerColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Ein Teilnehmer mit der Farbe, die er in diesem Spiel hat. */
data class SetupEntry(val player: Player, val colorIndex: Int)

/**
 * Bildschirm "Reihenfolge & Farben" vor der ersten Runde.
 * Das Spiel wird erst in [start] angelegt. Farbänderungen werden dauerhaft am
 * Spieler gespeichert; innerhalb des Spiels ist jede Farbe nur einmal vergeben.
 */
class GameSetupViewModel(
    private val repository: GameRepository,
    private val playerIds: List<Long>,
    private val isTestGame: Boolean
) : ViewModel() {

    private val _entries = MutableStateFlow<List<SetupEntry>>(emptyList())
    val entries: StateFlow<List<SetupEntry>> = _entries

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    /** true, wenn beim Öffnen doppelte Farben automatisch geändert wurden. */
    private val _colorsAdjusted = MutableStateFlow(false)
    val colorsAdjusted: StateFlow<Boolean> = _colorsAdjusted

    private val _isStarting = MutableStateFlow(false)
    val isStarting: StateFlow<Boolean> = _isStarting

    init {
        viewModelScope.launch {
            val byId = repository.getPlayersByIds(playerIds).associateBy { it.id }
            val players = playerIds.mapNotNull { byId[it] }
            val colors = uniqueColors(players.map { it.colorIndex })
            _colorsAdjusted.value = colors != players.map { it.colorIndex }
            _entries.value = players.zip(colors) { p, c -> SetupEntry(p, c) }
            _isLoading.value = false
        }
    }

    fun moveUp(index: Int) = move(index, index - 1)

    fun moveDown(index: Int) = move(index, index + 1)

    private fun move(from: Int, to: Int) {
        _entries.update { list ->
            if (from !in list.indices || to !in list.indices) return@update list
            list.toMutableList().apply { add(to, removeAt(from)) }
        }
    }

    /**
     * Setzt die Farbe eines Spielers. Hat sie schon ein anderer Spieler im Spiel,
     * tauschen beide ihre Farben, damit keine Farbe doppelt vorkommt.
     */
    fun setColor(playerId: Long, colorIndex: Int) {
        _entries.update { list ->
            val me = list.firstOrNull { it.player.id == playerId } ?: return@update list
            val other = list.firstOrNull { it.player.id != playerId && it.colorIndex == colorIndex }
            list.map {
                when {
                    it.player.id == playerId -> it.copy(colorIndex = colorIndex)
                    other != null && it.player.id == other.player.id ->
                        it.copy(colorIndex = me.colorIndex)
                    else -> it
                }
            }
        }
    }

    fun start(onStarted: (gameId: Long) -> Unit) {
        val list = _entries.value
        if (_isStarting.value || list.size !in GameRules.MIN_PLAYERS..GameRules.MAX_PLAYERS) return
        _isStarting.value = true
        viewModelScope.launch {
            try {
                list.filter { it.colorIndex != it.player.colorIndex }
                    .forEach { repository.changePlayerColor(it.player, it.colorIndex) }
                val gameId = repository.startGame(list.map { it.player.id }, isTestGame)
                onStarted(gameId)
            } finally {
                _isStarting.value = false
            }
        }
    }

    companion object {
        /**
         * Macht die Farben eindeutig: die erste Verwendung einer Farbe bleibt,
         * jede weitere bekommt eine noch freie. Bei höchstens [PlayerColors.COUNT]
         * Spielern gibt es immer genug freie Farben.
         */
        fun uniqueColors(colors: List<Int>): List<Int> {
            val seen = mutableSetOf<Int>()
            val isDuplicate = colors.map { !seen.add(it) }
            val free = (0 until PlayerColors.COUNT).filter { it !in seen }.iterator()
            return colors.mapIndexed { i, c -> if (isDuplicate[i] && free.hasNext()) free.next() else c }
        }

        fun factory(
            repository: GameRepository,
            playerIds: List<Long>,
            isTestGame: Boolean
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer { GameSetupViewModel(repository, playerIds, isTestGame) }
        }
    }
}
