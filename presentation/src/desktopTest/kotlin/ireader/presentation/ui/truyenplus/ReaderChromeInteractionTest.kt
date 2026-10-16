package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.ReaderChapterTopBar
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderChromeInteractionTest {
    @Test fun aaAndChapterHeaderHaveAReadableMaterialOverTheOppositePaperColor() {
        for(paper in listOf(Color.White,Color.Black))scene {
            val ink=if(paper==Color.White)Color.Black else Color.White
            Box(Modifier.fillMaxSize().background(ink)) {
                ReaderChapterTopBar(Chapter(1,1,"a","Chương 1"),{},{},{},backgroundColor=paper,textColor=ink)
            }
        }.use {s ->
            render(s)
            val aa=nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-settings"}.boundsInRoot
            val pixels=s.render().toComposeImageBitmap().toPixelMap()
            val pixel=pixels[aa.center.x.toInt(),aa.top.toInt()+4]
            assertEquals(paper.red,pixel.red,.05f,"Aa needs the same readable backing as the chapter title")
        }
    }
    @Test fun appearanceHasOneEntryEvenWhenReadingToolsAreExpanded() {
        scene {
            Box(Modifier.fillMaxSize()) {
                ReaderChapterTopBar(Chapter(1,1,"a","Chương 1"),{},{},{},onEdit={})
                ReaderChapterFooter(0,3,false,false,{},{},{},{},{},{},Modifier.align(Alignment.BottomCenter))
            }
        }.use {s ->
            render(s)
            nodes(s).first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-footer-more"}
                .config[SemanticsActions.OnClick].action!!.invoke()
            render(s)
            assertEquals(1,nodes(s).count {it.config.getOrNull(SemanticsProperties.Text)?.any {text->text.text=="Aa"}==true})
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-appearance-shortcut"})
            assertFalse(nodes(s).any {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-menu-appearance"})
        }
    }
    @Test fun aFingerDownDoesNotWakeChromeAndOnlyAnUnclaimedTapDoes() {
        var wake=0
        scene {Box(Modifier.fillMaxSize().readerWakeOnTouch {wake++})}.use {s ->
            render(s)
            s.sendPointerEvent(PointerEventType.Press,Offset(100f,300f))
            render(s);assertEquals(0,wake,"A possible scroll must not reveal controls on finger down")
            s.sendPointerEvent(PointerEventType.Release,Offset(100f,300f))
            render(s);assertEquals(1,wake)
        }
    }
    @Test fun playbackButtonsOwnTheirTapWithoutWakingTheHeader() {
        var wake=0;var clicks=0
        scene {Box(Modifier.fillMaxSize().readerWakeOnTouch {wake++}) {
            Box(Modifier.size(160.dp).clickable {clicks++}) {Text("Play")}
        }}.use {s ->
            render(s)
            s.sendPointerEvent(PointerEventType.Press,Offset(40f,40f))
            s.sendPointerEvent(PointerEventType.Release,Offset(40f,40f))
            render(s)
            assertEquals(1,clicks);assertEquals(0,wake)
        }
    }
    @Test fun aDragDoesNotBecomeABlankTap() {
        var wake=0
        scene {Box(Modifier.fillMaxSize().readerWakeOnTouch {wake++})}.use {s ->
            render(s)
            s.sendPointerEvent(PointerEventType.Press,Offset(100f,500f))
            s.sendPointerEvent(PointerEventType.Move,Offset(100f,300f))
            s.sendPointerEvent(PointerEventType.Release,Offset(100f,300f))
            render(s);assertEquals(0,wake)
        }
    }
    private fun scene(content:@androidx.compose.runtime.Composable ()->Unit)=ImageComposeScene(393,852) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme(content=content)}
    }
    private fun render(s:ImageComposeScene) {repeat(3){s.render()}}
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
}
