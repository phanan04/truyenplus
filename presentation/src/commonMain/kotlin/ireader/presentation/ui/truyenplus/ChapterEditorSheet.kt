package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ireader.core.source.model.Text as PageText
import ireader.domain.models.editing.*
import ireader.i18n.localize
import ireader.i18n.resources.*
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun ChapterEditorSheet(chapterId: Long,onDismiss:()->Unit,beforeChange:()->Unit,onChanged:(Long)->Unit) {
    val store: ChapterEditingStore=koinInject()
    val settings: EditingSettings=koinInject()
    val editor: EditChapter=koinInject()
    val scope=rememberCoroutineScope()
    val controller=remember(chapterId){ChapterEditorController(scope,store,settings,editor)}
    val state by controller.state.collectAsState()
    var settingsOpen by remember{mutableStateOf(false)}
    var memoryOpen by remember{mutableStateOf(false)}
    var rulesOpen by remember{mutableStateOf(false)}
    var restoreOpen by remember{mutableStateOf(false)}
    LaunchedEffect(controller){controller.load(chapterId)}
    DisposableEffect(controller){onDispose{controller.cancel()}}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        ChapterEditorContent(state,controller::generate,controller::cancel,
            {controller.apply(beforeChange,onChanged)},{restoreOpen=true},{memoryOpen=true},{settingsOpen=true},{rulesOpen=true},controller::mode,controller::changeParagraph)
    }
    if(settingsOpen) EditingSettingsSheet {settingsOpen=false}
    val bookId=state.snapshot?.bookId
    if(memoryOpen && bookId!=null) EditingCharactersSheet(bookId,store) {memoryOpen=false}
    if(rulesOpen && bookId!=null) EditingRulesSheet(bookId,beforeChange={beforeChange()},onChanged={onChanged(chapterId)}) {rulesOpen=false}
    if(restoreOpen) AlertDialog(onDismissRequest={restoreOpen=false},title={Text(localize(Res.string.tp_edit_restore))},text={Text(localize(Res.string.tp_edit_restore_question))},
        confirmButton={TextButton(onClick={restoreOpen=false;controller.restore(beforeChange,onChanged)}){Text(localize(Res.string.tp_edit_restore))}},dismissButton={TextButton(onClick={restoreOpen=false}){Text(localize(Res.string.tp_edit_cancel))}})
}

@Composable fun ChapterEditorContent(state: ChapterEditorState,onGenerate:()->Unit,onCancel:()->Unit,onApply:()->Unit,onRestore:()->Unit,
    onMemory:()->Unit,onSettings:()->Unit,onRules:()->Unit,onMode:(EditingMode)->Unit,onEditParagraph:(Int,String)->Unit={_,_->}) {
    var editingIndex by remember{mutableStateOf<Int?>(null)}
    var editingText by remember{mutableStateOf("")}
    val proposal=state.proposal
    val changed=remember(proposal){proposal?.pages?.indices?.filter{proposal.pages[it] is PageText && proposal.pages[it]!=proposal.snapshot.pages[it]}?:emptyList()}
    LazyColumn(Modifier.fillMaxWidth().heightIn(max=900.dp).imePadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item { Text(localize(Res.string.tp_edit_chapter),style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.SemiBold) }
        item { Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            TextButton(onMemory,enabled=!state.busy && state.snapshot!=null,modifier=Modifier.testTag("editor-memory")){Text(localize(Res.string.tp_edit_memory))}
            TextButton(onSettings,enabled=!state.busy,modifier=Modifier.testTag("editor-settings")){Text(localize(Res.string.tp_edit_settings))}
        } }
        item { TextButton(onRules,enabled=!state.busy && state.snapshot!=null){Text(localize(Res.string.tp_edit_rules))} }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            FilterChip(state.mode==EditingMode.Grammar,{onMode(EditingMode.Grammar)},label={Text(localize(Res.string.tp_edit_grammar))},enabled=!state.busy,modifier=Modifier.weight(1f))
            FilterChip(state.mode==EditingMode.Translate,{onMode(EditingMode.Translate)},label={Text(localize(Res.string.tp_edit_translate))},enabled=!state.busy,modifier=Modifier.weight(1f))
        } }
        item { Text(localize(Res.string.tp_edit_send_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
        state.error?.let {error->item {Text(error,color=MaterialTheme.colorScheme.error)} }
        if(state.busy) item {
            LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
            Text(if(state.total>0) "${localize(Res.string.tp_edit_progress)} ${state.progress}/${state.total}" else localize(Res.string.tp_edit_load))
            if(state.total>0) TextButton(onCancel){Text(localize(Res.string.tp_edit_cancel))}
        }
        item { Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Button(onGenerate,enabled=!state.busy && state.snapshot!=null,modifier=Modifier.weight(1f)) {Text(localize(if(proposal==null)Res.string.tp_edit_generate else Res.string.tp_edit_regenerate))}
            if(proposal!=null) Button(onApply,enabled=!state.busy && changed.isNotEmpty(),modifier=Modifier.weight(1f).testTag("editor-apply")){Text(localize(Res.string.tp_edit_apply))}
        } }
        if(state.snapshot?.hasOriginal==true) item {TextButton(onRestore,enabled=!state.busy){Text(localize(Res.string.tp_edit_restore))}}
        if(proposal!=null) {
            item {Text(localize(Res.string.tp_edit_review_hint),style=MaterialTheme.typography.bodySmall)
                Text(if(changed.isEmpty())localize(Res.string.tp_edit_no_changes) else "${localize(Res.string.tp_edit_change_count)}: ${changed.size}")}
            items(changed,key={it}) {index->
                Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
                    Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("${localize(Res.string.tp_edit_before)} · ${index+1}",style=MaterialTheme.typography.labelMedium)
                        Text((proposal.snapshot.pages[index] as PageText).text,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider()
                        Text(localize(Res.string.tp_edit_after),style=MaterialTheme.typography.labelMedium)
                        Text((proposal.pages[index] as PageText).text)
                        TextButton(onClick={editingIndex=index;editingText=(proposal.pages[index] as PageText).text},enabled=!state.busy){Text(localize(Res.string.tp_edit_edit))}
                    }
                }
            }
        }
    }
    if(editingIndex!=null) AlertDialog(onDismissRequest={editingIndex=null},title={Text(localize(Res.string.tp_edit_manual_edit))},
        text={OutlinedTextField(editingText,{if(it.length<=18_000)editingText=it},modifier=Modifier.fillMaxWidth().heightIn(max=300.dp))},
        confirmButton={TextButton(onClick={onEditParagraph(editingIndex!!,editingText);editingIndex=null},enabled=editingText.isNotBlank()){Text(localize(Res.string.tp_edit_save))}},
        dismissButton={TextButton(onClick={editingIndex=null}){Text(localize(Res.string.tp_edit_cancel))}})
}
