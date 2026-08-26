package com.padelaragon.desktop.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.padelaragon.desktop.data.model.MatchDetail
import com.padelaragon.desktop.data.model.MatchResult
import com.padelaragon.desktop.di.AppContainer
import com.padelaragon.desktop.ui.screen.GroupsMasterDetailScreen
import com.padelaragon.desktop.ui.screen.PlayerDetailScreen
import com.padelaragon.desktop.ui.screen.TeamScreen
import com.padelaragon.desktop.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.PlayerDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.TeamViewModelFactory

/**
 * Desktop replacement for the Android NavGraph (androidx.navigation-compose).
 * Uses a simple in-memory back stack of screen destinations instead, since
 * this app only has a few linear destinations (groups -> team -> player).
 * The Groups + GroupDetail flow is combined into a single master-detail
 * destination (persistent left sidebar of categories + main panel), desktop-only.
 */
private sealed class Destination {
    data object Groups : Destination()
    data class TeamDetail(val teamId: Int, val teamName: String, val groupId: Int) : Destination()
    data class PlayerDetail(
        val playerName: String,
        val teamId: Int,
        val matchDetails: Map<String, MatchDetail>,
        val playedMatches: List<MatchResult>
    ) : Destination()
}

@Composable
fun AppNavHost(container: AppContainer) {
    var backStack by remember { mutableStateOf(listOf<Destination>(Destination.Groups)) }
    val current = backStack.last()

    fun push(destination: Destination) {
        backStack = backStack + destination
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    val navigateToTeam: (Int, String, Int) -> Unit = { teamId, teamName, groupId ->
        push(Destination.TeamDetail(teamId, teamName, groupId))
    }

    when (val destination = current) {
        is Destination.Groups -> {
            GroupsMasterDetailScreen(
                onTeamClick = navigateToTeam,
                listViewModelFactory = GroupListViewModelFactory(
                    container.groupDataSource,
                    container.favoritesDataSource,
                    container.prefetchGroupsUseCase
                ),
                groupDetailViewModelFactory = { groupId, groupName ->
                    GroupDetailViewModelFactory(
                        groupId, groupName,
                        container.standingsDataSource,
                        container.matchResultDataSource,
                        container.matchDetailDataSource,
                        container.favoritesDataSource
                    )
                }
            )
        }

        is Destination.TeamDetail -> {
            TeamScreen(
                teamId = destination.teamId,
                teamName = destination.teamName,
                groupId = destination.groupId,
                onBack = { pop() },
                onTeamClick = navigateToTeam,
                onPlayerClick = { playerName, matchDetails, playedMatches ->
                    push(Destination.PlayerDetail(playerName, destination.teamId, matchDetails, playedMatches))
                },
                viewModelFactory = TeamViewModelFactory(
                    destination.teamId, destination.teamName, destination.groupId,
                    container.teamDataSource,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource
                )
            )
        }

        is Destination.PlayerDetail -> {
            PlayerDetailScreen(
                playerName = destination.playerName,
                teamId = destination.teamId,
                onBack = { pop() },
                viewModelFactory = PlayerDetailViewModelFactory(
                    destination.playerName,
                    destination.teamId,
                    destination.matchDetails,
                    destination.playedMatches
                )
            )
        }
    }
}
