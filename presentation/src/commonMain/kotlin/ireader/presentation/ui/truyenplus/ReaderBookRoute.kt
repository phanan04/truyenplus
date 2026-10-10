package ireader.presentation.ui.truyenplus

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
                    Text("Không mở được sách.")
                    TextButton(onClick = { nav.safePopBackStack() }) { Text("Về thư viện") }
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
    var filter by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    val visible = chapters.filter { (filter != 1 || it.bookmark) && (filter != 2 || !it.read) && it.name.contains(query, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(Icons.Default.ChevronLeft, "Quay lại") }
                Text("TRONG THƯ VIỆN", style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = onExport) { Icon(Icons.Outlined.IosShare, "Xuất ebook EPUB") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ReaderCover(book, Modifier.widthIn(max = 198.dp).fillMaxWidth().height(270.dp))
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 24.dp))
                if (author.isNotBlank()) Text(author, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                Text("${book.total} chương · ${book.progress.percent}% đã đọc", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onRead, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Outlined.AutoStories, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text(if (book.read > 0) "Đọc tiếp" else "Bắt đầu đọc")
                    }
                    OutlinedButton(onClick = onListen, enabled = chapters.isNotEmpty(), shape = CircleShape, modifier = Modifier.height(52.dp)) { Icon(Icons.Outlined.Headphones, "Nghe truyện") }
                }
            }
        }
        item { ReaderChips(listOf("Chương", "Đánh dấu", "Chưa đọc"), filter) { filter = it } }
        item { OutlinedTextField(query, { query = it }, placeholder = { Text("Tìm chương") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(20.dp)) }
        if (visible.isEmpty()) item { Text(if (chapters.isEmpty()) "Sách chưa có nội dung. Hãy nhập ebook có chương." else "Không có chương trong mục này.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(24.dp)) }
        items(visible, key = { it.id }) { chapter ->
            ListItem(headlineContent = { Text(chapter.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                supportingContent = { Text(if (chapter.read) "Đã đọc" else "Chưa đọc") },
                leadingContent = { Icon(if (chapter.bookmark) Icons.Filled.Bookmark else Icons.Outlined.Description, null, tint = if (chapter.bookmark) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) },
                trailingContent = { Icon(Icons.Default.ChevronRight, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onChapter(chapter) })
        }
    }
}
