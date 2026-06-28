package com.crsmthw.unotracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * The UNO suit identity, used directly by the card-by-card palette, The Table, and variant tiles —
 * independent of the (dynamic / static / AMOLED) M3 [androidx.compose.material3.ColorScheme]. Each
 * suit carries a strong [fill] and a readable [onFill] for text on top.
 */
data class SuitColor(val fill: Color, val onFill: Color)

object UnoColors {
    // Light side / classic suits
    val Red = SuitColor(Color(0xFFE53935), Color(0xFFFFFFFF))
    val Yellow = SuitColor(Color(0xFFFDD835), Color(0xFF5A4500))
    val Green = SuitColor(Color(0xFF43A047), Color(0xFFFFFFFF))
    val Blue = SuitColor(Color(0xFF1E88E5), Color(0xFFFFFFFF))

    // Dark side suits (UNO Flip)
    val Pink = SuitColor(Color(0xFFEC407A), Color(0xFFFFFFFF))
    val Teal = SuitColor(Color(0xFF26A69A), Color(0xFF00322E))
    val Orange = SuitColor(Color(0xFFFB8C00), Color(0xFF4A2800))
    val Purple = SuitColor(Color(0xFF8E24AA), Color(0xFFFFFFFF))

    /** Neutral chip for wild / colorless cards. */
    val Wild = SuitColor(Color(0xFF1A1620), Color(0xFFFFFFFF))

    val lightSuits = listOf(Red, Yellow, Green, Blue)
    val darkSuits = listOf(Pink, Teal, Orange, Purple)

    /** Resolve a suit by the lowercase color key used in `variants.json` (`red`, `teal`, …). */
    fun byKey(key: String): SuitColor = when (key.lowercase()) {
        "red" -> Red
        "yellow" -> Yellow
        "green" -> Green
        "blue" -> Blue
        "pink" -> Pink
        "teal" -> Teal
        "orange" -> Orange
        "purple" -> Purple
        else -> Wild
    }
}

/** Convenience: the four classic suits as a row, e.g. for the app's UNO-card branding. */
@Composable
@ReadOnlyComposable
fun classicSuitColors(): List<Color> = UnoColors.lightSuits.map { it.fill }
