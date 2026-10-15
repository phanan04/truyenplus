package ireader.presentation.core.ui

import ireader.presentation.ui.truyenplus.readerWakeOnTouch
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.DragInteraction
import kotlin.time.TimeSource
import androidx.compose.foundation.layout.widthIn

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.core.log.Log
import ireader.core.util.getBuildNumber
import ireader.domain.preferences.prefs.ReadingMode
import ireader.domain.services.processstate.ProcessStateManager
import ireader.domain.services.processstate.ReaderProcessState
import ireader.domain.utils.extensions.currentTimeToLong
import ireader.i18n.UiText
import ireader.i18n.resources.Res
import ireader.i18n.resources.this_is_first_chapter
import ireader.i18n.LAST_CHAPTER
import ireader.i18n.NO_VALUE
import ireader.i18n.NULL_VALUE
import ireader.i18n.resources.this_is_last_chapter
import ireader.presentation.core.IModalDrawer
import ireader.presentation.core.IModalSheets
import ireader.presentation.core.LocalNavigator
import ireader.presentation.core.NavigationRoutes
import ireader.presentation.core.ensureAbsoluteUrlForWebView
import ireader.presentation.core.navigateTo
import ireader.presentation.core.safePopBackStack
import ireader.presentation.core.toDomainColor
import ireader.presentation.ui.truyenplus.readerUpdates
import ireader.presentation.ui.truyenplus.readerProjection
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest
import ireader.presentation.core.toComposeColor
import ireader.presentation.ui.component.getContextWrapper
import ireader.presentation.ui.core.theme.AppColors
import ireader.presentation.ui.core.theme.CustomSystemColor
import ireader.presentation.ui.core.ui.Colour.Transparent
import ireader.presentation.ui.core.ui.SnackBarListener
import ireader.presentation.ui.plugins.integration.FeaturePluginIntegration
import ireader.presentation.ui.plugins.integration.IncompatiblePluginHandler
import ireader.presentation.ui.reader.ReaderScreenDrawer
import ireader.presentation.ui.reader.ReaderScreenTopBar
import ireader.presentation.ui.reader.ReadingScreen
import ireader.presentation.ui.reader.components.ReaderSettingMainLayout
import ireader.presentation.ui.reader.reverse_swip_refresh.rememberSwipeRefreshState
import ireader.presentation.ui.reader.viewmodel.PlatformReaderSettingReader
import ireader.presentation.ui.reader.viewmodel.ReaderScreenViewModel
import ireader.presentation.ui.reader.viewmodel.ReaderState
import ireader.presentation.core.theme.AppThemeViewModel
import ireader.domain.models.BookCover
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.getKoin
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Resolves the initial chapter ID to load in the reader.
 *
 * If the user explicitly tapped a specific chapter (e.g., chapter 74 in the chapter list),
 * that explicit chapter ID MUST take precedence over any previously restored reading state.
 * Restored process state is only used for resume operations (LAST_CHAPTER / NO_VALUE / NULL_VALUE).
 */
fun resolveInitialReaderChapterId(
    requestedChapterId: Long,
    bookId: Long,
    restoredState: ReaderProcessState?
): Long {
    val isExplicitChapter = requestedChapterId > 0 &&
        requestedChapterId != LAST_CHAPTER &&
        requestedChapterId != NO_VALUE &&
        requestedChapterId != NULL_VALUE

    return if (isExplicitChapter) {
        requestedChapterId
    } else if (restoredState != null && restoredState.bookId == bookId && restoredState.chapterId > 0) {
        restoredState.chapterId
    } else {
        requestedChapterId
    }
}

@OptIn(ExperimentalMaterial3Api::class)
data class ReaderScreenSpec(
    val bookId: Long,
    val chapterId: Long,
    val listenOnOpen: Boolean = false,
    val listeningParagraph: Int = 0
) {


    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalMaterial3Api::class
    )
    @Composable
    fun Content() {
        val scope = rememberCoroutineScope()

        val platformReader: PlatformReaderSettingReader = koinInject()
        val processStateManager: ProcessStateManager = koinInject()

        // Check for process death / orientation state restoration
        val restoredState = remember { processStateManager.getReaderState() }

        // Track active chapter ID with rememberSaveable so orientation change retains the current chapter
        var activeChapterId by rememberSaveable(bookId, chapterId) {
            mutableStateOf(
                resolveInitialReaderChapterId(
                    requestedChapterId = chapterId,
                    bookId = bookId,
                    restoredState = restoredState
                )
            )
        }

        // One VM per back-stack entry: survives forward navigation, handles chapter
        // switches internally via contentVM (no VM recreation per chapter).
        val vm: ReaderScreenViewModel =
            koinViewModel(parameters = {
                parametersOf(
                    ReaderScreenViewModel.Param(
                        activeChapterId,
                        bookId
                    )
                )
            })
        val readerState by vm.state.collectAsState()

        val ttsController: ireader.domain.services.tts_service.v2.TTSController = koinInject()
        val listeningPreferences: ireader.domain.preferences.prefs.ReaderPreferences = koinInject()
        val wordHighlight by listeningPreferences.ttsHighlightWords().changes().collectAsState(initial=listeningPreferences.ttsHighlightWords().get())
        val highlightListening by listeningPreferences.ttsHighlightText().changes().collectAsState(initial=listeningPreferences.ttsHighlightText().get())
        val includeWords=wordHighlight && highlightListening
        val ttsState by remember(ttsController,includeWords) {ttsController.state.readerUpdates(includeWords)}.collectAsState(initial=ttsController.state.value.readerProjection(includeWords))
        var showListening by rememberSaveable { mutableStateOf(listenOnOpen) }
        val followState=remember { ireader.presentation.ui.truyenplus.ReaderFollowState() }
        val immersive=remember { ireader.presentation.ui.truyenplus.ReaderImmersiveState() }
        // Capture visibility on pointer-down, before waking. A callback reads this only on click.
        val wakingParagraphTap=remember { mutableStateOf(false) }
        val immersiveEpoch=remember { TimeSource.Monotonic.markNow() }
        val sentenceTarget=remember { kotlinx.coroutines.flow.MutableStateFlow<ireader.presentation.ui.truyenplus.ReaderSentenceTarget?>(null) }
        val reportSentenceBounds=remember { { page:Int,bounds:androidx.compose.ui.geometry.Rect -> sentenceTarget.value=ireader.presentation.ui.truyenplus.ReaderSentenceTarget(page,bounds) } }
        val target by sentenceTarget.collectAsState()
        val readerPanels=remember { ireader.presentation.ui.truyenplus.ReaderPanelState() }
        val blurSupported=ireader.presentation.ui.truyenplus.rememberReaderBlurSupported()
        val sourceLayer=androidx.compose.ui.graphics.rememberGraphicsLayer()
        val chapterBackdrop=remember(sourceLayer,blurSupported) {
            if(blurSupported) ireader.presentation.ui.truyenplus.ReaderNavigationBackdrop(sourceLayer) else null
        }
        val followListening by listeningPreferences.ttsFollowParagraph().changes().collectAsState(initial = listeningPreferences.ttsFollowParagraph().get())
        val readerDensity = androidx.compose.ui.platform.LocalDensity.current
        var readerFooterHeight by remember { mutableStateOf(72.dp) }
        var readerPlayerHeight by remember { mutableStateOf(156.dp) }
        val statusBarInset = androidx.compose.foundation.layout.WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navigationInset = androidx.compose.foundation.layout.WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        val listeningAdapter = remember(ttsController) { ireader.domain.services.tts_service.v2.TTSViewModelAdapter(ttsController, scope) }

        val uiPreferences=koinInject<ireader.domain.preferences.prefs.UiPreferences>()
        val appearance=androidx.compose.material3.MaterialTheme.colorScheme
        val followAppearancePref=remember(uiPreferences) { uiPreferences.readerFollowsAppearance() }
        val followAppearance by followAppearancePref.changes().collectAsState(initial=followAppearancePref.get())
        LaunchedEffect(appearance,followAppearance) {
            if(followAppearance) {
                vm.backgroundColor.value=appearance.background.toDomainColor()
                vm.textColor.value=appearance.onBackground.toDomainColor()
            }
        }
        val speechIndex=remember(ttsState.paragraphSourceIndices) {
            ireader.presentation.ui.truyenplus.ReaderSpeechIndex(ttsState.paragraphSourceIndices)
        }
        val selectSpeech: (Int)->Unit = remember(listeningAdapter) { { index ->
            if(ttsController.state.value.chunkModeEnabled) listeningAdapter.disableChunkMode()
            listeningAdapter.jumpToParagraph(index)
        } }

        // Extract values from state
        val successState = readerState as? ReaderState.Success
        val currentIndex = successState?.currentChapterIndex ?: 0
        val chapters = successState?.chapters ?: emptyList()
        // Use infiniteScrollVisibleChapter for infinite scroll mode
        val infiniteScrollChapter by vm.contentVM.infiniteScrollVisibleChapter.collectAsState()
        val chapter = if (vm.readingMode.value == ireader.domain.preferences.prefs.ReadingMode.InfiniteScroll && infiniteScrollChapter != null) {
            infiniteScrollChapter
        } else {
            successState?.currentChapter
        }

        // Keep activeChapterId updated as user reads through chapters
        LaunchedEffect(chapter?.id) {
            chapter?.id?.let { id ->
                if (id > 0 && id != activeChapterId) {
                    activeChapterId = id
                }
            }
        }

        val context = getContextWrapper()
        val scrollState = rememberScrollState()
        val lazyListState = rememberLazyListState()
        val navController = requireNotNull(LocalNavigator.current) { "LocalNavigator not provided" }

        val swipeState = rememberSwipeRefreshState(isRefreshing = false)

        // Track saved scroll position and offset across configuration changes
        var savedScrollPosition by rememberSaveable(bookId, activeChapterId) {
            mutableStateOf(if (restoredState != null && restoredState.bookId == bookId && restoredState.chapterId == activeChapterId) restoredState.scrollPosition else 0)
        }
        var savedScrollOffset by rememberSaveable(bookId, activeChapterId) {
            mutableStateOf(if (restoredState != null && restoredState.bookId == bookId && restoredState.chapterId == activeChapterId) restoredState.scrollOffset else 0)
        }
        var scrollRestoredForChapter by rememberSaveable(bookId, activeChapterId) { mutableStateOf(false) }

        // Observe scrolling outside composition: no whole-screen invalidation for every pixel.
        val latestReaderMode=rememberUpdatedState(successState?.isReaderModeEnabled ?: true)
        LaunchedEffect(bookId,chapter?.id,lazyListState) {
            val currentChapter=chapter ?: return@LaunchedEffect
            snapshotFlow { scrollRestoredForChapter }.first { it }
            snapshotFlow { lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset }
                .collectLatest { (position,offset) ->
                    savedScrollPosition=position
                    savedScrollOffset=offset
                    delay(300)
                    processStateManager.saveReaderState(ReaderProcessState(
                        bookId=bookId,chapterId=currentChapter.id,scrollPosition=position,scrollOffset=offset,
                        readingParagraph=0,isReaderModeEnabled=latestReaderMode.value,timestamp=currentTimeToLong()))
                }
        }

        // Restore scroll position after content is rendered
        LaunchedEffect(successState?.currentChapter?.id, activeChapterId) {
            if (successState != null && !scrollRestoredForChapter) {
                val targetPosition = when {
                    restoredState != null && restoredState.bookId == bookId && restoredState.chapterId == (chapter?.id ?: activeChapterId) -> restoredState.scrollPosition
                    savedScrollPosition > 0 -> savedScrollPosition
                    else -> 0
                }
                val targetOffset = when {
                    restoredState != null && restoredState.bookId == bookId && restoredState.chapterId == (chapter?.id ?: activeChapterId) -> restoredState.scrollOffset
                    savedScrollOffset > 0 -> savedScrollOffset
                    else -> 0
                }

                try {
                    val count=snapshotFlow { lazyListState.layoutInfo.totalItemsCount }.first { it>0 }
                    if(targetPosition>0 || targetOffset>0) {
                        lazyListState.scrollToItem(targetPosition.coerceIn(0,count-1),targetOffset)
                    }
                } catch (e: Exception) {
                    Log.warn { "Failed to restore scroll position: ${e.message}" }
                } finally {
                    scrollRestoredForChapter=true
                }
            }
        }

        DisposableEffect(key1 = true) {
            onDispose {
                vm.saveCurrentScrollPositionToDatabase()
                platformReader.apply {
                    if (context != null) {
                        vm.restoreSetting(context, scrollState, lazyListState)
                    }
                }
            }
        }


        // Track reading time - records time spent in reader screen
        // Save periodically to avoid losing data if app crashes or user reads for long periods
        DisposableEffect(key1 = Unit) {
            val startTime = currentTimeToLong()
            var lastSaveTime = startTime
            Log.info { "Reading time tracking started at: $startTime" }

            // Launch a coroutine to save reading time periodically (every 30 seconds)
            val saveJob = scope.launch {
                while (true) {
                    delay(30000) // 30 seconds
                    val currentTime = currentTimeToLong()
                    val durationSinceLastSave = currentTime - lastSaveTime
                    val durationMinutes = durationSinceLastSave / 60000

                    Log.info { "Periodic save check: duration since last save = ${durationSinceLastSave}ms (${durationMinutes}min)" }

                    // Save incremental time
                    if (durationSinceLastSave >= 5000) { // At least 5 seconds
                        try {
                            Log.info { "Saving reading time: ${durationMinutes} minutes" }
                            vm.trackReadingProgressUseCase.trackReadingTime(durationSinceLastSave)
                            vm.trackReadingProgressUseCase.updateReadingStreak(currentTime)
                            lastSaveTime = currentTime
                            Log.info { "Reading time saved successfully" }
                        } catch (e: Exception) {
                            Log.error { "Failed to track reading time periodically: ${e.message}" }
                            e.printStackTrace()
                        }
                    }
                }
            }

            onDispose {
                // Cancel periodic save job
                saveJob.cancel()

                // Save final reading time
                val endTime = currentTimeToLong()
                val finalDuration = endTime - lastSaveTime
                val totalDuration = endTime - startTime
                val finalMinutes = finalDuration / 60000
                val totalMinutes = totalDuration / 60000

                Log.info { "Reading session ended. Final duration: ${finalDuration}ms (${finalMinutes}min), Total: ${totalDuration}ms (${totalMinutes}min)" }

                // Only track if user spent at least 5 seconds reading since last save
                if (finalDuration >= 5000) {
                    scope.launch {
                        try {
                            Log.info { "Saving final reading time: ${finalMinutes} minutes" }
                            vm.trackReadingProgressUseCase.trackReadingTime(finalDuration)
                            vm.trackReadingProgressUseCase.updateReadingStreak(endTime)
                            Log.info { "Final reading time saved successfully" }
                        } catch (e: Exception) {
                            Log.error { "Failed to track final reading time: ${e.message}" }
                            e.printStackTrace()
                        }
                    }
                } else {
                    Log.info { "Skipping final save: duration (${finalDuration}ms) is less than 5 seconds" }
                }
            }
        }

        val autoScrollMode = successState?.autoScrollMode ?: false
        LaunchedEffect(autoScrollMode,showListening) {
            while(autoScrollMode && !showListening) {
                delay(50)
                if(!lazyListState.isScrollInProgress && !vm.showSettingsBottomSheet && readerPanels.active==ireader.presentation.ui.truyenplus.ReaderPanel.None) {
                    // The core speed control is 1..20: four dp/second per step.
                    val delta=with(readerDensity) { (vm.autoScrollOffset.value.coerceIn(1,20)*.2f).dp.toPx() }
                    lazyListState.scrollBy(delta)
                }
            }
        }

        LaunchedEffect(showListening, ttsState.chapter?.id) {
            if (showListening && ttsState.book?.id == bookId) vm.syncWithTTSChapter()
        }
        val spokenParagraph=ttsState.spokenSentence?.paragraphIndex ?: ttsState.currentParagraphIndex
        val listeningSourceIndex=ttsState.paragraphSourceIndices.getOrNull(spokenParagraph)
        LaunchedEffect(lazyListState) {
            lazyListState.interactionSource.interactions.collect { interaction ->
                if(interaction is DragInteraction.Start && showListening)followState.onUserDrag()
            }
        }
        val playbackActive=ttsState.isPlaying || ttsState.isLoading
        LaunchedEffect(showListening,playbackActive,readerPanels.active,vm.showSettingsBottomSheet) {
            while(true) {
                delay(500)
                immersive.update(immersiveEpoch.elapsedNow().inWholeMilliseconds,
                    !showListening || playbackActive,readerPanels.active!=ireader.presentation.ui.truyenplus.ReaderPanel.None || vm.showSettingsBottomSheet)
                if(!immersive.visible && !vm.isReaderModeEnable)vm.toggleReaderMode(true)
            }
        }
        LaunchedEffect(showListening,followListening,listeningSourceIndex,ttsState.spokenSentence,target,
            followState.following,followState.returnRequest,ttsState.isPlaying,readerPanels.active,readerPlayerHeight,vm.isReaderModeEnable,statusBarInset,navigationInset) {
            if(showListening && followListening && followState.following && (ttsState.isPlaying || followState.returnRequest>0) &&
                readerPanels.active==ireader.presentation.ui.truyenplus.ReaderPanel.None &&
                ttsState.book?.id==bookId && ttsState.chapter?.id==chapter?.id && listeningSourceIndex!=null) {
                delay(80)
                val info=lazyListState.layoutInfo
                val item=info.visibleItemsInfo.firstOrNull {it.index==listeningSourceIndex}
                if(item==null)lazyListState.animateScrollToItem(listeningSourceIndex)
                else {
                    val bounds=target?.takeIf {it.page==listeningSourceIndex}?.bounds
                    val top=item.offset+info.beforeContentPadding+(bounds?.top ?: 0f)
                    val bottom=item.offset+info.beforeContentPadding+(bounds?.bottom ?: minOf(item.size.toFloat(),80f))
                    val topInset=with(readerDensity) {
                        if(vm.isReaderModeEnable)0f else (statusBarInset+68.dp).toPx()
                    }
                    val bottomInset=with(readerDensity) {(readerPlayerHeight+navigationInset).toPx()}
                    ireader.presentation.ui.truyenplus.readerFollowScrollDelta(top,bottom,info.viewportSize.height.toFloat(),topInset,bottomInset)
                        ?.let {lazyListState.animateScrollBy(it)}
                }
            }
        }
        // Sync with TTS chapter when screen becomes active
        // Observe the back stack to detect when we return from TTS screen
        val backStackEntry by navController.currentBackStackEntryFlow.collectAsState(initial = null)
        LaunchedEffect(backStackEntry) {
            // When back stack changes and we're on this screen, sync with TTS
            val route = backStackEntry?.destination?.route
            if (route?.contains("reader") == true) {
                vm.syncWithTTSChapter()
            }
        }
        LaunchedEffect(key1 = vm.autoBrightnessMode.value) {
            platformReader.apply {
                if (context != null) {
                    vm.readBrightness(context)
                }
            }
        }
        LaunchedEffect(key1 = vm.orientation.value) {
            platformReader.apply {
                if (context != null) {
                    vm.readOrientation(context)
                }
            }
        }

        LaunchedEffect(key1 = vm.screenAlwaysOn.value) {
            vm.screenAlwaysOnUseCase(vm.screenAlwaysOn.value)
        }
        val bars = AppColors.current

        val customColor = remember {
            val barsColor = bars.bars.toComposeColor()
            if (getBuildNumber() < 25 && bars.isBarLight) {
                CustomSystemColor(Color.LightGray, barsColor)
            } else {
                CustomSystemColor(barsColor, barsColor)
            }

        }
        val hideSystemBar = remember { mutableStateOf(false) }
        val hideNavBar = remember { mutableStateOf(false) }

        val isInitialized = successState != null
        LaunchedEffect(key1 = isInitialized) {
            platformReader.apply {
                if (context != null && isInitialized) {
                    vm.prepareReaderSetting(
                        context = context,
                        scrollState,
                        onHideNav = {
                            hideNavBar.value = it
                        },
                        onHideStatus = {
                            hideSystemBar.value = it
                        }
                    )
                }
            }
        }

        LaunchedEffect(key1 = vm.immersiveMode.value, key2 = vm.isReaderModeEnable) {
            platformReader.apply {
                if (context != null) {
                    vm.readImmersiveMode(
                        context = context,
                        onHideNav = {
                            hideNavBar.value = it
                        },
                        onHideStatus = {
                            hideSystemBar.value = it
                        }
                    )
                }
            }
        }
        val host = SnackBarListener(vm)

        val sheetState = rememberModalBottomSheetState()
        val drawerState =
            androidx.compose.material3.rememberDrawerState(androidx.compose.material3.DrawerValue.Closed)

        // Handle back button to close drawer instead of closing screen
        // BackHandler removed - Android-specific, implement in androidMain if needed

        IModalDrawer(
            state = drawerState,
            sheetContent = {
                val drawerScrollState = rememberLazyListState()
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(initialOffsetY = { -it }),
                    exit = slideOutVertically(targetOffsetY = { -it })
                ) {
                    ReaderScreenDrawer(
                        onReverseIcon = {
                            vm.toggleDrawerAsc()
                        },
                        onChapter = { ch ->
                            val index = chapters.indexOfFirst { it.id == ch.id }
                            if (index != -1) {
                                scope.launch {
                                    vm.clearChapterShell(scrollState)
                                    vm.getLocalChapter(ch.id)
                                    drawerState.close()
                                }
                                scope.launch {
                                    scrollState.scrollTo(0)
                                }
                            }
                        },
                        chapter = chapter,
                        chapters = successState?.drawerChapters ?: emptyList(),
                        drawerScrollState = drawerScrollState,
                        onMap = { drawer ->
                            scope.launch {
                                try {
                                    val drawerChapters = successState?.drawerChapters ?: emptyList()
                                    val index = drawerChapters.indexOfFirst { it.id == chapter?.id }
                                    if (index != -1) {
                                        drawer.scrollToItem(
                                            index,
                                            -drawer.layoutInfo.viewportEndOffset / 2
                                        )
                                    }
                                } catch (e: Throwable) {
                                }
                            }
                        },
                    )
                }
            }
        ) {
            CustomSystemColor(
                enable = false,
                statusBar = customColor.status,
                navigationBar = customColor.navigation
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    ireader.presentation.ui.truyenplus.LocalReaderNavigationBackdrop provides chapterBackdrop,
                    ireader.presentation.ui.truyenplus.LocalReaderPanels provides readerPanels,
                    ireader.presentation.ui.truyenplus.LocalReaderPaper provides ireader.presentation.ui.truyenplus.ReaderPaperColors(vm.backgroundColor.value.toComposeColor(),vm.textColor.value.toComposeColor()),
                    ireader.presentation.ui.truyenplus.LocalReaderListeningDockHeight provides readerPlayerHeight
                ) {
                ireader.presentation.ui.truyenplus.ReaderPaperTheme {
                // Use Box to overlay top bar without affecting content layout
                androidx.compose.foundation.layout.Box(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize().readerWakeOnTouch {
                        wakingParagraphTap.value=vm.isReaderModeEnable
                        immersive.touch(immersiveEpoch.elapsedNow().inWholeMilliseconds)
                        if(vm.isReaderModeEnable)vm.toggleReaderMode(false)
                    }
                ) {
                    // Content first (below the top bar)
                    val headerInset=statusBarInset+68.dp
                    val padding = androidx.compose.foundation.layout.PaddingValues(
                        top=if(!vm.isReaderModeEnable)headerInset else 0.dp,
                        bottom = if (showListening) readerPlayerHeight + navigationInset else if (!vm.isReaderModeEnable) readerFooterHeight else 0.dp)

                    androidx.compose.runtime.CompositionLocalProvider(
                        ireader.presentation.ui.truyenplus.LocalReaderListening provides if (showListening)
                            remember(ttsState.book?.id,ttsState.chapter?.id,chapter?.id,speechIndex,spokenParagraph,highlightListening,ttsState.spokenSentence,ttsState.spokenWord,wordHighlight,chapters) {
                                val matching=ttsState.book?.id==bookId && ttsState.chapter?.id==chapter?.id
                                ireader.presentation.ui.truyenplus.ReaderListeningContext(
                                    if(matching) ttsState.paragraphSourceIndices else emptyList(),spokenParagraph,
                                    highlightListening,if(matching) speechIndex else ireader.presentation.ui.truyenplus.ReaderSpeechIndex(emptyList()),selectSpeech,
                                    if(matching)ttsState.spokenSentence else null,reportSentenceBounds,
                                    if(matching)ttsState.spokenWord else null,wordHighlight,
                                    chapterIndex=if(matching)chapters.indexOfFirst {it.id==ttsState.chapter?.id} else -1,
                                    chapterCount=chapters.size,allowInspection={!wakingParagraphTap.value})
                            } else null
                    ) {
                    ReadingScreen(
                        drawerState = drawerState,
                        vm = vm,
                        scrollState = scrollState,
                        onNext = { rest ->
                            if (currentIndex < chapters.lastIndex) {
                                try {
                                    // Dispatch navigation to ChapterController (Requirements: 10.1, 10.3)
                                    vm.dispatchNextChapter()
                                    scope.launch {
                                        if (rest) {
                                            vm.clearChapterShell(scrollState)
                                        }
                                    }
                                    when (vm.readingMode.value) {
                                        ReadingMode.Continues, ReadingMode.InfiniteScroll -> {}
                                        ReadingMode.Page -> {
                                            scope.launch {
                                                scrollState.scrollTo(0)
                                            }
                                        }
                                    }
                                } catch (e: Throwable) {
                                    Log.error(e, "Reader Spec failed to go next chapter")
                                }
                            } else {
                                scope.launch {
                                    vm.showSnackBar(
                                        UiText.MStringResource(
                                            Res.string.this_is_last_chapter
                                        )
                                    )
                                }
                            }
                        },
                        onPrev = { reset ->
                            try {
                                if (currentIndex > 0) {
                                    // Dispatch navigation to ChapterController (Requirements: 10.2, 10.3)
                                    vm.dispatchPrevChapter()

                                    scope.launch {
                                        if (reset) {
                                            vm.clearChapterShell(scrollState)
                                        }

                                        // For Page mode, also scroll to end
                                        if (vm.readingMode.value == ReadingMode.Page) {
                                            delay(100)
                                            scrollState.scrollTo(scrollState.maxValue)
                                        }
                                        // For Continues mode, scrolling is handled by LaunchedEffect in ReaderText
                                    }
                                } else {
                                    scope.launch {
                                        vm.showSnackBar(
                                            UiText.MStringResource(
                                                Res.string.this_is_first_chapter
                                            )
                                        )
                                    }
                                }
                            } catch (e: Throwable) {
                                Log.error(e, "Reader Spec failed to go previous chapter")
                            }
                        },
                        toggleReaderMode = {
                            // The initial pointer pass already revealed hidden controls.
                            // Do not let the paragraph's click immediately hide them again.
                            if (!wakingParagraphTap.value) vm.toggleReaderMode()
                        },
                        readerScreenPreferencesState = vm,
                        onBackgroundColorAndTextColorApply = { bgColor, txtColor ->
                            try {
                                if (bgColor.isNotBlank()) {
                                    vm.settingsViewModel.setReaderBackgroundColor(vm.backgroundColor.value.toComposeColor())
                                }
                            } catch (e: Throwable) {
                            }

                            try {
                                if (txtColor.isNotBlank()) {
                                    vm.settingsViewModel.setReaderTextColor(vm.textColor.value.toComposeColor())
                                }
                            } catch (e: Throwable) {
                            }
                        },

                        snackBarHostState = host,
                        swipeState = swipeState,
                        onSliderFinished = {
                            scope.launch {
                                if (currentIndex in chapters.indices) {
                                    vm.showSnackBar(
                                        UiText.DynamicString(
                                            chapters[currentIndex].name
                                        )
                                    )
                                }
                            }
                            scope.launch {
                                if (currentIndex in chapters.indices) {
                                    vm.getLocalChapter(
                                        chapters[currentIndex].id,
                                    )
                                }
                            }

                            scope.launch {
                                scrollState.animateScrollTo(0)
                            }
                        },
                        onSliderChange = {
                            // Slider change is now handled via state
                        },
                        onReaderPlay = { readerPanels.expand();showListening = true },
                        onReaderBottomOnSetting = {
                            scope.launch {
                                sheetState.show()
                            }
                        },
                        lazyListState = lazyListState,
                        onChapterShown = { shownChapter ->
                            if (shownChapter.id != chapter?.id) {
                                kotlin.runCatching {
                                    // Chapter shown is now handled via state
                                    val index =
                                        chapters.indexOfFirst { it.id == shownChapter.id }
                                    if (index != -1) {
                                        // Index update handled via navigateToChapter
                                    }
                                }
                            }
                        },
                        paddingValues = padding,
                        onNavigateToTranslationSettings = {
                            navController.navigate(NavigationRoutes.translationSettings)
                        },
                        onNavigateToCharacterArtUpload = { bookTitle, chapterTitle, prompt ->
                            navController.navigate(
                                NavigationRoutes.characterArtUploadWithData(
                                    bookTitle,
                                    chapterTitle,
                                    prompt
                                )
                            )
                        },
                        onChangeBrightness = { brightness ->
                            platformReader.apply {
                                if (context != null) {
                                    vm.saveBrightness(context, brightness)
                                }
                            }
                        },
                        onToggleAutoBrightness = {
                            vm.autoBrightnessMode.value = !vm.autoBrightnessMode.value
                        },
                        onNavigateToQuoteCreation = null,
                        onNavigate = { navController.navigate(it) },
                        onControlsHeightChanged = { readerFooterHeight = with(readerDensity) { it.toDp() } }
                    )
                    }

                    if(showListening && followListening && !followState.following && listeningSourceIndex!=null && readerPanels.active==ireader.presentation.ui.truyenplus.ReaderPanel.None && !vm.showSettingsBottomSheet) {
                        ireader.presentation.ui.truyenplus.ReaderReturnToSpeech(onClick={followState.resume()},
                            modifier=androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomEnd)
                                .padding(end=16.dp,bottom=readerPlayerHeight+navigationInset+12.dp))
                    }

                    // Top bar overlay (rendered on top of content)
                    val catalog = successState?.catalog
                    val book = successState?.book

                    ReaderScreenTopBar(
                        onPlay = { showListening = true },
                        modifier = androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.TopCenter),
                        isReaderModeEnable = successState?.isReaderModeEnabled ?: false,
                        isLoaded = successState?.isChapterLoaded ?: false,
                        onRefresh = {
                            scope.launch {
                                vm.loadChapterFromLocal(chapter?.id)
                            }
                        },
                        onRefreshFromRemote = {
                            scope.launch {
                                vm.fetchChapterFromRemote(chapter?.id)
                            }
                        },
                        chapter = chapter,
                        onWebView = {
                            try {
                                catalog?.let { catalog ->
                                    val absoluteUrl = chapter?.key?.let { url ->
                                        ensureAbsoluteUrlForWebView(url, catalog.source)
                                    }
                                    navController.navigateTo(
                                        WebViewScreenSpec(
                                            url = absoluteUrl,
                                            sourceId = catalog.sourceId,
                                            chapterId = chapter?.id,
                                            bookId = book?.id,
                                            enableChapterFetch = true,
                                            enableChaptersFetch = false,
                                            enableBookFetch = false
                                        )
                                    )
                                }
                            } catch (e: Throwable) {
                                scope.launch {
                                    vm.showSnackBar(
                                        UiText.ExceptionString(
                                            e
                                        )
                                    )
                                }
                            }
                        },
                        vm = vm,
                        state = vm,
                        onBookMark = {
                            vm.bookmarkChapter()
                        },
                        onPopBackStack = {
                            navController.safePopBackStack()
                        },
                        onChapterArt = {
                            vm.showChapterArtDialog()
                        },
                        hasPluginMenuItems = false,
                        onPluginMenu = {}
                    )

                    if (showListening && book != null && chapter != null) {
                        androidx.compose.foundation.layout.Box(Modifier.align(androidx.compose.ui.Alignment.BottomCenter).widthIn(max=760.dp).fillMaxWidth()
                            .then(Modifier.navigationBarsPadding())) {
                            TTSV2ScreenSpec(book.id, chapter.id, book.sourceId, listeningParagraph).Content(
                                embedded = true, onCloseListening = { readerPanels.close(ireader.presentation.ui.truyenplus.ReaderPanel.Listening);listeningAdapter.pause(); showListening = false },
                                onPlayerHeightChanged = { readerPlayerHeight = with(readerDensity) { it.toDp() } },
                                chapterNavigation = ireader.presentation.ui.truyenplus.ReaderChapterNavigation(
                                    chapters.indexOfFirst { it.id == (ttsState.chapter?.takeIf { ttsState.book?.id == bookId }?.id ?: chapter.id) },
                                    chapters.size))
                        }
                    }

                    // Modal sheet rendered inside the Box to ensure it appears above content
                    val isSettingChanging = vm.settingsViewModel.isSettingChanging
                    if (sheetState.isVisible) {
                        IModalSheets(
                            bottomSheetState = sheetState,
                            backgroundColor = if (isSettingChanging) MaterialTheme.colorScheme.Transparent.copy(
                                0f
                            ) else MaterialTheme.colorScheme.background,
                            contentColor = if (isSettingChanging) MaterialTheme.colorScheme.Transparent.copy(
                                0f
                            ) else MaterialTheme.colorScheme.onBackground,
                            sheetContent = { sheetModifier ->
                                Column(sheetModifier) {
                                    HorizontalDivider(
                                        modifier = Modifier.fillMaxWidth(),
                                        thickness = 1.dp,
                                        color = if (isSettingChanging) MaterialTheme.colorScheme.Transparent.copy(
                                            0f
                                        ) else MaterialTheme.colorScheme.onBackground.copy(.2f)
                                    )
                                    Spacer(modifier = Modifier.height(5.dp))
                                    ReaderSettingMainLayout(
                                        onFontSelected = { index ->
                                            vm.settingsViewModel.selectFont(index)
                                        },
                                        onChangeBrightness = { brightness ->
                                            platformReader.apply {
                                                if (context != null) {
                                                    vm.saveBrightness(context, brightness)
                                                }
                                            }
                                        },
                                        onBackgroundChange = { index ->
                                            vm.settingsViewModel.changeBackgroundColor(
                                                index,
                                                vm.readerColors
                                            )
                                        },
                                        vm = vm,
                                        onTextAlign = { alignment ->
                                            vm.textAlignment.value = alignment
                                            vm.settingsViewModel.saveTextAlignment(alignment)
                                        },
                                        onToggleAutoBrightness = {
                                            vm.autoBrightnessMode.value =
                                                !vm.autoBrightnessMode.value
                                        },
                                        onNavigate = { route ->
                                            navController.navigate(route)
                                        }
                                    )
                                }
                            }
                        ) {
                            // Empty content - the actual content is rendered above
                        }
                    }
                }
                }
                }
            }
        }
    }
}

private fun LazyListState.getId(): Long? = kotlin.runCatching {
    layoutInfo.visibleItemsInfo.firstOrNull()?.key.toString()
        .substringAfter("-").toLong()
}.getOrNull()
