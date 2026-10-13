package ireader.presentation.ui.truyenplus

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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditingCharactersSheet(bookId:Long,store:ChapterEditingStore,onDismiss:()->Unit) {
    val scope=rememberCoroutineScope()
    var people by remember(bookId){mutableStateOf<List<EditingCharacter>>(emptyList())}
    var busy by remember{mutableStateOf(true)}
    var error by remember{mutableStateOf<String?>(null)}
    var index by remember{mutableStateOf<Int?>(null)}
    var person by remember{mutableStateOf(EditingCharacter(""))}
    LaunchedEffect(bookId) {try{people=store.characters(bookId)}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}finally{busy=false}}
    fun save(updated:List<EditingCharacter>) {
        busy=true;error=null
        scope.launch {try{store.saveCharacters(bookId,updated);people=updated;index=null}
            catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}finally{busy=false}}
    }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
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
                    TextButton(onClick={index=null},enabled=!busy){Text(localize(Res.string.tp_edit_cancel))}
                }}
            } else {
                item{Button(onClick={index=-1;person=EditingCharacter("")},enabled=!busy && people.size<200){Text(localize(Res.string.tp_edit_add_character))}}
                if(people.isEmpty() && !busy)item{Text(localize(Res.string.tp_edit_no_characters))}
                itemsIndexed(people){i,p->
                    ListItem(headlineContent={Text(p.name)},supportingContent={Text(listOf(p.aliases,p.notes).filter{it.isNotBlank()}.joinToString(" · "))},
                        trailingContent={Row{
                            TextButton(onClick={index=i;person=p},enabled=!busy){Text(localize(Res.string.tp_edit_edit))}
                            TextButton(onClick={save(people.filterIndexed{position,_->position!=i})},enabled=!busy){Text(localize(Res.string.tp_edit_delete))}
                        }})
                }
            }
            item{TextButton(onDismiss,enabled=!busy){Text(localize(Res.string.tp_edit_close))}}
        }
    }
}
