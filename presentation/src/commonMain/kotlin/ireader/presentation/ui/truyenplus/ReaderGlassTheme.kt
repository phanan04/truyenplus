package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors

/** Four materials share every screen. No backdrop capture, blur pass or animated shader. */
enum class ReaderGlassTheme(val id: String, val dark: Boolean, val colors: ColorScheme) {
    Violet("violet", true, glassColors(true, 0xFFB7ACFF, 0xFF11131D, 0xFF202333, 0xFFEEEFFA, 0xFFBBC0D5)),
    Pearl("pearl", false, glassColors(false, 0xFF155DCC, 0xFFF3F7FD, 0xFFFFFFFF, 0xFF142137, 0xFF526278)),
    Graphite("graphite", true, glassColors(true, 0xFFABCEBF, 0xFF101413, 0xFF202725, 0xFFEEF3F0, 0xFFB6C3BD)),
    Amber("amber", false, glassColors(false, 0xFF8A4619, 0xFFFCF5E7, 0xFFFFF9EF, 0xFF302117, 0xFF705C4D));
    companion object { fun fromId(id: String) = entries.firstOrNull { it.id == id } ?: Violet }
}

private fun glassColors(dark: Boolean, accent: Long, background: Long, surface: Long, ink: Long, muted: Long): ColorScheme {
    val base=if(dark) TruyenPlusDarkColors else TruyenPlusLightColors
    val p=Color(accent); val bg=Color(background); val s=Color(surface); val text=Color(ink)
    return base.copy(primary=p,onPrimary=if(dark) Color(0xFF15201C) else Color.White,
        primaryContainer=lerp(s,p,.18f),onPrimaryContainer=text,
        secondary=p,onSecondary=if(dark) Color(0xFF15201C) else Color.White,
        secondaryContainer=lerp(s,p,.1f),onSecondaryContainer=text,
        tertiary=p,onTertiary=if(dark) Color(0xFF15201C) else Color.White,
        background=bg,onBackground=text,surface=s,onSurface=text,
        surfaceVariant=lerp(s,p,.09f),onSurfaceVariant=Color(muted),
        surfaceContainerLowest=bg,surfaceContainerLow=lerp(bg,s,.5f),surfaceContainer=s,
        surfaceContainerHigh=lerp(s,p,.07f),surfaceContainerHighest=lerp(s,p,.13f),
        outline=lerp(s,text,.45f),outlineVariant=lerp(s,text,.18f),surfaceTint=Color.Transparent)
}

val LocalReaderGlassTheme = staticCompositionLocalOf { ReaderGlassTheme.Violet }

/** Static gradients and one border give glass depth without sampling pixels behind the panel. */
@Composable
fun Modifier.readerGlass(radius: Int = 24): Modifier {
    val c=MaterialTheme.colorScheme
    val shape=remember(radius) { RoundedCornerShape(radius.dp) }
    val fill=remember(c) { Brush.linearGradient(listOf(lerp(c.surfaceContainerHigh,c.onSurface,.045f),c.surfaceContainer.copy(alpha=.96f),c.surfaceContainerLow)) }
    val edge=remember(c) { Brush.linearGradient(listOf(c.onSurface.copy(alpha=.24f),c.outlineVariant.copy(alpha=.35f),c.onSurface.copy(alpha=.08f))) }
    return clip(shape).background(fill).border(BorderStroke(1.dp,edge),shape)
}
