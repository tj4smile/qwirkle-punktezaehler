package com.thomas.qwirkle.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thomas.qwirkle.data.database.AppDatabase
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.ScoreEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Spielerstatistik gegen eine echte Room-Datenbank (nur im Arbeitsspeicher). */
@RunWith(AndroidJUnit4::class)
class PlayerStatisticsRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: GameRepository

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = GameRepository(
            db.playerDao(),
            db.gameHistoryDao(),
            db.gamePlayerDao(),
            db.roundDao(),
            db.roundScoreDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun runDb(block: suspend CoroutineScope.() -> Unit) {
        runBlocking { block() }
    }

    private suspend fun addPlayers(vararg names: String): List<Player> {
        names.forEach { repository.addPlayer(it) }
        val all = repository.getPlayers().first()
        return names.map { name -> all.first { it.name == name } }
    }

    /** Spielt eine Partie mit einer Runde und speichert sie. */
    private suspend fun playGame(
        a: Player, pointsA: Int, qwirklesA: Int,
        b: Player, pointsB: Int,
        endBonus: Player? = null,
        test: Boolean = false,
        finish: Boolean = true
    ): Long {
        val gameId = repository.startGame(listOf(a.id, b.id), test)
        repository.saveRound(gameId, listOf(ScoreEntry(a.id, pointsA, qwirklesA), ScoreEntry(b.id, pointsB, 0)))
        if (finish) repository.finishGame(gameId, endBonus?.id)
        return gameId
    }

    @Test
    fun savedGames_areCountedPerPlayer() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        playGame(anna, 30, 1, peter, 20)   // Anna 36, Peter 20
        playGame(anna, 10, 0, peter, 25)   // Anna 10, Peter 25

        val stats = repository.getPlayerStatistics().first()

        val a = stats.first { it.player.name == "Anna" }
        assertEquals(2, a.games)
        assertEquals(1, a.wins)
        assertEquals(46, a.totalPoints)
        assertEquals(36, a.bestPoints)
        assertEquals(1, a.totalQwirkles)
    }

    @Test
    fun endBonus_isIncludedInPlayerPoints() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        playGame(anna, 10, 0, peter, 12, endBonus = anna) // Anna 16, Peter 12

        val stats = repository.getPlayerStatistics().first()

        assertEquals(16, stats.first { it.player.name == "Anna" }.totalPoints)
        assertEquals(1, stats.first { it.player.name == "Anna" }.wins)
    }

    @Test
    fun testGamesAndUnfinishedGames_areExcluded() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        playGame(anna, 10, 0, peter, 5, test = true)
        playGame(anna, 10, 0, peter, 5, finish = false)

        assertTrue(repository.getPlayerStatistics().first().isEmpty())
    }

    @Test
    fun deletedPlayer_isHiddenButOldGamesStayForOthers() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        playGame(anna, 10, 0, peter, 50) // Peter gewinnt
        repository.deletePlayer(peter.id)

        val stats = repository.getPlayerStatistics().first()

        assertEquals(listOf("Anna"), stats.map { it.player.name })
        assertEquals(0, stats[0].wins)
        assertEquals(1, stats[0].games)
    }

    @Test
    fun deletingAGame_removesItFromPlayerStatistics() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = playGame(anna, 10, 0, peter, 5)

        repository.deleteGame(gameId)

        assertTrue(repository.getPlayerStatistics().first().isEmpty())
    }
}
