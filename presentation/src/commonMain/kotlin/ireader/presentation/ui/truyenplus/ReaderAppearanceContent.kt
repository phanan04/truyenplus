package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import ireader.domain.models.common.FontFamilyModel
import ireader.domain.models.prefs.PreferenceValues
import ireader.domain.preferences.models.ReaderColors
import ireader.domain.preferences.prefs.ReadingMode
import ireader.presentation.core.toComposeColor
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.i18n.resources.*
import kotlin.math.roundToInt

internal val ReaderControlAccent = Color(0xFFB78164)

data class ReaderAppearanceUi(val fontSize: Int = 20, val lineHeight: Int = 30, val margin: Int = 16,
    val scrollSpeed: Int = 10, val scrollEnabled: Boolean = false,
    val themes: List<ReaderColors> = emptyList(), val themeId: Long = 0,
    val alignment: PreferenceValues.PreferenceTextAlignment = PreferenceValues.PreferenceTextAlignment.Left,
    val mode: ReadingMode = ReadingMode.Continues, val fontFamily: FontFamilyModel = FontFamilyModel.Default,
    val listening: Boolean = false)

data class ReaderAppearanceActions(val onFontSize: (Int) -> Unit = {}, val onLineHeight: (Int) -> Unit = {},
    val onMargin: (Int) -> Unit = {}, val onScrollSpeed: (Int) -> Unit = {}, val onScrollEnabled: (Boolean) -> Unit = {},
    val onTheme: (Long) -> Unit = {}, val onAlignment: (PreferenceValues.PreferenceTextAlignment) -> Unit = {},
    val onMode: (ReadingMode) -> Unit = {}, val onFont: (FontFamilyModel) -> Unit = {}, val onMore: () -> Unit = {})

@Composable
fun ReaderAppearanceContent(state: ReaderAppearanceUi, actions: ReaderAppearanceActions, modifier: Modifier = Modifier) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    Column(modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.localize(Res.string.auto_scroll), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(state.scrollEnabled, actions.onScrollEnabled, modifier = Modifier.testTag("reader-scroll-toggle"),
                colors = SwitchDefaults.colors(checkedTrackColor = ReaderControlAccent, checkedThumbColor = Color.White))
        }
        if (!state.listening) NumericAdjuster(s.localize(Res.string.tp_reader_scroll_speed), state.scrollSpeed, 1, 20, actions.onScrollSpeed, "reader-scroll")
        LazyRowThemes(state, actions.onTheme)
        Levels(s.localize(Res.string.tp_reader_line_spacing), ((state.lineHeight-state.fontSize)/2f).roundToInt().coerceIn(1,7), 7, "reader-line") { actions.onLineHeight(state.fontSize + it*2) }
        Levels(s.localize(Res.string.tp_reader_margins), (state.margin/8f).roundToInt().coerceIn(1,4), 4, "reader-margin") { actions.onMargin(it*8) }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            for ((align, icon, label) in listOf(
                Triple(PreferenceValues.PreferenceTextAlignment.Left, Icons.AutoMirrored.Filled.FormatAlignLeft, s.localize(Res.string.text_align_left)),
                Triple(PreferenceValues.PreferenceTextAlignment.Justify, Icons.Default.FormatAlignJustify, s.localize(Res.string.text_align_justify)))) {
                val selected = state.alignment == align
                OutlinedIconButton(onClick = { actions.onAlignment(align) }, border = BorderStroke(1.dp, if (selected) ReaderControlAccent else Color.Transparent),
                    colors = IconButtonDefaults.outlinedIconButtonColors(contentColor = if (selected) ReaderControlAccent else LocalContentColor.current),
                    modifier = Modifier.testTag(if(align==PreferenceValues.PreferenceTextAlignment.Left) "reader-align-left" else "reader-align-justify")) { Icon(icon, label) }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { actions.onFontSize((state.fontSize-1).coerceAtLeast(8)) }, enabled = state.fontSize>8, modifier = Modifier.testTag("reader-font-decrease")) { Text("A−") }
            Text(state.fontSize.toString(), style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { actions.onFontSize((state.fontSize+1).coerceAtMost(40)) }, enabled = state.fontSize<40, modifier = Modifier.testTag("reader-font-increase")) { Text("A+") }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for ((mode, label, tag) in listOf(Triple(ReadingMode.Continues,s.localize(Res.string.tp_reader_scroll),"reader-mode-scroll"), Triple(ReadingMode.Page,s.localize(Res.string.tp_reader_pages),"reader-mode-page"))) {
                Choice(label, state.mode == mode || mode==ReadingMode.Continues && state.mode==ReadingMode.InfiniteScroll,
                    { actions.onMode(mode) }, Modifier.weight(1f).testTag(tag), enabled = !state.listening)
            }
        }
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val fonts = listOf(
                Triple(FontFamilyModel.Default,s.localize(Res.string.tp_reader_default_font),"default"),
                Triple(FontFamilyModel.SansSerif,"Sans serif","sans"), Triple(FontFamilyModel.Cursive,"Cursive","cursive"), Triple(FontFamilyModel.Monospace,"Monospace","mono"))
            for ((font,label,tag) in fonts) Choice(label, state.fontFamily==font, { actions.onFont(font) }, Modifier.testTag("reader-font-$tag"),
                fontFamily = when(font) { FontFamilyModel.Cursive -> FontFamily.Cursive; FontFamilyModel.Monospace -> FontFamily.Monospace; else -> FontFamily.Default })
        }
        TextButton(onClick = actions.onMore, Modifier.align(Alignment.End)) { Text(s.localize(Res.string.tp_reader_more_options), color = ReaderControlAccent) }
    }
}

@Composable private fun LazyRowThemes(state: ReaderAppearanceUi, select: (Long) -> Unit) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        state.themes.forEachIndexed { index, theme ->
            Surface(onClick = { select(theme.id) }, shape = CircleShape, color = theme.backgroundColor.toComposeColor(), contentColor = theme.onTextColor.toComposeColor(),
                border = BorderStroke(if (state.themeId==theme.id) 2.dp else 1.dp, if (state.themeId==theme.id) ReaderControlAccent else theme.onTextColor.toComposeColor().copy(alpha=.5f)),
                modifier = Modifier.size(48.dp).testTag("reader-theme-${theme.id}").semantics { selected = state.themeId==theme.id; contentDescription = "${s.localize(Res.string.tp_background_tab)} ${index+1}" }) {
                Box(contentAlignment = Alignment.Center) { Text("T", style = MaterialTheme.typography.titleLarge) }
            }
        }
    }
}

@Composable private fun Levels(title: String, selected: Int, count: Int, tag: String, change: (Int) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            for (level in 1..count) Choice(level.toString(),selected==level,{change(level)},Modifier.weight(1f).testTag("$tag-$level"), padding = 0)
        }
    }
}

@Composable private fun Choice(text: String, selected: Boolean, click: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, fontFamily: FontFamily? = null, padding: Int = 12) {
    Surface(onClick = click, enabled = enabled, modifier = modifier.heightIn(min=44.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp), color = Color.Transparent, contentColor = if (selected) ReaderControlAccent else LocalContentColor.current,
        border = BorderStroke(1.dp, if(selected) ReaderControlAccent else Color.Transparent)) {
        Box(Modifier.padding(horizontal = padding.dp), contentAlignment = Alignment.Center) { Text(text, fontFamily = fontFamily, style = MaterialTheme.typography.bodyLarge) }
    }
}

@Composable private fun NumericAdjuster(label: String, value: Int, min: Int, max: Int, change: (Int)->Unit, tag: String) {
    Row(Modifier.fillMaxWidth(),verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick={change((value-1).coerceAtLeast(min))},enabled=value>min,modifier=Modifier.testTag("$tag-decrease")) { Text("−") }
        Text(value.toString(),style=MaterialTheme.typography.titleMedium)
        IconButton(onClick={change((value+1).coerceAtMost(max))},enabled=value<max,modifier=Modifier.testTag("$tag-increase")) { Text("+") }
    }
}
