package com.padelaragon.desktop.di

import com.padelaragon.desktop.data.favorites.FavoritesManager
import com.padelaragon.desktop.data.local.AppDatabase
import com.padelaragon.desktop.data.model.League
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

/**
 * Per-league dependency container. One instance is built per [League] (Absoluta/Veteranos/Menores)
 * sharing the underlying [AppDatabase] and [HtmlFetcher], but keeping independent in-memory caches
 * and Room queries scoped to that league's id.
 */
class AppContainer(val league: League, database: AppDatabase, cacheDir: java.io.File? = null) {
    private val scraping = ScrapingService(database, HtmlFetcher(cacheDir))
    private val standingsRepository = StandingsRepository(league, scraping)
    private val matchResultRepository = MatchResultRepository(league, scraping)
    private val matchDetailRepository = MatchDetailRepository(scraping)
    private val groupRepository = GroupRepository(league, scraping, standingsRepo = standingsRepository, matchResultRepo = matchResultRepository)
    private val teamDetailRepository = TeamDetailRepository(
        league,
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
    val favoritesDataSource: FavoritesDataSource = FavoritesManager(league)
    val prefetchGroupsUseCase = PrefetchGroupsUseCase(groupRepository, standingsRepository, matchResultRepository)
}

/**
 * Holds one [AppContainer] per [League], built once (typically at app startup) and shared by
 * the whole navigation graph so switching between leagues doesn't re-create repositories/caches.
 */
class MultiLeagueContainer(database: AppDatabase, cacheDir: java.io.File? = null) {
    val containers: Map<League, AppContainer> = League.entries.associateWith { league ->
        AppContainer(league, database, cacheDir)
    }

    operator fun get(league: League): AppContainer = containers.getValue(league)
}
