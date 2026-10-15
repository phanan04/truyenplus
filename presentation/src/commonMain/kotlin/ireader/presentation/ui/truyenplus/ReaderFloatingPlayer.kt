package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper

internal fun listeningSpeedLabel(speed: Float): String = speed.toString().removeSuffix(".0") + "x"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFloatingPlayer(isPlaying: Boolean, speed: Float, progress: Float,
    onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    onSpeed: (Float) -> Unit, onSettings: () -> Unit, onClose: () -> Unit,
    audio: ListeningAudioState = ListeningAudioState(), onVolume: (Float) -> Unit = {},
    onMusicToggle: () -> Unit = {}, modifier: Modifier = Modifier, onMinimize: (() -> Unit)? = null,
    voiceLabel:String="",chapterLabel:String="",paragraphIndex:Int=0,paragraphCount:Int=0,isLoading:Boolean=false,canPrevious:Boolean=true,canNext:Boolean=true) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    val colors=MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp).readerPanel(26).clickable(enabled=onMinimize!=null,interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null) { onMinimize?.invoke() }.testTag("listen-expanded-toggle"),
        shape=RoundedCornerShape(26.dp),color=Color.Transparent,contentColor=colors.onSurface) {
        Column(Modifier.padding(horizontal=12.dp,vertical=4.dp)) {
            Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.Headphones,null,Modifier.size(20.dp))
                Column(Modifier.weight(1f)) {
                    Text(voiceLabel.ifBlank{s.localize(Res.string.tp_system_voice)},style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                    if(chapterLabel.isNotBlank())Text(chapterLabel,style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick=onSettings,Modifier.size(48.dp).testTag("listen-settings")){Icon(Icons.Outlined.Settings,s.localize(Res.string.tp_listening_settings))}
                IconButton(onClick=onMinimize ?: onClose,Modifier.size(48.dp).testTag(if(onMinimize!=null)"listen-minimize" else "listen-close")) {
                    Icon(if(onMinimize!=null)Icons.Default.KeyboardArrowDown else Icons.Default.Close,s.localize(if(onMinimize!=null)Res.string.tp_collapse_player else Res.string.tp_close_listening))
                }
            }
            LinearProgressIndicator(progress={progress.coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(3.dp).semantics {contentDescription=s.localize(Res.string.tp_progress)},
                color=colors.onSurface,trackColor=colors.onSurface.copy(alpha=.15f),drawStopIndicator={})
            if(paragraphCount>0)Text(s.localize(Res.string.tp_listening_paragraph,(paragraphIndex+1).coerceIn(1,paragraphCount),paragraphCount),
                Modifier.padding(top=6.dp),style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().height(84.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    IconButton(onClick=onPrevious,enabled=canPrevious,modifier=Modifier.size(48.dp).testTag("listen-previous-chapter")){Icon(Icons.Default.SkipPrevious,s.localize(Res.string.tp_previous_chapter))}
                    Text(s.localize(Res.string.tp_listening_previous),style=MaterialTheme.typography.labelSmall,maxLines=1)
                }
                FilledIconButton(onClick=onPlayPause,modifier=Modifier.size(56.dp).testTag("listen-play").semantics { if(isLoading)contentDescription=s.localize(Res.string.tp_listening_preparing) },colors=IconButtonDefaults.filledIconButtonColors(containerColor=colors.onSurface,contentColor=colors.surface)) {
                    if(isLoading)CircularProgressIndicator(Modifier.size(24.dp),strokeWidth=2.dp,color=colors.surface)
                    else Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,s.localize(if(isPlaying)Res.string.pause else Res.string.play),Modifier.size(30.dp))
                }
                Column(horizontalAlignment=Alignment.CenterHorizontally) {
                    IconButton(onClick=onNext,enabled=canNext,modifier=Modifier.size(48.dp).testTag("listen-next-chapter")){Icon(Icons.Default.SkipNext,s.localize(Res.string.tp_next_chapter))}
                    Text(s.localize(Res.string.tp_listening_next),style=MaterialTheme.typography.labelSmall,maxLines=1)
                }
            }
            ReaderListeningSpeed(speed,onSpeed)

        }
    }
}

/** Always one row: preserve readable labels and 48dp targets with an adjuster on narrow screens. */
@Composable
internal fun ReaderListeningSpeed(speed:Float,onSpeed:(Float)->Unit) {
    val colors=MaterialTheme.colorScheme
    val fontScale=LocalDensity.current.fontScale.coerceAtLeast(1f)
    val step=(speed*4).roundToInt().coerceIn(2,8)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(vertical=4.dp)) {
        val presetsFit=maxWidth.value>=7*48f*fontScale+6*4f
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp),verticalAlignment=Alignment.CenterVertically) {
            if(presetsFit) {
                for(value in 2..8) {
                    val selected=step==value
                    Surface(onClick={onSpeed(value*.25f)},shape=RoundedCornerShape(14.dp),
                        color=if(selected)colors.onSurface else colors.onSurface.copy(alpha=.04f),
                        contentColor=if(selected)colors.surface else colors.onSurface,
                        modifier=Modifier.weight(1f).height(48.dp).testTag("listen-speed-$value").semantics {this.selected=selected}) {
                        Box(contentAlignment=Alignment.Center) {
                            Text(listeningSpeedLabel(value*.25f),fontSize=12.sp,maxLines=1,softWrap=false,
                                fontWeight=if(selected)FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            } else {
                OutlinedButton(onClick={onSpeed((step-1)*.25f)},enabled=step>2,
                    modifier=Modifier.weight(1f).height(48.dp).testTag("listen-speed-decrease"),shape=RoundedCornerShape(14.dp)) {Text("−")}
                Surface(modifier=Modifier.weight(1f).height(48.dp).testTag("listen-speed-value"),
                    shape=RoundedCornerShape(14.dp),color=colors.onSurface,contentColor=colors.surface) {
                    Box(contentAlignment=Alignment.Center) {Text(listeningSpeedLabel(step*.25f),fontSize=12.sp,maxLines=1,softWrap=false,fontWeight=FontWeight.SemiBold)}
                }
                OutlinedButton(onClick={onSpeed((step+1)*.25f)},enabled=step<8,
                    modifier=Modifier.weight(1f).height(48.dp).testTag("listen-speed-increase"),shape=RoundedCornerShape(14.dp)) {Text("+")}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFloatingOptions(voice: String, pitch: Float, autoNext: Boolean, highlight: Boolean, autoScroll: Boolean,
    timer: Boolean, minutes: Int, audio: ListeningAudioState,
    onVoice: () -> Unit, onMusic: () -> Unit, onMusicEnabled: (Boolean) -> Unit, onMusicVolume: (Float) -> Unit,
    onPitch: (Float) -> Unit, onAutoNext: (Boolean) -> Unit, onHighlight: (Boolean) -> Unit, onAutoScroll: (Boolean) -> Unit,
    onTimer: (Boolean) -> Unit, onMinutes: (Int) -> Unit, notice: String = "", onDismiss:(()->Unit)?=null,onSpeechVolume:(Float)->Unit={},timerRemaining:String="",timerPaused:Boolean=false,
    highlightWords:Boolean=false,onHighlightWords:(Boolean)->Unit={},wordTiming:ReaderWordTiming=ReaderWordTiming.Conditional) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var page by remember { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp).readerPanel(20),color=Color.Transparent,contentColor=MaterialTheme.colorScheme.onSurface,shape=RoundedCornerShape(20.dp)) {
        Column {
            Row(Modifier.fillMaxWidth().height(48.dp).padding(start=18.dp,end=8.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(s.localize(Res.string.tp_listening_settings),Modifier.weight(1f),style=MaterialTheme.typography.titleMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(onDismiss!=null)IconButton(onClick=onDismiss,modifier=Modifier.size(48.dp).testTag("listen-options-close")){Icon(Icons.Default.Close,s.localize(Res.string.close))}
            }
            ReaderSettingsTabs(listOf(s.localize(Res.string.tp_settings_voice),s.localize(Res.string.tp_settings_music),s.localize(Res.string.tp_reading_tab),s.localize(Res.string.tp_settings_timer)),listOf("voice","music","reading","timer"),page,{page=it},"listen-page")
            BoxWithConstraints(Modifier.fillMaxWidth().height(208.dp).padding(horizontal=14.dp,vertical=4.dp)) {
                val wideShort=maxWidth>=400.dp && maxHeight<184.dp
                val speechVolume:@Composable ()->Unit={
                    Column {
                        if(!wideShort && wordTiming==ReaderWordTiming.Reported)Text(s.localize(Res.string.tp_speech_volume),style=MaterialTheme.typography.labelSmall,maxLines=1)
                        Row(Modifier.height(56.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VolumeDown,null,Modifier.size(18.dp))
                            Slider(audio.speechVolume.coerceIn(0f,1f),onSpeechVolume,modifier=Modifier.weight(1f).height(52.dp).testTag("listen-speech-volume").semantics {contentDescription=s.localize(Res.string.tp_speech_volume)})
                            Icon(Icons.Outlined.VolumeUp,null,Modifier.size(18.dp))
                        }
                    }
                }
                when(page) {
                    0 -> Column {
                        val voiceControls:@Composable ()->Unit={
                        OutlinedButton(onClick=onVoice,modifier=Modifier.fillMaxWidth().height(48.dp).testTag("listen-voice-choice"),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),shape=RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.Mic,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                            Text(voice.ifBlank{s.localize(Res.string.tp_choose_voice)},Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(if(wideShort)listeningSpeedLabel(pitch) else s.localize(Res.string.tp_pitch,listeningSpeedLabel(pitch).removeSuffix("x")),Modifier.weight(1f).semantics {contentDescription=s.localize(Res.string.tp_pitch,listeningSpeedLabel(pitch).removeSuffix("x"))},style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            IconButton(onClick={onPitch(((pitch*4).roundToInt()-1).coerceIn(2,8)/4f)},enabled=pitch>.5f,modifier=Modifier.size(48.dp).testTag("listen-pitch-decrease")){Text("−")}
                            IconButton(onClick={onPitch(((pitch*4).roundToInt()+1).coerceIn(2,8)/4f)},enabled=pitch<2f,modifier=Modifier.size(48.dp).testTag("listen-pitch-increase")){Text("+")}
                        }
                        }
                        if(wideShort)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Column(Modifier.weight(1f)){voiceControls()};Box(Modifier.weight(1f)){speechVolume()}}
                        else Column {voiceControls();speechVolume()}
                        if(wordTiming!=ReaderWordTiming.Reported)Text(s.localize(if(wordTiming==ReaderWordTiming.SentenceOnly)Res.string.tp_words_unavailable else Res.string.tp_words_system_hint),
                            style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    1 -> {
                        val musicChoice:@Composable ()->Unit={
                        OutlinedButton(onClick=onMusic,enabled=audio.supported,modifier=Modifier.fillMaxWidth().height(48.dp).testTag("listen-music-choice"),contentPadding=PaddingValues(horizontal=12.dp,vertical=4.dp),shape=RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.MusicNote,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                            Text(if(audio.musicError)s.localize(Res.string.tp_music_error) else audio.musicTitle.ifBlank{s.localize(Res.string.tp_choose_music)},Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,color=if(audio.musicError)MaterialTheme.colorScheme.error else LocalContentColor.current)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        ReaderSettingSwitch(s.localize(Res.string.tp_background_music),audio.musicEnabled,onMusicEnabled,enabled=audio.supported && audio.musicTitle.isNotBlank())
                        }
                        val musicVolume:@Composable ()->Unit={
                        Row(Modifier.height(56.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VolumeUp,null,Modifier.size(18.dp))
                            Slider(audio.musicVolume,onMusicVolume,enabled=audio.supported,modifier=Modifier.weight(1f).height(52.dp).semantics {contentDescription=s.localize(Res.string.tp_music_volume)})
                        }
                        }
                        if(wideShort)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Column(Modifier.weight(1f)){musicChoice()};Box(Modifier.weight(1f)){musicVolume()}}
                        else Column {musicChoice();musicVolume()}
                    }
                    2 -> {
                        val highlightStyles:@Composable ()->Unit={
                            ReaderSettingsTabs(listOf(s.localize(Res.string.tp_highlight_sentence),s.localize(Res.string.tp_highlight_words)),
                                listOf("sentence","words"),if(highlightWords && wordTiming!=ReaderWordTiming.SentenceOnly)1 else 0,
                                {onHighlightWords(it==1)},"listen-highlight",listOf(true,wordTiming!=ReaderWordTiming.SentenceOnly))
                        }
                        val followChoices:@Composable ()->Unit={
                        ReaderSettingSwitch(s.localize(Res.string.tp_auto_next),autoNext,onAutoNext)
                        ReaderSettingSwitch(s.localize(Res.string.tp_highlight_paragraph),highlight,onHighlight)
                        }
                        if(wideShort)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)){Column(Modifier.weight(1f)){followChoices()};Column(Modifier.weight(1f)){ReaderSettingSwitch(s.localize(Res.string.tp_follow_paragraph),autoScroll,onAutoScroll);highlightStyles()}}
                        else Column {followChoices();ReaderSettingSwitch(s.localize(Res.string.tp_follow_paragraph),autoScroll,onAutoScroll);highlightStyles()}
                    }
                    else -> {
                        val switch:@Composable ()->Unit={ReaderSettingSwitch(s.localize(Res.string.tp_sleep_timer),timer,onTimer)}
                        val remaining:@Composable ()->Unit={
                            if(timer && timerRemaining.isNotBlank())Text(s.localize(Res.string.tp_timer_remaining,timerRemaining)+
                                if(timerPaused)" · "+s.localize(Res.string.tp_listening_paused) else "",style=MaterialTheme.typography.labelSmall,maxLines=1,overflow=TextOverflow.Ellipsis)
                        }
                        val minutesControl:@Composable ()->Unit={
                            Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                                Text(s.localize(Res.string.tp_minutes,minutes),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                                IconButton(onClick={onMinutes((minutes-5).coerceAtLeast(5))},enabled=timer && minutes>5,modifier=Modifier.size(48.dp)){Text("−")}
                                IconButton(onClick={onMinutes((minutes+5).coerceAtMost(120))},enabled=timer && minutes<120,modifier=Modifier.size(48.dp)){Text("+")}
                            }
                        }
                        val presets:@Composable ()->Unit={
                            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)) {
                                for(value in listOf(15,30,45,60)) Surface(onClick={onMinutes(value);if(!timer)onTimer(true)},
                                    shape=RoundedCornerShape(12.dp),
                                    color=if(timer && minutes==value)MaterialTheme.colorScheme.onSurface.copy(alpha=.1f) else Color.Transparent,
                                    modifier=Modifier.weight(1f).height(48.dp).testTag("listen-timer-$value").semantics {
                                        selected=timer && minutes==value;contentDescription=s.localize(Res.string.tp_minutes,value)
                                    }) {
                                    Box(contentAlignment=Alignment.Center){Text(value.toString(),style=MaterialTheme.typography.labelLarge,maxLines=1)}
                                }
                            }
                        }
                        if(wideShort)Row(horizontalArrangement=Arrangement.spacedBy(16.dp)) {
                            Column(Modifier.weight(1f)){switch();remaining()};Column(Modifier.weight(1f)){minutesControl();presets()}
                        } else Column {switch();minutesControl();presets();remaining()}
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderCollapsedPlayer(isPlaying:Boolean,onPlay:()->Unit,onExpand:()->Unit,onSettings:()->Unit,onClose:()->Unit,
    modifier:Modifier=Modifier,voiceLabel:String="",isLoading:Boolean=false,
    onPrevious:(()->Unit)?=null,onNext:(()->Unit)?=null,canPrevious:Boolean=true,canNext:Boolean=true) {
    val s=requireNotNull(LocalLocalizeHelper.current)
    val colors=MaterialTheme.colorScheme
    Surface(modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp).readerPanel(22)
        .clickable(interactionSource=remember { androidx.compose.foundation.interaction.MutableInteractionSource() },indication=null,onClick=onExpand)
        .testTag("listen-compact-toggle"),shape=RoundedCornerShape(22.dp),color=Color.Transparent,contentColor=colors.onSurface) {
        BoxWithConstraints(Modifier.testTag("listen-collapsed").padding(horizontal=4.dp)) {
            val chapterButtons=onPrevious!=null && onNext!=null
            val narrow=chapterButtons && maxWidth.value/LocalDensity.current.fontScale<300f
            val play:@Composable ()->Unit={
                FilledIconButton(onClick=onPlay,modifier=Modifier.size(48.dp).testTag("listen-play")
                    .semantics {if(isLoading)contentDescription=s.localize(Res.string.tp_listening_preparing)},
                    colors=IconButtonDefaults.filledIconButtonColors(containerColor=colors.onSurface,contentColor=colors.surface)) {
                    if(isLoading)CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=colors.surface)
                    else Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,s.localize(if(isPlaying)Res.string.pause else Res.string.play))
                }
            }
            val previous:@Composable ()->Unit={IconButton(onClick={onPrevious?.invoke()},enabled=canPrevious,modifier=Modifier.size(48.dp).testTag("listen-compact-previous")) {Icon(Icons.Default.SkipPrevious,s.localize(Res.string.tp_previous_chapter))}}
            val next:@Composable ()->Unit={IconButton(onClick={onNext?.invoke()},enabled=canNext,modifier=Modifier.size(48.dp).testTag("listen-compact-next")) {Icon(Icons.Default.SkipNext,s.localize(Res.string.tp_next_chapter))}}
            val settings:@Composable ()->Unit={IconButton(onClick=onSettings,modifier=Modifier.size(48.dp).testTag("listen-settings")) {Icon(Icons.Outlined.Settings,s.localize(Res.string.settings))}}
            val close:@Composable ()->Unit={IconButton(onClick=onClose,modifier=Modifier.size(48.dp).testTag("listen-close")) {Icon(Icons.Default.Close,s.localize(Res.string.tp_close_listening))}}
            val expand:@Composable ()->Unit={IconButton(onClick=onExpand,modifier=Modifier.size(48.dp).testTag("listen-expand")) {Icon(Icons.Default.KeyboardArrowUp,s.localize(Res.string.tp_expand_player))}}
            val label:@Composable ()->Unit={
                Text(s.localize(if(isLoading)Res.string.tp_listening_preparing else if(isPlaying)Res.string.tp_listening_active else Res.string.tp_listening_paused),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                if(LocalDensity.current.fontScale<1.5f)Text(voiceLabel.ifBlank{s.localize(Res.string.tp_system_voice)},style=MaterialTheme.typography.labelSmall,color=colors.onSurfaceVariant,maxLines=1,overflow=TextOverflow.Ellipsis)
            }
            if(narrow) Column {
                Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {expand();Column(Modifier.weight(1f)){label()};close()}
                Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                    Row(Modifier.weight(1f),horizontalArrangement=Arrangement.Center,verticalAlignment=Alignment.CenterVertically) {previous();play();next()}
                    settings()
                }
            } else Row(Modifier.fillMaxWidth().height(56.dp),verticalAlignment=Alignment.CenterVertically) {
                if(!chapterButtons)expand()
                Column(Modifier.weight(1f).padding(start=12.dp).height(48.dp).clickable(onClick=onExpand).testTag(if(chapterButtons)"listen-expand" else "listen-status"),verticalArrangement=Arrangement.Center) {label()}
                if(chapterButtons)previous();play();if(chapterButtons)next();settings();close()
            }
        }
    }
}
