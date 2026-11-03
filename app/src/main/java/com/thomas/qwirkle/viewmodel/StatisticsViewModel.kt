package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.model.StatisticsSummary
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.PlayerStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StatisticsViewModel(private val repository: GameRepository) : ViewModel() {

    val stats: StateFlow<StatisticsSummary?> = repository.getStatistics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val games: StateFlow<List<GameHistory>> = repository.getFinishedGames()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val playerStats: StateFlow<List<PlayerStats>> = repository.getPlayerStatistics()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun deleteGame(gameId: Long) {
        viewModelScope.launch { repository.deleteGame(gameId) }
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { StatisticsViewModel(repository) } }
    }
}
