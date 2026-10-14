package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import ireader.domain.models.editing.*
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.coroutines.*
import org.koin.compose.koinInject

@Composable fun EditingSettingsSheet(onDismiss:()->Unit) {
    val settings: EditingSettings=koinInject()
    val api:ChapterEditingApi=koinInject()
    val scope=rememberCoroutineScope()
    var config by remember{mutableStateOf(EditingConfig())}
    var key by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(true)}
    var configured by remember{mutableStateOf(false)}
    var advanced by remember{mutableStateOf(false)}
    var remove by remember{mutableStateOf(false)}
    var message by remember{mutableStateOf<String?>(null)}
    val saved=localize(Res.string.tp_edit_saved)
    val failed=localize(Res.string.tp_operation_failed)
    val connected=localize(Res.string.tp_edit_connection_ok)
    LaunchedEffect(settings) {
        try{config=settings.load();configured=runCatching {settings.credentials(config)}.isSuccess}
        catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}
    }
    fun action(block:suspend ()->Unit) {if(!busy){busy=true;message=null;scope.launch {
        try{block()}catch(e:TimeoutCancellationException){message=failed}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}
    }}}
    ReaderGlassSheet(onDismissRequest={if(!busy)onDismiss()}) {
        LazyColumn(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Text(localize(Res.string.tp_edit_settings),style=MaterialTheme.typography.titleLarge)}
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {EditingProvider.entries.forEach {provider->
                FilterChip(config.provider==provider,onClick={config=EditingConfig(provider);key="";message=null;scope.launch {configured=runCatching {settings.credentials(config)}.isSuccess}},enabled=!busy,label={Text(provider.name)},modifier=Modifier.heightIn(min=48.dp))
            }}}
            item {Text(localize(if(configured)Res.string.tp_edit_key_configured else Res.string.tp_edit_key_missing),style=MaterialTheme.typography.labelLarge)}
            item {OutlinedTextField(key,{key=it.take(512)},label={Text(localize(Res.string.tp_edit_key))},supportingText={Text(localize(Res.string.tp_edit_key_hint))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
            item {TextButton(onClick={advanced=!advanced},modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_model_advanced))}}
            if(advanced) {
                item {FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    val models=if(config.provider==EditingProvider.Gemini)listOf("gemini-3.8-flash","gemini-3.5-flash-lite") else listOf("deepseek-flash","deepseek-v4-pro")
                    models.forEach {model->FilterChip(config.model==model,{config=config.copy(model=model)},enabled=!busy,label={Text(model)},modifier=Modifier.heightIn(min=48.dp))}
                }}
                item {OutlinedTextField(config.model,{config=config.copy(model=it.take(100))},label={Text(localize(Res.string.tp_edit_model))},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
            }
            message?.let{item{Text(it,style=MaterialTheme.typography.bodyMedium)}}
            if(busy)item {LinearProgressIndicator(Modifier.fillMaxWidth())}
            item {FlowRow(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                Button(onClick={action {
                    settings.save(config,key.takeIf{it.isNotBlank()});key="";configured=runCatching {settings.credentials(config)}.isSuccess;message=saved
                }},enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_save))}
                OutlinedButton(onClick={action {
                    val credentials=if(key.isNotBlank())EditingCredentials(config,key) else settings.credentials(config)
                    api.checkConnection(credentials);message=connected
                }},enabled=!busy && (configured || key.isNotBlank()),modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_test_connection))}
            }}
            if(configured)item {TextButton(onClick={remove=true},enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_delete_key))}}
            item {TextButton(onDismiss,enabled=!busy,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.tp_edit_close))}}
        }
    }
    if(remove)ReaderGlassDialog(onDismissRequest={remove=false},title={Text(localize(Res.string.tp_edit_delete_key))},text={Text(config.provider.name)},
        confirmButton={TextButton(onClick={remove=false;action {settings.clearKey(config.provider);key="";configured=false;message=saved}}){Text(localize(Res.string.tp_edit_delete))}},
        dismissButton={TextButton(onClick={remove=false}){Text(localize(Res.string.tp_edit_cancel))}})
}
