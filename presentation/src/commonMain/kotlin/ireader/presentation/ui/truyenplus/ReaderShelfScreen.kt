package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.models.shelf.*
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable fun ReaderLibrarySourceTabs(inShelf:Boolean,onLibrary:()->Unit,onShelf:()->Unit) {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 Row(Modifier.fillMaxWidth().padding(top=14.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
  FilterChip(!inShelf,onLibrary,label={Text(strings.localize(Res.string.tp_shelf_downloaded))},modifier=Modifier.weight(1f))
  FilterChip(inShelf,onShelf,label={Text(strings.localize(Res.string.tp_shelf))},modifier=Modifier.weight(1f))
 }
}

@Composable fun shelfFailureText(reason:ShelfFailure):String {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 return strings.localize(when(reason) {
  ShelfFailure.InvalidLink->Res.string.tp_shelf_invalid_link
  ShelfFailure.Authentication->Res.string.tp_shelf_auth_error
  ShelfFailure.AccessDenied->Res.string.tp_shelf_access_error
  ShelfFailure.RateLimited->Res.string.tp_shelf_rate_error
  ShelfFailure.InvalidFile->Res.string.tp_shelf_invalid_file
  ShelfFailure.FileTooLarge->Res.string.tp_shelf_large_file
  ShelfFailure.ImportFailed->Res.string.tp_shelf_import_error
  ShelfFailure.Unsupported->Res.string.tp_shelf_unsupported
  ShelfFailure.Configuration->Res.string.tp_shelf_configuration
  ShelfFailure.Network,ShelfFailure.InvalidCursor->Res.string.tp_shelf_network_error
 })
}

@Composable fun ReaderShelfRoute(onLibrary:()->Unit,onBook:(Long)->Unit,requestSearch:Boolean=false,onSearchFocused:()->Unit={}) {
 val repo:DriveShelfRepository=koinInject()
 val strings=requireNotNull(LocalLocalizeHelper.current)
 val sources by repo.sources.collectAsState(emptyList())
 val operations by repo.operations.collectAsState()
 var query by rememberSaveable { mutableStateOf("") }
 var searchTerm by remember { mutableStateOf("") }
 var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
 var offset by rememberSaveable { mutableIntStateOf(0) }
 var page by remember { mutableStateOf(ShelfSearchPage(emptyList(),false)) }
 var loading by remember { mutableStateOf(false) }
 var showAdd by rememberSaveable { mutableStateOf(false) }
 var link by rememberSaveable { mutableStateOf("") }
 var name by rememberSaveable { mutableStateOf("") }
 var adding by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<ShelfFailure?>(null) }
 var removeSource by remember { mutableStateOf<ShelfSource?>(null) }
 var retryAdd by remember { mutableStateOf(false) }
 val scope=rememberCoroutineScope()
 fun add() { scope.launch {
  adding=true;error=null
  try { repo.add(link,name);showAdd=false;link="";name="";retryAdd=false }
  catch(e:kotlinx.coroutines.CancellationException){throw e}
  catch(e:Exception){error=(e as? ShelfException)?.reason?:ShelfFailure.Network}
  finally{adding=false}
 } }
 val connect=rememberShelfAuthorization(onSuccess={error=null;if(retryAdd)add() else sources.forEach { repo.refresh(it.id) }},onFailure={code->error=when(code){"configuration"->ShelfFailure.Configuration;"unsupported"->ShelfFailure.Unsupported;else->ShelfFailure.Authentication}})
 LaunchedEffect(Unit){repo.refreshStale()}
 LaunchedEffect(query) { delay(250);searchTerm=query }
 LaunchedEffect(searchTerm,selectedSource,offset,sources,operations.mapValues { it.value.task }) {
  if(searchTerm.isBlank()){page=ShelfSearchPage(emptyList(),false);loading=false;return@LaunchedEffect}
  loading=true
  try { page=repo.search(searchTerm,selectedSource,offset) }
  finally{loading=false}
 }
 ReaderShelfScreen(sources,query,page,loading,selectedSource,operations,
  onQuery={query=it;offset=0;page=ShelfSearchPage(emptyList(),false)},onSource={selectedSource=it;offset=0},onAdd={showAdd=true;error=null},onRefresh=repo::refresh,
  onRemove={removeSource=it},onDownload=repo::download,onBook=onBook,onLibrary=onLibrary,
  onPrevious={offset=(offset-40).coerceAtLeast(0)},onNext={offset+=40},onConnect={retryAdd=false;connect()},onCancel=repo::cancel,
  requestSearch=requestSearch,onSearchFocused=onSearchFocused,offset=offset,error=error,showConnect=!repo.publicAccess)
 if(showAdd) AlertDialog(onDismissRequest={if(!adding){showAdd=false;retryAdd=false}},title={Text(strings.localize(Res.string.tp_shelf_add))},text={Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
  OutlinedTextField(link,{link=it},label={Text(strings.localize(Res.string.tp_shelf_link))},singleLine=true,enabled=!adding)
  OutlinedTextField(name,{name=it},label={Text(strings.localize(Res.string.tp_shelf_name))},singleLine=true,enabled=!adding)
  if(!repo.publicAccess)Text(strings.localize(Res.string.tp_shelf_auth_hint),style=MaterialTheme.typography.bodySmall)
  error?.let { Text(shelfFailureText(it),color=MaterialTheme.colorScheme.error) }
 }},confirmButton={TextButton(onClick={retryAdd=true;add()},enabled=!adding && link.isNotBlank()){Text(strings.localize(if(adding)Res.string.tp_shelf_busy else Res.string.tp_shelf_add))}},dismissButton={
  Row { if(error==ShelfFailure.Authentication)TextButton(onClick={retryAdd=true;connect()}){Text(strings.localize(Res.string.tp_shelf_connect))};TextButton(onClick={showAdd=false;retryAdd=false},enabled=!adding){Text(strings.localize(Res.string.tp_cancel))} }
 })
 removeSource?.let { source->AlertDialog(onDismissRequest={removeSource=null},title={Text(strings.localize(Res.string.tp_shelf_remove))},text={Text(strings.localize(Res.string.tp_shelf_remove_hint))},confirmButton={TextButton(onClick={scope.launch{repo.remove(source.id);if(selectedSource==source.id)selectedSource=null};removeSource=null}){Text(strings.localize(Res.string.tp_shelf_remove))}},dismissButton={TextButton(onClick={removeSource=null}){Text(strings.localize(Res.string.tp_cancel))}}) }
}

@Composable fun ReaderShelfScreen(sources:List<ShelfSource>,query:String,page:ShelfSearchPage,loading:Boolean,selectedSource:String?,operations:Map<String,ShelfOperation>,
 onQuery:(String)->Unit,onSource:(String?)->Unit,onAdd:()->Unit,onRefresh:(String)->Unit,onRemove:(ShelfSource)->Unit,onDownload:(ShelfEntry)->Unit,onBook:(Long)->Unit,onLibrary:()->Unit,onPrevious:()->Unit,onNext:()->Unit,onConnect:()->Unit,onCancel:(String)->Unit,
 requestSearch:Boolean=false,onSearchFocused:()->Unit={},offset:Int=0,error:ShelfFailure?=null,showConnect:Boolean=true) {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 val focus=remember { FocusRequester() };val keyboard=LocalSoftwareKeyboardController.current;val list=rememberLazyListState()
 LaunchedEffect(requestSearch){if(requestSearch){list.scrollToItem(0);withFrameNanos{};focus.requestFocus();keyboard?.show();onSearchFocused()}}
 LazyColumn(state=list,modifier=Modifier.fillMaxSize(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=18.dp,bottom=LocalReaderDockInset.current),verticalArrangement=Arrangement.spacedBy(16.dp)) {
  item { Column {
   Text(strings.localize(Res.string.tp_library),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
   Text(strings.localize(Res.string.tp_library_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
   ReaderLibrarySourceTabs(true,onLibrary,{})
   OutlinedTextField(query,onQuery,modifier=Modifier.fillMaxWidth().padding(top=12.dp).focusRequester(focus).testTag("shelf-query"),singleLine=true,placeholder={Text(strings.localize(Res.string.tp_shelf_search))},leadingIcon={Icon(Icons.Outlined.Search,null)},shape=CircleShape)
  } }
  item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
   Text(strings.localize(Res.string.tp_shelf_sources),style=MaterialTheme.typography.titleMedium,modifier=Modifier.weight(1f))
   TextButton(onClick=onAdd){Icon(Icons.Outlined.Add,null,Modifier.size(18.dp));Text(strings.localize(Res.string.tp_shelf_add))}
  } }
  if(showConnect && sources.isNotEmpty())item { TextButton(onClick=onConnect){Text(strings.localize(Res.string.tp_shelf_connect))} }
  items(sources,key={"source:${it.id}"}) { source ->
   val op=operations[source.id]
   Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.surfaceContainerLow) {
    Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
     Row(verticalAlignment=Alignment.CenterVertically) {
      Icon(Icons.Outlined.Folder,null,Modifier.size(28.dp));Spacer(Modifier.width(12.dp))
      Column(Modifier.weight(1f)) { Text(source.name,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis);Text("Google Drive",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
      IconButton(onClick={onRemove(source)}){Icon(Icons.Outlined.Close,strings.localize(Res.string.tp_shelf_remove))}
     }
     Text(strings.localize(if(op?.task==ShelfTask.Syncing)Res.string.tp_shelf_indexing else if(source.updatedAt==0L && source.entryCount==0L)Res.string.tp_shelf_unsynced else Res.string.tp_shelf_count,source.entryCount.toString()),style=MaterialTheme.typography.bodySmall)
     if(op?.task==ShelfTask.Paused)Text(strings.localize(Res.string.tp_shelf_paused),style=MaterialTheme.typography.bodySmall)
     op?.failure?.let { Text(shelfFailureText(it),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error) }
     Row {
      if(op?.task==ShelfTask.Syncing)TextButton(onClick={onCancel(source.id)}){Text(strings.localize(Res.string.tp_cancel))}
      else TextButton(onClick={onRefresh(source.id)}){Text(strings.localize(if(source.pending)Res.string.tp_shelf_resume else Res.string.tp_shelf_refresh))}
     }
    }
   }
  }
  error?.let { item{Text(shelfFailureText(it),color=MaterialTheme.colorScheme.error)} }
  if(query.isBlank())item {
   Column(Modifier.fillMaxWidth().padding(vertical=24.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(10.dp)) {
    Icon(Icons.Outlined.MenuBook,null,Modifier.size(36.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
    Text(strings.localize(Res.string.tp_shelf_empty_title),style=MaterialTheme.typography.titleMedium)
    Text(strings.localize(if(sources.isEmpty())Res.string.tp_shelf_no_sources else Res.string.tp_shelf_empty_hint),color=MaterialTheme.colorScheme.onSurfaceVariant)
   }
  } else {
   if(sources.size>1)item { ReaderChips(listOf(strings.localize(Res.string.tp_shelf_all_sources))+sources.map{it.name},sources.indexOfFirst{it.id==selectedSource}+1,{onSource(if(it==0)null else sources[it-1].id)}) }
   item{Text(strings.localize(Res.string.tp_shelf_results),style=MaterialTheme.typography.titleMedium)}
   if(loading)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
   if(!loading && page.entries.isEmpty())item{Text(strings.localize(Res.string.tp_shelf_no_results),color=MaterialTheme.colorScheme.onSurfaceVariant)}
   items(page.entries,key={"${it.sourceId}:${it.fileId}"}) { entry->
    val key="${entry.sourceId}:${entry.fileId}";val op=operations[key];val bookId=entry.bookId;val total=op?.totalBytes
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
     Text(entry.name.removeSuffix(".epub"),style=MaterialTheme.typography.titleMedium,maxLines=3,overflow=TextOverflow.Ellipsis)
     Text(sources.firstOrNull{it.id==entry.sourceId}?.name?:"Google Drive",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     if(entry.bookId==null)Text(strings.localize(Res.string.tp_shelf_metadata),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
     op?.failure?.let{Text(shelfFailureText(it),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
     Row(verticalAlignment=Alignment.CenterVertically) {
      if(op?.task==ShelfTask.Downloading || op?.task==ShelfTask.Importing) {
       Text(strings.localize(if(op.task==ShelfTask.Importing)Res.string.tp_shelf_importing else Res.string.tp_shelf_downloading),modifier=Modifier.weight(1f),style=MaterialTheme.typography.bodySmall)
       if(op.task==ShelfTask.Downloading)TextButton(onClick={onCancel(key)}){Text(strings.localize(Res.string.tp_cancel))}
      } else Button(onClick={if(bookId!=null)onBook(bookId) else onDownload(entry)},modifier=Modifier.testTag("shelf-download")) { Text(strings.localize(if(entry.bookId!=null)Res.string.tp_shelf_open else Res.string.tp_shelf_download)) }
     }
     if(op?.task==ShelfTask.Downloading && total!=null && total>0)LinearProgressIndicator(progress={(op.bytes.toFloat()/total).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
     HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
   }
   if(offset>0 || page.hasMore)item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) { TextButton(onClick=onPrevious,enabled=offset>0){Text(strings.localize(Res.string.tp_shelf_previous))};TextButton(onClick=onNext,enabled=page.hasMore){Text(strings.localize(Res.string.tp_shelf_next))} } }
  }
 }
}
