package ireader.presentation.ui.reader

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
        ReaderChapterTopBar(chapter, onBookMark, onPopBackStack, { vm.showSettingsBottomSheet = true }, modifier, onPlay,
            backgroundColor = vm.backgroundColor.value.toComposeColor(), textColor = vm.textColor.value.toComposeColor())
    }
}

@Composable
fun ReaderChapterTopBar(chapter: Chapter?, onBookmark: () -> Unit, onBack: () -> Unit,
    onSettings: () -> Unit, modifier: Modifier = Modifier, onListen: (() -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surface, textColor: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier = modifier.fillMaxWidth(), color = backgroundColor, contentColor = textColor) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            TopAppBarBackButton(tint = textColor, onClick = onBack)
            Text(chapter?.name.orEmpty(), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            AppIconButton(imageVector = Icons.Default.Tune, contentDescription = localize(Res.string.settings), onClick = onSettings, tint = textColor)
        }
    }
}
