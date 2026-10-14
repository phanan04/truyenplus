package ireader.presentation.ui.truyenplus
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
@OptIn(ExperimentalComposeUiApi::class)
class ReaderAccessibilityTest {
    @Test fun coveredChapterIsNotPartOfTheAccessibleTask() {
        scene {Column {
            Box(Modifier.readerCoveredContent(true)){Text("Covered chapter")}
            Text("Visible task")
        }}.use {s->
            s.render();s.render()
            val texts=nodes(s).flatMap {it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.map {it.text}
            assertFalse("Covered chapter" in texts);assertTrue("Visible task" in texts)
        }
    }
    @Test fun fontSteppersHaveNamesInsteadOfOnlyMinusAndPlus() {
        scene {ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())}.use {s->
            s.render();s.render()
            for(tag in listOf("reader-font-decrease","reader-font-increase")) {
                val n=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}
                assertTrue(n.config.getOrNull(SemanticsProperties.ContentDescription)?.any {it.length>3}==true)
            }
        }
    }
    private fun scene(content:@Composable ()->Unit)=ImageComposeScene(360,800){CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()){MaterialTheme {content()}}}
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
}
