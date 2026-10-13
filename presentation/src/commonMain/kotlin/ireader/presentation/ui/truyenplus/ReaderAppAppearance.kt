package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/** Flat reading controls; the navigation alone samples a blurred backdrop. */
@Composable
fun Modifier.readerPanel(radius: Int = 20): Modifier =
    clip(RoundedCornerShape(radius.dp)).background(MaterialTheme.colorScheme.surfaceContainerLow)
