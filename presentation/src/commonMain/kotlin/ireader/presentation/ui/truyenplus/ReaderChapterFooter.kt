package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.i18n.resources.*

@Composable
fun ReaderChapterFooter(chapterIndex: Int, totalChapters: Int, bookmarked: Boolean, autoScroll: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, onContents: () -> Unit, onListen: () -> Unit,
    onBookmark: () -> Unit, onAutoScroll: () -> Unit, modifier: Modifier = Modifier) {
    val ink = LocalContentColor.current
    val s = requireNotNull(LocalLocalizeHelper.current)
    BoxWithConstraints(modifier.fillMaxWidth().padding(horizontal=10.dp,vertical=8.dp)) {
        val compact = maxWidth.value / androidx.compose.ui.platform.LocalDensity.current.fontScale < 360f
        val listen: @Composable () -> Unit = {
            IconButton(onClick=onListen,modifier=Modifier.size(48.dp).testTag("reader-listen")) { Icon(Icons.Default.Headphones,s.localize(Res.string.tp_listen_book),tint=ink) }
        }
        val automatic: @Composable () -> Unit = {
            TextButton(onClick=onAutoScroll,modifier=Modifier.heightIn(min=48.dp).testTag("reader-auto-scroll"),colors=ButtonDefaults.textButtonColors(contentColor=if(autoScroll) ReaderControlAccent else ink)) {
                Text(s.localize(Res.string.auto_scroll),style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
        val mark: @Composable () -> Unit = {
            IconButton(onClick=onBookmark,modifier=Modifier.size(48.dp).testTag("reader-bookmark")) { Icon(if(bookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,s.localize(Res.string.tp_bookmarks),tint=if(bookmarked) ReaderControlAccent else ink) }
        }
        val previous: @Composable () -> Unit = {
            IconButton(onClick=onPrevious,enabled=chapterIndex>0,modifier=Modifier.size(48.dp).testTag("reader-previous")) { Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_previous_chapter),tint=ink.copy(alpha=if(chapterIndex>0) 1f else .35f)) }
        }
        val contents: @Composable () -> Unit = {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                IconButton(onClick=onContents,modifier=Modifier.size(48.dp).testTag("reader-contents")) { Icon(Icons.Default.MenuBook,s.localize(Res.string.drawer),tint=ink) }
                Text(if(totalChapters>0) "${(chapterIndex+1).coerceIn(1,totalChapters)}/$totalChapters" else "—",style=MaterialTheme.typography.labelSmall)
            }
        }
        val next: @Composable () -> Unit = {
            IconButton(onClick=onNext,enabled=chapterIndex<totalChapters-1,modifier=Modifier.size(48.dp).testTag("reader-next")) { Icon(Icons.Default.ChevronRight,s.localize(Res.string.tp_next_chapter),tint=ink.copy(alpha=if(chapterIndex<totalChapters-1) 1f else .35f)) }
        }
        Surface(shape=RoundedCornerShape(22.dp),color=ink.copy(alpha=.035f),contentColor=ink,border=BorderStroke(1.dp,ink.copy(alpha=.09f))) {
        Column {
            if(compact) {
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    listen(); Box(Modifier.weight(1f),contentAlignment=Alignment.Center) { automatic() };mark()
                }
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) { previous();contents();next() }
            } else Row(Modifier.fillMaxWidth().heightIn(min=68.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
                listen();Box(Modifier.weight(1f),contentAlignment=Alignment.CenterStart) { automatic() };mark();previous();contents();next()
            }
        }
        }
    }
}
