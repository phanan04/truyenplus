package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.library.LibrarySort
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderOrganizationControlsTest {
    @Test fun recentSearchesKeepLatestSpellingAndHaveBoundedStorage() {
        assertEquals(listOf("Cầu Ma","Khác"),updatedRecentSearches("  Cầu Ma  ",listOf("cầu ma","Khác")))
        assertEquals(listOf("Cầu Ma"),updatedRecentSearches(" ",listOf("Cầu Ma")))
        assertEquals(10,updatedRecentSearches("Mới",(1..20).map {"Truyện $it"}).size)
    }

    @Test fun searchScopeIsExplicitAndSwitchingItDoesNotInventAnotherNavigationTab() {
        var shelf=false
        scene {ReaderSearchScopePicker(false,{shelf=it})}.use {s ->
            s.render(0);s.render(1_000_000_000)
            click(s,"search-scope-shelf");assertTrue(shelf)
            click(s,"search-scope-library");assertFalse(shelf)
            assertEquals(3,ReaderDestination.entries.size)
        }
    }
    @Test fun sortingDisplaysSelectedTypeAndDirectionAndEmitsTheExactChoice() {
        var type:LibrarySort.Type?=null;var ascending:Boolean?=null
        scene {ReaderSortContent(LibrarySort(LibrarySort.Type.LastRead,false),{type=it},{ascending=it},{})}.use {s ->
            s.render(0);s.render(1_000_000_000)
            assertEquals(true,node(s,"sort-LastRead").config.getOrNull(SemanticsProperties.Selected))
            assertEquals(true,node(s,"sort-descending").config.getOrNull(SemanticsProperties.Selected))
            click(s,"sort-Title");assertEquals(LibrarySort.Type.Title,type)
            click(s,"sort-ascending");assertEquals(true,ascending)
        }
    }
    private fun scene(content:@androidx.compose.runtime.Composable ()->Unit)=ImageComposeScene(360,800) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}
    }
    private fun node(s:ImageComposeScene,tag:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}
    }
    private fun click(s:ImageComposeScene,tag:String) {node(s,tag).config[SemanticsActions.OnClick].action!!.invoke()}
}
