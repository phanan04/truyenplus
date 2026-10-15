package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass

@Composable expect fun rememberReaderBlurSupported(): Boolean

/** Shared chapter/shell source. Floating controls are deliberately outside this capture. */
@Stable
class ReaderNavigationBackdrop {
    val state = HazeState()
    var samplingEnabled by mutableStateOf(true)
}
val LocalReaderNavigationBackdrop = staticCompositionLocalOf<ReaderNavigationBackdrop?> { null }

@Composable
fun Modifier.readerNavigationSource(backdrop: ReaderNavigationBackdrop?): Modifier {
    val paper=LocalReaderPaper.current?.paper ?: androidx.compose.material3.MaterialTheme.colorScheme.background
    return if(backdrop==null || !backdrop.samplingEnabled) this else
        hazeSource(backdrop.state).background(paper)
}

/** Glass is limited to floating surfaces. Static list rows and highlight never run a shader. */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.readerNavigationGlass(tint: Color, blurTintAlpha: Float = .72f, radius:Int?=null): Modifier {
    val backdrop=LocalReaderNavigationBackdrop.current
    if(backdrop==null || !backdrop.samplingEnabled) {
        return drawWithContent {drawRect(tint);drawContent()}
    }
    val style=remember(tint,blurTintAlpha,radius) {
        GlassStyle.regular.then {
            shape(radius?.let {RoundedCornerShape(it.dp)} ?: RoundedCornerShape(50))
            backgroundColor(tint)
            tint(tint.copy(alpha=blurTintAlpha))
            optics(refractionStrength=.25f,refractionDisplacement=5.dp,blurRadius=16.dp)
            chromaticAberrationStrength(0f)
            specularIntensity(.2f)
            edgeShadow(Color.Transparent)
        }
    }
    // Haze may have no retained input during its first frame or a window transition.
    // Keep sharp controls behind this surface hidden even before Glass has captured its source.
    return drawWithContent {drawRect(tint);drawContent()}.hazeGlass(HazeInput.Sources(backdrop.state),style,
        performanceMode=HazePerformanceMode.Performance)
}
