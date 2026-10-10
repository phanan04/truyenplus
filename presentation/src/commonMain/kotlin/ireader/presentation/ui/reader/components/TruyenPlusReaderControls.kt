package ireader.presentation.ui.reader.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ireader.domain.preferences.models.ReaderColors
import ireader.presentation.core.toComposeColor
import ireader.i18n.localize
import ireader.i18n.resources.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TruyenPlusReaderControls(
    fontSize: Int,
    onFontSizeChange: (Int) -> Unit,
    themes: List<ReaderColors>,
    selectedThemeId: Long,
    onThemeChange: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            IconButton(onClick = { onFontSizeChange((fontSize - 1).coerceAtLeast(8)) }, enabled = fontSize > 8) {
                Text("Aa−", style = MaterialTheme.typography.bodyMedium)
            }
            Slider(
                value = fontSize.coerceIn(8, 40).toFloat(),
                onValueChange = { onFontSizeChange(it.toInt()) },
                valueRange = 8f..40f,
                modifier = Modifier.weight(1f),
                thumb = {
                    Surface(
                        modifier = Modifier.size(22.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        border = BorderStroke(2.dp, Color.White),
                        shadowElevation = 3.dp,
                    ) {}
                },
                track = { sliderState ->
                    LinearProgressIndicator(
                        progress = { ((sliderState.value - 8f) / 32f).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(3.dp).clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outlineVariant,
                        drawStopIndicator = {},
                    )
                },
            )
            IconButton(onClick = { onFontSizeChange((fontSize + 1).coerceAtMost(40)) }, enabled = fontSize < 40) {
                Text("Aa+", style = MaterialTheme.typography.bodyMedium)
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
            items(themes, key = { it.id }) { theme ->
                Surface(
                    onClick = { onThemeChange(theme.id) },
                    color = theme.backgroundColor.toComposeColor(),
                    contentColor = theme.onTextColor.toComposeColor(),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(if (theme.id == selectedThemeId) 2.dp else 1.dp,
                        if (theme.id == selectedThemeId) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.size(width = 48.dp, height = 40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("Aa", style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (theme.id == selectedThemeId) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
        Text(localize(Res.string.truyenplus_font_preview), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 6.dp))
    }
}
