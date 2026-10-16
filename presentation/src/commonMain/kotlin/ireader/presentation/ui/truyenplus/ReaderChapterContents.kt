package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton
import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedTextField as OutlinedTextField

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.entities.Chapter
import ireader.domain.models.shelf.shelfSearchKey
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.coroutines.*

internal fun filterReaderChapters(chapters:List<Chapter>,query:String,filter:Int):List<Chapter> {
    val key=shelfSearchKey(query)
    val tokens=key.split(' ').filter {it.isNotEmpty()}
    return chapters.filterIndexed {index,c->
        (filter!=1 || c.bookmark) && (filter!=2 || !c.read) && (tokens.isEmpty() ||
            tokens.all {it in shelfSearchKey(c.name)} ||
            key==(if(c.number>=0)c.number.toString().removeSuffix(".0") else (index+1).toString()))
    }
}

private data class ReaderTocResult(val source:List<Chapter>,val query:String,val filter:Int,val rows:List<Chapter>)

@Composable fun ReaderChapterContents(chapters:List<Chapter>,activeChapterId:Long?,onChapter:(Chapter)->Unit,
    modifier:Modifier=Modifier,onDismiss:(()->Unit)?=null,onReverse:(()->Unit)?=null,listState:LazyListState=rememberLazyListState(),book:ReaderBookUi?=null) {
    val title=localize(Res.string.tp_chapters)
    var query by rememberSaveable {mutableStateOf("")}
    var filter by rememberSaveable {mutableIntStateOf(0)}
    var reversed by rememberSaveable {mutableStateOf(false)}
    val ordered=remember(chapters,reversed){if(reversed)chapters.reversed() else chapters}
    val result by produceState(ReaderTocResult(ordered,query,filter,if(query.isBlank() && filter==0)ordered else emptyList()),ordered,query,filter) {
        value=ReaderTocResult(ordered,query,filter,if(query.isBlank() && filter==0)ordered else withContext(Dispatchers.Default){filterReaderChapters(ordered,query,filter)})
    }
    val pending=result.source!=ordered || result.query!=query || result.filter!=filter
    val visible=if(pending)emptyList() else result.rows
    val positions=remember(chapters){chapters.mapIndexed {i,c->c.id to i+1}.toMap()}
    val scope=rememberCoroutineScope()
    val keyboard=LocalSoftwareKeyboardController.current
    val currentIndex=ordered.indexOfFirst {it.id==activeChapterId}
    fun jump() {query="";filter=0;keyboard?.hide();scope.launch {
        withFrameNanos {};if(currentIndex>=0)listState.requestScrollToItem(currentIndex)
    }}
    LaunchedEffect(ordered,activeChapterId) {
        if(query.isBlank() && filter==0 && currentIndex>=0)listState.requestScrollToItem(currentIndex)
    }
    Column(modifier.fillMaxSize().semantics {paneTitle=title;isTraversalGroup=true}) {
        Row(Modifier.fillMaxWidth().padding(horizontal=24.dp,vertical=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(book!=null)ReaderCover(book,Modifier.size(44.dp,68.dp))
            Column(Modifier.weight(1f)) {
                Text(book?.title ?: title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,maxLines=2,overflow=TextOverflow.Ellipsis)
                if(book!=null)Text(localize(Res.string.tp_chapter_count,(currentIndex+1).coerceAtLeast(0),chapters.size),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick={jump()},enabled=currentIndex>=0,modifier=Modifier.size(48.dp).testTag("toc-current")){Icon(Icons.Default.MyLocation,localize(Res.string.find_current_chapter))}
            IconButton(onClick={if(onReverse==null)reversed=!reversed else onReverse()},modifier=Modifier.size(48.dp)){Icon(Icons.Default.SwapVert,localize(Res.string.reverse))}
            if(onDismiss!=null)IconButton(onDismiss,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,localize(Res.string.close))}
        }
        Box(Modifier.padding(horizontal=24.dp,vertical=8.dp)) {
            ReaderBooksSearchField(query,{query=it},localize(Res.string.tp_search_chapters),onSubmit={keyboard?.hide()})
        }
        ReaderChips(listOf(localize(Res.string.tp_chapters),localize(Res.string.tp_bookmarks),localize(Res.string.tp_unread)),filter){filter=it}
        LazyColumn(state=listState,modifier=Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(horizontal=24.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(0.dp)) {
            if(pending)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(!pending && visible.isEmpty())item {Text(localize(if(chapters.isEmpty())Res.string.tp_reading_empty_chapters else Res.string.tp_no_chapters),Modifier.padding(16.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(visible,key={it.id}) {chapter->
                ReaderChapterRow(chapter,chapter.id==activeChapterId,positions[chapter.id] ?: 0) {onChapter(chapter)}
                HorizontalDivider(color=MaterialTheme.colorScheme.onSurface.copy(alpha=.1f))
            }
        }
    }
}

@Composable private fun ReaderChapterRow(chapter:Chapter,current:Boolean,position:Int,onClick:()->Unit) {
    val currentLabel=localize(Res.string.tp_current_chapter)
    val readLabel=localize(if(chapter.read)Res.string.tp_read else Res.string.tp_unread)
    val bookmarkLabel=localize(Res.string.tp_bookmarks)
    Surface(onClick=onClick,shape=RoundedCornerShape(8.dp),color=if(current && !LocalReaderFullGlass.current)MaterialTheme.colorScheme.onSurface.copy(alpha=.08f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier=Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("toc-chapter-${chapter.id}").semantics {
            selected=current;stateDescription=listOfNotNull(if(current)currentLabel else null,readLabel,if(chapter.bookmark)bookmarkLabel else null).joinToString(" · ")
        }) {
        Row(Modifier.padding(horizontal=4.dp,vertical=14.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            if(chapter.bookmark)Icon(Icons.Default.Bookmark,null,Modifier.size(18.dp))
            Text(readerChapterDisplayTitle(chapter.name),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,
                fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis)
            Text(if(chapter.number>=0)chapter.number.toString().removeSuffix(".0") else position.toString(),
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(current)Icon(Icons.Default.VolumeUp,null,Modifier.size(16.dp))
            else if(chapter.read)Icon(Icons.Default.Check,null,Modifier.size(16.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
