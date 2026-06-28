package com.crsmthw.unotracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController

/**
 * Debounced navigation wrapper (≥350ms gate, always launchSingleTop) so rapid double-taps don't push
 * duplicate destinations or pop twice. Playbook §7.
 */
class NavGate(private val navController: NavHostController) {
    private var lastNav = 0L

    private fun gate(): Boolean {
        val now = System.currentTimeMillis()
        if (now - lastNav < 350) return false
        lastNav = now
        return true
    }

    fun go(route: String) {
        if (gate()) navController.navigate(route) { launchSingleTop = true }
    }

    fun up() {
        if (gate()) navController.navigateUp()
    }

    /** From setup, land on the new game and drop setup off the back stack. */
    fun replaceWithGame(gameId: Long) {
        if (gate()) navController.navigate("game/$gameId") {
            launchSingleTop = true
            popUpTo("main")
        }
    }

    fun popToMain() {
        if (gate()) navController.navigate("main") {
            launchSingleTop = true
            popUpTo("main") { inclusive = true }
        }
    }

    /**
     * State-driven navigation to the winner screen — deliberately NOT gated. Auto-routing right after
     * a round submit (which just popped) would otherwise be swallowed by the debounce window. Phase 10's
     * "winner screen never showed" fix: plain navigate + launchSingleTop + popUpTo so it can't dup.
     */
    fun toResults(gameId: Long) {
        navController.navigate("results/$gameId") {
            launchSingleTop = true
            popUpTo("main")
        }
    }
}

@Composable
fun remembernavGate(navController: NavHostController): NavGate = remember(navController) { NavGate(navController) }
