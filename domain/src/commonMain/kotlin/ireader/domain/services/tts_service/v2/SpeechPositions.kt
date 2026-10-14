package ireader.domain.services.tts_service.v2

/** Offsets are UTF-16 offsets into the original text, matching Android and Compose. */
data class SpeechSegment(val start: Int, val end: Int) {
    operator fun contains(offset: Int) = offset >= start && offset < end
}
data class SpokenSentencePosition(val paragraphIndex: Int, val start: Int, val end: Int, val text: String)

fun speechChunkRanges(text: String, chunks: List<String>): List<SpeechSegment?> {
    var cursor = 0
    return chunks.map { chunk ->
        val exact = text.indexOf(chunk, cursor)
        val match = if (exact < 0) Regex(chunk.split(Regex("\\s+")).joinToString("\\s*") { Regex.escape(it) }).find(text, cursor) else null
        val start = if (exact >= 0) exact else match?.range?.first
        if (start == null) null else {
            val end = if (exact >= 0) start + chunk.length else match!!.range.last + 1
            cursor = end
            SpeechSegment(start, end)
        }
    }
}
fun speechSegments(text: String, maxChunkLength: Int = -1): List<SpeechSegment> =
    speechChunkRanges(text, TTSSentenceSplitter.split(text, maxChunkLength)).filterNotNull()
fun sentenceAt(text: String, offset: Int): SpeechSegment? = speechSegments(text, Int.MAX_VALUE).firstOrNull { offset in it }

/** Built once per utterance; range callbacks only search the prepared sentence list. */
class SpokenTextIndex(paragraphs: List<String>, indices: List<Int>, spokenText: String) {
    private val length=spokenText.length
    private data class Entry(val global: SpeechSegment, val position: SpokenSentencePosition)
    private val entries = buildList {
        var cursor = 0
        for (index in indices) {
            val paragraph = paragraphs.getOrNull(index) ?: continue
            val content = paragraph.trim()
            val start = spokenText.indexOf(content, cursor)
            if (start < 0) continue
            val leading = paragraph.indexOf(content)
            for (sentence in speechSegments(content, Int.MAX_VALUE)) {
                add(Entry(SpeechSegment(start+sentence.start,start+sentence.end),
                    SpokenSentencePosition(index,leading+sentence.start,leading+sentence.end,content.substring(sentence.start,sentence.end))))
            }
            cursor = start + content.length
        }
    }
    fun at(offset: Int): SpokenSentencePosition? = entries.firstOrNull { offset in it.global }?.position
    fun range(start:Int,end:Int):SpokenSentencePosition? {
        if(start<0 || end<=start || end>length)return null
        val entry=entries.firstOrNull {start in it.global && end<=it.global.end} ?: return null
        val from=start-entry.global.start
        val until=end-entry.global.start
        return entry.position.copy(start=entry.position.start+from,end=entry.position.start+until,
            text=entry.position.text.substring(from,until))
    }
    fun wordAt(event:EngineEvent.RangeStarted):SpokenSentencePosition? =
        if(event.isWordBoundary)range(event.start,event.end) else null
}
fun spokenSentencePosition(paragraphs: List<String>, indices: List<Int>, spokenText: String, offset: Int) =
    SpokenTextIndex(paragraphs,indices,spokenText).at(offset)
