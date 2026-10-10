package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.presentation.ui.tts.components.SpotifyLikeTTSPlayer
import ireader.presentation.ui.home.tts.CommonTTSScreenState
import ireader.presentation.ui.home.tts.CommonTTSActions
import ireader.presentation.ui.home.tts.TTSContentDisplay

/** Full original chapter content, selectable by paragraph, with playback controls always visible. */
@Composable
fun ReaderListenChapterScreen(state: CommonTTSScreenState, actions: CommonTTSActions, onSettings: () -> Unit,
    lazyListState: LazyListState = rememberLazyListState(),
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    textColor: Color = MaterialTheme.colorScheme.onBackground, fontSize: Int = 18,
    textAlignment: TextAlign = TextAlign.Start, isTabletOrDesktop: Boolean = false) {
    Surface(color = backgroundColor, contentColor = textColor, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Text(state.bookTitle, style = MaterialTheme.typography.labelMedium, color = textColor.copy(alpha = .65f))
                Text(state.chapterName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
                Text(if (state.isPlaying) "Chạm vào đoạn để chuyển chỗ nghe" else "Chọn một đoạn, rồi nhấn phát", style = MaterialTheme.typography.bodySmall, color = textColor.copy(alpha = .7f), modifier = Modifier.padding(top = 8.dp))
            }
            TTSContentDisplay(state = state, actions = actions, lazyListState = lazyListState,
                backgroundColor = backgroundColor, textColor = textColor,
                currentParagraphColor = textColor, otherTextColor = textColor.copy(alpha = .8f),
                currentParagraphHighlightColor = textColor.copy(alpha = .1f),
                highlightColor = MaterialTheme.colorScheme.primaryContainer,
                fontSize = fontSize, lineHeight = fontSize + 10, textAlignment = textAlignment,
                isTabletOrDesktop = isTabletOrDesktop, modifier = Modifier.weight(1f))
            SpotifyLikeTTSPlayer(state.isPlaying, actions::onPreviousChapter, actions::onPreviousParagraph,
                { if (state.isPlaying) actions.onPause() else actions.onPlay() },
                actions::onNextParagraph, actions::onNextChapter, onSettings,
                progressFraction = state.progressFraction, currentParagraph = state.currentReadingParagraph,
                totalParagraphs = state.contentSize)
        }
    }
}

@Composable
fun ReaderNowPlayingScreen(book: ReaderBookUi, chapter: String, paragraph: String, isPlaying: Boolean,
    progress: Float, paragraphIndex: Int, paragraphCount: Int, speed: Float,
    onPreviousChapter: () -> Unit, onPrevious: () -> Unit, onPlayPause: () -> Unit,
    onNext: () -> Unit, onNextChapter: () -> Unit, onSettings: () -> Unit, onShowText: () -> Unit) {
    ReaderBackdrop {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                ReaderCover(book, Modifier.widthIn(max = 198.dp).fillMaxWidth().height(270.dp))
                Text(book.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(top = 26.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(chapter, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (paragraph.isNotBlank()) {
                    Column(Modifier.fillMaxWidth().clickable(onClick = onShowText).padding(top = 24.dp)) {
                        Text(paragraph, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .85f), maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Text("Mở toàn bộ chương · Chọn đoạn nghe", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            SpotifyLikeTTSPlayer(isPlaying, onPreviousChapter, onPrevious, onPlayPause, onNext, onNextChapter, onSettings,
                progressFraction = progress, currentParagraph = paragraphIndex, totalParagraphs = paragraphCount)
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onSettings) { Icon(Icons.Outlined.Speed, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("${speed}×") }
                TextButton(onClick = onShowText) { Icon(Icons.Outlined.Subject, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Nội dung") }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Timer, "Hẹn giờ tắt") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderListeningSettings(drawerState: DrawerState, speed: Float, pitch: Float, timer: Boolean, minutes: Int,
    onSpeed: (Float) -> Unit, onPitch: (Float) -> Unit, onTimer: (Boolean) -> Unit,
    onMinutes: (Int) -> Unit, onVoice: () -> Unit, onClose: () -> Unit, content: @Composable () -> Unit) {
    ModalNavigationDrawer(drawerState = drawerState, drawerContent = {
        ModalDrawerSheet(Modifier.width(320.dp)) {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp)) {
                Text("Tùy chỉnh nghe", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                TextButton(onClick = onVoice, modifier = Modifier.padding(top = 16.dp)) { Icon(Icons.Outlined.RecordVoiceOver, null); Spacer(Modifier.width(8.dp)); Text("Chọn giọng đọc") }
                Text("Tốc độ · ${speed}×", modifier = Modifier.padding(top = 20.dp)); Slider(speed.coerceIn(.5f, 3f), onSpeed, valueRange = .5f..3f)
                Text("Cao độ · ${pitch}×", modifier = Modifier.padding(top = 20.dp)); Slider(pitch.coerceIn(.5f, 2f), onPitch, valueRange = .5f..2f)
                Row(Modifier.fillMaxWidth().padding(top = 20.dp), verticalAlignment = Alignment.CenterVertically) { Text("Hẹn giờ tắt", Modifier.weight(1f)); Switch(timer, onTimer) }
                if (timer) {
                    Text("$minutes phút", modifier = Modifier.padding(top = 12.dp))
                    Slider(minutes.coerceIn(5, 120).toFloat(), { onMinutes(it.toInt()) }, valueRange = 5f..120f)
                }
                Button(onClick = onClose, shape = CircleShape, modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) { Text("Xong") }
            }
        }
    }, content = content)
}
