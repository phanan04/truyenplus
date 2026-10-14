package ireader.presentation.ui.truyenplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.home.library.components.TruyenPlusImportContent
import kotlin.test.*
@OptIn(ExperimentalComposeUiApi::class)
class ReaderImportPickerTest {
    @Test fun eachFormatHasOnlyOneReachableAction() {
        var epub=0;var pdf=0
        val scene=ImageComposeScene(320,600) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
            MaterialTheme {TruyenPlusImportContent({epub++},{pdf++},{})}
        }}
        try {
            scene.render();scene.render()
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            assertEquals(3,all.count {it.config.getOrNull(SemanticsActions.OnClick)!=null})
            for(tag in listOf("import-epub","import-pdf"))all.first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,epub);assertEquals(1,pdf)
        }finally {scene.close()}
    }
}
