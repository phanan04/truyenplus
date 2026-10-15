package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Small code-native symbols with the proportions of the supplied Books toolbar. */
internal object ReaderBooksSymbols {
    val Library=ImageVector.Builder("Books on shelf",28.dp,28.dp,28f,28f).apply {
        path(fill=SolidColor(Color.Black),pathFillType=PathFillType.EvenOdd) {
            moveTo(2f,5f);lineTo(6f,5f);lineTo(6f,26f);lineTo(2f,26f);close()
            moveTo(8f,1f);lineTo(14f,1f);lineTo(14f,26f);lineTo(8f,26f);close()
            moveTo(16f,5f);lineTo(21f,3.5f);lineTo(27f,24.5f);lineTo(22f,26f);close()
            moveTo(9.5f,5f);lineTo(12.5f,5f);lineTo(12.5f,6.5f);lineTo(9.5f,6.5f);close()
        }
    }.build()
}

@Composable internal fun ReaderBooksAccountSymbol(label:String) {
    val ink=LocalContentColor.current
    Canvas(Modifier.size(44.dp).semantics {contentDescription=label}) {
        val factor=size.width/44f
        scale(factor,factor,pivot=Offset.Zero) {
            drawCircle(ink,radius=20f,center=Offset(22f,22f),style=Stroke(width=3.2f))
            val clip=Path().apply {addOval(androidx.compose.ui.geometry.Rect(3.8f,3.8f,40.2f,40.2f))}
            clipPath(clip) {
                drawCircle(ink,radius=7.4f,center=Offset(22f,16f))
                drawPath(Path().apply {moveTo(6f,42f);cubicTo(6f,22f,38f,22f,38f,42f);close()},ink)
            }
        }
    }
}
