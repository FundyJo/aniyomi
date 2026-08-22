package eu.kanade.aniyomi.desktop

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.nio.file.Files
import java.nio.file.Path

class DesktopBackupService(
    private val repository: DesktopLibraryRepository,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exportTo(path: Path): DesktopBackupSummary = withContext(Dispatchers.IO) {
        val snapshot = repository.createBackupSnapshot()
        path.parent?.let(Files::createDirectories)
        Files.writeString(path, snapshot.toJson().toString())
        snapshot.summary(path)
    }

    suspend fun importFrom(path: Path): DesktopBackupSummary = withContext(Dispatchers.IO) {
        val snapshot = Files.readString(path).toBackupSnapshot()
        repository.restoreBackupSnapshot(snapshot)
        snapshot.summary(path)
    }

    suspend fun preview(path: Path): DesktopBackupSummary = withContext(Dispatchers.IO) {
        Files.readString(path).toBackupSnapshot().summary(path)
    }

    private fun DesktopBackupSnapshot.summary(path: Path): DesktopBackupSummary {
        return DesktopBackupSummary(
            path = path,
            mangaEntries = manga.size,
            chapters = manga.sumOf { it.chapters.size },
            animeEntries = anime.size,
            episodes = anime.sumOf { it.episodes.size },
        )
    }

    private fun String.toBackupSnapshot(): DesktopBackupSnapshot {
        val root = json.parseToJsonElement(this).jsonObject
        check(root["format"]?.jsonPrimitive?.content == BACKUP_FORMAT) { "Unsupported backup format" }
        val manga = root["manga"]?.jsonArray.orEmpty().map { it.jsonObject.toMangaBackup() }
        val anime = root["anime"]?.jsonArray.orEmpty().map { it.jsonObject.toAnimeBackup() }
        return DesktopBackupSnapshot(
            version = root["version"]?.jsonPrimitive?.longOrNull?.toInt() ?: 1,
            createdAt = root["createdAt"]?.jsonPrimitive?.longOrNull ?: 0L,
            manga = manga,
            anime = anime,
        )
    }
}

data class DesktopBackupSummary(
    val path: Path,
    val mangaEntries: Int,
    val chapters: Int,
    val animeEntries: Int,
    val episodes: Int,
)

data class DesktopBackupSnapshot(
    val version: Int,
    val createdAt: Long,
    val manga: List<DesktopMangaBackup>,
    val anime: List<DesktopAnimeBackup>,
)

data class DesktopMangaBackup(
    val sourceId: Long,
    val url: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val status: Int,
    val chapters: List<DesktopChapterBackup>,
)

data class DesktopChapterBackup(
    val url: String,
    val name: String,
    val number: Double,
    val dateUpload: Long,
    val lastPageRead: Long,
    val read: Boolean,
    val bookmark: Boolean,
)

data class DesktopAnimeBackup(
    val sourceId: Long,
    val url: String,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val status: Int,
    val episodes: List<DesktopEpisodeBackup>,
)

data class DesktopEpisodeBackup(
    val url: String,
    val name: String,
    val number: Double,
    val dateUpload: Long,
    val lastSecondSeen: Long,
    val totalSeconds: Long,
    val seen: Boolean,
    val bookmark: Boolean,
)

fun DesktopBackupSnapshot.toJson(): JsonObject {
    return buildJsonObject {
        put("format", JsonPrimitive(BACKUP_FORMAT))
        put("version", JsonPrimitive(version))
        put("createdAt", JsonPrimitive(createdAt))
        put("manga", buildJsonArray { manga.forEach { add(it.toJson()) } })
        put("anime", buildJsonArray { anime.forEach { add(it.toJson()) } })
    }
}

private fun DesktopMangaBackup.toJson(): JsonObject = buildJsonObject {
    put("sourceId", JsonPrimitive(sourceId))
    put("url", JsonPrimitive(url))
    put("title", JsonPrimitive(title))
    putNullable("description", description)
    putNullable("thumbnailUrl", thumbnailUrl)
    put("status", JsonPrimitive(status))
    put("chapters", buildJsonArray { chapters.forEach { add(it.toJson()) } })
}

private fun DesktopChapterBackup.toJson(): JsonObject = buildJsonObject {
    put("url", JsonPrimitive(url))
    put("name", JsonPrimitive(name))
    put("number", JsonPrimitive(number))
    put("dateUpload", JsonPrimitive(dateUpload))
    put("lastPageRead", JsonPrimitive(lastPageRead))
    put("read", JsonPrimitive(read))
    put("bookmark", JsonPrimitive(bookmark))
}

private fun DesktopAnimeBackup.toJson(): JsonObject = buildJsonObject {
    put("sourceId", JsonPrimitive(sourceId))
    put("url", JsonPrimitive(url))
    put("title", JsonPrimitive(title))
    putNullable("description", description)
    putNullable("thumbnailUrl", thumbnailUrl)
    put("status", JsonPrimitive(status))
    put("episodes", buildJsonArray { episodes.forEach { add(it.toJson()) } })
}

private fun DesktopEpisodeBackup.toJson(): JsonObject = buildJsonObject {
    put("url", JsonPrimitive(url))
    put("name", JsonPrimitive(name))
    put("number", JsonPrimitive(number))
    put("dateUpload", JsonPrimitive(dateUpload))
    put("lastSecondSeen", JsonPrimitive(lastSecondSeen))
    put("totalSeconds", JsonPrimitive(totalSeconds))
    put("seen", JsonPrimitive(seen))
    put("bookmark", JsonPrimitive(bookmark))
}

private fun JsonObject.toMangaBackup(): DesktopMangaBackup = DesktopMangaBackup(
    sourceId = requiredLong("sourceId"),
    url = requiredString("url"),
    title = requiredString("title"),
    description = optionalString("description"),
    thumbnailUrl = optionalString("thumbnailUrl"),
    status = requiredLong("status").toInt(),
    chapters = this["chapters"]?.jsonArray.orEmpty().map { it.jsonObject.toChapterBackup() },
)

private fun JsonObject.toChapterBackup(): DesktopChapterBackup = DesktopChapterBackup(
    url = requiredString("url"),
    name = requiredString("name"),
    number = requiredDouble("number"),
    dateUpload = requiredLong("dateUpload"),
    lastPageRead = requiredLong("lastPageRead"),
    read = requiredBoolean("read"),
    bookmark = requiredBoolean("bookmark"),
)

private fun JsonObject.toAnimeBackup(): DesktopAnimeBackup = DesktopAnimeBackup(
    sourceId = requiredLong("sourceId"),
    url = requiredString("url"),
    title = requiredString("title"),
    description = optionalString("description"),
    thumbnailUrl = optionalString("thumbnailUrl"),
    status = requiredLong("status").toInt(),
    episodes = this["episodes"]?.jsonArray.orEmpty().map { it.jsonObject.toEpisodeBackup() },
)

private fun JsonObject.toEpisodeBackup(): DesktopEpisodeBackup = DesktopEpisodeBackup(
    url = requiredString("url"),
    name = requiredString("name"),
    number = requiredDouble("number"),
    dateUpload = requiredLong("dateUpload"),
    lastSecondSeen = requiredLong("lastSecondSeen"),
    totalSeconds = requiredLong("totalSeconds"),
    seen = requiredBoolean("seen"),
    bookmark = requiredBoolean("bookmark"),
)

private fun JsonObject.requiredString(name: String): String = this[name]?.jsonPrimitive?.content ?: error("Missing $name")
private fun JsonObject.optionalString(name: String): String? = this[name]?.takeUnless { it is JsonNull }?.jsonPrimitive?.content
private fun JsonObject.requiredLong(name: String): Long = this[name]?.jsonPrimitive?.longOrNull ?: error("Missing $name")
private fun JsonObject.requiredDouble(name: String): Double = this[name]?.jsonPrimitive?.content?.toDoubleOrNull() ?: error("Missing $name")
private fun JsonObject.requiredBoolean(name: String): Boolean = this[name]?.jsonPrimitive?.booleanOrNull ?: error("Missing $name")
private fun JsonObjectBuilder.putNullable(name: String, value: String?) = put(name, value?.let(::JsonPrimitive) ?: JsonNull)

private const val BACKUP_FORMAT = "aniyomi-desktop-backup"
