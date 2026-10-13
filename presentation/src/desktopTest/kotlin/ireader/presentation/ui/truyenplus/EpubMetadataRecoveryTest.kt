package ireader.presentation.ui.truyenplus

import ireader.domain.usecases.epub.EpubMetadataReader
import kotlin.test.*
import okio.Path.Companion.toPath

class EpubMetadataRecoveryTest {
    @Test fun epub2CoverMetaAndSvgRelativeHrefAreResolved() {
        val opf = """<package><metadata xmlns:dc="urn:test"><dc:title>Vạn Cổ Thần Đế</dc:title><dc:creator>Phi Thiên Ngư</dc:creator><meta name="cover" content="art"/></metadata><manifest><item id="art" href="Images/Vạn%20Cổ.jpg" media-type="image/jpeg"/></manifest></package>"""
        val result = EpubMetadataReader.read("OEBPS/content.opf", opf) { null }
        assertEquals("Vạn Cổ Thần Đế", result.title)
        assertEquals("Phi Thiên Ngư", result.author)
        assertEquals("OEBPS/Images/Vạn Cổ.jpg", result.coverPath)
        val brokenMeta = opf.replace("content=\"art\"", "content=\"missing\"")
            .replace("</package>", "<guide><reference type=\"cover\" href=\"Text/cover.xhtml\"/></guide></package>")
        val svg = """<html><body><svg><image xlink:href="../Images/Vạn%20Cổ.jpg"/></svg></body></html>"""
        assertEquals("OEBPS/Images/Vạn Cổ.jpg", EpubMetadataReader.read("OEBPS/content.opf", brokenMeta) { if(it=="OEBPS/Text/cover.xhtml") svg else null }.coverPath)
    }
    @Test fun descriptionPreservesTextAndUnsafePathsAreRejected() {
        val result = EpubMetadataReader.read("OPS/book.opf", """<package><metadata><title>A</title><description>&lt;p&gt;Giới thiệu thật.&lt;/p&gt;</description></metadata><manifest><item properties="cover-image" href="../../outside.jpg"/></manifest></package>""") { null }
        assertEquals("Giới thiệu thật.", result.description)
        assertNull(result.coverPath)
        val empty=EpubMetadataReader.read("book.opf", "<package><metadata><title>A</title></metadata></package>") { null }
        assertEquals("", empty.description)
    }
    @Test fun repairingExistingEpubDoesNotDeleteOrRewriteChapters() = kotlinx.coroutines.runBlocking {
        val folder = java.nio.file.Files.createTempDirectory("epub-repair").toFile()
        val existing = ireader.domain.models.entities.Book(id=42,sourceId=ireader.core.source.LocalSource.SOURCE_ID,
            title="Truyện A",author="Tác giả A",key="old-import-123",favorite=true,flags=7,isPinned=true,chapterPage=9)
        var updated: ireader.domain.models.entities.Book? = null
        fun <T> proxy(type: Class<T>, call: (String, Array<out Any?>?) -> Any?): T = type.cast(
            java.lang.reflect.Proxy.newProxyInstance(type.classLoader,arrayOf(type)) { _, method, args -> call(method.name,args) })
        val books=proxy(ireader.domain.data.repository.BookRepository::class.java) { name,args -> when(name) {
            "findDuplicateBook", "findBookById" -> existing
            "updateBook" -> { updated=args!![0] as ireader.domain.models.entities.Book;Unit }
            else -> error("Unexpected book operation: $name")
        } }
        val chapters=proxy(ireader.domain.data.repository.ChapterRepository::class.java) { name,_ -> error("Must preserve chapters: $name") }
        val storage=proxy(ireader.domain.storage.StorageManager::class.java) { name,args ->
            require(name=="getSubDirectory");java.io.File(folder,args!![0].toString()).path.toPath()
        }
        val cache=proxy(ireader.domain.storage.CacheManager::class.java) { name,_ -> error("Covers must not use disposable cache: $name") }
        val simple=proxy(ireader.domain.usecases.files.GetSimpleStorage::class.java) { name,_ -> error("Unused: $name") }
        try {
            val file=java.io.File(folder,"book.epub")
            java.util.zip.ZipOutputStream(file.outputStream()).use { zip ->
                fun entry(name:String,data:ByteArray) { zip.putNextEntry(java.util.zip.ZipEntry(name));zip.write(data);zip.closeEntry() }
                entry("book.opf", """<package><metadata><title>Truyện A</title><creator>Tác giả A</creator><description>Giới thiệu trong EPUB.</description><meta name="cover" content="art"/></metadata><manifest><item id="art" href="cover.png" media-type="image/png"/></manifest></package>""".encodeToByteArray())
                entry("cover.png", java.util.Base64.getDecoder().decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6tokAAAAASUVORK5CYII="))
            }
            val importer=ireader.domain.usecases.epub.ImportEpub(books,chapters,simple,cache,storage)
            importer.parse(listOf(ireader.domain.models.common.Uri(file.path)))
            val book=requireNotNull(updated)
            assertEquals(existing.id,book.id);assertEquals(existing.key,book.key)
            assertTrue(book.favorite && book.isPinned);assertEquals(7L,book.flags);assertEquals(9,book.chapterPage)
            assertEquals("Giới thiệu trong EPUB.",book.description)
            assertTrue(java.io.File(book.cover).isFile)
            updated=null;importer.refreshMetadata(42,ireader.domain.models.common.Uri(file.path))
            assertEquals(42L,requireNotNull(updated).id)
        } finally { folder.deleteRecursively() }
    }

}
