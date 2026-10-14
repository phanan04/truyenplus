package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderHighlightSettingsTest {
    @Test fun readingTabContainsBothHighlightStylesWithoutScrollingOrClipping() {
        for((width,height,font) in listOf(Triple(320,640,1.5f),Triple(800,600,1f),Triple(640,320,1.5f))) {
            ImageComposeScene(width,height,Density(1f,font)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme {ReaderBackdrop {
                        ReaderListeningDock(true, {
                            ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,30,
                                ListeningAudioState(),{},{},{},{},{},{},{},{},{},{})
                        }, {ReaderCollapsedPlayer(false,{},{},{},{})})
                    }}
                }
            }.use {scene ->
                scene.render(0);scene.render(1_000_000_000)
                fun node(tag:String)=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}
                node("listen-page-reading").config[SemanticsActions.OnClick].action!!.invoke()
                scene.render(2_000_000_000);scene.render(3_000_000_000)
                val player=node("listen-compact-toggle").boundsInRoot
                for(tag in listOf("listen-highlight-sentence","listen-highlight-words")) {
                    val option=node(tag)
                    assertTrue(option.boundsInRoot.height>=47)
                    assertTrue(option.boundsInRoot.bottom<=player.top)
                    assertTrue(option.boundsInRoot.left>=0 && option.boundsInRoot.right<=width)
                }
                assertFalse(nodes(scene).any {it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
            }
        }
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
}
