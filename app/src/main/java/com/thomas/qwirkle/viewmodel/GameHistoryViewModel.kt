package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.RoundWithScores
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.GameRules
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GameHistoryViewModel(
    private val gameId: Long,
    private val repository: GameRepository
) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getGamePlayers(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val rounds: StateFlow<List<RoundWithScores>> = repository.getRoundsWithScores(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Das Spiel selbst (Datum, Endbonus); für die Leseansicht gespeicherter Spiele. */
    val game: StateFlow<GameHistory?> = repository.getGameWithRounds(gameId)
        .map { it?.game }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Qwirkle-Bonus dieses Spiels (6 Standard, 12 Alternativ). */
    val qwirkleBonus: StateFlow<Int> = repository.getQwirkleBonus(gameId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GameRules.QWIRKLE_BONUS)

    fun deleteLastRound() {
        val last = rounds.value.lastOrNull() ?: return
        viewModelScope.launch { repository.deleteRound(last.round.id) }
    }

    companion object {
        fun factory(gameId: Long, repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { GameHistoryViewModel(gameId, repository) } }
    }
}
