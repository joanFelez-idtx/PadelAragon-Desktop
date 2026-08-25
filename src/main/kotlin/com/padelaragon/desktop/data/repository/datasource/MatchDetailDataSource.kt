package com.padelaragon.desktop.data.repository.datasource

import com.padelaragon.desktop.data.model.MatchDetail
import com.padelaragon.desktop.data.model.MatchResult

interface MatchDetailDataSource {
    suspend fun getMatchDetail(detailUrl: String): MatchDetail?
    suspend fun prefetchMatchDetails(results: List<MatchResult>)
}
