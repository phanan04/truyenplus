package ireader.presentation.ui.reader.viewmodel

import ireader.core.log.Log
import ireader.domain.data.repository.ChapterRepository
import ireader.presentation.ui.reader.ReaderConstants
import kotlinx.coroutines.*

/**
 * Manages scroll position tracking and persistence for the Reader.
 *
 * Responsibilities:
 * - Tracking current scroll position in memory
 * - Persisting scroll position to database
 * - Restoring scroll position when re-entering a chapter
 * - Handling scroll-to-end on chapter navigation
 */
class ReaderScrollManager(
    private val scope: CoroutineScope,
    private val chapterRepository: ChapterRepository
) {
    private var pendingSave: Job? = null
    private var hasTrackedPosition=false
    private var _currentScrollPosition: Long = ReaderConstants.DEFAULT_SCROLL_POSITION
    val currentScrollPosition: Long get() = _currentScrollPosition

    private var _shouldScrollToEnd: Boolean = false
    val shouldScrollToEnd: Boolean get() = _shouldScrollToEnd

    /**
     * Update the current scroll position in memory.
     * Does NOT persist to database - use [saveScrollPosition] for that.
     */
    fun updatePosition(position: Long) {
        _currentScrollPosition = position
        hasTrackedPosition=true
    }

    /**
     * Save the current scroll position to the database.
     * Call this periodically as the user scrolls.
     */
    fun saveScrollPosition(chapterId: Long, position: Long) {
        updatePosition(position)
        pendingSave?.cancel()
        pendingSave=scope.launch {
            delay(500)
            persist(chapterId,position)
        }
    }

    /** Capture the old chapter position before reset; finish this small write on disposal. */
    fun forceSaveScrollPosition(chapterId: Long) {
        if(!hasTrackedPosition)return
        val position=_currentScrollPosition
        hasTrackedPosition=false
        pendingSave?.cancel()
        pendingSave=null
        scope.launch(start=CoroutineStart.UNDISPATCHED) {
            withContext(NonCancellable) { persist(chapterId,position) }
        }
    }

    private suspend fun persist(chapterId:Long,position:Long) {
        try {
            chapterRepository.updateLastPageRead(chapterId,position)
        } catch(e:CancellationException) { throw e }
        catch(e:Exception) { Log.error("Failed to save scroll position for chapter $chapterId",e) }
    }

    /**
     * Set whether the reader should scroll to end on next chapter load.
     */
    fun setScrollToEnd(scrollToEnd: Boolean) {
        _shouldScrollToEnd = scrollToEnd
    }

    /**
     * Reset scroll state for a new chapter.
     */
    fun resetForNewChapter(savedPosition: Long = 0L) {
        _currentScrollPosition = savedPosition
        hasTrackedPosition=false
        _shouldScrollToEnd = false
    }

    /**
     * Get the saved scroll position for a chapter from the database.
     */
    suspend fun getSavedScrollPosition(chapterId: Long): Long {
        return try {
            val chapter = chapterRepository.findChapterById(chapterId)
            chapter?.lastPageRead ?: ReaderConstants.DEFAULT_SCROLL_POSITION
        } catch (e: Exception) {
            Log.error("Failed to get saved scroll position", e)
            ReaderConstants.DEFAULT_SCROLL_POSITION
        }
    }
}
