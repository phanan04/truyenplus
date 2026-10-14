package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import kotlin.test.*

class ReaderWordHighlightTest {
    @Test fun actualEngineRangeMapsToOriginalRepeatedParagraphAndUtf16() {
        val paragraphs=listOf("  Nàng nói: 😀 Đi thôi.  ","  Nàng nói: 😀 Đi thôi.  ")
        val speech=paragraphs.joinToString("\n") {it.trim()}
        val index=SpokenTextIndex(paragraphs,listOf(0,1),speech)
        val start=speech.lastIndexOf("Đi")
        val word=index.range(start,start+2)!!
        assertEquals(1,word.paragraphIndex)
        assertEquals(paragraphs[1].indexOf("Đi"),word.start)
        assertEquals("Đi",word.text)
        assertNull(index.range(start,start))
        assertNull(index.range(-1,2))
        assertNull(index.range(start,speech.length+1))
    }
    @Test fun coarseRangeNeverPretendsToBeWordTiming() {
        val text="Hoàng Yên Trần rút kiếm. Nàng quay lại."
        val index=SpokenTextIndex(listOf(text),listOf(0),text)
        val coarse=EngineEvent.RangeStarted("a",0,text.length)
        assertNull(index.wordAt(coarse))
        val actual=EngineEvent.RangeStarted("a",6,9,isWordBoundary=true)
        assertEquals("Yên",index.wordAt(actual)?.text)
        assertEquals("Hoàng Yên Trần rút kiếm.",index.at(actual.start)?.text)
    }
    @Test fun wordModeFallsBackToSentenceWhenNoRangeAndIgnoresOtherPages() {
        val text="Nàng gật đầu. Nàng gật đầu."
        val start=text.lastIndexOf("Nàng")
        val sentence=SpokenSentencePosition(0,start,text.length,"Nàng gật đầu.")
        val word=SpokenSentencePosition(0,start,start+4,"Nàng")
        val context=ReaderListeningContext(listOf(7),0,true,ReaderSpeechIndex(listOf(7)),{},sentence,
            word=word,wordHighlight=true)
        assertEquals(start until start+4,context.wordRange(7,text))
        assertNull(context.wordRange(8,text))
        assertNull(context.copy(word=null).wordRange(7,text))
        assertNotNull(context.copy(word=null).sentenceRange(7,text))
        assertNull(context.copy(wordHighlight=false).wordRange(7,text))
        assertNull(context.copy(highlight=false).wordRange(7,text))
    }
}
