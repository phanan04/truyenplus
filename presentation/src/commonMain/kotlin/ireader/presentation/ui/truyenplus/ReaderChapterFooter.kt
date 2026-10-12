package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
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
    HorizontalDivider(color = ink.copy(alpha=.08f))
    Row(modifier.fillMaxWidth().heightIn(min=68.dp).padding(horizontal=8.dp),verticalAlignment=Alignment.CenterVertically) {
        IconButton(onClick=onListen,modifier=Modifier.testTag("reader-listen")) { Icon(Icons.Default.Headphones,s.localize(Res.string.tp_listen_book),tint=ink) }
        TextButton(onClick=onAutoScroll,modifier=Modifier.testTag("reader-auto-scroll"),contentPadding=PaddingValues(horizontal=4.dp),colors=ButtonDefaults.textButtonColors(contentColor=ink)) {
            Column(horizontalAlignment=Alignment.CenterHorizontally) {
                Icon(if(autoScroll) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,null,Modifier.size(18.dp),tint=if(autoScroll) ReaderControlAccent else ink)
                Text(s.localize(Res.string.auto_scroll),style=MaterialTheme.typography.labelSmall,color=ink)
            }
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick=onBookmark,modifier=Modifier.size(40.dp).testTag("reader-bookmark")) { Icon(if(bookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,s.localize(Res.string.tp_bookmarks),tint=if(bookmarked) ReaderControlAccent else ink) }
        IconButton(onClick=onPrevious,enabled=chapterIndex>0,modifier=Modifier.size(40.dp).testTag("reader-previous")) { Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_previous_chapter),tint=ink.copy(alpha=if(chapterIndex>0) 1f else .35f)) }
        Column(horizontalAlignment=Alignment.CenterHorizontally) {
            IconButton(onClick=onContents,modifier=Modifier.size(40.dp).testTag("reader-contents")) { Icon(Icons.Default.MenuBook,s.localize(Res.string.drawer),tint=ink) }
            Text(if(totalChapters>0) "${(chapterIndex+1).coerceIn(1,totalChapters)}/$totalChapters" else "—",style=MaterialTheme.typography.labelSmall)
        }
        IconButton(onClick=onNext,enabled=chapterIndex<totalChapters-1,modifier=Modifier.size(40.dp).testTag("reader-next")) { Icon(Icons.Default.ChevronRight,s.localize(Res.string.tp_next_chapter),tint=ink.copy(alpha=if(chapterIndex<totalChapters-1) 1f else .35f)) }
    }
}
