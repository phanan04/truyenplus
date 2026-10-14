package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ireader.domain.preferences.prefs.UiPreferences
import ireader.i18n.localize
import ireader.i18n.resources.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal fun updatedRecentSearches(query:String,previous:List<String>):List<String> {
    val value=query.trim().take(160)
    return (if(value.length<2)previous else listOf(value)+previous).distinctBy {it.lowercase()}.take(10)
}
private fun decodeSearches(value:String)=runCatching {Json.decodeFromString<List<String>>(value)}.getOrDefault(emptyList()).take(10)
data class ReaderSearchHistory(val queries:List<String>,val record:(String)->Unit,val clear:()->Unit)
@Composable fun rememberReaderSearchHistory(preferences:UiPreferences,shelf:Boolean):ReaderSearchHistory {
    val preference=remember(preferences,shelf){preferences.readerRecentSearches(shelf)}
    val saved by preference.changes().collectAsState(preference.get())
    return ReaderSearchHistory(remember(saved){decodeSearches(saved)},
        {query->preference.set(Json.encodeToString(updatedRecentSearches(query,decodeSearches(preference.get()))))},
        {preference.set("[]")})
}
@Composable fun ReaderRecentSearches(queries:List<String>,onQuery:(String)->Unit,onClear:()->Unit) {
    if(queries.isEmpty())return
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
            Text(localize(Res.string.tp_search_recent),Modifier.padding(top=12.dp),style=MaterialTheme.typography.labelLarge)
            TextButton(onClear,modifier=Modifier.heightIn(min=48.dp)){Text(localize(Res.string.clear))}
        }
        LazyRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            items(queries,key={it}) {query->SuggestionChip({onQuery(query)},label={Text(query,maxLines=1)},modifier=Modifier.heightIn(min=48.dp))}
        }
    }
}
