package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import ireader.domain.preferences.prefs.UiPreferences

// This preference changes at runtime. Track readers so the switch does not
// invalidate unrelated chapter content and navigation state below its provider.
val LocalReaderFullGlass=compositionLocalOf {false}
private val LocalReaderGlassPanel=staticCompositionLocalOf {false}

/** A panel diffuses its backdrop once. Inner cards inherit it instead of sampling the sharp page again. */
@Composable fun ReaderGlassPanelContent(content:@Composable ()->Unit) {
    CompositionLocalProvider(LocalReaderGlassPanel provides true,content=content)
}

@Composable fun ReaderGlassPanelColumn(modifier:Modifier=Modifier,
    verticalArrangement:Arrangement.Vertical=Arrangement.Top,horizontalAlignment:Alignment.Horizontal=Alignment.Start,
    content:@Composable ColumnScope.()->Unit) {
    ReaderGlassPanelContent {Column(modifier,verticalArrangement,horizontalAlignment,content)}
}

@Composable internal fun Modifier.readerCardGlass(tint:Color,radius:Int?=null,shape:androidx.compose.ui.graphics.Shape?=null):Modifier =
    if(LocalReaderGlassPanel.current)background(tint.copy(alpha=.08f)) else readerNavigationGlass(tint,radius=radius,shapeOverride=shape)

/** A new popup/window always needs its own diffusion even when opened from an existing panel. */
@Composable fun Modifier.readerOptionalPanelGlass(radius:Int=28,tint:Color=MaterialTheme.colorScheme.surface):Modifier =
    if(LocalReaderFullGlass.current)clip(RoundedCornerShape(radius.dp))
        .readerNavigationGlass(tint,radius=radius,material=ReaderGlassMaterial.Frosted) else this

/** One persisted choice above navigation, reader paper themes, dialogs and popups. */
@Composable fun ReaderGlassPreferences(preferences:UiPreferences,content:@Composable ()->Unit) {
    val preference=remember(preferences) {preferences.readerFullGlass()}
    val full by preference.changes().collectAsState(initial=preference.get())
    CompositionLocalProvider(LocalReaderFullGlass provides full,content=content)
}

/** Lower-z input lets cards inside a recorded page sample paper, never their own text. */
@Composable fun ReaderGlassBackground(modifier:Modifier) {
    val backdrop=LocalReaderNavigationBackdrop.current
    if(LocalReaderFullGlass.current && backdrop!=null) {
        val paper=LocalReaderPaper.current?.paper ?: MaterialTheme.colorScheme.background
        Box(modifier.hazeSource(backdrop.state,zIndex=-1f).background(paper))
    }
}

/** Existing solid components opt into Glass without changing the default appearance. */
@Composable fun Modifier.readerOptionalGlass(radius:Int=20,tint:Color=LocalReaderPaper.current?.paper ?: MaterialTheme.colorScheme.surface):Modifier =
    if(LocalReaderFullGlass.current)readerGlassCard(radius,tint) else this

@Composable fun readerOptionalGlassColor(standard:Color):Color =
    if(LocalReaderFullGlass.current)Color.Transparent else standard

fun readerGlassCaptureAllowed(api:Int,lowRam:Boolean,fullGlass:Boolean)=api>=32 && (fullGlass || !lowRam)
