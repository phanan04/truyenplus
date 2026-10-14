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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.lerp
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

internal val ReaderControlAccent: Color @Composable get() {
    val c=MaterialTheme.colorScheme
    val ink=LocalContentColor.current
    if(ink!=c.onSurface && ink!=c.onBackground && ink!=Color.Unspecified) return ink
    val a=c.primary.luminance();val b=c.surface.luminance()
    return if((maxOf(a,b)+.05f)/(minOf(a,b)+.05f)<3f) lerp(c.primary,c.onSurface,.7f) else c.primary
}

data class ReaderAppearanceUi(val fontSize: Int = 20, val lineHeight: Int = 30, val margin: Int = 16,
    val scrollSpeed: Int = 10, val scrollEnabled: Boolean = false,
    val themes: List<ReaderColors> = emptyList(), val themeId: Long = 0,
    val alignment: PreferenceValues.PreferenceTextAlignment = PreferenceValues.PreferenceTextAlignment.Left,
    val mode: ReadingMode = ReadingMode.Continues, val fontFamily: FontFamilyModel = FontFamilyModel.Default,
    val listening: Boolean = false, val brightness: Float = .5f, val autoBrightness: Boolean = true,
    val followAppearance: Boolean = true, val paragraphSpacing: Int = 4)

data class ReaderAppearanceActions(val onFontSize: (Int) -> Unit = {}, val onLineHeight: (Int) -> Unit = {},
    val onMargin: (Int) -> Unit = {}, val onScrollSpeed: (Int) -> Unit = {}, val onScrollEnabled: (Boolean) -> Unit = {},
    val onTheme: (Long) -> Unit = {}, val onAlignment: (PreferenceValues.PreferenceTextAlignment) -> Unit = {},
    val onMode: (ReadingMode) -> Unit = {}, val onFont: (FontFamilyModel) -> Unit = {}, val onBrightness: (Float) -> Unit = {},
    val onAutoBrightness: (Boolean) -> Unit = {}, val onFollowAppearance: (Boolean) -> Unit = {}, val onParagraphSpacing: (Int) -> Unit = {})

@Composable
fun ReaderAppearanceContent(state: ReaderAppearanceUi, actions: ReaderAppearanceActions, modifier: Modifier = Modifier) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var page by remember { mutableIntStateOf(0) }
    Column(modifier.fillMaxWidth()) {
        ReaderSettingsTabs(listOf(s.localize(Res.string.tp_settings_text),s.localize(Res.string.tp_settings_spacing),s.localize(Res.string.tp_settings_colors),s.localize(Res.string.tp_settings_scroll)),
            listOf("text","spacing","colors","scroll"),page,{page=it},"appearance-page")
        BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max=208.dp).padding(horizontal=16.dp,vertical=4.dp)) {
            val shortWide=maxWidth>=440.dp && maxHeight<192.dp
            when(page) {
                0 -> Column {
                    var fontsOpen by remember { mutableStateOf(false) }
                    val fonts=listOf(FontFamilyModel.Default,FontFamilyModel.Serif,FontFamilyModel.SansSerif)
                    fun label(font:FontFamilyModel)=if(font is FontFamilyModel.Custom) font.name else s.localize(when(font){FontFamilyModel.Serif->Res.string.tp_system_serif;FontFamilyModel.SansSerif->Res.string.tp_system_sans;else->Res.string.tp_reader_default_font})
                    Box {
                        OutlinedButton(onClick={fontsOpen=true},modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("reader-font-picker")) {
                            Text(label(state.fontFamily),Modifier.weight(1f),maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        DropdownMenu(fontsOpen,onDismissRequest={fontsOpen=false}) {
                            fonts.forEach { font-> DropdownMenuItem(text={Text(label(font),fontFamily=when(font){FontFamilyModel.Serif->FontFamily.Serif;FontFamilyModel.SansSerif->FontFamily.SansSerif;else->FontFamily.Default})},
                                modifier=Modifier.testTag("reader-font-${when(font){FontFamilyModel.Serif->"serif";FontFamilyModel.SansSerif->"sans";else->"default"}}"),onClick={actions.onFont(font);fontsOpen=false}) }
                        }
                    }
                    Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                        for((align,icon,tag) in listOf(Triple(PreferenceValues.PreferenceTextAlignment.Left,Icons.AutoMirrored.Filled.FormatAlignLeft,"left"),Triple(PreferenceValues.PreferenceTextAlignment.Justify,Icons.Default.FormatAlignJustify,"justify"))) {
                            OutlinedIconButton(onClick={actions.onAlignment(align)},modifier=Modifier.size(48.dp).testTag("reader-align-$tag"),border=BorderStroke(1.dp,if(state.alignment==align)ReaderControlAccent else Color.Transparent)) {Icon(icon,s.localize(if(tag=="left")Res.string.text_align_left else Res.string.text_align_justify))}
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick={actions.onFontSize((state.fontSize-1).coerceAtLeast(8))},enabled=state.fontSize>8,modifier=Modifier.size(48.dp).testTag("reader-font-decrease").semantics {contentDescription=s.localize(Res.string.decrease_font_size)}){Text("A−")}
                        Text(state.fontSize.toString(),style=MaterialTheme.typography.titleMedium)
                        IconButton(onClick={actions.onFontSize((state.fontSize+1).coerceAtMost(40))},enabled=state.fontSize<40,modifier=Modifier.size(48.dp).testTag("reader-font-increase").semantics {contentDescription=s.localize(Res.string.increase_font_size)}){Text("A+")}
                    }
                }
                1 -> {
                    val lines:@Composable ()->Unit={
                        NumericAdjuster(s.localize(Res.string.tp_reader_line_spacing),((state.lineHeight-state.fontSize)/2f).roundToInt().coerceIn(1,10),1,10,{actions.onLineHeight(state.fontSize+it*2)},"reader-line")
                    }
                    val paragraphs:@Composable ()->Unit={
                        NumericAdjuster(s.localize(Res.string.tp_reader_paragraph_spacing),state.paragraphSpacing.coerceIn(0,10),0,10,actions.onParagraphSpacing,"reader-paragraph")
                    }
                    val margins:@Composable ()->Unit={
                        NumericAdjuster(s.localize(Res.string.tp_reader_margins),(state.margin/8f).roundToInt().coerceIn(1,10),1,10,{actions.onMargin(it*8)},"reader-margin")
                    }
                    val modes:@Composable ()->Unit={
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            Choice(s.localize(Res.string.tp_reader_scroll),state.mode!=ReadingMode.Page,{actions.onMode(ReadingMode.Continues)},Modifier.weight(1f).testTag("reader-mode-scroll"),enabled=!state.listening)
                            Choice(s.localize(Res.string.tp_reader_pages),state.mode==ReadingMode.Page,{actions.onMode(ReadingMode.Page)},Modifier.weight(1f).testTag("reader-mode-page"),enabled=!state.listening)
                        }
                    }
                    if(shortWide) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                        Column(Modifier.weight(1f)){lines();paragraphs()}
                        Column(Modifier.weight(1f)){margins();modes()}
                    } else Column {lines();paragraphs();margins();modes()}
                }
                2 -> {
                    val themeControls:@Composable ()->Unit={
                        LazyRowThemes(state,actions.onTheme)
                        ReaderSettingSwitch(s.localize(Res.string.tp_follow_appearance),state.followAppearance,actions.onFollowAppearance)
                    }
                    val brightnessControls:@Composable ()->Unit={
                        ReaderSettingSwitch(s.localize(Res.string.tp_reader_auto_brightness),state.autoBrightness,actions.onAutoBrightness)
                        Slider(state.brightness.coerceIn(0f,.5f),actions.onBrightness,valueRange=0f.. .5f,enabled=!state.autoBrightness,modifier=Modifier.height(48.dp).semantics {contentDescription=s.localize(Res.string.tp_reader_brightness)})
                    }
                    if(shortWide) Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {Column(Modifier.weight(1f)){themeControls()};Column(Modifier.weight(1f)){brightnessControls()}}
                    else Column {themeControls();brightnessControls()}
                }
                else -> Column {
                    ReaderSettingSwitch(s.localize(Res.string.auto_scroll),state.scrollEnabled,actions.onScrollEnabled,Modifier.testTag("reader-scroll-toggle"))
                    if(!state.listening) NumericAdjuster(s.localize(Res.string.tp_reader_scroll_speed),state.scrollSpeed,1,20,actions.onScrollSpeed,"reader-scroll")
                }
            }
        }
    }
}

@Composable internal fun ReaderSettingsTabs(labels:List<String>,ids:List<String>,selected:Int,onSelect:(Int)->Unit,tag:String,enabled:List<Boolean> = List(labels.size){true}) {
    Row(Modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=4.dp),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
        labels.forEachIndexed { i,label->
            Surface(onClick={onSelect(i)},enabled=enabled[i],shape=RoundedCornerShape(12.dp),color=if(selected==i)LocalContentColor.current.copy(alpha=.09f) else Color.Transparent,
                modifier=Modifier.weight(1f).height(48.dp).testTag("$tag-${ids[i]}").semantics {this.selected=selected==i;contentDescription=label}) {
                Box(Modifier.padding(horizontal=4.dp),contentAlignment=Alignment.Center){Text(label,style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)}
            }
        }
    }
}
@Composable internal fun ReaderSettingSwitch(label:String,checked:Boolean,change:(Boolean)->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
    Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
        Text(label,Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        Switch(checked,change,enabled=enabled,modifier=modifier.semantics {contentDescription=label})
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

@Composable private fun Choice(text: String, selected: Boolean, click: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, fontFamily: FontFamily? = null, padding: Int = 12) {
    Surface(onClick = click, enabled = enabled, modifier = modifier.heightIn(min=48.dp).semantics { this.selected = selected },
        shape = RoundedCornerShape(12.dp), color = Color.Transparent, contentColor = if (selected) ReaderControlAccent else LocalContentColor.current,
        border = BorderStroke(1.dp, if(selected) ReaderControlAccent else Color.Transparent)) {
        Box(Modifier.padding(horizontal = padding.dp), contentAlignment = Alignment.Center) { Text(text, fontFamily = fontFamily, style = MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis) }
    }
}

@Composable private fun NumericAdjuster(label: String, value: Int, min: Int, max: Int, change: (Int)->Unit, tag: String) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        IconButton(onClick={change((value-1).coerceAtLeast(min))},enabled=value>min,modifier=Modifier.size(48.dp).testTag("$tag-decrease").semantics {contentDescription=s.localize(Res.string.tp_decrease_value,label)}) { Text("−") }
        Text(value.toString(),style=MaterialTheme.typography.titleMedium)
        IconButton(onClick={change((value+1).coerceAtMost(max))},enabled=value<max,modifier=Modifier.size(48.dp).testTag("$tag-increase").semantics {contentDescription=s.localize(Res.string.tp_increase_value,label)}) { Text("+") }
    }
}
