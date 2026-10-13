package ireader.domain.usecases.epub

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Document
import com.fleeksoft.ksoup.parser.Parser
import ireader.core.source.LocalSource
import ireader.core.source.model.MangaInfo
import ireader.core.source.model.Text
import ireader.domain.data.repository.BookRepository
import ireader.domain.data.repository.ChapterRepository
import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.storage.CacheManager
import ireader.domain.storage.StorageManager
import ireader.domain.usecases.files.GetSimpleStorage
import ireader.domain.utils.extensions.currentTimeToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath
import okio.buffer
import java.io.File
import java.util.zip.ZipFile

/**
 * Desktop implementation of EPUB import using pure Kotlin with okio and ksoup
 */
actual class ImportEpub(
    private val bookRepository: BookRepository,
    private val chapterRepository: ChapterRepository,
    private val getSimpleStorage: GetSimpleStorage,
    private val cacheManager: CacheManager,
    private val storageManager: StorageManager
) {
    
    actual suspend fun parse(uris: List<ireader.domain.models.common.Uri>) = withContext(Dispatchers.IO) {
        val errors = mutableListOf<Pair<String, String>>()
        
        uris.forEach { uri ->
            try {
                importEpub(uri)
            } catch (e: Exception) {
                errors.add(uri.uriString to (e.message ?: "Unknown error"))
            }
        }
        
        if (errors.isNotEmpty()) {
            val errorMessage = errors.joinToString("\n") { (path, error) ->
                "${path.toPath().name}: $error"
            }
            throw Exception("Failed to import ${errors.size} file(s):\n$errorMessage")
        }
    }
    
    actual suspend fun refreshMetadata(bookId: Long, uri: ireader.domain.models.common.Uri) = withContext(Dispatchers.IO) {
        val book = bookRepository.findBookById(bookId) ?: error("Book not found")
        val file = File(uri.uriString)
        try {
            ZipFile(file).use { zip ->
                val opf = findOpfFile(zip) ?: error("No OPF file found")
                val xml = zip.getInputStream(opf).bufferedReader().use { it.readText() }
                val doc = Ksoup.parse(xml, Parser.xmlParser())
                val metadata = EpubMetadataReader.read(opf.name, doc) { path -> zip.getEntry(path)?.let { zip.getInputStream(it).bufferedReader().use { r -> r.readText() } } }
                require(metadata.title.equals(book.title, ignoreCase = true)) { "EPUB title does not match this book" }
                val cover = extractCover(zip, doc, book.key)
                bookRepository.updateBook(EpubMetadataReader.merge(book, metadata, cover, currentTimeToLong()))
            }
        } finally { /* Original EPUB is owned by the user. */ }
    }

    private suspend fun importEpub(uri: ireader.domain.models.common.Uri) {
        val file = File(uri.uriString)
        if (!file.exists()) throw Exception("File not found: ${uri.uriString}")
        
        ZipFile(file).use { zip ->
            // Parse OPF file to get metadata and spine
            val opfEntry = findOpfFile(zip) ?: throw Exception("No OPF file found in EPUB")
            val opfContent = zip.getInputStream(opfEntry).bufferedReader().readText()
            val opfDoc = Ksoup.parse(opfContent, Parser.xmlParser())
            
            // Extract metadata
            val metadata = extractMetadata(opfDoc)
            val title = metadata["title"]?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
            val author = metadata["author"]?.takeIf { it.isNotBlank() } ?: "Unknown"
            val description = metadata["description"] ?: ""
            
            // Generate unique key
            val existing = bookRepository.findDuplicateBook(title, LocalSource.SOURCE_ID)?.takeIf {
                it.author.isBlank() || it.author == "Unknown" || it.author.equals(author, ignoreCase = true)
            }
            val key = existing?.key ?: generateBookKey(title, author)
            
            // Extract cover
            val coverPath = extractCover(zip, opfDoc, key)
            
            if (existing != null) {
                val recovered = EpubMetadataReader.read(opfEntry.name, opfDoc) { path -> zip.getEntry(path)?.let { zip.getInputStream(it).bufferedReader().use { r -> r.readText() } } }
                bookRepository.updateBook(EpubMetadataReader.merge(existing, recovered, coverPath, currentTimeToLong()))
                return
            }

            // Create book
            val bookId = Book(
                title = title,
                key = key,
                favorite = true,
                sourceId = LocalSource.SOURCE_ID,
                cover = coverPath,
                author = author,
                status = MangaInfo.PUBLISHING_FINISHED,
                description = description,
                lastUpdate = currentTimeToLong()
            ).let { bookRepository.upsert(it) }
            
            // Extract and insert chapters in streaming batches
            val chapterCount = extractAndInsertChapters(zip, opfDoc, bookId, key)
            if (chapterCount == 0) {
                throw Exception("No readable content found in EPUB")
            }
        }
    }
    
    private fun findOpfFile(zip: ZipFile): java.util.zip.ZipEntry? {
        // Read container.xml to find OPF location
        val containerEntry = zip.getEntry("META-INF/container.xml")
        if (containerEntry != null) {
            val containerXml = zip.getInputStream(containerEntry).bufferedReader().readText()
            val doc = Ksoup.parse(containerXml, Parser.xmlParser())
            val rootfile = doc.selectFirst("rootfile[full-path]")
            val opfPath = rootfile?.attr("full-path")
            if (opfPath != null) {
                return zip.getEntry(opfPath)
            }
        }
        
        // Fallback: search for .opf files
        return zip.entries().asSequence().find { it.name.endsWith(".opf") }
    }
    
    private fun extractMetadata(opfDoc: Document): Map<String, String> {
        val m = EpubMetadataReader.read("book.opf", opfDoc) { null }
        return mapOf("title" to m.title, "author" to m.author, "description" to m.description)
    }

    private fun extractCover(zip: ZipFile, opfDoc: Document, key: String): String {
        val opf = findOpfFile(zip) ?: return ""
        val metadata = EpubMetadataReader.read(opf.name, opfDoc) { path ->
            zip.getEntry(path)?.let { zip.getInputStream(it).bufferedReader().use { r -> r.readText() } }
        }
        val entry = metadata.coverPath?.let { zip.getEntry(it) } ?: return ""
        if (entry.size > 8 * 1024 * 1024 || entry.isDirectory) return ""
        // Persistent storage: cache eviction must not erase library covers.
        val directory = storageManager.getSubDirectory("library_covers")
        FileSystem.SYSTEM.createDirectories(directory)
        val extension = entry.name.substringAfterLast('.', "jpg").lowercase()
        val destination = directory / "${key.hashCode()}.$extension"
        zip.getInputStream(entry).use { input ->
            FileSystem.SYSTEM.sink(destination).buffer().use { sink ->
                val buffer = ByteArray(8192); var total = 0
                while (true) {
                    val count = input.read(buffer); if (count < 0) break
                    total += count; require(total <= 8 * 1024 * 1024) { "Cover is too large" }
                    sink.write(buffer, 0, count)
                }
            }
        }
        return destination.toString()
    }

    private suspend fun extractAndInsertChapters(zip: ZipFile, opfDoc: Document, bookId: Long, key: String): Int {
        val opfPath = findOpfFile(zip)?.name ?: ""
        val basePath = if (opfPath.contains("/")) opfPath.substringBeforeLast("/") + "/" else ""
        
        // Get spine items
        val spineItems = opfDoc.select("spine itemref")
        val manifestItems = opfDoc.select("manifest item").associateBy { it.attr("id") }
        
        val batch = mutableListOf<Chapter>()
        var totalInserted = 0
        val batchSize = 50
        
        spineItems.forEachIndexed { index, itemref ->
            val idref = itemref.attr("idref")
            val manifestItem = manifestItems[idref]
            val href = manifestItem?.attr("href") ?: return@forEachIndexed
            
            val fullPath = basePath + href
            val entry = zip.getEntry(fullPath) ?: return@forEachIndexed
            
            try {
                val html = zip.getInputStream(entry).bufferedReader().readText()
                val doc = Ksoup.parse(html)
                
                // Extract title
                val title = doc.selectFirst("h1, h2, h3, title")?.text()?.trim()
                    ?: "Chapter ${index + 1}"
                
                // Extract content
                val content = extractTextContent(doc)
                
                if (content.isNotEmpty()) {
                    batch.add(
                        Chapter(
                            name = title,
                            key = "${key}_chapter_$index",
                            bookId = bookId,
                            content = content,
                            number = index.toFloat(),
                            dateUpload = currentTimeToLong()
                        )
                    )
                    
                    if (batch.size >= batchSize) {
                        chapterRepository.insertChapters(batch)
                        totalInserted += batch.size
                        batch.clear()
                    }
                }
            } catch (_: Exception) {
                // Silently ignore chapter extraction errors
            }
        }
        
        if (batch.isNotEmpty()) {
            chapterRepository.insertChapters(batch)
            totalInserted += batch.size
            batch.clear()
        }
        
        return totalInserted
    }
    
    private fun extractTextContent(doc: Document): List<Text> {
        return EpubTextExtractor.extractTextContent(doc)
    }
    
    private fun generateBookKey(title: String, author: String): String {
        val sanitized = "${title}_${author}".replace(Regex("[^a-zA-Z0-9]"), "_")
        val timestamp = currentTimeToLong()
        return "${sanitized}_$timestamp"
    }

    actual fun getCacheSize(): String {
        return cacheManager.getCacheSize()
    }

    actual fun removeCache() {
        return cacheManager.clearAllCache()
    }
}