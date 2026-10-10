import ireader.presentation.ui.truyenplus.ReaderAppScope
import ireader.presentation.ui.truyenplus.ReaderDestination
fun main() {
    check(ReaderAppScope.features.size == 6)
    check(ReaderDestination.entries.size == 4)
    check(ReaderDestination.entries.map { it.name } == listOf("Home", "Library", "Settings", "Discover"))
    check(ReaderAppScope.allows("main"))
    check(ReaderAppScope.allows("bookDetail/12"))
    check(ReaderAppScope.allows("reader/12/-1"))
    check(!ReaderAppScope.allows("globalSearch"))
    check(!ReaderAppScope.allows("explore/1"))
    check(!ReaderAppScope.allows("updates"))
    check(!ReaderAppScope.allows("repository"))
    check(!ReaderAppScope.allows("pluginManagement"))
    check(!ReaderAppScope.allows("communityHub"))
    println("PASS: reader features plus discovery; unrelated source routes excluded")
}
