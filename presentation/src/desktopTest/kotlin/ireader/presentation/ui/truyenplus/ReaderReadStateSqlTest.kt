package ireader.presentation.ui.truyenplus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ireader.core.source.model.Text
import ireader.data.chapter.ChapterRepositoryImpl
import ireader.data.core.*
import ir.kazemcodes.infinityreader.Database
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ReaderReadStateSqlTest {
    @Test fun partialReadUpdatesKeepActualSqlTextBookmarksAndLatestPosition()=runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        try {
            val db=createDatabase(driver)
            driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(7,-1,'book','Book',0,1,1,0,0,0,0)",0)
            val text=listOf(Text("Văn bản đã biên tập."))
            db.chapterQueries.upsert(8,7,"chapter","Chương",null,true,true,66,1f,0,0,0,text,0)
            val handler=java.lang.reflect.Proxy.newProxyInstance(DatabaseHandler::class.java.classLoader,arrayOf(DatabaseHandler::class.java)) {_,method,args ->
                check(method.name=="await") {method.name}
                @Suppress("UNCHECKED_CAST") val block=args!![1] as suspend Database.()->Any?
                if(args[0] as Boolean)db.transactionWithResult {runBlocking {block(db)}} else runBlocking {block(db)}
            } as DatabaseHandler
            val repo=ChapterRepositoryImpl(handler)
            repo.updateReadState(mapOf(8L to false,999L to false))
            val saved=db.chapterQueries.getChapterById(8).executeAsOne()
            assertFalse(saved.read);assertTrue(saved.bookmark)
            assertEquals(66L,saved.last_page_read);assertEquals(text,saved.content)
            assertEquals("Chương",saved.name)
            assertEquals(1L,db.bookQueries.findBookById(7).executeAsOne().cached_unread_count)
            repo.updateReadState(mapOf(8L to true))
            assertTrue(db.chapterQueries.getChapterById(8).executeAsOne().read)
            assertEquals(1L,db.bookQueries.findBookById(7).executeAsOne().cached_read_count)
        } finally {driver.close()}
    }
}
