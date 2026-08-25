package com.padelaragon.desktop.data.repository.datasource

import com.padelaragon.desktop.data.model.LeagueGroup

interface GroupDataSource {
    suspend fun getGroups(): List<LeagueGroup>
    suspend fun refreshGroups(): List<LeagueGroup>
    suspend fun prefetchAllGroups()
    suspend fun prefetchGroups(groupIds: List<Int>)
}
