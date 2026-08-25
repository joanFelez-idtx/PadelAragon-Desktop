package com.padelaragon.desktop.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.padelaragon.desktop.data.model.MatchDetail
import com.padelaragon.desktop.data.model.MatchResult
import com.padelaragon.desktop.data.model.PlayerDetailStats
import com.padelaragon.desktop.domain.usecase.ComputePlayerDetailStatsUseCase

/**
 * Desktop-only ViewModel for the player detail screen. Takes the match/detail
 * data already loaded by [TeamViewModel]'s player-stats tab, so opening a
 * player's detail screen doesn't trigger any additional network calls.
 */
class PlayerDetailViewModel(
    playerName: String,
    teamId: Int,
    allDetails: Map<String, MatchDetail>,
    playedMatches: List<MatchResult>,
    computePlayerDetailStatsUseCase: ComputePlayerDetailStatsUseCase = ComputePlayerDetailStatsUseCase()
) : ViewModel() {

    val stats: PlayerDetailStats =
        computePlayerDetailStatsUseCase(allDetails, playedMatches, teamId, playerName)
}

class PlayerDetailViewModelFactory(
    private val playerName: String,
    private val teamId: Int,
    private val allDetails: Map<String, MatchDetail>,
    private val playedMatches: List<MatchResult>
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: kotlin.reflect.KClass<T>, extras: CreationExtras): T {
        require(modelClass == PlayerDetailViewModel::class) {
            "Unknown ViewModel class: $modelClass"
        }
        return PlayerDetailViewModel(playerName, teamId, allDetails, playedMatches) as T
    }
}
