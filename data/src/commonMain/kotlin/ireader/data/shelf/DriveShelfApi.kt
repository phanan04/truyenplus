package ireader.data.shelf

import ireader.domain.models.shelf.*
import io.ktor.client.HttpClient
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import okio.BufferedSink

class ShelfPublicCredentials(val key:String,val packageName:String,val certificateSha1:String)

expect fun createShelfPublicCredentials():ShelfPublicCredentials?

interface ShelfDriveApi {
    val publicAccess:Boolean get()=false
    suspend fun folder(link: DriveFolderLink): DriveItem
    suspend fun page(cursor: ShelfCursor): DrivePage
    suspend fun download(entry: ShelfEntry,sink: BufferedSink,onProgress:(Long,Long?)->Unit)
}
data class DrivePage(val items:List<DriveItem>,val nextToken:String?)

/** Drive v3 only: bounded page reads and streaming media, no cookies/web scraping. */
class DriveShelfApi(private val http: HttpClient,private val access:ShelfAccess,private val publicCredentials:()->ShelfPublicCredentials?={null}):ShelfDriveApi {
    override val publicAccess:Boolean get()=publicCredentials()!=null
    private suspend fun HttpRequestBuilder.authorize() {
        val credentials=publicCredentials()
        if(credentials!=null) {
            parameter("key",credentials.key)
            header("X-Android-Package",credentials.packageName)
            header("X-Android-Cert",credentials.certificateSha1)
        } else header(HttpHeaders.Authorization,"Bearer ${access.token()}")
    }
    private val json=Json { ignoreUnknownKeys=true }
    private suspend fun request(path:String,keyId:String,key:String?,params:Map<String,String>,block:suspend (HttpResponse)->Unit) {
        repeat(4) { attempt ->
            var retry=false
            withTimeout(60_000) {
                http.prepareGet("https://www.googleapis.com/drive/v3/$path") {
                    authorize()
                    if(!key.isNullOrBlank()) header("X-Goog-Drive-Resource-Keys","$keyId/$key")
                    params.forEach { (k,v)->parameter(k,v) }
                }.execute { response ->
                    when(response.status.value) {
                        200 -> block(response)
                        400 -> throw ShelfException(if(params.containsKey("pageToken"))ShelfFailure.InvalidCursor else ShelfFailure.Network)
                        401 -> {if(!publicAccess)access.invalidate();throw ShelfException(if(publicAccess)ShelfFailure.Configuration else ShelfFailure.Authentication)}
                        403 -> {
                            val error=response.bodyAsText().take(10000)
                            if("rateLimitExceeded" in error || "userRateLimitExceeded" in error) retry=true
                            else if("accessNotConfigured" in error || "SERVICE_DISABLED" in error) throw ShelfException(ShelfFailure.Configuration)
                            else throw ShelfException(ShelfFailure.AccessDenied)
                        }
                        404 -> throw ShelfException(ShelfFailure.AccessDenied)
                        429,500,502,503,504 -> retry=true
                        else -> throw ShelfException(ShelfFailure.Network)
                    }
                }
            }
            if(!retry)return
            if(attempt==3)throw ShelfException(ShelfFailure.RateLimited)
            delay((1000L shl attempt))
        }
    }
    private fun item(o:JsonObject):DriveItem {
        fun text(k:String)=o[k]?.jsonPrimitive?.contentOrNull
        val id=text("id")?:throw ShelfException(ShelfFailure.Network)
        require(Regex("[A-Za-z0-9_-]{1,200}").matches(id))
        return DriveItem(id,text("name")?:"",text("mimeType")?:"",text("size")?.toLongOrNull()?:0,text("modifiedTime")?:"",text("resourceKey"))
    }
    override suspend fun folder(link:DriveFolderLink):DriveItem {
        var result:DriveItem?=null
        request("files/${link.folderId}",link.folderId,link.resourceKey,mapOf("fields" to "id,name,mimeType,resourceKey","supportsAllDrives" to "true")) { r -> result=item(json.parseToJsonElement(r.bodyAsText()).jsonObject) }
        return requireNotNull(result).also { if(!it.isFolder)throw ShelfException(ShelfFailure.InvalidLink) }
    }
    override suspend fun page(cursor:ShelfCursor):DrivePage {
        var result:DrivePage?=null
        val params=mutableMapOf("q" to "'${cursor.folderId}' in parents and trashed=false", "pageSize" to "200","fields" to "nextPageToken,incompleteSearch,files(id,name,mimeType,size,modifiedTime,resourceKey)","supportsAllDrives" to "true","includeItemsFromAllDrives" to "true")
        cursor.token?.let { params["pageToken"]=it }
        request("files",cursor.folderId,cursor.resourceKey,params) { r ->
            val body=r.bodyAsText();if(body.length>2_000_000)throw ShelfException(ShelfFailure.Network)
            val o=json.parseToJsonElement(body).jsonObject
            if(o["incompleteSearch"]?.jsonPrimitive?.booleanOrNull==true)throw ShelfException(ShelfFailure.Network)
            result=DrivePage(o["files"]?.jsonArray?.map { item(it.jsonObject) }?:emptyList(),o["nextPageToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() })
        }
        return requireNotNull(result)
    }
    override suspend fun download(entry:ShelfEntry,sink:BufferedSink,onProgress:(Long,Long?)->Unit) {
        if(entry.size>MAX_EPUB_BYTES)throw ShelfException(ShelfFailure.FileTooLarge)
        // Media is not retried into a partially written file. Caller cleans up and retries explicitly.
        withTimeout(30*60_000L) {
            http.prepareGet("https://www.googleapis.com/drive/v3/files/${entry.fileId}") {
                authorize()
                entry.resourceKey?.let { header("X-Goog-Drive-Resource-Keys","${entry.fileId}/$it") }
                parameter("alt","media");parameter("supportsAllDrives","true")
            }.execute { r ->
                if(r.status.value==401){if(!publicAccess)access.invalidate();throw ShelfException(if(publicAccess)ShelfFailure.Configuration else ShelfFailure.Authentication)}
                if(r.status.value in listOf(403,404))throw ShelfException(ShelfFailure.AccessDenied)
                if(r.status.value==429)throw ShelfException(ShelfFailure.RateLimited)
                if(r.status.value!=200)throw ShelfException(ShelfFailure.Network)
                val total=r.headers[HttpHeaders.ContentLength]?.toLongOrNull()?:entry.size.takeIf { it>0 }
                if(total!=null && total>MAX_EPUB_BYTES)throw ShelfException(ShelfFailure.FileTooLarge)
                val channel=r.bodyAsChannel();val buffer=ByteArray(64*1024);var count=0L
                while(true) {
                    val n=channel.readAvailable(buffer,0,buffer.size);if(n<0)break;if(n==0)continue
                    count+=n;if(count>MAX_EPUB_BYTES)throw ShelfException(ShelfFailure.FileTooLarge)
                    sink.write(buffer,0,n);onProgress(count,total)
                }
                sink.flush()
                if(count==0L || (total!=null && count!=total))throw ShelfException(ShelfFailure.InvalidFile)
            }
        }
    }
    companion object { const val MAX_EPUB_BYTES=512L*1024*1024 }
}
