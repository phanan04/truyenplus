package ireader.presentation.ui.truyenplus

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.BookCover
import ireader.domain.models.entities.Chapter
import ireader.presentation.core.*
import ireader.presentation.core.ui.*
import ireader.presentation.ui.book.components.*
import ireader.presentation.ui.book.viewmodel.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import org.koin.compose.koinInject
import ireader.domain.usecases.epub.ImportEpub
import ireader.domain.data.repository.BookMetadataLookup
import ireader.domain.data.repository.BookRepository
import ireader.domain.preferences.prefs.AppPreferences
import ireader.presentation.ui.settings.advance.OnShowImportEpub

@Composable
fun ReaderBookRoute(bookId: Long) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val nav = requireNotNull(LocalNavigator.current)
    val vm: BookDetailViewModel = koinViewModel(parameters = { parametersOf(BookDetailViewModel.Param(bookId)) })
    val state by vm.state.collectAsState()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importer: ImportEpub = koinInject()
    val lookup: BookMetadataLookup = koinInject()
    val books: BookRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val sourcePreference = remember(bookId) { preferences.bookMetadataSource(bookId) }
    val metadataSource by sourcePreference.changes().collectAsState(initial = sourcePreference.get())
    val uriHandler = LocalUriHandler.current
    var showMetadataChoice by remember { mutableStateOf(false) }
    var pickMetadataEpub by remember { mutableStateOf(false) }
    var updatingMetadata by remember { mutableStateOf(false) }
    OnShowImportEpub(pickMetadataEpub) { uris ->
        withContext(Dispatchers.Main) { pickMetadataEpub = false; updatingMetadata = uris.isNotEmpty() }
        if (uris.isNotEmpty()) {
            try {
                val previous = books.findBookById(bookId)
                importer.refreshMetadata(bookId, uris.first())
                val refreshed = books.findBookById(bookId)
                if (previous?.description != refreshed?.description) sourcePreference.set("")
                withContext(Dispatchers.Main) { updatingMetadata = false }
                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_updated))
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                withContext(Dispatchers.Main) { updatingMetadata = false }
                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed))
            }
            finally { withContext(Dispatchers.Main) { updatingMetadata = false } }
        }
    }
    if (showMetadataChoice) AlertDialog(onDismissRequest = { showMetadataChoice = false },
        title = { Text(strings.localize(Res.string.tp_book_metadata)) }, text = {
            Column {
                TextButton(onClick = { showMetadataChoice = false; pickMetadataEpub = true }, Modifier.fillMaxWidth()) {
                    Text(strings.localize(Res.string.tp_metadata_epub))
                }
                TextButton(onClick = {
                    showMetadataChoice = false
                    scope.launch {
                        updatingMetadata = true
                        try {
                            val book = books.findBookById(bookId) ?: error("Book not found")
                            val found = lookup.find(book)
                            if (found == null) { updatingMetadata = false; snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed)) }
                            else {
                                withContext(Dispatchers.IO) { books.updateBook(found.book) }
                                sourcePreference.set(found.sourceUrl)
                                updatingMetadata = false
                                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_updated))
                            }
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { updatingMetadata = false; snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed)) }
                        finally { updatingMetadata = false }
                    }
                }, Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_metadata_online)) }
            }
        }, confirmButton = {})
    LaunchedEffect(bookId) { vm.onScreenResumed() }
    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            when (event) {
                is BookDetailEvent.ShowSnackbar -> snackbars.showSnackbar(event.message)
                is BookDetailEvent.NavigateToReader -> nav.navigateTo(ReaderScreenSpec(event.bookId, event.chapterId))
                BookDetailEvent.NavigateBack -> nav.safePopBackStack()
                else -> Unit // Online-source, migration and recommendation destinations are outside this app.
            }
        }
    }
    ReaderBackdrop {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val current = state) {
                is BookDetailState.Success -> {
                    val chapters = remember(current.chapters) { current.chapters.sortedBy { it.number } }
                    val resume = chapters.firstOrNull { it.id == current.lastReadChapterId }
                        ?: chapters.firstOrNull { !it.read } ?: chapters.firstOrNull()
                    ReaderBookScreen(
                        book = ReaderBookUi(current.book.id, current.book.title, BookCover.from(current.book), chapters.count { it.read }, chapters.size),
                        author = current.book.author.orEmpty(), chapters = chapters, onBack = { nav.safePopBackStack() },
                        onRead = { resume?.let { nav.navigateTo(ReaderScreenSpec(bookId, it.id)) } },
                        onListen = { resume?.let { nav.navigateTo(TTSV2ScreenSpec(bookId, it.id, current.book.sourceId, 0)) } },
                        onChapter = { nav.navigateTo(ReaderScreenSpec(bookId, it.id)) },
                        onExport = { vm.showEpubExportDialog = true },
                        description = current.book.description, activeChapterId = current.lastReadChapterId,
                        onMetadata = { showMetadataChoice = true }, metadataUpdating = updatingMetadata,
                        onMetadataSource = if (metadataSource.isNotBlank()) ({ uriHandler.openUri(metadataSource) }) else null)
                    if (vm.showEpubExportDialog) EpubExportDialog(current.book, current.chapters,
                        hasTranslations = false,
                        onExport = { options -> vm.showEpubExportDialog = false; vm.exportAsEpub(options) },
                        onDismiss = { vm.showEpubExportDialog = false })
                }
                BookDetailState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                    Text(strings.localize(Res.string.tp_cannot_open_book))
                    TextButton(onClick = { nav.safePopBackStack() }) { Text(strings.localize(Res.string.tp_back_library)) }
                }
            }
            SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter))
        }
    }
    val exportProgress by vm.epubExportProgress.collectAsState()
    EpubExportProgressDialog(progress = exportProgress, onDismiss = vm::dismissEpubExportProgress)
}

@Composable
fun ReaderBookScreen(book: ReaderBookUi, author: String, chapters: List<Chapter>, onBack: () -> Unit,
    onRead: () -> Unit, onListen: () -> Unit, onChapter: (Chapter) -> Unit, onExport: () -> Unit,
    description: String = "", activeChapterId: Long? = null, onMetadata: () -> Unit = {},
    metadataUpdating: Boolean = false, onMetadataSource: (() -> Unit)? = null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var expandedSummary by rememberSaveable { mutableStateOf(false) }
    val visible = remember(chapters, filter, query) { chapters.filter { (filter != 1 || it.bookmark) && (filter != 2 || !it.read) && it.name.contains(query, true) } }
    val positions = remember(chapters) { chapters.mapIndexed { index, chapter -> chapter.id to index + 1 }.toMap() }
    ReaderContentPane(maxWidth=960.dp) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, strings.localize(Res.string.tp_back)) }
                Text(strings.localize(Res.string.tp_in_library), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onExport) { Icon(Icons.Outlined.IosShare, strings.localize(Res.string.tp_export_epub)) }
            }
        }
        item {
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                val wide = maxWidth.value / androidx.compose.ui.platform.LocalDensity.current.fontScale >= 520f
                val details: @Composable () -> Unit = {
                    Column(Modifier.fillMaxWidth()) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = if(wide) 0.dp else 24.dp))
                if (author.isNotBlank()) Text(if(author=="Unknown") strings.localize(Res.string.unknown) else author, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Text(strings.localize(Res.string.tp_book_summary, book.total, book.progress.percent), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRead, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.weight(1f).heightIn(min=52.dp)) {
                        Icon(Icons.Outlined.AutoStories, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (book.read > 0) strings.localize(Res.string.tp_continue_reading) else strings.localize(Res.string.tp_start_reading))
                    }
                    OutlinedButton(onClick = onListen, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.heightIn(min=52.dp)) { Icon(Icons.Outlined.Headphones, strings.localize(Res.string.tp_listen_book)) }
                }
                    }
                }
                if(wide) Row(horizontalArrangement=Arrangement.spacedBy(28.dp),verticalAlignment=Alignment.CenterVertically) {
                    ReaderCover(book,Modifier.width(198.dp).height(270.dp))
                    Box(Modifier.weight(1f)) { details() }
                } else Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    ReaderCover(book,Modifier.widthIn(max=198.dp).fillMaxWidth().height(270.dp))
                    details()
                }
            }
        }
        item {
            Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color=androidx.compose.ui.graphics.Color.Transparent) {
                Column(Modifier.padding(vertical=16.dp)) {
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Text(strings.localize(Res.string.tp_book_about),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium)
                        IconButton(onClick=onMetadata,enabled=!metadataUpdating,modifier=Modifier.testTag("book-metadata-refresh")) {
                            Icon(Icons.Default.Refresh,strings.localize(Res.string.tp_book_metadata))
                        }
                    }
                    Text(description.ifBlank { strings.localize(Res.string.tp_metadata_missing) },
                        style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines=if(expandedSummary) Int.MAX_VALUE else 5,overflow=TextOverflow.Ellipsis,
                        modifier=Modifier.clickable(enabled=description.isNotBlank()) { expandedSummary=!expandedSummary })
                    if(metadataUpdating) Text(strings.localize(Res.string.tp_metadata_updating),Modifier.padding(top=8.dp),style=MaterialTheme.typography.labelSmall)
                    onMetadataSource?.let { action -> TextButton(onClick=action) { Text(strings.localize(Res.string.tp_metadata_source)+" · TruyenFull") } }
                }
            }
        }
        item { ReaderChips(listOf(strings.localize(Res.string.tp_chapters), strings.localize(Res.string.tp_bookmarks), strings.localize(Res.string.tp_unread)), filter) { filter = it } }
        item { OutlinedTextField(query, { query = it }, placeholder = { Text(strings.localize(Res.string.tp_search_chapters)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(20.dp)) }
        if (visible.isEmpty()) item { Text(if (chapters.isEmpty()) strings.localize(Res.string.tp_no_content) else strings.localize(Res.string.tp_no_chapters), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        items(visible, key = { it.id }) { chapter ->
            val current = chapter.id == activeChapterId
            Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=4.dp).clickable { onChapter(chapter) },
                shape=androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color=if(current) MaterialTheme.colorScheme.primaryContainer.copy(alpha=.5f) else androidx.compose.ui.graphics.Color.Transparent) {
                Row(Modifier.padding(horizontal=12.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Surface(shape=androidx.compose.foundation.shape.RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Box(Modifier.size(44.dp),contentAlignment=Alignment.Center) {
                            if(chapter.bookmark) Icon(Icons.Default.Bookmark,null,Modifier.size(18.dp))
                            else Text((positions[chapter.id] ?: 0).toString(),style=MaterialTheme.typography.labelMedium)
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(readerChapterDisplayTitle(chapter.name),maxLines=2,overflow=TextOverflow.Ellipsis,
                            style=MaterialTheme.typography.bodyMedium,fontWeight=if(current) FontWeight.SemiBold else FontWeight.Normal)
                        Text(if(current) strings.localize(Res.string.tp_continue_reading) else if(chapter.read) strings.localize(Res.string.tp_read) else strings.localize(Res.string.tp_unread),
                            style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=4.dp))
                    }
                    Icon(if(chapter.read) Icons.Default.CheckCircleOutline else Icons.Default.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    }
}
