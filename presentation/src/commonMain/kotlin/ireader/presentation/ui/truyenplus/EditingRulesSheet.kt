package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ireader.domain.data.repository.TextReplacementRepository
import ireader.domain.models.entities.TextReplacement
import ireader.domain.models.editing.EditingRule
import ireader.domain.utils.extensions.currentTimeToLong
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditingRulesSheet(bookId:Long,beforeChange:()->Unit={},onChanged:()->Unit={},onDismiss:()->Unit) {
    val repo:TextReplacementRepository=koinInject()
    val rules by remember(repo,bookId){repo.getReplacementsForBook(bookId)}.collectAsState(emptyList())
    val scope=rememberCoroutineScope()
    var editing by remember{mutableStateOf<TextReplacement?>(null)}
    var find by remember{mutableStateOf("")}
    var replace by remember{mutableStateOf("")}
    var wholeWord by remember{mutableStateOf(false)}
    var regex by remember{mutableStateOf(false)}
    var sensitive by remember{mutableStateOf(false)}
    var global by remember{mutableStateOf(false)}
    var formOpen by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf<String?>(null)}
    var busy by remember{mutableStateOf(false)}
    val invalid=localize(Res.string.tp_edit_invalid_rule)
    fun launchAction(block:suspend ()->Unit) {busy=true;error=null;scope.launch {
        try{beforeChange();block();onChanged()}catch(e:CancellationException){throw e}catch(e:Exception){error=e.message}finally{busy=false}
    }}
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        LazyColumn(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            item{Text(localize(Res.string.tp_edit_rules),style=MaterialTheme.typography.headlineSmall)}
            item{Text(localize(Res.string.tp_edit_rules_hint),style=MaterialTheme.typography.bodySmall)}
            error?.let{item{Text(it,color=MaterialTheme.colorScheme.error)}}
            if(formOpen) {
                item{OutlinedTextField(find,{find=it.take(500)},label={Text(localize(Res.string.tp_edit_find))},modifier=Modifier.fillMaxWidth(),enabled=!busy)}
                item{OutlinedTextField(replace,{replace=it.take(2000)},label={Text(localize(Res.string.tp_edit_replace))},modifier=Modifier.fillMaxWidth(),enabled=!busy)}
                item{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(regex,{regex=it},enabled=!busy);Text(localize(Res.string.tp_edit_regex))}}
                item{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(wholeWord,{wholeWord=it},enabled=!busy);Text(localize(Res.string.tp_edit_whole_word))}}
                item{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(sensitive,{sensitive=it},enabled=!busy);Text(localize(Res.string.tp_edit_case))}}
                item{Row(verticalAlignment=Alignment.CenterVertically){Checkbox(global,{global=it},enabled=!busy);Text(localize(Res.string.tp_edit_global))}}
                item{Row {
                    Button(onClick={
                        val rule=EditingRule(find,replace,regex,wholeWord)
                        val pattern=runCatching{rule.pattern()}.getOrNull()
                        if(pattern==null){error=invalid;return@Button}
                        launchAction{
                            val now=currentTimeToLong()
                            val value=TextReplacement(id=editing?.id?:0,bookId=if(global)null else bookId,name=find.take(100),findText="/$pattern/",replaceText=rule.replacement(),description=rule.metadata(),caseSensitive=sensitive,enabled=editing?.enabled?:true,createdAt=editing?.createdAt?:now,updatedAt=now)
                            if(editing==null)repo.insert(value) else repo.update(value)
                            formOpen=false
                        }
                    },enabled=!busy){Text(localize(Res.string.tp_edit_save))}
                    TextButton(onClick={formOpen=false},enabled=!busy){Text(localize(Res.string.tp_edit_cancel))}
                }}
            } else {
                item{Button(onClick={editing=null;find="";replace="";regex=false;wholeWord=false;sensitive=false;global=false;formOpen=true},enabled=!busy){Text(localize(Res.string.tp_edit_add_rule))}}
                if(rules.isEmpty())item{Text(localize(Res.string.tp_edit_no_rules))}
                items(rules,key={it.id}){rule->
                    Column {
                        Row(verticalAlignment=Alignment.CenterVertically){
                            Switch(rule.enabled,{launchAction{repo.toggleEnabled(rule.id)}},enabled=!busy)
                            Text(rule.name,Modifier.weight(1f).padding(start=12.dp))
                        }
                        Text(localize(if(rule.bookId==null)Res.string.tp_edit_global else Res.string.tp_edit_this_book),style=MaterialTheme.typography.labelSmall)
                        Row {
                            TextButton(onClick={editing=rule;find=rule.findText;replace=rule.replaceText;regex=true;sensitive=rule.caseSensitive;global=rule.bookId==null;formOpen=true
                                if(find.startsWith("/") && find.lastIndexOf('/')>0)find=find.substring(1,find.lastIndexOf('/'))
                                wholeWord=false
                                EditingRule.fromMetadata(rule.description)?.let{draft->find=draft.find;replace=draft.replace;regex=draft.regex;wholeWord=draft.wholeWord}
                            },enabled=!busy){Text(localize(Res.string.tp_edit_edit))}
                            TextButton(onClick={launchAction{repo.delete(rule.id)}},enabled=!busy){Text(localize(Res.string.tp_edit_delete))}
                        }
                        HorizontalDivider()
                    }
                }
            }
            item{TextButton(onDismiss,enabled=!busy){Text(localize(Res.string.tp_edit_close))}}
        }
    }
}
