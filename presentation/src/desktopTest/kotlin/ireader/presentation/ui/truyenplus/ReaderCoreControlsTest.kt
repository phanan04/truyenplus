package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderCoreControlsTest {
    @Test fun readingFooterStaysOneCompactRowAndOffersAppearanceNearTheThumb() {
        for((width,font) in listOf(240 to 2f,280 to 2f,320 to 1f,320 to 2f,600 to 1.5f,800 to 2f)) {
            scene(width,200,font) {ReaderChapterFooter(0,100,false,false,{},{},{},{},{},{})}.use {s ->
                s.render(0);s.render(1_000_000_000)
                val footer=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-footer-capsule"}
                assertTrue(footer.boundsInRoot.height<=84,"Footer crowds reading viewport at $width/$font: ${footer.boundsInRoot}")
                assertNotNull(nodes(s).firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-appearance-shortcut"})
                for(n in nodes(s).filter {it.config.getOrNull(SemanticsActions.OnClick)!=null}) {
                    assertTrue(n.boundsInRoot.width>=47 && n.boundsInRoot.height>=47)
                    assertTrue(n.boundsInRoot.left>=0 && n.boundsInRoot.right<=width)
                }
            }
        }
    }
    @Test fun timerHasDirectPresetsAndRetainsItsPlaybackDependentState() {
        var minutes=15
        scene(360,440) {ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,true,15,ListeningAudioState(),{},{},{},{},{},{},{},{},{},{minutes=it})}.use {s ->
            s.render(0);s.render(1_000_000_000)
            click(s,"listen-page-timer");s.render(2_000_000_000)
            for(value in listOf(15,30,45,60)) {
                click(s,"listen-timer-$value");assertEquals(value,minutes)
            }
        }
    }
    @Test fun paragraphSpacingCanBeAdjustedIndependentlyOfLineHeight() {
        scene(360,440) {ReaderAppearanceAdvancedContent(ReaderAppearanceUi(),ReaderAppearanceActions())}.use {s ->
            s.render(0);s.render(1_000_000_000)
            click(s,"appearance-page-spacing");s.render(2_000_000_000)
            assertNotNull(nodes(s).firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-paragraph-increase"})
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
        }
    }
    private fun scene(width:Int,height:Int,font:Float=1f,content:@androidx.compose.runtime.Composable ()->Unit)=ImageComposeScene(width,height,Density(1f,font)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {content()}}
    }
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
    private fun click(s:ImageComposeScene,tag:String) {
        nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action!!.invoke()
    }
}
