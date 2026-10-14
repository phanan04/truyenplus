package ireader.presentation.ui.truyenplus
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import ireader.domain.models.BookCover
import ireader.domain.models.entities.BookCategory
import ireader.domain.models.entities.LibraryBook
import ireader.domain.models.entities.ReadingStatisticsType1
import ireader.domain.models.entities.toBookCategory
import ireader.domain.models.library.LibrarySort
import ireader.domain.usecases.statistics.GetReadingStatisticsUseCase
import ireader.i18n.asString
import ireader.i18n.LAST_CHAPTER
import ireader.presentation.core.*
import ireader.presentation.core.ui.*
import ireader.presentation.ui.component.IBackHandler
import ireader.presentation.ui.home.history.viewmodel.HistoryViewModel
import ireader.presentation.ui.home.library.components.EditCategoriesDialog
import ireader.presentation.ui.home.library.components.TruyenPlusImportSheet
import ireader.presentation.ui.home.library.viewmodel.LibraryViewModel
import ireader.presentation.ui.settings.advance.OnShowImportEpub
import ireader.presentation.ui.settings.advance.OnShowImportPdf
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import org.koin.compose.koinInject

fun LibraryBook.toReaderBookUi() = ReaderBookUi(id, title, BookCover.from(toBookItem()), readCount, totalChapters, isPinned, lastRead = lastRead,author=author)

@Composable
fun ReaderShellRoute(initialIndex: Int = 0) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val navigator = requireNotNull(LocalNavigator.current)
    val vm: LibraryViewModel = getIViewModel(key = "library")
    val state by vm.state.collectAsState()
    val importState by vm.importState.collectAsState()
    val importNames=rememberReaderImportNameResolver()
    LaunchedEffect(importState.busy,importState.added) {
        if(!importState.busy && importState.added>0)vm.resetAllPagination()
    }
    val snackbars=remember {SnackbarHostState()}
    LaunchedEffect(vm) {
        vm.eventFlow.collect {event ->
            if(event is ireader.i18n.UiEvent.ShowSnackbar)snackbars.showSnackbar(event.uiText.asString(strings))
        }
    }
    LaunchedEffect(state.lastUndoState) {
        state.lastUndoState?.let {undo ->
            val result=snackbars.showSnackbar(strings.localize(Res.string.tp_read_state_changed),
                actionLabel=strings.localize(Res.string.undo),duration=SnackbarDuration.Long)
            if(result==SnackbarResult.ActionPerformed)vm.undoLastMark(undo) else vm.expireLastUndo(undo)
        }
    }
    DisposableEffect(vm) {onDispose {vm.expireLastUndo()}}
    val tts: ireader.domain.services.tts_service.v2.TTSController = koinInject()
    val ttsState by remember(tts) { tts.state.readerUpdates() }.collectAsState(initial=tts.state.value.readerProjection())
    val ttsService: ireader.domain.services.tts_service.v2.TTSV2ServiceStarter = koinInject()
    var destinationIndex by rememberSaveable { mutableIntStateOf(initialIndex.coerceIn(0, ReaderDestination.entries.lastIndex)) }
    val destination = ReaderDestination.entries.getOrElse(destinationIndex) { ReaderDestination.Home }
    var searchRequested by rememberSaveable { mutableStateOf(false) }
    var showImport by rememberSaveable { mutableStateOf(false) }
    var showProgress by rememberSaveable { mutableStateOf(false) }
    val uiPreferences: ireader.domain.preferences.prefs.UiPreferences = koinInject()
    val librarySearchHistory=rememberReaderSearchHistory(uiPreferences,false)
    val searchScopePreference=remember(uiPreferences){uiPreferences.readerSearchShelf()}
    var searchOpen by rememberSaveable {mutableStateOf(false)}
    var searchShelf by rememberSaveable {mutableStateOf(searchScopePreference.get())}
    val searchPages=rememberSaveableStateHolder()
    var showSort by rememberSaveable {mutableStateOf(false)}
    val keyboard=androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val localeHelper: ireader.presentation.core.theme.LocaleHelper = koinInject()
    val languageCode by localeHelper.currentLanguageFlow.collectAsState()
    val language = ReaderUiLanguage.fromCode(languageCode)
    LaunchedEffect(localeHelper) {
        val code = ReaderUiLanguage.fromCode(localeHelper.getCurrentLanguageCode()).code
        if (uiPreferences.language().get() != code) {
            uiPreferences.language().set(code)
            localeHelper.updateLocal()
        }
    }
    var confirmRemove by remember { mutableStateOf(false) }
    var showCategories by remember { mutableStateOf(false) }
    val addQueue = remember { mutableStateListOf<BookCategory>() }
    val deleteQueue = remember { mutableStateListOf<BookCategory>() }
    val scope = rememberCoroutineScope()
    val updatePlatform = rememberReaderUpdatePlatform()
    val updateManager = remember(updatePlatform) { ReaderUpdateManager(updatePlatform) }
    val updateState by updateManager.state.collectAsState()
    var showUpdate by rememberSaveable { mutableStateOf(false) }
    val voiceStore=org.koin.mp.KoinPlatform.getKoin().getOrNull<ireader.domain.services.tts_service.v2.offline.OfflineVoiceStore>()
    var showVoices by remember {mutableStateOf(false)}
    var showSystemVoices by remember {mutableStateOf(false)}
    var showEditingSettings by rememberSaveable { mutableStateOf(false) }
    var updateJob by remember { mutableStateOf<Job?>(null) }
    val books = state.books.map { it.toReaderBookUi() }
    val onBook: (Long) -> Unit = {if(searchOpen && !searchShelf)librarySearchHistory.record(state.searchQuery.orEmpty());navigator.navigateTo(BookDetailScreenSpec(it))}
    val bookRepository:ireader.domain.data.repository.BookRepository=koinInject()
    val pinUseCase:ireader.domain.usecases.local.book_usecases.ToggleBookPinUseCase=koinInject()
    var pinBusy by remember {mutableStateOf(false)}
    val lastRead=state.lastReadInfo
    val resumeBook by produceState<ReaderBookUi?>(null,lastRead,books) {
        value=lastRead?.let {info->
            val book=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO){bookRepository.findBookById(info.novelId)}
            if(book?.favorite==true && !book.isArchived)(books.firstOrNull {it.id==info.novelId} ?: ReaderBookUi(book.id,book.title,BookCover.from(book),author=book.author))
                .copy(chapter=info.chapterTitle,lastRead=info.lastReadAt) else null
        }
    }
    LaunchedEffect(destination, state.categories.map { it.id }) {
        if (destination != ReaderDestination.Settings) showProgress = false
        vm.loadLastReadInfo()
        vm.refreshCurrentCategoryIfStale()
        if (destination != ReaderDestination.Library) {
            vm.clearSearch()
            val allIndex = state.categories.indexOfFirst { it.id == 0L }
            if (allIndex >= 0) vm.setSelectedPage(allIndex)
        }
    }
    LaunchedEffect(state.selectedCategory?.id) {
        vm.loadInitialBooksForCategory(state.selectedCategory?.id ?: 0L)
    }
    LaunchedEffect(Unit) {
        MainStarterScreen.tabRequests.collect { index ->
            destinationIndex = index.coerceIn(0, ReaderDestination.entries.lastIndex)
        }
    }
    IBackHandler(enabled = destination != ReaderDestination.Home || state.selectionMode) {
        if (state.selectionMode) vm.unselectAll() else if(searchOpen){searchOpen=false;vm.clearSearch();keyboard?.hide()} else if (showProgress) showProgress = false else destinationIndex = 0
    }
    val appearancePref=remember(uiPreferences) { uiPreferences.readerAppearance() }
    val appearanceId by appearancePref.changes().collectAsState(initial=appearancePref.get())
    val blurSupported=rememberReaderBlurSupported()
    val sourceLayer=androidx.compose.ui.graphics.rememberGraphicsLayer()
    val backdrop=remember(sourceLayer,blurSupported) { if(blurSupported) ReaderNavigationBackdrop(sourceLayer) else null }
    val onSearch: ()->Unit = {vm.unselectAll();destinationIndex=ReaderDestination.Library.ordinal;searchOpen=true;searchRequested=true;if(!searchShelf)vm.setSearchMode(true)}
    var dockHeight by remember { mutableStateOf(100.dp) }
    val viewportDensity = androidx.compose.ui.platform.LocalDensity.current
    CompositionLocalProvider(LocalReaderDockInset provides dockHeight + 16.dp, LocalReaderNavigationBackdrop provides backdrop) {
    ReaderBackdrop {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
            ReaderContentPane {
            when (destination) {
                ReaderDestination.Home -> ReaderHomeScreen(books, state.isLoading, onBook,
                    onLibrary = { destinationIndex = 1 }, onImport = { showImport = true },
                    onSearch = onSearch, onUpdate = { showUpdate = true },
                    onContinue={navigator.navigateTo(ReaderScreenSpec(it,lastRead?.takeIf {info->info.novelId==it}?.chapterId ?: LAST_CHAPTER))},
                    onShelf={navigator.navigate("shelf")},resumeBook=resumeBook,onProgress={destinationIndex=ReaderDestination.Settings.ordinal;showProgress=true})
                ReaderDestination.Library -> Column {
                    if(searchOpen)ReaderSearchScopePicker(searchShelf) {shelf ->
                        keyboard?.hide();vm.unselectAll();searchShelf=shelf;searchScopePreference.set(shelf);searchRequested=true
                    }
                    Box(Modifier.weight(1f)) {
                    if(searchOpen && searchShelf)searchPages.SaveableStateProvider("shelf") {
                        ReaderShelfRoute(onLibrary={searchShelf=false;searchScopePreference.set(false);searchRequested=true},
                            onBook=onBook,requestSearch=searchRequested,onSearchFocused={searchRequested=false})
                    } else searchPages.SaveableStateProvider("library") { ReaderLibraryScreen(books,
                    categories = state.categories.map { when (it.category.id) {
                        0L -> strings.localize(Res.string.tp_all)
                        -1L -> strings.localize(Res.string.uncategorized)
                        else -> it.category.name
                    } }, categoryIndex = state.selectedCategoryIndex,
                    hasMore = vm.getPaginationState(state.selectedCategory?.id ?: 0L).hasMoreItems,
                    query = state.searchQuery.orEmpty(), isLoading = state.isLoading, selected = state.selectedBookIds,
                    onQuery = { vm.setSearchMode(true); vm.searchInLibrary(it) }, onCategory = vm::setSelectedPage,
                    onBook = onBook, onSelect = vm::toggleSelection, onImport = { showImport = true },
                    onCollections = { navigator.navigate(NavigationRoutes.category) },
                    onSort = {showSort=true},
                    onLoadMore = { vm.loadMoreBooks(state.selectedCategory?.id ?: 0L) },
                    requestSearch=searchRequested,onSearchFocused={searchRequested=false},onShelf={vm.unselectAll();navigator.navigate("shelf")},sort=state.sort,
                    preferList=state.layout==ireader.domain.models.DisplayMode.List,
                    onLayout={vm.onLayoutTypeChange(if(it)ireader.domain.models.DisplayMode.List else ireader.domain.models.DisplayMode.CompactGrid)},
                    recentSearches=if(searchOpen)librarySearchHistory.queries else emptyList(),onSearchSubmitted=librarySearchHistory.record,onClearSearchHistory=librarySearchHistory.clear)
                    }
                    }
                }
                ReaderDestination.Settings -> {
                    if (!showProgress) ReaderSettingsScreen(language, onLanguage = { selected ->
                        uiPreferences.language().set(selected.code)
                        localeHelper.updateLocal()
                    }, onProgress = { showProgress = true }, onUpdate = { showUpdate = true },
                        appearance=ReaderAppearanceMode.fromId(appearanceId),
                        onAppearance = { appearancePref.set(it.id) }, onImport={showImport=true},onEditing={showEditingSettings=true},onVoices=if(voiceStore!=null){{showVoices=true}} else null)
                    else {
                    val getStats: GetReadingStatisticsUseCase = koinInject()
                    val statisticsFlow = remember(getStats) { getStats() }
                    val stats by statisticsFlow.collectAsState(ReadingStatisticsType1())
                    val historyVm: HistoryViewModel = getIViewModel()
                    val historyState by historyVm.state.collectAsState()
                    val history = historyState.histories.values.flatten().distinctBy { it.id }.map { ReaderHistoryUi(it.id, it.bookId, it.chapterId, it.title, it.chapterName) }
                    Column {
                        TextButton(onClick = { showProgress = false }) { Text(strings.localize(Res.string.tp_back)) }
                        Box(Modifier.weight(1f)) { ReaderProgressScreen(stats, books, history, onBook,
                        onHistory = { navigator.navigateTo(ReaderScreenSpec(it.bookId, it.chapterId)) },
                        onMoreHistory = historyVm::loadMoreHistory) }
                    }
                    }
                }
            }
            }
            }
            Column(Modifier.align(Alignment.BottomCenter).widthIn(max=1120.dp).fillMaxWidth().onSizeChanged {
                dockHeight = with(viewportDensity) { it.height.toDp() }
            }) {
                ReaderImportFeedback(importState,vm::retryFailedImports,vm::clearImportResults)
                if (state.selectionMode) {
                    Surface(shape = MaterialTheme.shapes.large, color=androidx.compose.ui.graphics.Color.Transparent, contentColor=MaterialTheme.colorScheme.onSurface, modifier = Modifier.fillMaxWidth().padding(12.dp).readerPanel(24)) {
                        Column(Modifier.heightIn(max=LocalReaderViewport.current.height * .45f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp)) {
                            if(pinBusy || state.batchOperationInProgress)LinearProgressIndicator(Modifier.fillMaxWidth())
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = vm::unselectAll,enabled=!pinBusy && !state.batchOperationInProgress) { Text(strings.localize(Res.string.tp_unselect)) }
                                val pin=state.books.filter {it.id in state.selectedBookIds}.any {!it.isPinned}
                                TextButton(onClick={
                                    val ids=state.selectedBookIds.toList();pinBusy=true
                                    scope.launch {
                                        try {ids.forEach {pinUseCase.togglePin(it,pin).getOrThrow()};vm.unselectAll();snackbars.showSnackbar(strings.localize(Res.string.done))}
                                        catch(e:kotlinx.coroutines.CancellationException){throw e}
                                        catch(_:Exception){snackbars.showSnackbar(strings.localize(Res.string.tp_operation_failed))}
                                        finally {pinBusy=false}
                                    }
                                },enabled=!pinBusy && !state.batchOperationInProgress) {Text(strings.localize(if(pin)Res.string.pin else Res.string.unpin))}
                                TextButton(onClick = { showCategories = true },enabled=!pinBusy && !state.batchOperationInProgress) { Text(strings.localize(Res.string.tp_collections)) }
                                TextButton(onClick = { confirmRemove = true },enabled=!pinBusy && !state.batchOperationInProgress) { Text(strings.localize(Res.string.tp_remove_books)) }
                            }
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.markAsReadWithUndo(); vm.unselectAll() },enabled=!pinBusy && !state.batchOperationInProgress) { Text(strings.localize(Res.string.tp_mark_read)) }
                                TextButton(onClick = { vm.markAsUnreadWithUndo(); vm.unselectAll() },enabled=!pinBusy && !state.batchOperationInProgress) { Text(strings.localize(Res.string.tp_unread)) }
                            }
                        }
                    }
                } else {
                    val listeningBook = ttsState.book
                    val listeningChapter = ttsState.chapter
                    if (listeningBook != null && listeningChapter != null && ttsState.hasListeningSession) {
                        val openPlayer: () -> Unit = { navigator.navigateTo(readerListeningDestination(listeningBook.id, listeningChapter.id, ttsState.currentParagraphIndex)) }
                        ReaderMiniPlayer(ReaderBookUi(listeningBook.id, listeningBook.title, BookCover.from(listeningBook), chapter = listeningChapter.name),
                            onRead = openPlayer, onListen = openPlayer, isPlaying = ttsState.isPlaying,
                            onTogglePlayback = {
                                if (tts.state.value.isPlaying || tts.state.value.isLoading) tts.dispatch(ireader.domain.services.tts_service.v2.TTSCommand.Pause)
                                else {
                                    ttsService.startService(listeningBook.id, listeningChapter.id, ttsState.currentParagraphIndex)
                                    tts.dispatch(ireader.domain.services.tts_service.v2.TTSCommand.Play)
                                }
                            })
                    }
                    Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.Center) { ReaderBottomNav(destination, {searchOpen=false;keyboard?.hide();destinationIndex=it.ordinal},onSearch=onSearch) }
                }
            }
            SnackbarHost(snackbars,Modifier.align(Alignment.BottomCenter).padding(horizontal=12.dp).padding(bottom=dockHeight+12.dp))
        }
    }
    if(showSort)ReaderGlassSheet(onDismissRequest={showSort=false}) {
        ReaderSortContent(state.sort,{type->if(type!=state.sort.type)vm.toggleSort(type)},
            {ascending->if(ascending!=state.sort.isAscending)vm.toggleSortDirection()},{showSort=false})
    }
    if(showVoices && voiceStore!=null)ReaderVoiceManager(voiceStore,
        onSystem={showVoices=false;showSystemVoices=true},onDismiss={showVoices=false})
    if(showSystemVoices)ireader.presentation.ui.home.tts.TTSVoiceSelectionScreen(isDesktop=false,onDismiss={
        showSystemVoices=false;showVoices=true;tts.dispatch(ireader.domain.services.tts_service.v2.TTSCommand.SetEngine(ireader.domain.services.tts_service.v2.EngineType.NATIVE,true))
    })
    if (showEditingSettings) EditingSettingsSheet { showEditingSettings=false }
    if (showUpdate) ReaderUpdateSheet(updatePlatform.installedApp.versionName, updatePlatform.endpoint,
        updatePlatform.supported, updateState,
        onCheck = { address -> updateJob = scope.launch { updateManager.check(address) } },
        onDownload = { updateJob = scope.launch { updateManager.download() } },
        onInstall = { updateJob = scope.launch { updateManager.install() } },
        onCancel = { updateJob?.cancel() }, onDismiss = { showUpdate = false })
    if (showImport) TruyenPlusImportSheet(
        onImportEpub = { vm.setShowImportEpubDialog(true) }, onImportPdf = { vm.setShowImportPdfDialog(true) }, onDismiss = { showImport = false })
    OnShowImportEpub(show = state.showImportEpubDialog) { uris ->
        vm.setShowImportEpubDialog(false)
        if (uris.isNotEmpty()) vm.importEpubFiles(uris.map { it.toString() },importNames)
    }
    OnShowImportPdf(show = state.showImportPdfDialog) { uris ->
        vm.setShowImportPdfDialog(false)
        if (uris.isNotEmpty()) vm.importPdfFiles(uris.map { it.toString() },importNames)
    }
    if (confirmRemove) ReaderGlassDialog(onDismissRequest = { confirmRemove = false }, title = { Text(strings.localize(Res.string.tp_remove_title)) }, text = { Text(strings.localize(Res.string.tp_remove_count, state.selectedCount)) },
        confirmButton = { TextButton(onClick = { val ids = state.selectedBookIds.toList(); scope.launch { vm.deleteUseCase.unFavoriteBook(ids); vm.unselectAll() }; confirmRemove = false }) { Text(strings.localize(Res.string.tp_remove_books)) } }, dismissButton = { TextButton(onClick = { confirmRemove = false }) { Text(strings.localize(Res.string.tp_cancel)) } })
    EditCategoriesDialog(vm = vm, showDialog = showCategories,
        categories = state.categories.filter { !it.category.isSystemCategory },
        onConfirm = { scope.launch { vm.getCategory.insertBookCategory(addQueue.toList()); vm.getCategory.deleteBookCategory(deleteQueue.toList()); addQueue.clear(); deleteQueue.clear(); vm.unselectAll(); vm.resetAllPagination() }; showCategories = false },
        dismissDialog = { showCategories = false; addQueue.clear(); deleteQueue.clear() },
        onAddToInsertQueue = { addQueue.addAll(it.toBookCategory(state.selectedBookIds.toList())) },
        onAddDeleteQueue = { deleteQueue.addAll(it.toBookCategory(state.selectedBookIds.toList())) },
        onRemoteInInsertQueue = { category -> addQueue.removeAll { it.categoryId == category.id } },
        onRemoteInDeleteQueue = { category -> deleteQueue.removeAll { it.categoryId == category.id } })
    }
}
