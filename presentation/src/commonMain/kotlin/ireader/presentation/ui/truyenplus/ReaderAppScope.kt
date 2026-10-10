package ireader.presentation.ui.truyenplus

enum class ReaderFeature { Library, Reading, Progress, TextToSpeech, EbookFiles, Discovery }
enum class ReaderDestination(val label: String) { Home("Trang chủ"), Library("Thư viện"), Progress("Tiến độ"), Discover("Khám phá") }

/** Reader features plus the explicitly requested public discovery catalog. */
object ReaderAppScope {
    val features = ReaderFeature.entries.toSet()
    private val screens = setOf("main", "category", "fontSettings", "ttsEngineManager", "textReplacement")
    fun allows(route: String): Boolean = route in screens ||
        Regex("bookDetail/-?\\d+").matches(route) ||
        Regex("reader/-?\\d+/-?\\d+").matches(route) ||
        Regex("ttsV2/-?\\d+/-?\\d+/-?\\d+/-?\\d+").matches(route)
}
