package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlin.test.*

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
 @Test fun immersiveListeningWakesOnTouchAndNeverHidesOverOpenSettings() {
  val chrome=ReaderImmersiveState()
  chrome.touch(0)
  chrome.update(4999,true,false)
  assertTrue(chrome.visible)
  chrome.update(5000,true,false)
  assertFalse(chrome.visible)
  chrome.touch(6000)
  assertTrue(chrome.visible)
  chrome.update(15000,true,true)
  assertTrue(chrome.visible)
  chrome.touch(15000)
  chrome.update(18000,false,false)
  assertTrue(chrome.visible)
 }
 @Test fun mergedSpeechPositionMapsBackToTheCorrectParagraphWithoutChangingPlaybackIndex() {
  val paragraphs=listOf("Một câu đầu. Câu thứ hai.","Một đoạn mới. Kết thúc.")
  val merged=paragraphs.joinToString("\n\n")
  val position=spokenSentencePosition(paragraphs,listOf(0,1),merged,merged.indexOf("Kết"))!!
  assertEquals(1,position.paragraphIndex)
  assertEquals("Kết thúc.",paragraphs[1].substring(position.start,position.end))
 }
}
