package ireader.domain.usecases.local.insert_usecases

import ireader.domain.data.repository.ChapterRepository
import ireader.domain.models.entities.Chapter
import ireader.domain.utils.extensions.withIOContext



class InsertChapters(private val chapterRepository: ChapterRepository) {
    suspend fun updateReadState(states: Map<Long, Boolean>) = withIOContext {
        chapterRepository.updateReadState(states)
    }
    suspend operator fun invoke(chapters: List<Chapter>): List<Long> {
        return withIOContext {
            return@withIOContext chapterRepository.insertChapters(chapters)
        }
    }
}
