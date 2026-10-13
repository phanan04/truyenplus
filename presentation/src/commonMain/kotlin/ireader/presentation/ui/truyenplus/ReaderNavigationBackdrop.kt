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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

@Composable expect fun rememberReaderBlurSupported(): Boolean

/** A display list, not a screenshot. Navigation is outside this source to avoid feedback. */
@Stable
class ReaderNavigationBackdrop(val layer: GraphicsLayer) {
    var position by mutableStateOf(Offset.Zero)
}
val LocalReaderNavigationBackdrop = staticCompositionLocalOf<ReaderNavigationBackdrop?> { null }

fun Modifier.readerNavigationSource(backdrop: ReaderNavigationBackdrop?): Modifier =
    if(backdrop==null) this else onGloballyPositioned { backdrop.position=it.positionInRoot() }.drawWithContent {
        backdrop.layer.record { this@drawWithContent.drawContent() }
        drawLayer(backdrop.layer)
    }

/** Only the capsule-sized sampled layer gets a blur pass; icons/text are drawn afterwards. */
@Composable
fun Modifier.readerNavigationGlass(tint: Color, fallbackAlpha: Float = .94f, blurTintAlpha: Float = .72f): Modifier {
    val backdrop=LocalReaderNavigationBackdrop.current
    if(backdrop==null) return this.drawWithContent { drawRect(tint.copy(alpha=fallbackAlpha));drawContent() }
    val sampled=rememberGraphicsLayer()
    val density=androidx.compose.ui.platform.LocalDensity.current
    val blur=remember(density) { with(density) { BlurEffect(10.dp.toPx(),10.dp.toPx(),TileMode.Clamp) } }
    var position by remember { mutableStateOf(Offset.Zero) }
    return onGloballyPositioned { position=it.positionInRoot() }.drawWithContent {
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
