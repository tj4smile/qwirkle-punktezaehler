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
import com.thomas.qwirkle.util.GameCalculations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Kurzinfo zum laufenden Spiel für die Startseite.
 * leaderName ist null, wenn noch keine Runde gespielt wurde; isTie, wenn mehrere
 * Spieler an der Spitze gleichauf sind. Punkte ohne Endspielbonus (der kommt erst
 * beim Beenden).
 */
data class RunningGameInfo(
    val rounds: Int,
    val totalPoints: Int,
    val leaderName: String?,
    val leaderPoints: Int,
    val isTie: Boolean
) {
    companion object {
        const val MAX_NAME_LENGTH = 12

        fun of(players: List<Player>, rounds: List<RoundWithScores>): RunningGameInfo {
            if (rounds.isEmpty()) return RunningGameInfo(0, 0, null, 0, false)
            val totals = GameCalculations.playerTotals(players, rounds)
            val ranking = GameCalculations.ranking(totals)
            val top = ranking.firstOrNull()?.second
            val tied = ranking.count { it.first == 1 } > 1
            return RunningGameInfo(
                rounds = rounds.size,
                totalPoints = totals.sumOf { it.points },
                leaderName = top?.player?.name?.shorten(),
                leaderPoints = top?.points ?: 0,
                isTie = tied
            )
        }

        private fun String.shorten(): String =
            if (length > MAX_NAME_LENGTH) take(MAX_NAME_LENGTH - 1) + "…" else this
    }
}

class HomeViewModel(private val repository: GameRepository) : ViewModel() {

    val lastGame: StateFlow<GameHistory?> = repository.getLastFinishedGame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val gameCount: StateFlow<Int> = repository.gameCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val unfinishedGame: StateFlow<GameHistory?> = repository.getUnfinishedGame()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val runningInfo: StateFlow<RunningGameInfo?> = repository.getUnfinishedGame()
        .flatMapLatest { game ->
            if (game == null) {
                flowOf(null)
            } else {
                combine(
                    repository.getGamePlayers(game.id),
                    repository.getRoundsWithScores(game.id)
                ) { players, rounds -> RunningGameInfo.of(players, rounds) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun discardUnfinishedGame() {
        val game = unfinishedGame.value ?: return
        viewModelScope.launch { repository.discardGame(game.id) }
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { HomeViewModel(repository) } }
    }
}
