package ireader.data.category

import ir.kazemcodes.infinityreader.Database
import ireader.domain.models.entities.CategoryDeletionSnapshot

/** Restore the collection itself, not a new collection with only the same name. */
class SqlCategoryDeletionStore(private val db:Database) {
    fun remove(id:Long):CategoryDeletionSnapshot=db.transactionWithResult {
        require(id>0) {"System collections cannot be deleted"}
        val category=requireNotNull(db.categoryQueries.getCategory(id,categoryMapper).executeAsOneOrNull())
        val snapshot=CategoryDeletionSnapshot(category,
            db.bookcategoryQueries.findByCategoryId(id).executeAsList().map {it.book_id},
            db.categoryautoruleQueries.findByCategoryId(id,categoryAutoRuleMapper).executeAsList())
        // Explicit cleanup also works with SQLite foreign keys disabled.
        db.bookcategoryQueries.delete(id)
        db.categoryautoruleQueries.deleteByCategoryId(id)
        db.categoryQueries.delete(id)
        snapshot
    }
    fun restore(snapshot:CategoryDeletionSnapshot)=db.transaction {
        val category=snapshot.category
        require(category.id>0 && db.categoryQueries.getCategory(category.id).executeAsOneOrNull()==null) {"Collection identity is already in use"}
        db.categoryQueries.restoreCategory(category.id,category.name,category.order,category.flags)
        snapshot.bookIds.forEach {db.bookcategoryQueries.restoreAssignment(it,category.id)}
        snapshot.rules.forEach {r->db.categoryautoruleQueries.restoreRule(r.id,category.id,r.ruleType.name,r.value,r.isEnabled)}
    }
}
