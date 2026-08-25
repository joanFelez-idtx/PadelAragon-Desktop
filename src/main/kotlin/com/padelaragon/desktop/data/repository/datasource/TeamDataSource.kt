package com.padelaragon.desktop.data.repository.datasource

import com.padelaragon.desktop.data.model.TeamDetail
import com.padelaragon.desktop.data.model.TeamInfo

interface TeamDataSource {
    suspend fun getTeamDetail(teamId: Int, teamHref: String = ""): TeamDetail?
    suspend fun getTeamInfo(teamId: Int, teamName: String): TeamInfo?
    suspend fun getTeamInfoForGroup(teamId: Int, teamName: String, groupId: Int): TeamInfo?
}
