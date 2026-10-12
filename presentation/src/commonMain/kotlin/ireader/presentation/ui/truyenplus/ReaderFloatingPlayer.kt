package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalDensity
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

private val PlayerAccent = Color(0xFF6962F5)
private val PlayerSurface = Color(0xFF242528)
private val PlayerMuted = Color(0xFFA8ABC2)

internal fun listeningSpeedLabel(speed: Float): String = speed.toString().removeSuffix(".0") + "x"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderFloatingPlayer(isPlaying: Boolean, speed: Float, progress: Float,
    onPrevious: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    onSpeed: (Float) -> Unit, onSettings: () -> Unit, onClose: () -> Unit,
    audio: ListeningAudioState = ListeningAudioState(), onVolume: (Float) -> Unit = {},
    onMusicToggle: () -> Unit = {}, modifier: Modifier = Modifier) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    Surface(modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(22.dp), color = PlayerSurface, contentColor = PlayerMuted,
        border = BorderStroke(1.dp, Color.White.copy(alpha = .14f)), shadowElevation = 10.dp) {
        Column {
            LinearProgressIndicator(progress = { progress.coerceIn(0f,1f) }, color = PlayerAccent,
                trackColor = Color.Transparent, modifier = Modifier.fillMaxWidth().height(2.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.width(148.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceEvenly) {
                    IconButton(onClick = onPrevious, Modifier.size(40.dp)) { Icon(Icons.Default.SkipPrevious, s.localize(Res.string.tp_previous_chapter), Modifier.size(22.dp)) }
                    FilledIconButton(onClick = onPlayPause, colors = IconButtonDefaults.filledIconButtonColors(containerColor = PlayerAccent, contentColor = Color.White), modifier = Modifier.size(56.dp)) {
                        Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, s.localize(if (isPlaying) Res.string.pause else Res.string.play), Modifier.size(34.dp))
                    }
                    IconButton(onClick = onNext, Modifier.size(40.dp)) { Icon(Icons.Default.SkipNext, s.localize(Res.string.tp_next_chapter), Modifier.size(22.dp)) }
                }
                VerticalDivider(Modifier.height(64.dp), color = Color.White.copy(alpha = .12f))
                Column(Modifier.weight(1f).padding(start = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(onClick = onMusicToggle, enabled = audio.supported, modifier = Modifier.weight(1f).wrapContentWidth(Alignment.Start).widthIn(max = 104.dp).height(30.dp).semantics { contentDescription = s.localize(Res.string.tp_background_music) }, shape = CircleShape,
                            color = if (audio.musicEnabled) Color(0xFFE8B442).copy(alpha = .18f) else Color.White.copy(alpha = .06f),
                            border = BorderStroke(1.dp, if (audio.musicEnabled) Color(0xFFE8B442).copy(alpha = .55f) else Color.White.copy(alpha = .12f))) {
                            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.GraphicEq, null, tint = if (audio.musicEnabled) Color(0xFFE8B442) else PlayerMuted, modifier = Modifier.size(18.dp)) }
                        }
                        IconButton(onClick = onSettings, Modifier.size(36.dp)) { Icon(Icons.Outlined.Settings, s.localize(Res.string.tp_listening_settings), Modifier.size(20.dp)) }
                        IconButton(onClick = onClose, Modifier.size(36.dp)) { Icon(Icons.Default.Close, s.localize(Res.string.tp_close_listening), Modifier.size(20.dp)) }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.VolumeUp, s.localize(Res.string.tp_speech_volume), Modifier.size(16.dp))
                        Slider(audio.speechVolume, onVolume, modifier = Modifier.weight(1f).height(32.dp), enabled = audio.supported,
                            thumb = { Box(Modifier.size(12.dp).background(PlayerAccent, CircleShape)) },
                            track = { SliderDefaults.Track(it, modifier = Modifier.height(3.dp), colors = SliderDefaults.colors(activeTrackColor = PlayerAccent, inactiveTrackColor = Color.White.copy(alpha = .2f))) })
                    }
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = Color.White.copy(alpha = .1f))
            val density = LocalDensity.current
            val speedScroll = rememberScrollState(initial = with(density) { 100.dp.roundToPx() })
            Row(Modifier.horizontalScroll(speedScroll).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                for (step in 2..12) {
                    val value = step * .25f
                    val selected = kotlin.math.abs(speed - value) < .01f
                    Surface(onClick = { onSpeed(value) }, shape = RoundedCornerShape(10.dp),
                        color = if (selected) PlayerAccent else Color.Transparent,
                        modifier = Modifier.width(48.dp).height(38.dp).semantics { this.selected = selected }) {
                        Box(contentAlignment = Alignment.Center) { Text(listeningSpeedLabel(value), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (selected) Color.White else PlayerMuted) }
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
    onTimer: (Boolean) -> Unit, onMinutes: (Int) -> Unit) {
    val s = requireNotNull(LocalLocalizeHelper.current)
    Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp).heightIn(max = 380.dp), color = PlayerSurface,
        contentColor = PlayerMuted, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .14f))) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(14.dp)) {
            Text(s.localize(Res.string.tp_choose_voice), style = MaterialTheme.typography.labelMedium)
            OutlinedButton(onClick = onVoice, Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(10.dp)) {
                Icon(Icons.Outlined.Mic, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                Text(voice.ifBlank { s.localize(Res.string.tp_choose_voice) }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                Icon(Icons.Default.ExpandMore, null)
            }
            if (audio.supported) {
                HorizontalDivider(Modifier.padding(vertical = 10.dp), color = Color.White.copy(alpha = .1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(s.localize(Res.string.tp_background_music), Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
                    Switch(audio.musicEnabled, onMusicEnabled, enabled = audio.musicTitle.isNotBlank(), modifier = Modifier.scale(.8f), colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PlayerAccent))
                }
                OutlinedButton(onClick = onMusic, Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Outlined.MusicNote, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                    Text(audio.musicTitle.ifBlank { s.localize(Res.string.tp_choose_music) }, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White)
                    Icon(Icons.Default.ExpandMore, null)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.VolumeUp, null, Modifier.size(16.dp))
                    Slider(audio.musicVolume, onMusicVolume, modifier = Modifier.weight(1f).height(32.dp).semantics { contentDescription = s.localize(Res.string.tp_music_volume) },
                        thumb = { Box(Modifier.size(12.dp).background(PlayerAccent, CircleShape)) },
                        track = { SliderDefaults.Track(it, modifier = Modifier.height(3.dp), colors = SliderDefaults.colors(activeTrackColor = PlayerAccent, inactiveTrackColor = Color.White.copy(alpha = .2f))) })
                }
                if (audio.musicError) Text(s.localize(Res.string.tp_music_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            ToggleOption(s.localize(Res.string.tp_auto_next), autoNext, onAutoNext)
            ToggleOption(s.localize(Res.string.tp_highlight_paragraph), highlight, onHighlight)
            ToggleOption(s.localize(Res.string.tp_follow_paragraph), autoScroll, onAutoScroll)
            Text(s.localize(Res.string.tp_pitch, pitch), Modifier.padding(top = 8.dp))
            Slider(pitch.coerceIn(.5f,2f), onPitch, valueRange = .5f..2f, steps = 5)
            ToggleOption(s.localize(Res.string.tp_sleep_timer), timer, onTimer)
            if (timer) {
                Text(s.localize(Res.string.tp_minutes, minutes))
                Slider(minutes.coerceIn(5,120).toFloat(), { onMinutes(it.toInt()) }, valueRange = 5f..120f)
            }
        }
    }
}

@Composable private fun ToggleOption(label: String, checked: Boolean, change: (Boolean) -> Unit) {
    HorizontalDivider(color = Color.White.copy(alpha = .1f))
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = Color.White, style = MaterialTheme.typography.bodyMedium)
        Switch(checked, change, colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PlayerAccent), modifier = Modifier.scale(.8f).semantics { contentDescription = label })
    }
}
