package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
fun Modifier.readerPanel(radius: Int = 20, tint: Color = MaterialTheme.colorScheme.surface,
    ink: Color = MaterialTheme.colorScheme.onSurface): Modifier {
    val shape = RoundedCornerShape(radius.dp)
    return shadow(6.dp, shape).clip(shape)
        .readerNavigationGlass(tint, fallbackAlpha=.88f, blurTintAlpha=.78f)
        .border(1.dp,ink.copy(alpha=.12f),shape)
}

/** Scrolling cards use a static translucent treatment to avoid recursive sampling/large blurs. */
@Composable
fun Modifier.readerGlassCard(radius: Int = 20, tint: Color = MaterialTheme.colorScheme.surface,
    ink: Color = MaterialTheme.colorScheme.onSurface): Modifier {
    val shape=RoundedCornerShape(radius.dp)
    val sheen=Brush.linearGradient(listOf(tint.copy(alpha=.92f),tint.copy(alpha=.78f)))
    return clip(shape).background(sheen).border(1.dp,ink.copy(alpha=.1f),shape)
}
