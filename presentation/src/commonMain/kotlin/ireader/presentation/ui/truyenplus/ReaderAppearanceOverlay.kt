package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

/** Separate restart scopes keep appearance visibility out of chapter composition and layout. */
@Composable
fun ReaderContentAndAppearance(modifier: Modifier = Modifier, contentAlignment: Alignment = Alignment.Center,
    chapter: @Composable () -> Unit, appearance: @Composable () -> Unit) {
    Box(modifier.fillMaxSize(), contentAlignment = contentAlignment) { chapter(); appearance() }
}

/** In the existing window: no dialog creation, IME/inset changes or dismiss animation over chapter text. */
@Composable
fun ReaderAppearanceOverlay(onDismiss: () -> Unit, background: Color = MaterialTheme.colorScheme.surface,
    ink: Color = MaterialTheme.colorScheme.onSurface, content: @Composable ColumnScope.() -> Unit) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .28f))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onDismiss))
        Surface(Modifier.align(Alignment.BottomCenter).widthIn(max = 720.dp).fillMaxWidth()
            .height((maxHeight * .86f).coerceAtMost(660.dp))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), color = background, contentColor = ink) {
            Column(Modifier.navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(strings.localize(Res.string.tp_reader_appearance), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = onDismiss, Modifier.size(48.dp).testTag("reader-appearance-close")) {
                        Icon(Icons.Default.Close, strings.localize(Res.string.close))
                    }
                }
                HorizontalDivider(color = ink.copy(alpha = .08f))
                content()
            }
        }
    }
}
