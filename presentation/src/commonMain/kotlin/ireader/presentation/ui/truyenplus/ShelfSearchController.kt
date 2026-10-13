package ireader.presentation.ui.truyenplus

import ireader.domain.models.shelf.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class ShelfSearchUiState(val page:ShelfSearchPage=ShelfSearchPage(emptyList(),false),val loading:Boolean=false,val failure:ShelfFailure?=null)

/** Invalidates immediately on input; only the latest request may publish results. */
class ShelfSearchController(private val scope:CoroutineScope,private val search:suspend (String,String?,Int)->ShelfSearchPage) {
    private val mutable=MutableStateFlow(ShelfSearchUiState())
    val state=mutable.asStateFlow()
    private var generation=0L
    private var job:Job?=null
    private var previousQuery:String?=null
    fun invalidate(pending:Boolean) {
        generation++;job?.cancel();mutable.value=ShelfSearchUiState(loading=pending)
    }
    fun submit(query:String,source:String?,offset:Int) {
        val typing=query!=previousQuery;previousQuery=query
        invalidate(query.isNotBlank())
        if(query.isBlank())return
        val current=generation
        job=scope.launch {
            try {
                if(typing)delay(250)
                val page=search(query,source,offset)
                if(current==generation)mutable.value=ShelfSearchUiState(page=page)
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {if(current==generation)mutable.value=ShelfSearchUiState(failure=(e as? ShelfException)?.reason?:ShelfFailure.Network)}
            finally {if(current==generation)mutable.update{it.copy(loading=false)}}
        }
    }
}
