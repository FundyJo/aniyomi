package tachiyomi.data.database

import app.cash.sqldelight.db.SqlDriver

interface DatabaseDriverFactory {
    fun createMangaDriver(): SqlDriver

    fun createAnimeDriver(): SqlDriver
}
