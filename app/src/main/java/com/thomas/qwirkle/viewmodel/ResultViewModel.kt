package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.GameCalculations
import com.thomas.qwirkle.util.PlayerTotal
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ResultState(
    val game: GameHistory,
    val ranking: List<Pair<Int, PlayerTotal>>
)

class ResultViewModel(
    private val gameId: Long,
    private val repository: GameRepository
) : ViewModel() {

    val state: StateFlow<ResultState?> = combine(
        repository.getGamePlayers(gameId),
        repository.getGameWithRounds(gameId).filterNotNull()
    ) { players, gameWithRounds ->
        val totals = GameCalculations.playerTotals(
            players,
            gameWithRounds.rounds,
            gameWithRounds.game.endBonusPlayerId
        )
        ResultState(gameWithRounds.game, GameCalculations.ranking(totals))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** "Spiel speichern": schließt das Spiel ab und nimmt es in die Statistik auf. */
    fun save(onDone: () -> Unit) {
        val game = state.value?.game ?: return
        viewModelScope.launch {
            repository.finishGame(game.id, game.endBonusPlayerId)
            onDone()
        }
    }

    fun discard(onDone: () -> Unit) {
        viewModelScope.launch {
            repository.discardGame(gameId)
            onDone()
        }
    }

    companion object {
        fun factory(gameId: Long, repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { ResultViewModel(gameId, repository) } }
    }
}
