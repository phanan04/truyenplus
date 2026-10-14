package ireader.presentation.ui.truyenplus

import ireader.domain.models.editing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import ireader.core.source.model.Text

data class ChapterEditorState(
    val snapshot: EditingSnapshot? = null,
    val proposal: EditingProposal? = null,
    val mode: EditingMode = EditingMode.Grammar,
    val busy: Boolean = false,
    val progress: Int = 0,
    val total: Int = 0,
    val error: String? = null,
    val selected: Set<Int>? = null,
    val manual: EditParagraph? = null,
    val draftRestored: Boolean = false,
)
class ChapterEditorController(private val scope: CoroutineScope, private val store: ChapterEditingStore,
    private val settings: EditingSettings, private val editor: EditChapter, private val workDispatcher: CoroutineDispatcher = Dispatchers.Default) {
    private val mutable=MutableStateFlow(ChapterEditorState(busy=true))
    val state=mutable.asStateFlow()
    private var job: Job?=null
    private var generation=0
    private val writes=Mutex()
    fun load(id: Long) { task {
        val snapshot=store.load(id)
        val draft=store.loadDraft(id)
        val restored=draft?.takeIf {it.fingerprint==editingFingerprint(snapshot.pages)}?.let {
            runCatching { EditingProposal(snapshot,EditingParagraphs.apply(snapshot.pages,it.paragraphs)) }.getOrNull()
        }
        if(draft!=null && restored==null) store.clearDraft(id)
        val changed=restored?.pages?.indices?.filter {restored.pages[it]!=snapshot.pages[it]}?.toSet().orEmpty()
        mutable.value=ChapterEditorState(snapshot=snapshot,proposal=restored,mode=draft?.takeIf {restored!=null}?.mode ?: EditingMode.Grammar,
            selected=draft?.selected?.intersect(changed),manual=draft?.manual?.takeIf {restored!=null && it.id in changed && it.text.length<=18_000},draftRestored=restored!=null)
    } }
    fun reportError(message:String) {mutable.update {it.copy(error=message)}}
    fun mode(mode: EditingMode) {if(!state.value.busy) mutable.update{it.copy(mode=mode,error=null)}}
    fun cancel() {generation++;job?.cancel();mutable.update{it.copy(busy=false,total=0)}}
    fun generate() {
        val current=state.value;val id=current.snapshot?.chapterId ?: return
        if(current.busy)return
        task {
            mutable.update{it.copy(total=0)}
            val config=settings.load()
            val credentials=settings.credentials(config)
            val proposal=withContext(workDispatcher) {editor.propose(id,credentials,current.mode) {done,total->mutable.update{it.copy(progress=done,total=total)}}}
            mutable.update{it.copy(snapshot=proposal.snapshot,proposal=proposal,selected=proposal.pages.indices.filter {proposal.pages[it]!=proposal.snapshot.pages[it]}.toSet(),manual=null,draftRestored=false)}
        }
    }
    fun changeParagraph(index: Int,text: String) {
        if(text.isBlank() || text.length>18_000)return
        if(state.value.busy)return
        mutable.update {state->state.copy(proposal=state.proposal?.let {p->p.copy(pages=p.pages.mapIndexed {i,page->if(i==index && page is Text) Text(text) else page})},selected=state.selected?.plus(index),manual=null)}
    }
    fun select(index:Int,selected:Boolean) {
        if(state.value.busy)return
        mutable.update {s->val current=s.selected ?: s.proposal?.pages?.indices?.filter {s.proposal.pages[it]!=s.proposal.snapshot.pages[it]}?.toSet().orEmpty()
            s.copy(selected=if(selected)current+index else current-index)}
    }
    fun beginManual(index:Int) {
        if(state.value.busy)return
        val text=state.value.proposal?.pages?.getOrNull(index) as? Text ?: return
        mutable.update {it.copy(manual=EditParagraph(index,text.text))}
    }
    fun editManual(text:String) {if(!state.value.busy && text.length<=18_000)mutable.update {it.copy(manual=it.manual?.copy(text=text))}}
    fun saveManual() {state.value.manual?.let {changeParagraph(it.id,it.text)}}
    fun cancelManual() {mutable.update {it.copy(manual=null)}}
    suspend fun persistDraft()=writes.withLock {
        val current=state.value
        if(current.busy)return@withLock
        val snapshot=current.snapshot ?: return@withLock
        val proposal=current.proposal
        if(proposal==null){store.clearDraft(snapshot.chapterId);return@withLock}
        val paragraphs=proposal.pages.mapIndexedNotNull {i,p->(p as? Text)?.takeIf {it.text.isNotBlank()}?.let {EditParagraph(i,it.text)}}
        store.saveDraft(snapshot.chapterId,ChapterEditingDraft(editingFingerprint(snapshot.pages),current.mode,paragraphs,current.selected.orEmpty(),current.manual))
    }
    suspend fun discardDraft()=writes.withLock {
        state.value.snapshot?.chapterId?.let {store.clearDraft(it)}
        mutable.update {it.copy(proposal=null,selected=null,manual=null,draftRestored=false)}
    }
    fun apply(beforeChange:()->Unit,onChanged:(Long)->Unit) {
        val current=state.value
        val reviewed=current.proposal?:return
        if(current.busy)return
        val selected=current.selected ?: reviewed.pages.indices.filter {reviewed.pages[it]!=reviewed.snapshot.pages[it]}.toSet()
        if(selected.isEmpty())return
        val proposal=reviewed.copy(pages=reviewed.pages.mapIndexed {i,p->if(i in selected)p else reviewed.snapshot.pages[i]})
        task { beforeChange();withContext(NonCancellable) {
            writes.withLock {store.apply(proposal);store.clearDraft(proposal.snapshot.chapterId)}
            onChanged(proposal.snapshot.chapterId)
            mutable.update{it.copy(snapshot=store.load(proposal.snapshot.chapterId),proposal=null,selected=null,manual=null,draftRestored=false)}
        } }
    }
    fun restore(beforeChange:()->Unit,onChanged:(Long)->Unit) {
        val id=state.value.snapshot?.chapterId?:return
        if(state.value.busy)return
        task {beforeChange();withContext(NonCancellable) {
            writes.withLock {store.restore(id);store.clearDraft(id)};onChanged(id);mutable.update{it.copy(snapshot=store.load(id),proposal=null,selected=null,manual=null,draftRestored=false)}
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
