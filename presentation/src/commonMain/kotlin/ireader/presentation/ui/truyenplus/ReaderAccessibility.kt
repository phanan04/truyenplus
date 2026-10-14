package ireader.presentation.ui.truyenplus
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.semantics.clearAndSetSemantics
/** Keep the rendering/backdrop intact while excluding covered content from task focus. */
fun Modifier.readerCoveredContent(covered:Boolean):Modifier=if(covered)clearAndSetSemantics {}.focusProperties {canFocus=false} else this
