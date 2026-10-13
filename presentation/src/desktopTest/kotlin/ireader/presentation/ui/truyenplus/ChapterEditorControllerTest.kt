package ireader.presentation.ui.truyenplus
import ireader.domain.models.editing.*
import ireader.core.source.model.Text
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterEditorControllerTest {
    private class Store:ChapterEditingStore {
        var pages=listOf(Text("Gốc."));var saved=false;var calls=0
        override suspend fun load(chapterId:Long)=EditingSnapshot(chapterId,7,pages,saved)
        override suspend fun apply(proposal:EditingProposal){calls++;pages=proposal.pages.filterIsInstance<Text>();saved=true}
        override suspend fun restore(chapterId:Long){pages=listOf(Text("Gốc."));saved=false}
        override suspend fun characters(bookId:Long)=emptyList<EditingCharacter>()
        override suspend fun saveCharacters(bookId:Long,characters:List<EditingCharacter>){}
    }
    private val settings=object:EditingSettings {
        override suspend fun load()=EditingConfig()
        override suspend fun credentials(config:EditingConfig)=EditingCredentials(config,"test")
        override suspend fun save(config:EditingConfig,newKey:String?){}
        override suspend fun clearKey(provider:EditingProvider){}
    }
    @Test fun cancellingAnOldJobCannotMarkTheNewRequestIdle()=runTest {
        val store=Store()
        var started=CompletableDeferred<Unit>()
        var calls=0
        val api=object:ChapterEditingApi {
            override suspend fun edit(credentials:EditingCredentials,mode:EditingMode,paragraphs:List<EditParagraph>,characters:List<EditingCharacter>):List<EditParagraph> {
                val call=++calls
                started.complete(Unit)
                try {awaitCancellation()} finally {if(call==1)withContext(NonCancellable){delay(1000)}}
            }
        }
        val controller=ChapterEditorController(this,store,settings,EditChapter(store,api),StandardTestDispatcher(testScheduler))
        controller.load(8);advanceUntilIdle()
        controller.generate();runCurrent();started.await()
        controller.cancel()
        started=CompletableDeferred()
        controller.generate();runCurrent();started.await()
        advanceTimeBy(1001);runCurrent()
        assertTrue(controller.state.value.busy)
        controller.cancel();advanceUntilIdle();assertEquals(0,store.calls)
    }
}
