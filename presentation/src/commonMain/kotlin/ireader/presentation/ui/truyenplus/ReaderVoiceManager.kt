package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.semantics.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.services.tts_service.v2.*
import ireader.domain.services.tts_service.v2.online.*
import ireader.domain.services.tts_service.v2.offline.*
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import org.koin.compose.koinInject

@Composable
fun ReaderVoiceManager(store:OfflineVoiceStore,onSystem:()->Unit,onDismiss:()->Unit) {
    val state by store.state.collectAsState()
    val scope=rememberCoroutineScope()
    val controller:TTSController=koinInject()
    val actualEngine by remember(controller){controller.state.map {it.engineName}.distinctUntilChanged()}.collectAsState(initial=controller.state.value.engineName)
    val preferences:AppPreferences=koinInject()
    val onlineSupported=org.koin.mp.KoinPlatform.getKoin().getOrNull<CapCutSpeechSource>()!=null
    val selectedOnline by preferences.capCutVoiceId().changes().collectAsState(initial=preferences.capCutVoiceId().get())
    val uiPreferences:ireader.domain.preferences.prefs.UiPreferences=koinInject()
    val namePreference=remember(uiPreferences){uiPreferences.readerVoiceNames()}
    val storedNames by namePreference.changes().collectAsState(namePreference.get())
    val preferredNames=remember(storedNames){runCatching {Json.decodeFromString<Map<String,String>>(storedNames)}.getOrDefault(emptyMap())}
    var previewJob by remember {mutableStateOf<Job?>(null)}
    var previewGeneration by remember {mutableIntStateOf(0)}
    var preparing by remember {mutableStateOf(false)}
    var previewing by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    var previewError by remember {mutableStateOf<String?>(null)}
    var resumeOnDismiss by remember {mutableStateOf(false)}
    fun stopPreview() {previewGeneration++;previewJob?.cancel();previewJob=null;previewing=false;preparing=false}
    fun dismiss() {stopPreview();val resume=resumeOnDismiss;resumeOnDismiss=false;if(resume)controller.dispatch(TTSCommand.Play);onDismiss()}
    ireader.presentation.core.ui.PlatformBackHandler(true,::dismiss)
    DisposableEffect(Unit) {onDispose {previewJob?.cancel();if(resumeOnDismiss){resumeOnDismiss=false;controller.dispatch(TTSCommand.Play)}}}
    val preview:(String,Int)->Unit = {id,speaker ->
        stopPreview();failed=false;previewError=null
        val config=store.config(id,speaker)
        val previewToken=previewGeneration
        if(config!=null || CapCutVoices.find(id)!=null)previewJob=scope.launch {
            var engine:TTSEngine?=null
            try {
                previewing=true;preparing=true
                val old=controller.state.value
                if(old.isPlaying || old.isLoading) {
                    resumeOnDismiss=true;controller.dispatch(TTSCommand.Pause)
                    withTimeout(2000){controller.state.first {!it.isPlaying && !it.isLoading}}
                }
                val probe=(if(config!=null)TTSEngineFactory.createOfflineEngine(config) else TTSEngineFactory.createCapCutEngine(id)) ?: error("Unsupported runtime")
                engine=probe
                withTimeout(30_000){probe.events.first {it is EngineEvent.Ready}}
                val done=async(start=CoroutineStart.UNDISPATCHED) {
                    withTimeout(60_000){probe.events.first {event ->
                        if(event is EngineEvent.Started && previewToken==previewGeneration)preparing=false
                        event is EngineEvent.Completed || event is EngineEvent.Error}}
                }
                probe.speak("Gió đêm thổi qua, mang theo hương hoa thoang thoảng. Chào bạn, đây là giọng đọc tiếng Việt.","voice-preview")
                val result=done.await()
                if(result is EngineEvent.Error && previewToken==previewGeneration)
                    previewError=result.message.takeIf {it.startsWith("CapCut ")}
                check(result is EngineEvent.Completed)
                check(probe.name.startsWith(config?.model?.title ?: "CapCut"))
            } catch(_:TimeoutCancellationException){if(previewToken==previewGeneration)failed=true}
            catch(e:CancellationException){throw e}
            catch(_:Exception){if(previewToken==previewGeneration)failed=true}
            finally {withContext(Dispatchers.IO+NonCancellable){engine?.release()};if(previewToken==previewGeneration){previewing=false;preparing=false}}
        }
    }
    VoiceModelsPanel(state,previewing,failed,store::startDownload,store::cancelDownload,
        onDelete={id -> scope.launch {
            stopPreview()
            if(state.selection?.modelId==id) {
                store.select(null);controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
            }
            store.remove(id)
        }},onSelect={id,speaker ->
            stopPreview()
            if(store.select(id,speaker)) {
                preferences.capCutVoiceId().set("")
                preferences.useGradioTTS().set(false);preferences.useLocalTTS().set(false)
                controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
                if(resumeOnDismiss)controller.dispatch(TTSCommand.Play)
                resumeOnDismiss=false;onDismiss()
            }
        },onPreview=preview,onSystem={
            stopPreview();store.select(null);preferences.capCutVoiceId().set("")
            preferences.useGradioTTS().set(false);preferences.useLocalTTS().set(false)
            controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
            if(resumeOnDismiss)controller.dispatch(TTSCommand.Play)
            resumeOnDismiss=false;onSystem()
        },onDismiss=::dismiss,onStopPreview=::stopPreview,previewPreparing=preparing,actualEngine=actualEngine,preferredNames=preferredNames,
        onlineSupported=onlineSupported,selectedOnline=selectedOnline,previewError=previewError,
        onOnlinePreview={preview(it,0)},onOnlineSelect={id->
            if(CapCutVoices.find(id)!=null) {
                stopPreview();store.select(null);preferences.capCutVoiceId().set(id)
                preferences.useGradioTTS().set(false);preferences.useLocalTTS().set(false)
                controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
                if(resumeOnDismiss)controller.dispatch(TTSCommand.Play)
                resumeOnDismiss=false;onDismiss()
            }
        },onRename={id,speaker,name->
            val key="$id:$speaker"
            val updated=if(name.isBlank())preferredNames-key else preferredNames+(key to name.trim().take(120))
            namePreference.set(Json.encodeToString(updated))
        })
}

/** Pure UI: selected model IDs and availability are supplied by the verified file store. */
@Composable
fun VoiceModelsPanel(state:OfflineVoiceState,previewing:Boolean,previewFailed:Boolean,
    onDownload:(String)->Unit,onCancel:(String)->Unit,onDelete:(String)->Unit,
    onSelect:(String,Int)->Unit,onPreview:(String,Int)->Unit,onSystem:()->Unit,onDismiss:()->Unit,
    onStopPreview:()->Unit={},previewPreparing:Boolean=true,actualEngine:String="",preferredNames:Map<String,String> = emptyMap(),onRename:(String,Int,String)->Unit={_,_,_->},
    onlineSupported:Boolean=false,selectedOnline:String="",onOnlineSelect:(String)->Unit={},onOnlinePreview:(String)->Unit={},previewError:String?=null) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var speakers by remember {mutableStateOf(mapOf<String,Int>())}
    var renameModel by rememberSaveable {mutableStateOf<String?>(null)}
    var renameSpeaker by rememberSaveable {mutableIntStateOf(0)}
    var renameText by rememberSaveable {mutableStateOf("")}
    val dismiss:()->Unit={if(renameModel!=null)renameModel=null else onDismiss()}
    ireader.presentation.core.ui.PlatformBackHandler(renameModel!=null){renameModel=null}
    val pane=s.localize(Res.string.tp_voice_manager)
    ReaderPaperTheme {
        BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
            Box(Modifier.fillMaxSize().clickable(remember {androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=dismiss))
            Surface(Modifier.align(Alignment.Center).widthIn(max=720.dp).fillMaxWidth().padding(12.dp)
                .heightIn(max=(maxHeight-24.dp).coerceAtLeast(160.dp)).readerPanel(28).testTag("voice-manager").semantics {paneTitle=pane;isTraversalGroup=true},
                shape=RoundedCornerShape(28.dp),color=Color.Transparent) {
                ReaderGlassPanelColumn(Modifier.padding(16.dp)) {
                    ReaderBooksSheetHeader(s.localize(Res.string.tp_voice_manager),dismiss)
                    if(renameModel!=null) {
                        Text(OfflineVoiceCatalog.models.firstOrNull {it.id==renameModel}?.title.orEmpty()+" · "+s.localize(Res.string.tp_voice_number,renameSpeaker+1))
                        OutlinedTextField(renameText,{renameText=it.take(120)},label={Text(s.localize(Res.string.tp_edit_name))},modifier=Modifier.fillMaxWidth().testTag("voice-name-input"),singleLine=true)
                        Row(horizontalArrangement=Arrangement.spacedBy(12.dp)) {
                            Button(onClick={onRename(renameModel!!,renameSpeaker,renameText);renameModel=null},modifier=Modifier.heightIn(min=48.dp).testTag("voice-name-save")){Text(s.localize(Res.string.tp_edit_save))}
                            TextButton(onClick={renameModel=null},modifier=Modifier.heightIn(min=48.dp)){Text(s.localize(Res.string.tp_edit_cancel))}
                        }
                    } else {
                    OutlinedButton(onClick=onSystem,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("voice-system")) {
                        Icon(Icons.Default.Mic,null);Spacer(Modifier.width(8.dp));Text(s.localize(Res.string.tp_system_voice))
                    }
                    if(previewing)Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        if(previewPreparing)CircularProgressIndicator(Modifier.size(18.dp),strokeWidth=2.dp)
                        else Icon(Icons.Default.VolumeUp,null,Modifier.size(18.dp))
                        Text(s.localize(if(previewPreparing)Res.string.tp_voice_installing else Res.string.tp_voice_preview_active),Modifier.weight(1f).padding(8.dp),style=MaterialTheme.typography.labelMedium)
                        IconButton(onClick=onStopPreview){Icon(Icons.Default.Stop,s.localize(Res.string.stop))}
                    }
                    if(previewFailed)Text(s.localize(Res.string.tp_voice_preview_error),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                    if(previewFailed && previewError!=null)Text(previewError,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelSmall)
                    LazyColumn(Modifier.weight(1f,fill=false),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=16.dp)) {
                        if(onlineSupported)item(key="capcut-online") {
                            CapCutVoiceCard(selectedOnline,previewing,onOnlineSelect,onOnlinePreview)
                        }
                        items(OfflineVoiceCatalog.models,key={it.id}) {model ->
                            val installed=model.id in state.installed
                            val downloading=state.downloads[model.id]
                            val speaker=speakers[model.id] ?: state.selection?.takeIf {it.modelId==model.id}?.speaker ?: model.defaultSpeaker
                            Surface(Modifier.fillMaxWidth().readerGlassCard(18),shape=RoundedCornerShape(18.dp),color=Color.Transparent) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(model.title,style=MaterialTheme.typography.titleMedium)
                                            if(actualEngine.startsWith(model.title))Text(s.localize(Res.string.tp_voice_active)+
                                                if(model.speakers>1)" · "+actualEngine.substringAfter(" · ","") else "",
                                                style=MaterialTheme.typography.labelSmall,modifier=Modifier.testTag("voice-active-${model.id}"))
                                            else if(state.selection?.modelId==model.id)Text(s.localize(Res.string.tp_voice_selected),style=MaterialTheme.typography.labelSmall)
                                            Text(s.localize(if(installed)Res.string.tp_voice_downloaded else Res.string.tp_voice_model_size,(model.archiveBytes/1048576f).toInt()+1),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if(installed)IconButton(onClick={onDelete(model.id)},modifier=Modifier.testTag("voice-delete-${model.id}")){Icon(Icons.Default.DeleteOutline,s.localize(Res.string.delete))}
                                    }
                                    if(model.speakers>1) {
                                        val voiceList=rememberLazyListState(initialFirstVisibleItemIndex=speaker)
                                        LaunchedEffect(speaker){voiceList.requestScrollToItem(speaker)}
                                        LazyRow(state=voiceList,modifier=Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                        items(model.speakers) {id->FilterChip(speaker==id,onClick={speakers=speakers+(model.id to id)},
                                            label={Text(preferredNames["${model.id}:$id"] ?: model.voiceNames.getOrNull(id) ?: s.localize(Res.string.tp_voice_number,id+1))},modifier=Modifier.testTag("voice-speaker-${model.id}-$id"))}
                                        }
                                    }
                                    if(model.speakers==1)preferredNames["${model.id}:$speaker"]?.let {Text(it,style=MaterialTheme.typography.bodyMedium)}
                                    TextButton(onClick={renameModel=model.id;renameSpeaker=speaker;renameText=preferredNames["${model.id}:$speaker"] ?: if(model.speakers>1)(model.voiceNames.getOrNull(speaker) ?: s.localize(Res.string.tp_voice_number,speaker+1)) else model.title},modifier=Modifier.heightIn(min=48.dp).testTag("voice-rename-${model.id}")){Icon(Icons.Default.Edit,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp));Text(s.localize(Res.string.tp_edit_name))}
                                    if(model.id in state.errors)Text(s.localize(Res.string.tp_voice_download_error),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                                    if(downloading!=null) {
                                        if(downloading.phase==VoiceDownloadPhase.Downloading)LinearProgressIndicator(
                                            progress={(downloading.bytes.toFloat()/downloading.totalBytes).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
                                        else LinearProgressIndicator(Modifier.fillMaxWidth())
                                        Row(verticalAlignment=Alignment.CenterVertically) {
                                            Text(s.localize(if(downloading.phase==VoiceDownloadPhase.Installing)Res.string.tp_voice_installing else Res.string.tp_voice_downloading),Modifier.weight(1f),style=MaterialTheme.typography.labelSmall)
                                            TextButton(onClick={onCancel(model.id)}){Text(s.localize(Res.string.cancel))}
                                        }
                                    } else if(!installed)OutlinedButton(onClick={onDownload(model.id)},modifier=Modifier.testTag("voice-download-${model.id}")) {Text(s.localize(Res.string.download))}
                                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(onClick={onPreview(model.id,speaker)},enabled=installed && !previewing,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("voice-preview-${model.id}")) {Text(s.localize(Res.string.tp_voice_preview))}
                                        Button(onClick={onSelect(model.id,speaker)},enabled=installed,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("voice-use-${model.id}")) {Text(s.localize(Res.string.tp_voice_use))}
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable private fun CapCutVoiceCard(selected:String,previewing:Boolean,onUse:(String)->Unit,onPreview:(String)->Unit) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var voice by rememberSaveable {mutableStateOf(CapCutVoices.find(selected)?.id ?: CapCutVoices.voices.first().id)}
    Surface(Modifier.fillMaxWidth().readerGlassCard(18),shape=RoundedCornerShape(18.dp),color=Color.Transparent) {
        Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            Text("CapCut",style=MaterialTheme.typography.titleMedium)
            Text(s.localize(Res.string.tp_capcut_online),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                items(CapCutVoices.voices,key={it.id}) {item->
                    FilterChip(voice==item.id,{voice=item.id},label={Text(item.title)},modifier=Modifier.testTag("voice-capcut-${item.id}"))
                }
            }
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedButton({onPreview(voice)},enabled=!previewing,modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("voice-preview-capcut")){Text(s.localize(Res.string.tp_voice_preview))}
                Button({onUse(voice)},modifier=Modifier.weight(1f).heightIn(min=48.dp).testTag("voice-use-capcut")){Text(s.localize(Res.string.tp_voice_use))}
            }
        }
    }
}
