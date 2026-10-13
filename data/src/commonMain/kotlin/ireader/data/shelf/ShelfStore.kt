package ireader.data.shelf

import ir.kazemcodes.infinityreader.Database
import ireader.domain.models.shelf.*
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

data class DriveItem(val id: String, val name: String, val mimeType: String, val size: Long=0, val modifiedTime: String="", val resourceKey: String?=null) {
    val isFolder get()=mimeType=="application/vnd.google-apps.folder"
    val isEpub get()=mimeType=="application/epub+zip" || name.endsWith(".epub",true)
}
data class ShelfCursor(val folderId: String, val resourceKey: String?, val token: String?)

class SqlShelfStore(private val db: Database) {
    private val q get()=db.driveShelfQueries
    val sources: Flow<List<ShelfSource>> get()=q.sources { id,name,key,updated,_,pending,count -> ShelfSource(id,name,key,updated,count,pending!=0L) }.asFlow().mapToList(Dispatchers.IO)
    suspend fun add(link: DriveFolderLink,name:String): ShelfSource=withContext(Dispatchers.IO) {
        q.addSource(link.folderId,name.trim().take(100).ifBlank{"Google Drive"},link.resourceKey)
        source(link.folderId)!!
    }
    suspend fun source(id:String): ShelfSource?=withContext(Dispatchers.IO) { q.source(id).executeAsOneOrNull()?.let { ShelfSource(it.id,it.name,it.resource_key,it.updated_at,pending=it.pending!=0L) } }
    suspend fun all():List<ShelfSource> = withContext(Dispatchers.IO) { q.sources { id,name,key,updated,_,pending,count -> ShelfSource(id,name,key,updated,count,pending!=0L) }.executeAsList() }
    suspend fun rename(id:String,name:String)=withContext(Dispatchers.IO) { require(name.isNotBlank());q.renameSource(name.trim().take(100),id);Unit }
    suspend fun remove(id:String)=withContext(Dispatchers.IO) { db.transaction { q.removeEntries(id);q.clearCursors(id);q.removeSource(id) } }
    suspend fun begin(id:String)=withContext(Dispatchers.IO) {
        val s=q.source(id).executeAsOneOrNull()?:return@withContext
        if(s.pending==0L) db.transaction { q.clearCursors(id);q.startScan(id);q.queueFolder(id,id,s.resource_key) }
    }
    suspend fun restart(id:String) { withContext(Dispatchers.IO) { db.transaction { q.clearCursors(id);q.resetScan(id) } };begin(id) }
    suspend fun next(id:String):ShelfCursor?=withContext(Dispatchers.IO) { q.nextCursor(id).executeAsOneOrNull()?.let { ShelfCursor(it.folder_id,it.resource_key,it.token) } }
    suspend fun page(id:String,cursor:ShelfCursor,items:List<DriveItem>,token:String?)=withContext(Dispatchers.IO) {
        val generation=q.source(id).executeAsOneOrNull()?.generation?:return@withContext
        require(token==null || token!=cursor.token) { "Repeated Drive cursor" }
        db.transaction {
            items.forEach { item ->
                if(item.isFolder) q.queueFolder(id,item.id,item.resourceKey)
                else if(item.isEpub) q.upsertEntry(id,item.id,item.name,shelfSearchKey(item.name),item.size,item.modifiedTime,item.resourceKey,generation)
            }
            q.advanceCursor(token,if(token==null)1L else 0L,id,cursor.folderId)
        }
    }
    suspend fun finish(id:String,now:Long)=withContext(Dispatchers.IO) {
        check(q.nextCursor(id).executeAsOneOrNull()==null)
        val s=q.source(id).executeAsOneOrNull()?:return@withContext
        db.transaction { q.pruneEntries(id,s.generation);q.finishScan(now,id);q.clearCursors(id) }
    }
    suspend fun search(query:String,source:String?,offset:Int):ShelfSearchPage=withContext(Dispatchers.IO) {
        val key=shelfSearchKey(query).take(200)
        if(key.isBlank()) return@withContext ShelfSearchPage(emptyList(),false)
        val pattern="%"+key.replace("\\","\\\\").replace("%","\\%").replace("_","\\_").replace(" ","%")+"%"
        val rows=q.search(source,pattern,41,offset.coerceIn(0,1000000).toLong()) { sourceId,fileId,name,_,size,modified,resource,_,book -> ShelfEntry(sourceId,fileId,name,size,modified,resource,book) }.executeAsList()
        ShelfSearchPage(rows.take(40),rows.size>40)
    }
    suspend fun chapterCount(bookId:Long):Long=withContext(Dispatchers.IO) { q.importedChapterCount(bookId).executeAsOne() }
    suspend fun findImportedBook(sourceId:Long,title:String,author:String):Long?=withContext(Dispatchers.IO) { q.importedBookForMetadata(sourceId,title,author).executeAsOneOrNull() }
    suspend fun imported(entry:ShelfEntry,bookId:Long)=withContext(Dispatchers.IO) { q.markImported(bookId,entry.sourceId,entry.fileId) }
}
