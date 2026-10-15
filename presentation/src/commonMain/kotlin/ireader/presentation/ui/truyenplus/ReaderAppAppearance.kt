package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** One design, with system/light/dark appearance. Old saved palettes keep their brightness. */
enum class ReaderAppearanceMode(val id: String) {
    System("system"), Light("light"), Dark("dark");
    fun isDark(systemDark: Boolean): Boolean = when(this) { System -> systemDark; Light -> false; Dark -> true }
    companion object {
        fun fromId(id: String): ReaderAppearanceMode = when(id) {
            "dark", "violet", "graphite" -> Dark
            "light", "pearl", "amber", "minimalist" -> Light
            else -> System
        }
    }
}

/** Floating controls sample the chapter/content layer; never record themselves into it. */
@Composable
fun Modifier.readerPanel(radius: Int = 20, tint: Color = LocalReaderPaper.current?.paper ?: MaterialTheme.colorScheme.surface,
    ink: Color = LocalReaderPaper.current?.ink ?: MaterialTheme.colorScheme.onSurface,
    material:ReaderGlassMaterial=ReaderGlassMaterial.Frosted): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return shadow(3.dp, shape).clip(shape)
        .readerNavigationGlass(tint,radius=radius,material=material)
        .border(.5.dp,ink.copy(alpha=.1f),shape)
}

/** Scrolling cards use a static translucent treatment to avoid recursive sampling/large blurs. */
@Composable
fun Modifier.readerGlassCard(radius: Int = 20, tint: Color = LocalReaderPaper.current?.paper ?: MaterialTheme.colorScheme.surface,
    ink: Color = LocalReaderPaper.current?.ink ?: MaterialTheme.colorScheme.onSurface): Modifier {
    val shape=RoundedCornerShape(radius.dp)
    if(LocalReaderFullGlass.current)return clip(shape).readerCardGlass(tint,radius=radius)
        .border(.5.dp,ink.copy(alpha=.1f),shape)
    val sheen=Brush.linearGradient(listOf(tint.copy(alpha=.92f),tint.copy(alpha=.78f)))
    return clip(shape).background(sheen).border(1.dp,ink.copy(alpha=.1f),shape)
}


data class ReaderPaperColors(val paper: Color, val ink: Color)
val LocalReaderPaper = staticCompositionLocalOf<ReaderPaperColors?> { null }

fun readerPaperColorScheme(base: ColorScheme, paper: Color, ink: Color):ColorScheme {
    val feedback=if(paper.luminance()>.5f) lightColorScheme() else darkColorScheme()
    return base.copy(
    background=paper, onBackground=ink, surface=paper, onSurface=ink,
    surfaceContainer=paper, surfaceContainerLow=paper, surfaceContainerHigh=paper,
    surfaceContainerHighest=paper, surfaceContainerLowest=paper, surfaceVariant=paper,
    onSurfaceVariant=ink.copy(alpha=.7f), primary=ink, onPrimary=paper,
    primaryContainer=ink.copy(alpha=.1f), onPrimaryContainer=ink,
    secondary=ink, onSecondary=paper, secondaryContainer=ink.copy(alpha=.1f),
    onSecondaryContainer=ink, tertiary=ink, onTertiary=paper,
    tertiaryContainer=ink.copy(alpha=.1f), onTertiaryContainer=ink,
    inverseSurface=ink, inverseOnSurface=paper, inversePrimary=paper,
    error=feedback.error, onError=feedback.onError, errorContainer=feedback.errorContainer,
    onErrorContainer=feedback.onErrorContainer, surfaceTint=Color.Transparent,
    outline=ink.copy(alpha=.3f), outlineVariant=ink.copy(alpha=.12f))
}

@Composable fun ReaderPaperTheme(
    paper: Color = LocalReaderPaper.current?.paper ?: MaterialTheme.colorScheme.surface,
    ink: Color = LocalReaderPaper.current?.ink ?: MaterialTheme.colorScheme.onSurface,
    content: @Composable () -> Unit
) {
    val base=MaterialTheme.colorScheme
    val colors=remember(base,paper,ink) {readerPaperColorScheme(base,paper,ink)}
    MaterialTheme(colorScheme=colors) {
        // Transparent surfaces inherit this local, rather than contentColorFor(surface).
        CompositionLocalProvider(LocalContentColor provides ink,
            LocalReaderPaper provides ReaderPaperColors(paper,ink),content=content)
    }
}
val LocalReaderListeningDockHeight = staticCompositionLocalOf { 72.dp }
