package com.crsmthw.unotracker.data.config.dto

import com.google.gson.JsonElement

/**
 * Gson DTOs for `variants.json`. EVERY field is nullable: Gson populates via Unsafe and bypasses
 * Kotlin constructors/defaults, so a missing field would land as a real null and crash a minified
 * release build. The parser normalises these into the non-null [com.crsmthw.unotracker.domain.Variant]
 * domain model. Kept by ProGuard (see proguard-rules.pro). Playbook §2.
 */
class VariantsRootDto {
    var variants: List<VariantDto>? = null
}

class VariantDto {
    var id: String? = null
    var displayName: String? = null
    var players: String? = null
    var ages: String? = null
    var deckSize: Int? = null
    var deckSizeRetail: Int? = null
    var target: Int? = null
    var scoringMode: String? = null
    var altScoringMode: String? = null
    var alternateWinCondition: String? = null
    var winnerScoresZero: Boolean? = null
    var unoFailPenalty: UnoFailPenaltyDto? = null
    var stackingAllowed: Boolean? = null
    var stackingRule: String? = null
    var sevenZeroBuiltIn: Boolean? = null
    var drawRule: String? = null
    var sideDependentScoring: Boolean? = null
    var scoreSideField: String? = null
    var supportsJointWinners: Boolean? = null
    var supportsTeams: Boolean? = null
    var twoPlayerReverseIsSkip: Boolean? = null
    var mercyRuleCardCount: Int? = null
    var eliminatedHandScored: Boolean? = null
    var hasNumberCards: Boolean? = null
    var hasDrawCards: Boolean? = null
    var cantGoOutOn: List<String>? = null
    var bonuses: List<BonusDto>? = null
    var colors: JsonElement? = null          // array OR { light:[], dark:[] } OR []
    var cards: List<CardDto>? = null
    var notes: List<String>? = null
    var houseRules: List<String>? = null
    var sourceUrl: String? = null
    var verifiedAgainstMattelPdf: Boolean? = null
    var rulesAreReworded: Boolean? = null
}

class CardDto {
    var key: String? = null
    var name: String? = null
    var category: String? = null
    var points: JsonElement? = null          // a number, or the string "FACE_VALUE"
    var effect: String? = null
    var side: String? = null
    var restricted: Boolean? = null
    var challengeable: Boolean? = null
    var optional: Boolean? = null
    var wrongChallengerDraws: JsonElement? = null
}

class BonusDto {
    var key: String? = null
    var name: String? = null
    var points: Int? = null
    var manual: Boolean? = null
}

class UnoFailPenaltyDto {
    var type: String? = null
    var amount: Int? = null
}
