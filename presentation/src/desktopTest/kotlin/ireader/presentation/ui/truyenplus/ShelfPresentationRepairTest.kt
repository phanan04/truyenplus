package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.models.shelf.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ShelfPresentationRepairTest {
    @Test fun shelfIsSeparateFromExistingBottomNavigation() {
        assertTrue(ReaderAppScope.allows("shelf"))
        assertEquals(listOf(ReaderDestination.Home, ReaderDestination.Library, ReaderDestination.Settings), ReaderAppScope.destinations)
    }

    @Test fun searchKeepsSourcesOutOfResultsAndRemovesImplementationHints() {
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
        var back = false
        val entry = ShelfEntry("root", "a", "Vạn Cổ Thần Đế - Phi Thiên Ngư.epub")
        ImageComposeScene(360, 800, Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderBackdrop {
                    ReaderShelfScreen(listOf(ShelfSource("root", "Kho của tôi",updatedAt=1,entryCount=1)), "van co", ShelfSearchPage(listOf(entry), false), false, null, emptyMap(),
                        onQuery={}, onSource={}, onAdd={}, onRefresh={}, onRemove={}, onDownload={}, onBook={}, onLibrary={back=true}, onPrevious={}, onNext={}, onConnect={}, onCancel={}, showConnect=false)
                } }
            }
        }.use { scene ->
            scene.render(0); scene.render(1_000_000_000)
            val nodes = nodes(scene)
            val text = nodes.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.joinToString { it.text }
            assertFalse("thế giới riêng" in text)
            assertFalse("Bìa và thông tin" in text)
            assertFalse("Làm mới danh mục" in text)
            assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-manage" })
            nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-back" }.config[SemanticsActions.OnClick].action!!.invoke()
            assertTrue(back)
            nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)=="shelf-manage" }.config[SemanticsActions.OnClick].action!!.invoke()
            scene.render(2_000_000_000);scene.render(3_000_000_000)
            assertTrue(nodes(scene).any { n->n.config.getOrNull(SemanticsProperties.Text)?.any { it.text=="Kho của tôi" }==true })
        }
    }

    private fun nodes(scene: ImageComposeScene): List<SemanticsNode> {
        fun all(n: SemanticsNode): List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap { all(it.rootSemanticsNode) }
    }
}
