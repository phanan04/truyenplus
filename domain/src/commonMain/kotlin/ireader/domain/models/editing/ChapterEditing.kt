package ireader.domain.models.editing

import ireader.core.source.model.Page
import ireader.core.source.model.Text
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ByteString.Companion.encodeUtf8

@Serializable data class EditParagraph(val id: Int, val text: String)
@Serializable data class EditingCharacter(val name: String, val aliases: String = "", val notes: String = "")
@Serializable enum class EditingProvider(val defaultModel: String) {
    Gemini("gemini-3.8-flash"), DeepSeek("deepseek-flash")
}
@Serializable data class EditingConfig(val provider: EditingProvider = EditingProvider.Gemini, val model: String = provider.defaultModel)
@Serializable enum class EditingMode { Grammar, Translate }
// Keys are deliberately not serializable and never included in saved configuration.
class EditingCredentials(val config: EditingConfig, val apiKey: String)
data class EditingSnapshot(val chapterId: Long, val bookId: Long, val pages: List<Page>, val hasOriginal: Boolean)
data class EditingProposal(val snapshot: EditingSnapshot, val pages: List<Page>)

@Serializable data class ChapterEditingDraft(val fingerprint:String,val mode:EditingMode,
    val paragraphs:List<EditParagraph>,val selected:Set<Int>,val manual:EditParagraph?=null)
enum class EditingFormKind { Characters, Rules }
fun editingFingerprint(pages:List<Page>):String=Json.encodeToString(pages).encodeUtf8().sha256().hex()

interface ChapterEditingStore {
    suspend fun loadDraft(chapterId:Long):ChapterEditingDraft? = null
    suspend fun saveDraft(chapterId:Long,draft:ChapterEditingDraft) {}
    suspend fun clearDraft(chapterId:Long) {}
    suspend fun loadFormDraft(bookId:Long,kind:EditingFormKind):String? = null
    suspend fun saveFormDraft(bookId:Long,kind:EditingFormKind,payload:String) {}
    suspend fun clearFormDraft(bookId:Long,kind:EditingFormKind) {}

    suspend fun load(chapterId: Long): EditingSnapshot
    suspend fun apply(proposal: EditingProposal)
    suspend fun restore(chapterId: Long)
    suspend fun characters(bookId: Long): List<EditingCharacter>
    suspend fun saveCharacters(bookId: Long, characters: List<EditingCharacter>)
}
interface ChapterEditingApi {
    suspend fun checkConnection(credentials:EditingCredentials) {throw UnsupportedOperationException("Connection check is unavailable")}

    suspend fun edit(credentials: EditingCredentials, mode: EditingMode, paragraphs: List<EditParagraph>, characters: List<EditingCharacter>): List<EditParagraph>
}
interface EditingSettings {
    suspend fun load(): EditingConfig
    suspend fun save(config: EditingConfig, newKey: String?)
    suspend fun credentials(config: EditingConfig): EditingCredentials
    suspend fun clearKey(provider: EditingProvider)
}

object EditingParagraphs {
    const val MAX_BATCH_CHARS = 6000
    fun batches(pages: List<Page>): List<List<EditParagraph>> {
        require(pages.size <= 5000) { "Chương quá lớn để biên tập." }
        val paragraphs = pages.mapIndexedNotNull { index, page -> (page as? Text)?.text?.takeIf { it.isNotBlank() }?.let { EditParagraph(index,it) } }
        require(paragraphs.isNotEmpty()) { "Chương chưa có nội dung." }
        require(paragraphs.sumOf { it.text.length } <= 120_000) { "Chương quá lớn để biên tập." }
        val result = mutableListOf<List<EditParagraph>>()
        var current = mutableListOf<EditParagraph>()
        var chars = 0
        for (paragraph in paragraphs) {
            require(paragraph.text.length <= MAX_BATCH_CHARS) { "Một đoạn quá dài. Hãy chia đoạn trước khi biên tập." }
            if (chars + paragraph.text.length > MAX_BATCH_CHARS || current.size >= 40) {
                result.add(current); current = mutableListOf(); chars = 0
            }
            current.add(paragraph); chars += paragraph.text.length
        }
        if (current.isNotEmpty()) result.add(current)
        return result
    }
    fun apply(pages: List<Page>, result: List<EditParagraph>): List<Page> {
        val expected = pages.mapIndexedNotNull { index,page -> if (page is Text && page.text.isNotBlank()) index else null }
        require(result.map { it.id } == expected) { "Bản đề xuất thiếu, trùng hoặc đảo thứ tự đoạn. Chưa áp dụng." }
        require(result.all { it.text.isNotBlank() && it.text.length <= MAX_BATCH_CHARS * 3 }) { "Bản đề xuất có đoạn rỗng hoặc quá dài." }
        val byId = result.associateBy { it.id }
        return pages.mapIndexed { index,page -> byId[index]?.let { Text(it.text) } ?: page }
    }
}
class EditChapter(private val store: ChapterEditingStore, private val api: ChapterEditingApi) {
    suspend fun propose(chapterId: Long, credentials: EditingCredentials, mode: EditingMode, progress: (Int,Int)->Unit): EditingProposal {
        val snapshot = store.load(chapterId)
        val characters = store.characters(snapshot.bookId)
        val batches = EditingParagraphs.batches(snapshot.pages)
        val result = mutableListOf<EditParagraph>()
        batches.forEachIndexed { i,batch ->
            progress(i,batches.size)
            val edited = api.edit(credentials,mode,batch,characters)
            require(edited.map { it.id } == batch.map { it.id }) { "AI trả thiếu hoặc sai thứ tự đoạn. Hãy thử lại." }
            result.addAll(edited)
        }
        progress(batches.size,batches.size)
        return EditingProposal(snapshot,EditingParagraphs.apply(snapshot.pages,result))
    }
}
