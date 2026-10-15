package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.*
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.GlassDefaults
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur

@Composable expect fun rememberReaderBlurSupported(): Boolean

/** Shared chapter/shell source. Floating controls are deliberately outside this capture. */
@Stable
class ReaderNavigationBackdrop {
    val state = HazeState()
    var samplingEnabled by mutableStateOf(true)
}
val LocalReaderNavigationBackdrop = staticCompositionLocalOf<ReaderNavigationBackdrop?> { null }

enum class ReaderGlassMaterial { Compact, Frosted }

@Composable
fun Modifier.readerNavigationSource(backdrop: ReaderNavigationBackdrop?): Modifier {
    val paper=LocalReaderPaper.current?.paper ?: androidx.compose.material3.MaterialTheme.colorScheme.background
    return if(backdrop==null || !backdrop.samplingEnabled) this else
        hazeSource(backdrop.state).background(paper)
}

/** Floating surfaces always use the current material; cards opt in through the global setting. */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun Modifier.readerNavigationGlass(tint: Color, radius:Int?=null,
    material:ReaderGlassMaterial=ReaderGlassMaterial.Compact): Modifier {
    val backdrop=LocalReaderNavigationBackdrop.current
    if(backdrop==null || !backdrop.samplingEnabled) {
        return drawWithContent {drawRect(tint);drawContent()}
    }
    val dark=tint.luminance()<.5f
    val full=LocalReaderFullGlass.current
    val base=drawWithContent {drawRect(tint);drawContent()}
    // Large panels need diffusion and readable text, not extra refraction render layers.
    if(material==ReaderGlassMaterial.Frosted && !full) {
        val style=remember(tint) {HazeBlurStyle {
            backgroundColor(tint)
            blurRadius(12.dp)
            noiseFactor(0f)
            colorEffects(listOf(HazeColorEffect.tint(tint.copy(alpha=.62f))))
            fallbackColorEffect(HazeColorEffect.tint(tint))
        }}
        return base.hazeBlur(HazeInput.Sources(backdrop.state),style,
            performanceMode=HazePerformanceMode.Default)
    }
    val style=remember(tint,radius,material,full) {
        GlassStyle.regular.then {
            shape(radius?.let {RoundedCornerShape(it.dp)} ?: RoundedCornerShape(50))
            backgroundColor(tint)
            tint(tint.copy(alpha=if(full && material==ReaderGlassMaterial.Frosted).42f else if(dark).22f else .18f))
            // Keep Haze's size-aware Regular optics, including its curved edge refraction.
            // The earlier fixed, weak optics made Glass look almost like ordinary blur.
            optics(GlassDefaults.optics)
            contentNormalBlend(0f)
            chromaticAberrationStrength(0f)
            // Explicit tone follows app/reader paper, even if the system theme is opposite.
            ambientResponse(if(dark).08f else .15f)
            specularIntensity(if(dark).38f else .48f)
            contrast(0f)
            whitePoint(0f)
            chromaMultiplier(1f)
            edgeShadow(Color.Transparent)
        }
    }
    // Haze may have no retained input during its first frame or a window transition.
    // Keep sharp controls behind this surface hidden even before Glass has captured its source.
    return base.hazeGlass(HazeInput.Sources(backdrop.state),style,
        performanceMode=HazePerformanceMode.Default)
}
