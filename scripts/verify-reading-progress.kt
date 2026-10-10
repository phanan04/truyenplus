import ireader.presentation.ui.component.list.layouts.bookReadingProgress

fun main() {
    check(bookReadingProgress(520, 275).percent == 47)
    check(bookReadingProgress(520, 275).readChapters == 245)
    check(bookReadingProgress(520, 275).fraction in 0f..1f)
    check(bookReadingProgress(0, 0).fraction == 0f)
    check(bookReadingProgress(-1, 4).percent == 0)
    check(bookReadingProgress(10, 15).readChapters == 0)
    check(bookReadingProgress(10, -2).percent == 100)
    check(bookReadingProgress(10, null).percent == 0)
    println("PASS: 8 reading progress checks (normal, empty, stale counts, unknown counts)")
}
