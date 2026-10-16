package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

@Composable
fun ReaderReturnToSpeech(onClick: () -> Unit, modifier: Modifier = Modifier) {
    ReaderPaperTheme {
        Surface(onClick=onClick,modifier=modifier.readerPanel(24).testTag("reader-return-to-speech"),
            color=Color.Transparent,contentColor=MaterialTheme.colorScheme.onSurface,shape=RoundedCornerShape(24.dp)) {
            Box(Modifier.heightIn(min=48.dp).padding(horizontal=16.dp),contentAlignment=androidx.compose.ui.Alignment.Center) {
                Text(requireNotNull(LocalLocalizeHelper.current).localize(Res.string.tp_return_to_speech),
                    style=MaterialTheme.typography.labelLarge,maxLines=1)
            }
        }
    }
}
