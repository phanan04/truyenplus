"""Compile the real Android music adapter against controllable Android media APIs."""
from pathlib import Path
import os, subprocess, tempfile
root=Path(__file__).resolve().parents[1]
import xml.etree.ElementTree as ET
manifest=ET.parse(root/"android/src/main/AndroidManifest.xml").getroot()
assert any(p.attrib.get("{http://schemas.android.com/apk/res/android}name")=="android.permission.MODIFY_AUDIO_SETTINGS" for p in manifest.findall("uses-permission")), "Playback volume permission must be declared"
cache=Path.home()/'.gradle/caches/modules-2/files-2.1'
def jar(group,name,version=None):
    found=sorted((cache/group/name/version).glob('*/*.jar')) if version else sorted((cache/group/name).glob('*/*/*.jar'))
    if not found: raise SystemExit('Missing cached library: '+name)
    return str(found[-1])
stdlib=jar('org.jetbrains.kotlin','kotlin-stdlib','2.3.21'); coroutines=jar('org.jetbrains.kotlinx','kotlinx-coroutines-core-jvm','1.11.0'); test=jar('org.jetbrains.kotlinx','kotlinx-coroutines-test-jvm','1.11.0')
compiler=os.pathsep.join([jar('org.jetbrains.kotlin','kotlin-compiler-embeddable','2.3.21'),stdlib,jar('org.jetbrains.kotlin','kotlin-reflect','2.3.21'),jar('org.jetbrains.kotlin','kotlin-script-runtime','2.3.21'),jar('org.jetbrains','annotations'),coroutines])
runtime=os.pathsep.join([stdlib,coroutines,test]); java=str(Path(os.environ['JAVA_HOME'])/'bin/java')
fakes={
'Context.kt': '''package android.content
import android.media.AudioManager
class Context {
 companion object { const val MODE_PRIVATE=0; const val AUDIO_SERVICE="audio" }
 val applicationContext get()=this
 val manager=AudioManager(); val prefs=Preferences()
 fun getSystemService(key:String):Any=manager
 fun getSharedPreferences(name:String,mode:Int)=prefs
}
class Preferences {
 val values=mutableMapOf<String,Any>()
 fun getString(key:String,default:String):String?=values[key] as? String ?: default
 fun getFloat(key:String,default:Float)=values[key] as? Float ?: default
 fun getBoolean(key:String,default:Boolean)=values[key] as? Boolean ?: default
 fun edit()=Editor(this)
}
class Editor(val prefs:Preferences) {
 fun putString(k:String,v:String)=apply { prefs.values[k]=v }
 fun putFloat(k:String,v:Float)=apply { prefs.values[k]=v }
 fun putBoolean(k:String,v:Boolean)=apply { prefs.values[k]=v }
 fun apply() {}
}''',
'Uri.kt': 'package android.net; class Uri(val value:String) { companion object { fun parse(value:String)=Uri(value) } }',
'Media.kt': '''package android.media
import android.content.Context
import android.net.Uri
class AudioManager {
 companion object { const val STREAM_MUSIC=3 }
 var volume=8
 fun getStreamMaxVolume(stream:Int)=10
 fun getStreamVolume(stream:Int)=volume
 fun setStreamVolume(stream:Int,v:Int,flags:Int) { volume=v }
}
class AudioAttributes {
 companion object { const val USAGE_MEDIA=1; const val CONTENT_TYPE_MUSIC=2 }
 class Builder { fun setUsage(v:Int)=this; fun setContentType(v:Int)=this; fun build()=AudioAttributes() }
}
class MediaPlayer {
 companion object { val instances=mutableListOf<MediaPlayer>(); var reject=false }
 init { instances+=this }
 var isLooping=false; var isPlaying=false; var released=false; var starts=0; var pauses=0; var volume=.0f
 lateinit var prepared:(MediaPlayer)->Unit; lateinit var error:(MediaPlayer,Int,Int)->Boolean
 fun setAudioAttributes(a:AudioAttributes) {}
 fun setOnPreparedListener(f:(MediaPlayer)->Unit) { prepared=f }
 fun setOnErrorListener(f:(MediaPlayer,Int,Int)->Boolean) { error=f }
 fun setDataSource(c:Context,uri:Uri) { if(reject) { reject=false; error("Unreadable file") } }
 fun prepareAsync() {}
 fun start() { check(!released); starts++; isPlaying=true }
 fun pause() { check(!released); pauses++; isPlaying=false }
 fun release() { released=true; isPlaying=false }
 fun setVolume(l:Float,r:Float) { check(!released); volume=l }
}''',
'TTS.kt': '''package ireader.domain.services.tts_service.v2
import kotlinx.coroutines.flow.MutableStateFlow
class TTSViewModelAdapter
class TTSState(val playbackState:PlaybackState)
enum class PlaybackState { IDLE,LOADING,PLAYING,PAUSED,STOPPED,ERROR }
''',
'Test.kt': '''package ireader.presentation.ui.truyenplus
import android.content.Context
import android.media.MediaPlayer
import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
@OptIn(ExperimentalCoroutinesApi::class)
fun main()=runTest {
 val dispatcher=StandardTestDispatcher(testScheduler); Dispatchers.setMain(dispatcher)
 val context=Context(); val playback=MutableStateFlow(TTSState(PlaybackState.PAUSED))
 val audio=AndroidListeningAudio(context,playback,CoroutineScope(dispatcher+SupervisorJob()))
 fun play(state:PlaybackState) { playback.value=TTSState(state); runCurrent() }
 try {
  runCurrent(); audio.selectMusic("content://music/one","One.mp3")
  check(MediaPlayer.instances.isEmpty())
  play(PlaybackState.PLAYING); val first=MediaPlayer.instances.last(); first.prepared(first)
  check(first.isPlaying && first.isLooping && first.volume==.2f)
  play(PlaybackState.LOADING); check(first.isPlaying && first.pauses==0)
  play(PlaybackState.PAUSED); check(!first.isPlaying && first.pauses==1)
  play(PlaybackState.PLAYING); check(first.isPlaying && first.starts==2)
  println("PASS music loops, survives chapter loading, pauses and resumes with TTS")
  audio.musicVolume(-.5f); check(first.volume==0f)
  audio.speechVolume(2f); check(context.manager.volume==10 && audio.state.value.speechVolume==1f)
  audio.selectMusic("content://music/two","Two.mp3"); val second=MediaPlayer.instances.last()
  check(first.released); first.prepared(first); first.error(first,1,1)
  check(!audio.state.value.musicError && !second.isPlaying)
  play(PlaybackState.PAUSED); second.prepared(second); check(!second.isPlaying)
  play(PlaybackState.PLAYING); check(second.isPlaying)
  println("PASS stale callbacks cannot start replaced audio; pending preparation respects pause")
  audio.musicEnabled(false); check(second.released && !audio.state.value.musicEnabled)
  audio.musicEnabled(true); val third=MediaPlayer.instances.last(); third.prepared(third)
  check(third.isPlaying)
  play(PlaybackState.STOPPED); check(!third.isPlaying)
  play(PlaybackState.PLAYING); MediaPlayer.reject=true
  audio.selectMusic("content://music/broken","Broken.mp3")
  check(audio.state.value.musicError && !audio.state.value.musicEnabled && MediaPlayer.instances.last().released)
  println("PASS disable releases media; stopped TTS silences music; bad files report errors")
 } finally { audio.close(); Dispatchers.resetMain() }
}'''}
with tempfile.TemporaryDirectory(prefix='truyenplus-music-') as temp:
    folder=Path(temp); sources=[]
    common=(root/'presentation/src/commonMain/kotlin/ireader/presentation/ui/truyenplus/ListeningAudio.kt').read_text()
    common='\n'.join(line for line in common.splitlines() if 'import androidx.compose.runtime.Composable' not in line and '@Composable expect fun' not in line)
    fakes['ListeningAudio.kt']=common
    for name,source in fakes.items():
        p=folder/name; p.write_text(source); sources.append(str(p))
    sources.append(str(root/'presentation/src/androidMain/kotlin/ireader/presentation/ui/truyenplus/AndroidListeningAudio.kt'))
    result=subprocess.run([java,'-cp',compiler,'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler','-no-stdlib','-no-reflect','-classpath',runtime,'-d',str(folder/'classes'),*sources],capture_output=True,text=True)
    if result.returncode: raise SystemExit(result.stderr)
    subprocess.run([java,'-cp',str(folder/'classes')+os.pathsep+runtime,'ireader.presentation.ui.truyenplus.TestKt'],check=True)
