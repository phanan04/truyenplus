package ireader.presentation.ui.truyenplus

import ireader.domain.models.shelf.DriveFolderLink
import ireader.domain.models.shelf.shelfSearchKey
import kotlin.test.*

class DriveShelfTest {
    @Test fun onlyDriveFolderLinksAreAcceptedAndResourceKeyIsPreserved() {
        val link=DriveFolderLink.parse("https://drive.google.com/drive/folders/1qtBLTRHSsPKtRSoGugzlFfXuE-dO7Xn8?hl=vi&resourcekey=abc-123")
        assertEquals("1qtBLTRHSsPKtRSoGugzlFfXuE-dO7Xn8",link.folderId)
        assertEquals("abc-123",link.resourceKey)
        assertEquals(link.folderId,DriveFolderLink.parse("https://drive.google.com/drive/u/0/folders/${link.folderId}").folderId)
        for(url in listOf("https://drive.google.com.evil.com/drive/folders/a","http://drive.google.com/drive/folders/a","https://drive.google.com/file/d/a/view","file:///etc/passwd")) assertFails { DriveFolderLink.parse(url) }
    }
    @Test fun searchHandlesVietnameseAccentsAndSqlWildcards() {
        assertEquals("van co than de",shelfSearchKey("Vạn Cổ Thần Đế"))
        assertEquals("duong",shelfSearchKey("Đường"))
    }
}
