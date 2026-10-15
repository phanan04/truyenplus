package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksAccountFlowTest {
    @Test fun accountDismissesAndRetainsAllExistingUtilityActions() {
        var dismissed=false
        scene {ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{},onImport={},onEditing={},onVoices={},onDismiss={dismissed=true})}.use {s->
            render(s)
            node(s,"books-account-close").config[SemanticsActions.OnClick].action!!.invoke()
            assertTrue(dismissed)
            assertNotNull(node(s,"settings-general"))
        }
    }
    @Test fun collectionChoiceOpensAGroupedSheetAndKeepsItsExactSelectedScope() {
        var choice=-1
        scene {ReaderCollectionPicker(listOf("Tất cả","Yêu thích"),1,{choice=it},{})}.use {s->
            render(s);node(s,"library-collections").config[SemanticsActions.OnClick].action!!.invoke();s.render(2_000_000_000);s.render(3_000_000_000)
            assertNotNull(node(s,"books-collection-sheet"))
            assertEquals(true,node(s,"collection-choice-1").config.getOrNull(SemanticsProperties.Selected))
            node(s,"collection-choice-0").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(0,choice)
        }
    }
    private fun scene(content:@Composable ()->Unit)=ImageComposeScene(393,1000) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {content()}}}}
    private fun render(s:ImageComposeScene){s.render(0);s.render(1_000_000_000)}
    private fun node(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
