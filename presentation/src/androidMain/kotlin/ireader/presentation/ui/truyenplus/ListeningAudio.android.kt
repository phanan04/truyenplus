package ireader.presentation.ui.truyenplus

import android.content.Intent
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ireader.domain.services.tts_service.v2.TTSViewModelAdapter

private object ListeningAudioOwner { var player: AndroidListeningAudio? = null }

@Composable actual fun rememberListeningAudio(adapter: TTSViewModelAdapter): ListeningAudio {
    val context = LocalContext.current.applicationContext
    val player = remember { ListeningAudioOwner.player ?: AndroidListeningAudio(context, adapter.state).also { ListeningAudioOwner.player = it } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val name = context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)) else null
                } ?: uri.lastPathSegment.orEmpty()
                player.selectMusic(uri.toString(), name.ifBlank { "Audio" })
            }.onFailure { player.fail() }
        }
    }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(player, launcher, owner) {
        player.launchPicker = { launcher.launch(arrayOf("audio/*")) }
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) player.refreshVolume() }
        owner.lifecycle.addObserver(observer)
        player.refreshVolume()
        onDispose { owner.lifecycle.removeObserver(observer); player.launchPicker = null }
    }
    return player
}
