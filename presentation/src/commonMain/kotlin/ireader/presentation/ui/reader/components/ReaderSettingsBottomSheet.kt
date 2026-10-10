package ireader.presentation.ui.reader.components

import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import ireader.presentation.ui.component.components.TruyenPlusTabs
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ireader.domain.models.prefs.PreferenceValues
import ireader.i18n.resources.Res
import ireader.i18n.resources.colors
import ireader.i18n.resources.fonts
import ireader.i18n.resources.general
import ireader.i18n.resources.reader
import ireader.presentation.ui.component.IBackHandler
import ireader.presentation.ui.component.reusable_composable.MidSizeTextComposable
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.viewmodel.ReaderScreenViewModel
import kotlinx.coroutines.launch

// Animation durations - reduced for better performance
private const val TAB_ANIMATION_DURATION_MS = 150 // Reduced from default ~300ms
private const val TAB_ANIMATION_DISABLED_MS = 0 // No animation for reduced animations mode

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderSettingsBottomSheet(
    modifier: Modifier = Modifier,
    vm: ReaderScreenViewModel,
    onDismiss: () -> Unit,
    onFontSelected: (Int) -> Unit,
    onToggleAutoBrightness: () -> Unit,
    onChangeBrightness: (Float) -> Unit,
    onBackgroundChange: (themeId: Long) -> Unit,
    onTextAlign: (PreferenceValues.PreferenceTextAlignment) -> Unit,
    onNavigate: ((String) -> Unit) = {}
) {
    val localizeHelper = requireNotNull(LocalLocalizeHelper.current) { "LocalLocalizeHelper not provided" }
    
    // Check if reduced animations is enabled for older devices
    val reducedAnimations = vm.readerPreferences.reducedAnimations().get()
    val animationDuration = if (reducedAnimations) TAB_ANIMATION_DISABLED_MS else TAB_ANIMATION_DURATION_MS
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        3 // Reading, colours and fonts only
    }
    val scope = rememberCoroutineScope()
    
    // Handle back button to dismiss the sheet
    IBackHandler(enabled = true, onBack = onDismiss)

    // Use faster animation spec for bottom sheet
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true // Skip partial expansion for faster opening
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier,
        sheetState = sheetState,
        dragHandle = {
            // Simplified drag handle for better performance
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .background(
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), 
                            RoundedCornerShape(2.dp)
                        )
                )
            }
        },
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 720.dp)
                .fillMaxHeight(0.74f)
        ) {
            TruyenPlusReaderControls(
                fontSize = vm.fontSize.value,
                onFontSizeChange = vm::setFontSize,
                themes = vm.readerColors,
                selectedThemeId = vm.readerTheme.value.id,
                onThemeChange = { themeId -> onBackgroundChange(themeId); vm.readerThemeSavable = false },
            )
            TruyenPlusTabs(
                labels = listOf("Đọc", "Màu nền", "Phông chữ"),
                selectedIndex = pagerState.currentPage,
                onSelect = { page -> scope.launch {
                    if (reducedAnimations) pagerState.scrollToPage(page)
                    else pagerState.animateScrollToPage(page, animationSpec = tween(durationMillis = animationDuration))
                } },
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                // Only keep 1 page on each side in memory for better performance
                beyondViewportPageCount = if (reducedAnimations) 0 else 1,
                // Disable user scroll gesture in reduced animations mode for snappier feel
                userScrollEnabled = !reducedAnimations,
                key = { it } // Stable keys for better recomposition
            ) { page ->
                when (page) {
                    0 -> ReaderScreenTab(vm, onTextAlign)
                    1 -> ColorScreenTab(vm, onChangeBrightness, onBackgroundChange)
                    2 -> FontPickerTab(vm)
                }
            }
        }
    }
}
