package com.crsmthw.unotracker.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.drop

/**
 * Semantic M3 Expressive haptics — one vocabulary for the whole app, fired only from user gestures
 * (never reactive state). Every helper is gated on [HapticsConfig.enabled], mirrored from the
 * Settings toggle, so turning haptics off no-ops every call site.
 */
object HapticsConfig {
    @Volatile var enabled: Boolean = true
}

/** On/off feedback for a switch-like control. */
fun HapticFeedback.toggle(enabled: Boolean) =
    perform(if (enabled) HapticFeedbackType.ToggleOn else HapticFeedbackType.ToggleOff)

/** Affirmative: submit a round, start a game, confirm a dialog, declare a winner. */
fun HapticFeedback.confirm() = perform(HapticFeedbackType.Confirm)

/** Failed/blocked or destructive action (end game / delete). */
fun HapticFeedback.reject() = perform(HapticFeedbackType.Reject)

/** Long-press to reveal a menu or start a drag. */
fun HapticFeedback.longPress() = perform(HapticFeedbackType.LongPress)

/** Light click: tapping a card, opening a sheet, secondary buttons. */
fun HapticFeedback.press() = perform(HapticFeedbackType.ContextClick)

/** A stepper/slider crossing a notch (e.g. ± a card on the palette). */
fun HapticFeedback.tick() = perform(HapticFeedbackType.SegmentTick)

/** A list crossing an item boundary while scrolling. */
fun HapticFeedback.scrollTick() = perform(HapticFeedbackType.SegmentFrequentTick)

/** A drag crossing an activation threshold (pull-to-refresh, reorder). */
fun HapticFeedback.threshold() = perform(HapticFeedbackType.GestureThresholdActivate)

private fun HapticFeedback.perform(type: HapticFeedbackType) {
    if (HapticsConfig.enabled) performHapticFeedback(type)
}

/**
 * Fires [scrollTick] once each time the last visible item's index changes while [listState] is
 * actively scrolling. The initial value is dropped so opening a screen never buzzes.
 */
@Composable
fun ListScrollHaptics(listState: LazyListState) {
    val haptics = LocalHapticFeedback.current
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .drop(1)
            .collect { if (listState.isScrollInProgress) haptics.scrollTick() }
    }
}
