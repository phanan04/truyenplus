package ireader.presentation.ui.truyenplus
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.onSizeChanged

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import ireader.domain.models.BookCover
import ireader.domain.models.entities.BookCategory
import ireader.domain.models.entities.LibraryBook
import ireader.domain.models.entities.ReadingStatisticsType1
import ireader.domain.models.entities.toBookCategory
import ireader.domain.models.library.LibrarySort
import ireader.domain.usecases.statistics.GetReadingStatisticsUseCase
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

fun LibraryBook.toReaderBookUi() = ReaderBookUi(id, title, BookCover.from(toBookItem()), readCount, totalChapters, isPinned, lastRead = lastRead)

@Composable
fun ReaderShellRoute(initialIndex: Int = 0) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val navigator = requireNotNull(LocalNavigator.current)
    val vm: LibraryViewModel = getIViewModel(key = "library")
    val state by vm.state.collectAsState()
    val tts: ireader.domain.services.tts_service.v2.TTSController = koinInject()
    val ttsState by remember(tts) { tts.state.readerUpdates() }.collectAsState(initial=tts.state.value.readerProjection())
    val ttsService: ireader.domain.services.tts_service.v2.TTSV2ServiceStarter = koinInject()
    var destinationIndex by rememberSaveable { mutableIntStateOf(initialIndex.coerceIn(0, ReaderDestination.entries.lastIndex)) }
    val destination = ReaderDestination.entries[destinationIndex]
    var showImport by rememberSaveable { mutableStateOf(false) }
    var showProgress by rememberSaveable { mutableStateOf(false) }
    val uiPreferences: ireader.domain.preferences.prefs.UiPreferences = koinInject()
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
    var updateJob by remember { mutableStateOf<Job?>(null) }
    val books = state.books.map { it.toReaderBookUi() }
    val onBook: (Long) -> Unit = { navigator.navigateTo(BookDetailScreenSpec(it)) }
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
        if (state.selectionMode) vm.unselectAll() else if (showProgress) showProgress = false else destinationIndex = 0
    }
    var dockHeight by remember { mutableStateOf(185.dp) }
    val viewportDensity = androidx.compose.ui.platform.LocalDensity.current
    ReaderBackdrop {
        CompositionLocalProvider(LocalReaderDockInset provides dockHeight + 16.dp) {
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            ReaderContentPane {
            when (destination) {
                ReaderDestination.Home -> ReaderHomeScreen(books, state.isLoading, onBook,
                    onLibrary = { destinationIndex = 1 }, onImport = { showImport = true },
                    onSearch = { destinationIndex = 1; vm.setSearchMode(true) }, onUpdate = { showUpdate = true })
                ReaderDestination.Library -> ReaderLibraryScreen(books,
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
                    onSort = { vm.toggleSort(if (state.sort.type == LibrarySort.Type.Title) LibrarySort.Type.LastRead else LibrarySort.Type.Title) },
                    onLoadMore = { vm.loadMoreBooks(state.selectedCategory?.id ?: 0L) })
                ReaderDestination.Discover -> {
                    val discoveryVm: DiscoveryViewModel = getIViewModel()
                    val discoveryState by discoveryVm.state.collectAsState()
                    LaunchedEffect(discoveryVm) { discoveryVm.load() }
                    val translationState by discoveryVm.translated.collectAsState()
                    DiscoveryScreen(discoveryState, discoveryVm::refresh, translationState, language.code,
                        discoveryVm::configure, discoveryVm::retryTranslation, discoveryVm::loadMore)
                }
                ReaderDestination.Settings -> {
                    if (!showProgress) ReaderSettingsScreen(language, onLanguage = { selected ->
                        uiPreferences.language().set(selected.code)
                        localeHelper.updateLocal()
                    }, onProgress = { showProgress = true }, onUpdate = { showUpdate = true },
                        onAppearance = { uiPreferences.readerAppearance().set(it.id) })
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
            Column(Modifier.align(Alignment.BottomCenter).widthIn(max=1120.dp).fillMaxWidth().onSizeChanged {
                dockHeight = with(viewportDensity) { it.height.toDp() }
            }) {
                if (state.selectionMode) {
                    Surface(shape = MaterialTheme.shapes.large, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Column(Modifier.heightIn(max=LocalReaderViewport.current.height * .45f).verticalScroll(androidx.compose.foundation.rememberScrollState()).padding(12.dp)) {
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = vm::unselectAll) { Text(strings.localize(Res.string.tp_unselect)) }
                                TextButton(onClick = { showCategories = true }) { Text(strings.localize(Res.string.tp_collections)) }
                                TextButton(onClick = { confirmRemove = true }) { Text(strings.localize(Res.string.tp_remove_books)) }
                            }
                            FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TextButton(onClick = { vm.markAsReadWithUndo(); vm.unselectAll() }) { Text(strings.localize(Res.string.tp_mark_read)) }
                                TextButton(onClick = { vm.markAsUnreadWithUndo(); vm.unselectAll() }) { Text(strings.localize(Res.string.tp_unread)) }
                            }
                        }
                    }
                } else {
                    val listeningBook = ttsState.book
                    val listeningChapter = ttsState.chapter
                    if (listeningBook != null && listeningChapter != null) {
                        val openPlayer: () -> Unit = { navigator.navigateTo(TTSV2ScreenSpec(listeningBook.id, listeningChapter.id, listeningBook.sourceId, ttsState.currentParagraphIndex)) }
                        ReaderMiniPlayer(ReaderBookUi(listeningBook.id, listeningBook.title, BookCover.from(listeningBook), chapter = listeningChapter.name),
                            onRead = openPlayer, onListen = openPlayer, isPlaying = ttsState.isPlaying,
                            onTogglePlayback = {
                                if (tts.state.value.isPlaying || tts.state.value.isLoading) tts.dispatch(ireader.domain.services.tts_service.v2.TTSCommand.Pause)
                                else {
                                    ttsService.startService(listeningBook.id, listeningChapter.id, ttsState.currentParagraphIndex)
                                    val current = tts.state.value
                                    tts.dispatch(if (current.isPaused && current.engineType == ireader.domain.services.tts_service.v2.EngineType.EDGE)
                                        ireader.domain.services.tts_service.v2.TTSCommand.Resume else ireader.domain.services.tts_service.v2.TTSCommand.Play)
                                }
                            })
                    } else {
                    state.lastReadInfo?.let { info ->
                        val book = books.firstOrNull { it.id == info.novelId }
                            ?: ReaderBookUi(info.novelId, info.novelTitle, chapter = info.chapterTitle)
                        ReaderMiniPlayer(book.copy(chapter = info.chapterTitle),
                            onRead = { navigator.navigateTo(ReaderScreenSpec(info.novelId, info.chapterId)) },
                            onListen = {
                                val original = state.books.firstOrNull { it.id == info.novelId }
                                if (original != null) navigator.navigateTo(TTSV2ScreenSpec(original.id, info.chapterId, original.sourceId, 0))
                                else onBook(info.novelId)
                            })
                    }
                    }
                    ReaderBottomNav(destination) { destinationIndex = it.ordinal }
                }
            }
        }
        }
    }
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
        if (uris.isNotEmpty()) vm.importEpubFiles(uris.map { it.toString() })
    }
    OnShowImportPdf(show = state.showImportPdfDialog) { uris ->
        vm.setShowImportPdfDialog(false)
        if (uris.isNotEmpty()) vm.importPdfFiles(uris.map { it.toString() })
    }
    if (confirmRemove) AlertDialog(onDismissRequest = { confirmRemove = false }, title = { Text(strings.localize(Res.string.tp_remove_title)) }, text = { Text(strings.localize(Res.string.tp_remove_count, state.selectedCount)) },
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
