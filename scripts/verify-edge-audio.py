"""Exercise the production Android Edge player with controllable media callbacks; no device required."""
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, name, version=None):
    matches = sorted((cache / group / name / version).glob('*/*.jar')) if version else sorted((cache / group / name).glob('*/*/*.jar'))
    return str(matches[-1])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.3.21')
coroutines = jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.11.0')
test = jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-test-jvm', '1.11.0')
compiler = os.pathsep.join([jar('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.3.21'), stdlib,
    jar('org.jetbrains.kotlin','kotlin-reflect','2.3.21'), jar('org.jetbrains.kotlin','kotlin-script-runtime','2.3.21'), jar('org.jetbrains','annotations'), coroutines])
runtime = os.pathsep.join([stdlib, coroutines, test])
java = str(Path(os.environ['JAVA_HOME']) / 'bin/java')
fakes = {
'Context.kt': '''package android.content
class Context(val cacheDir: java.io.File)''',
'Media.kt': '''package android.media
class AudioAttributes {
 companion object { const val USAGE_MEDIA=1; const val CONTENT_TYPE_SPEECH=1 }
 class Builder { fun setUsage(i:Int)=this; fun setContentType(i:Int)=this; fun build()=AudioAttributes() }
}
class PlaybackParams {
 var speed=1f; var pitch=1f
 fun setSpeed(f:Float)=apply { speed=f }; fun setPitch(f:Float)=apply { pitch=f }
}
class MediaPlayer {
 companion object { val instances=mutableListOf<MediaPlayer>() }
 init { instances+=this }
 var prepared:(MediaPlayer)->Unit={}; var completed:(MediaPlayer)->Unit={}; var failed:(MediaPlayer,Int,Int)->Boolean={_,_,_->false}
 var playing=false; var released=false; var async=false; var path=""
 var playbackParams=PlaybackParams()
   set(value) { field=value; playing=true }
 fun setAudioAttributes(a:AudioAttributes) {}
 fun setOnPreparedListener(f:(MediaPlayer)->Unit) { prepared=f }
 fun setOnCompletionListener(f:(MediaPlayer)->Unit) { completed=f }
 fun setOnErrorListener(f:(MediaPlayer,Int,Int)->Boolean) { failed=f }
 fun setDataSource(p:String) { path=p }
 fun prepareAsync() { async=true }
 fun start() { check(!released); playing=true }
 fun pause() { check(!released); playing=false }
 fun release() { released=true; playing=false }
}''',
'EdgeAudioOutput.kt': '''package ireader.domain.services.tts_service.v2
interface EdgeAudioOutput {
 suspend fun play(bytes:ByteArray,onStarted:()->Unit,onCompleted:()->Unit,onError:()->Unit)
 fun pause(); fun resume(); fun stop(); fun release(); fun setSpeed(speed:Float); fun setPitch(pitch:Float)
}''',
'Test.kt': '''package ireader.domain.services.tts_service.v2
import android.content.Context
import android.media.MediaPlayer
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import java.io.File
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runBlocking {
 val main=newSingleThreadContext("test-main"); Dispatchers.setMain(main)
 val folder=java.nio.file.Files.createTempDirectory("edge-audio-test").toFile()
 try { withContext(Dispatchers.Main) {
  val output=AndroidEdgeAudioOutput(Context(folder))
  var starts=0; var completions=0; var errors=0
  suspend fun play()=output.play(byteArrayOf(1,2,3),{starts++},{completions++},{errors++})
  play(); val first=MediaPlayer.instances.last()
  check(first.async && starts==0 && !first.playing)
  output.pause(); first.prepared(first); check(starts==0 && !first.playing)
  output.resume(); check(first.playing)
  output.pause(); output.setSpeed(1.75f); output.setPitch(1.25f)
  check(!first.playing && first.playbackParams.speed==1.75f && first.playbackParams.pitch==1.25f)
  output.resume(); first.completed(first)
  check(completions==1 && errors==0 && first.released && !File(first.path).exists())
  println("PASS async preparation, paused parameter changes, actual completion and file cleanup")
  play(); val stale=MediaPlayer.instances.last(); play(); val current=MediaPlayer.instances.last()
  stale.prepared(stale); stale.completed(stale); stale.failed(stale,1,1)
  check(starts==0 && completions==1 && errors==0 && stale.released)
  current.prepared(current); check(starts==1 && current.playing)
  current.failed(current,1,1); check(errors==1 && completions==1 && current.released)
  println("PASS replaced callbacks ignored; playback errors never report completion")
  play(); val stopped=MediaPlayer.instances.last(); output.stop(); stopped.prepared(stopped)
  check(!stopped.playing && stopped.released)
  output.release(); play(); check(MediaPlayer.instances.last()===stopped)
  check(folder.listFiles()!!.isEmpty())
  println("PASS stop/release cannot start late audio; no temporary MP3 remains")
 } } finally { Dispatchers.resetMain(); main.close(); folder.deleteRecursively() }
}'''}
with tempfile.TemporaryDirectory(prefix='truyenplus-edge-') as temp:
    folder=Path(temp); sources=[]
    for name,source in fakes.items():
        p=folder/name; p.write_text(source); sources.append(str(p))
    sources.append(str(root/'domain/src/androidMain/kotlin/ireader/domain/services/tts_service/v2/AndroidEdgeAudioOutput.kt'))
    result=subprocess.run([java,'-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',runtime,'-d',str(folder/'classes'),*sources],capture_output=True,text=True)
    if result.returncode: raise SystemExit(result.stderr)
    subprocess.run([java,'-cp',str(folder/'classes')+os.pathsep+runtime,'ireader.domain.services.tts_service.v2.TestKt'],check=True)
