package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import kotlin.math.roundToInt
import androidx.compose.ui.draw.scale
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

private val PlayerAccent: Color @Composable get() = MaterialTheme.colorScheme.primary
private val PlayerSurface: Color @Composable get() = MaterialTheme.colorScheme.surface
private val PlayerMuted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

internal fun listeningSpeedLabel(speed: Float): String = speed.toString().removeSuffix(".0") + "x"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFloatingPlayer(isPlaying: Boolean, speed: Float, progress: Float,
    onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    onSpeed: (Float) -> Unit, onSettings: () -> Unit, onClose: () -> Unit,
    audio: ListeningAudioState = ListeningAudioState(), onVolume: (Float) -> Unit = {},
    onMusicToggle: () -> Unit = {}, modifier: Modifier = Modifier, onMinimize: (() -> Unit)? = null) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    Surface(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp).readerPanel(22),
        shape = RoundedCornerShape(22.dp), color = Color.Transparent, contentColor = PlayerMuted,
        shadowElevation = 0.dp) {
        Column {
            LinearProgressIndicator(progress = { progress.coerceIn(0f,1f) }, color = PlayerAccent,
                trackColor = Color.Transparent, modifier = Modifier.fillMaxWidth().height(2.dp))
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal=8.dp,vertical=8.dp)) {
                val compact = maxWidth.value < 340f || maxWidth.value / LocalDensity.current.fontScale < 300f
                val playback: @Composable () -> Unit = {
                    Row(Modifier.width(if(compact) 144.dp else 156.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.SpaceEvenly) {
                        IconButton(onClick=onPrevious,Modifier.size(48.dp)) { Icon(Icons.Default.SkipPrevious,s.localize(Res.string.tp_previous_chapter),Modifier.size(22.dp)) }
                        FilledIconButton(onClick=onPlayPause,colors=IconButtonDefaults.filledIconButtonColors(containerColor=PlayerAccent,contentColor=MaterialTheme.colorScheme.onPrimary),modifier=Modifier.size(if(compact) 48.dp else 56.dp)) {
                            Icon(if(isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,s.localize(if(isPlaying) Res.string.pause else Res.string.play),Modifier.size(34.dp))
                        }
                        IconButton(onClick=onNext,Modifier.size(48.dp)) { Icon(Icons.Default.SkipNext,s.localize(Res.string.tp_next_chapter),Modifier.size(22.dp)) }
                    }
                }
                val tools: @Composable RowScope.() -> Unit = {
                    if(!compact) Surface(onClick=onMusicToggle,enabled=audio.supported,modifier=Modifier.weight(1f).wrapContentWidth(Alignment.Start).widthIn(max=104.dp).heightIn(min=48.dp),shape=CircleShape,
                        color=if(audio.musicEnabled) PlayerAccent.copy(alpha=.18f) else MaterialTheme.colorScheme.onSurface.copy(alpha=.06f)) {
                        Box(contentAlignment=Alignment.Center) { Icon(Icons.Outlined.GraphicEq,s.localize(Res.string.tp_background_music),tint=if(audio.musicEnabled) PlayerAccent else PlayerMuted) }
                    }
                    IconButton(onClick=onSettings,Modifier.size(48.dp)) { Icon(Icons.Outlined.Settings,s.localize(Res.string.tp_listening_settings)) }
                    IconButton(onClick=onMinimize ?: onClose,Modifier.size(48.dp).testTag(if(onMinimize!=null) "listen-minimize" else "listen-close")) { Icon(if(onMinimize!=null) Icons.Default.KeyboardArrowDown else Icons.Default.Close,s.localize(if(onMinimize!=null)Res.string.tp_collapse_player else Res.string.tp_close_listening)) }
                }
                val volume: @Composable () -> Unit = {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        if(compact) IconButton(onClick=onMusicToggle,enabled=audio.supported) { Icon(Icons.Outlined.GraphicEq,s.localize(Res.string.tp_background_music),tint=if(audio.musicEnabled) PlayerAccent else PlayerMuted) }
                        Icon(Icons.Outlined.VolumeUp,s.localize(Res.string.tp_speech_volume),Modifier.size(16.dp))
                        Slider(audio.speechVolume,onVolume,modifier=Modifier.weight(1f).heightIn(min=48.dp),enabled=audio.supported,
                            thumb={Box(Modifier.size(12.dp).background(PlayerAccent,CircleShape))},
                            track={SliderDefaults.Track(it,modifier=Modifier.height(3.dp),colors=SliderDefaults.colors(activeTrackColor=PlayerAccent,inactiveTrackColor=MaterialTheme.colorScheme.onSurface.copy(alpha=.2f)))})
                    }
                }
                if(compact) Column {
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) { Box(Modifier.weight(1f)) { playback() };tools() }
                    volume()
                } else Row(verticalAlignment=Alignment.CenterVertically) {
                    playback();VerticalDivider(Modifier.height(72.dp),color=MaterialTheme.colorScheme.onSurface.copy(alpha=.12f))
                    Column(Modifier.weight(1f).padding(start=8.dp)) { Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,content=tools);volume() }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f))
            val density = LocalDensity.current
            val speedScroll = rememberScrollState(initial = with(density) { 100.dp.roundToPx() })
            Row(Modifier.horizontalScroll(speedScroll).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (step in 2..12) {
                    val value = step * .25f
                    val selected = kotlin.math.abs(speed - value) < .01f
                    Surface(onClick = { onSpeed(value) }, shape = RoundedCornerShape(10.dp),
                        color = if (selected) PlayerAccent else Color.Transparent,
                        modifier = Modifier.width((48f * LocalDensity.current.fontScale.coerceAtLeast(1f)).dp).heightIn(min=48.dp).semantics { this.selected = selected }) {
                        Box(contentAlignment = Alignment.Center) { Text(listeningSpeedLabel(value), maxLines=1, softWrap=false, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else PlayerMuted) }
                    }
                }
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
    onTimer: (Boolean) -> Unit, onMinutes: (Int) -> Unit, notice: String = "") {
    val s=requireNotNull(LocalLocalizeHelper.current)
    var page by remember { mutableIntStateOf(0) }
    Surface(Modifier.fillMaxWidth().padding(horizontal=12.dp).readerPanel(20),color=Color.Transparent,shape=RoundedCornerShape(20.dp)) {
        Column {
            ReaderSettingsTabs(listOf(s.localize(Res.string.tp_settings_voice),s.localize(Res.string.tp_settings_music),s.localize(Res.string.tp_reading_tab),s.localize(Res.string.tp_settings_timer)),listOf("voice","music","reading","timer"),page,{page=it},"listen-page")
            Box(Modifier.fillMaxWidth().height(160.dp).padding(horizontal=14.dp,vertical=4.dp)) {
                when(page) {
                    0 -> Column {
                        OutlinedButton(onClick=onVoice,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("listen-voice-choice"),shape=RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.Mic,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                            Text(voice.ifBlank{s.localize(Res.string.tp_choose_voice)},Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(s.localize(Res.string.tp_pitch,listeningSpeedLabel(pitch).removeSuffix("x")),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            IconButton(onClick={onPitch(((pitch*4).roundToInt()-1).coerceIn(2,8)/4f)},enabled=pitch>.5f,modifier=Modifier.size(48.dp).testTag("listen-pitch-decrease")){Text("−")}
                            IconButton(onClick={onPitch(((pitch*4).roundToInt()+1).coerceIn(2,8)/4f)},enabled=pitch<2f,modifier=Modifier.size(48.dp).testTag("listen-pitch-increase")){Text("+")}
                        }
                        if(notice.isNotBlank())Text(notice,style=MaterialTheme.typography.labelSmall,maxLines=2,overflow=TextOverflow.Ellipsis)
                    }
                    1 -> Column {
                        OutlinedButton(onClick=onMusic,enabled=audio.supported,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp).testTag("listen-music-choice"),shape=RoundedCornerShape(12.dp)) {
                            Icon(Icons.Outlined.MusicNote,null,Modifier.size(18.dp));Spacer(Modifier.width(8.dp))
                            Text(if(audio.musicError)s.localize(Res.string.tp_music_error) else audio.musicTitle.ifBlank{s.localize(Res.string.tp_choose_music)},Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis,color=if(audio.musicError)MaterialTheme.colorScheme.error else LocalContentColor.current)
                            Icon(Icons.Default.ExpandMore,null)
                        }
                        ReaderSettingSwitch(s.localize(Res.string.tp_background_music),audio.musicEnabled,onMusicEnabled,enabled=audio.supported && audio.musicTitle.isNotBlank())
                        Row(Modifier.height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Outlined.VolumeUp,null,Modifier.size(18.dp))
                            Slider(audio.musicVolume,onMusicVolume,enabled=audio.supported,modifier=Modifier.weight(1f).semantics {contentDescription=s.localize(Res.string.tp_music_volume)})
                        }
                    }
                    2 -> Column {
                        ReaderSettingSwitch(s.localize(Res.string.tp_auto_next),autoNext,onAutoNext)
                        ReaderSettingSwitch(s.localize(Res.string.tp_highlight_paragraph),highlight,onHighlight)
                        ReaderSettingSwitch(s.localize(Res.string.tp_follow_paragraph),autoScroll,onAutoScroll)
                    }
                    else -> Column {
                        ReaderSettingSwitch(s.localize(Res.string.tp_sleep_timer),timer,onTimer)
                        Row(Modifier.fillMaxWidth().height(48.dp),verticalAlignment=Alignment.CenterVertically) {
                            Text(s.localize(Res.string.tp_minutes,minutes),Modifier.weight(1f),style=MaterialTheme.typography.bodyMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
                            IconButton(onClick={onMinutes((minutes-5).coerceAtLeast(5))},enabled=timer && minutes>5,modifier=Modifier.size(48.dp)){Text("−")}
                            IconButton(onClick={onMinutes((minutes+5).coerceAtMost(120))},enabled=timer && minutes<120,modifier=Modifier.size(48.dp)){Text("+")}
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderCollapsedPlayer(isPlaying:Boolean,onPlay:()->Unit,onExpand:()->Unit,onSettings:()->Unit,onClose:()->Unit,modifier:Modifier=Modifier) {
 val s=requireNotNull(LocalLocalizeHelper.current)
 Surface(modifier.fillMaxWidth().padding(horizontal=12.dp,vertical=8.dp).readerPanel(22).testTag("listen-collapsed"),shape=RoundedCornerShape(22.dp),color=Color.Transparent) {
  Row(Modifier.fillMaxWidth().height(56.dp).padding(horizontal=4.dp),verticalAlignment=Alignment.CenterVertically) {
   IconButton(onClick=onExpand,modifier=Modifier.size(48.dp).testTag("listen-expand")){Icon(Icons.Default.KeyboardArrowUp,s.localize(Res.string.tp_expand_player))}
   Text(s.localize(Res.string.tp_listen_book),Modifier.weight(1f),style=MaterialTheme.typography.labelMedium,maxLines=1,overflow=TextOverflow.Ellipsis)
   IconButton(onClick=onPlay,modifier=Modifier.size(48.dp).testTag("listen-play")){Icon(if(isPlaying)Icons.Default.Pause else Icons.Default.PlayArrow,s.localize(if(isPlaying)Res.string.pause else Res.string.play))}
   IconButton(onClick=onSettings,modifier=Modifier.size(48.dp).testTag("listen-settings")){Icon(Icons.Outlined.Settings,s.localize(Res.string.settings))}
   IconButton(onClick=onClose,modifier=Modifier.size(48.dp).testTag("listen-close")){Icon(Icons.Default.Close,s.localize(Res.string.close))}
  }
 }
}
