package com.thomas.qwirkle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.repository.GameRepository
import com.thomas.qwirkle.util.NameValidation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlayerViewModel(private val repository: GameRepository) : ViewModel() {

    val players: StateFlow<List<Player>> = repository.getPlayers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** onResult wird mit dem Ergebnis der Namensprüfung aufgerufen (VALID = gespeichert). */
    fun addPlayer(name: String, onResult: (NameValidation) -> Unit) {
        viewModelScope.launch { onResult(repository.addPlayer(name)) }
    }

    fun editPlayer(
        player: Player,
        newName: String,
        colorIndex: Int,
        onResult: (NameValidation) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.updatePlayer(player, newName)
            if (result == NameValidation.VALID && colorIndex != player.colorIndex) {
                // Umbenannten Spieler übergeben, damit der neue Name nicht überschrieben wird
                repository.changePlayerColor(player.copy(name = newName.trim()), colorIndex)
            }
            onResult(result)
        }
    }

    fun deletePlayer(player: Player) {
        viewModelScope.launch { repository.deletePlayer(player.id) }
    }

    companion object {
        fun factory(repository: GameRepository): ViewModelProvider.Factory =
            viewModelFactory { initializer { PlayerViewModel(repository) } }
    }
}
