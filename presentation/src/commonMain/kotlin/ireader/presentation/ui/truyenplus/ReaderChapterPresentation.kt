package ireader.presentation.ui.truyenplus

/** Presentation only: keep imported chapter names/content unchanged in the database. */
internal fun readerChapterDisplayTitle(title: String): String =
    title.replace(Regex("^(Chương\\s+\\d+\\s*:\\s*)(?:\\1)+", RegexOption.IGNORE_CASE), "$1").trim()
