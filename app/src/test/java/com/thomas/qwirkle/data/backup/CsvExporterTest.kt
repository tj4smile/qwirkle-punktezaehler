package com.thomas.qwirkle.data.backup

import com.thomas.qwirkle.data.entity.GameHistory
import com.thomas.qwirkle.data.entity.GamePlayer
import com.thomas.qwirkle.data.entity.Player
import com.thomas.qwirkle.data.entity.Round
import com.thomas.qwirkle.data.entity.RoundScore
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class CsvExporterTest {

    private lateinit var oldZone: TimeZone

    @Before
    fun setUp() {
        oldZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(oldZone)
    }

    private val labels = CsvLabels(
        date = "Date", game = "Game", testGame = "Test game", finished = "Finished",
        round = "Round", player = "Player", points = "Points", qwirkles = "Qwirkles",
        qwirkleBonus = "Qwirkle bonus", roundTotal = "Round total", endBonus = "End bonus",
        yes = "Yes", no = "No"
    )

    private val anna = Player(1, "Anna", 0)
    private val peter = Player(2, "Peter", 1)

    private val oct26 = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, Calendar.OCTOBER, 26, 12, 0, 0)
    }.timeInMillis

    private fun game(
        id: Long = 10,
        finished: Boolean = true,
        test: Boolean = false,
        endBonus: Long? = 2
    ) = GameHistory(
        id = id, date = oct26, playerCount = 2, totalPoints = 59, totalQwirkles = 3,
        isTestGame = test, isFinished = finished, endBonusPlayerId = endBonus
    )

    /** Runde 1: Anna 10, Peter 20 (1 Qwirkle). Runde 2: Anna 5 (2 Qwirkles), Peter 0. */
    private fun data(
        game: GameHistory = game(),
        players: List<Player> = listOf(anna, peter),
        positions: List<Pair<Long, Int>> = listOf(1L to 0, 2L to 1)
    ) = BackupData(
        players = players,
        games = listOf(game),
        gamePlayers = positions.mapIndexed { index, (playerId, position) ->
            GamePlayer(index + 1L, game.id, playerId, position)
        },
        rounds = listOf(Round(100, game.id, 1), Round(101, game.id, 2)),
        roundScores = listOf(
            RoundScore(1000, 100, 1, 10, 0),
            RoundScore(1001, 100, 2, 20, 1),
            RoundScore(1002, 101, 1, 5, 2),
            RoundScore(1003, 101, 2, 0, 0)
        )
    )

    private fun lines(text: String) = text.split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun header_usesLabels() {
        val result = lines(CsvExporter.build(data(), labels, ','))

        assertEquals(
            "Date,Game,Test game,Finished,Round,Player,Points,Qwirkles,Qwirkle bonus,Round total",
            result[0]
        )
    }

    @Test
    fun rows_containPointsBonusAndRoundTotal() {
        val result = lines(CsvExporter.build(data(), labels, ','))

        assertEquals("2026-10-26,10,No,Yes,1,Anna,10,0,0,10", result[1])
        assertEquals("2026-10-26,10,No,Yes,1,Peter,20,1,6,26", result[2])
        assertEquals("2026-10-26,10,No,Yes,2,Anna,5,2,12,17", result[3])
        assertEquals("2026-10-26,10,No,Yes,2,Peter,0,0,0,0", result[4])
    }

    @Test
    fun endBonus_isOwnRow() {
        val result = lines(CsvExporter.build(data(), labels, ','))

        assertEquals(6, result.size)
        assertEquals("2026-10-26,10,No,Yes,End bonus,Peter,0,0,0,6", result[5])
    }

    @Test
    fun sumOfRoundTotals_equalsGameTotalPoints() {
        val result = lines(CsvExporter.build(data(), labels, ','))

        val sum = result.drop(1).sumOf { it.split(',').last().toInt() }
        assertEquals(59, sum)
    }

    @Test
    fun unfinishedGame_hasNoEndBonusRow() {
        val result = lines(CsvExporter.build(data(game(finished = false)), labels, ','))

        assertEquals(5, result.size)
        assertTrue(result.drop(1).all { it.contains(",No,No,") })
    }

    @Test
    fun noEndBonusPlayer_hasNoEndBonusRow() {
        val result = lines(CsvExporter.build(data(game(endBonus = null)), labels, ','))

        assertEquals(5, result.size)
    }

    @Test
    fun testGame_isMarked() {
        val result = lines(CsvExporter.build(data(game(test = true)), labels, ','))

        assertTrue(result[1].startsWith("2026-10-26,10,Yes,Yes,"))
    }

    @Test
    fun playersFollowPlayingOrder() {
        val reversed = data(positions = listOf(1L to 1, 2L to 0))

        val result = lines(CsvExporter.build(reversed, labels, ','))

        assertTrue(result[1].contains(",1,Peter,"))
        assertTrue(result[2].contains(",1,Anna,"))
    }

    @Test
    fun inactivePlayer_stillShowsName() {
        val inactive = data(players = listOf(anna.copy(isActive = false), peter))

        val result = lines(CsvExporter.build(inactive, labels, ','))

        assertTrue(result[1].contains(",Anna,"))
    }

    @Test
    fun semicolonDelimiter_isUsed() {
        val result = lines(CsvExporter.build(data(), labels, ';'))

        assertEquals("2026-10-26;10;No;Yes;1;Anna;10;0;0;10", result[1])
    }

    @Test
    fun nameWithDelimiterAndQuotes_isEscaped() {
        val tricky = data(players = listOf(anna.copy(name = "Anna, \"Ace\""), peter))

        val result = lines(CsvExporter.build(tricky, labels, ','))

        assertTrue(result[1].contains(",\"Anna, \"\"Ace\"\"\","))
    }

    @Test
    fun nameStartingWithFormulaCharacter_isNeutralized() {
        val formula = data(players = listOf(anna.copy(name = "=SUM(A1)"), peter))

        val result = lines(CsvExporter.build(formula, labels, ','))

        assertTrue(result[1].contains(",'=SUM(A1),"))
    }

    @Test
    fun emptyData_givesOnlyHeader() {
        val empty = BackupData(emptyList(), emptyList(), emptyList(), emptyList(), emptyList())

        assertEquals(1, lines(CsvExporter.build(empty, labels, ',')).size)
    }

    @Test
    fun delimiter_dependsOnLanguage() {
        assertEquals(';', CsvExporter.delimiterFor(Locale.GERMANY))
        assertEquals(';', CsvExporter.delimiterFor(Locale("de", "AT")))
        assertEquals(',', CsvExporter.delimiterFor(Locale.US))
        assertEquals(',', CsvExporter.delimiterFor(Locale.FRANCE))
    }

    @Test
    fun linesEndWithCrLf() {
        val text = CsvExporter.build(data(), labels, ',')

        assertTrue(text.endsWith("\r\n"))
    }
}
