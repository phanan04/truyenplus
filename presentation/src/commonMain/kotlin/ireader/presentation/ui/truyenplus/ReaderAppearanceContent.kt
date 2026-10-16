package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedButton as OutlinedButton
import ireader.presentation.ui.truyenplus.ReaderGlassTextButton as TextButton
import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton

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

/** The opening surface mirrors Books: quick type, brightness and papers; detailed controls are one step deeper. */
@Composable
fun ReaderAppearanceContent(state:ReaderAppearanceUi,actions:ReaderAppearanceActions,modifier:Modifier=Modifier) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var customizing by remember {mutableStateOf(false)}
    var options by remember {mutableStateOf(false)}
    ireader.presentation.core.ui.PlatformBackHandler(customizing || options){customizing=false;options=false}
    Column(modifier.fillMaxWidth()) {
        if(customizing) {
            Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton({customizing=false},Modifier.size(48.dp).testTag("reader-appearance-home-back")){Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_back))}
                Text(s.localize(Res.string.tp_reader_customize),style=MaterialTheme.typography.titleMedium)
            }
            ReaderAppearanceAdvancedContent(state,actions)
        } else if(options)Column(Modifier.padding(horizontal=16.dp,vertical=4.dp)) {
            Row(Modifier.heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                IconButton({options=false},Modifier.size(48.dp).testTag("reader-paper-options-back")){Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_back))}
                Text(s.localize(Res.string.tp_reader_brightness),style=MaterialTheme.typography.titleMedium)
            }
            ReaderSettingSwitch(s.localize(Res.string.tp_follow_appearance),state.followAppearance,actions.onFollowAppearance)
            ReaderSettingSwitch(s.localize(Res.string.tp_reader_auto_brightness),state.autoBrightness,actions.onAutoBrightness)
            if(!state.autoBrightness)Slider(state.brightness.coerceIn(0f,.5f),actions.onBrightness,valueRange=0f.. .5f,modifier=Modifier.height(48.dp))
        } else BoxWithConstraints(Modifier.fillMaxWidth().heightIn(max=420.dp).padding(horizontal=18.dp).testTag("reader-appearance-home")) {
            val short=maxHeight<360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale>=1.5f
            val papers=remember(state.themes,state.themeId) {
                val order=listOf(-2L,-3L,-5L,-1L,-11L,-7L)
                val quick=state.themes.sortedBy {order.indexOf(it.id).takeIf {i->i>=0} ?: (order.size+state.themes.indexOf(it))}.take(6)
                val selected=state.themes.firstOrNull {it.id==state.themeId}
                if(selected!=null && quick.none {it.id==selected.id})quick.take(5)+selected else quick
            }
            val optionWidth=if(maxWidth>=350.dp)112.dp else 96.dp
            val sizeControls:@Composable ()->Unit={
                Row(Modifier.fillMaxWidth().height(48.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                    Surface(shape=CircleShape,color=LocalContentColor.current.copy(alpha=.07f),modifier=Modifier.weight(1f)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            IconButton({actions.onFontSize((state.fontSize-1).coerceAtLeast(8))},enabled=state.fontSize>8,
                                modifier=Modifier.weight(1f).height(48.dp).testTag("reader-font-decrease").semantics {contentDescription=s.localize(Res.string.decrease_font_size)}) {Text("A",style=MaterialTheme.typography.bodyMedium)}
                            VerticalDivider(Modifier.height(32.dp),color=LocalContentColor.current.copy(alpha=.12f))
                            IconButton({actions.onFontSize((state.fontSize+1).coerceAtMost(40))},enabled=state.fontSize<40,
                                modifier=Modifier.weight(1f).height(48.dp).testTag("reader-font-increase").semantics {contentDescription=s.localize(Res.string.increase_font_size)}) {Text("A",style=MaterialTheme.typography.titleLarge)}
                        }
                    }
                    Surface(shape=CircleShape,color=LocalContentColor.current.copy(alpha=.07f),modifier=Modifier.width(optionWidth)) {
                        Row {
                            IconButton({customizing=true},Modifier.weight(1f).height(48.dp)){Icon(Icons.Default.AutoStories,s.localize(Res.string.tp_reader_customize),Modifier.size(22.dp))}
                            IconButton({options=true},Modifier.weight(1f).height(48.dp).testTag("reader-paper-options")){Icon(Icons.Default.Contrast,s.localize(Res.string.tp_appearance),Modifier.size(22.dp))}
                        }
                    }
                }
            }
            val brightness:@Composable ()->Unit={
                Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.BrightnessLow,null,Modifier.size(18.dp))
                    Slider(state.brightness.coerceIn(0f,.5f),{if(state.autoBrightness)actions.onAutoBrightness(false);actions.onBrightness(it)},
                        valueRange=0f.. .5f,modifier=Modifier.weight(1f).semantics {contentDescription=s.localize(Res.string.tp_reader_brightness)})
                    Icon(Icons.Default.BrightnessHigh,null,Modifier.size(18.dp))
                }
            }
            val customize:@Composable ()->Unit={
                Surface(onClick={customizing=true},modifier=Modifier.fillMaxWidth().height(48.dp).testTag("reader-appearance-customize"),
                    shape=CircleShape,color=LocalContentColor.current.copy(alpha=.07f)) {
                    Row(horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Tune,null,Modifier.size(20.dp));Spacer(Modifier.width(8.dp))
                        Text(s.localize(Res.string.tp_reader_customize),style=MaterialTheme.typography.titleSmall,maxLines=1)
                    }
                }
            }
            if(short && maxWidth>=440.dp)Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                Column(Modifier.weight(.48f),verticalArrangement=Arrangement.spacedBy(8.dp)){sizeControls();brightness();customize()}
                Box(Modifier.weight(.52f)){ReaderThemePalette(papers,state.themeId,actions.onTheme,pageSize=3,gap=0.dp)}
            } else Column(verticalArrangement=Arrangement.spacedBy(10.dp)) {
                sizeControls();if(!short)brightness()
                HorizontalDivider(color=LocalContentColor.current.copy(alpha=.08f))
                ReaderThemePalette(papers,state.themeId,actions.onTheme,pageSize=if(short)3 else 6,gap=10.dp,namedCards=!short)
                customize()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
internal fun ReaderAppearanceAdvancedContent(state: ReaderAppearanceUi, actions: ReaderAppearanceActions, modifier: Modifier = Modifier) {
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
                        ReaderGlassDropdownMenu(fontsOpen,onDismissRequest={fontsOpen=false}) {
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
                    var options by remember {mutableStateOf(false)}
                    ireader.presentation.core.ui.PlatformBackHandler(options){options=false}
                    if(options)Column {
                        Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
                            IconButton(onClick={options=false},modifier=Modifier.size(48.dp).testTag("reader-paper-options-back")) {Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_back))}
                            Text(s.localize(Res.string.tp_reader_brightness),style=MaterialTheme.typography.titleMedium)
                        }
                        ReaderSettingSwitch(s.localize(Res.string.tp_follow_appearance),state.followAppearance,actions.onFollowAppearance)
                        ReaderSettingSwitch(s.localize(Res.string.tp_reader_auto_brightness),state.autoBrightness,actions.onAutoBrightness)
                        if(!state.autoBrightness)Slider(state.brightness.coerceIn(0f,.5f),actions.onBrightness,valueRange=0f.. .5f,
                            modifier=Modifier.height(48.dp).semantics {contentDescription=s.localize(Res.string.tp_reader_brightness)})
                    } else ReaderThemePalette(state.themes,state.themeId,actions.onTheme) {
                        TextButton(onClick={options=true},modifier=Modifier.heightIn(min=48.dp).testTag("reader-paper-options")) {
                            Icon(Icons.Default.Brightness6,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp));Text(s.localize(Res.string.tp_reader_brightness))
                        }
                    }
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
            Surface(onClick={onSelect(i)},enabled=enabled[i],shape=RoundedCornerShape(12.dp),color=if(selected==i && !LocalReaderFullGlass.current)LocalContentColor.current.copy(alpha=.09f) else Color.Transparent,
                modifier=Modifier.weight(1f).readerControlGlass(RoundedCornerShape(12.dp),enabled[i],selected==i).height(48.dp).testTag("$tag-${ids[i]}").semantics {this.selected=selected==i;contentDescription=label}) {
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

/** All page papers stay visible, like the Apple Books theme palette, without a horizontal hunt. */
@Composable internal fun ReaderThemePalette(themes:List<ReaderColors>,selectedId:Long,select:(Long)->Unit,
    pageSize:Int=8,gap:androidx.compose.ui.unit.Dp=8.dp,namedCards:Boolean=false,controls:(@Composable RowScope.()->Unit)?=null) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    val pages=themes.chunked(pageSize)
    val columns=if(pageSize==8)4 else 3
    var page by remember(themes,selectedId,pageSize) {mutableIntStateOf((themes.indexOfFirst {it.id==selectedId}.coerceAtLeast(0)/pageSize))}
    Column(verticalArrangement=Arrangement.spacedBy(gap)) {
        pages.getOrNull(page).orEmpty().chunked(columns).forEach {row->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                row.forEach {theme->
                    Surface(onClick={select(theme.id)},shape=RoundedCornerShape(if(namedCards)20.dp else 14.dp),
                        color=theme.backgroundColor.toComposeColor(),contentColor=theme.onTextColor.toComposeColor(),
                        border=BorderStroke(if(selectedId==theme.id)3.dp else .5.dp,theme.onTextColor.toComposeColor().copy(alpha=if(selectedId==theme.id).85f else .2f)),
                        modifier=Modifier.weight(1f).heightIn(min=if(namedCards)92.dp else 56.dp).testTag("reader-theme-${theme.id}")
                            .semantics {selected=selectedId==theme.id;contentDescription="${s.localize(Res.string.tp_background_tab)} ${themes.indexOf(theme)+1}"}) {
                        Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                            Text("Aa",fontFamily=FontFamily.Serif,style=if(namedCards)MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleMedium)
                            if(namedCards)Text(readerPaperName(theme,themes.indexOf(theme)),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat(columns-row.size){Spacer(Modifier.weight(1f))}
            }
        }
        if(pages.size>1 || controls!=null)Row(Modifier.fillMaxWidth().heightIn(min=48.dp),verticalAlignment=Alignment.CenterVertically) {
            if(pages.size>1) {
                IconButton(onClick={page--},enabled=page>0,modifier=Modifier.size(48.dp).testTag("reader-paper-previous")) {Icon(Icons.Default.ChevronLeft,s.localize(Res.string.tp_shelf_previous))}
                Text("${page+1}/${pages.size}",style=MaterialTheme.typography.labelMedium)
                IconButton(onClick={page++},enabled=page<pages.lastIndex,modifier=Modifier.size(48.dp).testTag("reader-paper-next")) {Icon(Icons.Default.ChevronRight,s.localize(Res.string.tp_shelf_next))}
            }
            Spacer(Modifier.weight(1f));controls?.invoke(this)
        }
    }
}

@Composable private fun readerPaperName(theme:ReaderColors,index:Int):String {
    val s=requireNotNull(LocalLocalizeHelper.current)
    val name=when(theme.id) {
        -2L->Res.string.tp_paper_white; -3L->Res.string.tp_paper_charcoal; -5L->Res.string.tp_paper_light
        -1L->Res.string.tp_paper_night; -11L->Res.string.tp_paper_warm; -7L->Res.string.tp_paper_blue
        else->null
    }
    return if(name!=null)s.localize(name) else "${s.localize(Res.string.tp_background_tab)} ${index+1}"
}

@Composable private fun Choice(text: String, selected: Boolean, click: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, fontFamily: FontFamily? = null, padding: Int = 12) {
    Surface(onClick = click, enabled = enabled, modifier = modifier.readerControlGlass(RoundedCornerShape(12.dp),enabled,selected).heightIn(min=48.dp).semantics { this.selected = selected },
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
