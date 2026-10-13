package ireader.data.shelf
import ireader.domain.models.shelf.*
import ireader.domain.usecases.epub.EpubMetadataReader
import java.util.zip.ZipFile
import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.parser.Parser
actual fun readShelfEpubMetadata(path:String): EpubMetadataReader.Metadata {
 try {
  ZipFile(path).use { zip ->
   require(zip.size()<=100000)
   fun text(name:String):String? {
    val e=zip.getEntry(name)?:return null
    if(e.size>2_000_000)throw ShelfException(ShelfFailure.InvalidFile)
    val bytes=zip.getInputStream(e).use { input ->
     val out=java.io.ByteArrayOutputStream();val buffer=ByteArray(8192)
     while(true){val n=input.read(buffer);if(n<0)break;out.write(buffer,0,n);if(out.size()>2_000_000)throw ShelfException(ShelfFailure.InvalidFile)}
     out.toByteArray()
    }
    if(bytes.size>2_000_000)throw ShelfException(ShelfFailure.InvalidFile)
    return bytes.toString(Charsets.UTF_8)
   }
   val container=text("META-INF/container.xml")
   val opf=container?.let { Ksoup.parse(it,Parser.xmlParser()).selectFirst("rootfile[full-path]")?.attr("full-path") }
       ?:zip.entries().asSequence().firstOrNull { it.name.endsWith(".opf",true) }?.name
       ?:throw ShelfException(ShelfFailure.InvalidFile)
   return EpubMetadataReader.read(opf,text(opf)?:throw ShelfException(ShelfFailure.InvalidFile),::text).also { if(it.title.isBlank())throw ShelfException(ShelfFailure.InvalidFile) }
  }
 }catch(e:ShelfException){throw e}catch(_:Exception){throw ShelfException(ShelfFailure.InvalidFile)}
}
