package ireader.presentation.ui.truyenplus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ireader.data.core.createDatabase
import ireader.data.editing.SqlChapterEditingStore
import ireader.domain.models.editing.*
import ireader.core.source.model.*
import ir.kazemcodes.infinityreader.Database
import kotlinx.coroutines.runBlocking
import kotlin.test.*

class ChapterEditingStoreTest {
    @Test fun applyAndRestorePreserveOriginalAndLiveProgressAndRejectStaleProposal() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        Database.Schema.create(driver)
        try {
            val db = createDatabase(driver)
            driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(7,-1,'book','Book',0,1,1,0,0,0,0)",0)
            val original = listOf(Text("Câu ban đầu."), ImageUrl("image"))
            db.chapterQueries.upsert(8,7,"chapter","Chapter",null,false,true,5,-1f,0,0,0,original,0)
            val store = SqlChapterEditingStore(db)
            val snapshot = store.load(8)
            val proposal = EditingProposal(snapshot,listOf(Text("Câu đã sửa."),original[1]))
            // Progress can change while a network request is in flight.
            db.chapterQueries.updateLastPageRead(6,8)
            store.apply(proposal)
            assertEquals(proposal.pages,store.load(8).pages)
            assertTrue(store.load(8).hasOriginal)
            assertFailsWith<IllegalArgumentException> { store.apply(proposal) }
            store.apply(EditingProposal(store.load(8),listOf(Text("Sửa lần hai."),original[1])))
            // Reopen: original must still be the first version, never the previous edit.
            SqlChapterEditingStore(createDatabase(driver)).restore(8)
            assertEquals(original,store.load(8).pages)
            val chapter = db.chapterQueries.getChapterById(8).executeAsOne()
            assertEquals(6,chapter.last_page_read)
            assertTrue(chapter.bookmark)
            assertFalse(chapter.read)
            assertFalse(store.load(8).hasOriginal)
            val people = listOf(EditingCharacter("Hoàng Yên Trần","Yên Trần","Nữ; gọi Trương Nhược Trần là ngươi."))
            store.saveCharacters(7,people)
            assertEquals(people,SqlChapterEditingStore(createDatabase(driver)).characters(7))
            driver.execute(null,"DELETE FROM chapter WHERE _id=8",0)
            assertFails { store.load(8) }; Unit
        } finally { driver.close() }
    }
    @Test fun draftsSurviveReopenAndTheMigrationKeepsExistingTextAndCharacters()=runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            val db=createDatabase(driver)
            driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES(7,-1,'book','Book',0,1,1,0,0,0,0)",0)
            val original=listOf(Text("Bản gốc giữ nguyên."))
            db.chapterQueries.upsert(8,7,"chapter","Chương",null,false,true,9,1f,0,0,0,original,0)
            val store=SqlChapterEditingStore(db)
            val people=listOf(EditingCharacter("Hoàng Yên Trần"))
            store.saveCharacters(7,people)
            driver.execute(null,"DROP TABLE chapter_edit_draft",0)
            driver.execute(null,"DROP TABLE book_edit_form_draft",0)
            data.DatabaseMigrations.migrate(driver,41)
            val draft=ChapterEditingDraft(editingFingerprint(original),EditingMode.Grammar,listOf(EditParagraph(0,"Đề xuất chưa áp dụng.")),setOf(0),EditParagraph(0,"Đang gõ dở"))
            store.saveDraft(8,draft)
            store.saveFormDraft(7,EditingFormKind.Characters,"{\"name\":\"Đang nhập\"}")
            val reopened=SqlChapterEditingStore(createDatabase(driver))
            assertEquals(draft,reopened.loadDraft(8))
            assertEquals("{\"name\":\"Đang nhập\"}",reopened.loadFormDraft(7,EditingFormKind.Characters))
            assertEquals(original,reopened.load(8).pages)
            assertEquals(people,reopened.characters(7))
            assertTrue(db.chapterQueries.getChapterById(8).executeAsOne().bookmark)
            reopened.clearDraft(8);assertNull(reopened.loadDraft(8))
        } finally {driver.close()}
    }
    @Test fun version39MigrationKeepsChapters() = runBlocking {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            driver.execute(null,"DROP TABLE chapter_edit_original",0)
            driver.execute(null,"DROP TABLE book_edit_characters",0)
            data.DatabaseMigrations.migrate(driver,39)
            assertEquals(emptyList(),SqlChapterEditingStore(createDatabase(driver)).characters(7))
        } finally {driver.close()}
    }
}
