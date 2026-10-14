package ireader.presentation.ui.reader

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Insets scroll with the chapter: floating controls never clip the reading viewport. */
@Composable
internal fun ReaderScrollViewport(state: LazyListState, contentPadding: PaddingValues = PaddingValues(0.dp),
    modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: LazyListScope.() -> Unit) {
    LazyColumn(modifier=modifier.fillMaxSize(),state=state,contentPadding=contentPadding,
        horizontalAlignment=horizontalAlignment,content=content)
}
