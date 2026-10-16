package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassButton as Button
import ireader.presentation.ui.truyenplus.ReaderGlassTextButton as TextButton
import ireader.presentation.ui.truyenplus.ReaderGlassIconButton as IconButton

import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

import ireader.presentation.ui.truyenplus.ReaderGlassGroupSurface as Surface
import ireader.presentation.ui.truyenplus.ReaderGlassSnackbarHost as SnackbarHost
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import kotlin.reflect.KClass
import ireader.domain.models.BookCover
import ireader.domain.models.entities.Chapter
import ireader.presentation.core.*
import ireader.presentation.core.ui.*
import ireader.presentation.ui.book.components.*
import ireader.presentation.ui.book.viewmodel.*
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.*
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.koin.compose.koinInject
import ireader.domain.usecases.epub.ImportEpub
import ireader.domain.data.repository.BookMetadataLookup
import ireader.domain.data.repository.BookRepository
import ireader.domain.preferences.prefs.AppPreferences
import ireader.presentation.ui.settings.advance.OnShowImportEpub

/** Each inline detail has an isolated store, so opening another book cannot reuse its state. */
@Composable
internal fun ReaderEmbeddedBookDetails(bookId:Long,onDismiss:()->Unit,bottomInset:Dp) {
    val owner=remember(bookId) {object:ViewModelStoreOwner {override val viewModelStore=ViewModelStore()}}
    var model by remember(bookId) {mutableStateOf<BookDetailViewModel?>(null)}
    // Legacy detail initialization mutates snapshot fields. Run it after composition,
    // as a navigation event, rather than while the new page is being composed.
    LaunchedEffect(bookId) {
        val provider=ViewModelProvider.create(owner,object:ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T:ViewModel> create(modelClass:KClass<T>,extras:CreationExtras):T =
                org.koin.mp.KoinPlatform.getKoin().get<BookDetailViewModel>(parameters={parametersOf(BookDetailViewModel.Param(bookId))}) as T
        })
        model=provider["books-detail-$bookId",BookDetailViewModel::class]
    }
    DisposableEffect(owner) {onDispose {owner.viewModelStore.clear()}}
    val current=model
    if(current==null)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center) {CircularProgressIndicator()}
    else ReaderBookRoute(bookId,onDismiss,bottomInset,current)
}

@Composable
fun ReaderBookRoute(bookId: Long,onDismiss:(()->Unit)?=null,bottomInset:Dp=0.dp,detailModel:BookDetailViewModel?=null) {
    val strings = requireNotNull(LocalLocalizeHelper.current)
    val nav = requireNotNull(LocalNavigator.current)
    val vm: BookDetailViewModel = detailModel ?: koinViewModel(parameters={parametersOf(BookDetailViewModel.Param(bookId))})
    val dismiss=onDismiss ?: {nav.safePopBackStack();Unit}
    val state by vm.state.collectAsState()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val importer: ImportEpub = koinInject()
    val lookup: BookMetadataLookup = koinInject()
    val books: BookRepository = koinInject()
    val preferences: AppPreferences = koinInject()
    val playback: ireader.domain.services.tts_service.v2.TTSController = koinInject()
    val sourcePreference = remember(bookId) { preferences.bookMetadataSource(bookId) }
    val metadataSource by sourcePreference.changes().collectAsState(initial = sourcePreference.get())
    val uriHandler = LocalUriHandler.current
    var showMetadataChoice by remember { mutableStateOf(false) }
    var pickMetadataEpub by remember { mutableStateOf(false) }
    var updatingMetadata by remember { mutableStateOf(false) }
    OnShowImportEpub(pickMetadataEpub) { uris ->
        withContext(Dispatchers.Main) { pickMetadataEpub = false; updatingMetadata = uris.isNotEmpty() }
        if (uris.isNotEmpty()) {
            try {
                val previous = books.findBookById(bookId)
                importer.refreshMetadata(bookId, uris.first())
                val refreshed = books.findBookById(bookId)
                if (previous?.description != refreshed?.description) sourcePreference.set("")
                withContext(Dispatchers.Main) { updatingMetadata = false }
                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_updated))
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) {
                withContext(Dispatchers.Main) { updatingMetadata = false }
                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed))
            }
            finally { withContext(Dispatchers.Main) { updatingMetadata = false } }
        }
    }
    LaunchedEffect(bookId) { vm.onScreenResumed() }
    LaunchedEffect(vm) {
        vm.events.collectLatest { event ->
            when (event) {
                is BookDetailEvent.ShowSnackbar -> snackbars.showSnackbar(event.message)
                is BookDetailEvent.NavigateToReader -> nav.navigateTo(readerResumeDestination(event.bookId, event.chapterId,playback.state.value))
                BookDetailEvent.NavigateBack -> dismiss()
                else -> Unit // Online-source, migration and recommendation destinations are outside this app.
            }
        }
    }
    ReaderAppBackdrop {
    if (showMetadataChoice) ReaderGlassDialog(onDismissRequest = { showMetadataChoice = false },
        title = { Text(strings.localize(Res.string.tp_book_metadata)) }, text = {
            Column {
                TextButton(onClick = { showMetadataChoice = false; pickMetadataEpub = true }, Modifier.fillMaxWidth()) {
                    Text(strings.localize(Res.string.tp_metadata_epub))
                }
                TextButton(onClick = {
                    showMetadataChoice = false
                    scope.launch {
                        updatingMetadata = true
                        try {
                            val book = books.findBookById(bookId) ?: error("Book not found")
                            val found = lookup.find(book)
                            if (found == null) { updatingMetadata = false; snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed)) }
                            else {
                                withContext(Dispatchers.IO) { books.updateBook(found.book) }
                                sourcePreference.set(found.sourceUrl)
                                updatingMetadata = false
                                snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_updated))
                            }
                        } catch (cancel: CancellationException) { throw cancel }
                        catch (_: Exception) { updatingMetadata = false; snackbars.showSnackbar(strings.localize(Res.string.tp_metadata_failed)) }
                        finally { updatingMetadata = false }
                    }
                }, Modifier.fillMaxWidth()) { Text(strings.localize(Res.string.tp_metadata_online)) }
            }
        }, confirmButton = {})
        Box(Modifier.fillMaxSize().safeDrawingPadding()) {
            when (val current = state) {
                is BookDetailState.Success -> {
                    val chapters = remember(current.chapters) { current.chapters.sortedBy { it.number } }
                    val resume = chapters.firstOrNull { it.id == current.lastReadChapterId }
                        ?: chapters.firstOrNull { !it.read } ?: chapters.firstOrNull()
                    ReaderBookScreen(
                        book = ReaderBookUi(current.book.id, current.book.title, BookCover.from(current.book), chapters.count { it.read }, chapters.size),
                        author = current.book.author.orEmpty(), chapters = chapters, onBack = dismiss,
                        onRead = { resume?.let { nav.navigateTo(readerResumeDestination(bookId, it.id,playback.state.value)) } },
                        onListen = { resume?.let { nav.navigateTo(readerListeningDestination(bookId, it.id)) } },
                        onChapter = { nav.navigateTo(ReaderScreenSpec(bookId, it.id)) },
                        onExport = { vm.showEpubExportDialog = true },
                        description = current.book.description, activeChapterId = current.lastReadChapterId,
                        onMetadata = { showMetadataChoice = true }, metadataUpdating = updatingMetadata,bottomInset=bottomInset,
                        onMetadataSource = if (metadataSource.isNotBlank()) ({ uriHandler.openUri(metadataSource) }) else null)
                    if (vm.showEpubExportDialog) EpubExportDialog(current.book, current.chapters,
                        hasTranslations = false,
                        onExport = { options -> vm.showEpubExportDialog = false; vm.exportAsEpub(options) },
                        onDismiss = { vm.showEpubExportDialog = false })
                }
                BookDetailState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.align(Alignment.Center).padding(24.dp)) {
                    Text(strings.localize(Res.string.tp_cannot_open_book))
                    TextButton(onClick = dismiss) { Text(strings.localize(Res.string.tp_back_library)) }
                }
            }
            SnackbarHost(snackbars, Modifier.align(Alignment.BottomCenter))
        }
    }
    val exportProgress by vm.epubExportProgress.collectAsState()
    EpubExportProgressDialog(progress = exportProgress, onDismiss = vm::dismissEpubExportProgress)
}

@Composable
fun ReaderBookScreen(book:ReaderBookUi,author:String,chapters:List<Chapter>,onBack:()->Unit,
    onRead:()->Unit,onListen:()->Unit,onChapter:(Chapter)->Unit,onExport:()->Unit,
    description:String="",activeChapterId:Long?=null,onMetadata:()->Unit={},
    metadataUpdating:Boolean=false,onMetadataSource:(()->Unit)?=null,artworkColor:Color?=null,bottomInset:Dp=0.dp) {
    val strings=requireNotNull(LocalLocalizeHelper.current)
    var expandedSummary by rememberSaveable {mutableStateOf(false)}
    var contentsOpen by rememberSaveable {mutableStateOf(false)}
    val artwork=rememberReaderBookArtwork(book)
    val tone=artworkColor ?: artwork ?: MaterialTheme.colorScheme.surfaceContainerHigh
    val ink=if(artworkColor!=null || artwork!=null)Color.White else MaterialTheme.colorScheme.onSurface
    val summary:LazyListScope.()->Unit={
        item {
            Column(Modifier.fillMaxWidth().background(tone).testTag("book-artwork-hero"),horizontalAlignment=Alignment.CenterHorizontally) {
                Box(Modifier.fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter)) {
                    Box(Modifier.fillMaxWidth().padding(top=64.dp),contentAlignment=Alignment.Center) {
                        ReaderCover(book,Modifier.width(186.dp).aspectRatio(.64f).testTag("book-hero-cover").shadow(12.dp,androidx.compose.foundation.shape.RoundedCornerShape(4.dp)))
                    }
                    Row(Modifier.align(Alignment.TopCenter).fillMaxWidth().padding(top=12.dp),horizontalArrangement=Arrangement.SpaceBetween) {
                        Surface(shape=CircleShape,color=Color.Transparent,contentColor=ink,modifier=Modifier.size(48.dp).readerGlassCard(24,tone,ink)) {
                            IconButton(onBack,Modifier.fillMaxSize().testTag("book-detail-close")) {Icon(Icons.Default.Close,strings.localize(Res.string.tp_back))}
                        }
                        Surface(shape=CircleShape,color=Color.Transparent,contentColor=ink,modifier=Modifier.size(48.dp).readerGlassCard(24,tone,ink)) {
                            IconButton(onExport,Modifier.fillMaxSize()) {Icon(Icons.Outlined.IosShare,strings.localize(Res.string.tp_export_epub))}
                        }
                    }
                }
                Column(Modifier.widthIn(max=620.dp).fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter).padding(top=28.dp,bottom=28.dp),
                    horizontalAlignment=Alignment.CenterHorizontally) {
                    Text(book.title,modifier=Modifier.testTag("book-detail-title"),style=ReaderBooksTokens.section,color=ink,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    if(author.isNotBlank())Text(if(author=="Unknown")strings.localize(Res.string.unknown) else author,
                        style=MaterialTheme.typography.bodyLarge.copy(fontSize=16.sp,lineHeight=20.sp),color=ink.copy(alpha=.85f),modifier=Modifier.padding(top=8.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    if(book.total>0)Text(strings.localize(Res.string.tp_book_summary,book.total,"${book.progress.percent}%"),
                        style=MaterialTheme.typography.bodySmall,color=ink.copy(alpha=.75f),modifier=Modifier.padding(top=12.dp),textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                    Surface(Modifier.fillMaxWidth().padding(top=20.dp),shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),color=ink.copy(alpha=.1f)) {
                        BoxWithConstraints(Modifier.padding(14.dp)) {
                            val stacked=maxWidth.value/androidx.compose.ui.platform.LocalDensity.current.fontScale<260f
                            val readDescription=strings.localize(if(book.read>0)Res.string.tp_continue_reading else Res.string.tp_start_reading)
                            val read:@Composable (Modifier)->Unit={buttonModifier->
                                androidx.compose.material3.Button(onRead,enabled=chapters.isNotEmpty(),shape=CircleShape,
                                    colors=ButtonDefaults.buttonColors(containerColor=ink,contentColor=tone),contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp),
                                    modifier=buttonModifier.heightIn(min=48.dp).testTag("book-read").semantics {contentDescription=readDescription}) {
                                    Text(strings.localize(if(book.read>0)Res.string.continues else Res.string.read),style=MaterialTheme.typography.labelLarge,
                                        maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            val listen:@Composable (Modifier)->Unit={buttonModifier->
                                androidx.compose.material3.Button(onListen,enabled=chapters.isNotEmpty(),shape=CircleShape,
                                    colors=ButtonDefaults.buttonColors(containerColor=ink.copy(alpha=.14f),contentColor=ink),contentPadding=PaddingValues(horizontal=8.dp,vertical=10.dp),
                                    modifier=buttonModifier.heightIn(min=48.dp).testTag("book-listen")) {
                                    Icon(Icons.Outlined.Headphones,null,Modifier.size(18.dp));Spacer(Modifier.width(6.dp))
                                    Text(strings.localize(Res.string.tp_listen_book),style=MaterialTheme.typography.labelLarge,maxLines=1,overflow=TextOverflow.Ellipsis,textAlign=androidx.compose.ui.text.style.TextAlign.Center)
                                }
                            }
                            if(stacked)Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {read(Modifier.fillMaxWidth());listen(Modifier.fillMaxWidth())}
                            else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listen(Modifier.weight(1f));read(Modifier.weight(1f))}
                        }
                    }
                }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().padding(horizontal=ReaderBooksTokens.gutter).padding(top=28.dp,bottom=16.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(strings.localize(Res.string.tp_book_about),Modifier.weight(1f),style=ReaderBooksTokens.section)
                    IconButton(onMetadata,enabled=!metadataUpdating,modifier=Modifier.size(48.dp).testTag("book-metadata-refresh")) {Icon(Icons.Default.Refresh,strings.localize(Res.string.tp_book_metadata))}
                }
                Text(description.ifBlank {strings.localize(Res.string.tp_metadata_missing)},style=MaterialTheme.typography.bodyLarge.copy(lineHeight=25.sp),
                    color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=if(expandedSummary)Int.MAX_VALUE else 5,overflow=TextOverflow.Ellipsis,
                    modifier=Modifier.padding(top=12.dp).clickable(enabled=description.isNotBlank()) {expandedSummary=!expandedSummary})
                if(metadataUpdating)Text(strings.localize(Res.string.tp_metadata_updating),Modifier.padding(top=8.dp),style=MaterialTheme.typography.bodySmall)
                onMetadataSource?.let {action->TextButton(action) {Text(strings.localize(Res.string.tp_metadata_source)+" · TruyenFull")}}
            }
        }
        item {
            Box(Modifier.padding(horizontal=ReaderBooksTokens.gutter,vertical=16.dp).readerOptionalGlass(18).background(readerOptionalGlassColor(MaterialTheme.colorScheme.onSurface.copy(alpha=.04f)),ReaderBooksTokens.rowShape)) {
                ReaderBooksActionRow(strings.localize(Res.string.tp_chapters),chapters.size.toString(),Icons.Default.FormatListBulleted,
                    onClick={if(chapters.isNotEmpty())contentsOpen=true},modifier=Modifier.testTag("book-contents"),enabled=chapters.isNotEmpty())
            }
        }
    }
    ReaderContentPane {
        BoxWithConstraints {
            if(maxWidth.value/androidx.compose.ui.platform.LocalDensity.current.fontScale>=840f)Row(horizontalArrangement=Arrangement.spacedBy(24.dp)) {
                LazyColumn(Modifier.weight(.5f).fillMaxHeight(),contentPadding=PaddingValues(bottom=bottomInset+24.dp),content=summary)
                Box(Modifier.weight(.5f).fillMaxHeight()) {ReaderChapterContents(chapters,activeChapterId,onChapter,book=book)}
            } else LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(bottom=bottomInset+24.dp),content=summary)
        }
    }
    if(contentsOpen)ReaderGlassSheet(onDismissRequest={contentsOpen=false}) {
        Box(Modifier.fillMaxWidth().heightIn(max=LocalReaderViewport.current.height*.82f)) {
            ReaderChapterContents(chapters,activeChapterId,{contentsOpen=false;onChapter(it)},onDismiss={contentsOpen=false},book=book)
        }
    }
}
