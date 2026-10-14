package ireader.data.tts.offline

import android.content.Context
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.services.tts_service.v2.offline.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/** Models are optional app-private files. Only an atomic verified install is exposed as playable. */
class AndroidOfflineVoiceStore(context:Context,private val preferences:AppPreferences):OfflineVoiceStore {
    private val root=File(context.noBackupFilesDir,"voices").apply {mkdirs()}
    private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
    private val sessionTag=java.util.UUID.randomUUID().toString()
    private val jobs=ConcurrentHashMap<String,Job>()
    private val mutableState=MutableStateFlow(OfflineVoiceState(installed=installed(),selection=readChoice()))
    override val state=mutableState.asStateFlow()
    init {scope.launch {
        // The retired engine is never selected or offered again; reclaim only its known model folder.
        File(root,"sherpa-onnx-supertonic-3-tts-int8-2026-05-11").deleteRecursively()
        if(preferences.offlineVoiceChoice().get().startsWith("supertonic3-vi-int8|"))preferences.offlineVoiceChoice().set("")
        root.listFiles()?.filter {it.name.startsWith(".install-") && !it.name.endsWith(sessionTag)}?.forEach {it.deleteRecursively()}
    }}
    private fun installed()=OfflineVoiceCatalog.models.filter(::ready).map{it.id}.toSet()
    private fun ready(model:OfflineVoiceModel):Boolean = runCatching {
        val folder=File(root,model.folder)
        File(folder,".verified").takeIf {it.isFile}?.readText()==model.sha256 && model.files.all {
            val file=File(folder,it);file.isDirectory || (file.isFile && file.length()>0)
        }
    }.getOrDefault(false)
    private fun readChoice():OfflineVoiceSelection? {
        val raw=preferences.offlineVoiceChoice().get().split('|')
        val model=raw.firstOrNull()?.let(OfflineVoiceCatalog::find) ?: return null
        val speaker=raw.getOrNull(1)?.toIntOrNull() ?: 0
        return if(speaker in 0 until model.speakers && ready(model))OfflineVoiceSelection(model.id,speaker) else null
    }
    override fun config(modelId:String,speaker:Int):OfflineVoiceConfig? {
        val model=OfflineVoiceCatalog.find(modelId) ?: return null
        return if(ready(model) && speaker in 0 until model.speakers)model.config(root.absolutePath,speaker) else null
    }
    override fun selectedConfig()=mutableState.value.selection?.let{config(it.modelId,it.speaker)}
    override fun select(modelId:String?,speaker:Int):Boolean {
        if(modelId!=null && config(modelId,speaker)==null)return false
        val selected=modelId?.let {OfflineVoiceSelection(it,speaker)}
        preferences.offlineVoiceChoice().set(if(selected==null)"" else "${selected.modelId}|${selected.speaker}")
        mutableState.update {it.copy(selection=selected)}
        return true
    }
    override fun startDownload(modelId:String) {
        val model=OfflineVoiceCatalog.find(modelId) ?: return
        if(ready(model))return
        val job=scope.launch(start=CoroutineStart.LAZY) {
            val stage=File(root,".install-${model.id}-$sessionTag").apply {mkdirs()}
            try {
                mutableState.update {it.copy(errors=it.errors-modelId,downloads=it.downloads+(modelId to VoiceDownload(VoiceDownloadPhase.Downloading,0,model.archiveBytes)))}
                val extracted=File(stage,"model").apply {mkdirs()}
                if(model.artifacts.isNotEmpty()) {
                    var completed=0L
                    for(asset in model.artifacts) {
                        require(asset.name.matches(Regex("[a-zA-Z0-9_.-]+")))
                        val target=File(extracted,asset.name)
                        val connection=URI(asset.url).toURL().openConnection() as HttpURLConnection
                        connection.connectTimeout=20_000;connection.readTimeout=30_000
                        connection.setRequestProperty("User-Agent","TruyenPlus-VoiceModels")
                        try {
                            require(connection.responseCode==200 && connection.url.protocol=="https")
                            val digest=MessageDigest.getInstance("SHA-256");var bytes=0L;var lastUpdate=0L
                            connection.inputStream.use {input->target.outputStream().use {output->
                                val buffer=ByteArray(64*1024)
                                while(true) {
                                    ensureActive();val count=input.read(buffer);if(count<0)break
                                    bytes+=count;require(bytes<=asset.bytes);digest.update(buffer,0,count);output.write(buffer,0,count)
                                    if(bytes-lastUpdate>=512*1024) {
                                        mutableState.update {it.copy(downloads=it.downloads+(modelId to VoiceDownload(VoiceDownloadPhase.Downloading,completed+bytes,model.archiveBytes)))}
                                        lastUpdate=bytes
                                    }
                                }
                            }}
                            require(bytes==asset.bytes && digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}==asset.sha256)
                            completed+=bytes
                        } finally {connection.disconnect()}
                    }
                    require(completed==model.archiveBytes)
                } else {
                val archive=File(stage,"archive.bz2")
                val connection=URI(model.url).toURL().openConnection() as HttpURLConnection
                connection.connectTimeout=20_000;connection.readTimeout=30_000
                connection.setRequestProperty("User-Agent","TruyenPlus-VoiceModels")
                try {
                    require(connection.responseCode==200 && connection.url.protocol=="https")
                    val digest=MessageDigest.getInstance("SHA-256")
                    var bytes=0L;var lastUpdate=0L;var lastUpdateTime=System.nanoTime()
                    connection.inputStream.use {input -> archive.outputStream().use {output ->
                        val buffer=ByteArray(64*1024)
                        while(true) {
                            ensureActive();val count=input.read(buffer);if(count<0)break
                            bytes+=count;require(bytes<=model.archiveBytes)
                            digest.update(buffer,0,count);output.write(buffer,0,count)
                            if(bytes-lastUpdate>=256*1024 && System.nanoTime()-lastUpdateTime>=100_000_000) {
                                mutableState.update {it.copy(downloads=it.downloads+(modelId to VoiceDownload(VoiceDownloadPhase.Downloading,bytes,model.archiveBytes)))}
                                lastUpdate=bytes;lastUpdateTime=System.nanoTime()
                            }
                        }
                    }}
                    val sha=digest.digest().joinToString(""){"%02x".format(it.toInt() and 255)}
                    require(bytes==model.archiveBytes && sha==model.sha256)
                } finally {connection.disconnect()}
                mutableState.update {it.copy(downloads=it.downloads+(modelId to VoiceDownload(VoiceDownloadPhase.Installing,model.archiveBytes,model.archiveBytes)))}
                var expanded=0L
                TarArchiveInputStream(BZip2CompressorInputStream(archive.inputStream().buffered())).use {tar ->
                    while(true) {
                        ensureActive();val entry=tar.nextEntry ?: break
                        require(!entry.isSymbolicLink && !entry.isLink)
                        val relative=safeVoiceArchivePath(model.folder,entry.name)
                        if(relative.isEmpty())continue
                        val target=File(extracted,relative)
                        require(target.canonicalPath.startsWith(extracted.canonicalPath+File.separator))
                        if(entry.isDirectory)target.mkdirs()
                        else {
                            require(entry.isFile && entry.size>=0 && entry.size<=256*1024*1024)
                            expanded+=entry.size;require(expanded<=320*1024*1024)
                            target.parentFile?.mkdirs()
                            target.outputStream().use {out ->
                                val buffer=ByteArray(64*1024)
                                while(true){ensureActive();val count=tar.read(buffer);if(count<0)break;out.write(buffer,0,count)}
                            }
                        }
                    }
                }
                }
                require(model.files.all {File(extracted,it).exists()})
                File(extracted,".verified").writeText(model.sha256)
                ensureActive()
                val final=File(root,model.folder)
                require(!final.exists() || final.deleteRecursively())
                require(extracted.renameTo(final))
                mutableState.update {it.copy(installed=installed())}
            } catch(e:CancellationException) {throw e}
            catch(e:Exception) {
                ireader.core.log.Log.warn {"Voice model ${model.id}: ${e.message}"}
                mutableState.update {it.copy(errors=it.errors+modelId)}
            } finally {
                stage.deleteRecursively();jobs.remove(modelId,coroutineContext[Job])
                mutableState.update {it.copy(downloads=it.downloads-modelId)}
            }
        }
        if(jobs.putIfAbsent(modelId,job)==null)job.start() else job.cancel()
    }
    override fun cancelDownload(modelId:String) {jobs[modelId]?.cancel()}
    override suspend fun remove(modelId:String)=withContext(Dispatchers.IO) {
        val model=OfflineVoiceCatalog.find(modelId) ?: return@withContext
        jobs[modelId]?.cancelAndJoin()
        if(mutableState.value.selection?.modelId==modelId)select(null)
        File(root,model.folder).deleteRecursively()
        mutableState.update {it.copy(installed=installed(),errors=it.errors-modelId)}
    }
}
