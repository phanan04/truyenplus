package ireader.presentation.ui.reader.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.domain.models.common.FontFamilyModel
import ireader.domain.models.prefs.PreferenceValues
import ireader.domain.preferences.models.FontType
import ireader.domain.preferences.prefs.ReadingMode
import ireader.i18n.resources.*
import ireader.presentation.core.toComposeColor
import ireader.presentation.ui.component.IBackHandler
import ireader.presentation.ui.component.components.TruyenPlusTabs
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.viewmodel.ReaderScreenViewModel
import ireader.presentation.ui.truyenplus.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(modifier: Modifier = Modifier, vm: ReaderScreenViewModel, onDismiss: () -> Unit,
    onFontSelected: (Int) -> Unit, onToggleAutoBrightness: () -> Unit, onChangeBrightness: (Float) -> Unit,
    onBackgroundChange: (Long) -> Unit, onTextAlign: (PreferenceValues.PreferenceTextAlignment) -> Unit,
    onNavigate: (String) -> Unit = {}) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    var advanced by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pager = rememberPagerState(pageCount = { 3 })
    val listening = LocalReaderListening.current != null
    val follow by vm.readerPreferences.ttsFollowParagraph().changes().collectAsState(initial=vm.readerPreferences.ttsFollowParagraph().get())
    val background = vm.backgroundColor.value.toComposeColor()
    val ink = vm.textColor.value.toComposeColor()
    IBackHandler(enabled=true) { if(advanced) advanced=false else onDismiss() }
    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(surface=background,background=background,onSurface=ink,onBackground=ink,
        primary=ReaderControlAccent,onPrimary=Color.White,onSurfaceVariant=ink.copy(alpha=.7f),outline=ink.copy(alpha=.2f))) {
        ModalBottomSheet(onDismissRequest=onDismiss,modifier=modifier,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),
            shape=RoundedCornerShape(topStart=24.dp,topEnd=24.dp),containerColor=background,contentColor=ink,tonalElevation=0.dp) {
            Column(Modifier.fillMaxWidth().heightIn(max=720.dp).fillMaxHeight(.78f)) {
                if(!advanced) ReaderAppearanceContent(
                    ReaderAppearanceUi(fontSize=vm.fontSize.value,lineHeight=vm.lineHeight.value,margin=vm.leftMargin.value,
                        scrollSpeed=vm.autoScrollOffset.value,scrollEnabled=if(listening) follow else vm.autoScrollMode,
                        themes=vm.readerColors,themeId=vm.readerTheme.value.id,alignment=vm.textAlignment.value,
                        mode=if(listening) ReadingMode.Continues else vm.readingMode.value,fontFamily=vm.font.value.fontFamily,listening=listening),
                    ReaderAppearanceActions(
                        onFontSize={ size ->
                            val gap=(vm.lineHeight.value-vm.fontSize.value).coerceAtLeast(2)
                            vm.setFontSize(size);vm.setLineHeight(size+gap)
                        },onLineHeight=vm::setLineHeight,
                        onMargin={ vm.leftMargin.value=it;vm.rightMargin.value=it },
                        onScrollSpeed={ vm.autoScrollOffset.value=it },
                        onScrollEnabled={ enabled ->
                            if(listening) vm.readerPreferences.ttsFollowParagraph().set(enabled)
                            else {
                                if(enabled && vm.readingMode.value!=ReadingMode.Continues) {
                                    vm.readingMode.value=ReadingMode.Continues
                                    vm.readerPreferences.defaultReadingMode().set(ReadingMode.Continues)
                                }
                                vm.autoScrollMode=enabled
                            }
                        },onTheme={ onBackgroundChange(it);vm.readerThemeSavable=false },onAlignment=onTextAlign,
                        onMode={ vm.autoScrollMode=false;vm.readingMode.value=it;vm.readerPreferences.defaultReadingMode().set(it) },
                        onFont={ font ->
                            vm.selectedFontId.value=""
                            vm.font.value=FontType(when(font) {
                                FontFamilyModel.Default -> "Default";FontFamilyModel.SansSerif -> "Sans serif"
                                FontFamilyModel.Cursive -> "Cursive";FontFamilyModel.Monospace -> "Monospace";else -> "Default"
                            },font)
                        },onMore={advanced=true}))
                else {
                    TextButton(onClick={advanced=false}) { Text("‹ ${s.localize(Res.string.settings)}") }
                    TruyenPlusTabs(labels=listOf(s.localize(Res.string.tp_reading_tab),s.localize(Res.string.tp_background_tab),s.localize(Res.string.tp_font_tab)),
                        selectedIndex=pager.currentPage,onSelect={page->scope.launch { pager.scrollToPage(page) }})
                    HorizontalPager(state=pager,modifier=Modifier.weight(1f),beyondViewportPageCount=0) { page ->
                        when(page) {
                            0->ReaderScreenTab(vm,onTextAlign)
                            1->ColorScreenTab(vm,onChangeBrightness,onBackgroundChange)
                            2->FontPickerTab(vm)
                        }
                    }
                }
            }
        }
    }
}
