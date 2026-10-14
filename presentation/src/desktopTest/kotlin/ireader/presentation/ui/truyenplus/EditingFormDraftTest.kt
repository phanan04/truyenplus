package ireader.presentation.ui.truyenplus

import ireader.domain.models.editing.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class EditingFormDraftTest {
    @Test fun closingWithoutWaitingForDebouncePreservesLatestFormAndDiscardIsExplicit()=runTest {
        val forms=mutableMapOf<Pair<Long,EditingFormKind>,String>()
        val store=object:ChapterEditingStore {
            override suspend fun load(chapterId:Long):EditingSnapshot=error("unused")
            override suspend fun apply(proposal:EditingProposal){}
            override suspend fun restore(chapterId:Long){}
            override suspend fun characters(bookId:Long)=emptyList<EditingCharacter>()
            override suspend fun saveCharacters(bookId:Long,characters:List<EditingCharacter>){}
            override suspend fun loadFormDraft(bookId:Long,kind:EditingFormKind)=forms[bookId to kind]
            override suspend fun saveFormDraft(bookId:Long,kind:EditingFormKind,payload:String){forms[bookId to kind]=payload}
            override suspend fun clearFormDraft(bookId:Long,kind:EditingFormKind){forms.remove(bookId to kind)}
        }
        val current=EditingFormDraftController(store,7,EditingFormKind.Characters)
        current.load();current.update("first");current.update("latest");current.persist()
        val reopened=EditingFormDraftController(store,7,EditingFormKind.Characters)
        assertEquals("latest",reopened.load())
        val otherBook=EditingFormDraftController(store,9,EditingFormKind.Characters)
        assertNull(otherBook.load())
        reopened.update(null);reopened.persist()
        assertNull(EditingFormDraftController(store,7,EditingFormKind.Characters).load())
    }
}
