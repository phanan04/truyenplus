package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlin.test.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class SmartReaderBehaviourTest {
 @Test fun repeatedSentencesHighlightTheActualOccurrence() {
  val text="Nàng gật đầu. Nàng gật đầu."
  val second=text.lastIndexOf("Nàng")
  val context=ReaderListeningContext(listOf(0),0,true,ReaderSpeechIndex(listOf(0)),{},
   SpokenSentencePosition(0,second,text.length,"Nàng gật đầu."))
  assertEquals(second until text.length,context.sentenceRange(0,text))
 }
 @Test fun sentenceBoundsUseOriginalOffsetsAndDoNotSplitDecimals() {
  val text="  Hoàng Yên Trần rút kiếm. Giá là 1.25 đồng!  Nàng nói: ‘Đi thôi.’"
  val sentences=speechSegments(text,Int.MAX_VALUE)
  assertEquals(3,sentences.size)
  assertEquals("Hoàng Yên Trần rút kiếm.",text.substring(sentences[0].start,sentences[0].end))
  assertEquals("Giá là 1.25 đồng!",text.substring(sentences[1].start,sentences[1].end))
  assertTrue(sentenceAt(text,text.indexOf("1.25"))!!.contains(text.indexOf("đồng")))
 }
 @Test fun followingStaysSuspendedUntilUserExplicitlyReturns() {
  val follow=ReaderFollowState()
  assertTrue(follow.following)
  follow.onUserDrag()
  assertFalse(follow.following)
  // A new spoken sentence must not undo the reader's manual scroll.
  follow.onSpeechMoved()
  assertFalse(follow.following)
  follow.resume()
  assertTrue(follow.following)
  assertEquals(1,follow.returnRequest)
 }
 @Test fun immersiveListeningKeepsSettingsReachableWithoutWakingTheHeader() {
  val chrome=ReaderImmersiveState()
  chrome.touch(0)
  chrome.update(4999,true,false)
  assertTrue(chrome.visible)
  chrome.update(5000,true,false)
  assertFalse(chrome.visible)
  chrome.touch(6000)
  assertTrue(chrome.visible)
  chrome.update(15000,true,true)
  assertFalse(chrome.visible)
  assertTrue(chrome.playbackVisible)
  chrome.touch(15000)
  chrome.update(18000,false,false)
  assertTrue(chrome.visible)
 }
 @Test fun onlyManualScrollHidesBothChromeAndPlaybackUntilTheNextTouch() {
  val chrome=ReaderImmersiveState()
  val connection=ReaderUserScrollConnection {chrome.onUserScroll()}
  chrome.touch(0)
  assertEquals(Offset.Zero,connection.onPreScroll(Offset(0f,-30f),NestedScrollSource.SideEffect))
  assertTrue(chrome.visible && chrome.playbackVisible,"Smart follow must not hide the controls")
  connection.onPreScroll(Offset.Zero,NestedScrollSource.UserInput)
  assertTrue(chrome.visible && chrome.playbackVisible)
  assertEquals(Offset.Zero,connection.onPreScroll(Offset(-30f,0f),NestedScrollSource.UserInput))
  assertFalse(chrome.visible);assertFalse(chrome.playbackVisible)
  chrome.touch(1000)
  assertTrue(chrome.visible && chrome.playbackVisible)
  chrome.update(6000,true,false)
  assertFalse(chrome.visible)
  assertTrue(chrome.playbackVisible,"Idle listening still keeps its player; a manual swipe hides both")
  chrome.onUserScroll()
  chrome.update(6500,true,true)
  assertFalse(chrome.visible,"Settings must not reveal the header")
  assertTrue(chrome.playbackVisible,"Open settings keep playback reachable")
 }
 @Test fun mergedSpeechPositionMapsBackToTheCorrectParagraphWithoutChangingPlaybackIndex() {
  val paragraphs=listOf("Một câu đầu. Câu thứ hai.","Một đoạn mới. Kết thúc.")
  val merged=paragraphs.joinToString("\n\n")
  val position=spokenSentencePosition(paragraphs,listOf(0,1),merged,merged.indexOf("Kết"))!!
  assertEquals(1,position.paragraphIndex)
  assertEquals("Kết thúc.",paragraphs[1].substring(position.start,position.end))
 }
 @Test fun openingSettingsDoesNotRevealTheChapterHeader() {
  val chrome=ReaderImmersiveState()
  chrome.touch(0);chrome.onUserScroll()
  chrome.update(8000,true,true)
  assertFalse(chrome.visible,"Opening a panel must not reveal unrelated reader controls")
  assertTrue(chrome.playbackVisible,"Playback stays reachable below listening settings")
 }
 @Test fun readingIdleHidesBothControlsButListeningIdleKeepsPlayback() {
  val reading=ReaderImmersiveState();val listening=ReaderImmersiveState()
  reading.touch(100);listening.touch(100)
  reading.update(5099,true,false,listening=false)
  assertTrue(reading.visible)
  reading.update(5100,true,false,listening=false)
  listening.update(5100,true,false,listening=true)
  assertFalse(reading.visible || reading.playbackVisible)
  assertFalse(listening.visible);assertTrue(listening.playbackVisible)
 }
 @Test fun blankTapTogglesBothButAControlInteractionOnlyRefreshesTheDeadline() {
  val chrome=ReaderImmersiveState()
  chrome.touch(0);chrome.toggle(100)
  assertFalse(chrome.visible || chrome.playbackVisible)
  chrome.interact(200)
  assertFalse(chrome.visible || chrome.playbackVisible,"Seeking/Play must not reveal the header")
  chrome.toggle(300)
  assertTrue(chrome.visible && chrome.playbackVisible)
  chrome.interact(5000);chrome.update(8000,true,false)
  assertTrue(chrome.visible)
  chrome.update(10000,true,false)
  assertFalse(chrome.visible)
 }
 @Test fun readingSettingsDoNotForceEitherToolbarBackOnScreen() {
  val chrome=ReaderImmersiveState()
  chrome.touch(0);chrome.update(100,true,true,listening=false)
  assertFalse(chrome.visible || chrome.playbackVisible)
  chrome.update(200,true,false,listening=false)
  assertFalse(chrome.visible || chrome.playbackVisible)
 }
}
