package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** App surfaces use the app palette. Keep the reader's independently selected paper intact. */
@Composable fun Modifier.readerAppGlassCard(radius:Int=20):Modifier {
    val c=MaterialTheme.colorScheme
    val shape=RoundedCornerShape(radius.dp)
    // Scrolling lists never sample themselves or run a blur pass for every row.
    return clip(shape)
        .background(Brush.linearGradient(listOf(c.surface.copy(alpha=.88f),c.surface.copy(alpha=.78f))))
        .border(1.dp,c.onSurface.copy(alpha=.12f),shape)
}

/** Standalone book/shelf pages have only static cards in the recorded content layer. */
@Composable fun ReaderAppBackdrop(content:@Composable BoxScope.()->Unit) {
    val supported=rememberReaderBlurSupported()
    val backdrop=remember(supported) {if(supported)ReaderNavigationBackdrop() else null}
    CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
        ReaderBackdrop {Box(Modifier.fillMaxSize().readerNavigationSource(backdrop),content=content)}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderGlassSheet(onDismissRequest:()->Unit,content:@Composable ColumnScope.()->Unit) {
    val c=MaterialTheme.colorScheme
    ModalBottomSheet(onDismissRequest=onDismissRequest,
        shape=RoundedCornerShape(28.dp),dragHandle=null,
        containerColor=Color.Transparent,contentColor=c.onSurface,tonalElevation=0.dp,
        sheetMaxWidth=720.dp,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        // Clip the actual panel, after Material's sheet motion. Clipping its outer modifier
        // cuts off descendants while the sheet is translated into the window.
        Surface(Modifier.fillMaxWidth().readerPanel(28,c.surface,c.onSurface),shape=RoundedCornerShape(28.dp),
            color=Color.Transparent,contentColor=c.onSurface) {
            Column {
                BottomSheetDefaults.DragHandle(Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally))
                content()
            }
        }
    }
}

@Composable fun ReaderGlassDialog(onDismissRequest:()->Unit,title:@Composable ()->Unit,
    text:@Composable ()->Unit,confirmButton:@Composable ()->Unit,dismissButton:(@Composable ()->Unit)?=null) {
    val c=MaterialTheme.colorScheme
    AlertDialog(onDismissRequest=onDismissRequest,title=title,text=text,confirmButton=confirmButton,dismissButton=dismissButton,
        modifier=Modifier.readerPanel(28,c.surface,c.onSurface),shape=RoundedCornerShape(28.dp),
        containerColor=Color.Transparent,titleContentColor=c.onSurface,textContentColor=c.onSurfaceVariant,tonalElevation=0.dp)
}
