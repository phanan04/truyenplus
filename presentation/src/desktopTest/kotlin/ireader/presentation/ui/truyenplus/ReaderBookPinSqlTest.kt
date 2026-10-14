package ireader.presentation.ui.truyenplus

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import ireader.data.core.createDatabase
import ir.kazemcodes.infinityreader.Database
import kotlin.test.*

class ReaderBookPinSqlTest {
    @Test fun pinOrderUsesAnAggregateAndEmptyLibraryStartsAtZero() {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);Database.Schema.create(driver)
        try {
            val db=createDatabase(driver)
            assertEquals(0L,db.bookQueries.getMaxPinnedOrder().executeAsOne())
            driver.execute(null,"INSERT INTO book(_id,source,url,title,status,favorite,initialized,viewer,chapter_flags,cover_last_modified,date_added,is_pinned,pinned_order) VALUES(7,-1,'book','Book',0,1,1,0,0,0,0,1,8)",0)
            assertEquals(8L,db.bookQueries.getMaxPinnedOrder().executeAsOne())
            db.bookQueries.updatePinStatus(false,0,7)
            assertEquals(0L,db.bookQueries.getMaxPinnedOrder().executeAsOne())
        } finally {driver.close()}
    }
}
