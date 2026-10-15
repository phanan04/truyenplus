package ireader.presentation.ui.truyenplus
import kotlinx.coroutines.flow.first

import ireader.i18n.resources.*
import ireader.i18n.asString
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
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
    val chapter: String = "", val lastRead: Long = 0, val author: String = "",
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
fun ReaderCover(book: ReaderBookUi, modifier: Modifier = Modifier,elevated:Boolean=false) {
    val shape=RoundedCornerShape(3.dp)
    val frame=if(elevated)modifier.shadow(8.dp,shape,ambientColor=Color.Black.copy(alpha=.07f),spotColor=Color.Black.copy(alpha=.16f)) else modifier
    Box(frame.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        if (book.cover != null && !book.cover.cover.isNullOrBlank()) {
            IBookImageComposable(image = book.cover, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            if(elevated)Box(Modifier.align(Alignment.CenterStart).width(4.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(Color.White.copy(alpha=.22f),Color.Black.copy(alpha=.08f),Color.Transparent))))
        } else {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                if(maxWidth<80.dp || maxHeight<96.dp) {
                    val iconSize=minOf(maxWidth,maxHeight,24.dp)
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {
                        Icon(Icons.Outlined.AutoStories,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier=Modifier.size(iconSize))
                    }
                } else Column(Modifier.fillMaxSize().padding(14.dp),verticalArrangement=Arrangement.SpaceBetween) {
                    Icon(Icons.Outlined.AutoStories,null,tint=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.size(28.dp))
                    Text(book.title,color=MaterialTheme.colorScheme.onSurface,fontWeight=FontWeight.SemiBold,
                        style=MaterialTheme.typography.titleMedium,maxLines=4,overflow=TextOverflow.Ellipsis)
                    Box(Modifier.width(26.dp).height(2.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha=.5f)))
                }
            }
        }
    }
}

@Composable
fun ReaderChips(labels: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    LazyRow(contentPadding = PaddingValues(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(labels) { index, label ->
            Surface(onClick = { onSelect(index) }, shape = CircleShape,
                modifier=if(selected==index)Modifier else Modifier.readerAppGlassCard(24),
                color = if (selected == index) MaterialTheme.colorScheme.primary else Color.Transparent,
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
private fun SectionTitle(eyebrow:String,title:String,onMore:(()->Unit)?=null) = ReaderBooksSection(title,onMore,eyebrow)

@Composable
private fun Shelf(books: List<ReaderBookUi>, onBook: (Long) -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    LazyRow(contentPadding = PaddingValues(horizontal = ReaderBooksTokens.gutter), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        items(books, key = { it.id }) { book ->
            Column(Modifier.width((156f * LocalDensity.current.fontScale.coerceIn(1f,1.5f)).dp).clickable { onBook(book.id) }.semantics {contentDescription=book.title}) {
                ReaderCover(book, Modifier.fillMaxWidth().aspectRatio(.64f).testTag("book-cover-${book.id}"),elevated=true)
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(book.total>0)"${book.progress.percent}%" else strings.localize(Res.string.tp_library_ebook),Modifier.weight(1f),
                        color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                    IconButton(onClick={onBook(book.id)},Modifier.size(48.dp)) {Icon(Icons.Outlined.MoreHoriz,strings.localize(Res.string.tp_book_details))}
                }
            }
        }
    }
}

@Composable
fun ReaderEmptyLibrary(onImport: () -> Unit, modifier: Modifier = Modifier) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    Column(modifier.fillMaxWidth().padding(top=60.dp,bottom=24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(strings.localize(Res.string.tp_first_story),style=ReaderBooksTokens.section,color=MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign=androidx.compose.ui.text.style.TextAlign.Center)
        Text(strings.localize(Res.string.tp_first_story_hint),style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign=androidx.compose.ui.text.style.TextAlign.Center,modifier=Modifier.padding(horizontal=24.dp).padding(top=14.dp))
        TextButton(onClick=onImport,modifier=Modifier.padding(top=12.dp).heightIn(min=48.dp)) {
            Text(strings.localize(Res.string.tp_import_book))
        }
    }

}

@Composable
fun ReaderHomeScreen(books: List<ReaderBookUi>, isLoading: Boolean, onBook: (Long) -> Unit, onLibrary: () -> Unit, onImport: () -> Unit, onSearch: () -> Unit, onUpdate: (() -> Unit)? = null, onContinue:(Long)->Unit=onBook, onShelf:(()->Unit)?=null,resumeBook:ReaderBookUi?=null,onProgress:(()->Unit)?=null,onSettings:(()->Unit)?=null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val current = resumeBook ?: remember(books) { books.filter { it.read > 0 && (it.total == 0 || it.read < it.total) }.maxByOrNull { it.lastRead } }
    val pinned = remember(books,current) { books.filter { it.pinned && it.id != current?.id } }
    val remaining = remember(books,current,pinned) { books.filter { it.id != current?.id && !it.pinned }.take(12) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalReaderDockInset.current)) {
        item {
            val c=MaterialTheme.colorScheme
            Column(Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(c.background,lerp(c.background,c.onBackground,.035f)))).testTag("books-home-opening")) {
                ReaderBooksHeader(strings.localize(Res.string.tp_home),onSettings)
                current?.let {book->
                    ReaderBooksSection(strings.localize(Res.string.tp_continue_reading),topPadding=14.dp,bottomPadding=5.dp)
                    ReaderContinueBook(book,onRead={onContinue(book.id)},onDetails={onBook(book.id)})
                    Spacer(Modifier.height(32.dp))
                }
            }
        }
        if (isLoading && books.isEmpty() && current==null) item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(24.dp)) }
        else if (books.isEmpty() && current==null) item { ReaderEmptyLibrary(onImport, Modifier.padding(top = 40.dp)) }
        else {
            if(pinned.isNotEmpty()) {
                item { SectionTitle("",strings.localize(Res.string.tp_pinned_books),onLibrary) }
                item { Shelf(pinned,onBook) }
            }
            if(remaining.isNotEmpty()) {
                item { SectionTitle("",strings.localize(Res.string.tp_your_library),onLibrary) }
                item { Shelf(remaining,onBook) }
            }
            if(onProgress!=null)item {
                HorizontalDivider(Modifier.padding(top=24.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.06f))
                ReaderHomeReadingActivity(current,onProgress)

            }

        }
    }
}

/** Uses real chapter progress; it does not invent a daily reading goal. */
@Composable private fun ReaderHomeReadingActivity(book:ReaderBookUi?,onProgress:()->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    val ink=MaterialTheme.colorScheme.onSurface
    Column(Modifier.fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter,vertical=32.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(s.localize(Res.string.tp_reading_activity),style=ReaderBooksTokens.section)
        if(book!=null)Text(book.title,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis,modifier=Modifier.padding(top=6.dp))
        Box(Modifier.padding(top=24.dp).size(232.dp).clickable(onClick=onProgress).testTag("home-progress"),contentAlignment=Alignment.Center) {
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val stroke=androidx.compose.ui.graphics.drawscope.Stroke(width=4.dp.toPx(),cap=StrokeCap.Round)
                drawCircle(ink.copy(alpha=.07f),style=stroke)
                if(book!=null && book.total>0)drawArc(Color(0xFF52ACD9),-90f,360f*book.progress.fraction,false,style=stroke)
            }
            Column(Modifier.padding(24.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                Text(s.localize(Res.string.tp_progress),style=MaterialTheme.typography.bodyMedium,fontWeight=FontWeight.SemiBold)
                Text(if(book!=null && book.total>0)"${book.progress.percent}%" else "—",style=ReaderBooksTokens.title.copy(fontSize=48.sp,lineHeight=58.sp),modifier=Modifier.padding(top=8.dp))
                if(book!=null && book.total>0)Text(s.localize(Res.string.tp_chapter_count,book.read,book.total),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Button(onProgress,modifier=Modifier.padding(top=20.dp).heightIn(min=48.dp),shape=CircleShape,contentPadding=PaddingValues(horizontal=28.dp)) {Text(s.localize(Res.string.tp_progress_setting_hint))}
    }
}

@Composable private fun ReaderContinueBook(book:ReaderBookUi,onRead:()->Unit,onDetails:()->Unit) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    val artwork=rememberReaderBookArtwork(book)
    val ink=if(artwork!=null)Color.White else MaterialTheme.colorScheme.onSurface
    val scale=LocalDensity.current.fontScale
    Box(Modifier.fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter)) {
        Surface(onClick=onRead,modifier=Modifier.widthIn(max=(244f*scale.coerceIn(1f,1.35f)).dp).fillMaxWidth()
            .testTag("home-continue"),shape=RoundedCornerShape(10.dp),color=artwork ?: MaterialTheme.colorScheme.surfaceContainerHigh,contentColor=ink) {
            Box {
                Row(Modifier.padding(10.dp).heightIn(min=60.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    ReaderCover(book,Modifier.width(38.dp).height(60.dp))
                    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(1.dp)) {
                        Text(book.title,style=MaterialTheme.typography.bodyLarge.copy(fontWeight=FontWeight.SemiBold,lineHeight=20.sp),maxLines=1,overflow=TextOverflow.Ellipsis)
                        val detail=book.author.ifBlank {readerChapterDisplayTitle(book.chapter)}
                        if(detail.isNotBlank())Text(detail,style=MaterialTheme.typography.bodyMedium.copy(lineHeight=18.sp),maxLines=1,overflow=TextOverflow.Ellipsis,color=ink.copy(alpha=.8f))
                        if(book.total>0)Text("${book.progress.percent}%",style=MaterialTheme.typography.bodySmall.copy(fontSize=13.sp,lineHeight=16.sp),color=ink.copy(alpha=.7f))
                    }
                    Spacer(Modifier.width(24.dp))
                }
                IconButton(onDetails,Modifier.align(Alignment.CenterEnd).size(48.dp)) {Icon(Icons.Outlined.MoreHoriz,strings.localize(Res.string.tp_book_details),tint=ink)}
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
    requestSearch:Boolean=false,onSearchFocused:()->Unit={},onShelf:(()->Unit)?=null,sort:ireader.domain.models.library.LibrarySort?=null,
    preferList:Boolean=false,onLayout:(Boolean)->Unit={},recentSearches:List<String> = emptyList(),onSearchSubmitted:(String)->Unit={},onClearSearchHistory:()->Unit={},
    searchMode:Boolean=requestSearch || query.isNotBlank(),onSettings:(()->Unit)?=null,onClearSelection:()->Unit={},onDetails:((Long)->Unit)?=null,showSearchField:Boolean=true) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val listMode=preferList || LocalDensity.current.fontScale>=1.3f || LocalReaderViewport.current.width<320.dp
    var editing by remember {mutableStateOf(false)}
    val gridState=rememberLazyGridState()
    val searchFocus=remember { androidx.compose.ui.focus.FocusRequester() }
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(requestSearch) { if(requestSearch) { gridState.requestScrollToItem(0);snapshotFlow {gridState.layoutInfo.visibleItemsInfo.any {it.index==0}}.first {it};androidx.compose.runtime.withFrameNanos {};androidx.compose.runtime.withFrameNanos {};searchFocus.requestFocus();keyboard?.show();onSearchFocused() } }
    LazyVerticalGrid(state=gridState, columns = if(listMode) GridCells.Fixed(1) else GridCells.Adaptive(132.dp), modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = ReaderBooksTokens.gutter, end = ReaderBooksTokens.gutter, bottom = LocalReaderDockInset.current), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item(span={GridItemSpan(maxLineSpan)}) {
            if(!searchMode)Column {
                var organizeOpen by remember {mutableStateOf(false)}
                ReaderBooksHeader(categories.getOrNull(categoryIndex)?.takeIf {categoryIndex>0 && it.isNotBlank()} ?: strings.localize(Res.string.tp_library),horizontalPadding=0.dp,actions={
                    if(editing || selected.isNotEmpty())TextButton(onClick={editing=false;onClearSelection()},modifier=Modifier.heightIn(min=48.dp).testTag("library-edit")) {Text(strings.localize(Res.string.done))}
                    else {
                        ReaderCollectionPicker(categories,categoryIndex,onCategory,onCollections,iconOnly=true)
                        Box {
                            IconButton(onClick={organizeOpen=true},modifier=Modifier.padding(start=8.dp).size(48.dp).shadow(4.dp,CircleShape).background(MaterialTheme.colorScheme.surface,CircleShape).testTag("library-organize")) {Icon(Icons.Outlined.MoreHoriz,strings.localize(Res.string.more))}
                            DropdownMenu(organizeOpen,{organizeOpen=false}) {
                                DropdownMenuItem(text={Text(strings.localize(Res.string.select))},modifier=Modifier.heightIn(min=56.dp).testTag("library-edit"),onClick={organizeOpen=false;editing=true},leadingIcon={Icon(Icons.Default.CheckCircleOutline,null)})
                                DropdownMenuItem(text={Text(strings.localize(Res.string.tp_sort_books))},modifier=Modifier.heightIn(min=56.dp),onClick={organizeOpen=false;onSort()},leadingIcon={Icon(Icons.Default.SortByAlpha,null)})
                                DropdownMenuItem(text={Text(strings.localize(if(preferList)Res.string.grid else Res.string.list))},modifier=Modifier.heightIn(min=56.dp).testTag("library-layout"),onClick={organizeOpen=false;onLayout(!preferList)},leadingIcon={Icon(if(preferList)Icons.Default.GridView else Icons.Default.ViewList,null)})
                                DropdownMenuItem(text={Text(strings.localize(Res.string.tp_import_ebook))},modifier=Modifier.heightIn(min=56.dp),onClick={organizeOpen=false;onImport()},leadingIcon={Icon(Icons.Outlined.Add,null)})
                                if(onSettings!=null)DropdownMenuItem(text={Text(strings.localize(Res.string.tp_settings))},modifier=Modifier.heightIn(min=56.dp).testTag("books-account"),onClick={organizeOpen=false;onSettings()},leadingIcon={Icon(Icons.Outlined.AccountCircle,null)})
                            }
                        }
                    }
                })
            } else if(showSearchField)Column(Modifier.padding(top=12.dp,bottom=8.dp)) {
                ReaderBooksSearchField(query,onQuery,strings.localize(Res.string.tp_search_query_hint),searchFocus,onSearchSubmitted)
            }
        }
        if(query.isBlank() && recentSearches.isNotEmpty())item(span={GridItemSpan(maxLineSpan)}) {ReaderRecentSearches(recentSearches,{onQuery(it);onSearchSubmitted(it);keyboard?.hide()},onClearSearchHistory)}
        if (isLoading && books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        else if (books.isEmpty()) item(span = { GridItemSpan(maxLineSpan) }) {
            if (query.isBlank() && !searchMode) ReaderEmptyLibrary(onImport) else Text(strings.localize(if(query.isBlank())Res.string.tp_search_prompt else Res.string.tp_no_books_found), modifier = Modifier.padding(vertical = 30.dp))
        }
        items(if(searchMode && query.isBlank())emptyList() else books, key = { it.id }) { book ->
            val action=Modifier.combinedClickable(onClick={if(editing || selected.isNotEmpty())onSelect(book.id) else onBook(book.id)},
                onLongClick={onSelect(book.id)}).semantics {this.selected=book.id in selected;testTag="library-book-${book.id}";contentDescription=book.title+if(book.author.isBlank())"" else ", "+book.author}
            if(listMode) Row(action.fillMaxWidth().padding(vertical=8.dp),verticalAlignment=Alignment.CenterVertically,
                horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                ReaderCover(book,Modifier.size(54.dp,76.dp).then(if(book.id in selected)Modifier.border(2.dp,ReaderControlAccent,RoundedCornerShape(6.dp)) else Modifier))
                Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                    Text(book.title,maxLines=3,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold)
                    if(book.author.isNotBlank())Text(book.author,maxLines=2,overflow=TextOverflow.Ellipsis,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(strings.localize(Res.string.tp_chapter_count,book.read,book.total),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if(book.id in selected)Icon(Icons.Default.CheckCircle,strings.localize(Res.string.tp_selected),tint=ReaderControlAccent)
                else if(onDetails!=null)IconButton(onClick={onDetails(book.id)},modifier=Modifier.size(48.dp).semantics {testTag="library-details-${book.id}"}) {Icon(Icons.Outlined.MoreHoriz,strings.localize(Res.string.tp_book_details))}
            } else Column(action) {
                Box {
                    ReaderCover(book,Modifier.fillMaxWidth().aspectRatio(.64f).testTag("book-cover-${book.id}").then(if(book.id in selected)Modifier.border(3.dp,ReaderControlAccent,RoundedCornerShape(12.dp)) else Modifier),elevated=true)
                    if(book.id in selected)Icon(Icons.Default.CheckCircle,strings.localize(Res.string.tp_selected),tint=ReaderControlAccent,modifier=Modifier.align(Alignment.TopEnd).padding(8.dp))
                }
                Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(book.total>0)"${book.progress.percent}%" else strings.localize(Res.string.tp_library_ebook),Modifier.weight(1f),
                        style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(onDetails!=null)IconButton(onClick={onDetails(book.id)},modifier=Modifier.size(48.dp).testTag("library-details-${book.id}")) {Icon(Icons.Outlined.MoreHoriz,strings.localize(Res.string.tp_book_details))}
                }

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
            if(isPlaying==null)IconButton(onClick=onListen) {Icon(Icons.Outlined.Headphones,strings.localize(Res.string.tp_continue_listening))}
            IconButton(onClick=if(isPlaying==null)onRead else onTogglePlayback ?: {},enabled=isPlaying==null || onTogglePlayback!=null) { Icon(if(isPlaying==null)Icons.Outlined.MenuBook else if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlaying == null) strings.localize(Res.string.tp_continue_reading) else if (isPlaying) strings.localize(Res.string.tp_pause) else strings.localize(Res.string.tp_resume)) }
        }
    }
}

@Composable
fun ReaderProgressScreen(stats: ReadingStatisticsType1, books: List<ReaderBookUi>, history: List<ReaderHistoryUi>, onBook: (Long) -> Unit, onHistory: (ReaderHistoryUi) -> Unit, onMoreHistory: () -> Unit,showHeading:Boolean=true) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    var tab by rememberSaveable { mutableIntStateOf(0) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = LocalReaderDockInset.current)) {
        if(showHeading)item { Column(Modifier.padding(20.dp)) { Text(strings.localize(Res.string.tp_reading_journey), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) } }
        item { ReaderChips(listOf(strings.localize(Res.string.tp_overview), strings.localize(Res.string.tp_history)), tab) { tab = it } }
        if (tab == 0) {
            item {
                Surface(shape = RoundedCornerShape(24.dp), color = Color.Transparent, modifier = Modifier.fillMaxWidth().padding(20.dp).readerAppGlassCard(24)) {
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
                            Text(strings.localize(Res.string.tp_book_percent, book.read, book.total, "${book.progress.percent}%"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 6.dp))
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
