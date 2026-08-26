package com.padelaragon.desktop.data.repository

import com.padelaragon.desktop.util.Logger

import com.padelaragon.desktop.data.local.entity.LeagueGroupEntity
import com.padelaragon.desktop.data.model.League
import com.padelaragon.desktop.data.model.LeagueGroup
import com.padelaragon.desktop.data.parser.GroupParser
import com.padelaragon.desktop.data.repository.ScrapingService.Companion.BASE_URL
import com.padelaragon.desktop.data.repository.datasource.GroupDataSource
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

class GroupRepository(
    private val league: League,
    private val scraping: ScrapingService,
    private val groupParser: GroupParser = GroupParser(),
    private val standingsRepo: StandingsRepository,
    private val matchResultRepo: MatchResultRepository
) : GroupDataSource {

    @Volatile
    private var cachedGroups: List<LeagueGroup>? = null

    override suspend fun getGroups(): List<LeagueGroup> {
        cachedGroups?.let { return it }

        val roomGroups = scraping.db.leagueGroupDao().getByLeagueId(league.id).map { it.toModel() }
        if (roomGroups.isNotEmpty()) {
            cachedGroups = roomGroups
            return roomGroups
        }

        val url = "${BASE_URL}Ligas_Calendario.asp?Liga=${league.id}"
        Logger.d("GroupRepo", "Fetching groups from: $url")
        val html = scraping.withSemaphore { scraping.fetcher.get(url) }
        val groups = groupParser.parse(html)
        cachedGroups = groups

        scraping.db.leagueGroupDao().deleteByLeagueId(league.id)
        scraping.db.leagueGroupDao().insertAll(groups.map { LeagueGroupEntity.fromModel(league.id, it) })

        return groups
    }

    override suspend fun refreshGroups(): List<LeagueGroup> {
        cachedGroups = null
        scraping.db.leagueGroupDao().deleteByLeagueId(league.id)
        return getGroups()
    }

    override suspend fun prefetchAllGroups() {
        val groups = cachedGroups ?: return
        coroutineScope {
            groups.map { group ->
                launch {
                    runCatching {
                        coroutineScope {
                            launch { standingsRepo.getStandings(group.id) }
                            launch { matchResultRepo.getAllMatchResults(group.id) }
                        }
                    }
                }
            }
        }
    }

    override suspend fun prefetchGroups(groupIds: List<Int>) {
        val groups = cachedGroups ?: return
        val targetGroups = groups.filter { it.id in groupIds }
        coroutineScope {
            targetGroups.map { group ->
                launch {
                    runCatching {
                        coroutineScope {
                            launch { standingsRepo.getStandings(group.id) }
                            launch { matchResultRepo.getAllMatchResults(group.id) }
                        }
                    }
                }
            }
        }
    }

    fun getCachedGroups(): List<LeagueGroup>? = cachedGroups
}
