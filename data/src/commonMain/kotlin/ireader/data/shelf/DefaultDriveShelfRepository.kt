package ireader.data.shelf

import ireader.domain.models.shelf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import okio.FileSystem
import okio.Path
import okio.buffer

class DefaultDriveShelfRepository(
 private val store:SqlShelfStore,
 private val api:ShelfDriveApi,
 private val files:FileSystem,
 private val directory:Path,
 private val scope:CoroutineScope,
 private val importer:suspend (String)->Long,
 private val now:()->Long,
):DriveShelfRepository {
 override val sources=store.sources
 override val operations=MutableStateFlow<Map<String,ShelfOperation>>(emptyMap())
 private val jobs=mutableMapOf<String,Job>()
 private val guard=Mutex()
 private val downloadLock=Mutex()
 private val scanLimit=Semaphore(2)
 private val cleanup=scope.launch(Dispatchers.IO) {
  try { files.listOrNull(directory).orEmpty().filter { it.name.endsWith(".part") || it.name.endsWith(".epub") }.forEach { files.delete(it,mustExist=false) } } catch(_:okio.IOException) {}
 }
 override suspend fun add(link:String,name:String):ShelfSource {
  val parsed=DriveFolderLink.parse(link)
  val folder=api.folder(parsed)
  val source=store.add(parsed.copy(resourceKey=folder.resourceKey?:parsed.resourceKey),name.ifBlank { folder.name })
  refresh(source.id);return source
 }
 override suspend fun rename(sourceId:String,name:String)=store.rename(sourceId,name)
 override suspend fun remove(sourceId:String) {
  val removed=guard.withLock { jobs.filterKeys { it==sourceId || it.startsWith("$sourceId:") }.values.toList().also { jobs.keys.removeAll { key->key==sourceId || key.startsWith("$sourceId:") } } }
  removed.forEach { it.cancelAndJoin() }
  operations.update { it.filterKeys { key->key!=sourceId && !key.startsWith("$sourceId:") } }
  store.remove(sourceId)
 }
 private fun enqueue(key:String,task:ShelfTask,block:suspend ()->Unit) {
  scope.launch {
   guard.withLock {
    if(jobs[key]?.isActive==true)return@withLock
    val job=scope.launch(start=CoroutineStart.LAZY) {
     operations.update { it+(key to ShelfOperation(task)) }
     try { block();operations.update { it-key } }
     catch(e:CancellationException) { operations.update { it+(key to ShelfOperation(ShelfTask.Paused)) };throw e }
     catch(e:Exception) { operations.update { it+(key to ShelfOperation(ShelfTask.Failed,failure=(e as? ShelfException)?.reason?:ShelfFailure.Network)) } }
     finally { guard.withLock { if(jobs[key]===currentCoroutineContext()[Job])jobs.remove(key) } }
    }
    jobs[key]=job;job.start()
   }
  }
 }
 override fun refresh(sourceId:String)=enqueue(sourceId,ShelfTask.Syncing) {
  scanLimit.withPermit {
  store.begin(sourceId)
  var restarted=false
  while(true) {
   currentCoroutineContext().ensureActive()
   val cursor=store.next(sourceId)?:break
   val page=try { api.page(cursor) }catch(e:ShelfException) {
    if(e.reason==ShelfFailure.InvalidCursor && !restarted) {store.restart(sourceId);restarted=true;continue}
    throw e
   }
   store.page(sourceId,cursor,page.items,page.nextToken)
   val count=store.all().firstOrNull { it.id==sourceId }?.entryCount?:0
   operations.update { it+(sourceId to ShelfOperation(ShelfTask.Syncing,processed=count)) }
  }
  store.finish(sourceId,now())
  }
 }
 override fun refreshStale() {
  scope.launch { try { store.all().filter { it.pending || it.updatedAt==0L || now()-it.updatedAt>15*60_000 }.forEach { refresh(it.id) } }catch(e:CancellationException){throw e}catch(_:Exception){} }
 }
 override fun cancel(key:String){scope.launch { guard.withLock { if(operations.value[key]?.task!=ShelfTask.Importing)jobs[key]?.cancel() } }}
 override suspend fun search(query:String,sourceId:String?,offset:Int)=store.search(query,sourceId,offset)
 override fun download(entry:ShelfEntry) {
  if(entry.bookId!=null)return
  val key="${entry.sourceId}:${entry.fileId}"
  enqueue(key,ShelfTask.Downloading) {
   downloadLock.withLock {
    withContext(Dispatchers.IO) {
     cleanup.join()
     files.createDirectories(directory)
     // IDs are validated by the Drive API; no filename from Drive becomes a path.
     require(Regex("[A-Za-z0-9_-]{1,200}").matches(entry.fileId))
     val temporary=directory/"${entry.fileId}.part"
     val target=directory/"${entry.fileId}.epub"
     try {
      var lastUpdate=0L
      files.sink(temporary).buffer().use { sink->api.download(entry,sink) { bytes,total->
       if(now()-lastUpdate>=200 || bytes==total){lastUpdate=now();operations.update { it+(key to ShelfOperation(ShelfTask.Downloading,bytes=bytes,totalBytes=total)) }}
      } }
      files.atomicMove(temporary,target)
      currentCoroutineContext().ensureActive()
      operations.update { it+(key to ShelfOperation(ShelfTask.Importing)) }
      // Finish the existing importer once it starts; cancellation must not leave
      // a book with half its chapters. Process termination remains an OS limit.
      withContext(NonCancellable) {
       val bookId=try { importer(target.toString()) }catch(e:ShelfException){throw e}catch(_:Exception){throw ShelfException(ShelfFailure.ImportFailed)}
       store.imported(entry,bookId)
      }
     } finally { files.delete(temporary,mustExist=false);files.delete(target,mustExist=false) }
    }
   }
  }
 }
}
