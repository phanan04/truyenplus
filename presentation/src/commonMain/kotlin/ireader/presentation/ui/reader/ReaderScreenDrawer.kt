package ireader.presentation.ui.reader

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ireader.domain.models.entities.Chapter

@Composable
fun ReaderScreenDrawer(
        modifier: Modifier = Modifier,
        relativeTime: Int = 0,
        dateFormat: String = "",
        chapter: Chapter?,
        onChapter: (chapter: Chapter) -> Unit,
        chapters: List<Chapter>,
        onReverseIcon: () -> Unit,
        drawerScrollState: LazyListState,
        onMap: (LazyListState) -> Unit,
) {
    ireader.presentation.ui.truyenplus.ReaderChapterContents(chapters,chapter?.id,onChapter,modifier,
        onReverse=onReverseIcon,listState=drawerScrollState)
}
