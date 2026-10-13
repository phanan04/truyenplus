package ireader.presentation.ui.truyenplus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ireader.data.core.createDatabase
import ireader.data.shelf.*
import ireader.domain.models.shelf.*
import ir.kazemcodes.infinityreader.Database
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class DriveShelfSearchTest {
    @Test fun fullTitleComesFirstAndWordsDoNotMatchInsideOtherWords()=runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            val store=SqlShelfStore(createDatabase(driver));store.add(DriveFolderLink("root"),"Kho");store.begin("root")
            val names=listOf(
                "a" to "A Vạn Công Thần Đệ.epub",
                "b" to "A Vạn Cổ Thần Đế Ngoại Truyện - Tác Giả.epub",
                "c" to "Vạn Cổ Thần Đế - Phi Thiên Ngư.epub",
                "d" to "Một Truyện Khác - Vạn Cổ Thần Đế.epub",
                "e" to "Vạn_Cổ_Thần_Đế_2 - Phi Thiên Ngư.epub",
                "f" to "Thần Đế Vạn Cổ - Tác Giả.epub")
            store.page("root",store.next("root")!!,names.map{DriveItem(it.first,it.second,"application/epub+zip")},null);store.finish("root",1)
            val page=store.search("vạn cổ thần đế",null,0)
            assertEquals("c",page.entries.first().fileId)
            assertFalse(page.entries.any{it.fileId=="a"})
            assertTrue(page.entries.any{it.fileId=="e"})
            assertTrue(page.entries.any{it.fileId=="f"})
            assertTrue(page.entries.indexOfFirst{it.fileId=="b"}<page.entries.indexOfFirst{it.fileId=="d"})
            assertEquals(page,store.search("VAN CO THAN DE",null,0))
            assertTrue(store.search("%",null,0).entries.isEmpty())
        }finally{driver.close()}
    }
    @Test fun existingCacheIsReindexedLocallyWithoutLosingImportedBookOrSyncCursor()=runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            driver.execute(null,"DROP TABLE shelf_entry",0)
            driver.execute(null,"DROP TABLE shelf_search_state",0)
            driver.execute(null,"""CREATE TABLE shelf_entry(source_id TEXT NOT NULL,file_id TEXT NOT NULL,name TEXT NOT NULL,search_key TEXT NOT NULL,size INTEGER NOT NULL DEFAULT 0,modified_time TEXT NOT NULL,resource_key TEXT,generation INTEGER NOT NULL,book_id INTEGER,PRIMARY KEY(source_id,file_id))""",0)
            driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(7,-1,'saved','Saved book',0,1,1,0,0,0,0)",0)
            val db=createDatabase(driver)
            db.driveShelfQueries.addSource("root","Kho",null)
            db.driveShelfQueries.startScan("root")
            db.driveShelfQueries.queueFolder("root","root",null)
            db.driveShelfQueries.advanceCursor("next",0,"root","root")
            driver.execute(null,"""INSERT INTO shelf_entry VALUES('root','cached','Vạn_Cổ_Thần_Đế - Phi Thiên Ngư.epub','van_co_than_de - phi thien ngu.epub',123,'old','resource',1,7)""",0)
            data.DatabaseMigrations.migrate(driver,40)
            val store=SqlShelfStore(db)
            val entry=store.search("vạn cổ thần đế",null,0).entries.single()
            assertEquals("cached",entry.fileId);assertEquals(7L,entry.bookId)
            assertEquals(123,entry.size);assertEquals("resource",entry.resourceKey)
            assertEquals("next",store.next("root")!!.token)
            assertTrue(store.source("root")!!.pending)
            assertEquals(1L,db.driveShelfQueries.searchIndexVersion().executeAsOne())
            data.DatabaseMigrations.migrate(driver,40) // generated + manual migration stays idempotent
            assertEquals(entry,SqlShelfStore(createDatabase(driver)).search("van co than de",null,0).entries.single())
        }finally{driver.close()}
    }
    @Test fun longQueriesPaginationAndLastWordPrefixKeepAllRequiredTerms()=runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            val store=SqlShelfStore(createDatabase(driver));store.add(DriveFolderLink("root"),"Kho");store.begin("root")
            val phrase="một hai ba bốn năm sáu bảy tám chín mười mười một"
            val files=(1..85).map{DriveItem("f$it","$phrase $it - Tác Giả.epub","application/epub+zip")}
            store.page("root",store.next("root")!!,files,null);store.finish("root",1)
            val first=store.search(phrase,null,0);val second=store.search(phrase,null,40)
            assertEquals(40,first.entries.size);assertTrue(first.hasMore)
            assertEquals(40,second.entries.size);assertTrue(second.hasMore)
            assertTrue(first.entries.map{it.fileId}.intersect(second.entries.map{it.fileId}.toSet()).isEmpty())
            assertEquals(5,store.search(phrase,null,80).entries.size)
            assertTrue(store.search("một hai ba bốn năm sáu bảy tám chín mười thiếu",null,0).entries.isEmpty())
            assertTrue(store.search("mot hai ba bo",null,0).entries.isNotEmpty())
            assertTrue(store.search(phrase,"another-source",0).entries.isEmpty())
        }finally{driver.close()}
    }

}
