package ireader.presentation.ui.truyenplus

enum class ReaderFeature { Library, Reading, Progress, TextToSpeech, EbookFiles }
enum class ReaderDestination { Home, Library, Settings, Shelf }

/** Local library, reading, listening, progress and ebook files. */
object ReaderAppScope {
    val features = ReaderFeature.entries.toSet()
    val destinations = listOf(ReaderDestination.Home, ReaderDestination.Library, ReaderDestination.Shelf)
    private val screens = setOf("main", "shelf", "category", "fontSettings", "ttsEngineManager", "textReplacement")
    fun allows(route: String): Boolean = route in screens ||
        Regex("bookDetail/-?\\d+").matches(route) ||
        Regex("reader/-?\\d+/-?\\d+").matches(route) ||
        Regex("ttsV2/-?\\d+/-?\\d+/-?\\d+/-?\\d+").matches(route)
}
