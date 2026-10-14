package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable expect fun rememberReaderBlurSupported(): Boolean

/** A display list, not a screenshot. Navigation is outside this source to avoid feedback. */
@Stable
class ReaderNavigationBackdrop(val layer: GraphicsLayer) {
    var samplingEnabled by mutableStateOf(true)
    var position by mutableStateOf(Offset.Zero)
}
val LocalReaderNavigationBackdrop = staticCompositionLocalOf<ReaderNavigationBackdrop?> { null }

private fun LayoutCoordinates.backdropPosition():Offset {
    val screen=positionOnScreen()
    return if(screen.x.isFinite() && screen.y.isFinite())screen else positionInRoot()
}

@Composable
fun Modifier.readerNavigationSource(backdrop: ReaderNavigationBackdrop?): Modifier {
    val paper=LocalReaderPaper.current?.paper ?: androidx.compose.material3.MaterialTheme.colorScheme.background
    return if(backdrop==null) this else onGloballyPositioned {backdrop.position=it.backdropPosition()}.drawWithContent {
        if(backdrop.samplingEnabled) {
            // A transparent source leaves the real, sharp navigation/player visible beneath the blur.
            backdrop.layer.record {drawRect(paper);this@drawWithContent.drawContent()}
            drawLayer(backdrop.layer)
        } else drawContent()
    }
}

/** Only the capsule-sized sampled layer gets a blur pass; icons/text are drawn afterwards. */
@Composable
fun Modifier.readerNavigationGlass(tint: Color, blurTintAlpha: Float = .72f): Modifier {
    val backdrop=LocalReaderNavigationBackdrop.current
    if(backdrop==null) return this.drawWithContent { drawRect(tint);drawContent() }
    val sampled=rememberGraphicsLayer()
    val density=androidx.compose.ui.platform.LocalDensity.current
    val blur=remember(density) { with(density) { BlurEffect(10.dp.toPx(),10.dp.toPx(),TileMode.Clamp) } }
    var position by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { position=it.backdropPosition() }.drawWithContent {
        if(!backdrop.samplingEnabled) {
            drawRect(tint);drawContent()
            return@drawWithContent
        }
        // Opaque base also covers holes outside the recorded viewport and prevents sharp underlays
        // bleeding through native dialog/sheet windows. Only the sampled content is translucent.
        drawRect(tint)
        val padding=12.dp.roundToPx()
        val offset=position-backdrop.position
        sampled.renderEffect=blur
        sampled.record(size=IntSize(size.width.toInt()+padding*2,size.height.toInt()+padding*2)) {
            translate(padding-offset.x,padding-offset.y) { drawLayer(backdrop.layer) }
        }
        translate(-padding.toFloat(),-padding.toFloat()) { drawLayer(sampled) }
        drawRect(tint.copy(alpha=blurTintAlpha))
        drawContent()
    }
}
