package ireader.presentation.ui.truyenplus

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import ireader.domain.services.tts_service.v2.TTSState
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.roundToInt

/** Application-owned player follows the TTS service even when the reader is closed. */
internal class AndroidListeningAudio(context: Context, playback: StateFlow<TTSState>,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)) : ListeningAudio {
    private val context = context.applicationContext
    private val preferences = this.context.getSharedPreferences("truyenplus_listening_music", Context.MODE_PRIVATE)
    private val manager = this.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var uri = preferences.getString("uri", "").orEmpty()
    private val mutableState = MutableStateFlow(ListeningAudioState(supported = true,
        speechVolume = currentVolume(), musicTitle = preferences.getString("title", "").orEmpty(),
        musicVolume = preferences.getFloat("volume", .2f).coerceIn(0f,1f),
        musicEnabled = uri.isNotBlank() && preferences.getBoolean("enabled", false)))
    override val state = mutableState.asStateFlow()
    private var player: MediaPlayer? = null
    private var ready = false
    private var active = false
    var launchPicker: (() -> Unit)? = null

    init {
        scope.launch {
            playback.map { it.playbackState }.distinctUntilChanged().collect {
                active = keepListeningMusicPlaying(active, it)
                synchronize()
            }
        }
    }
    private fun currentVolume(): Float {
        val max = manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return if (max > 0) manager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()/max else 0f
    }
    fun refreshVolume() { mutableState.update { it.copy(speechVolume = currentVolume()) } }
    override fun speechVolume(value: Float) {
        manager.setStreamVolume(AudioManager.STREAM_MUSIC, (value.coerceIn(0f,1f)*manager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).roundToInt(), 0)
        refreshVolume()
    }
    override fun chooseMusic() { launchPicker?.invoke() }
    fun selectMusic(value: String, title: String) {
        releasePlayer()
        uri = value
        preferences.edit().putString("uri", value).putString("title", title).putBoolean("enabled", true).apply()
        mutableState.update { it.copy(musicTitle = title, musicEnabled = true, musicError = false) }
        synchronize()
    }
    override fun musicEnabled(value: Boolean) {
        val enabled = value && uri.isNotBlank()
        preferences.edit().putBoolean("enabled", enabled).apply()
        mutableState.update { it.copy(musicEnabled = enabled, musicError = false) }
        if (!enabled) releasePlayer() else synchronize()
    }
    override fun musicVolume(value: Float) {
        val volume = value.coerceIn(0f,1f)
        preferences.edit().putFloat("volume", volume).apply()
        mutableState.update { it.copy(musicVolume = volume) }
        if (ready) runCatching { player?.setVolume(volume, volume) }.onFailure { fail() }
    }
    fun fail() {
        releasePlayer()
        preferences.edit().putBoolean("enabled", false).apply()
        mutableState.update { it.copy(musicError = true, musicEnabled = false) }
    }
    private fun synchronize() {
        if (!state.value.musicEnabled || !active) {
            if (ready) runCatching { if (player?.isPlaying == true) player?.pause() }.onFailure { fail() }
            return
        }
        if (player == null && uri.isNotBlank()) {
            val created = MediaPlayer()
            player = created
            runCatching {
                created.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                created.isLooping = true
                created.setOnPreparedListener {
                    if (player === created) {
                        ready = true
                        runCatching { created.setVolume(state.value.musicVolume, state.value.musicVolume) }.onFailure { fail() }
                        synchronize()
                    }
                }
                created.setOnErrorListener { _, _, _ ->
                    if (player === created) fail()
                    true
                }
                created.setDataSource(context, Uri.parse(uri))
                created.prepareAsync()
            }.onFailure { fail() }
        } else if (ready) {
            runCatching { if (player?.isPlaying != true) player?.start() }.onFailure { fail() }
        }
    }
    private fun releasePlayer() { val old = player; player = null; ready = false; runCatching { old?.release() } }
    fun close() { releasePlayer(); scope.cancel() }
}
