package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ireader.core.source.model.Text as PageText
import ireader.domain.models.editing.*
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.coroutines.*
import org.koin.compose.koinInject

@Composable fun ChapterEditorSheet(chapterId: Long,onDismiss:()->Unit,beforeChange:()->Unit,onChanged:(Long)->Unit) {
    val store: ChapterEditingStore=koinInject()
    val settings: EditingSettings=koinInject()
    val editor: EditChapter=koinInject()
    val scope=rememberCoroutineScope()
    val controller=remember(chapterId){ChapterEditorController(scope,store,settings,editor)}
    val state by controller.state.collectAsState()
    var pane by rememberSaveable(chapterId){mutableStateOf("main")}
    var restoreOpen by remember{mutableStateOf(false)}
    var closing by remember {mutableStateOf(false)}
    val draftFailure=localize(Res.string.tp_operation_failed)
    LaunchedEffect(controller){controller.load(chapterId)}
    DisposableEffect(controller){onDispose{controller.cancel()}}
    LaunchedEffect(state.proposal,state.manual,state.selected,state.mode,state.busy) {
        if(!state.busy){delay(500);try {controller.persistDraft()}catch(e:CancellationException){throw e}catch(_:Exception){controller.reportError(draftFailure)}}
    }
    val close:()->Unit={if(!closing){closing=true;controller.cancel();scope.launch {
        try {withContext(NonCancellable){controller.persistDraft()};onDismiss()}
        catch(e:CancellationException){throw e}
        catch(_:Exception){closing=false;controller.reportError(draftFailure)}
    }}}
    val bookId=state.snapshot?.bookId
    when(pane) {
        "settings" -> EditingSettingsSheet {pane="main"}
        "characters" -> if(bookId!=null)EditingCharactersSheet(bookId,store){pane="main"}
        "rules" -> if(bookId!=null)EditingRulesSheet(bookId,beforeChange,onChanged={onChanged(chapterId)}){pane="main"}
        else -> ReaderGlassSheet(onDismissRequest=close) {
            ChapterEditorContent(state.copy(busy=state.busy || closing),controller::generate,controller::cancel,
                {controller.apply(beforeChange,onChanged)},{restoreOpen=true},{pane="characters"},{pane="settings"},{pane="rules"},controller::mode,controller::changeParagraph,
                onSelect=controller::select,onManual=controller::beginManual,onManualText=controller::editManual,onManualSave=controller::saveManual,onManualCancel=controller::cancelManual,onClose=close)
        }
    }
    if(restoreOpen) ReaderGlassDialog(onDismissRequest={restoreOpen=false},title={Text(localize(Res.string.tp_edit_restore))},text={Text(localize(Res.string.tp_edit_restore_question))},
        confirmButton={TextButton(onClick={restoreOpen=false;controller.restore(beforeChange,onChanged)}){Text(localize(Res.string.tp_edit_restore))}},dismissButton={TextButton(onClick={restoreOpen=false}){Text(localize(Res.string.tp_edit_cancel))}})
}

/** Bounded, linear diff span; avoids quadratic work on long paragraphs. */
internal fun changedTextRange(before:String,after:String):IntRange? {
    if(before==after)return null
    var start=0
    while(start<minOf(before.length,after.length) && before[start]==after[start])start++
    var tail=0
    while(tail<before.length-start && tail<after.length-start && before[before.lastIndex-tail]==after[after.lastIndex-tail])tail++
    return start until (after.length-tail)
}

@Composable fun ChapterEditorContent(state: ChapterEditorState,onGenerate:()->Unit,onCancel:()->Unit,onApply:()->Unit,onRestore:()->Unit,
    onMemory:()->Unit,onSettings:()->Unit,onRules:()->Unit,onMode:(EditingMode)->Unit,onEditParagraph:(Int,String)->Unit={_,_->},
    onSelect:(Int,Boolean)->Unit={_,_->},onManual:(Int)->Unit={},onManualText:(String)->Unit={},onManualSave:()->Unit={},onManualCancel:()->Unit={},onClose:()->Unit={}) {
    val title=localize(Res.string.tp_edit_chapter)
    val proposal=state.proposal
    val changed=remember(proposal){proposal?.pages?.indices?.filter{proposal.pages[it] is PageText && proposal.pages[it]!=proposal.snapshot.pages[it]}?:emptyList()}
    val selected=state.selected ?: changed.toSet()
    val manual=state.manual
    var confirmDiscard by remember {mutableStateOf(false)}
    Column(Modifier.fillMaxWidth().heightIn(max=900.dp).imePadding().navigationBarsPadding().semantics {paneTitle=title}) {
        Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
            Text(if(manual==null)title else localize(Res.string.tp_edit_manual_edit),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
            IconButton(onClick=onClose,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,localize(Res.string.tp_edit_close))}
        }
        if(manual!=null) {
            Column(Modifier.fillMaxWidth().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(manual.text,onManualText,enabled=!state.busy,modifier=Modifier.fillMaxWidth().heightIn(max=LocalReaderViewport.current.height*.5f).testTag("editor-manual"),minLines=3)
                Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                    Button(onManualSave,enabled=!state.busy && manual.text.isNotBlank(),modifier=Modifier.weight(1f).heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_save))}
                    TextButton(onClick={confirmDiscard=true},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_cancel))}
                }
            }
        } else LazyColumn(Modifier.fillMaxWidth().weight(1f,fill=false),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                TextButton(onMemory,enabled=!state.busy && state.snapshot!=null,modifier=Modifier.heightIn(min=48.dp).testTag("editor-memory")){Text(localize(Res.string.tp_edit_memory))}
                TextButton(onRules,enabled=!state.busy && state.snapshot!=null,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_rules))}
                TextButton(onSettings,enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp).testTag("editor-settings")){Text(localize(Res.string.tp_edit_settings))}
            } }
            if(state.draftRestored)item {Text(localize(Res.string.tp_draft_restored),style=MaterialTheme.typography.labelLarge)}
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                FilterChip(state.mode==EditingMode.Grammar,{onMode(EditingMode.Grammar)},label={Text(localize(Res.string.tp_edit_grammar))},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp))
                FilterChip(state.mode==EditingMode.Translate,{onMode(EditingMode.Translate)},label={Text(localize(Res.string.tp_edit_translate))},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp))
            } }
            item {Text(localize(Res.string.tp_edit_send_hint),style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}
            state.error?.let {error->item {Text(error,color=MaterialTheme.colorScheme.error)} }
            if(state.busy)item {
                LinearProgressIndicator(modifier=Modifier.fillMaxWidth())
                Text(if(state.total>0) "${localize(Res.string.tp_edit_progress)} ${state.progress}/${state.total}" else localize(Res.string.tp_edit_load))
                if(state.total>0)TextButton(onCancel,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_cancel))}
            }
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onGenerate,enabled=!state.busy && state.snapshot!=null,modifier=Modifier.heightIn(min=48.dp)){Text(localize(if(proposal==null)Res.string.tp_edit_generate else Res.string.tp_edit_regenerate))}
                if(proposal!=null)Button(onApply,enabled=!state.busy && selected.intersect(changed.toSet()).isNotEmpty(),modifier=Modifier.heightIn(min=48.dp).testTag("editor-apply")){Text("${localize(Res.string.tp_edit_apply)} (${selected.intersect(changed.toSet()).size})")}
            } }
            if(state.snapshot?.hasOriginal==true)item {TextButton(onRestore,enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_restore))}}
            if(proposal!=null) {
                item {Text(localize(Res.string.tp_edit_review_hint),style=MaterialTheme.typography.bodySmall)
                    Text(if(changed.isEmpty())localize(Res.string.tp_edit_no_changes) else "${localize(Res.string.tp_edit_change_count)}: ${changed.size}")}
                items(changed,key={it}) {index->
                    Surface(shape=RoundedCornerShape(16.dp),color=MaterialTheme.colorScheme.surfaceContainer) {
                        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment=Alignment.CenterVertically) {
                                Checkbox(index in selected,{onSelect(index,it)},enabled=!state.busy)
                                Text("${localize(Res.string.tp_edit_after)} · ${index+1}",Modifier.weight(1f),style=MaterialTheme.typography.labelLarge)
                                TextButton(onClick={onManual(index)},enabled=!state.busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_edit))}
                            }
                            val before=(proposal.snapshot.pages[index] as PageText).text
                            val after=(proposal.pages[index] as PageText).text
                            BoxWithConstraints {
                                val beforeContent:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(localize(Res.string.tp_edit_before),style=MaterialTheme.typography.labelMedium)
                                    Text(before,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                }}
                                val afterContent:@Composable ()->Unit={Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text(localize(Res.string.tp_edit_after),style=MaterialTheme.typography.labelMedium)
                                    val color=MaterialTheme.colorScheme.onSurface.copy(alpha=.13f)
                                    val annotated=remember(before,after,color){buildAnnotatedString {
                                        append(after);changedTextRange(before,after)?.takeIf {!it.isEmpty()}?.let {addStyle(SpanStyle(background=color,fontWeight=FontWeight.Medium),it.first,it.last+1)}
                                    }}
                                    Text(annotated)
                                }}
                                if(maxWidth>=600.dp)Row(horizontalArrangement=Arrangement.spacedBy(20.dp)) {
                                    Box(Modifier.weight(1f)){beforeContent()};Box(Modifier.weight(1f)){afterContent()}
                                } else Column(verticalArrangement=Arrangement.spacedBy(12.dp)){beforeContent();HorizontalDivider();afterContent()}
                            }
                        }
                    }
                }
            }
        }
    }
    if(confirmDiscard)ReaderGlassDialog(onDismissRequest={confirmDiscard=false},title={Text(localize(Res.string.tp_discard_draft))},text={Text(localize(Res.string.tp_discard_draft_hint))},
        confirmButton={TextButton(onClick={confirmDiscard=false;onManualCancel()}){Text(localize(Res.string.tp_discard))}},dismissButton={TextButton(onClick={confirmDiscard=false}){Text(localize(Res.string.tp_keep_editing))}})
}
