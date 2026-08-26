package com.padelaragon.desktop.domain.usecase

import com.padelaragon.desktop.data.model.Gender

/**
 * A player considered for pairing, with an already-resolved age.
 */
data class AgedPlayer(
    val name: String,
    val age: Int
)

/** One pair (pareja) within a suggested combination, matched against one of the three age-sum tiers. */
data class SuggestedPair(
    val player1: AgedPlayer,
    val player2: AgedPlayer,
    val tierIndex: Int, // 0 = Pareja 1 (lowest threshold), 1 = Pareja 2, 2 = Pareja 3 (highest threshold)
    val requiredSum: Int
) {
    val ageSum: Int get() = player1.age + player2.age
}

/** A full valid combination of 3 disjoint pairs, one per tier, sorted by tier index. */
data class CoupleCombination(
    val pairs: List<SuggestedPair>
)

/**
 * Generates every possible way to split exactly 6 selected players (i.e. a full match lineup:
 * 3 pairs) into 3 disjoint pairs ("parejas") such that each pair's combined age meets or
 * exceeds the minimum required for its tier, per the following rules (age-sum thresholds, in
 * ascending order):
 *
 * - Masculina: Pareja 1 >= 95, Pareja 2 >= 100, Pareja 3 >= 105
 * - Femenina:  Pareja 1 >= 80, Pareja 2 >= 85,  Pareja 3 >= 90
 *
 * Exactly 6 players must be provided (a match fields 3 pairs = 6 players). Requiring an exact
 * lineup instead of an arbitrary larger candidate pool keeps the number of results meaningful:
 * allowing any subset of a larger selection causes a combinatorial explosion of largely
 * redundant groupings once age thresholds are easily cleared.
 */
class GeneratePossibleCouplesUseCase {
    companion object {
        val MASCULINA_THRESHOLDS = listOf(95, 100, 105)
        val FEMENINA_THRESHOLDS = listOf(80, 85, 90)
        const val REQUIRED_PLAYER_COUNT = 6
    }

    sealed interface Result {
        data class Success(val combinations: List<CoupleCombination>) : Result
        object NoCombinationsPossible : Result
        object RequiresExactlySixPlayers : Result
    }

    operator fun invoke(players: List<AgedPlayer>, gender: Gender): Result {
        if (players.size != REQUIRED_PLAYER_COUNT) return Result.RequiresExactlySixPlayers

        val thresholds = if (gender == Gender.MASCULINA) MASCULINA_THRESHOLDS else FEMENINA_THRESHOLDS
        val combinations = mutableListOf<CoupleCombination>()
        val seenKeys = HashSet<String>()

        findTriplesOfPairs(players) { threePairs ->
            // Sort the 3 pairs ascending by sum; a valid assignment exists iff each sum
            // (once sorted) meets its corresponding threshold (ascending too).
            val sortedPairs = threePairs.sortedBy { it.first.age + it.second.age }
            val sums = sortedPairs.map { it.first.age + it.second.age }
            val isValid = sums.indices.all { i -> sums[i] >= thresholds[i] }
            if (isValid) {
                val key = sortedPairs.joinToString("|") { (a, b) ->
                    listOf(a.name, b.name).sorted().joinToString(",")
                }
                if (seenKeys.add(key)) {
                    val suggestedPairs = sortedPairs.mapIndexed { index, (a, b) ->
                        SuggestedPair(a, b, tierIndex = index, requiredSum = thresholds[index])
                    }
                    combinations += CoupleCombination(suggestedPairs)
                }
            }
        }

        return if (combinations.isEmpty()) Result.NoCombinationsPossible else Result.Success(combinations)
    }

    /**
     * Enumerates every way to pick 3 disjoint, unordered pairs from [players] (players not
     * included in any pair are simply left out) and invokes [onTriple] for each.
     */
    private fun findTriplesOfPairs(
        players: List<AgedPlayer>,
        onTriple: (List<Pair<AgedPlayer, AgedPlayer>>) -> Unit
    ) {
        val n = players.size
        for (i1 in 0 until n) {
            for (j1 in i1 + 1 until n) {
                val pairA = players[i1] to players[j1]
                for (i2 in 0 until n) {
                    if (i2 == i1 || i2 == j1) continue
                    for (j2 in i2 + 1 until n) {
                        if (j2 == i1 || j2 == j1) continue
                        // Enforce a canonical ordering between pair A and pair B to avoid
                        // generating the same unordered pair-of-pairs twice.
                        if (i2 < i1 || (i2 == i1 && j2 <= j1)) continue
                        val pairB = players[i2] to players[j2]
                        for (i3 in 0 until n) {
                            if (i3 == i1 || i3 == j1 || i3 == i2 || i3 == j2) continue
                            for (j3 in i3 + 1 until n) {
                                if (j3 == i1 || j3 == j1 || j3 == i2 || j3 == j2) continue
                                if (i3 < i2 || (i3 == i2 && j3 <= j2)) continue
                                val pairC = players[i3] to players[j3]
                                onTriple(listOf(pairA, pairB, pairC))
                            }
                        }
                    }
                }
            }
        }
    }
}
