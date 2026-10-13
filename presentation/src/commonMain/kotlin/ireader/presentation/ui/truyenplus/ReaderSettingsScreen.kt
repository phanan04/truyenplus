package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

enum class ReaderUiLanguage(val code: String, val displayName: String) {
    Vietnamese("vi", "Tiếng Việt"), English("en", "English");
    companion object { fun fromCode(code:String)=if(code.substringBefore('-').substringBefore('_')=="vi") Vietnamese else English }
}
fun ReaderDestination.localizedLabel(strings:LocalizeHelper):String = strings.localize(when(this) {
    ReaderDestination.Home -> Res.string.tp_home
    ReaderDestination.Library -> Res.string.tp_library
    ReaderDestination.Settings -> Res.string.tp_settings
})
fun ReaderAppearanceMode.localizedLabel(strings:LocalizeHelper):String = strings.localize(when(this) {
    ReaderAppearanceMode.System -> Res.string.tp_mode_system
    ReaderAppearanceMode.Light -> Res.string.tp_mode_light
    ReaderAppearanceMode.Dark -> Res.string.tp_mode_dark
})

@Composable
fun ReaderSettingsScreen(language:ReaderUiLanguage,onLanguage:(ReaderUiLanguage)->Unit,
    onProgress:()->Unit,onUpdate:()->Unit,appearance:ReaderAppearanceMode=ReaderAppearanceMode.System,
    onAppearance:(ReaderAppearanceMode)->Unit={},onImport:()->Unit={},onEditing:()->Unit={}) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    var languageOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max=760.dp),
        contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=LocalReaderDockInset.current),verticalArrangement=Arrangement.spacedBy(20.dp)) {
        item { Text(strings.localize(Res.string.tp_settings),Modifier.padding(top=24.dp,bottom=8.dp),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold) }
        item {
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
                Column {
                    SettingsRow(strings.localize(Res.string.tp_language),language.displayName,Icons.Outlined.Language,{languageOpen=true})
                    HorizontalDivider(Modifier.padding(start=56.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
                    SettingsRow(strings.localize(Res.string.tp_appearance),appearance.localizedLabel(strings),Icons.Outlined.Contrast,{appearanceOpen=true})
                }
            }
        }
        item {
            Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
                Column {
                    SettingsRow(strings.localize(Res.string.tp_import_ebook),strings.localize(Res.string.tp_import_formats),Icons.Outlined.FolderOpen,onImport)
                    HorizontalDivider(Modifier.padding(start=56.dp),color=MaterialTheme.colorScheme.outlineVariant.copy(alpha=.5f))
                    SettingsRow(strings.localize(Res.string.tp_progress),strings.localize(Res.string.tp_progress_setting_hint),Icons.Outlined.BarChart,onProgress)
                }
            }
        }
        item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
            SettingsRow(strings.localize(Res.string.tp_edit_settings),strings.localize(Res.string.tp_edit_settings_hint),Icons.Outlined.AutoFixHigh,onEditing)
        } }
        item { Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surface) {
            SettingsRow(strings.localize(Res.string.tp_app_update),"Truyện+",Icons.Outlined.SystemUpdate,onUpdate)
        } }
    }
    if(languageOpen) AlertDialog(onDismissRequest={languageOpen=false},title={Text(strings.localize(Res.string.tp_language))},
        text={Column { ReaderUiLanguage.entries.forEach { option ->
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(option==language,onClick={onLanguage(option);languageOpen=false},role=Role.RadioButton),verticalAlignment=Alignment.CenterVertically) {
                RadioButton(option==language,onClick=null);Text(option.displayName,Modifier.padding(start=8.dp))
            }
        } }},confirmButton={TextButton(onClick={languageOpen=false}) {Text(strings.localize(Res.string.tp_cancel))}})
    if(appearanceOpen) AlertDialog(onDismissRequest={appearanceOpen=false},title={Text(strings.localize(Res.string.tp_appearance))},
        text={ReaderAppearancePicker(appearance) {onAppearance(it);appearanceOpen=false}},
        confirmButton={TextButton(onClick={appearanceOpen=false}) {Text(strings.localize(Res.string.tp_cancel))}})
}

@Composable private fun SettingsRow(title:String,value:String,icon:ImageVector,onClick:()->Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick=onClick).heightIn(min=68.dp).padding(horizontal=16.dp,vertical=14.dp),
        verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
        Icon(icon,null,Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title,style=MaterialTheme.typography.bodyLarge)
            Text(value,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.padding(top=3.dp))
        }
        Icon(Icons.Outlined.ChevronRight,null,Modifier.size(18.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable fun ReaderAppearancePicker(selected:ReaderAppearanceMode,onSelect:(ReaderAppearanceMode)->Unit) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    Column {
        ReaderAppearanceMode.entries.forEach { mode ->
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(selected==mode,onClick={onSelect(mode)},role=Role.RadioButton).testTag("appearance-${mode.id}"),verticalAlignment=Alignment.CenterVertically) {
                RadioButton(selected==mode,onClick=null)
                Text(mode.localizedLabel(strings),Modifier.padding(start=8.dp))
            }
        }
    }
}
