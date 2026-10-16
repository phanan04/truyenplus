package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.i18n.resources.*

@Composable
fun ReaderChapterFooter(chapterIndex: Int, totalChapters: Int, bookmarked: Boolean, autoScroll: Boolean,
    onPrevious: () -> Unit, onNext: () -> Unit, onContents: () -> Unit, onListen: () -> Unit,
    onBookmark: () -> Unit, onAutoScroll: () -> Unit, modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surface, onAppearance: () -> Unit = {}) {
    val ink=LocalContentColor.current
    val s=requireNotNull(LocalLocalizeHelper.current)
    var menuOpen by remember {mutableStateOf(false)}
    val activePanel=LocalReaderPanels.current?.active
    LaunchedEffect(activePanel) {if(activePanel!=null && activePanel!=ReaderPanel.None)menuOpen=false}
    ireader.presentation.core.ui.PlatformBackHandler(menuOpen){menuOpen=false}
    val position=if(totalChapters>0)"${(chapterIndex+1).coerceIn(1,totalChapters)}/$totalChapters" else "—"
    Box(modifier.fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
        BoxWithConstraints(Modifier.widthIn(max=760.dp).fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)) {
            val menuWidth=minOf(272.dp,maxWidth)
            Column(Modifier.fillMaxWidth(),horizontalAlignment=Alignment.End,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                if(menuOpen)Column(Modifier.width(menuWidth).testTag("reader-reading-menu"),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                    Surface(onClick={menuOpen=false;onContents()},shape=RoundedCornerShape(24.dp),color=ink,contentColor=backgroundColor,
                        modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("reader-contents")) {
                        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(s.localize(Res.string.tp_chapters)+" · "+position,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Icon(Icons.Default.FormatListBulleted,null,Modifier.size(24.dp))
                        }
                    }
                    Surface(onClick={menuOpen=false;onAppearance()},shape=RoundedCornerShape(24.dp),color=Color.Transparent,contentColor=ink,
                        modifier=Modifier.fillMaxWidth().readerPanel(24,backgroundColor,ink).heightIn(min=48.dp).testTag("reader-menu-appearance")) {
                        Row(Modifier.padding(horizontal=16.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(s.localize(Res.string.tp_reader_appearance),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text("Aa",style=MaterialTheme.typography.titleMedium)
                        }
                    }
                    Surface(Modifier.fillMaxWidth().readerPanel(28,backgroundColor,ink),shape=RoundedCornerShape(28.dp),color=Color.Transparent,contentColor=ink) {
                        Row(Modifier.height(56.dp).padding(horizontal=4.dp),horizontalArrangement=Arrangement.SpaceEvenly,verticalAlignment=Alignment.CenterVertically) {
                            IconButton({menuOpen=false;onPrevious()},Modifier.size(48.dp).testTag("reader-previous"),enabled=chapterIndex>0){Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_previous_chapter))}
                            IconButton({menuOpen=false;onNext()},Modifier.size(48.dp).testTag("reader-next"),enabled=chapterIndex<totalChapters-1){Icon(Icons.Default.ChevronRight,s.localize(Res.string.tp_next_chapter))}
                            IconButton({menuOpen=false;onBookmark()},Modifier.size(48.dp).testTag("reader-bookmark")){Icon(if(bookmarked)Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,s.localize(Res.string.tp_bookmarks))}
                            IconButton({menuOpen=false;onAutoScroll()},Modifier.size(48.dp).testTag("reader-auto-scroll")){Icon(if(autoScroll)Icons.Default.Pause else Icons.Default.UnfoldMore,s.localize(Res.string.auto_scroll))}
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().height(56.dp).testTag("reader-footer-capsule"),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    Row(Modifier.testTag("reader-toolbar-reading"),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Surface(Modifier.size(48.dp).readerPanel(24,backgroundColor,ink),shape=RoundedCornerShape(24.dp),color=Color.Transparent,contentColor=ink) {
                            IconButton({menuOpen=false;onListen()},Modifier.fillMaxSize().testTag("reader-listen")){Icon(Icons.Default.Headphones,s.localize(Res.string.tp_listen_book))}
                        }
                        IconButton({menuOpen=false;onAppearance()},Modifier.size(48.dp).testTag("reader-appearance-shortcut").semantics {contentDescription=s.localize(Res.string.tp_reader_appearance)}) {Text("Aa",color=ink,style=MaterialTheme.typography.titleMedium)}
                    }
                    Row(Modifier.weight(1f).testTag("reader-toolbar-navigation"),verticalAlignment=Alignment.CenterVertically) {
                        Text(position,Modifier.weight(1f),style=MaterialTheme.typography.bodySmall,color=ink.copy(alpha=.6f),textAlign=androidx.compose.ui.text.style.TextAlign.Center,maxLines=1)
                        Surface(Modifier.size(48.dp).readerPanel(24,backgroundColor,ink),shape=RoundedCornerShape(24.dp),color=Color.Transparent,contentColor=ink) {
                            IconButton({menuOpen=!menuOpen},Modifier.fillMaxSize().testTag("reader-footer-more")){Icon(if(menuOpen)Icons.Default.Close else Icons.Default.FormatListBulleted,s.localize(Res.string.more))}
                        }
                    }
                }
            }
        }
    }
}
