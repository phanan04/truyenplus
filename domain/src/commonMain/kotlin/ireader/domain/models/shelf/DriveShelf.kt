package ireader.domain.models.shelf

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** Links are identifiers, never arbitrary HTTP endpoints or local paths. */
data class DriveFolderLink(val folderId: String, val resourceKey: String? = null) {
    val url: String get() = "https://drive.google.com/drive/folders/$folderId" + (resourceKey?.let { "?resourcekey=$it" } ?: "")
    companion object {
        fun parse(input: String): DriveFolderLink {
            val match = Regex("^https://drive\\.google\\.com/drive/(?:u/[0-9]+/)?folders/([A-Za-z0-9_-]{1,200})/?(?:\\?([^#]*))?(?:#.*)?$").matchEntire(input.trim())
                ?: throw ShelfException(ShelfFailure.InvalidLink)
            val key = match.groupValues[2].split('&').firstOrNull { it.startsWith("resourcekey=") }?.substringAfter('=')
            if (key != null && !Regex("[A-Za-z0-9_-]{1,200}").matches(key)) throw ShelfException(ShelfFailure.InvalidLink)
            return DriveFolderLink(match.groupValues[1], key)
        }
    }
}

private val shelfAccentFold = listOf("aàáạảãâầấậẩẫăằắặẳẵ", "eèéẹẻẽêềếệểễ", "iìíịỉĩ", "oòóọỏõôồốộổỗơờớợởỡ", "uùúụủũưừứựửữ", "yỳýỵỷỹ", "dđ")
    .flatMap { group -> group.map { it to group.first() } }.toMap()
private val shelfWhitespace = Regex("\\s+")
fun shelfSearchKey(value: String): String = buildString {
    for(c in value.lowercase()) {
        when {
            c in '\u0300'..'\u036f' -> Unit
            c.isLetterOrDigit() -> append(shelfAccentFold[c] ?: c)
            else -> append(' ')
        }
    }
}.trim().replace(shelfWhitespace," ")

/** Filename title only; author stays searchable through the full filename key. */
fun shelfTitleKey(value: String): String {
    val stem=value.trim().replace(Regex("\\.epub$",RegexOption.IGNORE_CASE),"")
    val separator=Regex("\\s+[-–—]\\s+").findAll(stem).lastOrNull()?.range?.first
    return shelfSearchKey(if(separator==null)stem else stem.substring(0,separator)).ifBlank { shelfSearchKey(value) }
}

data class ShelfSource(val id: String, val name: String, val resourceKey: String? = null, val updatedAt: Long = 0, val entryCount: Long = 0, val pending: Boolean = false)
data class ShelfEntry(val sourceId: String, val fileId: String, val name: String, val size: Long = 0, val modifiedTime: String = "", val resourceKey: String? = null, val bookId: Long? = null)
data class ShelfSearchPage(val entries: List<ShelfEntry>, val hasMore: Boolean)
enum class ShelfFailure { InvalidLink, InvalidCursor, Authentication, AccessDenied, RateLimited, Network, InvalidFile, FileTooLarge, ImportFailed, Unsupported, Configuration }
class ShelfException(val reason: ShelfFailure) : Exception(reason.name)
enum class ShelfTask { Syncing, Downloading, Importing, Paused, Failed }
data class ShelfOperation(val task: ShelfTask, val processed: Long = 0, val bytes: Long = 0, val totalBytes: Long? = null, val failure: ShelfFailure? = null)

interface DriveShelfRepository {
    val publicAccess:Boolean get()=false
    val sources: Flow<List<ShelfSource>>
    val operations: StateFlow<Map<String, ShelfOperation>>
    suspend fun add(link: String, name: String): ShelfSource
    suspend fun rename(sourceId: String, name: String)
    suspend fun remove(sourceId: String)
    fun refresh(sourceId: String)
    fun refreshStale()
    fun cancel(key: String)
    fun download(entry: ShelfEntry)
    suspend fun search(query: String, sourceId: String? = null, offset: Int = 0): ShelfSearchPage
}

/** Access token is supplied by platform authorization, never stored in shelf data. */
interface ShelfAccess { suspend fun token(): String; fun invalidate() }
