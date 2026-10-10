package ireader.presentation.ui.truyenplus

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.discovery.*
import ireader.presentation.imageloader.IImageLoader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiscoveryScreen(state: DiscoveryState, onRefresh: () -> Unit) {
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
    val genres = remember(feed) { feed?.books.orEmpty().flatMap { it.genres }.distinct().sorted() }
    val books = remember(feed, section, genre, query) { filterDiscoveryBooks(feed, section, genre, query) }
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
                        Text("Khám phá truyện", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Bảng xếp hạng từ ba nguồn", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    }
                    if (state.refreshing) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    else IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Làm mới dữ liệu") }
                }
                ReaderChips(DiscoverySource.entries.map { it.label }, sourceIndex) { sourceIndex = it; genre = "" }
                Spacer(Modifier.height(12.dp))
                ReaderChips(DiscoverySection.entries.map { it.label }, sectionIndex) { sectionIndex = it }
                OutlinedTextField(value = query, onValueChange = { query = it }, singleLine = true,
                    placeholder = { Text("Tìm truyện, tác giả") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Outlined.Close, "Xóa tìm kiếm") } },
                    shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp))
                if (genres.isNotEmpty()) ReaderChips(listOf("Tất cả thể loại") + genres.map(::discoveryGenreLabel),
                    if (genre.isBlank()) 0 else genres.indexOf(genre) + 1) { genre = if (it == 0) "" else genres[it - 1] }
                Column(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${source.label} · ${source.board}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (feed != null) {
                        Text("Dữ liệu nguồn: ${feed.updatedTime.ifBlank { feed.updatedDate }}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${books.size} truyện · qua novel-tracker", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (section == DiscoverySection.New) Text("Truyện mới xuất hiện trên bảng, không phải ngày xuất bản.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    state.errors[source]?.let { error ->
                        Text(error, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = onRefresh, enabled = !state.refreshing) { Text("Thử lại") }
                    }
                    if (linkError) Text("Chưa mở được liên kết. Hãy kiểm tra trình duyệt trên máy.", color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.AutoStories, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when {
                    feed == null && state.refreshing -> "Đang lấy dữ liệu thật từ các nguồn…"
                    feed == null -> "Chưa có dữ liệu của ${source.label}."
                    query.isNotBlank() || genre.isNotBlank() -> "Không có truyện khớp bộ lọc."
                    section == DiscoverySection.New -> "Nguồn chưa ghi nhận truyện mới lên bảng."
                    else -> "Chưa có truyện trong mục này."
                }, style = MaterialTheme.typography.bodyMedium)
                if (feed == null && !state.refreshing && source !in state.errors) TextButton(onClick = onRefresh) { Text("Tải dữ liệu") }
                if (query.isNotBlank() || genre.isNotBlank()) TextButton(onClick = { genre = ""; query = "" }) { Text("Xóa bộ lọc") }
            }
        }
        items(books, key = { it.key }) { book ->
            DiscoveryBookCard(book, Modifier.padding(horizontal = 20.dp)) { selected = book }
        }
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text("Tên và mô tả giữ nguyên từ nguồn. Bấm truyện để xem chi tiết và trang chính thức.",
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    selected?.let { book ->
        ModalBottomSheet(onDismissRequest = { selected = null }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            DiscoveryDetailsContent(book, openUrl)
        }
    }
}

@Composable
private fun DiscoveryCover(book: DiscoveryBook, modifier: Modifier) {
    Box(modifier.clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.surfaceContainerHighest), contentAlignment = Alignment.Center) {
        Icon(Icons.Outlined.AutoStories, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        if (book.coverUrl != null) IImageLoader(model = book.coverUrl, modifier = Modifier.fillMaxSize(),
            placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest), contentScale = ContentScale.Crop,
            contentDescription = "Bìa ${book.title}", enableMemoryCache = true, enableDiskCache = true, onLoading = null)
    }
}

@Composable
private fun DiscoveryBookCard(book: DiscoveryBook, modifier: Modifier, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            DiscoveryCover(book, Modifier.size(82.dp, 116.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(book.source.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    book.rank?.let { Text("#$it", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelMedium) }
                }
                Text(book.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(book.description.ifBlank { "Nguồn chưa cung cấp mô tả." }, style = MaterialTheme.typography.bodySmall,
                    maxLines = 3, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                book.genres.firstOrNull()?.let { Text(discoveryGenreLabel(it), style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
fun DiscoveryDetailsContent(book: DiscoveryBook, onOpenUrl: (String) -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            DiscoveryCover(book, Modifier.size(108.dp, 156.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(book.author, style = MaterialTheme.typography.bodyMedium)
                Text(book.source.label + (book.rank?.let { " · Hạng $it" } ?: ""), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (book.status.isNotBlank()) Text(book.status, style = MaterialTheme.typography.bodySmall)
            }
        }
        if (book.genres.isNotEmpty()) Text(book.genres.joinToString(" · ") { discoveryGenreLabel(it) }, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { onOpenUrl(book.url) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.OpenInNew, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Xem trên ${book.source.label}")
        }
        Text("Giới thiệu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(book.description.ifBlank { "Nguồn chưa cung cấp mô tả." }, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun discoveryGenreLabel(tag: String): String = when (tag) {
    "玄幻" -> "Huyền huyễn · $tag"
    "仙侠" -> "Tiên hiệp · $tag"
    "都市" -> "Đô thị · $tag"
    "言情" -> "Ngôn tình · $tag"
    "历史" -> "Lịch sử · $tag"
    "科幻" -> "Khoa học viễn tưởng · $tag"
    "武侠" -> "Võ hiệp · $tag"
    "奇幻" -> "Kỳ ảo · $tag"
    "悬疑" -> "Bí ẩn · $tag"
    "游戏" -> "Trò chơi · $tag"
    "古代言情" -> "Ngôn tình cổ đại · $tag"
    "现代言情" -> "Ngôn tình hiện đại · $tag"
    else -> tag
}
