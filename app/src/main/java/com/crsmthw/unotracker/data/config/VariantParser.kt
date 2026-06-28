package com.crsmthw.unotracker.data.config

import com.crsmthw.unotracker.data.config.dto.CardDto
import com.crsmthw.unotracker.data.config.dto.VariantDto
import com.crsmthw.unotracker.data.config.dto.VariantsRootDto
import com.crsmthw.unotracker.domain.Bonus
import com.crsmthw.unotracker.domain.CardCategory
import com.crsmthw.unotracker.domain.CardDef
import com.crsmthw.unotracker.domain.DrawRule
import com.crsmthw.unotracker.domain.ScoringMode
import com.crsmthw.unotracker.domain.UnoFailPenalty
import com.crsmthw.unotracker.domain.Variant
import com.google.gson.Gson
import com.google.gson.JsonElement

/**
 * Pure (JVM-only, no Android) parser that turns the bundled `variants.json` text into the non-null
 * [Variant] domain model. Kept Android-free so the scoring engine and its unit tests can share it.
 */
object VariantParser {

    private val gson = Gson()

    fun parse(json: String): List<Variant> {
        val root = gson.fromJson(json, VariantsRootDto::class.java)
        return root?.variants.orEmpty().mapNotNull { it.toDomainOrNull() }
    }

    private fun VariantDto.toDomainOrNull(): Variant? {
        val id = id ?: return null
        val (light, dark) = parseColors(colors)
        return Variant(
            id = id,
            displayName = displayName ?: id,
            players = players ?: "",
            ages = ages ?: "",
            deckSize = deckSize ?: 0,
            deckSizeRetail = deckSizeRetail,
            target = target ?: 500,
            scoringMode = ScoringMode.fromKey(scoringMode),
            altScoringMode = altScoringMode?.let { ScoringMode.fromKey(it) },
            alternateWinCondition = alternateWinCondition,
            winnerScoresZero = winnerScoresZero ?: false,
            unoFailPenalty = unoFailPenalty?.let { p ->
                UnoFailPenalty(type = p.type ?: "draw_cards", amount = p.amount ?: 0)
            },
            stackingAllowed = stackingAllowed ?: false,
            stackingRule = stackingRule,
            sevenZeroBuiltIn = sevenZeroBuiltIn ?: false,
            drawRule = DrawRule.fromKey(drawRule),
            sideDependentScoring = sideDependentScoring ?: false,
            supportsJointWinners = supportsJointWinners ?: false,
            supportsTeams = supportsTeams ?: false,
            twoPlayerReverseIsSkip = twoPlayerReverseIsSkip ?: true,
            mercyRuleCardCount = mercyRuleCardCount,
            eliminatedHandScored = eliminatedHandScored ?: true,
            bonuses = bonuses.orEmpty().mapNotNull { b ->
                val key = b.key ?: return@mapNotNull null
                Bonus(key = key, name = b.name ?: key, points = b.points ?: 0, manual = b.manual ?: false)
            },
            hasNumberCards = hasNumberCards ?: true,
            hasDrawCards = hasDrawCards ?: true,
            cantGoOutOn = cantGoOutOn.orEmpty(),
            lightColors = light,
            darkColors = dark,
            cards = cards.orEmpty().mapNotNull { it.toDomainOrNull() },
            notes = notes.orEmpty(),
            houseRules = houseRules.orEmpty(),
            sourceUrl = sourceUrl,
            verifiedAgainstMattelPdf = verifiedAgainstMattelPdf ?: false,
        )
    }

    private fun CardDto.toDomainOrNull(): CardDef? {
        val key = key ?: return null
        val (pts, faceValue) = parsePoints(points)
        return CardDef(
            key = key,
            name = name ?: key,
            category = parseCategory(category),
            points = pts,
            isFaceValue = faceValue,
            effect = effect ?: "",
            side = side,
            restricted = restricted ?: false,
            challengeable = challengeable ?: false,
            optional = optional ?: false,
        )
    }

    private fun parseCategory(raw: String?): CardCategory = when (raw?.uppercase()) {
        "NUMBER" -> CardCategory.NUMBER
        "WILD" -> CardCategory.WILD
        else -> CardCategory.ACTION
    }

    /** `points` is either a JSON number or the string "FACE_VALUE". Returns (points?, isFaceValue). */
    private fun parsePoints(el: JsonElement?): Pair<Int?, Boolean> {
        if (el == null || el.isJsonNull) return null to false
        return try {
            if (el.isJsonPrimitive && el.asJsonPrimitive.isString) {
                if (el.asString.equals("FACE_VALUE", ignoreCase = true)) null to true else null to false
            } else {
                el.asInt to false
            }
        } catch (_: Exception) {
            null to false
        }
    }

    /** `colors` is an array (classic), a `{ light:[], dark:[] }` object (Flip), or empty (All Wild). */
    private fun parseColors(el: JsonElement?): Pair<List<String>, List<String>> {
        if (el == null || el.isJsonNull) return emptyList<String>() to emptyList()
        return try {
            when {
                el.isJsonArray -> el.asJsonArray.map { it.asString } to emptyList()
                el.isJsonObject -> {
                    val obj = el.asJsonObject
                    val light = obj.getAsJsonArray("light")?.map { it.asString } ?: emptyList()
                    val dark = obj.getAsJsonArray("dark")?.map { it.asString } ?: emptyList()
                    light to dark
                }
                else -> emptyList<String>() to emptyList()
            }
        } catch (_: Exception) {
            emptyList<String>() to emptyList()
        }
    }
}
