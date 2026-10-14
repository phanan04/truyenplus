package ireader.presentation.ui.truyenplus

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
    modifier:Modifier=Modifier,onDismiss:(()->Unit)?=null,onReverse:(()->Unit)?=null,listState:LazyListState=rememberLazyListState()) {
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
        withFrameNanos {};if(currentIndex>=0)listState.scrollToItem(currentIndex)
    }}
    LaunchedEffect(ordered,activeChapterId) {
        if(query.isBlank() && filter==0 && currentIndex>=0)listState.scrollToItem(currentIndex)
    }
    Column(modifier.fillMaxSize().semantics {paneTitle=title;isTraversalGroup=true}) {
        Row(Modifier.fillMaxWidth().padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(title,Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            IconButton(onClick={jump()},enabled=currentIndex>=0,modifier=Modifier.size(48.dp).testTag("toc-current")){Icon(Icons.Default.MyLocation,localize(Res.string.find_current_chapter))}
            IconButton(onClick={if(onReverse==null)reversed=!reversed else onReverse()},modifier=Modifier.size(48.dp)){Icon(Icons.Default.SwapVert,localize(Res.string.reverse))}
            if(onDismiss!=null)IconButton(onDismiss,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,localize(Res.string.close))}
        }
        OutlinedTextField(query,{query=it},placeholder={Text(localize(Res.string.tp_search_chapters),maxLines=1,overflow=TextOverflow.Ellipsis)},
            leadingIcon={Icon(Icons.Default.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton({query=""}){Icon(Icons.Default.Close,localize(Res.string.clear))}},
            singleLine=true,modifier=Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=8.dp),shape=RoundedCornerShape(18.dp),
            keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(imeAction=androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions=androidx.compose.foundation.text.KeyboardActions(onSearch={keyboard?.hide()}))
        ReaderChips(listOf(localize(Res.string.tp_chapters),localize(Res.string.tp_bookmarks),localize(Res.string.tp_unread)),filter){filter=it}
        LazyColumn(state=listState,modifier=Modifier.fillMaxWidth().weight(1f),contentPadding=PaddingValues(horizontal=12.dp,vertical=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            if(pending)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(!pending && visible.isEmpty())item {Text(localize(if(chapters.isEmpty())Res.string.tp_reading_empty_chapters else Res.string.tp_no_chapters),Modifier.padding(16.dp),color=MaterialTheme.colorScheme.onSurfaceVariant)}
            items(visible,key={it.id}) {chapter->ReaderChapterRow(chapter,chapter.id==activeChapterId,positions[chapter.id] ?: 0) {onChapter(chapter)}}
        }
    }
}

@Composable private fun ReaderChapterRow(chapter:Chapter,current:Boolean,position:Int,onClick:()->Unit) {
    val currentLabel=localize(Res.string.tp_current_chapter)
    val readLabel=localize(if(chapter.read)Res.string.tp_read else Res.string.tp_unread)
    val bookmarkLabel=localize(Res.string.tp_bookmarks)
    Surface(onClick=onClick,shape=RoundedCornerShape(16.dp),color=if(current)MaterialTheme.colorScheme.onSurface.copy(alpha=.08f) else androidx.compose.ui.graphics.Color.Transparent,
        modifier=Modifier.fillMaxWidth().heightIn(min=64.dp).testTag("toc-chapter-${chapter.id}").semantics {
            selected=current;stateDescription=listOfNotNull(if(current)currentLabel else null,readLabel,if(chapter.bookmark)bookmarkLabel else null).joinToString(" · ")
        }) {
        Row(Modifier.padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Box(Modifier.widthIn(min=40.dp),contentAlignment=Alignment.Center) {
                if(chapter.bookmark)Icon(Icons.Default.Bookmark,null) else Text(if(chapter.number>=0)chapter.number.toString().removeSuffix(".0") else position.toString(),style=MaterialTheme.typography.labelMedium)
            }
            Column(Modifier.weight(1f)) {
                Text(readerChapterDisplayTitle(chapter.name),style=MaterialTheme.typography.bodyLarge,fontWeight=if(current)FontWeight.SemiBold else FontWeight.Normal,maxLines=3,overflow=TextOverflow.Ellipsis)
                Text(if(current)currentLabel else readLabel,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(if(chapter.read)Icons.Default.CheckCircleOutline else Icons.Default.ChevronRight,null,Modifier.size(20.dp))
        }
    }
}
