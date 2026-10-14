package ireader.presentation.ui.truyenplus

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

enum class ReaderImportFormat { Epub, Pdf }
enum class ReaderImportPhase { Queued, Importing, Added, Failed }
@Immutable data class ReaderImportFile(val id:Int,val uri:String,val format:ReaderImportFormat,
    val phase:ReaderImportPhase=ReaderImportPhase.Queued,val name:String?=null)
@Immutable data class ReaderImportState(val files:List<ReaderImportFile> = emptyList()) {
    val busy get()=files.any {it.phase==ReaderImportPhase.Queued || it.phase==ReaderImportPhase.Importing}
    val added get()=files.count {it.phase==ReaderImportPhase.Added}
    val failed get()=files.count {it.phase==ReaderImportPhase.Failed}
}
/** Keeps errors isolated to their file and never cancels work when another picker returns. */
class ReaderImportQueue(private val scope:CoroutineScope,
    private val parse:suspend (ReaderImportFormat,String)->Unit) {
    private val mutable=MutableStateFlow(ReaderImportState())
    val state=mutable.asStateFlow()
    private var nextId=0
    private var worker:Job?=null
    private val nameResolvers=mutableMapOf<Int,suspend (String)->String?>()
    fun enqueue(format:ReaderImportFormat,uris:List<String>,nameResolver:(suspend (String)->String?)?=null) {
        if(uris.isEmpty())return
        if(!state.value.busy)nameResolvers.clear()
        val files=uris.distinct().map {ReaderImportFile(++nextId,it,format).also {file->if(nameResolver!=null)nameResolvers[file.id]=nameResolver}}
        mutable.update {old->ReaderImportState((if(old.busy)old.files else emptyList())+files)}
        drain()
    }
    fun retryFailed() {
        if(state.value.busy)return
        mutable.update {old->old.copy(files=old.files.map {if(it.phase==ReaderImportPhase.Failed)it.copy(phase=ReaderImportPhase.Queued) else it})}
        drain()
    }
    fun clearResults() {if(!state.value.busy){mutable.value=ReaderImportState();nameResolvers.clear()}}
    private fun update(id:Int,phase:ReaderImportPhase) {
        mutable.update {old->old.copy(files=old.files.map {if(it.id==id)it.copy(phase=phase) else it})}
    }
    private fun drain() {
        if(worker?.isActive==true)return
        worker=scope.launch {
            try {
                while(true) {
                    val file=state.value.files.firstOrNull {it.phase==ReaderImportPhase.Queued}?:break
                    update(file.id,ReaderImportPhase.Importing)
                    try {
                        val name=try {nameResolvers[file.id]?.invoke(file.uri)}catch(e:CancellationException){throw e}catch(_:Exception){null}
                        if(!name.isNullOrBlank())mutable.update {old->old.copy(files=old.files.map {if(it.id==file.id)it.copy(name=name.take(240)) else it})}
                        parse(file.format,file.uri);update(file.id,ReaderImportPhase.Added);nameResolvers.remove(file.id)
                    }
                    catch(e:CancellationException){update(file.id,ReaderImportPhase.Queued);throw e}
                    catch(_:Exception){update(file.id,ReaderImportPhase.Failed)}
                }
            } finally {worker=null}
        }
    }
}
