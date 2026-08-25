package com.padelaragon.desktop.domain.usecase

import com.padelaragon.desktop.data.model.MatchDetail
import com.padelaragon.desktop.data.model.MatchResult
import com.padelaragon.desktop.data.model.PairDetail
import com.padelaragon.desktop.data.model.SetScore
import org.junit.Assert.assertEquals
import org.junit.Test

class ComputePlayerDetailStatsUseCaseTest {

    private val useCase = ComputePlayerDetailStatsUseCase()

    private fun match(jornada: Int, localTeamId: Int, visitorTeamId: Int, detailUrl: String) = MatchResult(
        localTeam = "Local",
        localTeamId = localTeamId,
        visitorTeam = "Visitor",
        visitorTeamId = visitorTeamId,
        localScore = "2",
        visitorScore = "1",
        date = null,
        venue = null,
        jornada = jornada,
        detailUrl = detailUrl
    )

    @Test
    fun `sums games won and lost for the target player as local team`() {
        val teamId = 1
        val detailUrl = "match1"
        val detail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = "Bob",
                    visitorPlayer1 = "Carol",
                    visitorPlayer2 = "Dave",
                    sets = listOf(SetScore(6, 4), SetScore(3, 6), SetScore(6, 2))
                )
            )
        )
        val playedMatches = listOf(match(jornada = 1, localTeamId = teamId, visitorTeamId = 2, detailUrl = detailUrl))

        val stats = useCase(mapOf(detailUrl to detail), playedMatches, teamId, "Alice")

        assertEquals(15, stats.gamesWon) // 6 + 3 + 6
        assertEquals(12, stats.gamesLost) // 4 + 6 + 2
        assertEquals(1, stats.topPartners.size)
        assertEquals("Bob", stats.topPartners.first().name)
        assertEquals(1, stats.topPartners.first().matchesTogether)
    }

    @Test
    fun `sums games for the target player as visitor team and swaps score sides`() {
        val teamId = 2
        val detailUrl = "match1"
        val detail = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Carol",
                    localPlayer2 = "Dave",
                    visitorPlayer1 = "Alice",
                    visitorPlayer2 = "Bob",
                    sets = listOf(SetScore(6, 4), SetScore(3, 6))
                )
            )
        )
        val playedMatches = listOf(match(jornada = 1, localTeamId = 1, visitorTeamId = teamId, detailUrl = detailUrl))

        val stats = useCase(mapOf(detailUrl to detail), playedMatches, teamId, "Alice")

        // Alice is on the visitor side, so games won/lost should use the visitor score column.
        assertEquals(10, stats.gamesWon) // 4 + 6
        assertEquals(9, stats.gamesLost) // 6 + 3
    }

    @Test
    fun `ranks top 3 partners by frequency`() {
        val teamId = 1
        fun detailWithPartner(partner: String) = MatchDetail(
            pairs = listOf(
                PairDetail(
                    pairNumber = 1,
                    localPlayer1 = "Alice",
                    localPlayer2 = partner,
                    visitorPlayer1 = "X",
                    visitorPlayer2 = "Y",
                    sets = listOf(SetScore(6, 0))
                )
            )
        )

        val allDetails = mapOf(
            "m1" to detailWithPartner("Bob"),
            "m2" to detailWithPartner("Bob"),
            "m3" to detailWithPartner("Bob"),
            "m4" to detailWithPartner("Carol"),
            "m5" to detailWithPartner("Carol"),
            "m6" to detailWithPartner("Dave"),
            "m7" to detailWithPartner("Eve")
        )
        val playedMatches = allDetails.keys.mapIndexed { index, url ->
            match(jornada = index + 1, localTeamId = teamId, visitorTeamId = 99, detailUrl = url)
        }

        val stats = useCase(allDetails, playedMatches, teamId, "Alice")

        assertEquals(listOf("Bob", "Carol"), stats.topPartners.take(2).map { it.name })
        assertEquals(3, stats.topPartners.size)
        assertEquals(3, stats.topPartners[0].matchesTogether)
        assertEquals(2, stats.topPartners[1].matchesTogether)
    }
}
