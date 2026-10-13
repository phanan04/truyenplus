package ireader.presentation.ui.truyenplus
import ireader.data.shelf.*
import ireader.data.core.createDatabase
import ireader.domain.models.shelf.*
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ir.kazemcodes.infinityreader.Database
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import okio.FileSystem
import okio.BufferedSink
import okio.Path.Companion.toPath
import kotlin.test.*
class DriveShelfRepositoryTest {
 @Test fun recursiveScanPersistsInterruptionAndDoesNotDownloadAnything()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  val root=java.nio.file.Files.createTempDirectory("shelf-test").toFile()
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
  var fail=true;var downloadCalls=0
  val api=object:ShelfDriveApi {
   override suspend fun folder(link:DriveFolderLink)=DriveItem(link.folderId,"Kho thật","application/vnd.google-apps.folder")
   override suspend fun page(cursor:ShelfCursor):DrivePage {
    return when(cursor.folderId) {
     "root"->DrivePage(listOf(DriveItem("sub","Tiên hiệp","application/vnd.google-apps.folder"),DriveItem("a","A.epub","application/epub+zip")),null)
     else->{if(fail)throw ShelfException(ShelfFailure.Network);DrivePage(listOf(DriveItem("b","B.epub","application/epub+zip")),null)}
    }
   }
   override suspend fun download(entry:ShelfEntry,sink:BufferedSink,onProgress:(Long,Long?)->Unit){downloadCalls++}
  }
  try {
   val repo=DefaultDriveShelfRepository(SqlShelfStore(createDatabase(driver)),api,FileSystem.SYSTEM,root.path.toPath(),scope,{42},{123})
   repo.add("https://drive.google.com/drive/folders/root","")
   withTimeout(5000){while(repo.operations.value["root"]?.task!=ShelfTask.Failed)delay(10)}
   assertEquals(1,repo.search("epub").entries.size)
   fail=false;repo.refresh("root")
   withTimeout(5000){repo.sources.first { it.firstOrNull()?.updatedAt==123L }}
   assertEquals(2,repo.search("epub").entries.size);assertEquals(0,downloadCalls)
   repo.remove("root");assertTrue(repo.search("epub").entries.isEmpty())
  } finally{scope.cancel();driver.close();root.deleteRecursively()}
 }

 @Test fun failedDownloadsLeaveNoTemporaryFilesAndSuccessfulImportsAreLinked()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  val root=java.nio.file.Files.createTempDirectory("shelf-download-test").toFile()
  val scope=CoroutineScope(SupervisorJob()+Dispatchers.Default)
  var fail=true;var imports=0
  val api=object:ShelfDriveApi {
   override suspend fun folder(link:DriveFolderLink)=DriveItem("root","Kho","application/vnd.google-apps.folder")
   override suspend fun page(cursor:ShelfCursor)=DrivePage(listOf(DriveItem("a","A.epub","application/epub+zip")),null)
   override suspend fun download(entry:ShelfEntry,sink:BufferedSink,onProgress:(Long,Long?)->Unit) {
    sink.writeUtf8("downloaded bytes");onProgress(16,16)
    if(fail)throw ShelfException(ShelfFailure.Network)
   }
  }
  try {
   val store=SqlShelfStore(createDatabase(driver))
   val repo=DefaultDriveShelfRepository(store,api,FileSystem.SYSTEM,root.path.toPath(),scope,{ path ->
    assertTrue(java.io.File(path).exists());imports++
    driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(42,-1,'test','A',0,1,1,0,0,0,0)",0)
    42L
   },{System.currentTimeMillis()})
   repo.add("https://drive.google.com/drive/folders/root","")
   withTimeout(5000){repo.sources.first { it.firstOrNull()?.updatedAt?.let { t->t>0 }==true }}
   val entry=repo.search("A.epub").entries.single()
   repo.download(entry)
   withTimeout(5000){while(repo.operations.value["root:a"]?.task!=ShelfTask.Failed)delay(10)}
   assertEquals(0,imports);assertTrue(root.listFiles().orEmpty().isEmpty());assertNull(repo.search("A.epub").entries.single().bookId)
   fail=false;repo.download(entry)
   withTimeout(5000){while(repo.search("A.epub").entries.single().bookId==null)delay(10)}
   assertEquals(1,imports);assertEquals(42L,repo.search("A.epub").entries.single().bookId)
   withTimeout(5000){while(repo.operations.value["root:a"]?.task!=null)delay(10)}
   assertTrue(root.listFiles().orEmpty().isEmpty())
   repo.remove("root");assertEquals(42L,store.findImportedBook(-1,"A",""))
  }finally {scope.cancel();driver.close();root.deleteRecursively()}
 }
}
