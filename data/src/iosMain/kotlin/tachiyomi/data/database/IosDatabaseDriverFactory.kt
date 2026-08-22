package tachiyomi.data.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import tachiyomi.data.Database
import tachiyomi.mi.data.AnimeDatabase

class IosDatabaseDriverFactory : DatabaseDriverFactory {

    override fun createMangaDriver(): SqlDriver {
        return NativeSqliteDriver(Database.Schema, MANGA_DATABASE_NAME)
    }

    override fun createAnimeDriver(): SqlDriver {
        return NativeSqliteDriver(AnimeDatabase.Schema, ANIME_DATABASE_NAME)
    }
}
