package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.focusGroup
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

/** Same-window task: the caller supplies the available area above playback. */
@Composable fun ReaderFloatingTaskPanel(title:String,onDismiss:()->Unit,content:@Composable ColumnScope.()->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    ireader.presentation.core.ui.PlatformBackHandler(true,onDismiss)
    val closeFocus=remember {FocusRequester()}
    LaunchedEffect(Unit){withFrameNanos {};closeFocus.requestFocus()}
    ReaderPaperTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clickable(remember {MutableInteractionSource()},indication=null,onClick=onDismiss))
            Surface(Modifier.align(Alignment.Center).widthIn(max=720.dp).fillMaxWidth().padding(12.dp)
                .heightIn(max=(maxHeight-24.dp).coerceAtLeast(48.dp)).readerPanel(28).testTag("reader-floating-task")
                .semantics {paneTitle=title;isTraversalGroup=true}.focusGroup()
                .clickable(remember {MutableInteractionSource()},indication=null){},
                shape=RoundedCornerShape(28.dp),color=Color.Transparent,contentColor=MaterialTheme.colorScheme.onSurface) {
                ReaderGlassPanelColumn(Modifier.padding(16.dp)) {
                    ReaderBooksSheetHeader(title,onDismiss,closeModifier=Modifier.focusRequester(closeFocus))
                    content()
                }
            }
        }
    }
}
