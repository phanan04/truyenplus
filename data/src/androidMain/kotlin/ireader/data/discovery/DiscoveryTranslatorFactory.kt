package ireader.data.discovery

import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import ireader.domain.data.repository.DiscoveryTranslator
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Reuses the installed ML Kit SDK; metadata stays on device and clients are always closed. */
actual fun createDiscoveryTranslator(): DiscoveryTranslator = DiscoveryTranslator { texts, target ->
    val client = Translation.getClient(TranslatorOptions.Builder()
        .setSourceLanguage("zh").setTargetLanguage(target).build())
    try {
        client.downloadModelIfNeeded(DownloadConditions.Builder().requireWifi().build()).awaitDiscoveryTask()
        texts.map { client.translate(it).awaitDiscoveryTask() }
    } finally { client.close() }
}

private suspend fun <T> Task<T>.awaitDiscoveryTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { if (continuation.isActive) continuation.resumeWithException(IllegalStateException("Translation task cancelled")) }
}
