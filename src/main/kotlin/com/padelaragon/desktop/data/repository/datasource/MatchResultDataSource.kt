package com.padelaragon.desktop.data.repository.datasource

import com.padelaragon.desktop.data.model.MatchResult

interface MatchResultDataSource {
    suspend fun getMatchResults(groupId: Int, jornada: Int): List<MatchResult>
    suspend fun getAllMatchResults(groupId: Int): Map<Int, List<MatchResult>>
    suspend fun refreshMatchResults(groupId: Int): Map<Int, List<MatchResult>>
    suspend fun getJornadas(groupId: Int): List<Int>
}
