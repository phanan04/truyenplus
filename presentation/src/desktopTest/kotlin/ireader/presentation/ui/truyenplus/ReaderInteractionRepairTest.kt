package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlinx.coroutines.runBlocking
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderInteractionRepairTest {
    @Test fun panelsExcludeEachOtherAndMinimizingKeepsListening() {
        val state=ReaderPanelState()
        state.open(ReaderPanel.Appearance)
        state.open(ReaderPanel.Listening)
        assertEquals(ReaderPanel.Listening,state.active)
        state.minimize()
        assertTrue(state.minimized)
        assertEquals(ReaderPanel.None,state.active)
        state.expand()
        assertFalse(state.minimized)
    }
    @Test fun scrollTrackingDoesNotRecomposeTheChapterParent() {
        val list=LazyListState();var compositions=0;var saved=-1
        ImageComposeScene(360,600,Density(1f)) {
            ReaderTrackScroll(list,7,true){saved=it}
            SideEffect { compositions++ }
            LazyColumn(state=list) { items(100){Text("Đoạn văn $it",Modifier.height(60.dp))} }
        }.use { scene ->
            scene.render(0);scene.render(1_000_000_000);val before=compositions
            runBlocking { list.scrollToItem(25) }
            scene.render(2_000_000_000);scene.render(3_000_000_000)
            assertEquals(25,saved)
            assertEquals(before,compositions)
        }
    }
    @Test fun collapsedPlayerHasExpandAndPlaybackWithoutStopping() {
        var expand=0;var play=0
        scene(360,600) { ReaderCollapsedPlayer(false,{play++},{expand++},{},{}) }.use { scene ->
            scene.render();scene.render()
            click(scene,"listen-expand");click(scene,"listen-play")
            assertEquals(1,expand);assertEquals(1,play)
        }
    }
    @Test fun appearanceHasNoVerticalScrollAndBothSpacingRangesReachTen() {
        var line by mutableIntStateOf(30);var margin by mutableIntStateOf(16)
        scene(360,640) { ReaderAppearanceContent(ReaderAppearanceUi(lineHeight=line,margin=margin),ReaderAppearanceActions(onLineHeight={line=it},onMargin={margin=it})) }.use { scene ->
            scene.render(0);scene.render(1_000_000_000)
            click(scene,"appearance-page-spacing")
            scene.render(2_000_000_000)
            repeat(9){i ->
                click(scene,"reader-line-increase");click(scene,"reader-margin-increase")
                scene.render((i+3L)*1_000_000_000)
            }
            assertEquals(40,line);assertEquals(80,margin)
            assertFalse(nodes(scene).any { it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null })
        }
    }
    @Test fun appearanceLeavesRoomForCollapsedListeningAndAllTabsStayFixed() {
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
        for((width,height,font) in listOf(Triple(320,640,1.5f),Triple(800,600,1f),Triple(640,320,1.5f))) {
            val panels=ReaderPanelState().also {it.open(ReaderPanel.Appearance)}
            ImageComposeScene(width,height,Density(1f,font)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderPanels provides panels,
                    LocalReaderListening provides ReaderListeningContext(emptyList(),0,false,{})) {
                    MaterialTheme {ReaderBackdrop {
                        ReaderAppearanceOverlay({}) {ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())}
                        ReaderCollapsedPlayer(true,{},{},{},{},Modifier.align(Alignment.BottomCenter))
                    }}
                }
            }.use { scene ->
                scene.render(0);scene.render(1_000_000_000)
                val panel=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-appearance-panel"}.boundsInRoot
                val player=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)=="listen-collapsed"}.boundsInRoot
                assertTrue(panel.bottom<=player.top,"Panel overlaps listening: $panel vs $player")
                for((i,page) in listOf("text","spacing","colors","scroll").withIndex()) {
                    click(scene,"appearance-page-$page");scene.render((i+2L)*1_000_000_000);scene.render((i+2L)*1_000_000_000+500_000_000)
                    assertFalse(nodes(scene).any {it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
                    if(page=="spacing") {
                        for(tag in listOf("reader-line-increase","reader-margin-increase","reader-mode-page")) {
                            val b=nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.boundsInRoot
                            assertTrue(b.height>=47f && b.bottom<=panel.bottom,"Clipped $tag: $b in $panel")
                        }
                    }
                    val file=java.io.File("build/reports/reader-controls/$page-$width-$height.png");file.parentFile.mkdirs()
                    file.writeBytes(requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes)
                }
                val file=java.io.File("build/reports/reader-controls/fixed-$width-$height.png");file.parentFile.mkdirs()
                file.writeBytes(requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes)
            }
        }
    }
    private fun scene(width:Int,height:Int,content:@Composable ()->Unit)=ImageComposeScene(width,height,Density(1f)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) { MaterialTheme { ReaderBackdrop { content() } } }
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
    private fun click(scene:ImageComposeScene,tag:String) { nodes(scene).first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action!!.invoke() }
}
