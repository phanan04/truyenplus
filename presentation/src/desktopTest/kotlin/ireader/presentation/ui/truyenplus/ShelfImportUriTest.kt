package ireader.presentation.ui.truyenplus

import ireader.domain.models.common.Uri
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfImportUriTest {
    @Test fun downloadedEpubUsesPlatformFileUriWithoutLosingSpecialCharacters() {
        val file = File.createTempFile("truyện # 100% ", ".epub")
        try {
            file.writeText("epub")
            val uri = Uri.fromFilePath(file.absolutePath)
            // Desktop EPUB importer consumes the raw file path; Android uses a file:// URI.
            assertEquals(file.absolutePath, uri.path)
            assertEquals("epub", File(uri.toString()).readText())
        } finally {
            file.delete()
        }
    }
}
