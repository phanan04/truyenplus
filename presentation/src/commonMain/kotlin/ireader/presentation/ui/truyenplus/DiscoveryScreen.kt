package ireader.presentation.ui.truyenplus

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.discovery.*
import ireader.presentation.imageloader.IImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(state: DiscoveryState, onRefresh: () -> Unit,
    translationState: DiscoveryTranslationState = DiscoveryTranslationState(), language: String = "vi",
    onConfigure: (DiscoverySource, String) -> Unit = { _, _ -> }, onRetryTranslation: () -> Unit = {}) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var sourceIndex by rememberSaveable { mutableIntStateOf(0) }
    var sectionIndex by rememberSaveable { mutableIntStateOf(0) }
    var genre by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selected by remember { mutableStateOf<DiscoveryBook?>(null) }
    var linkError by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val openUrl: (String) -> Unit = { linkError = runCatching { uriHandler.openUri(it) }.isFailure }
    val source = DiscoverySource.entries[sourceIndex]
    val section = DiscoverySection.entries[sectionIndex]
    val feed = state.feeds[source]
    val translations = if (translationState.language == language) translationState.values else emptyMap()
    LaunchedEffect(source, language) { onConfigure(source, language) }
    val genres = remember(feed) { feed?.books.orEmpty().flatMap { it.genres }.distinct().sorted() }
    val books = remember(feed, section, genre, query, translations) { filterDiscoveryBooks(feed, section, genre, query, translations) }
    val listState = rememberLazyGridState()
    LaunchedEffect(sourceIndex, sectionIndex, genre, query) { listState.scrollToItem(0) }
    LaunchedEffect(genres) { if (genre !in genres) genre = "" }

    LazyVerticalGrid(columns = GridCells.Adaptive(320.dp), state = listState,
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 185.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.localize(Res.string.tp_discover_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text(strings.localize(Res.string.tp_discover_subtitle), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, strings.localize(Res.string.tp_refresh)) }
                }
                ReaderChips(DiscoverySource.entries.map { it.label }, sourceIndex) { sourceIndex = it; genre = "" }
                Spacer(Modifier.height(12.dp))
                ReaderChips(DiscoverySection.entries.map { strings.localize(when (it) {
                    DiscoverySection.Ranking -> Res.string.tp_ranking
                    DiscoverySection.Hot -> Res.string.tp_hot
                    DiscoverySection.New -> Res.string.tp_new_rankings
                }) }, sectionIndex) { sectionIndex = it }
                OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text(strings.localize(Res.string.tp_search_novels)) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, strings.localize(Res.string.tp_clear_search)) } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp))
                if (genres.isNotEmpty()) ReaderChips(listOf(strings.localize(Res.string.tp_all_genres)) + genres.map { tag ->
                        val localized = feed?.books.orEmpty().firstNotNullOfOrNull { book ->
                            translations[book.key]?.genres?.getOrNull(book.genres.indexOf(tag))
                        }
                        discoveryGenreLabel(tag, strings, localized)
                    },
                    if (genre.isBlank()) 0 else genres.indexOf(genre) + 1) { genre = if (it == 0) "" else genres[it - 1] }
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${source.label} · ${strings.localize(when (source) { DiscoverySource.Qidian -> Res.string.tp_board_qidian; DiscoverySource.Fanqie -> Res.string.tp_board_fanqie; DiscoverySource.Jinjiang -> Res.string.tp_board_jinjiang })}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (feed != null) {
                        Text(strings.localize(Res.string.tp_source_date, feed.updatedTime.ifBlank { feed.updatedDate }), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(strings.localize(Res.string.tp_novels_count, books.size), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (section == DiscoverySection.New) Text(strings.localize(Res.string.tp_new_ranking_hint),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.errors[source]?.let { error ->
                        Text(strings.localize(if (error.contains("cache")) Res.string.tp_cache_error else Res.string.tp_feed_error), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                        if (feed != null && !error.contains("cache")) Text(strings.localize(Res.string.tp_feed_cached), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRefresh, enabled = !state.refreshing) { Text(strings.localize(Res.string.tp_retry)) }
                    }
                    if (translationState.translating) Text(strings.localize(Res.string.tp_translation_loading), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (translationState.failed) {
                        Text(strings.localize(Res.string.tp_translation_failed), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRetryTranslation) { Text(strings.localize(Res.string.tp_retry)) }
                    }
                    if (linkError) Text(strings.localize(Res.string.tp_link_error), color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.AutoStories, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when {
                    feed == null && state.refreshing -> strings.localize(Res.string.tp_loading_feeds)
                    feed == null -> strings.localize(Res.string.tp_no_source_data, source.label)
                    query.isNotBlank() || genre.isNotBlank() -> strings.localize(Res.string.tp_no_novels_filter)
                    section == DiscoverySection.New -> strings.localize(Res.string.tp_no_new_rankings)
                    else -> strings.localize(Res.string.tp_no_novels)
                }, style = MaterialTheme.typography.bodyMedium)
                if (feed == null && !state.refreshing && source !in state.errors) TextButton(onClick = onRefresh) { Text(strings.localize(Res.string.tp_load_data)) }
                if (query.isNotBlank() || genre.isNotBlank()) TextButton(onClick = { genre = ""; query = "" }) { Text(strings.localize(Res.string.tp_clear_filters)) }
            }
        }
        items(books, key = { it.key }) { book ->
            DiscoveryBookCard(book, translations[book.key], Modifier.padding(horizontal = 20.dp)) { selected = book }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(strings.localize(Res.string.tp_translation_notice),
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    selected?.let { book ->
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            val currentBook = feed?.books?.firstOrNull { it.key == book.key } ?: book
            DiscoveryDetailsContent(currentBook, translations[currentBook.key], openUrl)
        }
    }
}

@Composable
private fun DiscoveryCover(book: DiscoveryBook, modifier: Modifier) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (book.coverUrl != null) IImageLoader(model = book.coverUrl, modifier = Modifier.fillMaxSize(),
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest), contentScale = ContentScale.Crop,
            contentDescription = strings.localize(Res.string.tp_cover, book.title), enableMemoryCache = true, enableDiskCache = true, onLoading = null)
    }
}

@Composable
private fun DiscoveryBookCard(book: DiscoveryBook, translation: ireader.domain.data.repository.DiscoveryTranslation?, modifier: Modifier, onClick: () -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DiscoveryCover(book, Modifier.size(82.dp, 116.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(book.source.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    book.rank?.let { Text("#$it", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium) }
                }
                Text(translation?.title ?: book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text((translation?.description ?: book.description).ifBlank { strings.localize(Res.string.tp_no_description) }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                book.genres.firstOrNull()?.let { Text(discoveryGenreLabel(it, strings, translation?.genres?.getOrNull(book.genres.indexOf(it))), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (translation != null) DiscoveryAttribution()
            }
        }
    }
}

@Composable
fun DiscoveryDetailsContent(book: DiscoveryBook, localized: ireader.domain.data.repository.DiscoveryTranslation? = null, onOpenUrl: (String) -> Unit) {
    var original by remember(book.key) { mutableStateOf(false) }
    val translation = if (original) null else localized
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DiscoveryCover(book, Modifier.size(108.dp, 156.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(translation?.title ?: book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(book.author, style = MaterialTheme.typography.bodyMedium)
                Text(book.source.label + (book.rank?.let { strings.localize(Res.string.tp_rank, it) } ?: ""), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (book.status.isNotBlank()) Text(translation?.status ?: book.status, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (book.genres.isNotEmpty()) Text(book.genres.joinToString(" · ") { discoveryGenreLabel(it, strings, translation?.genres?.getOrNull(book.genres.indexOf(it))) }, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (localized != null) TextButton(onClick = { original = !original }) {
            Text(strings.localize(if (original) Res.string.tp_translated else Res.string.tp_original))
        }
        Button(onClick = { onOpenUrl(book.url) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(strings.localize(Res.string.tp_open_source, book.source.label))
        }
        if (translation != null) DiscoveryAttribution()
        Text(strings.localize(Res.string.tp_introduction), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text((translation?.description ?: book.description).ifBlank { strings.localize(Res.string.tp_no_description) }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun discoveryGenreLabel(tag: String, strings: ireader.i18n.LocalizeHelper, translated: String? = null): String {
    val resource = when (tag) {
        "玄幻" -> Res.string.tp_genre_xuanhuan
        "仙侠" -> Res.string.tp_genre_xianxia
        "都市" -> Res.string.tp_genre_urban
        "言情" -> Res.string.tp_genre_romance
        "历史" -> Res.string.tp_genre_historical
        "科幻" -> Res.string.tp_genre_scifi
        "武侠" -> Res.string.tp_genre_wuxia
        "奇幻" -> Res.string.tp_genre_fantasy
        "悬疑" -> Res.string.tp_genre_mystery
        "游戏" -> Res.string.tp_genre_game
        "古代言情" -> Res.string.tp_genre_ancient_romance
        "现代言情" -> Res.string.tp_genre_modern_romance
        else -> null
    }
    return resource?.let(strings::localize) ?: translated ?: tag
}

/** Official badge, displayed unchanged next to machine-translated information. */
@Composable
private fun DiscoveryAttribution() {
    val uri = LocalUriHandler.current
    Surface(color = Color.White, shape = RoundedCornerShape(4.dp),
        modifier = Modifier.clickable { runCatching { uri.openUri("https://translate.google.com") } }) {
        Image(painterResource(Res.drawable.google_translate_attribution), "Powered by Google Translate",
            Modifier.padding(4.dp).size(176.dp, 16.dp))
    }
}
