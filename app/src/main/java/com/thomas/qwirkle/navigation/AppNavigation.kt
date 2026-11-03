package com.thomas.qwirkle.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.thomas.qwirkle.ui.screens.data.DataScreen
import com.thomas.qwirkle.ui.screens.gameplay.GameEndScreen
import com.thomas.qwirkle.ui.screens.gameplay.GameHistoryScreen
import com.thomas.qwirkle.ui.screens.home.HomeScreen
import com.thomas.qwirkle.ui.screens.info.InfoScreen
import com.thomas.qwirkle.ui.screens.newgame.GameSetupScreen
import com.thomas.qwirkle.ui.screens.newgame.NewGameScreen
import com.thomas.qwirkle.ui.screens.players.PlayerManagementScreen
import com.thomas.qwirkle.ui.screens.results.ResultScreen
import com.thomas.qwirkle.ui.screens.scoring.ScoreEntryScreen
import com.thomas.qwirkle.ui.screens.statistics.StatisticsScreen

private val gameIdArgument = listOf(
    navArgument(NavRoutes.ARG_GAME_ID) { type = NavType.LongType }
)

private fun NavBackStackEntry.gameId(): Long =
    arguments?.getLong(NavRoutes.ARG_GAME_ID) ?: 0L

private fun NavBackStackEntry.roundId(): Long =
    arguments?.getLong(NavRoutes.ARG_ROUND_ID) ?: 0L

private val editRoundArguments = listOf(
    navArgument(NavRoutes.ARG_GAME_ID) { type = NavType.LongType },
    navArgument(NavRoutes.ARG_ROUND_ID) { type = NavType.LongType }
)

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    // Zurück zum Startbildschirm. Ein laufendes Spiel bleibt gespeichert.
    val goHome: () -> Unit = { navController.popBackStack(NavRoutes.HOME, inclusive = false) }

    NavHost(navController = navController, startDestination = NavRoutes.HOME) {

        composable(NavRoutes.HOME) {
            HomeScreen(
                onNewGame = { navController.navigate(NavRoutes.NEWGAME) },
                onStatistics = { navController.navigate(NavRoutes.STATISTICS) },
                onPlayers = { navController.navigate(NavRoutes.PLAYERS) },
                onData = { navController.navigate(NavRoutes.DATA) },
                onInfo = { navController.navigate(NavRoutes.INFO) },
                onResumeGame = { gameId -> navController.navigate(NavRoutes.history(gameId)) }
            )
        }

        composable(NavRoutes.PLAYERS) {
            PlayerManagementScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.STATISTICS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.INFO) {
            InfoScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.DATA) {
            DataScreen(onBack = { navController.popBackStack() })
        }

        composable(NavRoutes.NEWGAME) {
            NewGameScreen(
                onBack = { navController.popBackStack() },
                onManagePlayers = { navController.navigate(NavRoutes.PLAYERS) },
                onContinue = { ids, isTest ->
                    navController.navigate(NavRoutes.setup(ids, isTest))
                }
            )
        }

        // Reihenfolge & Farben -> Spiel anlegen -> erste Runde
        composable(
            NavRoutes.SETUP,
            arguments = listOf(
                navArgument(NavRoutes.ARG_PLAYER_IDS) { type = NavType.StringType },
                navArgument(NavRoutes.ARG_TEST_GAME) { type = NavType.BoolType }
            )
        ) { entry ->
            val ids = entry.arguments?.getString(NavRoutes.ARG_PLAYER_IDS).orEmpty()
                .split(",").mapNotNull { it.toLongOrNull() }
            GameSetupScreen(
                playerIds = ids,
                isTestGame = entry.arguments?.getBoolean(NavRoutes.ARG_TEST_GAME) ?: false,
                onBack = { navController.popBackStack() },
                onGameStarted = { gameId ->
                    navController.navigate(NavRoutes.scoring(gameId)) {
                        popUpTo(NavRoutes.NEWGAME) { inclusive = true }
                    }
                }
            )
        }

        // Punkteerfassung -> Runde gespeichert -> Spielverlauf
        composable(NavRoutes.SCORING, arguments = gameIdArgument) { entry ->
            val gameId = entry.gameId()
            ScoreEntryScreen(
                gameId = gameId,
                onRoundSaved = {
                    navController.navigate(NavRoutes.history(gameId)) {
                        popUpTo(NavRoutes.SCORING) { inclusive = true }
                    }
                },
                onFinish = { navController.navigate(NavRoutes.gameEnd(gameId)) },
                onBack = goHome
            )
        }

        // Vorhandene Runde bearbeiten (aus dem Spielverlauf), danach zurück zum Verlauf
        composable(NavRoutes.EDITROUND, arguments = editRoundArguments) { entry ->
            ScoreEntryScreen(
                gameId = entry.gameId(),
                roundId = entry.roundId(),
                onRoundSaved = { navController.popBackStack() },
                onFinish = {},
                onBack = { navController.popBackStack() }
            )
        }

        // Spielverlauf -> neue Runde oder Spielende
        composable(NavRoutes.HISTORY, arguments = gameIdArgument) { entry ->
            val gameId = entry.gameId()
            GameHistoryScreen(
                gameId = gameId,
                onAddRound = {
                    navController.navigate(NavRoutes.scoring(gameId)) {
                        popUpTo(NavRoutes.HISTORY) { inclusive = true }
                    }
                },
                onEditRound = { roundId ->
                    navController.navigate(NavRoutes.editRound(gameId, roundId))
                },
                onFinish = { navController.navigate(NavRoutes.gameEnd(gameId)) },
                onBack = goHome
            )
        }

        // Spielende (Endspielbonus) -> Endauswertung
        composable(NavRoutes.GAMEEND, arguments = gameIdArgument) { entry ->
            val gameId = entry.gameId()
            GameEndScreen(
                gameId = gameId,
                onBack = { navController.popBackStack() },
                onShowResults = { navController.navigate(NavRoutes.results(gameId)) }
            )
        }

        // Endauswertung -> speichern oder verwerfen -> Start
        composable(NavRoutes.RESULTS, arguments = gameIdArgument) { entry ->
            ResultScreen(
                gameId = entry.gameId(),
                onBack = { navController.popBackStack() },
                onSaved = goHome,
                onDiscarded = goHome
            )
        }
    }
}
