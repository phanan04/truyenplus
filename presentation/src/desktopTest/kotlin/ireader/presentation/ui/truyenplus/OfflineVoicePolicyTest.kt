package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.offline.*
import kotlin.test.*

class OfflineVoicePolicyTest {
 @Test fun onlyVerifiedVietnameseModelsAndValidSpeakerSelectionsAreAllowed() {
  val piper=OfflineVoiceCatalog.models.first {it.family==OfflineVoiceFamily.Piper}
  val supertonic=OfflineVoiceCatalog.models.first {it.family==OfflineVoiceFamily.Supertonic}
  assertTrue(piper.archiveBytes<25_000_000)
  assertTrue(piper.sha256.matches(Regex("[a-f0-9]{64}")))
  assertEquals(1,piper.speakers)
  assertFailsWith<IllegalArgumentException>{piper.config("/models",1)}
  assertEquals(9,supertonic.config("/models",9).speaker)
  assertFailsWith<IllegalArgumentException>{supertonic.config("/models",10)}
 }
 @Test fun archivePathsCannotEscapeTheExpectedModelFolder() {
  val root="model"
  assertEquals("espeak-ng-data/voices/a",safeVoiceArchivePath(root,"model/espeak-ng-data/voices/a"))
  for(path in listOf("../x","/model/a","model/../outside","model/../../outside","other/a","model/a\\..\\b")) {
   assertFailsWith<IllegalArgumentException>(path){safeVoiceArchivePath(root,path)}
  }
 }
 @Test fun generatedAudioIsPeakLimitedAndRejectsNaNsInsteadOfProducingCrackle() {
  val pcm=normalizedVoicePcm(floatArrayOf(-1.34f,0f,.2f,1.1f))
  assertTrue(pcm.all {kotlin.math.abs(it.toInt())<=32113})
  assertEquals(0,pcm[1].toInt())
  assertTrue(pcm[2]>0 && pcm[0]<0)
  assertFailsWith<IllegalArgumentException>{normalizedVoicePcm(floatArrayOf(Float.NaN))}
 }
}
