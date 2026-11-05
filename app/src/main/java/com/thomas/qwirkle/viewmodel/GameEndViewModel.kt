package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.repository.GameRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameEndViewModel(
    private val gameId: Long,
    private val repository: GameRepository
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getGamePlayers(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // null = noch nichts gewählt, NO_BONUS = bewusst niemand, sonst Spieler-ID
    private val _choice = MutableStateFlow<Long?>(null)
    val choice: StateFlow<Long?> = _choice

    init {
        // Falls man aus der Endauswertung zurückkommt: frühere Wahl vorauswählen
        viewModelScope.launch {
            repository.getGame(gameId)?.endBonusPlayerId?.let { _choice.value = it }
        }
    }

    /** Antippen wählt aus, nochmaliges Antippen hebt die Wahl wieder auf. */
    fun select(id: Long) {
        _choice.value = if (_choice.value == id) null else id
    }

    fun confirm(onDone: () -> Unit) {
        val selected = _choice.value ?: return
        viewModelScope.launch {
            repository.setEndBonusPlayer(gameId, if (selected == NO_BONUS) null else selected)
            onDone()
        }
    }

    companion object {
        const val NO_BONUS = -1L

        fun factory(gameId: Long, repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { GameEndViewModel(gameId, repository) } }
    }
}
