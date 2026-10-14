package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.compositionLocalOf

data class ReaderListeningContext(val sourceIndices: List<Int>, val currentParagraph: Int,
    val highlight: Boolean, val index: ReaderSpeechIndex, val onSelect: (Int) -> Unit,
    val sentence: ireader.domain.services.tts_service.v2.SpokenSentencePosition? = null,
    val onSentenceBounds: (Int,androidx.compose.ui.geometry.Rect)->Unit = {_,_->}) {
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
}
// Track only paragraphs consuming this value, not the entire reader/settings subtree.
val LocalReaderListening = compositionLocalOf<ReaderListeningContext?> { null }
