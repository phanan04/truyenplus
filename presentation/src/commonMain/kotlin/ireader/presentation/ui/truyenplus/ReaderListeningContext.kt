package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.compositionLocalOf

data class ReaderListeningContext(val sourceIndices: List<Int>, val currentParagraph: Int,
    val highlight: Boolean, val index: ReaderSpeechIndex, val onSelect: (Int) -> Unit) {
    constructor(sourceIndices: List<Int>, currentParagraph: Int, highlight: Boolean, onSelect: (Int)->Unit)
        : this(sourceIndices,currentParagraph,highlight,ReaderSpeechIndex(sourceIndices),onSelect)
    fun paragraphForReaderPage(index: Int): Int? = this.index.paragraphForReaderPage(index)
    fun isHighlighted(index: Int): Boolean = highlight && sourceIndices.getOrNull(currentParagraph) == index
}
// Track only paragraphs consuming this value, not the entire reader/settings subtree.
val LocalReaderListening = compositionLocalOf<ReaderListeningContext?> { null }
