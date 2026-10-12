package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.staticCompositionLocalOf

data class ReaderListeningContext(val sourceIndices: List<Int>, val currentParagraph: Int,
    val highlight: Boolean, val onSelect: (Int) -> Unit) {
    fun paragraphForReaderPage(index: Int): Int? = sourceIndices.indexOf(index).takeIf { it >= 0 }
    fun isHighlighted(index: Int): Boolean = highlight && sourceIndices.getOrNull(currentParagraph) == index
}
val LocalReaderListening = staticCompositionLocalOf<ReaderListeningContext?> { null }
