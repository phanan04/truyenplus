"""Run the production Android native TTS player against a controllable Android engine."""
from pathlib import Path
import os, subprocess, tempfile
root = Path(__file__).resolve().parents[1]
cache = Path.home() / '.gradle/caches/modules-2/files-2.1'
def jar(group, name, version=None):
    directory = cache / group / name
    matches = sorted((directory / version).glob('*/*.jar')) if version else sorted(directory.glob('*/*/*.jar'))
    if not matches: raise SystemExit('Missing cached library: ' + name)
    return str(matches[-1])
stdlib = jar('org.jetbrains.kotlin', 'kotlin-stdlib', '2.3.21')
coroutines = jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-core-jvm', '1.11.0')
test = jar('org.jetbrains.kotlinx', 'kotlinx-coroutines-test-jvm', '1.11.0')
compile_cp = os.pathsep.join([jar('org.jetbrains.kotlin', 'kotlin-compiler-embeddable', '2.3.21'), stdlib,
    jar('org.jetbrains.kotlin', 'kotlin-reflect', '2.3.21'), jar('org.jetbrains.kotlin', 'kotlin-script-runtime', '2.3.21'),
    jar('org.jetbrains', 'annotations'), coroutines])
runtime_cp = os.pathsep.join([stdlib, coroutines, test])
java = Path(os.environ['JAVA_HOME']) / 'bin/java'
fakes = {
'Context.kt': 'package android.content; class Context { val applicationContext get() = this }',
'Chapter.kt': 'package ireader.domain.models.entities; class Chapter',
'Coqui.kt': 'package ireader.domain.services.tts; class CoquiTTSService',
'Voice.kt': 'package ireader.domain.preferences.models.prefs; data class IReaderVoice(val name: String)',
'Log.kt': '''package ireader.core.log
object Log { fun info(f: () -> String) {} ; fun warn(f: () -> String) {}; fun error(f: () -> String) {} }''',
'AndroidTTS.kt': '''package android.speech.tts
import android.content.Context
abstract class UtteranceProgressListener {
 abstract fun onStart(id: String?); abstract fun onDone(id: String?); abstract fun onError(id: String?)
 open fun onError(id: String?, code: Int) { onError(id) }
}
class Voice(val name: String)
class TextToSpeech(context: Context, val initialize: (Int) -> Unit) {
 companion object { const val SUCCESS = 0; const val ERROR = -1; const val QUEUE_ADD = 1; const val QUEUE_FLUSH = 0; lateinit var instance: TextToSpeech }
 init { instance = this }
 val voices = emptySet<Voice>()
 lateinit var listener: UtteranceProgressListener
 val queued = mutableListOf<String>()
 val texts = mutableListOf<String>()
 var calls = 0
 var rejectCall = -1
 var rejectAll = false
 fun setOnUtteranceProgressListener(value: UtteranceProgressListener) { listener = value }
 fun speak(text: String, mode: Int, params: Any?, id: String): Int {
  calls++
  if (rejectAll || calls == rejectCall) return ERROR // Android may reject without firing onError.
  queued += id
  texts += text
  return SUCCESS
 }
 fun setVoice(voice: Voice) = SUCCESS
 fun setSpeechRate(rate: Float) = SUCCESS
 fun setPitch(pitch: Float) = SUCCESS
 fun stop() = SUCCESS
 fun shutdown() {}
}''',
'Test.kt': '''import android.content.Context
import android.speech.tts.TextToSpeech
import ireader.domain.services.tts_service.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
@OptIn(ExperimentalCoroutinesApi::class)
fun main() = runTest {
 Dispatchers.setMain(StandardTestDispatcher(testScheduler))
 val player = NativeTTSPlayer(Context())
 var completed = 0
 var failures = 0
 player.setCallback(object : TTSCallback {
  override fun onStart(id: String) {}
  override fun onDone(id: String) { completed++ }
  override fun onError(id: String, error: String) { failures++ }
 })
 try {
  val engine = TextToSpeech.instance
  engine.initialize(TextToSpeech.SUCCESS)
  runCurrent()
  engine.rejectCall = 3
  player.speak("First sentence. Second sentence. Third sentence.", "paragraph")
  val first = engine.queued[0]
  val second = engine.queued[1]
  engine.listener.onStart(first)
  engine.listener.onDone(first) // Queuing chunk 3 returns ERROR with no callback.
  runCurrent()
  engine.listener.onError("paragraph__sub__2", TextToSpeech.ERROR)
  runCurrent()
  check(completed == 0 && failures == 0) { "Duplicate error completed the paragraph before chunk 2 finished" }
  engine.listener.onDone(second)
  runCurrent()
  check(completed == 1 || failures == 1) { "Rejected Android speak() left paragraph stalled: done=$completed error=$failures" }
  println("PASS: synchronous queue rejection and duplicate errors cannot stall or finish playback early")
  player.stop()
  engine.queued.clear(); engine.texts.clear(); engine.rejectCall = -1
  player.setSpeed(.5f)
  val longText = (1..50).joinToString(" ") { "word$it" } + "."
  player.speak(longText, "slow")
  check(engine.texts.all { it.length <= 175 }) { "Slow speech chunks can exceed synthesis timeout: ${engine.texts.map { it.length }}" }
  var index = 0
  while (index < engine.queued.size) {
   engine.listener.onDone(engine.queued[index++]); runCurrent()
  }
  check(engine.texts.all { it.length <= 175 })
  check(engine.texts.joinToString(" ") == longText) { "Splitting lost or reordered words" }
  println("PASS: half-speed speech uses shorter chunks without losing text")
  check(ireader.domain.services.tts_service.v2.TTSSentenceSplitter.split("Ừ") == listOf("Ừ")) { "A single-letter spoken response was discarded" }
  println("PASS: single-letter Vietnamese responses are preserved")
  player.stop()
  engine.rejectAll = true
  player.speak("Rejected first sentence. Rejected second sentence.", "rejected")
  runCurrent()
  check(failures == 1 && completed == 2) { "All rejected chunks must report exactly one paragraph error" }
  println("PASS: all rejected chunks report one error even without onStart")
 } finally { player.cleanup(); Dispatchers.resetMain() }
}'''
}
with tempfile.TemporaryDirectory(prefix='truyenplus-native-tts-') as tmp:
    folder = Path(tmp)
    sources = []
    for name, text in fakes.items():
        path = folder / name; path.write_text(text); sources.append(str(path))
    native = (root / 'domain/src/androidMain/kotlin/ireader/domain/services/tts_service/UnifiedTTSPlayer.kt').read_text()
    # Compile the real native class; the unrelated Coqui implementation starts below it.
    native = native[:native.index('/**\n * Gradio TTS implementation')]
    path = folder / 'NativeTTSPlayer.kt'; path.write_text(native); sources.append(str(path))
    sources.append(str(root / 'domain/src/commonMain/kotlin/ireader/domain/services/tts_service/v2/TTSSentenceSplitter.kt'))
    result = subprocess.run([str(java), '-cp', compile_cp, 'org.jetbrains.kotlin.cli.jvm.K2JVMCompiler',
        '-no-stdlib', '-no-reflect', '-classpath', runtime_cp, '-d', str(folder / 'classes'), *sources], capture_output=True, text=True)
    if result.returncode: raise SystemExit(result.stderr)
    subprocess.run([str(java), '-cp', str(folder / 'classes') + os.pathsep + runtime_cp, 'TestKt'], check=True)
