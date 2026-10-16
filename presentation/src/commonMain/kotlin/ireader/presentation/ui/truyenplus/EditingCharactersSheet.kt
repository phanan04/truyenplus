package ireader.presentation.ui.truyenplus

import ireader.presentation.ui.truyenplus.ReaderGlassButton as Button
import ireader.presentation.ui.truyenplus.ReaderGlassTextButton as TextButton
import ireader.presentation.ui.truyenplus.ReaderGlassOutlinedTextField as OutlinedTextField

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ireader.domain.models.editing.*
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.*
import kotlinx.serialization.json.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditingCharactersSheet(bookId:Long,store:ChapterEditingStore,onDismiss:()->Unit) {
    val scope=rememberCoroutineScope()
    var people by remember(bookId){mutableStateOf<List<EditingCharacter>>(emptyList())}
    var busy by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    var index by remember{mutableStateOf<Int?>(null)}
    var person by remember{mutableStateOf(EditingCharacter(""))}
    var draftLoaded by remember(bookId){mutableStateOf(false)}
    var discard by remember {mutableStateOf(false)}
    var undo by remember {mutableStateOf<List<EditingCharacter>?>(null)}
    val draft=remember(bookId,store){EditingFormDraftController(store,bookId,EditingFormKind.Characters)}
    fun currentPayload():String?=index?.let {position->buildJsonObject {
        put("original",people.getOrNull(position)?.name.orEmpty());put("name",person.name);put("aliases",person.aliases);put("notes",person.notes)
    }.toString()}
    val close:()->Unit={if(!busy){draft.update(currentPayload());scope.launch {try {withContext(NonCancellable){draft.persist()};onDismiss()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}}}}
    LaunchedEffect(bookId) {try{people=store.characters(bookId)
        draft.load()?.let {payload->runCatching {
            val obj=Json.parseToJsonElement(payload).jsonObject
            val original=obj["original"]?.jsonPrimitive?.content.orEmpty()
            index=if(original.isEmpty())-1 else people.indexOfFirst {it.name==original}
            person=EditingCharacter(obj["name"]!!.jsonPrimitive.content,obj["aliases"]!!.jsonPrimitive.content,obj["notes"]!!.jsonPrimitive.content)
        }};draftLoaded=true}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}finally{busy=false}}
    LaunchedEffect(index,person,busy,draftLoaded) {
        if(draftLoaded && !busy){
            draft.update(currentPayload())
            delay(400);try {draft.persist()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}
        }
    }
    fun save(updated:List<EditingCharacter>,rememberUndo:Boolean=false) {
        busy=true;error=null
        scope.launch {try{withContext(NonCancellable){store.saveCharacters(bookId,updated)};undo=if(rememberUndo)people else null;people=updated;index=null;draft.update(null);draft.persist()}
            catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}finally{busy=false}}
    }
    ReaderGlassSheet(onDismissRequest=close) {
        LazyColumn(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item{Text(localize(Res.string.tp_edit_character_memory),style=MaterialTheme.typography.headlineSmall)}
            item{Text(localize(Res.string.tp_edit_memory_hint),style=MaterialTheme.typography.bodySmall)}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
            if(busy)item{LinearProgressIndicator(Modifier.fillMaxWidth())}
            if(index!=null) {
                item{OutlinedTextField(person.name,{person=person.copy(name=it.take(200))},label={Text(localize(Res.string.tp_edit_name))},enabled=!busy,modifier=Modifier.fillMaxWidth())}
                item{OutlinedTextField(person.aliases,{person=person.copy(aliases=it.take(500))},label={Text(localize(Res.string.tp_edit_aliases))},enabled=!busy,modifier=Modifier.fillMaxWidth())}
                item{OutlinedTextField(person.notes,{person=person.copy(notes=it.take(1000))},label={Text(localize(Res.string.tp_edit_notes))},enabled=!busy,modifier=Modifier.fillMaxWidth(),minLines=3)}
                item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                    Button(onClick={val position=index!!;save(if(position<0)people+person.copy(name=person.name.trim()) else people.mapIndexed{i,p->if(i==position)person.copy(name=person.name.trim()) else p})},enabled=!busy && person.name.isNotBlank()){Text(localize(Res.string.tp_edit_save))}
                    TextButton(onClick={discard=true},enabled=!busy){Text(localize(Res.string.tp_edit_cancel))}
                }}
            } else {
                item{Button(onClick={index=-1;person=EditingCharacter("")},enabled=!busy && people.size<200){Text(localize(Res.string.tp_edit_add_character))}}
                if(people.isEmpty() && !busy)item{Text(localize(Res.string.tp_edit_no_characters))}
                itemsIndexed(people){i,p->
                    Column(Modifier.fillMaxWidth().readerAppGlassCard().padding(16.dp)) {
                        Text(p.name,style=MaterialTheme.typography.titleMedium)
                        val detail=listOf(p.aliases,p.notes).filter{it.isNotBlank()}.joinToString(" · ")
                        if(detail.isNotEmpty())Text(detail,style=MaterialTheme.typography.bodyMedium)
                        FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick={index=i;person=p},enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_edit))}
                            TextButton(onClick={save(people.filterIndexed{position,_->position!=i},rememberUndo=true)},enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_delete))}
                        }
                    }
                }
            }
            if(undo!=null)item{TextButton(onClick={val previous=undo!!;undo=null;save(previous)},enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.undo))}}
            item{TextButton(close,enabled=!busy){Text(localize(Res.string.tp_edit_close))}}
        }
    }
    if(discard)ReaderGlassDialog(onDismissRequest={discard=false},title={Text(localize(Res.string.tp_discard_draft))},text={Text(localize(Res.string.tp_discard_draft_hint))},
        confirmButton={TextButton(onClick={discard=false;index=null}){Text(localize(Res.string.tp_discard))}},dismissButton={TextButton(onClick={discard=false}){Text(localize(Res.string.tp_keep_editing))}})
}
