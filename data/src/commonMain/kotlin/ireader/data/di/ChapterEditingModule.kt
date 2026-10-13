package ireader.data.di

import ireader.data.editing.*
import ireader.domain.models.editing.*
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.koin.dsl.module

val chapterEditingModule = module {
    single<ChapterEditingStore> { SqlChapterEditingStore(get(),get()) }
    single<EditingSettings> { SecureEditingSettings { get() } }
    single<ChapterEditingApi> {
        // Separate client: keys/text must never enter the reader's HTTP disk cache/logs.
        CloudChapterEditingApi(HttpClient { install(HttpTimeout) { connectTimeoutMillis=20_000;requestTimeoutMillis=120_000;socketTimeoutMillis=120_000 } })
    }
    factory { EditChapter(get(),get()) }
}
