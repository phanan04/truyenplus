package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class AppleBooksFlowTest {
    @Test fun collectionChooserKeepsSelectedCollectionAndDoesNotSelectBeforeTap() {
        var chosen=-1
        ImageComposeScene(360,700,Density(1f,1.5f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
                ReaderCollectionPicker(listOf("Tất cả","Truyện yêu thích","Đã hoàn thành"),1,{chosen=it},{})
            }}
        }.use {scene->
            scene.render(0);scene.render(1_000_000_000)
            fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
            fun nodes()=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
            assertEquals(-1,chosen)
            nodes().first {it.config.getOrNull(SemanticsProperties.TestTag)=="library-collections"}.config[SemanticsActions.OnClick].action!!.invoke()
            scene.render(2_000_000_000);scene.render(3_000_000_000)
            val selected=nodes().first {it.config.getOrNull(SemanticsProperties.TestTag)=="collection-choice-1"}
            assertTrue(selected.config[SemanticsProperties.Selected])
            val last=nodes().first {it.config.getOrNull(SemanticsProperties.TestTag)=="collection-choice-2"}
            // Popup geometry is provided by a native window; ImageComposeScene returns NaN.
            // Its touch bounds are verified in the Android device test instead.
            last.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(2,chosen)
        }
    }
}
