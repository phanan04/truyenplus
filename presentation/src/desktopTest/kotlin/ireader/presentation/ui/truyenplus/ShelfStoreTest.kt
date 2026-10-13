package ireader.presentation.ui.truyenplus
import ireader.data.shelf.SqlShelfStore
import ireader.data.shelf.ShelfCursor
import ireader.data.shelf.DriveItem
import ireader.domain.models.shelf.*
import ireader.data.core.createDatabase
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ir.kazemcodes.infinityreader.Database
import kotlinx.coroutines.runBlocking
import kotlin.test.*
class ShelfStoreTest {
 @Test fun scanResumesAndRemovesStaleOnlyAfterSuccessfulCompletion()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  try {
   val store=SqlShelfStore(createDatabase(driver));store.add(DriveFolderLink("root"),"Kho")
   store.begin("root");val cursor=store.next("root")!!
   store.page("root",cursor,listOf(DriveItem("a","Vạn Cổ Thần Đế.epub","application/epub+zip")),"next")
   assertEquals(1,store.search("van co",null,0).entries.size)
   assertEquals("next",store.next("root")!!.token)
   // New repository instance observes the persisted cursor; no duplicate insert.
   val reopened=SqlShelfStore(createDatabase(driver));val next=reopened.next("root")!!
   reopened.page("root",next,listOf(DriveItem("a","Vạn Cổ Thần Đế.epub","application/epub+zip"),DriveItem("b","B.epub","application/epub+zip")),null)
   reopened.finish("root",100);assertEquals(2,reopened.search("epub",null,0).entries.size)
   reopened.begin("root");assertEquals(2,reopened.search("epub",null,0).entries.size)
   reopened.page("root",reopened.next("root")!!,listOf(DriveItem("b","B.epub","application/epub+zip")),null)
   reopened.finish("root",200);assertEquals(listOf("b"),reopened.search("epub",null,0).entries.map{it.fileId})
  }finally{driver.close()}
 }
 @Test fun searchIsBoundedLiteralAndSourceRemovalPreservesLibrary()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  try {
   val db=createDatabase(driver);val store=SqlShelfStore(db);store.add(DriveFolderLink("root"),"Kho");store.begin("root")
   store.page("root",store.next("root")!!,(0 until 18000).map{DriveItem("f$it","Truyện $it.epub","application/epub+zip")},null);store.finish("root",1)
   val first=store.search("truyen",null,0);assertEquals(40,first.entries.size);assertTrue(first.hasMore)
   assertEquals(40,store.search("truyen",null,40).entries.size)
   assertTrue(store.search("%",null,0).entries.isEmpty())
   store.remove("root");assertTrue(store.search("truyen",null,0).entries.isEmpty())
  }finally{driver.close()}
 }

 @Test fun upgradingAnExistingDatabaseAddsShelfWithoutTouchingBooks()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  try {
   driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(7,-1,'saved','Saved book',0,1,1,0,0,0,0)",0)
   for(table in listOf("shelf_source","shelf_entry","shelf_cursor"))driver.execute(null,"DROP TABLE $table",0)
   data.DatabaseMigrations.migrate(driver,38)
   val db=createDatabase(driver);val store=SqlShelfStore(db)
   store.add(DriveFolderLink("root"),"Kho")
   assertEquals("Kho",store.all().single().name)
   assertEquals(7L,db.driveShelfQueries.importedBookForMetadata(-1,"Saved book","").executeAsOne())
  }finally{driver.close()}
 }

 @Test fun restartingExpiredScanRetainsOldCatalogUntilComplete()=runBlocking {
  val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
  try {
   val store=SqlShelfStore(createDatabase(driver));store.add(DriveFolderLink("root"),"Kho");store.begin("root")
   store.page("root",store.next("root")!!,listOf(DriveItem("a","A.epub","application/epub+zip")),"expired")
   store.restart("root");assertNull(store.next("root")!!.token);assertEquals(1,store.search("A",null,0).entries.size)
   store.page("root",store.next("root")!!,emptyList(),null);store.finish("root",100)
   assertTrue(store.search("A",null,0).entries.isEmpty())
  }finally{driver.close()}
 }
}
