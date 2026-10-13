package ireader.domain.models.editing

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable data class EditingRule(val find: String, val replace: String, val regex: Boolean=false, val wholeWord: Boolean=false) {
    fun pattern(): String {
        require(find.isNotBlank() && find.length<=500 && replace.length<=2000)
        val body=if(regex)find else Regex.escape(find)
        val pattern=if(wholeWord) "(?<![\\p{L}\\p{N}_])(?:$body)(?![\\p{L}\\p{N}_])" else body
        Regex(pattern) // Reject invalid expressions before saving a rule.
        return pattern
    }
    fun replacement(): String = if(regex)replace else Regex.escapeReplacement(replace)
    fun metadata(): String = PREFIX + Json.encodeToString(this)
    companion object {
        private const val PREFIX="truyenplus-rule:"
        fun fromMetadata(value:String?): EditingRule? = value?.takeIf {it.startsWith(PREFIX)}?.let {runCatching{Json.decodeFromString<EditingRule>(it.removePrefix(PREFIX))}.getOrNull()}
    }
}
