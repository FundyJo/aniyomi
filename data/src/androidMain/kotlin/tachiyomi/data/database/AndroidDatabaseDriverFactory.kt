package tachiyomi.data.database

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import tachiyomi.data.Database
import tachiyomi.mi.data.AnimeDatabase

class AndroidDatabaseDriverFactory(
    private val context: Context,
    private val openHelperFactory: SupportSQLiteOpenHelper.Factory,
) : DatabaseDriverFactory {

    override fun createMangaDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = Database.Schema,
            context = context,
            name = MANGA_DATABASE_NAME,
            factory = openHelperFactory,
            callback = object : AndroidSqliteDriver.Callback(Database.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    setPragmas(db)
                }
            },
        )
    }

    override fun createAnimeDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = AnimeDatabase.Schema,
            context = context,
            name = ANIME_DATABASE_NAME,
            factory = openHelperFactory,
            callback = object : AndroidSqliteDriver.Callback(AnimeDatabase.Schema) {
                override fun onOpen(db: SupportSQLiteDatabase) {
                    super.onOpen(db)
                    setPragmas(db)
                }
            },
        )
    }

    private fun setPragmas(db: SupportSQLiteDatabase) {
        setPragma(db, "foreign_keys = ON")
        setPragma(db, "journal_mode = WAL")
        setPragma(db, "synchronous = NORMAL")
    }

    private fun setPragma(db: SupportSQLiteDatabase, pragma: String) {
        val cursor = db.query("PRAGMA $pragma")
        cursor.moveToFirst()
        cursor.close()
    }
}
