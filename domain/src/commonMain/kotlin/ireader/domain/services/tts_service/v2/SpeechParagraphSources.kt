package ireader.domain.services.tts_service.v2

import ireader.core.source.model.Page
import ireader.core.source.model.Text

data class SpeechParagraphSources(val texts: List<String>, val sources: List<Int>)

/** Preserve the reader page index through HTML cleanup, splitting and text rules. */
suspend fun assembleSpeechParagraphs(pages: List<Page>, transform: suspend (String) -> List<String>): SpeechParagraphSources {
    val texts = mutableListOf<String>(); val sources = mutableListOf<Int>()
    pages.forEachIndexed { index, page ->
        if (page is Text && !page.text.contains("PLACEHOLDER_DO_NOT_DISPLAY_THIS_TEXT_TO_USER")) {
            val clean = page.text.replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<p[^>]*>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<")
                .replace("&gt;", ">").replace("&quot;", "\"")
            for (line in clean.split(Regex("\n+")).map { it.trim() }.filter { it.isNotBlank() }) {
                for (processed in transform(line)) {
                    if (processed.isNotBlank() && !processed.contains("PLACEHOLDER_DO_NOT_DISPLAY_THIS_TEXT_TO_USER")) {
                        texts += processed; sources += index
                    }
                }
            }
        }
    }
    return SpeechParagraphSources(texts, sources)
}
