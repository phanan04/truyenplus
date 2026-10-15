package ireader.presentation.ui.truyenplus

import ireader.presentation.core.ui.ReaderScreenSpec
import ireader.presentation.core.NavigationRoutes
import ireader.domain.services.tts_service.v2.TTSState

/** Listening keeps the chapter visible and reuses the active speech position. */
fun readerListeningDestination(bookId: Long, chapterId: Long, paragraph: Int = 0) =
    ReaderScreenSpec(bookId, chapterId, listenOnOpen = true, listeningParagraph = paragraph)

/** A resume action follows the live session; selecting a specific chapter remains independent. */
fun readerResumeDestination(bookId:Long,chapterId:Long,state:TTSState):ReaderScreenSpec {
    val liveChapter=state.chapter
    return if(state.book?.id==bookId && liveChapter!=null && (state.isPlaying || state.isLoading))
        readerListeningDestination(bookId,liveChapter.id,state.currentParagraphIndex)
    else ReaderScreenSpec(bookId,chapterId)
}

fun ReaderScreenSpec.navigationRoute()=NavigationRoutes.reader(bookId,chapterId,listenOnOpen,listeningParagraph)
