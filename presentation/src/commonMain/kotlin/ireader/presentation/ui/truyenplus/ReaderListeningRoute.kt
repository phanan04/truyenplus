package ireader.presentation.ui.truyenplus

import ireader.presentation.core.ui.ReaderScreenSpec

/** Listening keeps the chapter visible and reuses the active speech position. */
fun readerListeningDestination(bookId: Long, chapterId: Long, paragraph: Int = 0) =
    ReaderScreenSpec(bookId, chapterId, listenOnOpen = true, listeningParagraph = paragraph)
