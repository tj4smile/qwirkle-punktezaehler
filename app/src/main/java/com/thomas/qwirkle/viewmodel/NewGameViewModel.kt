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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class NewGameViewModel(private val repository: GameRepository) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Reihenfolge der Auswahl = Spielreihenfolge
    private val _selectedIds = MutableStateFlow<List<Long>>(emptyList())
    val selectedIds: StateFlow<List<Long>> = _selectedIds

    private val _isTestGame = MutableStateFlow(false)
    val isTestGame: StateFlow<Boolean> = _isTestGame

    fun togglePlayer(id: Long) {
        _selectedIds.update { current ->
            when {
                id in current -> current - id
                current.size < GameRules.MAX_PLAYERS -> current + id
                else -> current
            }
        }
    }

    fun setTestGame(value: Boolean) {
        _isTestGame.value = value
    }

    /**
     * Farben der gewählten Spieler (Spieler-ID -> colorIndex) in Spielreihenfolge.
     * Doppelte Farben sind schon aufgelöst, damit die Kachel sofort die endgültige Farbe zeigt.
     */
    val colors: StateFlow<Map<Long, Int>> = combine(players, _selectedIds) { all, ids ->
        val byId = all.associateBy { it.id }
        val selected = ids.mapNotNull { byId[it] }
        val unique = PlayerColors.uniqueColors(selected.map { it.colorIndex })
        selected.map { it.id }.zip(unique).toMap()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    private val _isStarting = MutableStateFlow(false)

    /**
     * Legt das Spiel an. Geänderte (vorher doppelte) Farben werden dauerhaft am Spieler
     * gespeichert. Meldet die Spiel-ID, danach geht es direkt in die Rundenerfassung.
     */
    fun start(onStarted: (gameId: Long) -> Unit) {
        if (_isStarting.value) return
        val byId = players.value.associateBy { it.id }
        val selected = _selectedIds.value.mapNotNull { byId[it] }
        if (selected.size !in GameRules.MIN_PLAYERS..GameRules.MAX_PLAYERS) return
        val unique = PlayerColors.uniqueColors(selected.map { it.colorIndex })
        _isStarting.value = true
        viewModelScope.launch {
            try {
                selected.zip(unique)
                    .filter { (player, color) -> player.colorIndex != color }
                    .forEach { (player, color) -> repository.changePlayerColor(player, color) }
                val gameId = repository.startGame(selected.map { it.id }, _isTestGame.value)
                onStarted(gameId)
            } finally {
                _isStarting.value = false
            }
        }
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { NewGameViewModel(repository) } }
    }
}
