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
    val ink = LocalContentColor.current
    val s = requireNotNull(LocalLocalizeHelper.current)
    Box(modifier.fillMaxWidth(),contentAlignment=Alignment.BottomCenter) {
    BoxWithConstraints(Modifier.widthIn(max=760.dp).fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp)) {
        val fullControls=maxWidth>=312.dp
        var moreOpen by remember {mutableStateOf(false)}
        val listen: @Composable () -> Unit = {
            IconButton(onClick=onListen,modifier=Modifier.size(48.dp).testTag("reader-listen")) { Icon(Icons.Default.Headphones,s.localize(Res.string.tp_listen_book),tint=ink) }
        }
        val appearance: @Composable () -> Unit = {
            IconButton(onClick=onAppearance,modifier=Modifier.size(48.dp).testTag("reader-appearance-shortcut")) {
                Text("Aa",color=ink,style=MaterialTheme.typography.titleMedium,
                    modifier=Modifier.semantics {contentDescription=s.localize(Res.string.tp_reader_appearance)})
            }
        }
        val mark: @Composable () -> Unit = {
            IconButton(onClick=onBookmark,modifier=Modifier.size(48.dp).testTag("reader-bookmark")) { Icon(if(bookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,s.localize(Res.string.tp_bookmarks),tint=if(bookmarked) ReaderControlAccent else ink) }
        }
        val previous: @Composable () -> Unit = {
            IconButton(onClick=onPrevious,enabled=chapterIndex>0,modifier=Modifier.size(48.dp).testTag("reader-previous")) { Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_previous_chapter),tint=ink.copy(alpha=if(chapterIndex>0) 1f else .35f)) }
        }
        val contents: @Composable () -> Unit = {
            val position=if(totalChapters>0) "${(chapterIndex+1).coerceIn(1,totalChapters)}/$totalChapters" else "—"
            IconButton(onClick=onContents,modifier=Modifier.size(48.dp).testTag("reader-contents")) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    Icon(Icons.Default.MenuBook,s.localize(Res.string.drawer)+" · "+position,tint=ink,modifier=Modifier.size(22.dp))
                    if(androidx.compose.ui.platform.LocalDensity.current.fontScale<1.5f)
                        Text(position,style=MaterialTheme.typography.labelSmall,maxLines=1)
                }
            }
        }
        val next: @Composable () -> Unit = {
            IconButton(onClick=onNext,enabled=chapterIndex<totalChapters-1,modifier=Modifier.size(48.dp).testTag("reader-next")) { Icon(Icons.Default.ChevronRight,s.localize(Res.string.tp_next_chapter),tint=ink.copy(alpha=if(chapterIndex<totalChapters-1) 1f else .35f)) }
        }
        Row(Modifier.fillMaxWidth().heightIn(min=56.dp).testTag("reader-footer-capsule"),
            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceBetween) {
            Surface(Modifier.readerPanel(28,backgroundColor,ink).testTag("reader-toolbar-reading"),
                shape=RoundedCornerShape(28.dp),color=Color.Transparent,contentColor=ink) {
                Row(Modifier.height(56.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    listen();appearance();if(fullControls)mark()
                }
            }
            Surface(Modifier.readerPanel(28,backgroundColor,ink).testTag("reader-toolbar-navigation"),
                shape=RoundedCornerShape(28.dp),color=Color.Transparent,contentColor=ink) {
                Row(Modifier.height(56.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
                    if(fullControls){previous();contents();next()} else {
                        contents()
                        Box {
                            IconButton(onClick={moreOpen=true},modifier=Modifier.size(48.dp).testTag("reader-footer-more")){Icon(Icons.Default.MoreVert,s.localize(Res.string.more))}
                            ReaderGlassDropdownMenu(moreOpen,{moreOpen=false}) {
                                DropdownMenuItem(text={Text(s.localize(Res.string.tp_previous_chapter))},modifier=Modifier.heightIn(min=64.dp).testTag("reader-previous"),enabled=chapterIndex>0,onClick={moreOpen=false;onPrevious()})
                                DropdownMenuItem(text={Text(s.localize(Res.string.tp_next_chapter))},modifier=Modifier.heightIn(min=64.dp).testTag("reader-next"),enabled=chapterIndex<totalChapters-1,onClick={moreOpen=false;onNext()})
                                DropdownMenuItem(text={Text(s.localize(Res.string.tp_bookmarks))},modifier=Modifier.heightIn(min=64.dp).testTag("reader-bookmark"),onClick={moreOpen=false;onBookmark()})
                                DropdownMenuItem(text={Text(s.localize(Res.string.auto_scroll))},onClick={moreOpen=false;onAutoScroll()})
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
