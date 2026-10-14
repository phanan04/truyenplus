package ireader.presentation.ui.truyenplus

import ireader.domain.models.editing.ChapterEditingStore
import ireader.domain.models.editing.EditingFormKind
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Debounce belongs to the UI; explicit close flushes the most recent value. */
class EditingFormDraftController(private val store:ChapterEditingStore,private val bookId:Long,private val kind:EditingFormKind) {
    private var payload:String?=null
    private val writes=Mutex()
    suspend fun load():String?=writes.withLock {store.loadFormDraft(bookId,kind).also {payload=it}}
    fun update(value:String?) {payload=value}
    suspend fun persist()=writes.withLock {
        val current=payload
        if(current==null)store.clearFormDraft(bookId,kind) else store.saveFormDraft(bookId,kind,current)
    }
}
