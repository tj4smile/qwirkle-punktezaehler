package com.thomas.qwirkle.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thomas.qwirkle.data.database.AppDatabase
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.ScoreEntry
import com.thomas.qwirkle.util.GameCalculations
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Nachträgliches Ändern von Runden gegen eine echte Room-Datenbank (nur im Arbeitsspeicher). */
@RunWith(AndroidJUnit4::class)
class RoundEditRepositoryTest {

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

    private suspend fun expectFailure(expected: Class<out Exception>, block: suspend () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            if (expected.isInstance(e)) return
            throw e
        }
        fail("${expected.simpleName} erwartet")
    }

    /** Runde 1: Anna 10, Peter 20 mit 1 Qwirkle. Runde 2: Anna 5 mit 2 Qwirkles, Peter 0. */
    private suspend fun twoRounds(): Triple<Long, Player, Player> {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        repository.saveRound(gameId, listOf(ScoreEntry(anna.id, 10, 0), ScoreEntry(peter.id, 20, 1)))
        repository.saveRound(gameId, listOf(ScoreEntry(anna.id, 5, 2), ScoreEntry(peter.id, 0, 0)))
        return Triple(gameId, anna, peter)
    }

    private suspend fun totals(gameId: Long): Map<String, Int> {
        val players = repository.getGamePlayersOnce(gameId)
        val rounds = repository.getRoundsWithScores(gameId).first()
        return GameCalculations.playerTotals(players, rounds).associate { it.player.name to it.points }
    }

    @Test
    fun updateRound_changesPointsAndQwirkles() = runDb {
        val (gameId, anna, peter) = twoRounds()
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round

        repository.updateRound(
            round1.id,
            listOf(ScoreEntry(anna.id, 12, 1), ScoreEntry(peter.id, 3, 0))
        )

        val scores = repository.getRoundsWithScores(gameId).first()[0].scores
        assertEquals(12, scores.first { it.playerId == anna.id }.points)
        assertEquals(1, scores.first { it.playerId == anna.id }.qwirkles)
        assertEquals(3, scores.first { it.playerId == peter.id }.points)
        assertEquals(0, scores.first { it.playerId == peter.id }.qwirkles)
    }

    @Test
    fun updateRound_movesTotals() = runDb {
        val (gameId, anna, peter) = twoRounds()
        // vorher: Anna 10 + (5 + 12) = 27, Peter (20 + 6) + 0 = 26
        assertEquals(mapOf("Anna" to 27, "Peter" to 26), totals(gameId))
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round

        repository.updateRound(
            round1.id,
            listOf(ScoreEntry(anna.id, 10, 0), ScoreEntry(peter.id, 0, 0))
        )

        assertEquals(mapOf("Anna" to 27, "Peter" to 0), totals(gameId))
    }

    @Test
    fun updateRound_keepsRoundNumberAndOtherRounds() = runDb {
        val (gameId, anna, peter) = twoRounds()
        val before = repository.getRoundsWithScores(gameId).first()
        val round1 = before[0].round

        repository.updateRound(
            round1.id,
            listOf(ScoreEntry(anna.id, 99, 0), ScoreEntry(peter.id, 99, 0))
        )

        val after = repository.getRoundsWithScores(gameId).first()
        assertEquals(listOf(1, 2), after.map { it.round.roundNumber })
        assertEquals(before[1].scores, after[1].scores) // Runde 2 unverändert
        assertEquals(before[0].scores.map { it.id }, after[0].scores.map { it.id }) // gleiche Zeilen
    }

    @Test
    fun updateRound_ofFinishedGame_isRejected() = runDb {
        val (gameId, anna, peter) = twoRounds()
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round
        repository.finishGame(gameId, null)

        expectFailure(IllegalStateException::class.java) {
            repository.updateRound(
                round1.id,
                listOf(ScoreEntry(anna.id, 1, 0), ScoreEntry(peter.id, 1, 0))
            )
        }

        assertEquals(10, repository.getRoundsWithScores(gameId).first()[0].scores
            .first { it.playerId == anna.id }.points)
    }

    @Test
    fun updateRound_unknownRound_isRejected() = runDb {
        val (_, anna, peter) = twoRounds()

        expectFailure(IllegalArgumentException::class.java) {
            repository.updateRound(9999, listOf(ScoreEntry(anna.id, 1, 0), ScoreEntry(peter.id, 1, 0)))
        }
    }

    @Test
    fun updateRound_playerNotInRound_changesNothing() = runDb {
        val (gameId, anna, _) = twoRounds()
        val outsider = addPlayers("Klaus")[0]
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round

        expectFailure(IllegalArgumentException::class.java) {
            repository.updateRound(
                round1.id,
                listOf(ScoreEntry(anna.id, 77, 0), ScoreEntry(outsider.id, 1, 0))
            )
        }

        // auch Annas Wert bleibt unverändert (Transaktion)
        val scores = repository.getRoundsWithScores(gameId).first()[0].scores
        assertEquals(10, scores.first { it.playerId == anna.id }.points)
    }

    @Test
    fun updateRound_negativeValues_areRejected() = runDb {
        val (gameId, anna, peter) = twoRounds()
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round

        expectFailure(IllegalArgumentException::class.java) {
            repository.updateRound(round1.id, listOf(ScoreEntry(anna.id, -1, 0), ScoreEntry(peter.id, 1, 0)))
        }
        expectFailure(IllegalArgumentException::class.java) {
            repository.updateRound(round1.id, listOf(ScoreEntry(anna.id, 1, -1), ScoreEntry(peter.id, 1, 0)))
        }
        expectFailure(IllegalArgumentException::class.java) {
            repository.updateRound(round1.id, emptyList())
        }
    }

    @Test
    fun getRoundWithScoresOnce_returnsRoundAndScores() = runDb {
        val (gameId, _, _) = twoRounds()
        val round2 = repository.getRoundsWithScores(gameId).first()[1].round

        val loaded = repository.getRoundWithScoresOnce(round2.id)

        assertNotNull(loaded)
        assertEquals(2, loaded!!.round.roundNumber)
        assertEquals(2, loaded.scores.size)
    }

    @Test
    fun editedGame_finishesWithNewTotals() = runDb {
        val (gameId, anna, peter) = twoRounds()
        val round1 = repository.getRoundsWithScores(gameId).first()[0].round

        repository.updateRound(round1.id, listOf(ScoreEntry(anna.id, 10, 0), ScoreEntry(peter.id, 0, 0)))
        repository.finishGame(gameId, anna.id)

        val game = repository.getGame(gameId)!!
        // Anna 27 + 6 Endbonus, Peter 0 -> Summe 33, Qwirkles 2
        assertEquals(33, game.totalPoints)
        assertEquals(2, game.totalQwirkles)
    }
}
