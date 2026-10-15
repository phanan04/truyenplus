package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.common.ColorModel
import ireader.domain.preferences.models.ReaderColors
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class BooksAppearanceFlowTest {
    @Test fun appearanceStartsWithQuickTypeAndPapersThenCustomizesInTheSamePane() {
        var size=0;var selected=0L
        val papers=(1L..14L).map {ReaderColors(it,ColorModel(1f,1f,1f),ColorModel(0f,0f,0f))}
        ImageComposeScene(393,500) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderAppearanceContent(ReaderAppearanceUi(themes=papers,themeId=1),ReaderAppearanceActions(onFontSize={size=it},onTheme={selected=it}))
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            assertNotNull(tag(s,"reader-appearance-home"))
            tag(s,"reader-font-increase").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(21,size)
            tag(s,"reader-theme-2").config[SemanticsActions.OnClick].action!!.invoke();assertEquals(2,selected)
            tag(s,"reader-appearance-customize").config[SemanticsActions.OnClick].action!!.invoke();s.render(2_000_000_000)
            assertNotNull(tag(s,"reader-font-picker"))
            assertNotNull(tag(s,"appearance-page-spacing"))
            tag(s,"reader-appearance-home-back").config[SemanticsActions.OnClick].action!!.invoke();s.render(3_000_000_000)
            assertNotNull(tag(s,"reader-appearance-home"))
        }
    }
    @Test fun openingControlsStayFullyReachableWithLargeTypeAndShortWideWindow() {
        val papers=(1L..14L).map {ReaderColors(it,ColorModel(1f,1f,1f),ColorModel(0f,0f,0f))}
        for((width,height,font) in listOf(Triple(320,640,2f),Triple(640,320,1.5f))) {
            ImageComposeScene(width,height,androidx.compose.ui.unit.Density(1f,font)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderListening provides ReaderListeningContext(emptyList(),0,false,{})) {
                    MaterialTheme {ReaderBackdrop {ReaderAppearanceOverlay({}) {ReaderAppearanceContent(ReaderAppearanceUi(themes=papers,themeId=1),ReaderAppearanceActions())}}}
                }
            }.use {s->
                s.render(0);s.render(1_000_000_000)
                val pane=tag(s,"reader-appearance-panel").boundsInRoot
                for(t in listOf("reader-font-increase","reader-paper-next","reader-appearance-customize")) {
                    val b=tag(s,t).boundsInRoot
                    assertTrue(b.height>=47 && b.bottom<=pane.bottom,"$width/$height/$font clipped $t: $b / $pane")
                }
            }
        }
    }
    private fun tag(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}
