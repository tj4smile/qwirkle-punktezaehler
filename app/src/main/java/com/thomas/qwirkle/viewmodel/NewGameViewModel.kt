package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.GameRules
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

    /** Prüft die Auswahl und meldet Spielerreihenfolge und Testspiel-Kennzeichen. */
    fun continueToSetup(onNext: (playerIds: List<Long>, isTestGame: Boolean) -> Unit) {
        // Nur Spieler, die es noch gibt (falls zwischenzeitlich gelöscht)
        val existing = players.value.map { it.id }.toSet()
        val ids = _selectedIds.value.filter { it in existing }
        if (ids.size !in GameRules.MIN_PLAYERS..GameRules.MAX_PLAYERS) return
        onNext(ids, _isTestGame.value)
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { NewGameViewModel(repository) } }
    }
}
