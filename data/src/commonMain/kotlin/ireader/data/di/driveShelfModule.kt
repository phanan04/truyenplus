package ireader.data.di
import ireader.data.shelf.*
import ireader.data.util.AppDataDirectory
import ireader.domain.models.shelf.*
import ireader.domain.data.repository.BookRepository
import ireader.domain.models.common.Uri
import ireader.domain.usecases.epub.ImportEpub
import ireader.core.source.LocalSource
import kotlinx.coroutines.*
import org.koin.dsl.module
import ireader.domain.utils.extensions.currentTimeToLong

val driveShelfModule=module {
 single<ShelfAccess> { createShelfAccess() }
 single { SqlShelfStore(get()) }
 single<ShelfDriveApi> {
  // The general reader HTTP client caches responses; never buffer EPUB media there.
  val http=io.ktor.client.HttpClient {
   install(io.ktor.client.plugins.HttpTimeout) {
    connectTimeoutMillis=20_000;socketTimeoutMillis=60_000
    requestTimeoutMillis=io.ktor.client.plugins.HttpTimeoutConfig.INFINITE_TIMEOUT_MS
   }
  }
  DriveShelfApi(http,get(),::createShelfPublicCredentials)
 }
 single<DriveShelfRepository> {
  val epub=get<ImportEpub>();val books=get<BookRepository>();val store=get<SqlShelfStore>()
  DefaultDriveShelfRepository(store,get(),get(),AppDataDirectory.getPath()/"shelf-downloads",
   CoroutineScope(SupervisorJob()+Dispatchers.Default),importer={path ->
    withContext(Dispatchers.IO) {
     val metadata=readShelfEpubMetadata(path)
     epub.parse(listOf(Uri.parse(path)))
     val id=store.findImportedBook(LocalSource.SOURCE_ID,metadata.title,metadata.author.ifBlank{"Unknown"})?:throw ShelfException(ShelfFailure.ImportFailed)
     val book=books.findBookById(id)?:throw ShelfException(ShelfFailure.ImportFailed)
     if(store.chapterCount(book.id)==0L)throw ShelfException(ShelfFailure.ImportFailed)
     if(!book.favorite)books.updateBook(book.copy(favorite=true))
     book.id
    }
   },now=::currentTimeToLong)
 }
}
