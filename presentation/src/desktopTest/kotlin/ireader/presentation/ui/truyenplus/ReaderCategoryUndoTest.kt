package ireader.presentation.ui.truyenplus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ireader.data.core.createDatabase
import ireader.data.category.SqlCategoryDeletionStore
import ir.kazemcodes.infinityreader.Database
import kotlin.test.*

class ReaderCategoryUndoTest {
    @Test fun undoRestoresIdentityAssignmentsAndRulesWithoutRecreatingOnlyTheName() {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            val db=createDatabase(driver)
            for(id in listOf(7,8))driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added) VALUES($id,-1,'book$id','Book',0,1,1,0,0,0,0)",0)
            driver.execute(null,"INSERT INTO categories(_id,name,sort,flags) VALUES(4,'Bộ sưu tập',5,9)",0)
            db.bookcategoryQueries.insert(7,4);db.bookcategoryQueries.insert(8,4)
            db.categoryautoruleQueries.insert(4,"GENRE","Tiên hiệp",false)
            val originalRule=db.categoryautoruleQueries.findByCategoryId(4).executeAsOne()
            val store=SqlCategoryDeletionStore(db)
            val deleted=store.remove(4)
            assertNull(db.categoryQueries.getCategory(4).executeAsOneOrNull())
            assertTrue(db.bookcategoryQueries.findAll().executeAsList().isEmpty())
            driver.execute(null,"DELETE FROM book WHERE _id=8",0)
            store.restore(deleted)
            val restored=db.categoryQueries.getCategory(4).executeAsOne()
            assertEquals("Bộ sưu tập",restored.name);assertEquals(5L,restored.sort);assertEquals(9L,restored.flags)
            assertEquals(listOf(7L),db.bookcategoryQueries.findAll().executeAsList().map {it.book_id})
            assertEquals(originalRule,db.categoryautoruleQueries.findByCategoryId(4).executeAsOne())
            assertFailsWith<IllegalArgumentException> {store.restore(deleted)}
            assertFailsWith<IllegalArgumentException> {store.remove(0)}
        }finally {driver.close()}
    }
}
