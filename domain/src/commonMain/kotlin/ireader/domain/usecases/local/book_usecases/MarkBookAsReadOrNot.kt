package ireader.domain.usecases.local.book_usecases

import ireader.domain.models.entities.Chapter


class MarkBookAsReadOrNotUseCase(
    private val localGetChapterUseCase: ireader.domain.usecases.local.LocalGetChapterUseCase,
    private val insertUseCases: ireader.domain.usecases.local.LocalInsertUseCases,

    ) {

    suspend fun markAsRead(bookIds: List<Long>) { mark(bookIds, true) }
    suspend fun markAsNotRead(bookIds: List<Long>) { mark(bookIds, false) }
    suspend fun markAsReadWithUndo(bookIds: List<Long>): MarkResult = mark(bookIds, true)
    suspend fun markAsUnreadWithUndo(bookIds: List<Long>): MarkResult = mark(bookIds, false)

    private suspend fun mark(bookIds: List<Long>, read: Boolean): MarkResult.Success {
        val previous = bookIds.distinct().associateWith { localGetChapterUseCase.findChaptersByBookId(it) }
        val chapters = previous.values.flatten()
        insertUseCases.insertChapters.updateReadState(chapters.associate { it.id to read })
        return MarkResult.Success(chapters.size, previous.size, previous)
    }

    suspend fun undoMark(previousStates: Map<Long, List<Chapter>>) {
        insertUseCases.insertChapters.updateReadState(previousStates.values.flatten().associate { it.id to it.read })
    }

}

/**
 * Result of mark as read/unread operation
 */
sealed class MarkResult {
    data class Success(
        val totalChapters: Int,
        val totalBooks: Int,
        val previousStates: Map<Long, List<Chapter>>
    ) : MarkResult()
    
    data class Failure(
        val message: String
    ) : MarkResult()
}