package com.padelaragon.desktop.data.model

/**
 * Wins/losses and pareja (1/2/3) counts scoped to either home or away matches only.
 */
data class PlayerStatsSplit(
    val wins: Int = 0,
    val losses: Int = 0,
    val pair1Count: Int = 0,
    val pair2Count: Int = 0,
    val pair3Count: Int = 0
)

data class PlayerStats(
    val name: String,
    val home: PlayerStatsSplit,
    val away: PlayerStatsSplit
)
