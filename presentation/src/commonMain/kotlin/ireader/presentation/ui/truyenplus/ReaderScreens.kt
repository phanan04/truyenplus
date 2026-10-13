package ireader.presentation.ui.truyenplus

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import ireader.domain.models.BookCover
import ireader.domain.models.entities.ReadingStatisticsType1
import ireader.presentation.ui.component.components.IBookImageComposable
import ireader.presentation.ui.component.list.layouts.bookReadingProgress

/** Data is supplied by the original library/reading services, never by a demo repository. */
data class ReaderBookUi(
    val id: Long, val title: String, val cover: BookCover? = null,
    val read: Int = 0, val total: Int = 0, val pinned: Boolean = false,
    val chapter: String = "", val lastRead: Long = 0,
) {
    val progress get() = bookReadingProgress(total, (total - read).coerceAtLeast(0))
}

@Composable
fun ReaderBackdrop(content: @Composable BoxScope.() -> Unit) {
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
        val c=MaterialTheme.colorScheme

        ReaderResponsiveFrame { Box(Modifier.fillMaxSize().background(c.background), content = content) }
    }
}

@Composable
fun ReaderCover(book: ReaderBookUi, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(6.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (book.cover != null && !book.cover.cover.isNullOrBlank()) {
            IBookImageComposable(image = book.cover, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(28.dp))
                Text(book.title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Box(Modifier.width(26.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f)))
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, onImport: () -> Unit, onSearch: () -> Unit, onUpdate: (() -> Unit)? = null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var showMenu by remember { mutableStateOf(false) }
    if (LocalReaderViewport.current.compactControls) {
        Column(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=14.dp)) {
            Text(title,style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
            Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.align(Alignment.End)) {
                IconButton(onClick=onSearch) { Icon(Icons.Outlined.Search,strings.localize(Res.string.tp_search_library)) }
                IconButton(onClick=onImport) { Icon(Icons.Outlined.Add,strings.localize(Res.string.tp_import_ebook)) }
                if(onUpdate!=null) IconButton(onClick=onUpdate) { Icon(Icons.Outlined.SystemUpdate,strings.localize(Res.string.tp_app_update)) }
            }
        }
        return
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, strings.localize(Res.string.tp_search_library)) }
        IconButton(onClick = onImport) { Icon(Icons.Outlined.Add, strings.localize(Res.string.tp_import_ebook)) }
        if (onUpdate != null) Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreHoriz, strings.localize(Res.string.tp_app_options)) }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text(strings.localize(Res.string.tp_app_update)) }, leadingIcon = { Icon(Icons.Outlined.SystemUpdate, null) },
                    onClick = { showMenu = false; onUpdate() })
            }
        }
    }
}

@Composable
fun ReaderChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(labels) { index, label ->
            Surface(onClick = { onSelect(index) }, shape = CircleShape,
                color = if (selected == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor=if(selected==index) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                border = BorderStroke(1.dp, if (selected == index) Color.Transparent else Color.Transparent)) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    if (selected == index) Icon(Icons.Default.Check, null, Modifier.size(14.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(eyebrow: String, title: String, onMore: (() -> Unit)? = null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            if(eyebrow.isNotBlank()) Text(eyebrow, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        if (onMore != null) IconButton(onClick = onMore) { Icon(Icons.Default.ChevronRight, strings.localize(Res.string.tp_see_all)) }
    }
}

@Composable
private fun Shelf(books: List<ReaderBookUi>, onBook: (Long) -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(books, key = { it.id }) { book ->
            Column(Modifier.width((146f * LocalDensity.current.fontScale.coerceIn(1f,1.5f)).dp).clickable { onBook(book.id) }) {
                ReaderCover(book, Modifier.fillMaxWidth().height(196.dp))
                Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text(if (book.total > 0) strings.localize(Res.string.tp_chapter_count, book.read, book.total) else strings.localize(Res.string.tp_library_ebook),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                if (book.read > 0) LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clip(CircleShape), drawStopIndicator = {})
            }
        }
    }
}

@Composable
fun ReaderEmptyLibrary(onImport: () -> Unit, modifier: Modifier = Modifier) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
            Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(28.dp).size(58.dp))
        }
        Text(strings.localize(Res.string.tp_first_story), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Text(strings.localize(Res.string.tp_first_story_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
        Button(onClick = onImport, shape = CircleShape, contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp)) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(strings.localize(Res.string.tp_import_book))
        }
    }
}

@Composable
fun ReaderHomeScreen(books: List<ReaderBookUi>, isLoading: Boolean, onBook: (Long) -> Unit, onLibrary: () -> Unit, onImport: () -> Unit, onSearch: () -> Unit, onUpdate: (() -> Unit)? = null, onContinue:(Long)->Unit=onBook) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val current = remember(books) { books.filter { it.read > 0 && (it.total == 0 || it.read < it.total) }.maxByOrNull { it.lastRead } }
    val pinned = remember(books,current) { books.filter { it.pinned && it.id != current?.id } }
    val remaining = remember(books,current,pinned) { books.filter { it.id != current?.id && !it.pinned }.take(12) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalReaderDockInset.current)) {
        item { PageHeader("Truyện+", strings.localize(Res.string.tp_reading_space), onImport, onSearch, onUpdate) }
        if (isLoading && books.isEmpty()) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp)) }
        else if (books.isEmpty()) item { ReaderEmptyLibrary(onImport, Modifier.padding(top = 40.dp)) }
        else {
            current?.let { book ->
                item { SectionTitle("",strings.localize(Res.string.tp_continue_reading)) }
                item { ReaderContinueBook(book) { onContinue(book.id) } }
            }
            if(pinned.isNotEmpty()) {
                item { SectionTitle("",strings.localize(Res.string.tp_pinned_books),onLibrary) }
                item { Shelf(pinned,onBook) }
            }
            if(remaining.isNotEmpty()) {
                item { SectionTitle("",strings.localize(if(current==null) Res.string.tp_start_reading else Res.string.tp_your_library),onLibrary) }
                item { Shelf(remaining,onBook) }
            }
            item {
                TextButton(onClick=onImport,modifier=Modifier.padding(horizontal=12.dp,vertical=20.dp)) {
                    Icon(Icons.Outlined.Add,null,Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(strings.localize(Res.string.tp_import_book))
                }
            }
        }
    }
}

@Composable private fun ReaderContinueBook(book:ReaderBookUi,onRead:()->Unit) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp),shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(18.dp)) {
            ReaderCover(book,Modifier.width(92.dp).height(132.dp).clickable(onClick=onRead))
            Column(Modifier.weight(1f)) {
                Text(book.title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis)
                Text(strings.localize(Res.string.tp_chapter_count,book.read,book.total),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=6.dp))
                LinearProgressIndicator(progress={book.progress.fraction},modifier=Modifier.fillMaxWidth().padding(top=12.dp).height(3.dp).clip(CircleShape),drawStopIndicator={})
                Button(onClick=onRead,shape=CircleShape,modifier=Modifier.padding(top=12.dp).heightIn(min=48.dp).semantics { testTag="home-continue" }) {
                    Text(strings.localize(Res.string.tp_continue_reading),maxLines=2)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ReaderLibraryScreen(books: List<ReaderBookUi>, categories: List<String>, categoryIndex: Int, query: String,
    hasMore: Boolean, isLoading: Boolean, selected: Set<Long>, onQuery: (String) -> Unit, onCategory: (Int) -> Unit,
    onBook: (Long) -> Unit, onSelect: (Long) -> Unit, onImport: () -> Unit,
    onCollections: () -> Unit, onSort: () -> Unit, onLoadMore: () -> Unit,
    requestSearch:Boolean=false,onSearchFocused:()->Unit={}) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val gridState=rememberLazyGridState()
    val searchFocus=remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(requestSearch) { if(requestSearch) { gridState.scrollToItem(0);androidx.compose.runtime.withFrameNanos {};searchFocus.requestFocus();keyboard?.show();onSearchFocused() } }
    LazyVerticalGrid(state=gridState, columns = GridCells.Adaptive((96f * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalReaderDockInset.current), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text(strings.localize(Res.string.tp_library), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(strings.localize(Res.string.tp_library_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    IconButton(onClick = onImport) { Icon(Icons.Default.Add, strings.localize(Res.string.tp_import_ebook)) }
                }
                OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth().padding(top = 16.dp).then(Modifier.focusRequester(searchFocus)), singleLine = true,
                    placeholder = { Text(strings.localize(Res.string.tp_search_books)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = CircleShape)
            }
        }
        if (categories.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.offset(x = (-20).dp).fillMaxWidth()) { ReaderChips(categories, categoryIndex, onCategory) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected.isEmpty()) strings.localize(Res.string.tp_books_shown, books.size) else strings.localize(Res.string.tp_books_selected, selected.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCollections) { Icon(Icons.Outlined.Folder, strings.localize(Res.string.tp_manage_collections)) }
                IconButton(onClick = onSort) { Icon(Icons.Default.SortByAlpha, strings.localize(Res.string.tp_sort_books)) }
            }
        }
        if (isLoading && books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            if (query.isBlank()) ReaderEmptyLibrary(onImport) else Text(strings.localize(Res.string.tp_no_books_found), modifier = Modifier.padding(vertical = 30.dp))
        }
        items(books, key = { it.id }) { book ->
            Column(Modifier.combinedClickable(onClick = { if (selected.isNotEmpty()) onSelect(book.id) else onBook(book.id) }, onLongClick = { onSelect(book.id) })) {
                Box {
                    ReaderCover(book, Modifier.fillMaxWidth().aspectRatio(.73f).then(if (book.id in selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier))
                    if (book.id in selected) Icon(Icons.Default.CheckCircle, strings.localize(Res.string.tp_selected), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                Text(strings.localize(Res.string.tp_chapter_count, book.read, book.total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clip(CircleShape), drawStopIndicator = {})
            }
        }
        if (hasMore && books.isNotEmpty() && query.isBlank()) item(span = { GridItemSpan(maxLineSpan) }) { TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_show_more)) } }
    }
}

@Composable
fun ReaderMiniPlayer(book: ReaderBookUi, onRead: () -> Unit, onListen: () -> Unit, isPlaying: Boolean? = null, onTogglePlayback: (() -> Unit)? = null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Surface(onClick = onRead, shape = RoundedCornerShape(18.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).readerPanel(18)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReaderCover(book, Modifier.size(width = 38.dp, height = 50.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.chapter.ifBlank { strings.localize(Res.string.tp_continue_story) }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onListen) { Icon(Icons.Outlined.Headphones, strings.localize(Res.string.tp_continue_listening), tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onTogglePlayback ?: onRead) { Icon(if (isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying == null) strings.localize(Res.string.tp_continue_reading) else if (isPlaying) strings.localize(Res.string.tp_pause) else strings.localize(Res.string.tp_resume)) }
        }
    }
}

@Composable
fun ReaderProgressScreen(stats: ReadingStatisticsType1, books: List<ReaderBookUi>, history: List<ReaderHistoryUi>, onBook: (Long) -> Unit, onHistory: (ReaderHistoryUi) -> Unit, onMoreHistory: () -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalReaderDockInset.current)) {
        item { Column(Modifier.padding(20.dp)) { Text(strings.localize(Res.string.tp_reading_journey), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text(strings.localize(Res.string.tp_journey_hint), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) } }
        item { ReaderChips(listOf(strings.localize(Res.string.tp_overview), strings.localize(Res.string.tp_history)), tab) { tab = it } }
        if (tab == 0) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Column(Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text(strings.localize(Res.string.tp_streak_days, stats.readingStreak), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text(strings.localize(Res.string.tp_reading_streak), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric("${stats.totalReadingTimeMinutes / 60}h ${stats.totalReadingTimeMinutes % 60}m", strings.localize(Res.string.tp_reading_time), Modifier.weight(1f))
                            Metric("${stats.totalChaptersRead}", strings.localize(Res.string.tp_chapters_read), Modifier.weight(1f))
                            Metric("${stats.booksCompleted}", strings.localize(Res.string.tp_books_completed), Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionTitle(strings.localize(Res.string.tp_book_progress), strings.localize(Res.string.tp_currently_reading)) }
            val reading = books.filter { it.read > 0 && it.read < it.total }
            if (reading.isEmpty()) item { Text(strings.localize(Res.string.tp_progress_hint), modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(reading, key = { it.id }) { book ->
                Surface(onClick = { onBook(book.id) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .8f), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ReaderCover(book, Modifier.size(width = 48.dp, height = 66.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(strings.localize(Res.string.tp_book_percent, book.read, book.total, book.progress.percent), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                            LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().height(3.dp), drawStopIndicator = {})
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        } else {
            item { SectionTitle(strings.localize(Res.string.tp_recently_read), strings.localize(Res.string.tp_your_history)) }
            if (history.isEmpty()) item { Text(strings.localize(Res.string.tp_no_history), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
            items(history, key = { it.id }) { entry ->
                ListItem(headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, supportingContent = { Text(entry.chapter, maxLines = 2, overflow = TextOverflow.Ellipsis) }, leadingContent = { Icon(Icons.Outlined.History, null) }, trailingContent = { Icon(Icons.Default.PlayArrow, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onHistory(entry) })
            }
            if (history.isNotEmpty()) item { TextButton(onClick = onMoreHistory, modifier = Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_more_history)) } }
        }
    }
}

data class ReaderHistoryUi(val id: Long, val bookId: Long, val chapterId: Long, val title: String, val chapter: String)
@Composable private fun Metric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier) { Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) } }
