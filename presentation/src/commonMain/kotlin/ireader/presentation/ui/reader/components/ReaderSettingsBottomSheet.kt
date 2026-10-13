package ireader.presentation.ui.reader.components

import androidx.compose.foundation.layout.*
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
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.viewmodel.ReaderScreenViewModel
import ireader.presentation.ui.truyenplus.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderSettingsBottomSheet(modifier: Modifier = Modifier, vm: ReaderScreenViewModel, onDismiss: () -> Unit,
    onToggleAutoBrightness: () -> Unit, onChangeBrightness: (Float) -> Unit,
    onBackgroundChange: (Long) -> Unit, onTextAlign: (PreferenceValues.PreferenceTextAlignment) -> Unit) {
    val preferences=org.koin.compose.koinInject<ireader.domain.preferences.prefs.UiPreferences>()
    val followPreference=remember(preferences) { preferences.readerFollowsAppearance() }
    val followAppearance by followPreference.changes().collectAsState(initial=followPreference.get())
    val listening = LocalReaderListening.current != null
    val follow by vm.readerPreferences.ttsFollowParagraph().changes().collectAsState(initial=vm.readerPreferences.ttsFollowParagraph().get())
    val background = vm.backgroundColor.value.toComposeColor()
    val ink = vm.textColor.value.toComposeColor()
    IBackHandler(enabled=true) { onDismiss() }
    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(surface=background,background=background,onSurface=ink,onBackground=ink,
        onSurfaceVariant=ink.copy(alpha=.7f),outline=ink.copy(alpha=.2f))) {
        ReaderAppearanceOverlay(onDismiss=onDismiss,background=background,ink=ink) {
            Column(Modifier.fillMaxWidth()) {
                ReaderAppearanceContent(
                    ReaderAppearanceUi(fontSize=vm.fontSize.value,lineHeight=vm.lineHeight.value,margin=vm.leftMargin.value,
                        scrollSpeed=vm.autoScrollOffset.value,scrollEnabled=if(listening) follow else vm.autoScrollMode,
                        themes=vm.readerColors,themeId=if(followAppearance) Long.MIN_VALUE else vm.readerTheme.value.id,alignment=vm.textAlignment.value,
                        mode=if(listening) ReadingMode.Continues else vm.readingMode.value,fontFamily=vm.font.value.fontFamily,listening=listening,
                        brightness=vm.brightness.value,autoBrightness=vm.autoBrightnessMode.value,followAppearance=followAppearance),
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
                        },onTheme={ followPreference.set(false);onBackgroundChange(it);vm.readerThemeSavable=false },onAlignment=onTextAlign,
                        onMode={ vm.autoScrollMode=false;vm.readingMode.value=it;vm.readerPreferences.defaultReadingMode().set(it) },
                        onFont={ font ->
                            vm.selectedFontId.value=""
                            vm.font.value=FontType(when(font) {
                                FontFamilyModel.Default -> "Default";FontFamilyModel.Serif -> "Serif";FontFamilyModel.SansSerif -> "Sans serif"
                                FontFamilyModel.Cursive -> "Cursive";FontFamilyModel.Monospace -> "Monospace";else -> "Default"
                            },font)
                        },onBrightness=onChangeBrightness,
                        onAutoBrightness={ if(it!=vm.autoBrightnessMode.value) onToggleAutoBrightness() },
                        onFollowAppearance=followPreference::set))
            }
        }
    }
}
