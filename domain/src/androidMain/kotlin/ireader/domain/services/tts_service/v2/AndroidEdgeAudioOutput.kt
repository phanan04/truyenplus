package ireader.domain.services.tts_service.v2

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** Asynchronous preparation; owns only one temporary MP3 and ignores callbacks from replaced players. */
class AndroidEdgeAudioOutput(private val context: Context) : EdgeAudioOutput {
    private var player: MediaPlayer? = null
    private var file: File? = null
    private var generation = 0
    private var ready = false
    private var paused = false
    private var released = false
    private var speed = 1f
    private var pitch = 1f
    private var failure: (() -> Unit)? = null

    override suspend fun play(bytes: ByteArray, onStarted: () -> Unit, onCompleted: () -> Unit, onError: () -> Unit) {
        stop()
        if (released) return
        val token = generation
        var pendingFile: File? = null
        try {
            val temporary = withContext(Dispatchers.IO) {
                File.createTempFile("edge-", ".mp3", context.cacheDir).also { created ->
                    pendingFile = created
                    created.writeBytes(bytes)
                    ensureActive()
                }
            }
            withContext(Dispatchers.Main.immediate) {
                if (token != generation || released) { temporary.delete(); return@withContext }
                file = temporary
                val created = MediaPlayer()
                player = created
                failure = onError
                created.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                created.setOnPreparedListener {
                    if (player !== created || token != generation || released) return@setOnPreparedListener
                    ready = true
                    if (!paused) runCatching { applyParameters(created); created.start(); onStarted() }
                        .onFailure { stop(); onError() }
                }
                created.setOnCompletionListener {
                    if (player === created && token == generation && !released) { stop(); onCompleted() }
                }
                created.setOnErrorListener { _, _, _ ->
                    if (player === created && token == generation && !released) { stop(); onError() }
                    true
                }
                created.setDataSource(temporary.absolutePath)
                created.prepareAsync()
            }
        } catch (error: Throwable) {
            pendingFile?.delete()
            withContext(kotlinx.coroutines.NonCancellable + Dispatchers.Main.immediate) {
                if (token == generation) stop()
            }
            throw error
        }
    }

    private fun applyParameters(current: MediaPlayer) {
        current.playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitch)
        if (paused) current.pause()
    }
    private fun updateParameters() {
        val current = player ?: return
        if (!ready) return
        runCatching { applyParameters(current) }.onFailure { val callback = failure; stop(); callback?.invoke() }
    }
    override fun pause() { paused = true; if (ready) runCatching { player?.pause() } }
    override fun resume() {
        paused = false
        if (ready) runCatching { player?.let { applyParameters(it); it.start() } }
            .onFailure { val callback = failure; stop(); callback?.invoke() }
    }
    override fun stop() {
        generation++
        val previous = player
        player = null; ready = false; paused = false; failure = null
        runCatching { previous?.release() }
        file?.delete(); file = null
    }
    override fun release() { stop(); released = true }
    override fun setSpeed(speed: Float) { this.speed = speed.coerceIn(.5f, 3f); updateParameters() }
    override fun setPitch(pitch: Float) { this.pitch = pitch.coerceIn(.5f, 2f); updateParameters() }
}
