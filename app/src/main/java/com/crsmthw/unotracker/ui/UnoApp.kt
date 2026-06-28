package com.crsmthw.unotracker.ui

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.crsmthw.unotracker.UnoApplication
import com.crsmthw.unotracker.di.AppContainer
import com.crsmthw.unotracker.ui.screens.AboutScreen
import com.crsmthw.unotracker.ui.screens.ActiveGameScreen
import com.crsmthw.unotracker.ui.screens.GameDetailScreen
import com.crsmthw.unotracker.ui.screens.GameResultsScreen
import com.crsmthw.unotracker.ui.screens.MainScreen
import com.crsmthw.unotracker.ui.screens.ManualScreen
import com.crsmthw.unotracker.ui.screens.PlayerRosterScreen
import com.crsmthw.unotracker.ui.screens.RoundEntryScreen
import com.crsmthw.unotracker.ui.screens.RoundHistoryScreen
import com.crsmthw.unotracker.ui.screens.SettingsScreen
import com.crsmthw.unotracker.ui.screens.GameSetupScreen

@Composable
fun rememberAppContainer(): AppContainer =
    (LocalContext.current.applicationContext as UnoApplication).container

@Composable
fun UnoApp(themeVm: ThemeViewModel) {
    val navController = rememberNavController()
    val nav = remembernavGate(navController)

    NavHost(
        navController = navController,
        startDestination = "main",
        enterTransition = { slideIntoContainer(SlideDirection.Start, spring(0.85f, Spring.StiffnessMediumLow)) + fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, spring<IntOffset>(0.85f, Spring.StiffnessMediumLow)) + fadeOut() },
    ) {
        composable("main") {
            MainScreen(
                onOpenSetup = { variantId -> nav.go("setup/$variantId") },
                onOpenGame = { gameId -> nav.go("game/$gameId") },
                onOpenResults = { gameId -> nav.toResults(gameId) },
                onOpenManual = { variantId -> nav.go("manual/$variantId") },
                onOpenSettings = { nav.go("settings") },
                onOpenRoster = { nav.go("roster") },
                onOpenDetail = { gameId -> nav.go("gameDetail/$gameId") },
            )
        }
        composable(
            "setup/{variantId}",
            arguments = listOf(navArgument("variantId") { type = NavType.StringType }),
        ) { backStack ->
            val variantId = backStack.arguments?.getString("variantId").orEmpty()
            GameSetupScreen(
                variantId = variantId,
                onBack = { nav.up() },
                onStarted = { gameId -> nav.replaceWithGame(gameId) },
            )
        }
        composable(
            "game/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: -1L
            ActiveGameScreen(
                gameId = gameId,
                onBack = { nav.up() },
                onAddRound = { nav.go("round/$gameId/0") },
                onEditRound = { roundId -> nav.go("round/$gameId/$roundId") },
                onOpenHistory = { nav.go("history/$gameId") },
                onOpenResults = { nav.go("results/$gameId") },
                onOpenManual = { variantId -> nav.go("manual/$variantId") },
                onGameEnded = { nav.toResults(gameId) },
                onAbandoned = { nav.popToMain() },
            )
        }
        composable(
            "round/{gameId}/{roundId}",
            arguments = listOf(
                navArgument("gameId") { type = NavType.LongType },
                navArgument("roundId") { type = NavType.LongType },
            ),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: -1L
            val roundId = backStack.arguments?.getLong("roundId") ?: 0L
            RoundEntryScreen(gameId = gameId, roundId = roundId, onDone = { nav.up() }, onBack = { nav.up() })
        }
        composable(
            "results/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: -1L
            GameResultsScreen(gameId = gameId, onDone = { nav.popToMain() })
        }
        composable(
            "history/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: -1L
            RoundHistoryScreen(gameId = gameId, onBack = { nav.up() }, onEditRound = { roundId -> nav.go("round/$gameId/$roundId") })
        }
        composable(
            "manual/{variantId}",
            arguments = listOf(navArgument("variantId") { type = NavType.StringType }),
        ) { backStack ->
            val variantId = backStack.arguments?.getString("variantId").orEmpty()
            ManualScreen(variantId = variantId, onBack = { nav.up() }, onStartGame = { nav.go("setup/$variantId") })
        }
        composable(
            "gameDetail/{gameId}",
            arguments = listOf(navArgument("gameId") { type = NavType.LongType }),
        ) { backStack ->
            val gameId = backStack.arguments?.getLong("gameId") ?: -1L
            GameDetailScreen(gameId = gameId, onBack = { nav.up() }, onDeleted = { nav.up() })
        }
        composable("roster") { PlayerRosterScreen(onBack = { nav.up() }) }
        composable("settings") { SettingsScreen(themeVm = themeVm, onBack = { nav.up() }, onOpenAbout = { nav.go("about") }) }
        composable("about") { AboutScreen(onBack = { nav.up() }) }
    }
}
