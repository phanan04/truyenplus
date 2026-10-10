package ireader.presentation.ui.component.list.layouts

import kotlin.math.roundToInt

/** UI progress uses chapter counts, never the progress of a single chapter. */
data class BookReadingProgress(val readChapters: Int, val totalChapters: Int) {
    val fraction: Float get() = if (totalChapters > 0) readChapters.toFloat() / totalChapters else 0f
    val percent: Int get() = (fraction * 100).roundToInt()
}

fun bookReadingProgress(totalChapters: Int, unreadChapters: Int?): BookReadingProgress {
    val total = totalChapters.coerceAtLeast(0)
    val unread = (unreadChapters ?: total).coerceIn(0, total)
    return BookReadingProgress(total - unread, total)
}
