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
    ReaderAppBackdrop {
    if (showMetadataChoice) ReaderGlassDialog(onDismissRequest = { showMetadataChoice = false },
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
                        onListen = { resume?.let { nav.navigateTo(readerListeningDestination(bookId, it.id)) } },
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
    var expandedSummary by rememberSaveable { mutableStateOf(false) }
    var contentsOpen by rememberSaveable {mutableStateOf(false)}
    val summary:LazyListScope.()->Unit={
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).readerAppGlassCard(28), verticalAlignment = Alignment.CenterVertically) {
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
            Surface(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=8.dp).readerAppGlassCard(20),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                color=androidx.compose.ui.graphics.Color.Transparent) {
                Column(Modifier.padding(horizontal=16.dp,vertical=16.dp)) {
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
        item {
            OutlinedButton(onClick={contentsOpen=true},enabled=chapters.isNotEmpty(),modifier=Modifier.fillMaxWidth().padding(20.dp).heightIn(min=52.dp).testTag("book-contents")) {
                Icon(Icons.Default.FormatListBulleted,null);Spacer(Modifier.width(12.dp));Text("${strings.localize(Res.string.tp_chapters)} · ${chapters.size}")
            }
        }
    }
    ReaderContentPane(maxWidth=1120.dp) {
        BoxWithConstraints {
            val supporting=maxWidth.value / androidx.compose.ui.platform.LocalDensity.current.fontScale>=840f
            if(supporting)Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                LazyColumn(Modifier.weight(.46f).fillMaxHeight(),contentPadding=PaddingValues(bottom=24.dp),content=summary)
                Box(Modifier.weight(.54f).fillMaxHeight().readerAppGlassCard(24)) {ReaderChapterContents(chapters,activeChapterId,onChapter)}
            } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=32.dp),content=summary)
        }
    }
    if(contentsOpen)ReaderGlassSheet(onDismissRequest={contentsOpen=false}) {
        Box(Modifier.fillMaxWidth().heightIn(max=LocalReaderViewport.current.height*.82f)) {
            ReaderChapterContents(chapters,activeChapterId,{contentsOpen=false;onChapter(it)},onDismiss={contentsOpen=false})
        }
    }
}
