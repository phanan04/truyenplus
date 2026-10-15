package ireader.presentation.ui.truyenplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.shelf.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
@OptIn(ExperimentalComposeUiApi::class)
class BooksShelfNavigationTest {
 @Test fun repositoryOpensTheSharedSearchInsteadOfCreatingAnotherSearchWorkspace() {
  var opened=false;var edited=false
  ImageComposeScene(393,900) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {ReaderBackdrop {
   ReaderShelfScreen(listOf(ShelfSource("root","Kho truyện")),"",ShelfSearchPage(emptyList(),false),false,null,emptyMap(),
    onQuery={edited=true},onSource={},onAdd={},onRefresh={},onRemove={},onDownload={},onBook={},onLibrary={},onPrevious={},onNext={},onConnect={},onCancel={},
    root=true,onSettings={},onSearchRequested={opened=true})
  }}}}.use {s->
   s.render(0);s.render(1_000_000_000)
   fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
   val nodes=s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
   assertFalse(nodes.any {it.config.getOrNull(SemanticsActions.SetText)!=null})
   nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="books-open-search"}.config[SemanticsActions.OnClick].action!!.invoke()
   assertTrue(opened);assertFalse(edited)
  }
 }
}
