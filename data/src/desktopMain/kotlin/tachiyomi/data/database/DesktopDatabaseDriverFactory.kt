package tachiyomi.data.database

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import tachiyomi.data.Database
import tachiyomi.mi.data.AnimeDatabase
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString

class DesktopDatabaseDriverFactory(
    private val databaseDirectory: Path = defaultApplicationDataDirectory(),
) : DatabaseDriverFactory {

    override fun createMangaDriver(): SqlDriver {
        return createDriver(MANGA_DATABASE_NAME, Database.Schema)
    }

    override fun createAnimeDriver(): SqlDriver {
        return createDriver(ANIME_DATABASE_NAME, AnimeDatabase.Schema)
    }

    private fun createDriver(name: String, schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver {
        Files.createDirectories(databaseDirectory)
        return JdbcSqliteDriver(
            url = "jdbc:sqlite:${databaseDirectory.resolve(name).absolutePathString()}",
            schema = schema,
        )
    }
}

private fun defaultApplicationDataDirectory(): Path {
    val osName = System.getProperty("os.name").lowercase()
    val userHome = System.getProperty("user.home")
    val baseDirectory = when {
        osName.contains("win") -> System.getenv("APPDATA")?.let(Path::of) ?: Path.of(userHome, "AppData", "Roaming")
        osName.contains("mac") -> Path.of(userHome, "Library", "Application Support")
        else -> System.getenv("XDG_DATA_HOME")?.let(Path::of) ?: Path.of(userHome, ".local", "share")
    }
    return baseDirectory.resolve("Aniyomi")
}
