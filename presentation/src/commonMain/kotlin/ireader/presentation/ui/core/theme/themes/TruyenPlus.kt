package ireader.presentation.ui.core.theme.themes

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** Neutral, content-first palette shared by Android and Desktop. */
val TruyenPlusLightColors = lightColorScheme(
    primary = Color(0xFF202020), onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E8E8), onPrimaryContainer = Color(0xFF202020),
    secondary = Color(0xFF555555), onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0F0F0), onSecondaryContainer = Color(0xFF333333),
    tertiary = Color(0xFF555555), onTertiary = Color.White,
    background = Color.White, onBackground = Color(0xFF202020),
    surface = Color.White, onSurface = Color(0xFF202020),
    surfaceVariant = Color(0xFFF0F0F0), onSurfaceVariant = Color(0xFF666666),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8F8F8),
    surfaceContainer = Color(0xFFF4F4F4), surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Color(0xFFE4E4E4),
    outline = Color(0xFF8A8A8A), outlineVariant = Color(0xFFE0E0E0),
    surfaceTint = Color.Transparent,
)

val TruyenPlusDarkColors = darkColorScheme(
    primary = Color(0xFFECECEC), onPrimary = Color(0xFF202020),
    primaryContainer = Color(0xFF383838), onPrimaryContainer = Color(0xFFF2F2F2),
    secondary = Color(0xFFC4C4C4), onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF303030), onSecondaryContainer = Color(0xFFECECEC),
    tertiary = Color(0xFFCCCCCC), onTertiary = Color(0xFF202020),
    background = Color.Black, onBackground = Color(0xFFECECEC),
    surface = Color(0xFF1B1B1D), onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF363636), onSurfaceVariant = Color(0xFFB4B4B4),
    surfaceContainerLowest = Color(0xFF181818), surfaceContainerLow = Color(0xFF242424),
    surfaceContainer = Color(0xFF2F2F2F), surfaceContainerHigh = Color(0xFF363636),
    surfaceContainerHighest = Color(0xFF414141),
    outline = Color(0xFF858585), outlineVariant = Color(0xFF414141),
    surfaceTint = Color.Transparent,
)
