package com.padelaragon.desktop.data.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import com.padelaragon.desktop.data.local.dao.CacheTimestampDao
import com.padelaragon.desktop.data.local.dao.JornadaDao
import com.padelaragon.desktop.data.local.dao.LeagueGroupDao
import com.padelaragon.desktop.data.local.dao.MatchDetailDao
import com.padelaragon.desktop.data.local.dao.MatchResultDao
import com.padelaragon.desktop.data.local.dao.StandingRowDao
import com.padelaragon.desktop.data.local.dao.TeamDetailDao
import com.padelaragon.desktop.data.local.entity.CacheTimestamp
import com.padelaragon.desktop.data.local.entity.JornadaEntity
import com.padelaragon.desktop.data.local.entity.LeagueGroupEntity
import com.padelaragon.desktop.data.local.entity.MatchDetailPairEntity
import com.padelaragon.desktop.data.local.entity.MatchResultEntity
import com.padelaragon.desktop.data.local.entity.PlayerEntity
import com.padelaragon.desktop.data.local.entity.StandingRowEntity
import com.padelaragon.desktop.data.local.entity.TeamDetailEntity

@Database(
    entities = [
        LeagueGroupEntity::class,
        StandingRowEntity::class,
        MatchResultEntity::class,
        MatchDetailPairEntity::class,
        TeamDetailEntity::class,
        PlayerEntity::class,
        JornadaEntity::class,
        CacheTimestamp::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun leagueGroupDao(): LeagueGroupDao
    abstract fun standingRowDao(): StandingRowDao
    abstract fun matchResultDao(): MatchResultDao
    abstract fun matchDetailDao(): MatchDetailDao
    abstract fun teamDetailDao(): TeamDetailDao
    abstract fun jornadaDao(): JornadaDao
    abstract fun cacheTimestampDao(): CacheTimestampDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Builds (or returns the cached) desktop Room database.
         * [appDataDir] is the per-user app data directory (e.g. ~/.padelaragon).
         */
        fun getInstance(appDataDir: File): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    appDataDir.mkdirs()
                    val dbFile = File(appDataDir, "padel_aragon.db")
                    Room.databaseBuilder<AppDatabase>(dbFile.absolutePath)
                        .setDriver(BundledSQLiteDriver())
                        .fallbackToDestructiveMigration(true)
                        .build()
                        .also { INSTANCE = it }
                }
            }
        }
    }
}
