package eu.kanade.aniyomi.desktop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
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
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import eu.kanade.tachiyomi.source.preference.DesktopSourcePreferenceStores
import eu.kanade.tachiyomi.source.GlobalSearchQuery
import eu.kanade.tachiyomi.source.MultiplatformAnimeSource
import eu.kanade.tachiyomi.source.MultiplatformMangaSource
import eu.kanade.tachiyomi.source.MultiplatformSource
import eu.kanade.tachiyomi.source.SearchState
import eu.kanade.tachiyomi.source.SourceCapability
import eu.kanade.tachiyomi.source.SourceEpisode
import eu.kanade.tachiyomi.source.SourceMedia
import eu.kanade.tachiyomi.source.SourcePageImage
import eu.kanade.tachiyomi.source.SourceSearchResult
import eu.kanade.tachiyomi.source.VideoSource
import eu.kanade.tachiyomi.source.network.HttpHeaders
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import tachiyomi.domain.library.anime.LibraryAnime
import tachiyomi.domain.library.manga.LibraryManga
import java.awt.Canvas
import java.text.DateFormat
import java.util.Date
import kotlin.io.path.toUri

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
    Extensions("Extensions"),
}

private sealed interface DesktopRoute {
    val destination: DesktopDestination?

    data object Library : DesktopRoute { override val destination = DesktopDestination.Library }
    data object Updates : DesktopRoute { override val destination = DesktopDestination.Updates }
    data object History : DesktopRoute { override val destination = DesktopDestination.History }
    data object Browse : DesktopRoute { override val destination = DesktopDestination.Browse }
    data object Downloads : DesktopRoute { override val destination = DesktopDestination.Downloads }
    data object Settings : DesktopRoute { override val destination = DesktopDestination.Settings }
    data object Extensions : DesktopRoute { override val destination = DesktopDestination.Extensions }
    data class Source(val sourceId: Long) : DesktopRoute { override val destination = DesktopDestination.Browse }
    data class AnimeDetails(val sourceId: Long, val anime: SourceMedia) : DesktopRoute { override val destination = DesktopDestination.Browse }
    data class MangaDetails(val sourceId: Long, val manga: SourceMedia) : DesktopRoute { override val destination = DesktopDestination.Browse }
    data class Reader(val sourceId: Long, val manga: SourceMedia, val chapter: SourceEpisode) : DesktopRoute { override val destination: DesktopDestination? = null }
    data class Player(val sourceId: Long, val anime: SourceMedia, val episode: SourceEpisode) : DesktopRoute { override val destination: DesktopDestination? = null }
}

private class DesktopNavigator(start: DesktopRoute = DesktopRoute.Library) {
    private val stack = mutableStateListOf(start)
    val current: DesktopRoute get() = stack.last()
    val canGoBack: Boolean get() = stack.size > 1

    fun push(route: DesktopRoute) {
        stack += route
    }

    fun replace(route: DesktopRoute) {
        stack[stack.lastIndex] = route
    }

    fun pop(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }
}

private fun DesktopDestination.toRoute(): DesktopRoute = when (this) {
    DesktopDestination.Library -> DesktopRoute.Library
    DesktopDestination.Updates -> DesktopRoute.Updates
    DesktopDestination.History -> DesktopRoute.History
    DesktopDestination.Browse -> DesktopRoute.Browse
    DesktopDestination.Downloads -> DesktopRoute.Downloads
    DesktopDestination.Settings -> DesktopRoute.Settings
    DesktopDestination.Extensions -> DesktopRoute.Extensions
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun DesktopApplication(dependencies: DesktopDependencyContainer) {
    val navigator = remember { DesktopNavigator() }
    val searchFocusRequester = remember { FocusRequester() }
    var fullscreen by remember { mutableStateOf(false) }
    val themeMode by dependencies.preferences.themeMode.collectAsState()
    val systemDark = isSystemInDarkTheme()
    val route = navigator.current
    val darkTheme = when (themeMode) {
        DesktopThemeMode.System -> systemDark
        DesktopThemeMode.Light -> false
        DesktopThemeMode.Dark -> true
    }

    MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxSize().onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when {
                    event.isCtrlPressed && event.key == Key.F -> {
                        searchFocusRequester.requestFocus()
                        true
                    }
                    event.isCtrlPressed && event.key == Key.L -> {
                        navigator.replace(DesktopRoute.Library)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.D -> {
                        navigator.replace(DesktopRoute.Downloads)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.Comma -> {
                        navigator.replace(DesktopRoute.Settings)
                        true
                    }
                    event.isCtrlPressed && event.key == Key.R -> true
                    event.key == Key.Escape -> {
                        if (fullscreen) fullscreen = false else navigator.pop()
                        true
                    }
                    else -> false
                }
            },
        ) {
            Row(Modifier.fillMaxSize()) {
                if (!fullscreen) {
                    NavigationSidebar(
                        selected = route.destination,
                        canGoBack = navigator.canGoBack,
                        onBack = { navigator.pop() },
                        onDestinationSelected = { navigator.replace(it.toRoute()) },
                    )
                    Divider(Modifier.fillMaxHeight().width(1.dp))
                }
                Box(Modifier.weight(1f).fillMaxHeight()) {
                    when (val currentRoute = route) {
                        DesktopRoute.Library -> LibraryScreen(dependencies.libraryRepository, searchFocusRequester)
                        DesktopRoute.Updates -> UpdatesScreen(dependencies.libraryRepository)
                        DesktopRoute.History -> HistoryScreen(dependencies.libraryRepository, searchFocusRequester)
                        DesktopRoute.Browse -> BrowseScreen(dependencies, navigator)
                        DesktopRoute.Downloads -> DownloadsScreen(dependencies)
                        DesktopRoute.Settings -> SettingsScreen(dependencies)
                        DesktopRoute.Extensions -> ExtensionsScreen(dependencies)
                        is DesktopRoute.Source -> BrowseScreen(dependencies, navigator, currentRoute.sourceId)
                        is DesktopRoute.AnimeDetails -> AnimeDetailsScreen(dependencies, currentRoute.sourceId, currentRoute.anime, onOpenEpisode = { episode -> navigator.push(DesktopRoute.Player(currentRoute.sourceId, currentRoute.anime, episode)) })
                        is DesktopRoute.MangaDetails -> MangaDetailsScreen(dependencies, currentRoute.sourceId, currentRoute.manga, onOpenChapter = { chapter -> navigator.push(DesktopRoute.Reader(currentRoute.sourceId, currentRoute.manga, chapter)) })
                        is DesktopRoute.Reader -> ReaderScreen(dependencies, currentRoute.sourceId, currentRoute.manga, currentRoute.chapter, fullscreen, onFullscreen = { fullscreen = it }, onClose = { navigator.pop() })
                        is DesktopRoute.Player -> PlayerScreen(dependencies, currentRoute.sourceId, currentRoute.anime, currentRoute.episode, fullscreen, onFullscreen = { fullscreen = it }, onClose = { navigator.pop() })
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationSidebar(
    selected: DesktopDestination?,
    canGoBack: Boolean,
    onBack: () -> Unit,
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
        Spacer(Modifier.height(16.dp))
        Button(onClick = onBack, enabled = canGoBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
        Spacer(Modifier.height(8.dp))
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
private fun BrowseScreen(
    dependencies: DesktopDependencyContainer,
    navigator: DesktopNavigator,
    initialSourceId: Long? = null,
) {
    val sources by dependencies.sourceRegistry.sources().collectAsState(emptyList())
    val extensions by dependencies.extensionManager.extensions.collectAsState(emptyList())
    var selectedSource by remember { mutableStateOf<MultiplatformSource?>(null) }
    val animeSources = sources.filterIsInstance<MultiplatformAnimeSource>()
    val mangaSources = sources.filterIsInstance<MultiplatformMangaSource>()

    LaunchedEffect(Unit) {
        dependencies.extensionManager.reload()
    }
    LaunchedEffect(initialSourceId, sources) {
        if (initialSourceId != null) selectedSource = sources.firstOrNull { it.id == initialSourceId }
    }

    ScreenScaffold("Browse") {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxSize()) {
            Column(Modifier.weight(0.9f).fillMaxHeight()) {
                ExtensionStatusPanel(dependencies, extensions, onReload = { dependencies.extensionManager.reload() })
                Spacer(Modifier.height(16.dp))
                SourceSection("Anime Sources", animeSources, onClick = { source ->
                    selectedSource = source
                    navigator.push(DesktopRoute.Source(source.id))
                })
                Spacer(Modifier.height(16.dp))
                SourceSection("Manga Sources", mangaSources, onClick = { source ->
                    selectedSource = source
                    navigator.push(DesktopRoute.Source(source.id))
                })
            }
            Column(Modifier.weight(1.1f).fillMaxHeight()) {
                SelectedSourcePanel(selectedSource, navigator)
                Spacer(Modifier.height(16.dp))
                GlobalSearchPanel(dependencies, navigator)
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
private fun SelectedSourcePanel(source: MultiplatformSource?, navigator: DesktopNavigator) {
    var query by remember(source) { mutableStateOf("") }
    var loading by remember(source) { mutableStateOf(false) }
    var error by remember(source) { mutableStateOf<String?>(null) }
    var results by remember(source) { mutableStateOf<List<SourceMedia>>(emptyList()) }
    var hasNextPage by remember(source) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun loadResults(mode: String) {
        val currentSource = source ?: return
        loading = true
        error = null
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
        else -> SourceResultAndDetails(results, hasNextPage, source) { media ->
            when (source) {
                is MultiplatformAnimeSource -> navigator.push(DesktopRoute.AnimeDetails(source.id, media))
                is MultiplatformMangaSource -> navigator.push(DesktopRoute.MangaDetails(source.id, media))
            }
        }
    }
}

@Composable
private fun SourceResultAndDetails(
    results: List<SourceMedia>,
    hasNextPage: Boolean,
    source: MultiplatformSource,
    onMediaClick: (SourceMedia) -> Unit,
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.height(280.dp)) {
        items(results) { media ->
            SourceMediaRow(media, onClick = { onMediaClick(media) })
        }
        if (hasNextPage) item { Text("More results available", color = MaterialTheme.colorScheme.primary) }
        item { Text("Click a ${sourceTypeLabel(source).lowercase()} result to open source details.", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun GlobalSearchPanel(
    dependencies: DesktopDependencyContainer,
    navigator: DesktopNavigator,
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
        is SearchState.Partial -> SearchResults(current.results, isPartial = true, onRetry = { runSearch() }, navigator = navigator)
        is SearchState.Complete -> SearchResults(current.results, isPartial = false, onRetry = { runSearch() }, navigator = navigator)
    }
}

@Composable
private fun SearchResults(
    results: List<SourceSearchResult>,
    isPartial: Boolean,
    onRetry: () -> Unit,
    navigator: DesktopNavigator,
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
                        else -> page.entries.take(8).forEach { media ->
                            SourceMediaRow(media, onClick = {
                                when (val source = result.source) {
                                    is MultiplatformAnimeSource -> navigator.push(DesktopRoute.AnimeDetails(source.id, media))
                                    is MultiplatformMangaSource -> navigator.push(DesktopRoute.MangaDetails(source.id, media))
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}



private val DesktopImageHeaders = HttpHeaders.of(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) AniyomiDesktop/1.0 Safari/537.36",
    "Accept" to "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
)

private sealed interface LoadState<out T> {
    data object Loading : LoadState<Nothing>
    data class Success<T>(val value: T) : LoadState<T>
    data class Error(val message: String) : LoadState<Nothing>
}

private data class SourceDetailsData(
    val media: SourceMedia,
    val items: List<SourceEpisode>,
)

private enum class ReaderMode {
    WEBTOON,
    VERTICAL,
    HORIZONTAL_LTR,
    HORIZONTAL_RTL,
    SINGLE_PAGE,
    DOUBLE_PAGE,
}

private enum class ReaderFitMode {
    FIT_WIDTH,
    FIT_HEIGHT,
    ORIGINAL,
}

private enum class ReaderPageState {
    QUEUED,
    LOADING,
    READY,
    ERROR,
}

private data class ReaderPage(
    val index: Int,
    val imageUrl: String,
    val headers: HttpHeaders = HttpHeaders.Empty,
    val state: ReaderPageState = ReaderPageState.QUEUED,
)

private data class ReaderState(
    val chapter: SourceEpisode,
    val pages: List<ReaderPage> = emptyList(),
    val currentPage: Int = 0,
    val mode: ReaderMode = ReaderMode.WEBTOON,
    val fitMode: ReaderFitMode = ReaderFitMode.FIT_WIDTH,
    val zoom: Float = 1f,
    val loading: Boolean = true,
    val error: String? = null,
)

private data class PlayerState(
    val videos: List<VideoSource> = emptyList(),
    val selectedVideo: VideoSource? = null,
    val error: String? = null,
    val loading: Boolean = true,
    val resumePositionMs: Long = 0,
)

@Composable
private fun MangaDetailsScreen(
    dependencies: DesktopDependencyContainer,
    sourceId: Long,
    manga: SourceMedia,
    onOpenChapter: (SourceEpisode) -> Unit,
) {
    val source = dependencies.sourceRegistry.get(sourceId) as? MultiplatformMangaSource
    var state by remember(sourceId, manga.url) { mutableStateOf<LoadState<SourceDetailsData>>(LoadState.Loading) }
    var libraryState by remember(sourceId, manga.url) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        if (source == null) {
            state = LoadState.Error("Manga source $sourceId is not registered.")
            return
        }
        state = LoadState.Loading
        scope.launch {
            runCatching {
                val details = source.getMangaDetails(manga)
                SourceDetailsData(details, source.getChapterList(details))
            }.onSuccess { state = LoadState.Success(it) }
                .onFailure { state = LoadState.Error(it.message ?: it::class.simpleName.orEmpty()) }
        }
    }

    LaunchedEffect(sourceId, manga.url) { load() }

    ScreenScaffold("Manga Details") {
        when (val current = state) {
            LoadState.Loading -> LoadingState("Loading real manga details from ${source?.name ?: sourceId}")
            is LoadState.Error -> ErrorState("Manga details failed", current.message, "Retry", onRetry = ::load)
            is LoadState.Success -> {
                val details = current.value.media
                DetailsHeader(
                    media = details,
                    sourceName = source?.name ?: sourceId.toString(),
                    metadata = listOf("Status: ${details.status.name}", "Source: ${source?.name ?: sourceId}"),
                    onAddToLibrary = {
                        scope.launch {
                            libraryState = "Saving…"
                            libraryState = runCatching {
                                dependencies.libraryRepository.addMangaToLibrary(sourceId, details)
                                "Added to library"
                            }.getOrElse { "Library error: ${it.message ?: it::class.simpleName}" }
                        }
                    },
                    libraryState = libraryState,
                )
                Spacer(Modifier.height(20.dp))
                Text("Chapters", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (current.value.items.isEmpty()) {
                    EmptyState("No chapters returned by ${source?.name ?: sourceId}.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(current.value.items) { chapter ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoCard(
                                    title = chapter.name,
                                    subtitle = "${formatDate(chapter.dateUpload)} • ${chapter.url}",
                                    tags = listOf("Chapter ${chapter.number}"),
                                    modifier = Modifier.clickable { onOpenChapter(chapter) },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onOpenChapter(chapter) }) { Text("Read") }
                                    Button(onClick = {
                                        scope.launch {
                                            runCatching {
                                                val pages = source?.getPageList(chapter).orEmpty()
                                                dependencies.downloadEngine.enqueueMangaChapter(sourceId, details, chapter, pages, DesktopImageHeaders)
                                            }.onFailure { dependencies.notificationService.notify("Download failed", it.message ?: it::class.simpleName.orEmpty()) }
                                        }
                                    }) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimeDetailsScreen(
    dependencies: DesktopDependencyContainer,
    sourceId: Long,
    anime: SourceMedia,
    onOpenEpisode: (SourceEpisode) -> Unit,
) {
    val source = dependencies.sourceRegistry.get(sourceId) as? MultiplatformAnimeSource
    var state by remember(sourceId, anime.url) { mutableStateOf<LoadState<SourceDetailsData>>(LoadState.Loading) }
    var libraryState by remember(sourceId, anime.url) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun load() {
        if (source == null) {
            state = LoadState.Error("Anime source $sourceId is not registered.")
            return
        }
        state = LoadState.Loading
        scope.launch {
            runCatching {
                val details = source.getAnimeDetails(anime)
                SourceDetailsData(details, source.getEpisodeList(details))
            }.onSuccess { state = LoadState.Success(it) }
                .onFailure { state = LoadState.Error(it.message ?: it::class.simpleName.orEmpty()) }
        }
    }

    LaunchedEffect(sourceId, anime.url) { load() }

    ScreenScaffold("Anime Details") {
        when (val current = state) {
            LoadState.Loading -> LoadingState("Loading real anime details from ${source?.name ?: sourceId}")
            is LoadState.Error -> ErrorState("Anime details failed", current.message, "Retry", onRetry = ::load)
            is LoadState.Success -> {
                val details = current.value.media
                DetailsHeader(
                    media = details,
                    sourceName = source?.name ?: sourceId.toString(),
                    metadata = listOf("Status: ${details.status.name}", "Source: ${source?.name ?: sourceId}"),
                    onAddToLibrary = {
                        scope.launch {
                            libraryState = "Saving…"
                            libraryState = runCatching {
                                dependencies.libraryRepository.addAnimeToLibrary(sourceId, details)
                                "Added to library"
                            }.getOrElse { "Library error: ${it.message ?: it::class.simpleName}" }
                        }
                    },
                    libraryState = libraryState,
                )
                Spacer(Modifier.height(20.dp))
                Text("Episodes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                if (current.value.items.isEmpty()) {
                    EmptyState("No episodes returned by ${source?.name ?: sourceId}.")
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(current.value.items) { episode ->
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                InfoCard(
                                    title = episode.name,
                                    subtitle = "${formatDate(episode.dateUpload)} • ${episode.url}",
                                    tags = listOf("Episode ${episode.number}"),
                                    modifier = Modifier.clickable { onOpenEpisode(episode) },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(onClick = { onOpenEpisode(episode) }) { Text("Watch") }
                                    Button(onClick = {
                                        scope.launch {
                                            runCatching {
                                                val video = source?.getVideoList(episode).orEmpty().firstOrNull()
                                                    ?: error("No video source returned")
                                                dependencies.downloadEngine.enqueueAnimeEpisode(sourceId, details, episode, video)
                                            }.onFailure { dependencies.notificationService.notify("Download failed", it.message ?: it::class.simpleName.orEmpty()) }
                                        }
                                    }) { Text("Download") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailsHeader(
    media: SourceMedia,
    sourceName: String,
    metadata: List<String>,
    onAddToLibrary: () -> Unit,
    libraryState: String?,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
        CoverImage(media.thumbnailUrl, media.title, Modifier.width(180.dp).height(260.dp))
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(media.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            metadata.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
            Text("Source: $sourceName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            media.description?.takeIf(String::isNotBlank)?.let { description ->
                Spacer(Modifier.height(8.dp))
                Text(description)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = onAddToLibrary) { Text("Add to Library") }
                libraryState?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

@Composable
private fun ReaderScreen(
    dependencies: DesktopDependencyContainer,
    sourceId: Long,
    manga: SourceMedia,
    chapter: SourceEpisode,
    fullscreen: Boolean,
    onFullscreen: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val source = dependencies.sourceRegistry.get(sourceId) as? MultiplatformMangaSource
    val scope = rememberCoroutineScope()
    var state by remember(sourceId, chapter.url) { mutableStateOf(ReaderState(chapter = chapter)) }
    val listState = rememberLazyListState()

    fun setCurrentPage(page: Int) {
        val coerced = page.coerceIn(0, (state.pages.size - 1).coerceAtLeast(0))
        state = state.copy(currentPage = coerced)
        scope.launch { dependencies.libraryRepository.saveChapterProgress(sourceId, manga, chapter, coerced, state.pages.size) }
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        scope.launch {
            runCatching {
                val offline = dependencies.downloadEngine.findChapter(sourceId, manga, chapter)
                val pages = if (offline != null) {
                    offline.pages.mapIndexed { index, path -> ReaderPage(index = index, imageUrl = path.toUri().toString()) }
                } else {
                    val activeSource = source ?: error("Manga source $sourceId is not registered.")
                    activeSource.getPageList(chapter).sortedBy(SourcePageImage::index).mapIndexedNotNull { index, page ->
                        val url = page.imageUrl ?: page.url.takeIf(String::isNotBlank)
                        url?.let { ReaderPage(index = index, imageUrl = it, headers = DesktopImageHeaders) }
                    }
                }
                val resume = dependencies.libraryRepository.getChapterProgress(sourceId, manga, chapter)
                pages to resume
            }.onSuccess { (pages, resume) ->
                state = state.copy(pages = pages, currentPage = resume.coerceIn(0, (pages.size - 1).coerceAtLeast(0)), loading = false, error = null)
                if (pages.isNotEmpty()) listState.scrollToItem(state.currentPage)
            }.onFailure {
                state = state.copy(loading = false, error = it.message ?: it::class.simpleName.orEmpty())
            }
        }
    }

    fun moveBy(delta: Int) = setCurrentPage(state.currentPage + delta)

    LaunchedEffect(sourceId, chapter.url) { load() }

    Column(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            when (event.key) {
                Key.DirectionLeft -> { if (state.mode == ReaderMode.HORIZONTAL_RTL) moveBy(1) else moveBy(-1); true }
                Key.DirectionRight -> { if (state.mode == ReaderMode.HORIZONTAL_RTL) moveBy(-1) else moveBy(1); true }
                Key.DirectionUp, Key.PageUp -> { moveBy(-1); true }
                Key.DirectionDown, Key.PageDown -> { moveBy(1); true }
                Key.MoveHome -> { setCurrentPage(0); true }
                Key.MoveEnd -> { setCurrentPage(state.pages.lastIndex); true }
                else -> false
            }
        },
    ) {
        ReaderToolbar(state, fullscreen, onMode = { state = state.copy(mode = it) }, onFitMode = { state = state.copy(fitMode = it) }, onZoom = { state = state.copy(zoom = it) }, onPrevious = { moveBy(-1) }, onNext = { moveBy(1) }, onFullscreen = onFullscreen, onClose = onClose)
        when {
            state.loading -> LoadingState("Resolving real MangaPill pages for ${chapter.name}")
            state.error != null -> ErrorState("Reader failed", state.error.orEmpty(), "Retry", onRetry = ::load)
            state.pages.isEmpty() -> EmptyState("No image pages returned by ${source?.name ?: sourceId}.")
            state.mode == ReaderMode.WEBTOON -> LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(16.dp), modifier = Modifier.fillMaxSize()) {
                items(state.pages) { page ->
                    ReaderPageImage(page, state.fitMode, state.zoom, Modifier.fillMaxWidth())
                }
            }
            state.mode == ReaderMode.DOUBLE_PAGE -> DoublePageReader(state, onPageClick = ::setCurrentPage)
            else -> SinglePageReader(state, onPrevious = { moveBy(-1) }, onNext = { moveBy(1) })
        }
    }
}

@Composable
private fun ReaderToolbar(
    state: ReaderState,
    fullscreen: Boolean,
    onMode: (ReaderMode) -> Unit,
    onFitMode: (ReaderFitMode) -> Unit,
    onZoom: (Float) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onFullscreen: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(state.chapter.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Button(onClick = onPrevious, enabled = state.currentPage > 0) { Text("Previous") }
        Button(onClick = onNext, enabled = state.currentPage < state.pages.lastIndex) { Text("Next") }
        EnumMenu("Mode: ${state.mode.name}", ReaderMode.entries, onMode)
        EnumMenu("Fit: ${state.fitMode.name}", ReaderFitMode.entries, onFitMode)
        Text("${state.currentPage + 1}/${state.pages.size}")
        Slider(value = state.zoom, onValueChange = onZoom, valueRange = 0.5f..3f, modifier = Modifier.width(120.dp))
        Button(onClick = { onZoom(1f) }) { Text("100%") }
        Button(onClick = { onFullscreen(!fullscreen) }) { Text(if (fullscreen) "Window" else "Fullscreen") }
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun <T : Enum<T>> EnumMenu(label: String, values: List<T>, onSelected: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(onClick = { expanded = true }) { Text(label) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            values.forEach { value ->
                DropdownMenuItem(text = { Text(value.name) }, onClick = { expanded = false; onSelected(value) })
            }
        }
    }
}

@Composable
private fun SinglePageReader(state: ReaderState, onPrevious: () -> Unit, onNext: () -> Unit) {
    Box(Modifier.fillMaxSize().pointerInput(state.currentPage) {
        detectTapGestures(
            onDoubleTap = {},
            onTap = { offset -> if (offset.x < size.width / 2) onPrevious() else onNext() },
        )
    }, contentAlignment = Alignment.Center) {
        state.pages.getOrNull(state.currentPage)?.let { page -> ReaderPageImage(page, state.fitMode, state.zoom, Modifier.fillMaxSize()) }
    }
}

@Composable
private fun DoublePageReader(state: ReaderState, onPageClick: (Int) -> Unit) {
    val firstIndex = if (state.currentPage % 2 == 0) state.currentPage else state.currentPage - 1
    val pages = listOfNotNull(state.pages.getOrNull(firstIndex), state.pages.getOrNull(firstIndex + 1))
    Row(Modifier.fillMaxSize().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        pages.forEach { page ->
            ReaderPageImage(page, state.fitMode, state.zoom, Modifier.weight(1f).fillMaxHeight().clickable { onPageClick(page.index) })
        }
    }
}

@Composable
private fun ReaderPageImage(page: ReaderPage, fitMode: ReaderFitMode, zoom: Float, modifier: Modifier = Modifier) {
    NetworkImage(
        url = page.imageUrl,
        headers = page.headers,
        contentDescription = "Page ${page.index + 1}",
        modifier = modifier.pointerInput(page.imageUrl, zoom) { detectTapGestures(onDoubleTap = {}) },
        contentScale = when (fitMode) {
            ReaderFitMode.FIT_WIDTH -> ContentScale.FillWidth
            ReaderFitMode.FIT_HEIGHT -> ContentScale.Fit
            ReaderFitMode.ORIGINAL -> ContentScale.None
        },
    )
}

@Composable
private fun PlayerScreen(
    dependencies: DesktopDependencyContainer,
    sourceId: Long,
    anime: SourceMedia,
    episode: SourceEpisode,
    fullscreen: Boolean,
    onFullscreen: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    val source = dependencies.sourceRegistry.get(sourceId) as? MultiplatformAnimeSource
    val scope = rememberCoroutineScope()
    val engine = remember(sourceId, episode.url) { DesktopMediaPlayerEngine() }
    val engineState by engine.state.collectAsState()
    var state by remember(sourceId, episode.url) { mutableStateOf(PlayerState()) }

    fun persistProgress() {
        val currentEngineState = engine.state.value
        val positionSeconds = currentEngineState.positionMs / 1000
        val durationSeconds = currentEngineState.durationMs / 1000
        if (positionSeconds > 0 || durationSeconds > 0) {
            scope.launch { dependencies.libraryRepository.saveEpisodeProgress(sourceId, anime, episode, positionSeconds, durationSeconds) }
        }
    }

    fun selectVideo(video: VideoSource) {
        val resumeMs = engineState.positionMs.takeIf { it > 0 } ?: state.resumePositionMs
        state = state.copy(selectedVideo = video, error = null, resumePositionMs = resumeMs)
        engine.load(video, resumeMs)
    }

    fun load() {
        state = state.copy(loading = true, error = null)
        scope.launch {
            runCatching {
                val offline = dependencies.downloadEngine.findEpisode(sourceId, anime, episode)
                val videos = if (offline != null) {
                    listOf(VideoSource(url = offline.file.toUri().toString(), quality = "Downloaded"))
                } else {
                    val activeSource = source ?: error("Anime source $sourceId is not registered.")
                    activeSource.getVideoList(episode)
                }
                val progress = dependencies.libraryRepository.getEpisodeProgress(sourceId, anime, episode)
                videos to progress
            }.onSuccess { (videos, progress) ->
                val selected = videos.firstOrNull()
                val resumeMs = progress * 1000
                state = state.copy(videos = videos, selectedVideo = selected, resumePositionMs = resumeMs, loading = false)
                selected?.let { engine.load(it, resumeMs) }
            }.onFailure { state = state.copy(loading = false, error = it.message ?: it::class.simpleName.orEmpty()) }
        }
    }

    LaunchedEffect(sourceId, episode.url) { load() }
    LaunchedEffect(sourceId, episode.url) {
        while (true) {
            delay(10_000)
            persistProgress()
        }
    }
    DisposableEffect(sourceId, episode.url) {
        onDispose {
            persistProgress()
            engine.release()
        }
    }

    Column(Modifier.fillMaxSize().onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.Spacebar -> { engine.togglePlayPause(); true }
            Key.DirectionLeft -> { engine.seekBy(-10_000); true }
            Key.DirectionRight -> { engine.seekBy(10_000); true }
            Key.DirectionUp -> { engine.setVolume((engineState.volume + 0.05f).coerceAtMost(1f)); true }
            Key.DirectionDown -> { engine.setVolume((engineState.volume - 0.05f).coerceAtLeast(0f)); true }
            Key.M -> { engine.setMuted(!engineState.muted); true }
            Key.F -> { onFullscreen(!fullscreen); true }
            Key.Escape -> { if (fullscreen) onFullscreen(false) else { persistProgress(); onClose() }; true }
            else -> false
        }
    }) {
        PlayerToolbar(
            state = state,
            engineState = engineState,
            fullscreen = fullscreen,
            onPlayPause = engine::togglePlayPause,
            onSeek = engine::seekBy,
            onPosition = engine::seekTo,
            onVolume = engine::setVolume,
            onMute = { engine.setMuted(!engineState.muted) },
            onSpeed = engine::setSpeed,
            onQuality = ::selectVideo,
            onAudio = engine::selectAudioTrack,
            onSubtitle = engine::selectSubtitleTrack,
            onFullscreen = onFullscreen,
            onClose = {
                persistProgress()
                onClose()
            },
        )
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            when {
                state.loading -> LoadingState("Resolving Jellyfin video streams for ${episode.name}")
                state.error != null -> ErrorState("Player failed", state.error.orEmpty(), "Retry", onRetry = ::load)
                engineState.error != null -> ErrorState("Player failed", "${engineState.error?.type}: ${engineState.error?.message}", "Retry", onRetry = ::load)
                state.selectedVideo == null -> EmptyState("No video sources returned by ${source?.name ?: sourceId}.")
                else -> PlayerVideoSurface(engine, Modifier.fillMaxSize())
            }
        }
    }
}

@Composable
private fun PlayerToolbar(
    state: PlayerState,
    engineState: MediaPlayerEngineState,
    fullscreen: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onPosition: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onMute: () -> Unit,
    onSpeed: (Float) -> Unit,
    onQuality: (VideoSource) -> Unit,
    onAudio: (String?) -> Unit,
    onSubtitle: (String?) -> Unit,
    onFullscreen: (Boolean) -> Unit,
    onClose: () -> Unit,
) {
    var qualityMenu by remember { mutableStateOf(false) }
    var audioMenu by remember { mutableStateOf(false) }
    var subtitleMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = onPlayPause) { Text(if (engineState.playing) "Pause" else "Play") }
        Button(onClick = { onSeek(-10_000) }) { Text("-10s") }
        Button(onClick = { onSeek(10_000) }) { Text("+10s") }
        Text(formatDuration(engineState.positionMs), modifier = Modifier.width(72.dp))
        val durationForSlider = engineState.durationMs.coerceAtLeast(1)
        Slider(
            value = engineState.positionMs.coerceIn(0, durationForSlider).toFloat(),
            onValueChange = { onPosition(it.toLong()) },
            valueRange = 0f..durationForSlider.toFloat(),
            modifier = Modifier.width(180.dp),
        )
        Text(formatDuration(engineState.durationMs), modifier = Modifier.width(72.dp))
        Button(onClick = onMute) { Text(if (engineState.muted) "Unmute" else "Mute") }
        Slider(value = engineState.volume, onValueChange = onVolume, modifier = Modifier.width(96.dp))
        Slider(value = engineState.speed, onValueChange = onSpeed, valueRange = 0.5f..2f, modifier = Modifier.width(96.dp))
        Box {
            Button(onClick = { qualityMenu = true }) { Text(state.selectedVideo?.quality ?: "Quality") }
            DropdownMenu(expanded = qualityMenu, onDismissRequest = { qualityMenu = false }) {
                state.videos.forEach { video ->
                    DropdownMenuItem(text = { Text(video.quality) }, onClick = { qualityMenu = false; onQuality(video) })
                }
            }
        }
        if (engineState.audioTracks.isNotEmpty()) {
            Box {
                Button(onClick = { audioMenu = true }) { Text(engineState.audioTracks.firstOrNull { it.selected }?.title ?: "Audio") }
                DropdownMenu(expanded = audioMenu, onDismissRequest = { audioMenu = false }) {
                    engineState.audioTracks.forEach { track ->
                        DropdownMenuItem(text = { Text(track.titleWithLanguage()) }, onClick = { audioMenu = false; onAudio(track.id) })
                    }
                }
            }
        }
        if (engineState.subtitleTracks.isNotEmpty()) {
            Box {
                Button(onClick = { subtitleMenu = true }) { Text(engineState.subtitleTracks.firstOrNull { it.selected }?.title ?: "Subtitles") }
                DropdownMenu(expanded = subtitleMenu, onDismissRequest = { subtitleMenu = false }) {
                    engineState.subtitleTracks.forEach { track ->
                        DropdownMenuItem(text = { Text(track.titleWithLanguage()) }, onClick = { subtitleMenu = false; onSubtitle(track.id.takeUnless { it == "no" }) })
                    }
                }
            }
        }
        Text(engineState.status.name, modifier = Modifier.weight(1f))
        Button(onClick = { onFullscreen(!fullscreen) }) { Text(if (fullscreen) "Window" else "Fullscreen") }
        Button(onClick = onClose) { Text("Close") }
    }
}

@Composable
private fun PlayerVideoSurface(engine: MediaPlayerEngine, modifier: Modifier = Modifier) {
    SwingPanel(
        modifier = modifier,
        factory = {
            object : Canvas() {
                override fun addNotify() {
                    super.addNotify()
                    engine.attach(this)
                }
            }.apply { background = java.awt.Color.BLACK }
        },
        update = { canvas -> if (canvas.isDisplayable) engine.attach(canvas) },
    )
}

private fun formatDuration(milliseconds: Long): String {
    val totalSeconds = (milliseconds / 1000).coerceAtLeast(0)
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

private fun PlayerTrack.titleWithLanguage(): String {
    return if (language.isNullOrBlank()) title else "$title ($language)"
}

@Composable
private fun CoverImage(url: String?, title: String, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        Box(modifier.clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Text(title.firstOrNull()?.uppercase() ?: "?", style = MaterialTheme.typography.headlineLarge)
        }
    } else {
        NetworkImage(url, DesktopImageHeaders, title, modifier.clip(RoundedCornerShape(16.dp)), ContentScale.Crop)
    }
}

@Composable
private fun NetworkImage(
    url: String,
    headers: HttpHeaders,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalPlatformContext.current
    val request = remember(url, headers) {
        val networkHeaders = NetworkHeaders.Builder().apply {
            headers.forEach { (name, value) -> set(name, value) }
        }.build()
        ImageRequest.Builder(context)
            .data(url)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .httpHeaders(networkHeaders)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
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
private fun DownloadsScreen(dependencies: DesktopDependencyContainer) {
    val downloads by dependencies.downloadEngine.items.collectAsState()
    ScreenScaffold("Downloads") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Default download directory: ${dependencies.directories.downloads}", modifier = Modifier.weight(1f))
            Button(onClick = { dependencies.externalBrowser.open(dependencies.directories.downloads.toUri()) }) { Text("Open Folder") }
        }
        Spacer(Modifier.height(12.dp))
        if (downloads.isEmpty()) {
            EmptyState("No desktop downloads queued yet. Use Download from manga chapters or anime episodes.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(downloads.sortedByDescending(DesktopDownloadItem::createdAt)) { item ->
                    DownloadQueueCard(dependencies, item)
                }
            }
        }
    }
}

@Composable
private fun DownloadQueueCard(dependencies: DesktopDependencyContainer, item: DesktopDownloadItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SourceAvatar(item.mediaTitle)
                Column(Modifier.weight(1f)) {
                    Text(item.mediaTitle, fontWeight = FontWeight.Bold)
                    Text("${item.itemTitle} • ${item.kind.name} • ${item.status.name}", style = MaterialTheme.typography.bodySmall)
                    item.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                }
                Text(item.progressLabel())
            }
            item.fraction?.let { CircularProgressIndicator(progress = { it.coerceIn(0f, 1f) }, modifier = Modifier.size(32.dp)) }
            Text(item.targetPath.toString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { dependencies.downloadEngine.pause(item.id) }, enabled = item.status == DesktopDownloadStatus.Downloading || item.status == DesktopDownloadStatus.Queued) { Text("Pause") }
                Button(onClick = { dependencies.downloadEngine.resume(item.id) }, enabled = item.status == DesktopDownloadStatus.Paused) { Text("Resume") }
                Button(onClick = { dependencies.downloadEngine.retry(item.id) }, enabled = item.status == DesktopDownloadStatus.Failed) { Text("Retry") }
                Button(onClick = { dependencies.downloadEngine.cancel(item.id) }, enabled = item.status == DesktopDownloadStatus.Downloading || item.status == DesktopDownloadStatus.Queued || item.status == DesktopDownloadStatus.Paused) { Text("Cancel") }
                Button(onClick = { dependencies.downloadEngine.deleteFiles(item.id) }) { Text("Delete") }
                Button(onClick = { dependencies.externalBrowser.open((item.targetPath.parent ?: item.targetPath).toUri()) }) { Text("Open Folder") }
            }
        }
    }
}

private fun DesktopDownloadItem.progressLabel(): String {
    return when {
        totalUnits > 0 -> "$completedUnits/$totalUnits"
        downloadedBytes > 0L -> formatBytes(downloadedBytes)
        else -> status.name
    }
}

private fun formatBytes(bytes: Long): String {
    val units = listOf("B", "KiB", "MiB", "GiB")
    var value = bytes.toDouble()
    var unit = 0
    while (value >= 1024 && unit < units.lastIndex) {
        value /= 1024
        unit += 1
    }
    return if (unit == 0) "$bytes ${units[unit]}" else "%.1f %s".format(value, units[unit])
}

@Composable
private fun ExtensionsScreen(dependencies: DesktopDependencyContainer) {
    val extensions by dependencies.extensionManager.extensions.collectAsState(emptyList())
    val sources by dependencies.sourceRegistry.sources().collectAsState(emptyList())
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { dependencies.extensionManager.reload() }

    ScreenScaffold("Extensions") {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { scope.launch { dependencies.extensionManager.reload() } }) { Text("Reload") }
            Button(onClick = {
                val jar = dependencies.filePicker.pickFile("Install desktop extension JAR")
                if (jar != null) {
                    scope.launch {
                        status = "Installing…"
                        status = runCatching {
                            dependencies.extensionManager.installJar(jar)
                            "Installed ${jar.fileName}"
                        }.getOrElse { "Install failed: ${it.message ?: it::class.simpleName}" }
                    }
                }
            }) { Text("Install JAR") }
            Button(onClick = { dependencies.externalBrowser.open(dependencies.directories.extensions.toUri()) }) { Text("Open Extensions Folder") }
        }
        status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(12.dp))
        Text("Installed JARs", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (extensions.isEmpty()) {
            EmptyState("No desktop extension JARs are installed yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(260.dp)) {
                items(extensions) { extension ->
                    val extensionStatus = when (extension.status) {
                        DesktopExtensionStatus.Loaded -> "Loaded"
                        DesktopExtensionStatus.Invalid -> "Invalid manifest"
                        DesktopExtensionStatus.Failed -> "Failed: ${extension.error ?: "Unknown error"}"
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        InfoCard(
                            title = extension.name,
                            subtitle = "${extension.language.uppercase()} • ${extension.version} • $extensionStatus",
                            tags = listOfNotNull(extension.type?.name, extension.id) + extension.sources.map { it.name },
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { dependencies.externalBrowser.open(extension.packagePath.parent.toUri()) }) { Text("Open Folder") }
                            Button(onClick = {
                                scope.launch {
                                    status = "Removing…"
                                    status = runCatching {
                                        dependencies.extensionManager.remove(extension)
                                        "Removed ${extension.name}"
                                    }.getOrElse { "Remove failed: ${it.message ?: it::class.simpleName}" }
                                }
                            }) { Text("Remove") }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Registered Sources", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sources) { source ->
                InfoCard(
                    title = source.name,
                    subtitle = "${source.lang.uppercase()} • ${source.id} • ${sourceTypeLabel(source)}",
                    tags = source.capabilities.map { it.name }.sorted(),
                )
            }
        }
    }
}

@Composable
private fun SettingsScreen(dependencies: DesktopDependencyContainer) {
    ScreenScaffold("Settings") {
        Text("Settings groups backed by desktop services and existing preferences will be enabled incrementally.")
        Spacer(Modifier.height(16.dp))
        JellyfinSettingsPanel(dependencies)
        Spacer(Modifier.height(16.dp))
        AppearanceSettingsPanel(dependencies)
        Spacer(Modifier.height(16.dp))
        BackupSettingsPanel(dependencies)
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
            "Backup",
            "Advanced",
            "About",
        ).forEach { group ->
            InfoCard(title = group, subtitle = settingSubtitle(group, dependencies), tags = emptyList())
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AppearanceSettingsPanel(dependencies: DesktopDependencyContainer) {
    val themeMode by dependencies.preferences.themeMode.collectAsState()
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Appearance", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Theme changes are saved immediately and applied to the running desktop window.")
            EnumMenu("Theme: ${themeMode.name}", DesktopThemeMode.entries) { dependencies.preferences.setThemeMode(it) }
        }
    }
}

@Composable
private fun BackupSettingsPanel(dependencies: DesktopDependencyContainer) {
    var status by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Desktop Backup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Exports library entries, history progress, chapter read progress, and episode watch progress. Secure tokens and cookies are not included.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    val target = dependencies.filePicker.saveFile("Export Aniyomi backup", "aniyomi-desktop-backup.json")
                    if (target != null) {
                        scope.launch {
                            status = "Exporting…"
                            status = runCatching { dependencies.backupService.exportTo(target).toStatus("Exported") }
                                .getOrElse { "Export failed: ${it.message ?: it::class.simpleName}" }
                        }
                    }
                }) { Text("Export") }
                Button(onClick = {
                    val source = dependencies.filePicker.pickFile("Preview Aniyomi backup")
                    if (source != null) {
                        scope.launch {
                            status = "Reading…"
                            status = runCatching { dependencies.backupService.preview(source).toStatus("Preview") }
                                .getOrElse { "Preview failed: ${it.message ?: it::class.simpleName}" }
                        }
                    }
                }) { Text("Preview") }
                Button(onClick = {
                    val source = dependencies.filePicker.pickFile("Restore Aniyomi backup")
                    if (source != null) {
                        scope.launch {
                            status = "Restoring…"
                            status = runCatching { dependencies.backupService.importFrom(source).toStatus("Restored") }
                                .getOrElse { "Restore failed: ${it.message ?: it::class.simpleName}" }
                        }
                    }
                }) { Text("Restore") }
            }
            status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        }
    }
}


@Composable
private fun JellyfinSettingsPanel(dependencies: DesktopDependencyContainer) {
    val sources by dependencies.sourceRegistry.sources().collectAsState(emptyList())
    val jellyfin = sources.filterIsInstance<MultiplatformAnimeSource>().firstOrNull { it.name.startsWith("Jellyfin") }
    var serverUrl by remember(jellyfin?.id) { mutableStateOf("") }
    var userId by remember(jellyfin?.id) { mutableStateOf("") }
    var libraryId by remember(jellyfin?.id) { mutableStateOf("") }
    var apiToken by remember(jellyfin?.id) { mutableStateOf("") }
    var status by remember(jellyfin?.id) { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(jellyfin?.id) {
        val sourceId = jellyfin?.id ?: return@LaunchedEffect
        val preferences = DesktopSourcePreferenceStores.forSource(sourceId)
        serverUrl = preferences.getString("host_url", "")
        userId = preferences.getString("user_id", "")
        libraryId = preferences.getString("library_id", "")
    }

    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jellyfin Source Settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            if (jellyfin == null) {
                Text("Install or reload the Jellyfin desktop extension before configuring the source.")
                return@Column
            }
            OutlinedTextField(serverUrl, { serverUrl = it }, label = { Text("Server URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(userId, { userId = it }, label = { Text("User ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(apiToken, { apiToken = it }, label = { Text("API Token (stored with Windows DPAPI)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(libraryId, { libraryId = it }, label = { Text("Library ID optional") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = {
                    scope.launch {
                        val preferences = DesktopSourcePreferenceStores.forSource(jellyfin.id)
                        val secrets = DesktopSourcePreferenceStores.secretStoreForSource(jellyfin.id)
                        preferences.putString("host_url", serverUrl.trim())
                        preferences.putString("user_id", userId.trim())
                        preferences.putString("library_id", libraryId.trim())
                        val secretSaved = if (apiToken.isBlank()) true else secrets.putString("api_key", apiToken)
                        status = if (secretSaved) "Saved" else "Secure storage unsupported on this desktop platform"
                    }
                }) { Text("Save") }
                Button(onClick = {
                    scope.launch {
                        status = "Testing…"
                        status = runCatching {
                            jellyfin.searchAnime(1, "", jellyfin.getAnimeFilters())
                            "Connected"
                        }.getOrElse { error ->
                            val message = error.message.orEmpty()
                            when {
                                message.contains("401") || message.contains("Unauthorized", ignoreCase = true) -> "Unauthorized"
                                message.contains("URL", ignoreCase = true) -> "Invalid URL"
                                else -> "Server unreachable: ${message.ifBlank { error::class.simpleName.orEmpty() }}"
                            }
                        }
                    }
                }) { Text("Test Connection") }
                status?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}

private fun settingSubtitle(group: String, dependencies: DesktopDependencyContainer): String = when (group) {
    "Downloads" -> dependencies.directories.downloads.toString()
    "Network" -> "KtorNetworkClient with CIO engine"
    "Sources" -> "BuiltinSourceRegistry (${dependencies.sourceRegistry.get(-1)?.name ?: "0 bundled sources"})"
    "About" -> "${dependencies.platformInfo.name} ${dependencies.platformInfo.version} (${dependencies.platformInfo.architecture})"
    "Tracking" -> "Jellyfin API tokens use Windows DPAPI-backed desktop source secret storage"
    "Backup" -> "JSON import/export for library entries and progress; secrets are excluded"
    "Appearance" -> "Theme mode persists as System, Light, or Dark"
    else -> "No desktop-specific preference bound yet"
}

private fun DesktopBackupSummary.toStatus(action: String): String {
    return "$action ${path.fileName}: $mangaEntries manga, $chapters chapters, $animeEntries anime, $episodes episodes"
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
