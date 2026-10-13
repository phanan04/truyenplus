package ireader.presentation.ui.truyenplus

import ireader.domain.models.editing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ChapterEditorState(
    val snapshot: EditingSnapshot? = null,
    val proposal: EditingProposal? = null,
    val mode: EditingMode = EditingMode.Grammar,
    val busy: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val error: String? = null,
)
class ChapterEditorController(private val scope: CoroutineScope, private val store: ChapterEditingStore,
    private val settings: EditingSettings, private val editor: EditChapter, private val workDispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private val mutable=MutableStateFlow(ChapterEditorState(busy=true))
    val state=mutable.asStateFlow()
    private var job: Job?=null
    private var generation=0
    fun load(id: Long) { task {mutable.value=ChapterEditorState(snapshot=store.load(id))} }
    fun mode(mode: EditingMode) {if(!state.value.busy) mutable.update{it.copy(mode=mode,proposal=null,error=null)}}
    fun cancel() {generation++;job?.cancel();mutable.update{it.copy(busy=false,total=0)}}
    fun generate() {
        val current=state.value;val id=current.snapshot?.chapterId ?: return
        if(current.busy)return
        task {
            mutable.update{it.copy(proposal=null,total=0)}
            val config=settings.load()
            val credentials=settings.credentials(config)
            val proposal=withContext(workDispatcher) {editor.propose(id,credentials,current.mode) {done,total->mutable.update{it.copy(progress=done,total=total)}}}
            mutable.update{it.copy(snapshot=proposal.snapshot,proposal=proposal)}
        }
    }
    fun changeParagraph(index: Int,text: String) {
        if(text.isBlank() || text.length>18_000)return
        mutable.update {state->state.copy(proposal=state.proposal?.let {p->p.copy(pages=p.pages.mapIndexed {i,page->if(i==index && page is ireader.core.source.model.Text) ireader.core.source.model.Text(text) else page})})}
    }
    fun apply(beforeChange:()->Unit,onChanged:(Long)->Unit) {
        val proposal=state.value.proposal?:return
        if(state.value.busy)return
        task { beforeChange();withContext(NonCancellable) {
            store.apply(proposal);onChanged(proposal.snapshot.chapterId)
            mutable.update{it.copy(snapshot=store.load(proposal.snapshot.chapterId),proposal=null)}
        } }
    }
    fun restore(beforeChange:()->Unit,onChanged:(Long)->Unit) {
        val id=state.value.snapshot?.chapterId?:return
        if(state.value.busy)return
        task {beforeChange();withContext(NonCancellable) {
            store.restore(id);onChanged(id);mutable.update{it.copy(snapshot=store.load(id),proposal=null)}
        } }
    }
    private fun task(block:suspend ()->Unit) {
        val currentGeneration=++generation
        job?.cancel();mutable.update{it.copy(busy=true,error=null,total=0,progress=0)}
        job=scope.launch {
            try {block()}
            catch(_: TimeoutCancellationException) {if(currentGeneration==generation)mutable.update{it.copy(error="Dịch vụ phản hồi quá chậm. Hãy thử lại.")}}
            catch(e: CancellationException) {throw e}
            catch(e: Exception) {if(currentGeneration==generation)mutable.update{it.copy(error=e.message)}}
            finally {if(currentGeneration==generation)mutable.update{it.copy(busy=false)}}
        }
    }
}
