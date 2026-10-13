package ireader.presentation.ui.truyenplus

import ireader.data.discovery.TruyenFullMetadataLookup
import ireader.domain.models.entities.Book
import kotlin.test.*
import kotlinx.coroutines.runBlocking

class BookMetadataLookupTest {
    private val html="""<html><head><link rel="canonical" href="https://truyenfull.live/van-co-than-de/"/></head><body><div id="truyen"><h3 class="title" itemprop="name">Vạn Cổ Thần Đế</h3><div class="book"><img itemprop="image" src="https://img.example.org/cover.jpg"/></div><div class="info"><a itemprop="author">Phi Thiên Ngư</a><a itemprop="genre">Tiên Hiệp</a></div><div class="desc-text" itemprop="description">Giới thiệu từ trang nguồn.<script>tracking()</script></div></div></body></html>"""
    @Test fun retrievesRealSourceFieldsAndPreservesLocalIdentity()=runBlocking {
        var requested=""
        val lookup=TruyenFullMetadataLookup { requested=it;html }
        val book=Book(id=7,title="Vạn Cổ Thần Đế",author="Phi Thiên Ngư",sourceId=-1,key="epub-original",favorite=true,flags=3)
        val result=requireNotNull(lookup.find(book)).book
        assertEquals("https://truyenfull.live/van-co-than-de/",requested)
        assertEquals("Giới thiệu từ trang nguồn.",result.description)
        assertEquals("https://img.example.org/cover.jpg",result.cover)
        assertEquals(book.id,result.id);assertEquals(book.key,result.key);assertEquals(book.sourceId,result.sourceId)
        assertEquals(book.flags,result.flags);assertTrue(result.favorite)
    }
    @Test fun rejectsDifferentAuthorAndWrongCanonicalBook()=runBlocking {
        val book=Book(id=7,title="Vạn Cổ Thần Đế",author="Tác giả khác",sourceId=-1,key="epub")
        assertNull(TruyenFullMetadataLookup { html }.find(book))
        assertNull(TruyenFullMetadataLookup { html.replace("/van-co-than-de/", "/truyen-khac/") }.find(book.copy(author="Phi Thiên Ngư")))
    }
}
