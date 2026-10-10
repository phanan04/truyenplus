package ireader.presentation.ui.truyenplus

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
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), content = content)
    }
}

@Composable
fun ReaderCover(book: ReaderBookUi, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (book.cover != null && !book.cover.cover.isNullOrBlank()) {
            IBookImageComposable(image = book.cover, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Column(Modifier.fillMaxSize().padding(14.dp), verticalArrangement = Arrangement.SpaceBetween) {
                Icon(Icons.Outlined.AutoStories, null, tint = Color.White.copy(alpha = .55f), modifier = Modifier.size(28.dp))
                Text(book.title, color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 4, overflow = TextOverflow.Ellipsis)
                Box(Modifier.width(26.dp).height(2.dp).background(Color.White.copy(alpha = .6f)))
            }
        }
    }
}

@Composable
private fun PageHeader(title: String, subtitle: String, onImport: () -> Unit, onSearch: () -> Unit, onUpdate: (() -> Unit)? = null) {
    var showMenu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 4.dp))
        }
        IconButton(onClick = onSearch) { Icon(Icons.Outlined.Search, "Tìm trong thư viện") }
        IconButton(onClick = onImport) { Icon(Icons.Outlined.Add, "Nhập ebook") }
        if (onUpdate != null) Box {
            IconButton(onClick = { showMenu = true }) { Icon(Icons.Outlined.MoreHoriz, "Tùy chọn ứng dụng") }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(text = { Text("Cập nhật ứng dụng") }, leadingIcon = { Icon(Icons.Outlined.SystemUpdate, null) },
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
                color = if (selected == index) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
                border = BorderStroke(1.dp, if (selected == index) Color.Transparent else MaterialTheme.colorScheme.outlineVariant)) {
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
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 28.dp, bottom = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(eyebrow.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
        if (onMore != null) IconButton(onClick = onMore) { Icon(Icons.Default.ChevronRight, "Xem tất cả") }
    }
}

@Composable
private fun Shelf(books: List<ReaderBookUi>, onBook: (Long) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        items(books, key = { it.id }) { book ->
            Column(Modifier.width(146.dp).clickable { onBook(book.id) }) {
                ReaderCover(book, Modifier.fillMaxWidth().height(196.dp))
                Text(book.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 10.dp))
                Text(if (book.total > 0) "${book.read}/${book.total} chương" else "Ebook trong thư viện",
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                if (book.read > 0) LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clip(CircleShape), drawStopIndicator = {})
            }
        }
    }
}

@Composable
fun ReaderEmptyLibrary(onImport: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.primary.copy(alpha = .12f)) {
            Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(28.dp).size(58.dp))
        }
        Text("Câu chuyện đầu tiên của bạn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 22.dp))
        Text("Thêm EPUB hoặc PDF để bắt đầu đọc và nghe.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 10.dp, bottom = 24.dp))
        Button(onClick = onImport, shape = CircleShape, contentPadding = PaddingValues(horizontal = 26.dp, vertical = 14.dp)) {
            Icon(Icons.Default.Add, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Nhập sách")
        }
    }
}

@Composable
fun ReaderHomeScreen(books: List<ReaderBookUi>, isLoading: Boolean, onBook: (Long) -> Unit, onLibrary: () -> Unit, onImport: () -> Unit, onSearch: () -> Unit, onUpdate: (() -> Unit)? = null) {
    var filter by rememberSaveable { mutableIntStateOf(0) }
    val visible = when (filter) { 1 -> books.filter { it.read > 0 && it.read < it.total }; 2 -> books.filter { it.read == 0 }; else -> books }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 175.dp)) {
        item { PageHeader("Truyện+", "Không gian đọc của bạn", onImport, onSearch, onUpdate) }
        item { ReaderChips(listOf("Tất cả", "Đang đọc", "Chưa đọc"), filter) { filter = it } }
        if (isLoading && books.isEmpty()) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp)) }
        else if (books.isEmpty()) item { ReaderEmptyLibrary(onImport, Modifier.padding(top = 55.dp)) }
        else {
            val reading = visible.filter { it.read > 0 }.sortedByDescending { it.lastRead }
            val shelfBooks = if (reading.isEmpty()) visible else reading
            val shelfTitle = when {
                filter == 2 -> "Chưa đọc"
                filter == 0 && reading.isEmpty() -> "Bắt đầu đọc"
                else -> "Đọc tiếp"
            }
            item { SectionTitle("CÂU CHUYỆN CỦA BẠN", shelfTitle, onLibrary) }
            if (shelfBooks.isNotEmpty()) item { Shelf(shelfBooks.take(12), onBook) }
            else item { Text("Chưa có sách trong mục này.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
            item {
                Surface(onClick = onImport, color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = .7f), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Outlined.CreateNewFolder, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Column(Modifier.weight(1f)) { Text("Mang sách của bạn vào đây", fontWeight = FontWeight.SemiBold); Text("Nhập EPUB · PDF", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                        Icon(Icons.Default.ChevronRight, null)
                    }
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
    onCollections: () -> Unit, onSort: () -> Unit, onLoadMore: () -> Unit) {
    LazyVerticalGrid(columns = GridCells.Adaptive(140.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 175.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.padding(top = 18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) { Text("Thư viện của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Sách, bộ sưu tập và những thế giới riêng.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) }
                    IconButton(onClick = onImport) { Icon(Icons.Default.Add, "Nhập ebook") }
                }
                OutlinedTextField(query, onQuery, modifier = Modifier.fillMaxWidth().padding(top = 16.dp), singleLine = true,
                    placeholder = { Text("Tìm sách trong thư viện") }, leadingIcon = { Icon(Icons.Outlined.Search, null) }, shape = CircleShape)
            }
        }
        if (categories.isNotEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.offset(x = (-20).dp).fillMaxWidth()) { ReaderChips(categories, categoryIndex, onCategory) }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (selected.isEmpty()) "${books.size} sách đang hiển thị" else "Đã chọn ${selected.size} sách", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onCollections) { Icon(Icons.Outlined.Folder, "Quản lý bộ sưu tập") }
                IconButton(onClick = onSort) { Icon(Icons.Default.SortByAlpha, "Đổi thứ tự sách") }
            }
        }
        if (isLoading && books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            if (query.isBlank()) ReaderEmptyLibrary(onImport) else Text("Không tìm thấy sách phù hợp.", modifier = Modifier.padding(vertical = 30.dp))
        }
        items(books, key = { it.id }) { book ->
            Column(Modifier.combinedClickable(onClick = { if (selected.isNotEmpty()) onSelect(book.id) else onBook(book.id) }, onLongClick = { onSelect(book.id) })) {
                Box {
                    ReaderCover(book, Modifier.fillMaxWidth().aspectRatio(.73f).then(if (book.id in selected) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)) else Modifier))
                    if (book.id in selected) Icon(Icons.Default.CheckCircle, "Đã chọn", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                Text(book.title, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 10.dp))
                Text("${book.read}/${book.total} chương", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(3.dp).clip(CircleShape), drawStopIndicator = {})
            }
        }
        if (hasMore && books.isNotEmpty() && query.isBlank()) item(span = { GridItemSpan(maxLineSpan) }) { TextButton(onClick = onLoadMore, modifier = Modifier.fillMaxWidth()) { Text("Hiển thị thêm") } }
    }
}

@Composable
fun ReaderBottomNav(selected: ReaderDestination, onSelect: (ReaderDestination) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf(ReaderDestination.Home, ReaderDestination.Discover, ReaderDestination.Library, ReaderDestination.Progress).forEach { destination ->
                val active = selected == destination
                val icon = when (destination) { ReaderDestination.Home -> Icons.Outlined.Home; ReaderDestination.Library -> Icons.Outlined.LibraryBooks; ReaderDestination.Progress -> Icons.Outlined.BarChart; ReaderDestination.Discover -> Icons.Outlined.Explore }
                Surface(onClick = { onSelect(destination) }, modifier = Modifier.weight(1f), shape = CircleShape, color = if (active) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent) {
                    Column(Modifier.padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(icon, destination.label, tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
                        Text(destination.label, style = MaterialTheme.typography.labelSmall, color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderMiniPlayer(book: ReaderBookUi, onRead: () -> Unit, onListen: () -> Unit, isPlaying: Boolean? = null, onTogglePlayback: (() -> Unit)? = null) {
    Surface(onClick = onRead, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ReaderCover(book, Modifier.size(width = 38.dp, height = 50.dp))
            Column(Modifier.weight(1f)) {
                Text(book.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.chapter.ifBlank { "Tiếp tục câu chuyện" }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onListen) { Icon(Icons.Outlined.Headphones, "Nghe tiếp", tint = MaterialTheme.colorScheme.primary) }
            IconButton(onClick = onTogglePlayback ?: onRead) { Icon(if (isPlaying == true) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying == null) "Đọc tiếp" else if (isPlaying) "Tạm dừng" else "Phát tiếp") }
        }
    }
}

@Composable
fun ReaderProgressScreen(stats: ReadingStatisticsType1, books: List<ReaderBookUi>, history: List<ReaderHistoryUi>, onBook: (Long) -> Unit, onHistory: (ReaderHistoryUi) -> Unit, onMoreHistory: () -> Unit) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 175.dp)) {
        item { Column(Modifier.padding(20.dp)) { Text("Hành trình đọc", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Những trang sách bạn đã đi qua.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp)) } }
        item { ReaderChips(listOf("Tổng quan", "Lịch sử"), tab) { tab = it } }
        if (tab == 0) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Column(Modifier.padding(24.dp)) {
                        Icon(Icons.Outlined.LocalFireDepartment, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                        Text("${stats.readingStreak} ngày", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
                        Text("Chuỗi ngày đọc liên tiếp", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(Modifier.fillMaxWidth().padding(top = 24.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Metric("${stats.totalReadingTimeMinutes / 60}h ${stats.totalReadingTimeMinutes % 60}m", "Thời gian đọc", Modifier.weight(1f))
                            Metric("${stats.totalChaptersRead}", "Chương đã đọc", Modifier.weight(1f))
                            Metric("${stats.booksCompleted}", "Sách hoàn thành", Modifier.weight(1f))
                        }
                    }
                }
            }
            item { SectionTitle("TIẾN ĐỘ SÁCH", "Bạn đang đọc") }
            val reading = books.filter { it.read > 0 && it.read < it.total }
            if (reading.isEmpty()) item { Text("Tiến độ sẽ hiện khi bạn bắt đầu đọc sách.", modifier = Modifier.padding(horizontal = 20.dp), color = MaterialTheme.colorScheme.onSurfaceVariant) }
            items(reading, key = { it.id }) { book ->
                Surface(onClick = { onBook(book.id) }, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface.copy(alpha = .8f), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 5.dp)) {
                    Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        ReaderCover(book, Modifier.size(width = 48.dp, height = 66.dp))
                        Column(Modifier.weight(1f)) {
                            Text(book.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${book.read}/${book.total} chương · ${book.progress.percent}%", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
                            LinearProgressIndicator(progress = { book.progress.fraction }, modifier = Modifier.fillMaxWidth().height(3.dp), drawStopIndicator = {})
                        }
                        Icon(Icons.Default.ChevronRight, null)
                    }
                }
            }
        } else {
            item { SectionTitle("ĐỌC GẦN ĐÂY", "Lịch sử của bạn") }
            if (history.isEmpty()) item { Text("Chưa có lịch sử đọc.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(20.dp)) }
            items(history, key = { it.id }) { entry ->
                ListItem(headlineContent = { Text(entry.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }, supportingContent = { Text(entry.chapter, maxLines = 2, overflow = TextOverflow.Ellipsis) }, leadingContent = { Icon(Icons.Outlined.History, null) }, trailingContent = { Icon(Icons.Default.PlayArrow, null) }, colors = ListItemDefaults.colors(containerColor = Color.Transparent), modifier = Modifier.clickable { onHistory(entry) })
            }
            if (history.isNotEmpty()) item { TextButton(onClick = onMoreHistory, modifier = Modifier.fillMaxWidth()) { Text("Xem thêm lịch sử") } }
        }
    }
}

data class ReaderHistoryUi(val id: Long, val bookId: Long, val chapterId: Long, val title: String, val chapter: String)
@Composable private fun Metric(value: String, label: String, modifier: Modifier = Modifier) { Column(modifier) { Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold); Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp)) } }
