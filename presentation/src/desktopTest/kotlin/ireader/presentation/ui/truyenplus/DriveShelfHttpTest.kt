package ireader.presentation.ui.truyenplus
import ireader.data.shelf.*
import ireader.domain.models.shelf.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.runBlocking
import okio.Buffer
import kotlin.test.*
class DriveShelfHttpTest {
 private val access=object:ShelfAccess {override suspend fun token()="test-token";override fun invalidate(){}}
 @Test fun paginationAndResourceKeysUseOfficialApiOnly()=runBlocking {
  val client=HttpClient(MockEngine { req ->
   assertEquals("www.googleapis.com",req.url.host);assertEquals("Bearer test-token",req.headers[HttpHeaders.Authorization])
   assertEquals("root/key",req.headers["X-Goog-Drive-Resource-Keys"]);assertEquals("second",req.url.parameters["pageToken"])
   assertEquals("200",req.url.parameters["pageSize"]);assertEquals("'root' in parents and trashed=false",req.url.parameters["q"])
   respond("""{"files":[{"id":"a","name":"A.epub","mimeType":"application/epub+zip","size":"123","resourceKey":"k"}],"nextPageToken":"third"}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
  })
  try {val page=DriveShelfApi(client,access).page(ShelfCursor("root","key","second"));assertEquals("third",page.nextToken);assertEquals(123,page.items.single().size);assertEquals("k",page.items.single().resourceKey)}finally{client.close()}
 }
 @Test fun incompleteListingCannotReplaceCachedCatalog()=runBlocking {
  val client=HttpClient(MockEngine { respond("""{"files":[],"incompleteSearch":true}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json")) })
  try {assertFailsWith<ShelfException>{DriveShelfApi(client,access).page(ShelfCursor("root",null,null))};Unit}finally{client.close()}
 }
 @Test fun mediaIsStreamedAndOversizedFilesNeverRequestDownload()=runBlocking {
  var requests=0
  val client=HttpClient(MockEngine { requests++;respond("EPUB-BYTES",HttpStatusCode.OK,headersOf(HttpHeaders.ContentLength,"10")) })
  try {
   val api=DriveShelfApi(client,access);val buffer=Buffer();var progress=0L
   api.download(ShelfEntry("root","a","A.epub",10),buffer){n,_->progress=n}
   assertEquals("EPUB-BYTES",buffer.readUtf8());assertEquals(10,progress)
   assertEquals(ShelfFailure.FileTooLarge,assertFailsWith<ShelfException>{api.download(ShelfEntry("root","b","B.epub",DriveShelfApi.MAX_EPUB_BYTES+1),Buffer()){_,_->}}.reason)
   assertEquals(1,requests)
  }finally{client.close()}
 }

 @Test fun expiredPageCursorCanRestartWithoutDroppingCachedFiles()=runBlocking {
  val client=HttpClient(MockEngine { respond("""{"error":{"code":400,"message":"Invalid page token"}}""",HttpStatusCode.BadRequest) })
  try {assertEquals("InvalidCursor",assertFailsWith<ShelfException>{DriveShelfApi(client,access).page(ShelfCursor("root",null,"expired"))}.reason.name)}finally{client.close()}
 }

 @Test fun publicRequestsUseRestrictedKeyWithoutGoogleAccount()=runBlocking {
  val forbidden=object:ShelfAccess {override suspend fun token():String=error("Public sources must not sign in");override fun invalidate(){error("Must not invalidate account")}}
  val client=HttpClient(MockEngine { req ->
   assertNull(req.headers[HttpHeaders.Authorization]);assertEquals("test-public-key",req.url.parameters["key"])
   assertEquals("app.package",req.headers["X-Android-Package"]);assertEquals("ABC123",req.headers["X-Android-Cert"])
   if(req.url.parameters["alt"]=="media")respond("EPUB",HttpStatusCode.OK,headersOf(HttpHeaders.ContentLength,"4"))
   else respond("""{"files":[]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
  })
  try {
   val api=DriveShelfApi(client,forbidden,{ShelfPublicCredentials("test-public-key","app.package","ABC123")})
   assertTrue(api.publicAccess)
   assertTrue(api.page(ShelfCursor("root",null,null)).items.isEmpty())
   val buffer=Buffer();api.download(ShelfEntry("root","a","A.epub",4),buffer){_,_->};assertEquals("EPUB",buffer.readUtf8())
  }finally{client.close()}
 }
}
