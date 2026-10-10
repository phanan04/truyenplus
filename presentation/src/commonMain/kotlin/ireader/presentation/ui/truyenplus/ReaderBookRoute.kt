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

@Composable
fun ReaderBookRoute(bookId: Long) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val nav = requireNotNull(LocalNavigator.current)
    val vm: BookDetailViewModel = koinViewModel(parameters = { parametersOf(BookDetailViewModel.Param(bookId)) })
    val state by vm.state.collectAsState()
    val snackbars = remember { SnackbarHostState() }
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
                    val chapters = current.chapters.sortedBy { it.number }
                    val resume = chapters.firstOrNull { it.id == current.lastReadChapterId }
                        ?: chapters.firstOrNull { !it.read } ?: chapters.firstOrNull()
                    ReaderBookScreen(
                        book = ReaderBookUi(current.book.id, current.book.title, BookCover.from(current.book), chapters.count { it.read }, chapters.size),
                        author = current.book.author.orEmpty(), chapters = chapters, onBack = { nav.safePopBackStack() },
                        onRead = { resume?.let { nav.navigateTo(ReaderScreenSpec(bookId, it.id)) } },
                        onListen = { resume?.let { nav.navigateTo(TTSV2ScreenSpec(bookId, it.id, current.book.sourceId, 0)) } },
                        onChapter = { nav.navigateTo(ReaderScreenSpec(bookId, it.id)) },
                        onExport = { vm.showEpubExportDialog = true })
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
    onRead: () -> Unit, onListen: () -> Unit, onChapter: (Chapter) -> Unit, onExport: () -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = chapters.filter { (filter != 1 || it.bookmark) && (filter != 2 || !it.read) && it.name.contains(query, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, strings.localize(Res.string.tp_back)) }
                Text(strings.localize(Res.string.tp_in_library), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onExport) { Icon(Icons.Outlined.IosShare, strings.localize(Res.string.tp_export_epub)) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ReaderCover(book, Modifier.widthIn(max = 198.dp).fillMaxWidth().height(270.dp))
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                if (author.isNotBlank()) Text(author, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Text(strings.localize(Res.string.tp_book_summary, book.total, book.progress.percent), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRead, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Outlined.AutoStories, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (book.read > 0) strings.localize(Res.string.tp_continue_reading) else strings.localize(Res.string.tp_start_reading))
                    }
                    OutlinedButton(onClick = onListen, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.height(52.dp)) { Icon(Icons.Outlined.Headphones, strings.localize(Res.string.tp_listen_book)) }
                }
            }
        }
        item { ReaderChips(listOf(strings.localize(Res.string.tp_chapters), strings.localize(Res.string.tp_bookmarks), strings.localize(Res.string.tp_unread)), filter) { filter = it } }
        item { OutlinedTextField(query, { query = it }, placeholder = { Text(strings.localize(Res.string.tp_search_chapters)) }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(20.dp)) }
        if (visible.isEmpty()) item { Text(if (chapters.isEmpty()) strings.localize(Res.string.tp_no_content) else strings.localize(Res.string.tp_no_chapters), color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        items(visible, key = { it.id }) { chapter ->
            ListItem(headlineContent = { Text(chapter.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(if (chapter.read) strings.localize(Res.string.tp_read) else strings.localize(Res.string.tp_unread)) },
                leadingContent = { Icon(if (chapter.bookmark) Icons.Filled.Bookmark else Icons.Outlined.Description, null, tint = if (chapter.bookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onChapter(chapter) })
        }
    }
}
