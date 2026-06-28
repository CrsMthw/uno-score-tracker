package com.crsmthw.unotracker

import com.crsmthw.unotracker.data.config.VariantParser
import com.crsmthw.unotracker.domain.FlipSide
import com.crsmthw.unotracker.domain.JointWinnerSplit
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.Variant
import com.crsmthw.unotracker.domain.scoring.HandCard
import com.crsmthw.unotracker.domain.scoring.HandTally
import com.crsmthw.unotracker.domain.scoring.RoundInput
import com.crsmthw.unotracker.domain.scoring.ScoringEngine
import com.crsmthw.unotracker.domain.scoring.WinReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Validates the scoring engine against docs/uno-scoring-test-fixtures.md (F1-F14 + invariants).
 * Loads the real bundled variants.json so the config and the engine are tested together.
 */
class ScoringEngineTest {

    private val variants: List<Variant> = run {
        val json = javaClass.getResourceAsStream("/variants.json")!!
            .bufferedReader().use { it.readText() }
        VariantParser.parse(json)
    }

    private fun v(id: String): Variant = variants.first { it.id == id }
    private val engine = ScoringEngine()

    private fun num(n: Int): HandCard = HandCard.Number(n)
    private fun card(key: String): HandCard = HandCard.Keyed(key)
    private fun hand(vararg cards: HandCard): HandTally = HandTally.Cards(cards.toList())
    private fun Map<String, Int>.at(e: String): Int = this[e] ?: 0

    // ── Mode A (winner_takes) ──────────────────────────────────────────────────────────────────

    @Test fun f1_original_basic() {
        val r = engine.roundDeltas(
            v("original"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf(
                    "Bob" to hand(num(9), card("skip"), card("wild")),     // 9 + 20 + 50 = 79
                    "Cara" to hand(num(0), num(5), card("reverse")),       // 0 + 5 + 20 = 25
                ),
            ),
        ).deltas
        assertEquals(104, r.at("Alice"))
        assertEquals(0, r.at("Bob"))
        assertEquals(0, r.at("Cara"))
    }

    @Test fun f2_original_forced_draw_counts() {
        // Alice goes out on Draw Two; Bob draws 2 (wild_draw_four + 3) before the tally.
        val r = engine.roundDeltas(
            v("original"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(7), num(7), card("wild_draw_four"), num(3))),
            ),
        ).deltas
        assertEquals(67, r.at("Alice"))
    }

    @Test fun f3_flip_dark_side() {
        val r = engine.roundDeltas(
            v("flip"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(7), card("skip_everyone"), card("wild_draw_color"))),
                endedOnSide = FlipSide.DARK,
            ),
        ).deltas
        assertEquals(97, r.at("Alice")) // 7 + 30 + 60
    }

    @Test fun f4_flip_light_side() {
        val r = engine.roundDeltas(
            v("flip"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(3), card("draw_one"), card("wild_light"))),
                endedOnSide = FlipSide.LIGHT,
            ),
        ).deltas
        assertEquals(53, r.at("Alice")) // 3 + 10 + 40
    }

    @Test fun f5_no_mercy_knockout_and_uncounted_hand() {
        val r = engine.roundDeltas(
            v("no_mercy"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf(
                    "Bob" to hand(card("draw_four"), card("wild_draw_ten")),  // 20 + 50 = 70
                    "Cara" to hand(num(5), card("skip_everyone")),            // 5 + 20 = 25
                    "Dave" to hand(num(9)),                                   // eliminated → not scored
                ),
                knockouts = mapOf("Alice" to 1),
                eliminatedThisHand = setOf("Dave"),
            ),
        ).deltas
        assertEquals(345, r.at("Alice")) // 70 + 25 + 250
        assertEquals(0, r.at("Dave"))
    }

    @Test fun f6_no_mercy_last_player_standing() {
        val eval = engine.evaluateGameEnd(
            v("no_mercy"), ScoringMode.WINNER_TAKES, target = 1000,
            totals = mapOf("Alice" to 120, "Bob" to 0, "Cara" to 0, "Dave" to 0),
            allEntities = setOf("Alice", "Bob", "Cara", "Dave"),
            eliminatedFromGame = setOf("Bob", "Cara", "Dave"),
        )
        assertTrue(eval.gameOver)
        assertEquals(listOf("Alice"), eval.winners)
        assertEquals(WinReason.LAST_STANDING, eval.reason)
    }

    @Test fun f7_all_wild_no_numbers() {
        val r = engine.roundDeltas(
            v("all_wild"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(card("wild"), card("wild_forced_swap"), card("wild_skip_two"))),
            ),
        ).deltas
        assertEquals(120, r.at("Alice")) // 20 + 50 + 50
    }

    @Test fun f8_attack_own_table() {
        val r = engine.roundDeltas(
            v("attack"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(8), card("hit_2"), card("discard_all"), card("wild_hit_fire"))),
            ),
        ).deltas
        assertEquals(108, r.at("Alice")) // 8 + 20 + 30 + 50
    }

    @Test fun f9_triple_play() {
        val r = engine.roundDeltas(
            v("triple_play"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(4), card("discard_two_same_color"), card("wild_give_away"))),
            ),
        ).deltas
        assertEquals(84, r.at("Alice")) // 4 + 30 + 50
    }

    @Test fun f10_party() {
        val r = engine.roundDeltas(
            v("party"), ScoringMode.WINNER_TAKES,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf("Bob" to hand(num(6), card("point_taken"), card("wild_pile_up"), card("wild_drawn_together"))),
            ),
        ).deltas
        assertEquals(126, r.at("Alice")) // 6 + 20 + 50 + 50
    }

    // ── Joint winners (Liar's) ─────────────────────────────────────────────────────────────────

    @Test fun f11_liars_joint_winners_full_to_each() {
        val input = RoundInput(
            winners = listOf("Alice", "Bob"),
            loserHands = mapOf("Cara" to hand(num(4), card("draw_two"), card("wild_draw_four"))), // 74
        )
        val full = ScoringEngine(JointWinnerSplit.FULL_TO_EACH)
            .roundDeltas(v("liars"), ScoringMode.WINNER_TAKES, input).deltas
        assertEquals(74, full.at("Alice"))
        assertEquals(74, full.at("Bob"))
        assertEquals(0, full.at("Cara"))

        val split = ScoringEngine(JointWinnerSplit.SPLIT_EVENLY)
            .roundDeltas(v("liars"), ScoringMode.WINNER_TAKES, input).deltas
        assertEquals(37, split.at("Alice"))
        assertEquals(37, split.at("Bob"))
    }

    // ── Mode B (lowest_total) ──────────────────────────────────────────────────────────────────

    @Test fun f12_original_mode_b() {
        val r = engine.roundDeltas(
            v("original"), ScoringMode.LOWEST_TOTAL,
            RoundInput(
                winners = listOf("Alice"),
                loserHands = mapOf(
                    "Bob" to hand(num(9), card("skip")),  // 29
                    "Cara" to hand(card("wild")),         // 50
                ),
            ),
        ).deltas
        assertEquals(0, r.at("Alice"))
        assertEquals(29, r.at("Bob"))
        assertEquals(50, r.at("Cara"))
    }

    @Test fun f13_mode_b_game_end_lowest_wins() {
        val totals = mapOf("Alice" to 480, "Bob" to 345, "Cara" to 510)
        val eval = engine.evaluateGameEnd(
            v("original"), ScoringMode.LOWEST_TOTAL, target = 500,
            totals = totals,
            allEntities = setOf("Alice", "Bob", "Cara"),
        )
        assertTrue(eval.gameOver)
        assertEquals(listOf("Bob"), eval.winners) // min total, not the crosser
        assertEquals(WinReason.LOWEST_TOTAL, eval.reason)

        // Optional elimination reading: crossing the target eliminates; play continues.
        val elim = engine.evaluateGameEnd(
            v("original"), ScoringMode.LOWEST_TOTAL, target = 500,
            totals = totals,
            allEntities = setOf("Alice", "Bob", "Cara"),
            challengeElimination = true,
        )
        assertFalse(elim.gameOver)
        assertEquals(setOf("Cara"), elim.newlyEliminated)
    }

    // ── FACE_VALUE ─────────────────────────────────────────────────────────────────────────────

    @Test fun f14_face_value_resolution() {
        val original = v("original")
        for (n in 0..9) assertEquals(n, engine.cardValue(original, num(n)))
        assertEquals(50, engine.cardValue(original, card("wild")))
        assertEquals(20, engine.cardValue(original, card("skip")))
    }

    // ── Cross-variant invariants ───────────────────────────────────────────────────────────────

    @Test fun inv1_winner_takes_equals_sum_of_losers() {
        val variant = v("original")
        val input = RoundInput(
            winners = listOf("Alice"),
            loserHands = mapOf(
                "Bob" to hand(num(9), card("skip")),
                "Cara" to hand(card("wild"), num(3)),
            ),
        )
        val deltas = engine.roundDeltas(variant, ScoringMode.WINNER_TAKES, input).deltas
        val loserSum = input.loserHands.values.sumOf { engine.handValue(variant, it) }
        assertEquals(loserSum, deltas.at("Alice"))
    }

    @Test fun inv2_mode_b_conservation() {
        val variant = v("original")
        val input = RoundInput(
            winners = listOf("Alice"),
            loserHands = mapOf("Bob" to hand(num(9), card("skip")), "Cara" to hand(card("wild"))),
        )
        val deltas = engine.roundDeltas(variant, ScoringMode.LOWEST_TOTAL, input).deltas
        assertEquals(0, deltas.at("Alice"))
        for ((entity, tally) in input.loserHands) {
            assertEquals(engine.handValue(variant, tally), deltas.at(entity))
        }
    }

    @Test fun inv7_joint_winners_only_when_supported() {
        assertTrue(v("liars").supportsJointWinners)
        assertFalse(v("original").supportsJointWinners)
    }

    @Test fun inv8_target_and_mode_per_variant() {
        assertEquals(1000, v("no_mercy").target)
        assertEquals(500, v("original").target)
        assertEquals(500, v("flip").target)
        assertEquals(ScoringMode.WINNER_TAKES, v("original").scoringMode)
    }

    @Test fun config_all_eight_variants_parsed() {
        val ids = variants.map { it.id }.toSet()
        assertTrue(
            ids.containsAll(
                listOf("original", "flip", "liars", "no_mercy", "all_wild", "attack", "triple_play", "party"),
            ),
        )
    }
}
