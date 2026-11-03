package com.thomas.qwirkle.data.repository

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thomas.qwirkle.data.database.AppDatabase
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.model.ScoreEntry
import com.thomas.qwirkle.util.NameValidation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Tests gegen eine echte Room-Datenbank, die nur im Arbeitsspeicher liegt.
 * Laufen auf Emulator oder Gerät.
 */
@RunWith(AndroidJUnit4::class)
class GameRepositoryTest {

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

    // ---------- Hilfsfunktionen ----------

    private fun runDb(block: suspend CoroutineScope.() -> Unit) {
        runBlocking { block() }
    }

    /** Legt Spieler an und gibt sie in der übergebenen Reihenfolge zurück. */
    private suspend fun addPlayers(vararg names: String): List<Player> {
        names.forEach { repository.addPlayer(it) }
        val all = repository.getPlayers().first()
        return names.map { name -> all.first { it.name == name } }
    }

    private suspend fun assertIllegalArgument(block: suspend () -> Unit) {
        try {
            block()
        } catch (e: IllegalArgumentException) {
            return
        }
        fail("IllegalArgumentException erwartet")
    }

    /**
     * Runde 1: Anna 10, Peter 20 mit 1 Qwirkle
     * Runde 2: Anna 5 mit 2 Qwirkles, Peter 0
     * Anna = 10 + (5 + 12) = 27, Peter = (20 + 6) + 0 = 26, Summe 53, Qwirkles 3
     */
    private suspend fun playTwoRounds(gameId: Long, anna: Player, peter: Player) {
        repository.saveRound(
            gameId,
            listOf(ScoreEntry(anna.id, 10, 0), ScoreEntry(peter.id, 20, 1))
        )
        repository.saveRound(
            gameId,
            listOf(ScoreEntry(anna.id, 5, 2), ScoreEntry(peter.id, 0, 0))
        )
    }

    private suspend fun insertGame(
        date: Long,
        points: Int,
        qwirkles: Int,
        isTest: Boolean = false,
        isFinished: Boolean = true
    ): Long = db.gameHistoryDao().insert(
        GameHistory(
            date = date,
            playerCount = 2,
            totalPoints = points,
            totalQwirkles = qwirkles,
            isTestGame = isTest,
            isFinished = isFinished
        )
    )

    // ---------- Spieler: anlegen ----------

    @Test
    fun addPlayer_validName_isSavedAndListed() = runDb {
        assertEquals(NameValidation.VALID, repository.addPlayer("Anna"))

        val players = repository.getPlayers().first()
        assertEquals(listOf("Anna"), players.map { it.name })
        assertTrue(players[0].isActive)
    }

    @Test
    fun addPlayer_trimsName() = runDb {
        repository.addPlayer("  Anna  ")

        assertEquals(listOf("Anna"), repository.getPlayers().first().map { it.name })
    }

    @Test
    fun addPlayer_emptyName_isRejected() = runDb {
        assertEquals(NameValidation.EMPTY, repository.addPlayer("   "))

        assertTrue(repository.getPlayers().first().isEmpty())
    }

    @Test
    fun addPlayer_tooLongName_isRejected() = runDb {
        assertEquals(NameValidation.TOO_LONG, repository.addPlayer("a".repeat(21)))

        assertTrue(repository.getPlayers().first().isEmpty())
    }

    @Test
    fun addPlayer_duplicateName_isRejectedIgnoringCase() = runDb {
        repository.addPlayer("Anna")

        assertEquals(NameValidation.DUPLICATE, repository.addPlayer("anna"))
        assertEquals(1, repository.getPlayers().first().size)
    }

    @Test
    fun addPlayer_assignsFirstFreeColor() = runDb {
        repository.addPlayer("A")
        repository.addPlayer("B")
        repository.addPlayer("C")

        val colors = repository.getPlayers().first().sortedBy { it.name }.map { it.colorIndex }
        assertEquals(listOf(0, 1, 2), colors)
    }

    @Test
    fun getPlayers_isSortedAlphabeticallyIgnoringCase() = runDb {
        repository.addPlayer("peter")
        repository.addPlayer("Anna")
        repository.addPlayer("Klaus")

        assertEquals(
            listOf("Anna", "Klaus", "peter"),
            repository.getPlayers().first().map { it.name }
        )
    }

    // ---------- Spieler: bearbeiten und löschen ----------

    @Test
    fun updatePlayer_renamesPlayer() = runDb {
        val (anna) = addPlayers("Anna")

        assertEquals(NameValidation.VALID, repository.updatePlayer(anna, "Annika"))

        assertEquals(listOf("Annika"), repository.getPlayers().first().map { it.name })
    }

    @Test
    fun updatePlayer_nameOfOtherPlayer_isRejected() = runDb {
        val (anna, _) = addPlayers("Anna", "Peter")

        assertEquals(NameValidation.DUPLICATE, repository.updatePlayer(anna, "peter"))

        assertEquals(listOf("Anna", "Peter"), repository.getPlayers().first().map { it.name })
    }

    @Test
    fun updatePlayer_changingOwnNameCase_isAllowed() = runDb {
        val (anna) = addPlayers("Anna")

        assertEquals(NameValidation.VALID, repository.updatePlayer(anna, "ANNA"))

        assertEquals(listOf("ANNA"), repository.getPlayers().first().map { it.name })
    }

    @Test
    fun renameAndChangeColor_keepsBothChanges() = runDb {
        // Genau die Reihenfolge, die PlayerViewModel.editPlayer verwendet
        val (anna) = addPlayers("Anna")

        repository.updatePlayer(anna, "Annika")
        repository.changePlayerColor(anna.copy(name = "Annika"), 5)

        val saved = repository.getPlayers().first().single()
        assertEquals("Annika", saved.name)
        assertEquals(5, saved.colorIndex)
    }

    @Test
    fun deletePlayer_hidesPlayerButKeepsRecord() = runDb {
        val (anna, _) = addPlayers("Anna", "Peter")

        repository.deletePlayer(anna.id)

        assertEquals(listOf("Peter"), repository.getPlayers().first().map { it.name })
        val all = repository.getAllPlayersIncludingInactive().first()
        assertEquals(2, all.size)
        assertFalse(all.first { it.id == anna.id }.isActive)
    }

    @Test
    fun deletePlayer_nameBecomesAvailableAgain() = runDb {
        val (anna) = addPlayers("Anna")
        repository.deletePlayer(anna.id)

        assertEquals(NameValidation.VALID, repository.addPlayer("Anna"))
    }

    @Test
    fun deletePlayer_colorBecomesFreeAgain() = runDb {
        val (a, _) = addPlayers("A", "B")   // Farben 0 und 1
        repository.deletePlayer(a.id)

        repository.addPlayer("C")

        val c = repository.getPlayers().first().first { it.name == "C" }
        assertEquals(0, c.colorIndex)
    }

    // ---------- Spiel starten ----------

    @Test
    fun startGame_createsUnfinishedGameWithPlayersInOrder() = runDb {
        val (anna, peter, klaus) = addPlayers("Anna", "Peter", "Klaus")

        val gameId = repository.startGame(listOf(klaus.id, anna.id, peter.id), isTestGame = false)

        val game = repository.getGame(gameId)
        assertNotNull(game)
        assertFalse(game!!.isFinished)
        assertFalse(game.isTestGame)
        assertEquals(3, game.playerCount)
        assertEquals(
            listOf("Klaus", "Anna", "Peter"),
            repository.getGamePlayersOnce(gameId).map { it.name }
        )
    }

    @Test
    fun startGame_testGame_isMarked() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")

        val gameId = repository.startGame(listOf(anna.id, peter.id), isTestGame = true)

        assertTrue(repository.getGame(gameId)!!.isTestGame)
    }

    @Test
    fun startGame_withOnePlayer_isRejected() = runDb {
        assertIllegalArgument { repository.startGame(listOf(1L), isTestGame = false) }
    }

    @Test
    fun startGame_withEightPlayers_isRejected() = runDb {
        assertIllegalArgument { repository.startGame((1L..8L).toList(), isTestGame = false) }
    }

    @Test
    fun startGame_withSevenPlayers_works() = runDb {
        val names = (1..7).map { "Spieler$it" }.toTypedArray()
        val players = addPlayers(*names)

        val gameId = repository.startGame(players.map { it.id }, isTestGame = false)

        assertEquals(7, repository.getGamePlayersOnce(gameId).size)
    }

    @Test
    fun startGame_replacesOlderUnfinishedGame() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val first = repository.startGame(listOf(anna.id, peter.id), false)

        val second = repository.startGame(listOf(anna.id, peter.id), false)

        assertNull(repository.getGame(first))
        assertEquals(second, repository.getUnfinishedGame().first()?.id)
    }

    @Test
    fun startGame_keepsFinishedGames() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val first = repository.startGame(listOf(anna.id, peter.id), false)
        repository.finishGame(first, null)

        repository.startGame(listOf(anna.id, peter.id), false)

        assertNotNull(repository.getGame(first))
    }

    @Test
    fun getUnfinishedGame_withoutRunningGame_isNull() = runDb {
        assertNull(repository.getUnfinishedGame().first())
    }

    @Test
    fun deletedPlayer_staysPartOfExistingGame() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)

        repository.deletePlayer(anna.id)

        assertEquals(
            listOf("Anna", "Peter"),
            repository.getGamePlayersOnce(gameId).map { it.name }
        )
    }

    // ---------- Runden ----------

    @Test
    fun saveRound_numbersRoundsConsecutively() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        val entries = listOf(ScoreEntry(anna.id, 1, 0), ScoreEntry(peter.id, 2, 0))

        assertEquals(1, repository.saveRound(gameId, entries))
        assertEquals(2, repository.saveRound(gameId, entries))
        assertEquals(3, repository.saveRound(gameId, entries))
    }

    @Test
    fun saveRound_storesPointsAndQwirklesPerPlayer() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        val rounds = repository.getRoundsWithScores(gameId).first()

        assertEquals(listOf(1, 2), rounds.map { it.round.roundNumber })
        val peterRound1 = rounds[0].scores.first { it.playerId == peter.id }
        assertEquals(20, peterRound1.points)
        assertEquals(1, peterRound1.qwirkles)
        val annaRound2 = rounds[1].scores.first { it.playerId == anna.id }
        assertEquals(5, annaRound2.points)
        assertEquals(2, annaRound2.qwirkles)
    }

    @Test
    fun saveRound_savesOneScorePerPlayer() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        val rounds = repository.getRoundsWithScores(gameId).first()

        assertTrue(rounds.all { it.scores.size == 2 })
    }

    @Test
    fun deleteRound_removesRoundWithItsScores_andNumberIsReused() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        val last = repository.getRoundsWithScores(gameId).first().last()
        repository.deleteRound(last.round.id)

        assertEquals(1, repository.getRoundsWithScores(gameId).first().size)
        val next = repository.saveRound(
            gameId,
            listOf(ScoreEntry(anna.id, 1, 0), ScoreEntry(peter.id, 1, 0))
        )
        assertEquals(2, next)
    }

    @Test
    fun rounds_belongToTheirOwnGameOnly() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val first = repository.startGame(listOf(anna.id, peter.id), false)
        repository.finishGame(first, null)
        val second = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(second, anna, peter)

        assertTrue(repository.getRoundsWithScores(first).first().isEmpty())
        assertEquals(2, repository.getRoundsWithScores(second).first().size)
    }

    // ---------- Spiel beenden / verwerfen ----------

    @Test
    fun finishGame_calculatesTotalsIncludingBonuses() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        repository.finishGame(gameId, endBonusPlayerId = peter.id)

        val game = repository.getGame(gameId)!!
        assertTrue(game.isFinished)
        assertEquals(59, game.totalPoints)      // 27 + 26 + 6 Endspielbonus
        assertEquals(3, game.totalQwirkles)
        assertEquals(peter.id, game.endBonusPlayerId)
    }

    @Test
    fun finishGame_withoutEndBonus_addsNoBonus() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        repository.finishGame(gameId, endBonusPlayerId = null)

        val game = repository.getGame(gameId)!!
        assertEquals(53, game.totalPoints)
        assertNull(game.endBonusPlayerId)
    }

    @Test
    fun setEndBonusPlayer_doesNotFinishTheGame() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)

        repository.setEndBonusPlayer(gameId, anna.id)

        val game = repository.getGame(gameId)!!
        assertFalse(game.isFinished)
        assertEquals(anna.id, game.endBonusPlayerId)
        assertEquals(0, game.totalPoints)
    }

    @Test
    fun setEndBonusPlayer_nullClearsTheChoice() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        repository.setEndBonusPlayer(gameId, anna.id)

        repository.setEndBonusPlayer(gameId, null)

        assertNull(repository.getGame(gameId)!!.endBonusPlayerId)
    }

    @Test
    fun discardGame_removesGameRoundsAndParticipants() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)
        playTwoRounds(gameId, anna, peter)

        repository.discardGame(gameId)

        assertNull(repository.getGame(gameId))
        assertTrue(repository.getRoundsWithScores(gameId).first().isEmpty())
        assertTrue(repository.getGamePlayersOnce(gameId).isEmpty())
    }

    @Test
    fun discardGame_keepsThePlayers() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")
        val gameId = repository.startGame(listOf(anna.id, peter.id), false)

        repository.discardGame(gameId)

        assertEquals(2, repository.getPlayers().first().size)
    }

    // ---------- Statistik und Historie ----------

    @Test
    fun statistics_emptyDatabase_isAllZero() = runDb {
        val stats = repository.getStatistics().first()

        assertEquals(0, stats.totalGames)
        assertEquals(0, stats.totalPoints)
        assertEquals(0, stats.totalQwirkles)
        assertEquals(0.0, stats.averagePoints, 0.001)
        assertEquals(0.0, stats.averageQwirkles, 0.001)
        assertEquals(0, stats.maxPoints)
        assertEquals(0, stats.maxQwirkles)
    }

    @Test
    fun statistics_countsOnlyFinishedRealGames() = runDb {
        insertGame(date = 3000, points = 100, qwirkles = 3)
        insertGame(date = 2000, points = 50, qwirkles = 1)
        insertGame(date = 4000, points = 999, qwirkles = 99, isTest = true)
        insertGame(date = 5000, points = 777, qwirkles = 88, isFinished = false)

        val stats = repository.getStatistics().first()

        assertEquals(2, stats.totalGames)
        assertEquals(150, stats.totalPoints)
        assertEquals(4, stats.totalQwirkles)
        assertEquals(75.0, stats.averagePoints, 0.001)
        assertEquals(2.0, stats.averageQwirkles, 0.001)
        assertEquals(100, stats.maxPoints)
        assertEquals(3, stats.maxQwirkles)
    }

    @Test
    fun finishedGames_areNewestFirst_withoutTestAndUnfinishedGames() = runDb {
        val older = insertGame(date = 2000, points = 50, qwirkles = 1)
        val newer = insertGame(date = 3000, points = 100, qwirkles = 3)
        insertGame(date = 4000, points = 1, qwirkles = 1, isTest = true)
        insertGame(date = 5000, points = 1, qwirkles = 1, isFinished = false)

        val games = repository.getFinishedGames().first()

        assertEquals(listOf(newer, older), games.map { it.id })
    }

    @Test
    fun lastFinishedGame_isNewestRealFinishedGame() = runDb {
        insertGame(date = 2000, points = 50, qwirkles = 1)
        val newest = insertGame(date = 3000, points = 100, qwirkles = 3)
        insertGame(date = 4000, points = 1, qwirkles = 1, isTest = true)

        assertEquals(newest, repository.getLastFinishedGame().first()?.id)
    }

    @Test
    fun lastFinishedGame_withoutGames_isNull() = runDb {
        assertNull(repository.getLastFinishedGame().first())
    }

    @Test
    fun gameCount_ignoresTestAndUnfinishedGames() = runDb {
        insertGame(date = 1000, points = 10, qwirkles = 0)
        insertGame(date = 2000, points = 10, qwirkles = 0)
        insertGame(date = 3000, points = 10, qwirkles = 0, isTest = true)
        insertGame(date = 4000, points = 10, qwirkles = 0, isFinished = false)

        assertEquals(2, repository.gameCount().first())
    }

    @Test
    fun deleteGame_removesItFromStatistics() = runDb {
        val big = insertGame(date = 3000, points = 100, qwirkles = 3)
        insertGame(date = 2000, points = 50, qwirkles = 1)

        repository.deleteGame(big)

        val stats = repository.getStatistics().first()
        assertEquals(1, stats.totalGames)
        assertEquals(50, stats.totalPoints)
        assertEquals(50, stats.maxPoints)
    }

    @Test
    fun finishedRealGame_appearsInStatistics_testGameDoesNot() = runDb {
        val (anna, peter) = addPlayers("Anna", "Peter")

        val real = repository.startGame(listOf(anna.id, peter.id), isTestGame = false)
        playTwoRounds(real, anna, peter)
        repository.finishGame(real, peter.id)

        val test = repository.startGame(listOf(anna.id, peter.id), isTestGame = true)
        playTwoRounds(test, anna, peter)
        repository.finishGame(test, null)

        val stats = repository.getStatistics().first()
        assertEquals(1, stats.totalGames)
        assertEquals(59, stats.totalPoints)
        assertEquals(3, stats.totalQwirkles)
    }
}
