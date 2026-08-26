package com.padelaragon.desktop.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.padelaragon.desktop.data.model.League
import com.padelaragon.desktop.data.model.MatchDetail
import com.padelaragon.desktop.data.model.MatchResult
import com.padelaragon.desktop.di.AppContainer
import com.padelaragon.desktop.di.MultiLeagueContainer
import com.padelaragon.desktop.domain.usecase.StartupPreloader
import com.padelaragon.desktop.ui.screen.GroupsMasterDetailScreen
import com.padelaragon.desktop.ui.screen.LeagueChooserScreen
import com.padelaragon.desktop.ui.screen.PlayerDetailScreen
import com.padelaragon.desktop.ui.screen.StartupLoadingScreen
import com.padelaragon.desktop.ui.screen.TeamScreen
import com.padelaragon.desktop.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.PlayerDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.TeamViewModelFactory

/**
 * Desktop replacement for the Android NavGraph (androidx.navigation-compose).
 * Uses a simple in-memory back stack of screen destinations instead, since
 * this app only has a few linear destinations (startup loading -> league chooser -> groups -> team -> player).
 * The Groups + GroupDetail flow is combined into a single master-detail
 * destination (persistent left sidebar of categories + main panel), desktop-only.
 */
private sealed class Destination {
    data object LeagueChooser : Destination()
    data class Groups(val league: League) : Destination()
    data class TeamDetail(val league: League, val teamId: Int, val teamName: String, val groupId: Int) : Destination()
    data class PlayerDetail(
        val league: League,
        val playerName: String,
        val teamId: Int,
        val matchDetails: Map<String, MatchDetail>,
        val playedMatches: List<MatchResult>
    ) : Destination()
}

@Composable
fun AppNavHost(multiLeagueContainer: MultiLeagueContainer) {
    val preloader = remember {
        StartupPreloader { league ->
            val container = multiLeagueContainer[league]
            container.groupDataSource.getGroups()
            container.prefetchGroupsUseCase.prefetchAll()
        }
    }
    val progress by preloader.progress.collectAsState()
    var preloadStarted by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!preloadStarted) {
            preloadStarted = true
            preloader.preloadAll()
        }
    }

    if (!progress.isDone) {
        StartupLoadingScreen(progress)
        return
    }

    var backStack by remember { mutableStateOf(listOf<Destination>(Destination.LeagueChooser)) }
    val current = backStack.last()

    fun push(destination: Destination) {
        backStack = backStack + destination
    }

    fun pop() {
        if (backStack.size > 1) {
            backStack = backStack.dropLast(1)
        }
    }

    when (val destination = current) {
        is Destination.LeagueChooser -> {
            LeagueChooserScreen(onLeagueSelected = { league -> push(Destination.Groups(league)) })
        }

        is Destination.Groups -> {
            val container: AppContainer = multiLeagueContainer[destination.league]
            val navigateToTeam: (Int, String, Int) -> Unit = { teamId, teamName, groupId ->
                push(Destination.TeamDetail(destination.league, teamId, teamName, groupId))
            }
            GroupsMasterDetailScreen(
                onTeamClick = navigateToTeam,
                title = destination.league.displayName,
                onBack = { backStack = listOf(Destination.LeagueChooser) },
                viewModelKey = "groups_${destination.league.id}",
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
            val container: AppContainer = multiLeagueContainer[destination.league]
            TeamScreen(
                teamId = destination.teamId,
                teamName = destination.teamName,
                groupId = destination.groupId,
                onBack = { pop() },
                onTeamClick = { teamId, teamName, groupId ->
                    push(Destination.TeamDetail(destination.league, teamId, teamName, groupId))
                },
                onPlayerClick = { playerName, matchDetails, playedMatches ->
                    push(Destination.PlayerDetail(destination.league, playerName, destination.teamId, matchDetails, playedMatches))
                },
                viewModelKey = "team_${destination.league.id}_${destination.teamId}",
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
                viewModelKey = "player_${destination.league.id}_${destination.teamId}_${destination.playerName}",
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

