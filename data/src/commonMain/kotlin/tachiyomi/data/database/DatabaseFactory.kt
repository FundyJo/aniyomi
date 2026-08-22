package tachiyomi.data.database

import app.cash.sqldelight.db.SqlDriver
import data.Mangas
import dataanime.Animes
import dataanime.Episodes
import tachiyomi.data.AnimeUpdateStrategyColumnAdapter
import tachiyomi.data.Database
import tachiyomi.data.FetchTypeColumnAdapter
import tachiyomi.data.MangaUpdateStrategyColumnAdapter
import tachiyomi.data.MemoColumnAdapter
import tachiyomi.data.StringListColumnAdapter
import tachiyomi.mi.data.AnimeDatabase

fun createMangaDatabase(driver: SqlDriver): Database {
    return Database(
        driver = driver,
        mangasAdapter = Mangas.Adapter(
            genreAdapter = StringListColumnAdapter,
            update_strategyAdapter = MangaUpdateStrategyColumnAdapter,
        ),
    )
}

fun createAnimeDatabase(driver: SqlDriver): AnimeDatabase {
    return AnimeDatabase(
        driver = driver,
        animesAdapter = Animes.Adapter(
            genreAdapter = StringListColumnAdapter,
            update_strategyAdapter = AnimeUpdateStrategyColumnAdapter,
            fetch_typeAdapter = FetchTypeColumnAdapter,
            memoAdapter = MemoColumnAdapter,
        ),
        episodesAdapter = Episodes.Adapter(
            memoAdapter = MemoColumnAdapter,
        ),
    )
}
