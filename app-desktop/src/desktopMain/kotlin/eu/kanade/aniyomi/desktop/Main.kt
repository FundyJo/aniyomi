package eu.kanade.aniyomi.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import eu.kanade.tachiyomi.source.GlobalSearchQuery
import eu.kanade.tachiyomi.source.MultiplatformAnimeSource
import eu.kanade.tachiyomi.source.MultiplatformMangaSource
import eu.kanade.tachiyomi.source.MultiplatformSource
import eu.kanade.tachiyomi.source.SearchState
import eu.kanade.tachiyomi.source.SourceCapability
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourceSearchResult
import kotlinx.coroutines.launch
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import java.text.DateFormat
import java.util.Date

fun main() = application {
    val dependencies = remember { DesktopDependencyContainer() }
    val windowState = remember {
        WindowState(
            placement = WindowPlacement.Floating,
            position = WindowPosition.Aligned(Alignment.Center),
            size = DpSize(1280.dp, 800.dp),
        )
    }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Aniyomi",
        icon = painterResource("aniyomi.png"),
    ) {
        window.minimumSize = java.awt.Dimension(1024, 640)
        DesktopApplication(dependencies)
    }
}

private enum class DesktopDestination(val label: String) {
    Library("Library"),
    Updates("Updates"),
    History("History"),
    Browse("Browse"),
    Downloads("Downloads"),
    Settings("Settings"),
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopApplication(dependencies: DesktopDependencyContainer) {
    var destination by remember { mutableStateOf(DesktopDestination.Library) }
    val searchFocusRequester = remember { FocusRequester() }

    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.isCtrlPressed && event.key == Key.F -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.L -> {
                        destination = DesktopDestination.Library
                        true
                    }
                    event.isCtrlPressed && event.key == Key.D -> {
                        destination = DesktopDestination.Downloads
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Comma -> {
                        destination = DesktopDestination.Settings
                        true
                    }
                    event.isCtrlPressed && event.key == Key.R -> true
                    event.key == Key.Escape -> true
                    else -> false
                }
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                NavigationSidebar(destination, onDestinationSelected = { destination = it })
                Divider(Modifier.fillMaxHeight().width(1.dp))
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (destination) {
                        DesktopDestination.Library -> LibraryScreen(dependencies.libraryRepository, searchFocusRequester)
                        DesktopDestination.Updates -> UpdatesScreen(dependencies.libraryRepository)
                        DesktopDestination.History -> HistoryScreen(dependencies.libraryRepository, searchFocusRequester)
                        DesktopDestination.Browse -> BrowseScreen(dependencies)
                        DesktopDestination.Downloads -> DownloadsScreen(dependencies.directories.downloads.toString())
                        DesktopDestination.Settings -> SettingsScreen(dependencies)
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationSidebar(
    selected: DesktopDestination,
    onDestinationSelected: (DesktopDestination) -> Unit,
) {
    Column(
        modifier = Modifier.width(184.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Image(painterResource("aniyomi.png"), contentDescription = null, modifier = Modifier.size(40.dp).clip(CircleShape))
            Text("Aniyomi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))
        DesktopDestination.entries.forEach { destination ->
            val selectedBackground = if (destination == selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            val selectedContent = if (destination == selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            Text(
                text = destination.label,
                color = selectedContent,
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedBackground)
                    .clickable { onDestinationSelected(destination) }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}

@Composable
private fun LibraryScreen(
    repository: DesktopLibraryRepository,
    searchFocusRequester: FocusRequester,
) {
    var query by remember { mutableStateOf("") }
    var anime by remember { mutableStateOf<List<LibraryAnime>>(emptyList()) }
    var manga by remember { mutableStateOf<List<LibraryManga>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching {
            anime = repository.getAnimeLibrary()
            manga = repository.getMangaLibrary()
        }.onFailure {
            error = it.message ?: it::class.simpleName
        }
        loading = false
    }

    ScreenScaffold("Library") {
        SearchField("Search library", query, { query = it }, searchFocusRequester)
        Spacer(Modifier.height(16.dp))
        if (loading) {
            LoadingState("Opening tachiyomi.db and tachiyomi.animedb")
        } else if (error != null) {
            ErrorState("Library database error", error.orEmpty(), retryLabel = null, onRetry = {})
        } else {
            val filteredAnime = anime.filter { it.anime.title.contains(query, ignoreCase = true) }
            val filteredManga = manga.filter { it.manga.title.contains(query, ignoreCase = true) }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
                LibraryColumn(
                    title = "Anime Library",
                    emptyText = "No anime favorites in the database yet.",
                    modifier = Modifier.weight(1f),
                    items = filteredAnime.map {
                        LibraryRow(
                            title = it.anime.title,
                            subtitle = "${it.unseenCount} unwatched • ${it.totalCount} episodes • Source ${it.anime.source}",
                            tags = listOf("Category ${it.category}", if (it.hasBookmarks) "Bookmarked" else "No bookmarks"),
                        )
                    },
                )
                LibraryColumn(
                    title = "Manga Library",
                    emptyText = "No manga favorites in the database yet.",
                    modifier = Modifier.weight(1f),
                    items = filteredManga.map {
                        LibraryRow(
                            title = it.manga.title,
                            subtitle = "${it.unreadCount} unread • ${it.totalChapters} chapters • Source ${it.manga.source}",
                            tags = listOf("Category ${it.category}", if (it.hasBookmarks) "Bookmarked" else "No bookmarks"),
                        )
                    },
                )
            }
        }
    }
}

private data class LibraryRow(
    val title: String,
    val subtitle: String,
    val tags: List<String>,
)

@Composable
private fun LibraryColumn(
    title: String,
    emptyText: String,
    items: List<LibraryRow>,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (items.isEmpty()) {
            EmptyState(emptyText)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) {
                items(items) { item ->
                    InfoCard(title = item.title, subtitle = item.subtitle, tags = item.tags)
                }
            }
        }
    }
}

@Composable
private fun BrowseScreen(dependencies: DesktopDependencyContainer) {
    val sources by dependencies.sourceRegistry.sources().collectAsState(emptyList())
    val extensions by dependencies.extensionManager.extensions.collectAsState(emptyList())
    var selectedSource by remember { mutableStateOf<MultiplatformSource?>(null) }
    val animeSources = sources.filterIsInstance<MultiplatformAnimeSource>()
    val mangaSources = sources.filterIsInstance<MultiplatformMangaSource>()

    LaunchedEffect(Unit) {
        dependencies.extensionManager.reload()
    }

    ScreenScaffold("Browse") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.weight(0.9f).fillMaxHeight()) {
                ExtensionStatusPanel(dependencies, extensions, onReload = { dependencies.extensionManager.reload() })
                Spacer(Modifier.height(16.dp))
                SourceSection("Anime Sources", animeSources, onClick = { selectedSource = it })
                Spacer(Modifier.height(16.dp))
                SourceSection("Manga Sources", mangaSources, onClick = { selectedSource = it })
            }
            Column(Modifier.weight(1.1f).fillMaxHeight()) {
                SelectedSourcePanel(selectedSource)
                Spacer(Modifier.height(16.dp))
                GlobalSearchPanel(dependencies)
            }
        }
    }
}

@Composable
private fun ExtensionStatusPanel(
    dependencies: DesktopDependencyContainer,
    extensions: List<DesktopExtension>,
    onReload: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    Text("Desktop Extensions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Text(dependencies.directories.extensions.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Button(onClick = { scope.launch { onReload() } }) { Text("Reload extensions") }
    Spacer(Modifier.height(8.dp))
    if (extensions.isEmpty()) {
        EmptyState("Place JVM source extension JARs in the extensions folder.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(160.dp)) {
            items(extensions) { extension ->
                val status = when (extension.status) {
                    DesktopExtensionStatus.Loaded -> "Loaded ${extension.sources.size} source(s)"
                    DesktopExtensionStatus.Invalid -> "Invalid"
                    DesktopExtensionStatus.Failed -> "Failed: ${extension.error ?: "Unknown error"}"
                }
                InfoCard(
                    title = extension.name,
                    subtitle = "${extension.language.uppercase()} • ${extension.version} • $status",
                    tags = listOfNotNull(extension.type?.name, extension.id),
                )
            }
        }
    }
}

@Composable
private fun SourceSection(
    title: String,
    sources: List<MultiplatformSource>,
    onClick: (MultiplatformSource) -> Unit,
) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    if (sources.isEmpty()) {
        EmptyState("No bundled multiplatform ${title.lowercase()} are registered yet.")
    } else {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(240.dp)) {
            items(sources) { source ->
                SourceCard(source, onClick)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SourceCard(source: MultiplatformSource, onClick: (MultiplatformSource) -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    InfoCard(
        title = source.name,
        subtitle = "${source.lang.uppercase()} • ${source.id} • ${sourceTypeLabel(source)}",
        tags = source.capabilities.map { it.name }.sorted(),
        modifier = Modifier.hoverable(interactionSource).combinedClickable(onClick = { onClick(source) }).focusable(),
    )
}

@Composable
private fun SelectedSourcePanel(source: MultiplatformSource?) {
    var query by remember(source) { mutableStateOf("") }
    var loading by remember(source) { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var results by remember(source) { mutableStateOf<List<SourceMedia>>(emptyList()) }
    var hasNextPage by remember(source) { mutableStateOf(false) }
    var selectedMedia by remember(source) { mutableStateOf<SourceMedia?>(null) }
    var details by remember(source) { mutableStateOf<SourceMedia?>(null) }
    var items by remember(source) { mutableStateOf<List<SourceEpisode>>(emptyList()) }
    val scope = rememberCoroutineScope()

    fun loadResults(mode: String) {
        val currentSource = source ?: return
        loading = true
        error = null
        selectedMedia = null
        details = null
        items = emptyList()
        scope.launch {
            runCatching {
                when (currentSource) {
                    is MultiplatformAnimeSource -> when (mode) {
                        "latest" -> currentSource.getLatestAnime(1)
                        "search" -> currentSource.searchAnime(1, query.trim(), currentSource.getAnimeFilters())
                        else -> currentSource.getPopularAnime(1)
                    }
                    is MultiplatformMangaSource -> when (mode) {
                        "latest" -> currentSource.getLatestManga(1)
                        "search" -> currentSource.searchManga(1, query.trim(), currentSource.getMangaFilters())
                        else -> currentSource.getPopularManga(1)
                    }
                    else -> throw UnsupportedOperationException("Unsupported source type")
                }
            }.onSuccess { page ->
                results = page.entries
                hasNextPage = page.hasNextPage
            }.onFailure { throwable ->
                error = throwable.message ?: throwable::class.simpleName
            }
            loading = false
        }
    }

    fun loadDetails(media: SourceMedia) {
        val currentSource = source ?: return
        selectedMedia = media
        details = null
        items = emptyList()
        error = null
        scope.launch {
            runCatching {
                when (currentSource) {
                    is MultiplatformAnimeSource -> currentSource.getAnimeDetails(media) to currentSource.getEpisodeList(media)
                    is MultiplatformMangaSource -> currentSource.getMangaDetails(media) to currentSource.getChapterList(media)
                    else -> throw UnsupportedOperationException("Unsupported source type")
                }
            }.onSuccess { (mediaDetails, sourceItems) ->
                details = mediaDetails
                items = sourceItems
            }.onFailure { throwable ->
                error = throwable.message ?: throwable::class.simpleName
            }
        }
    }

    Text("Source", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    if (source == null) {
        EmptyState("Select an anime or manga source to load real popular, latest, search, details, and episodes/chapters.")
        return
    }

    Text("${source.name} • ${source.lang.uppercase()} • ${sourceTypeLabel(source)}")
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = { loadResults("popular") }, enabled = SourceCapability.Popular in source.capabilities) { Text("Popular") }
        Button(onClick = { loadResults("latest") }, enabled = SourceCapability.Latest in source.capabilities) { Text("Latest") }
    }
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search ${source.name}") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { loadResults("search") }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { loadResults("search") }, enabled = SourceCapability.Search in source.capabilities) { Text("Search") }
    }
    Spacer(Modifier.height(12.dp))
    when {
        loading -> LoadingState("Loading ${source.name}")
        error != null -> ErrorState("Source error", error.orEmpty(), retryLabel = null, onRetry = {})
        results.isEmpty() -> EmptyState("No source results loaded yet.")
        else -> SourceResultAndDetails(results, hasNextPage, selectedMedia, details, items, source, onMediaClick = ::loadDetails)
    }
}

@Composable
private fun SourceResultAndDetails(
    results: List<SourceMedia>,
    hasNextPage: Boolean,
    selectedMedia: SourceMedia?,
    details: SourceMedia?,
    items: List<SourceEpisode>,
    source: MultiplatformSource,
    onMediaClick: (SourceMedia) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.height(280.dp)) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f).fillMaxHeight()) {
            items(results) { media ->
                SourceMediaRow(media, onClick = { onMediaClick(media) })
            }
            if (hasNextPage) item { Text("More results available", color = MaterialTheme.colorScheme.primary) }
        }
        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            val currentDetails = details ?: selectedMedia
            if (currentDetails == null) {
                EmptyState("Select a result to load details from the source.")
            } else {
                Text(currentDetails.title, fontWeight = FontWeight.Bold)
                Text(currentDetails.status.name, style = MaterialTheme.typography.bodySmall)
                currentDetails.description?.takeIf { it.isNotBlank() }?.let { description ->
                    Spacer(Modifier.height(8.dp))
                    Text(description)
                }
                Spacer(Modifier.height(12.dp))
                Text(if (source is MultiplatformAnimeSource) "Episodes" else "Chapters", fontWeight = FontWeight.Bold)
                if (items.isEmpty()) {
                    EmptyState("No ${if (source is MultiplatformAnimeSource) "episodes" else "chapters"} returned yet.")
                } else {
                    items.forEach { item ->
                        Text(item.name, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalSearchPanel(
    dependencies: DesktopDependencyContainer,
) {
    var query by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<SearchState>(SearchState.Complete(emptyList())) }
    val scope = rememberCoroutineScope()

    fun runSearch() {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        scope.launch {
            dependencies.globalSourceSearch.states(GlobalSearchQuery(trimmed)).collect { state = it }
        }
    }

    Text("Global Search", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Search sources") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { runSearch() }),
            modifier = Modifier.weight(1f),
        )
        Button(onClick = { runSearch() }) { Text("Search") }
    }
    Spacer(Modifier.height(12.dp))
    when (val current = state) {
        SearchState.Loading -> LoadingState("Searching registered sources")
        is SearchState.Partial -> SearchResults(current.results, isPartial = true, onRetry = { runSearch() })
        is SearchState.Complete -> SearchResults(current.results, isPartial = false, onRetry = { runSearch() })
    }
}

@Composable
private fun SearchResults(
    results: List<SourceSearchResult>,
    isPartial: Boolean,
    onRetry: () -> Unit,
) {
    if (results.isEmpty()) {
        EmptyState(if (isPartial) "Waiting for source responses…" else "No source results.")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
        if (isPartial) Text("Partial results", color = MaterialTheme.colorScheme.primary)
        results.forEach { result ->
            val page = result.page
            val error = result.error
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SourceAvatar(result.source)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(result.source.name, fontWeight = FontWeight.Bold)
                            Text(result.source.lang.uppercase(), style = MaterialTheme.typography.bodySmall)
                        }
                        if (error != null) Button(onClick = onRetry) { Text("Retry") }
                    }
                    Spacer(Modifier.height(8.dp))
                    when {
                        error != null -> Text("${error.type}: ${error.message ?: "Source failed"}", color = MaterialTheme.colorScheme.error)
                        page == null || page.entries.isEmpty() -> Text("No matches")
                        else -> page.entries.take(8).forEach { media -> SourceMediaRow(media) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceMediaRow(media: SourceMedia, onClick: (() -> Unit)? = null) {
    val modifier = if (onClick == null) {
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
    } else {
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(8.dp)
    }
    Column(modifier) {
        Text(media.title, fontWeight = FontWeight.Medium)
        Text(media.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun sourceTypeLabel(source: MultiplatformSource): String = when (source) {
    is MultiplatformAnimeSource -> "Anime"
    is MultiplatformMangaSource -> "Manga"
    else -> "Source"
}

@Composable
private fun HistoryScreen(
    repository: DesktopLibraryRepository,
    searchFocusRequester: FocusRequester,
) {
    var query by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<DesktopHistoryItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        loading = true
        runCatching { items = repository.getHistory(query) }.onFailure { error = it.message ?: it::class.simpleName }
        loading = false
    }

    ScreenScaffold("History") {
        SearchField("Search history", query, { query = it }, searchFocusRequester)
        Spacer(Modifier.height(16.dp))
        when {
            loading -> LoadingState("Loading continue watching and reading history")
            error != null -> ErrorState("History database error", error.orEmpty(), retryLabel = null, onRetry = {})
            items.isEmpty() -> EmptyState("No anime or manga history in the database yet.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    InfoCard(
                        title = item.title,
                        subtitle = "${item.type} item ${item.itemNumber} • Source ${item.source}",
                        tags = listOf(formatDate(item.timestamp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdatesScreen(repository: DesktopLibraryRepository) {
    var items by remember { mutableStateOf<List<DesktopUpdateItem>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { items = repository.getUpdates() }.onFailure { error = it.message ?: it::class.simpleName }
        loading = false
    }

    ScreenScaffold("Updates") {
        when {
            loading -> LoadingState("Loading recent anime and manga updates")
            error != null -> ErrorState("Updates database error", error.orEmpty(), retryLabel = null, onRetry = {})
            items.isEmpty() -> EmptyState("No recent anime or manga updates in the database yet.")
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items) { item ->
                    InfoCard(
                        title = item.title,
                        subtitle = "${item.itemName} • ${item.type} • Source ${item.source}",
                        tags = listOf(formatDate(item.dateUpload), if (item.downloaded) "Downloaded" else "Not downloaded"),
                    )
                }
            }
        }
    }
}

@Composable
private fun DownloadsScreen(downloadPath: String) {
    ScreenScaffold("Downloads") {
        Text("Desktop download worker is not started in this slice.")
        Spacer(Modifier.height(8.dp))
        Text("Default download directory: $downloadPath")
    }
}

@Composable
private fun SettingsScreen(dependencies: DesktopDependencyContainer) {
    ScreenScaffold("Settings") {
        Text("Settings groups backed by desktop services and existing preferences will be enabled incrementally.")
        Spacer(Modifier.height(16.dp))
        listOf(
            "General",
            "Appearance",
            "Library",
            "Reader",
            "Player",
            "Downloads",
            "Network",
            "Sources",
            "Tracking",
            "Advanced",
            "About",
        ).forEach { group ->
            InfoCard(title = group, subtitle = settingSubtitle(group, dependencies), tags = emptyList())
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun settingSubtitle(group: String, dependencies: DesktopDependencyContainer): String = when (group) {
    "Downloads" -> dependencies.directories.downloads.toString()
    "Network" -> "KtorNetworkClient with CIO engine"
    "Sources" -> "BuiltinSourceRegistry (${dependencies.sourceRegistry.get(-1)?.name ?: "0 bundled sources"})"
    "About" -> "${dependencies.platformInfo.name} ${dependencies.platformInfo.version} (${dependencies.platformInfo.architecture})"
    "Tracking" -> "Desktop secure storage refuses plaintext token persistence until DPAPI/Credential Manager is integrated"
    else -> "No desktop-specific preference bound yet"
}

@Composable
private fun ScreenScaffold(title: String, content: @Composable Column.() -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(20.dp))
        content()
    }
}

@Composable
private fun SearchField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

@Composable
private fun LoadingState(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CircularProgressIndicator(Modifier.size(24.dp))
        Text(message)
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(
        modifier = Modifier.fillMaxWidth().border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)).padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ErrorState(title: String, message: String, retryLabel: String?, onRetry: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
            if (retryLabel != null) Button(onClick = onRetry) { Text(retryLabel) }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    subtitle: String,
    tags: List<String>,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SourceAvatar(title)
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(tags) { tag -> CapabilityChip(tag) }
                }
            }
        }
    }
}

@Composable
private fun SourceAvatar(source: MultiplatformSource) {
    SourceAvatar(source.name)
}

@Composable
private fun SourceAvatar(seed: String) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(seed.firstOrNull()?.uppercase() ?: "?", color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun CapabilityChip(text: String) {
    Text(
        text = text,
        modifier = Modifier.clip(RoundedCornerShape(999.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
    )
}

private fun formatDate(epochMillis: Long): String {
    if (epochMillis <= 0) return "Unknown date"
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(epochMillis))
}
