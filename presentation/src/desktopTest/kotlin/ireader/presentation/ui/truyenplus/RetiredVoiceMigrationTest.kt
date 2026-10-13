package ireader.presentation.ui.truyenplus

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import ireader.core.prefs.DataStorePreferenceStore
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.services.tts_service.v2.EngineType
import kotlinx.coroutines.*
import java.io.File
import kotlin.test.*

class RetiredVoiceMigrationTest {
 @Test fun savedEdgeChoiceIsRemovedAndCannotSelectAnEngine()=withPreferences { store,prefs ->
  store.getBoolean("use_edge_tts",false).set(true)
  store.getString("edge_tts_voice","").set("vi-VN-NamMinhNeural")
  prefs.useGradioTTS().set(true);prefs.useLocalTTS().set(true)
  prefs.retireLegacyEdgeVoice()
  assertFalse(store.getBoolean("use_edge_tts",false).isSet())
  assertFalse(store.getString("edge_tts_voice","").isSet())
  assertFalse(prefs.useGradioTTS().get());assertFalse(prefs.useLocalTTS().get())
  assertFalse(EngineType.entries.any {it.name=="EDGE"})
 }
 @Test fun migrationIsIdempotentAndKeepsAnUnrelatedEngineChoice()=withPreferences {store,prefs ->
  prefs.useGradioTTS().set(true)
  store.getString("edge_tts_voice","").set("vi-VN-HoaiMyNeural")
  prefs.retireLegacyEdgeVoice();prefs.retireLegacyEdgeVoice()
  assertTrue(prefs.useGradioTTS().get())
  assertFalse(store.getString("edge_tts_voice","").isSet())
 }
 private fun withPreferences(block:(DataStorePreferenceStore,AppPreferences)->Unit) {
  val dir=java.nio.file.Files.createTempDirectory("retired-voice-").toFile()
  val scope=CoroutineScope(Dispatchers.IO+SupervisorJob())
  val store=DataStorePreferenceStore(PreferenceDataStoreFactory.create(scope=scope,produceFile={File(dir,"voice.preferences_pb")}))
  try {block(store,AppPreferences(store))}
  finally {runBlocking {scope.coroutineContext[Job]!!.cancelAndJoin()};dir.deleteRecursively()}
 }
}
