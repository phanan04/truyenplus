package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

enum class ReaderUiLanguage(val code: String, val displayName: String) {
    Vietnamese("vi", "Tiếng Việt"), English("en", "English");
    companion object {
        fun fromCode(code: String) = if (code.substringBefore('-').substringBefore('_') == "vi") Vietnamese else English
    }
}

fun ReaderDestination.localizedLabel(strings: LocalizeHelper): String = strings.localize(when (this) {
    ReaderDestination.Home -> Res.string.tp_home
    ReaderDestination.Discover -> Res.string.tp_discover
    ReaderDestination.Library -> Res.string.tp_library
    ReaderDestination.Settings -> Res.string.tp_settings
})

@Composable
fun ReaderSettingsScreen(language: ReaderUiLanguage, onLanguage: (ReaderUiLanguage) -> Unit,
    onProgress: () -> Unit, onUpdate: () -> Unit,
    appearance: ReaderGlassTheme = LocalReaderGlassTheme.current, onAppearance: (ReaderGlassTheme) -> Unit = {}) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    LazyColumn(Modifier.fillMaxSize().wrapContentWidth(androidx.compose.ui.Alignment.CenterHorizontally).widthIn(max=760.dp), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = LocalReaderDockInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Column(Modifier.padding(vertical = 18.dp)) {
                Text(strings.localize(Res.string.tp_settings), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(strings.localize(Res.string.tp_settings_hint), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
            }
        }
        item { ReaderAppearancePicker(appearance,onAppearance) }
        item {
            Surface(modifier=Modifier.readerGlass(),shape = MaterialTheme.shapes.large, color = androidx.compose.ui.graphics.Color.Transparent) {
                Column(Modifier.padding(18.dp)) {
                    Text(strings.localize(Res.string.tp_language), style = MaterialTheme.typography.titleMedium)
                    Text(strings.localize(Res.string.tp_language_hint), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp, bottom = 12.dp))
                    ReaderUiLanguage.entries.forEach { option ->
                        Row(Modifier.fillMaxWidth().clickable { onLanguage(option) }.padding(vertical = 6.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                            RadioButton(selected = option == language, onClick = { onLanguage(option) })
                            Text(option.displayName, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        }
        item { SettingsAction(strings.localize(Res.string.tp_progress), strings.localize(Res.string.tp_progress_setting_hint), onProgress, false) }
        item { SettingsAction(strings.localize(Res.string.tp_app_update), "Truyện+", onUpdate, true) }
    }
}

@Composable
private fun SettingsAction(title: String, subtitle: String, onClick: () -> Unit, update: Boolean) {
    Surface(onClick = onClick,modifier=Modifier.readerGlass(), shape = MaterialTheme.shapes.large, color = androidx.compose.ui.graphics.Color.Transparent) {
        ListItem(headlineContent = { Text(title) }, supportingContent = { Text(subtitle) },
            leadingContent = { Icon(if (update) Icons.Outlined.SystemUpdate else Icons.Outlined.BarChart, null) },
            trailingContent = { Icon(Icons.Outlined.ChevronRight, null) },
            colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent))
    }
}

@Composable
fun ReaderAppearancePicker(selected: ReaderGlassTheme, onSelect: (ReaderGlassTheme)->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    Column(Modifier.fillMaxWidth().readerGlass().padding(18.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Text(s.localize(Res.string.tp_appearance),style=MaterialTheme.typography.titleMedium)
        Text(s.localize(Res.string.tp_glass_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        for(theme in ReaderGlassTheme.entries) {
            val label=s.localize(when(theme) {
                ReaderGlassTheme.Violet->Res.string.tp_glass_violet
                ReaderGlassTheme.Pearl->Res.string.tp_glass_pearl
                ReaderGlassTheme.Graphite->Res.string.tp_glass_graphite
                ReaderGlassTheme.Amber->Res.string.tp_glass_amber
            })
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(selected==theme,onClick={onSelect(theme)},role=androidx.compose.ui.semantics.Role.RadioButton).testTag("appearance-${theme.id}"),verticalAlignment=androidx.compose.ui.Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).background(theme.colors.background,androidx.compose.foundation.shape.CircleShape).padding(8.dp)) {
                    Box(Modifier.fillMaxSize().background(theme.colors.primary,androidx.compose.foundation.shape.CircleShape))
                }
                Text(label,Modifier.weight(1f).padding(horizontal=12.dp))
                RadioButton(selected==theme,onClick=null)
            }
        }
    }
}
