package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

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
 var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
 var offset by rememberSaveable { mutableIntStateOf(0) }
 var showAdd by rememberSaveable { mutableStateOf(false) }
 var link by rememberSaveable { mutableStateOf("") }
 var name by rememberSaveable { mutableStateOf("") }
 var adding by remember { mutableStateOf(false) }
 var error by remember { mutableStateOf<ShelfFailure?>(null) }
 var removeSource by remember { mutableStateOf<ShelfSource?>(null) }
 var retryAdd by remember { mutableStateOf(false) }
 val scope=rememberCoroutineScope()
 val searches=remember(repo,scope){ShelfSearchController(scope,repo::search)}
 val searchState by searches.state.collectAsState()
 DisposableEffect(searches){onDispose{searches.invalidate(false)}}
 fun add() { scope.launch {
  adding=true;error=null
  try { repo.add(link,name);showAdd=false;link="";name="";retryAdd=false }
  catch(e:kotlinx.coroutines.CancellationException){throw e}
  catch(e:Exception){error=(e as? ShelfException)?.reason?:ShelfFailure.Network}
  finally{adding=false}
 } }
 val connect=rememberShelfAuthorization(onSuccess={error=null;if(retryAdd)add() else sources.forEach { repo.refresh(it.id) }},onFailure={code->error=when(code){"configuration"->ShelfFailure.Configuration;"unsupported"->ShelfFailure.Unsupported;else->ShelfFailure.Authentication}})
 LaunchedEffect(Unit){repo.refreshStale()}
 LaunchedEffect(query,selectedSource,offset,sources,operations.mapValues { it.value.task }) {
  searches.submit(query,selectedSource,offset)
 }
 ReaderShelfScreen(sources,query,searchState.page,searchState.loading,selectedSource,operations,
  onQuery={query=it;offset=0;searches.invalidate(it.isNotBlank())},onSource={selectedSource=it;offset=0;searches.invalidate(query.isNotBlank())},onAdd={showAdd=true;error=null},onRefresh=repo::refresh,
  onRemove={removeSource=it},onDownload=repo::download,onBook=onBook,onLibrary=onLibrary,
  onPrevious={offset=(offset-40).coerceAtLeast(0);searches.invalidate(query.isNotBlank())},onNext={offset+=40;searches.invalidate(query.isNotBlank())},onConnect={retryAdd=false;connect()},onCancel=repo::cancel,
  requestSearch=requestSearch,onSearchFocused=onSearchFocused,offset=offset,error=error?:searchState.failure,showConnect=!repo.publicAccess)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ReaderShelfScreen(sources:List<ShelfSource>,query:String,page:ShelfSearchPage,loading:Boolean,selectedSource:String?,operations:Map<String,ShelfOperation>,
 onQuery:(String)->Unit,onSource:(String?)->Unit,onAdd:()->Unit,onRefresh:(String)->Unit,onRemove:(ShelfSource)->Unit,onDownload:(ShelfEntry)->Unit,onBook:(Long)->Unit,onLibrary:()->Unit,onPrevious:()->Unit,onNext:()->Unit,onConnect:()->Unit,onCancel:(String)->Unit,
 requestSearch:Boolean=false,onSearchFocused:()->Unit={},offset:Int=0,error:ShelfFailure?=null,showConnect:Boolean=true) {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 val focus=remember { FocusRequester() }
 val keyboard=LocalSoftwareKeyboardController.current
 var manage by rememberSaveable { mutableStateOf(false) }
 val list=rememberLazyListState()
 LaunchedEffect(requestSearch){if(requestSearch){focus.requestFocus();keyboard?.show();onSearchFocused()}}
 LaunchedEffect(query,selectedSource,offset){list.scrollToItem(0)}
 Column(Modifier.fillMaxSize()) {
  Row(Modifier.fillMaxWidth().padding(start=8.dp,end=12.dp,top=8.dp,bottom=8.dp),verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onLibrary,modifier=Modifier.size(48.dp).testTag("shelf-back")){Icon(Icons.Outlined.ChevronLeft,strings.localize(Res.string.tp_back))}
   Text(strings.localize(Res.string.tp_shelf),Modifier.weight(1f),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold)
   IconButton(onClick={keyboard?.hide();manage=true},modifier=Modifier.size(48.dp).testTag("shelf-manage")){Icon(Icons.Outlined.Tune,strings.localize(Res.string.tp_shelf_sources))}
   IconButton(onClick=onAdd,modifier=Modifier.size(48.dp)){Icon(Icons.Outlined.Add,strings.localize(Res.string.tp_shelf_add))}
  }
  OutlinedTextField(query,onQuery,modifier=Modifier.fillMaxWidth().padding(horizontal=20.dp).readerGlassCard(28).focusRequester(focus).testTag("shelf-query"),
   singleLine=true,placeholder={Text(strings.localize(Res.string.tp_shelf_search))},leadingIcon={Icon(Icons.Outlined.Search,null)},
   trailingIcon={if(query.isNotEmpty())IconButton(onClick={onQuery("")}){Icon(Icons.Outlined.Close,strings.localize(Res.string.close))}},shape=CircleShape,
   colors=OutlinedTextFieldDefaults.colors(unfocusedBorderColor=Color.Transparent,focusedBorderColor=MaterialTheme.colorScheme.outline.copy(alpha=.4f)))
  if(sources.size>1) ReaderChips(listOf(strings.localize(Res.string.tp_shelf_all_sources))+sources.map{it.name},sources.indexOfFirst{it.id==selectedSource}+1,
   {onSource(if(it==0)null else sources[it-1].id)})
  LazyColumn(state=list,modifier=Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(start=20.dp,end=20.dp,top=20.dp,bottom=LocalReaderDockInset.current),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   error?.let { item{Text(shelfFailureText(it),color=MaterialTheme.colorScheme.error)} }
   if(query.isBlank()) {
    if(sources.isNotEmpty())item{Text(strings.localize(Res.string.tp_shelf_sources),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)}
    items(sources,key={"source:${it.id}"}) { source ->
     ShelfSourceCard(source,operations[source.id],onRefresh,onRemove,onCancel,
      onSelect={onSource(source.id);focus.requestFocus();keyboard?.show()})
    }
    if(showConnect && sources.isNotEmpty())item { TextButton(onClick=onConnect){Text(strings.localize(Res.string.tp_shelf_connect))} }
    item {
     Column(Modifier.fillMaxWidth().padding(vertical=36.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(12.dp)) {
      Icon(Icons.Outlined.ManageSearch,null,Modifier.size(40.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
      Text(strings.localize(Res.string.tp_shelf_empty_title),style=MaterialTheme.typography.titleMedium)
      if(sources.isEmpty())OutlinedButton(onClick=onAdd,shape=CircleShape){Icon(Icons.Outlined.Add,null);Spacer(Modifier.width(8.dp));Text(strings.localize(Res.string.tp_shelf_add))}
     }
    }
   } else {
    item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
     Text(strings.localize(Res.string.tp_shelf_results),style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.onSurfaceVariant,modifier=Modifier.weight(1f))
     if(loading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp)
    } }
    if(!loading && page.entries.isEmpty())item{Text(strings.localize(Res.string.tp_shelf_no_results),color=MaterialTheme.colorScheme.onSurfaceVariant)}
    items(page.entries,key={"${it.sourceId}:${it.fileId}"}) { entry ->
     ShelfBookRow(entry,sources.firstOrNull{it.id==entry.sourceId}?.name?:"Google Drive",operations["${entry.sourceId}:${entry.fileId}"],
      onDownload={onDownload(entry)},onOpen={entry.bookId?.let(onBook)},onCancel={onCancel("${entry.sourceId}:${entry.fileId}")})
    }
    if(offset>0 || page.hasMore)item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
     TextButton(onClick=onPrevious,enabled=offset>0 && !loading){Text(strings.localize(Res.string.tp_shelf_previous))}
     TextButton(onClick=onNext,enabled=page.hasMore && !loading){Text(strings.localize(Res.string.tp_shelf_next))}
    } }
   }
  }
 }
 if(manage) ModalBottomSheet(onDismissRequest={manage=false},containerColor=MaterialTheme.colorScheme.surface) {
  LazyColumn(Modifier.fillMaxWidth().heightIn(max=LocalReaderViewport.current.height*.7f),contentPadding=PaddingValues(start=20.dp,end=20.dp,bottom=24.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
   item { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
    Text(strings.localize(Res.string.tp_shelf_sources),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge)
    TextButton(onClick={manage=false;onAdd()}){Icon(Icons.Outlined.Add,null);Text(strings.localize(Res.string.tp_shelf_add))}
   } }
   items(sources,key={it.id}) { source->ShelfSourceCard(source,operations[source.id],onRefresh,onRemove,onCancel,onSelect={onSource(source.id);manage=false}) }
   if(showConnect && sources.isNotEmpty())item { TextButton(onClick={manage=false;onConnect()}){Text(strings.localize(Res.string.tp_shelf_connect))} }
  }
 }
}

@Composable private fun ShelfSourceCard(source:ShelfSource,op:ShelfOperation?,onRefresh:(String)->Unit,onRemove:(ShelfSource)->Unit,onCancel:(String)->Unit,onSelect:()->Unit) {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 var menu by remember { mutableStateOf(false) }
 Surface(onClick=onSelect,shape=RoundedCornerShape(20.dp),color=Color.Transparent,modifier=Modifier.fillMaxWidth().readerGlassCard(20)) {
  Column(Modifier.padding(start=16.dp,end=8.dp,top=10.dp,bottom=10.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically) {
    Icon(Icons.Outlined.CloudQueue,null,Modifier.size(26.dp));Spacer(Modifier.width(14.dp))
    Column(Modifier.weight(1f)) {
     Text(source.name,style=MaterialTheme.typography.titleSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
     Text(strings.localize(if(op?.task==ShelfTask.Syncing)Res.string.tp_shelf_indexing else if(source.entryCount==0L && source.updatedAt==0L)Res.string.tp_shelf_unsynced else Res.string.tp_shelf_count,source.entryCount.toString()),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if(op?.task==ShelfTask.Syncing)IconButton(onClick={onCancel(source.id)}){Icon(Icons.Outlined.Close,strings.localize(Res.string.tp_cancel))}
    else IconButton(onClick={onRefresh(source.id)}){Icon(Icons.Outlined.Refresh,strings.localize(if(source.pending)Res.string.tp_shelf_resume else Res.string.tp_shelf_refresh))}
    Box {
     IconButton(onClick={menu=true}){Icon(Icons.Outlined.MoreVert,strings.localize(Res.string.expand_menu))}
     DropdownMenu(menu,onDismissRequest={menu=false}) {
      DropdownMenuItem(text={Text(strings.localize(Res.string.tp_shelf_remove))},onClick={menu=false;onRemove(source)},leadingIcon={Icon(Icons.Outlined.DeleteOutline,null)})
     }
    }
   }
   if(op?.task==ShelfTask.Paused)Text(strings.localize(Res.string.tp_shelf_paused),style=MaterialTheme.typography.bodySmall)
   op?.failure?.let{Text(shelfFailureText(it),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
  }
 }
}

@Composable private fun ShelfBookRow(entry:ShelfEntry,sourceName:String,op:ShelfOperation?,onDownload:()->Unit,onOpen:()->Unit,onCancel:()->Unit) {
 val strings=requireNotNull(LocalLocalizeHelper.current)
 val filename=entry.name.replace(Regex("(?i)\\.epub$"),"").replace('_',' ').trim()
 val divider=filename.lastIndexOf(" - ")
 val title=if(divider>0)filename.substring(0,divider).trim() else filename
 val author=if(divider>0)filename.substring(divider+3).trim() else ""
 val busy=op?.task==ShelfTask.Downloading || op?.task==ShelfTask.Importing
 Surface(shape=RoundedCornerShape(20.dp),color=Color.Transparent,modifier=Modifier.fillMaxWidth().readerGlassCard(20)) {
  Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
   Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    // Only real filename metadata is available before import; no invented cover.
    Surface(shape=RoundedCornerShape(12.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.05f),modifier=Modifier.size(46.dp,64.dp)) {
     Box(contentAlignment=Alignment.Center){Icon(Icons.Outlined.MenuBook,null,Modifier.size(24.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)}
    }
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
     Text(title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.SemiBold,maxLines=3,overflow=TextOverflow.Ellipsis)
     if(author.isNotBlank())Text(author,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
     Text(sourceName,style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
    if(busy) {
     if(op?.task==ShelfTask.Downloading)IconButton(onClick=onCancel){Icon(Icons.Outlined.Close,strings.localize(Res.string.tp_cancel))}
     else CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp)
    } else IconButton(onClick=if(entry.bookId==null)onDownload else onOpen,modifier=Modifier.size(48.dp).testTag("shelf-download")) {
     Icon(if(entry.bookId==null)Icons.Outlined.FileDownload else Icons.Outlined.AutoStories,strings.localize(if(entry.bookId==null)Res.string.tp_shelf_download else Res.string.tp_shelf_open))
    }
   }
   op?.failure?.let{Text(shelfFailureText(it),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.error)}
   if(busy) {
    Text(strings.localize(if(op?.task==ShelfTask.Importing)Res.string.tp_shelf_importing else Res.string.tp_shelf_downloading),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    val total=op?.totalBytes
    if(op?.task==ShelfTask.Downloading && total!=null && total>0)LinearProgressIndicator(progress={(op.bytes.toFloat()/total).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(2.dp),drawStopIndicator={})
   }
  }
 }
}
