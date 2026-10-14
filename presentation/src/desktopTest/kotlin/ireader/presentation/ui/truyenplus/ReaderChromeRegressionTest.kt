package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import androidx.compose.ui.unit.*
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderChromeRegressionTest {
    @Test fun transparentReaderPanelsNeverInheritOppositeAppTextColor() {
        for(appDark in listOf(false,true)) for(paperDark in listOf(false,true)) {
            val paper=if(paperDark) Color(0xff171819) else Color.White
            val ink=if(paperDark) Color.White else Color.Black
            var inherited=Color.Unspecified
            var panelInk=Color.Unspecified
            ImageComposeScene(360,240,Density(1f)) {
                MaterialTheme(colorScheme=if(appDark) darkColorScheme() else lightColorScheme()) {
                    CompositionLocalProvider(LocalContentColor provides if(appDark) Color.White else Color.Black) {
                        ReaderPaperTheme(paper,ink) {
                            inherited=LocalContentColor.current
                            Surface(color=Color.Transparent) {panelInk=LocalContentColor.current;Text("Tuỳ chỉnh nghe")}
                        }
                    }
                }
            }.use {it.render()}
            assertEquals(ink,inherited,"Inherited content, appDark=$appDark paperDark=$paperDark")
            assertEquals(ink,panelInk,"Transparent panel, appDark=$appDark paperDark=$paperDark")
        }
    }

    @Test fun compactChapterButtonsAreAdjacentToPlayOnPhonesAndTablets() {
        for(width in listOf(280,360,412,800)) {
            ImageComposeScene(width,240,Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme {ReaderCollapsedPlayer(false,{},{},{},{},onPrevious={},onNext={})}
                }
            }.use {scene ->
                scene.render();scene.render()
                val all=nodes(scene)
                fun bounds(tag:String)=all.first{it.config.getOrNull(SemanticsProperties.TestTag)==tag}.boundsInRoot
                val previous=bounds("listen-compact-previous");val play=bounds("listen-play");val next=bounds("listen-compact-next")
                assertTrue(kotlin.math.abs(previous.right-play.left)<=2f,"Previous is separated from play at $width: $previous $play")
                assertTrue(kotlin.math.abs(play.right-next.left)<=2f,"Next is separated from play at $width")
            }
        }
    }
    @Test fun speedsUseOneRowOrQuarterStepAdjusterWithoutScrolling() {
        for((width,font) in listOf(280 to 2f,360 to 1f,412 to 1f,800 to 1f)) {
            var speed by mutableStateOf(1f)
            ImageComposeScene(width,440,Density(1f,font)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme {ReaderFloatingPlayer(false,speed,.2f,{},{},{},{speed=it},{},{})}
                }
            }.use {scene ->
                scene.render();scene.render()
                val all=nodes(scene)
                assertFalse(all.any{it.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange)!=null})
                val choices=all.filter{it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("listen-speed-")==true}
                val decrease=all.firstOrNull{it.config.getOrNull(SemanticsProperties.TestTag)=="listen-speed-decrease"}
                if(decrease!=null) {
                    val increase=all.first{it.config.getOrNull(SemanticsProperties.TestTag)=="listen-speed-increase"}
                    assertEquals(decrease.boundsInRoot.top,increase.boundsInRoot.top)
                    decrease.config[SemanticsActions.OnClick].action!!.invoke()
                    assertEquals(.75f,speed)
                    scene.render();scene.render()
                    nodes(scene).first{it.config.getOrNull(SemanticsProperties.TestTag)=="listen-speed-increase"}.config[SemanticsActions.OnClick].action!!.invoke()
                    assertEquals(1f,speed)
                } else {
                    assertEquals(7,choices.size)
                    assertEquals(1,choices.map{it.boundsInRoot.top}.distinct().size,"Speed choices wrap at $width/$font")
                    assertTrue(choices.last().boundsInRoot.right>=width-25)
                }
                for(n in choices) {
                    assertTrue(n.boundsInRoot.width>=47 && n.boundsInRoot.height>=47)
                    assertTrue(n.boundsInRoot.left>=0 && n.boundsInRoot.right<=width)
                }
            }
        }
    }
    @Test fun feedbackColorsRemainReadableWhenPageAndAppBrightnessDiffer() {
        for(appDark in listOf(false,true)) for(paperDark in listOf(false,true)) {
            val paper=if(paperDark) Color(0xff171819) else Color.White
            val ink=if(paperDark) Color.White else Color.Black
            val scheme=readerPaperColorScheme(if(appDark) darkColorScheme() else lightColorScheme(),paper,ink)
            for(color in listOf(scheme.primary,scheme.secondary,scheme.tertiary,scheme.error)) {
                val a=color.luminance();val b=paper.luminance()
                val contrast=(maxOf(a,b)+.05f)/(minOf(a,b)+.05f)
                assertTrue(contrast>=4.5f,"Feedback has contrast $contrast on pageDark=$paperDark appDark=$appDark")
            }
        }
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
    }
}
