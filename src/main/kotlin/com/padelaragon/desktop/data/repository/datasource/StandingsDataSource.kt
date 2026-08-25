package com.padelaragon.desktop.data.repository.datasource

import com.padelaragon.desktop.data.model.StandingRow

interface StandingsDataSource {
    suspend fun getStandings(groupId: Int): List<StandingRow>
    suspend fun refreshStandings(groupId: Int): List<StandingRow>
}
