import ireader.presentation.ui.truyenplus.ReaderAppScope
import ireader.presentation.ui.truyenplus.ReaderDestination
fun main() {
    check(ReaderAppScope.features.size == 5)
    check(ReaderDestination.entries.size == 3)
    check(ReaderAppScope.allows("main"))
    check(ReaderAppScope.allows("bookDetail/12"))
    check(ReaderAppScope.allows("reader/12/-1"))
    check(!ReaderAppScope.allows("globalSearch"))
    check(!ReaderAppScope.allows("explore/1"))
    check(!ReaderAppScope.allows("updates"))
    check(!ReaderAppScope.allows("repository"))
    check(!ReaderAppScope.allows("pluginManagement"))
    check(!ReaderAppScope.allows("communityHub"))
    println("PASS: only five reader features and three destinations; online/source/update routes excluded")
}
