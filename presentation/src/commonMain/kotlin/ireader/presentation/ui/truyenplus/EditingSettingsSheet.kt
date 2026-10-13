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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EditingSettingsSheet(onDismiss:()->Unit) {
    val settings: EditingSettings=koinInject()
    val scope=rememberCoroutineScope()
    var config by remember{mutableStateOf(EditingConfig())}
    var key by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(true)}
    var message by remember{mutableStateOf<String?>(null)}
    val saved=localize(Res.string.tp_edit_saved)
    LaunchedEffect(settings) {
        try{config=settings.load()}catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}
    }
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true)) {
        LazyColumn(Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
            item {Text(localize(Res.string.tp_edit_settings),style=MaterialTheme.typography.headlineSmall)}
            item {Text(localize(Res.string.tp_edit_key_info),style=MaterialTheme.typography.bodySmall)}
            item {Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {EditingProvider.entries.forEach {provider->
                FilterChip(config.provider==provider,onClick={config=EditingConfig(provider);key="";message=null},enabled=!busy,label={Text(provider.name)})
            }}}
            item {OutlinedTextField(config.model,{config=config.copy(model=it.take(100))},label={Text(localize(Res.string.tp_edit_model))},singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
            item {OutlinedTextField(key,{key=it.take(512)},label={Text(localize(Res.string.tp_edit_key))},supportingText={Text(localize(Res.string.tp_edit_key_hint))},visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),singleLine=true,enabled=!busy,modifier=Modifier.fillMaxWidth())}
            message?.let{item{Text(it)}}
            item {Button(onClick={busy=true;scope.launch {
                try{settings.save(config,key.takeIf{it.isNotBlank()});key="";message=saved}
                catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}
            }},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text(localize(Res.string.tp_edit_save))}}
            item {TextButton(onClick={busy=true;scope.launch {
                try{settings.clearKey(config.provider);key="";message=saved}
                catch(e:CancellationException){throw e}catch(e:Exception){message=e.message}finally{busy=false}
            }},enabled=!busy){Text(localize(Res.string.tp_edit_delete_key))}}
            item {TextButton(onDismiss,enabled=!busy){Text(localize(Res.string.tp_edit_close))}}
        }
    }
}
