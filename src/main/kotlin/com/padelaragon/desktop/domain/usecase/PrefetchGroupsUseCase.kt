package com.padelaragon.desktop.domain.usecase

import com.padelaragon.desktop.data.model.TeamInfo
import com.padelaragon.desktop.data.repository.datasource.GroupDataSource
import com.padelaragon.desktop.data.repository.datasource.MatchResultDataSource
import com.padelaragon.desktop.data.repository.datasource.StandingsDataSource
import com.padelaragon.desktop.data.repository.datasource.TeamDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class PrefetchGroupsUseCase(
    private val groupDataSource: GroupDataSource,
    private val standingsDataSource: StandingsDataSource,
    private val matchResultDataSource: MatchResultDataSource
) {
    suspend fun prefetchAll() {
        groupDataSource.prefetchAllGroups()
    }

    suspend fun prefetchFavorites(groupIds: List<Int>) {
        groupDataSource.prefetchGroups(groupIds)
    }
}
