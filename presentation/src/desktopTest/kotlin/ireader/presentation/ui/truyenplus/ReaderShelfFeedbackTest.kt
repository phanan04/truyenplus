package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.shelf.*
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderShelfFeedbackTest {
    @Test fun incompleteIndexDoesNotClaimThatNoBookMatchesAndCanResume() {
        val s=LocalizeHelper();var resumed:String?=null
        scene(listOf(ShelfSource("root","Nguồn",updatedAt=1,entryCount=18000,pending=true)),null,{resumed=it}).use {scene ->
            scene.render(0);scene.render(1_000_000_000)
            val all=nodes(scene)
            assertFalse(all.any {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==s.localize(Res.string.tp_shelf_no_results)}==true})
            val action=all.firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-resume-index-root"}
            assertNotNull(action)
            action.config[SemanticsActions.OnClick].action!!.invoke();assertEquals("root",resumed)
        }
    }
    @Test fun anUnselectedSyncingSourceDoesNotMakeCompleteResultsLookIncomplete() {
        scene(listOf(ShelfSource("root","Đã quét",updatedAt=1),ShelfSource("other","Đang quét",pending=true)),"root",{}).use {s ->
            s.render(0);s.render(1_000_000_000)
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-index-status"})
        }
    }
    private fun scene(sources:List<ShelfSource>,selected:String?,refresh:(String)->Unit)=ImageComposeScene(360,800) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
            MaterialTheme {ReaderShelfScreen(sources,"van co",ShelfSearchPage(emptyList(),false),false,selected,emptyMap(),
                onQuery={},onSource={},onAdd={},onRefresh=refresh,onRemove={},onDownload={},onBook={},onLibrary={},onPrevious={},onNext={},onConnect={},onCancel={},showConnect=false)}
        }
    }
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
}
