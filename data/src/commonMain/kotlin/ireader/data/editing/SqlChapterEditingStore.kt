package ireader.data.editing

import ir.kazemcodes.infinityreader.Database
import ireader.domain.models.editing.*
import ireader.domain.services.chapter.ChapterNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SqlChapterEditingStore(private val db: Database, private val notifier: ChapterNotifier? = null): ChapterEditingStore {
    private val q get() = db.chapterEditingQueries
    override suspend fun load(chapterId: Long): EditingSnapshot = withContext(Dispatchers.IO) { snapshot(chapterId) }
    private fun snapshot(id: Long): EditingSnapshot {
        val chapter = db.chapterQueries.getChapterById(id).executeAsOneOrNull() ?: error("Chương không còn tồn tại.")
        return EditingSnapshot(id,chapter.book_id,chapter.content,q.hasOriginal(id).executeAsOne() > 0)
    }
    override suspend fun apply(proposal: EditingProposal) = withContext(Dispatchers.IO) {
        val saved = proposal.snapshot
        db.transaction {
            val current = snapshot(saved.chapterId)
            require(current.bookId == saved.bookId && current.pages == saved.pages) { "Nội dung chương đã thay đổi. Hãy tạo bản đề xuất mới." }
            val paragraphs = proposal.pages.mapIndexedNotNull { index,page -> (page as? ireader.core.source.model.Text)?.takeIf { it.text.isNotBlank() }?.let { EditParagraph(index,it.text) } }
            require(EditingParagraphs.apply(current.pages,paragraphs) == proposal.pages) { "Bản đề xuất đã thay đổi cấu trúc chương." }
            q.backupOriginal(saved.chapterId)
            // Update only content: never write stale progress/read/bookmark metadata.
            q.setEditedContent(proposal.pages,saved.chapterId)
            db.translatedChapterQueries.deleteTranslatedChaptersByChapterId(saved.chapterId)
        }
        notifier?.tryNotifyChange(ChapterNotifier.ChangeType.ContentFetched(saved.chapterId,saved.bookId))
        Unit
    }
    override suspend fun restore(chapterId: Long) = withContext(Dispatchers.IO) {
        val current = snapshot(chapterId)
        require(current.hasOriginal) { "Chương chưa có bản gốc đã lưu." }
        db.transaction { q.restoreOriginal(chapterId); q.deleteOriginal(chapterId); db.translatedChapterQueries.deleteTranslatedChaptersByChapterId(chapterId) }
        notifier?.tryNotifyChange(ChapterNotifier.ChangeType.ContentFetched(chapterId,current.bookId))
        Unit
    }
    override suspend fun characters(bookId: Long): List<EditingCharacter> = withContext(Dispatchers.IO) {
        q.characters(bookId).executeAsOneOrNull()?.let { Json.decodeFromString<List<EditingCharacter>>(it) } ?: emptyList()
    }
    override suspend fun saveCharacters(bookId: Long, characters: List<EditingCharacter>) = withContext(Dispatchers.IO) {
        require(characters.size <= 200) { "Tối đa 200 nhân vật mỗi truyện." }
        require(characters.all { it.name.isNotBlank() && it.name.length <= 200 && it.aliases.length <= 500 && it.notes.length <= 1000 }) { "Thông tin nhân vật quá dài hoặc thiếu tên." }
        require(characters.map { it.name.trim().lowercase() }.distinct().size == characters.size) { "Tên nhân vật bị trùng." }
        val value = Json.encodeToString(characters)
        require(value.length <= 40_000) { "Bộ nhớ nhân vật quá lớn." }
        q.saveCharacters(bookId,value)
        Unit
    }
}
