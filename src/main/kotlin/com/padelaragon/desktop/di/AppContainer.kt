package com.padelaragon.desktop.di

import com.padelaragon.desktop.data.favorites.FavoritesManager
import com.padelaragon.desktop.data.local.AppDatabase
import com.padelaragon.desktop.data.network.HtmlFetcher
import com.padelaragon.desktop.data.repository.GroupRepository
import com.padelaragon.desktop.data.repository.MatchDetailRepository
import com.padelaragon.desktop.data.repository.MatchResultRepository
import com.padelaragon.desktop.data.repository.ScrapingService
import com.padelaragon.desktop.data.repository.StandingsRepository
import com.padelaragon.desktop.data.repository.TeamDetailRepository
import com.padelaragon.desktop.data.repository.datasource.FavoritesDataSource
import com.padelaragon.desktop.data.repository.datasource.GroupDataSource
import com.padelaragon.desktop.data.repository.datasource.MatchDetailDataSource
import com.padelaragon.desktop.data.repository.datasource.MatchResultDataSource
import com.padelaragon.desktop.data.repository.datasource.StandingsDataSource
import com.padelaragon.desktop.data.repository.datasource.TeamDataSource
import com.padelaragon.desktop.domain.usecase.PrefetchGroupsUseCase

class AppContainer(database: AppDatabase, cacheDir: java.io.File? = null) {
    private val scraping = ScrapingService(database, HtmlFetcher(cacheDir))
    private val standingsRepository = StandingsRepository(scraping)
    private val matchResultRepository = MatchResultRepository(scraping)
    private val matchDetailRepository = MatchDetailRepository(scraping)
    private val groupRepository = GroupRepository(scraping, standingsRepo = standingsRepository, matchResultRepo = matchResultRepository)
    private val teamDetailRepository = TeamDetailRepository(
        scraping,
        groupDataSource = groupRepository,
        standingsDataSource = standingsRepository,
        matchResultDataSource = matchResultRepository
    )

    val groupDataSource: GroupDataSource = groupRepository
    val standingsDataSource: StandingsDataSource = standingsRepository
    val matchResultDataSource: MatchResultDataSource = matchResultRepository
    val teamDataSource: TeamDataSource = teamDetailRepository
    val matchDetailDataSource: MatchDetailDataSource = matchDetailRepository
    val favoritesDataSource: FavoritesDataSource = FavoritesManager
    val prefetchGroupsUseCase = PrefetchGroupsUseCase(groupRepository, standingsRepository, matchResultRepository)
}
