package com.thomas.qwirkle.data.backup

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.thomas.qwirkle.data.dao.BackupDao
import com.thomas.qwirkle.data.database.AppDatabase
import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackupDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: BackupDao

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.backupDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private val sample = BackupData(
        players = listOf(Player(1, "Anna", 0, true), Player(2, "Peter", 1, false)),
        games = listOf(
            GameHistory(10, 1_700_000_000_000, 2, 59, 3, false, true, 2L),
            GameHistory(11, 1_700_100_000_000, 2, 0, 0, true, false, null)
        ),
        gamePlayers = listOf(
            GamePlayer(1, 10, 1, 0), GamePlayer(2, 10, 2, 1),
            GamePlayer(3, 11, 1, 0), GamePlayer(4, 11, 2, 1)
        ),
        rounds = listOf(Round(100, 10, 1), Round(101, 10, 2)),
        roundScores = listOf(
            RoundScore(1000, 100, 1, 10, 0), RoundScore(1001, 100, 2, 20, 1),
            RoundScore(1002, 101, 1, 5, 2), RoundScore(1003, 101, 2, 0, 0)
        )
    )

    @Test
    fun replaceAll_thenSnapshot_returnsSameData() = runBlocking {
        dao.replaceAll(sample)

        assertEquals(sample, dao.snapshot())
    }

    @Test
    fun snapshot_includesInactivePlayersAndUnfinishedGames() = runBlocking {
        dao.replaceAll(sample)

        val snapshot = dao.snapshot()

        assertTrue(snapshot.players.any { !it.isActive })
        assertTrue(snapshot.games.any { !it.isFinished })
    }

    @Test
    fun replaceAll_replacesExistingData() = runBlocking {
        dao.replaceAll(sample)
        val other = BackupData(
            players = listOf(Player(7, "Klaus", 2, true)),
            games = emptyList(), gamePlayers = emptyList(),
            rounds = emptyList(), roundScores = emptyList()
        )

        dao.replaceAll(other)

        assertEquals(other, dao.snapshot())
    }

    @Test
    fun replaceAll_withEmptyData_clearsEverything() = runBlocking {
        dao.replaceAll(sample)
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        dao.replaceAll(empty)

        assertEquals(empty, dao.snapshot())
    }

    @Test
    fun replaceAll_withBrokenReference_keepsOldData() = runBlocking {
        dao.replaceAll(sample)
        val broken = sample.copy(
            roundScores = sample.roundScores + RoundScore(2000, 100, 999, 1, 0) // Spieler 999 gibt es nicht
        )

        try {
            dao.replaceAll(broken)
            fail("Ein Fehler wegen des ungültigen Verweises wurde erwartet")
        } catch (e: Exception) {
            // erwartet: Transaktion wird zurückgerollt
        }

        assertEquals(sample, dao.snapshot())
    }

    @Test
    fun newRecords_afterRestore_getFreshIds() = runBlocking {
        dao.replaceAll(sample)

        val newId = db.playerDao().insert(Player(name = "Neu", colorIndex = 4))

        assertTrue(newId > 2)
    }
}
