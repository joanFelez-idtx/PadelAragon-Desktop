package com.padelaragon.desktop.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.padelaragon.desktop.di.AppContainer
import com.padelaragon.desktop.ui.screen.GroupDetailScreen
import com.padelaragon.desktop.ui.screen.GroupListScreen
import com.padelaragon.desktop.ui.screen.TeamScreen
import com.padelaragon.desktop.ui.viewmodel.GroupDetailViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.GroupListViewModelFactory
import com.padelaragon.desktop.ui.viewmodel.TeamViewModelFactory

/**
 * Desktop replacement for the Android NavGraph (androidx.navigation-compose).
 * Uses a simple in-memory back stack of screen destinations instead, since
 * this app only has three linear destinations (groups -> group -> team).
 */
private sealed class Destination {
    data object Groups : Destination()
    data class GroupDetail(val groupId: Int, val groupName: String) : Destination()
    data class TeamDetail(val teamId: Int, val teamName: String, val groupId: Int) : Destination()
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
            GroupListScreen(
                onGroupClick = { groupId, groupName -> push(Destination.GroupDetail(groupId, groupName)) },
                viewModelFactory = GroupListViewModelFactory(
                    container.groupDataSource,
                    container.favoritesDataSource,
                    container.prefetchGroupsUseCase
                )
            )
        }

        is Destination.GroupDetail -> {
            GroupDetailScreen(
                groupId = destination.groupId,
                groupName = destination.groupName,
                onBack = { pop() },
                onTeamClick = navigateToTeam,
                viewModelFactory = GroupDetailViewModelFactory(
                    destination.groupId, destination.groupName,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource,
                    container.favoritesDataSource
                )
            )
        }

        is Destination.TeamDetail -> {
            TeamScreen(
                teamId = destination.teamId,
                teamName = destination.teamName,
                groupId = destination.groupId,
                onBack = { pop() },
                onTeamClick = navigateToTeam,
                viewModelFactory = TeamViewModelFactory(
                    destination.teamId, destination.teamName, destination.groupId,
                    container.teamDataSource,
                    container.standingsDataSource,
                    container.matchResultDataSource,
                    container.matchDetailDataSource
                )
            )
        }
    }
}
