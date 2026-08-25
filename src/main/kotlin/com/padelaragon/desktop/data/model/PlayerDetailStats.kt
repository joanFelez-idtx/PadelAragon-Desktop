package com.padelaragon.desktop.data.model

/**
 * Detailed desktop-only statistics for a single player: total games won/lost
 * across all their sets, and the teammates they've partnered with most often.
 */
data class PlayerDetailStats(
    val name: String,
    val gamesWon: Int,
    val gamesLost: Int,
    val topPartners: List<PartnerCount>
)

data class PartnerCount(
    val name: String,
    val matchesTogether: Int
)
