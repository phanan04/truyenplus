package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.compositionLocalOf

/** Navigation belongs to the reader's chapter list, independent of speech highlighting. */
data class ReaderChapterNavigation(val position: Int, val total: Int) {
    val canPrevious get() = position > 0 && position < total
    val canNext get() = position >= 0 && position < total - 1
}

data class ReaderListeningContext(val sourceIndices: List<Int>, val currentParagraph: Int,
    val highlight: Boolean, val index: ReaderSpeechIndex, val onSelect: (Int) -> Unit,
    val sentence: ireader.domain.services.tts_service.v2.SpokenSentencePosition? = null,
    val onSentenceBounds: (Int,androidx.compose.ui.geometry.Rect)->Unit = {_,_->},
    val word: ireader.domain.services.tts_service.v2.SpokenSentencePosition? = null,
    val wordHighlight:Boolean=false, val chapterIndex:Int=-1, val chapterCount:Int=0,
    val focusSurroundings:Boolean=false) {
    constructor(sourceIndices: List<Int>, currentParagraph: Int, highlight: Boolean, onSelect: (Int)->Unit)
        : this(sourceIndices,currentParagraph,highlight,ReaderSpeechIndex(sourceIndices),onSelect)
    fun paragraphForReaderPage(index: Int): Int? = this.index.paragraphForReaderPage(index)
    fun sentenceRange(page:Int,text:String):IntRange? {
        val current=sentence ?: return null
        if(sourceIndices.getOrNull(current.paragraphIndex)!=page)return null
        val expected=current.start.takeIf {it>=0 && current.end<=text.length && current.end>it && text.substring(it,current.end)==current.text}
        val exact=expected ?: text.indexOf(current.text)
        return if(exact>=0)exact until exact+current.text.length else null
    }
    fun isHighlighted(index: Int): Boolean = highlight && sourceIndices.getOrNull(currentParagraph) == index
    fun wordRange(page:Int,text:String):IntRange? {
        val current=word?.takeIf {highlight && wordHighlight && sourceIndices.getOrNull(it.paragraphIndex)==page} ?: return null
        if(current.start<0 || current.end>text.length || current.end<=current.start)return null
        if(text.substring(current.start,current.end)!=current.text)return null
        val sentenceRange=sentenceRange(page,text) ?: return null
        return (current.start until current.end).takeIf {it.first>=sentenceRange.first && it.last<=sentenceRange.last}
    }
}
// Track only paragraphs consuming this value, not the entire reader/settings subtree.
val LocalReaderListening = compositionLocalOf<ReaderListeningContext?> { null }
