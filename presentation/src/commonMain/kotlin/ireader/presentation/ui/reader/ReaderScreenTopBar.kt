package ireader.presentation.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.testTag
import ireader.presentation.ui.truyenplus.readerPanel
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Report
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.material3.Text
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.domain.models.entities.Chapter
import ireader.i18n.localize
import ireader.i18n.resources.*
import ireader.i18n.resources.bookmark
import ireader.i18n.resources.expand_menu
import ireader.i18n.resources.find_in_chapter
import ireader.i18n.resources.refresh
import ireader.i18n.resources.report_broken_chapter
import ireader.i18n.resources.webView
import ireader.presentation.core.toComposeColor
import ireader.presentation.ui.component.reusable_composable.AppIconButton
import ireader.presentation.ui.component.reusable_composable.TopAppBarBackButton
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.viewmodel.ReaderScreenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreenTopBar(
    modifier: Modifier = Modifier,
    isReaderModeEnable: Boolean,
    vm: ReaderScreenViewModel,
    state: ReaderScreenViewModel, // Using ViewModel directly for state access
    chapter: Chapter?,
    onRefresh: () -> Unit,
    onRefreshFromRemote: () -> Unit,
    onWebView: () -> Unit,
    onBookMark: () -> Unit,
    onPopBackStack: () -> Unit,
    onChapterArt: () -> Unit = {},
    onPluginMenu: () -> Unit = {},
    hasPluginMenuItems: Boolean = false,
    isLoaded: Boolean = false,
    onPlay: (() -> Unit)? = null,
) {
    if (!isReaderModeEnable || !isLoaded) {
        val panels=ireader.presentation.ui.truyenplus.LocalReaderPanels.current
        ReaderChapterTopBar(chapter, onBookMark, onPopBackStack, {
            if(panels!=null)panels.open(ireader.presentation.ui.truyenplus.ReaderPanel.Appearance) else vm.showSettingsBottomSheet=true
        }, modifier, onPlay,
            backgroundColor = vm.backgroundColor.value.toComposeColor(), textColor = vm.textColor.value.toComposeColor(), bookTitle = vm.book?.title.orEmpty(), onEdit = { chapter?.id?.let { vm.chapterEditorId = it } })
    }
}

@Composable
fun ReaderChapterTopBar(chapter: Chapter?, onBookmark: () -> Unit, onBack: () -> Unit,
    onSettings: () -> Unit, modifier: Modifier = Modifier, onListen: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface, textColor: Color = MaterialTheme.colorScheme.onSurface, bookTitle: String = "", onEdit: (() -> Unit)? = null) {
    var moreOpen by remember { mutableStateOf(false) }
    Box(modifier.fillMaxWidth().statusBarsPadding().padding(horizontal=12.dp,vertical=6.dp),contentAlignment=Alignment.TopCenter) {
      Surface(modifier=Modifier.widthIn(max=760.dp).fillMaxWidth().readerPanel(28,backgroundColor,textColor).testTag("reader-top-capsule"),shape=RoundedCornerShape(28.dp),color=Color.Transparent,contentColor=textColor) {
        Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalIconButton(onClick=onBack,modifier=Modifier.size(48.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=textColor.copy(alpha=.06f),contentColor=textColor)) {
                Icon(Icons.Default.ChevronLeft,localize(Res.string.tp_back))
            }
            Column(Modifier.weight(1f).padding(horizontal=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
                if(bookTitle.isNotBlank() && LocalDensity.current.fontScale<1.5f) Text(bookTitle,style=MaterialTheme.typography.labelSmall,
                    color=textColor.copy(alpha=.6f),maxLines=1,overflow=TextOverflow.Ellipsis)
                Text(ireader.presentation.ui.truyenplus.readerChapterDisplayTitle(chapter?.name.orEmpty()),style=MaterialTheme.typography.titleSmall,
                    fontWeight=FontWeight.SemiBold,textAlign=TextAlign.Center,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            if (onEdit != null) Box {
                FilledTonalIconButton(onClick={moreOpen=true},modifier=Modifier.size(48.dp),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=textColor.copy(alpha=.06f),contentColor=textColor)) {
                    Icon(Icons.Default.MoreHoriz,localize(Res.string.expand_menu))
                }
                DropdownMenu(expanded=moreOpen,onDismissRequest={moreOpen=false}) {
                    DropdownMenuItem(text={Text(localize(Res.string.tp_edit_chapter))},onClick={moreOpen=false;onEdit()})
                    DropdownMenuItem(text={Text(localize(Res.string.bookmark))},onClick={moreOpen=false;onBookmark()})
                }
            }
            FilledTonalIconButton(onClick=onSettings,modifier=Modifier.size(48.dp).testTag("reader-settings"),colors=IconButtonDefaults.filledTonalIconButtonColors(containerColor=textColor.copy(alpha=.06f),contentColor=textColor)) {
                Icon(Icons.Default.Tune,localize(Res.string.settings))
            }
        }
    }
    }
}
