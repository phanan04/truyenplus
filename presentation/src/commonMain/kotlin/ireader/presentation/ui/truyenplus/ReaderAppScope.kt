package ireader.presentation.ui.truyenplus

enum class ReaderFeature { Library, Reading, Progress, TextToSpeech, EbookFiles, Discovery }
enum class ReaderDestination { Home, Library, Settings, Discover }

/** Reader features plus the explicitly requested public discovery catalog. */
object ReaderAppScope {
    val features = ReaderFeature.entries.toSet()
    val destinations = listOf(ReaderDestination.Home, ReaderDestination.Discover, ReaderDestination.Library, ReaderDestination.Settings)
    private val screens = setOf("main", "category", "fontSettings", "ttsEngineManager", "textReplacement")
    fun allows(route: String): Boolean = route in screens ||
        Regex("bookDetail/-?\\d+").matches(route) ||
        Regex("reader/-?\\d+/-?\\d+").matches(route) ||
        Regex("ttsV2/-?\\d+/-?\\d+/-?\\d+/-?\\d+").matches(route)
}
