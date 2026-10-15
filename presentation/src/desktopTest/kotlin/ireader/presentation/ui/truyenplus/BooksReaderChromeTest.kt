package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksReaderChromeTest {
    @Test fun readingToolsAndChapterNavigationFloatInSeparateReachableClusters() {
        var previous=0
        ImageComposeScene(393,200) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderChapterFooter(1,3,false,false,{previous++},{},{},{},{},{})
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val reading=node(s,"reader-toolbar-reading").boundsInRoot
            val navigation=node(s,"reader-toolbar-navigation").boundsInRoot
            assertTrue(reading.right<navigation.left && reading.height>=48 && navigation.height>=48)
            node(s,"reader-previous").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(1,previous)
        }
    }
    private fun node(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
