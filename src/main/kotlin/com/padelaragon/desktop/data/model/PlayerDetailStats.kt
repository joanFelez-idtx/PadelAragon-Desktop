package com.padelaragon.desktop.data.model

/**
 * Detailed desktop-only statistics for a single player: total games won/lost
 * across all their sets, and the teammates they've partnered with most often.
 */
data class PlayerDetailStats(
    val name: String,
    val gamesWon: Int,
    val gamesLost: Int,
    val topPartners: List<PartnerCount>,
    val matches: List<PlayerMatchRecord> = emptyList()
)

data class PartnerCount(
    val name: String,
    val matchesTogether: Int
)

/**
 * A single match the player took part in, with the opposing team/players and
 * the set-by-set result from the player's own side of the court.
 */
data class PlayerMatchRecord(
    val jornada: Int,
    val date: String?,
    val partnerName: String,
    val opponentTeam: String,
    val opponentPlayer1: String,
    val opponentPlayer2: String,
    val gamesWon: Int,
    val gamesLost: Int,
    val won: Boolean
)
