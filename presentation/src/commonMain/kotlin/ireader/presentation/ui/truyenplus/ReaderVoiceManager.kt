package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.services.tts_service.v2.*
import ireader.domain.services.tts_service.v2.offline.*
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.koin.compose.koinInject

@Composable
fun ReaderVoiceManager(store:OfflineVoiceStore,onSystem:()->Unit,onDismiss:()->Unit) {
    val state by store.state.collectAsState()
    val scope=rememberCoroutineScope()
    val controller:TTSController=koinInject()
    val preferences:AppPreferences=koinInject()
    var previewJob by remember {mutableStateOf<Job?>(null)}
    var previewGeneration by remember {mutableIntStateOf(0)}
    var preparing by remember {mutableStateOf(false)}
    var previewing by remember {mutableStateOf(false)}
    var failed by remember {mutableStateOf(false)}
    var resumeOnDismiss by remember {mutableStateOf(false)}
    fun stopPreview() {previewGeneration++;previewJob?.cancel();previewJob=null;previewing=false;preparing=false}
    fun dismiss() {stopPreview();val resume=resumeOnDismiss;resumeOnDismiss=false;if(resume)controller.dispatch(TTSCommand.Play);onDismiss()}
    ireader.presentation.core.ui.PlatformBackHandler(true,::dismiss)
    DisposableEffect(Unit) {onDispose {previewJob?.cancel();if(resumeOnDismiss){resumeOnDismiss=false;controller.dispatch(TTSCommand.Play)}}}
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
                preferences.useGradioTTS().set(false);preferences.useLocalTTS().set(false)
                controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
                if(resumeOnDismiss)controller.dispatch(TTSCommand.Play)
                resumeOnDismiss=false;onDismiss()
            }
        },onPreview={id,speaker ->
            stopPreview();failed=false
            val config=store.config(id,speaker)
            val previewToken=previewGeneration
            if(config!=null)previewJob=scope.launch {
                var engine:TTSEngine?=null
                try {
                    previewing=true;preparing=true
                    val old=controller.state.value
                    if(old.isPlaying || old.isLoading) {
                        resumeOnDismiss=true;controller.dispatch(TTSCommand.Pause)
                        withTimeout(2000){controller.state.first {!it.isPlaying && !it.isLoading}}
                    }
                    val probe=TTSEngineFactory.createOfflineEngine(config) ?: error("Unsupported runtime")
                    engine=probe
                    withTimeout(30_000){probe.events.first {it is EngineEvent.Ready}}
                    check(probe.name.startsWith(config.model.title))
                    val done=async(start=CoroutineStart.UNDISPATCHED) {
                        withTimeout(60_000){probe.events.first {event ->
                            if(event is EngineEvent.Started && previewToken==previewGeneration)preparing=false
                            event is EngineEvent.Completed || event is EngineEvent.Error}}
                    }
                    probe.speak("Gió đêm thổi qua, mang theo hương hoa thoang thoảng. Chào bạn, đây là giọng đọc tiếng Việt.","voice-preview")
                    check(done.await() is EngineEvent.Completed)
                    check(probe.name.startsWith(config.model.title))
                } catch(_:TimeoutCancellationException){if(previewToken==previewGeneration)failed=true}
                catch(e:CancellationException){throw e}
                catch(_:Exception){if(previewToken==previewGeneration)failed=true}
                finally {engine?.release();if(previewToken==previewGeneration){previewing=false;preparing=false}}
            }
        },onSystem={
            stopPreview();store.select(null)
            preferences.useGradioTTS().set(false);preferences.useLocalTTS().set(false)
            controller.dispatch(TTSCommand.SetEngine(EngineType.NATIVE,true))
            if(resumeOnDismiss)controller.dispatch(TTSCommand.Play)
            resumeOnDismiss=false;onSystem()
        },onDismiss=::dismiss,onStopPreview=::stopPreview,previewPreparing=preparing)
}

/** Pure UI: selected model IDs and availability are supplied by the verified file store. */
@Composable
fun VoiceModelsPanel(state:OfflineVoiceState,previewing:Boolean,previewFailed:Boolean,
    onDownload:(String)->Unit,onCancel:(String)->Unit,onDelete:(String)->Unit,
    onSelect:(String,Int)->Unit,onPreview:(String,Int)->Unit,onSystem:()->Unit,onDismiss:()->Unit,
    onStopPreview:()->Unit={},previewPreparing:Boolean=true) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var speakers by remember {mutableStateOf(mapOf<String,Int>())}
    ReaderPaperTheme {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize().clickable(remember {androidx.compose.foundation.interaction.MutableInteractionSource()},indication=null,onClick=onDismiss))
            Surface(Modifier.align(Alignment.Center).widthIn(max=720.dp).fillMaxWidth().padding(12.dp)
                .heightIn(max=(maxHeight-24.dp).coerceAtLeast(160.dp)).readerPanel(28).testTag("voice-manager"),
                shape=RoundedCornerShape(28.dp),color=Color.Transparent) {
                Column(Modifier.padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Text(s.localize(Res.string.tp_voice_manager),Modifier.weight(1f),style=MaterialTheme.typography.titleLarge,maxLines=1,overflow=TextOverflow.Ellipsis)
                        IconButton(onClick=onDismiss,modifier=Modifier.size(48.dp)){Icon(Icons.Default.Close,s.localize(Res.string.close))}
                    }
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
                    LazyColumn(Modifier.weight(1f,fill=false),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(top=16.dp)) {
                        items(OfflineVoiceCatalog.models,key={it.id}) {model ->
                            val installed=model.id in state.installed
                            val downloading=state.downloads[model.id]
                            val speaker=speakers[model.id] ?: state.selection?.takeIf {it.modelId==model.id}?.speaker ?: 0
                            Surface(Modifier.fillMaxWidth().readerGlassCard(18),shape=RoundedCornerShape(18.dp),color=Color.Transparent) {
                                Column(Modifier.padding(12.dp)) {
                                    Row(verticalAlignment=Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            Text(model.title,style=MaterialTheme.typography.titleMedium)
                                            Text(s.localize(if(installed)Res.string.tp_voice_downloaded else Res.string.tp_voice_model_size,(model.archiveBytes/1048576f).toInt()+1),style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        if(installed)IconButton(onClick={onDelete(model.id)},modifier=Modifier.testTag("voice-delete-${model.id}")){Icon(Icons.Default.DeleteOutline,s.localize(Res.string.delete))}
                                    }
                                    if(model.speakers>1)Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                        for(id in 0 until model.speakers)FilterChip(speaker==id,onClick={speakers=speakers+(model.id to id)},
                                            label={Text(s.localize(Res.string.tp_voice_number,id+1))},modifier=Modifier.testTag("voice-speaker-${model.id}-$id"))
                                    }
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
