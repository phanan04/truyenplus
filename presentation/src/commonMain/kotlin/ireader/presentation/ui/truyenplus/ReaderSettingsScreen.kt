package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassTextButton as TextButton

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
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
    ReaderDestination.Shelf -> Res.string.tp_shelf
})
fun ReaderAppearanceMode.localizedLabel(strings:LocalizeHelper):String = strings.localize(when(this) {
    ReaderAppearanceMode.System -> Res.string.tp_mode_system
    ReaderAppearanceMode.Light -> Res.string.tp_mode_light
    ReaderAppearanceMode.Dark -> Res.string.tp_mode_dark
})

@Composable
fun ReaderSettingsScreen(language:ReaderUiLanguage,onLanguage:(ReaderUiLanguage)->Unit,
    onProgress:()->Unit,onUpdate:()->Unit,appearance:ReaderAppearanceMode=ReaderAppearanceMode.System,
    onAppearance:(ReaderAppearanceMode)->Unit={},onImport:()->Unit={},onEditing:()->Unit={},onVoices:(()->Unit)?=null,onDismiss:(()->Unit)?=null,
    fullGlass:Boolean=LocalReaderFullGlass.current,onFullGlass:(Boolean)->Unit={}) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    var languageOpen by remember { mutableStateOf(false) }
    var appearanceOpen by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        if(onDismiss!=null)ReaderBooksSheetHeader(strings.localize(Res.string.tp_settings),onDismiss,closeTag="books-account-close")
        else ReaderBooksHeader(strings.localize(Res.string.tp_settings))
        LazyColumn(Modifier.weight(1f).fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally).widthIn(max=760.dp),
            contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(20.dp)) {
            item {
                Text(strings.localize(Res.string.tp_settings_general),Modifier.padding(bottom=8.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(Modifier.fillMaxWidth().readerOptionalGlass(14).testTag("settings-general"),shape=RoundedCornerShape(14.dp),color=readerOptionalGlassColor(MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsRow(strings.localize(Res.string.tp_language),language.displayName,Icons.Outlined.Language,{languageOpen=true})
                        HorizontalDivider(Modifier.padding(start=52.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
                        SettingsRow(strings.localize(Res.string.tp_appearance),appearance.localizedLabel(strings),Icons.Outlined.Contrast,{appearanceOpen=true})
                        HorizontalDivider(Modifier.padding(start=52.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
                        Row(Modifier.fillMaxWidth().heightIn(min=72.dp).toggleable(fullGlass,role=Role.Switch,onValueChange=onFullGlass)
                            .testTag("settings-full-glass").padding(horizontal=16.dp,vertical=12.dp),
                            verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(14.dp)) {
                            Icon(Icons.Outlined.Layers,null,Modifier.size(22.dp))
                            Column(Modifier.weight(1f)) {
                                Text(strings.localize(Res.string.tp_full_glass),style=MaterialTheme.typography.bodyLarge)
                                Text(strings.localize(Res.string.tp_full_glass_description),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Switch(fullGlass,onCheckedChange=null)
                        }
                    }
                }
            }
            item {
                Text(strings.localize(Res.string.tp_settings_read_listen),Modifier.padding(bottom=8.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(Modifier.fillMaxWidth().readerOptionalGlass(14),shape=RoundedCornerShape(14.dp),color=readerOptionalGlassColor(MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsRow(strings.localize(Res.string.tp_progress),"",Icons.Outlined.BarChart,onProgress)
                        if(onVoices!=null) {
                            HorizontalDivider(Modifier.padding(start=52.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
                            SettingsRow(strings.localize(Res.string.tp_voice_manager),"",Icons.Outlined.Mic,onVoices)
                        }
                    }
                }
            }
            item {
                Text(strings.localize(Res.string.tp_settings_tools),Modifier.padding(bottom=8.dp),style=MaterialTheme.typography.labelMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(Modifier.fillMaxWidth().readerOptionalGlass(14),shape=RoundedCornerShape(14.dp),color=readerOptionalGlassColor(MaterialTheme.colorScheme.surface)) {
                    Column {
                        SettingsRow(strings.localize(Res.string.tp_import_ebook),"",Icons.Outlined.FolderOpen,onImport)
                        HorizontalDivider(Modifier.padding(start=52.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.08f))
                        SettingsRow(strings.localize(Res.string.tp_edit_settings),"",Icons.Outlined.AutoFixHigh,onEditing)
                    }
                }
            }
            item {Surface(Modifier.fillMaxWidth().readerOptionalGlass(14),shape=RoundedCornerShape(14.dp),color=readerOptionalGlassColor(MaterialTheme.colorScheme.surface)) {
                SettingsRow(strings.localize(Res.string.tp_app_update),"Truyện+",Icons.Outlined.SystemUpdate,onUpdate)
            }}
        }
    }
    if(languageOpen) ReaderGlassDialog(onDismissRequest={languageOpen=false},title={Text(strings.localize(Res.string.tp_language))},
        text={Column { ReaderUiLanguage.entries.forEach { option ->
            Row(Modifier.fillMaxWidth().heightIn(min=56.dp).selectable(option==language,onClick={onLanguage(option);languageOpen=false},role=Role.RadioButton),verticalAlignment=Alignment.CenterVertically) {
                RadioButton(option==language,onClick=null);Text(option.displayName,Modifier.padding(start=8.dp))
            }
        } }},confirmButton={TextButton(onClick={languageOpen=false}) {Text(strings.localize(Res.string.tp_cancel))}})
    if(appearanceOpen) ReaderGlassDialog(onDismissRequest={appearanceOpen=false},title={Text(strings.localize(Res.string.tp_appearance))},
        text={ReaderAppearancePicker(appearance) {onAppearance(it);appearanceOpen=false}},
        confirmButton={TextButton(onClick={appearanceOpen=false}) {Text(strings.localize(Res.string.tp_cancel))}})
}

@Composable private fun SettingsRow(title:String,value:String,icon:ImageVector,onClick:()->Unit) = ReaderBooksActionRow(title,value,icon,onClick)

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
