package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.i18n.resources.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import kotlin.test.*
import java.io.File
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
class ResponsiveReaderTest {
    private data class Window(val width:Int,val height:Int,val font:Float)
    private val windows=listOf(Window(280,640,2f),Window(320,568,1f),Window(360,800,1.5f),Window(480,320,2f),Window(800,360,1.5f),Window(600,960,1f),Window(840,600,1.5f),Window(1280,800,2f),Window(1440,900,1f))
    private fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
    private fun scene(w:Window,content:@Composable ()->Unit)=ImageComposeScene(width=w.width,height=w.height,density=Density(1f,w.font)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
            MaterialTheme(colorScheme=TruyenPlusDarkColors) { ReaderBackdrop { content() } }
        }
    }
    private fun rendered(scene:ImageComposeScene):List<SemanticsNode> {
        scene.render(0);scene.render(1_000_000_000)
        return scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
    }
    private fun click(node:SemanticsNode) { assertTrue(requireNotNull(node.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()) }
    private fun assertVisible(node:SemanticsNode,w:Window) {
        val b=node.boundsInRoot
        assertTrue(b.width>=47f && b.height>=47f,"Touch target $b in $w")
        assertTrue(b.left>=-1 && b.top>=-1 && b.right<=w.width+1 && b.bottom<=w.height+1,"Offscreen $b in $w")
    }
    private fun save(scene:ImageComposeScene,name:String,w:Window) {
        val out=File("build/reports/truyenplus-responsive/$name-${w.width}x${w.height}-${w.font}.png")
        out.parentFile.mkdirs();out.writeBytes(requireNotNull(scene.render(20_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
    }
    @Test fun smallSplitWindowHasFixedSpacingControlsAtLargeFont() {
        val w=windows.first();val scene=scene(w) {ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())}
        try {
            val initial=rendered(scene)
            click(initial.first {it.config.getOrNull(SemanticsProperties.TestTag)=="appearance-page-spacing"})
            scene.render(2_000_000_000)
            val all=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            for(tag in listOf("reader-line-decrease","reader-line-increase","reader-margin-decrease","reader-margin-increase")) {
                assertVisible(all.first {it.config.getOrNull(SemanticsProperties.TestTag)==tag},w)
            }
            assertFalse(all.any {it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
            save(scene,"appearance",w)
        } finally {scene.close()}
    }
    @Test fun navigationRemainsReachableWithLargeFontAndSplitWindows() {
        for(w in windows) {
            val selected=mutableListOf<ReaderDestination>()
            val scene=scene(w) { Box(Modifier.fillMaxSize()) { Box(Modifier.align(Alignment.BottomCenter)) { ReaderBottomNav(ReaderDestination.Home) { selected+=it } } } }
            try {
                val all=rendered(scene)
                val tabs=all.filter { it.config.getOrNull(SemanticsProperties.Selected)!=null && it.config.getOrNull(SemanticsActions.OnClick)!=null }
                assertEquals(3,tabs.size,"$w")
                tabs.forEach { assertVisible(it,w);click(it) }
                assertEquals(ReaderAppScope.destinations,selected)
                assertEquals(1,tabs.count { it.config[SemanticsProperties.Selected] })
                save(scene,"nav",w)
            } finally { scene.close() }
        }
    }
    @Test fun chapterControlsStayTouchableAndKeepCallbacksAcrossWindows() {
        for(w in windows) {
            val calls=mutableListOf<String>()
            val scene=scene(w) { Box(Modifier.fillMaxSize()) { ReaderChapterFooter(2,20,false,true,{calls+="previous"},{calls+="next"},{calls+="contents"},{calls+="listen"},{calls+="bookmark"},{calls+="auto"},Modifier.align(Alignment.BottomCenter)) } }
            try {
                rendered(scene)
                for(tag in listOf("listen","contents","previous","next","bookmark")) {
                    fun current()=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
                    var n=current().firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-$tag"}
                    if(n==null) {
                        click(current().first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-footer-more"})
                        scene.render();scene.render()
                        n=current().first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-$tag"}
                    }
                    assertVisible(n,w);click(n);scene.render();scene.render()
                }
                assertEquals(listOf("listen","contents","previous","next","bookmark"),calls)
                save(scene,"footer",w)
            } finally { scene.close() }
        }
    }
    @Test fun expandedListeningKeepsPlaybackAndCloseVisibleInShortWindows() {
        for(w in windows) {
            var close=0;var play=0;var settings=0
            val scene=scene(w) { Box(Modifier.fillMaxSize()) { Box(Modifier.align(Alignment.BottomCenter)) {
                ReaderListeningDock(true,options={ ReaderFloatingOptions("Giọng trên máy",1f,true,true,true,false,30,ListeningAudioState(supported=true,musicTitle="Nhạc.mp3"),{},{},{},{},{},{},{},{},{},{}) },player={
                    ReaderFloatingPlayer(false,1f,.2f,{},{play++},{},{},{settings++},{close++},ListeningAudioState(supported=true))
                })
            } } }
            try {
                val all=rendered(scene)
                val s=LocalizeHelper()
                for(label in listOf(
                    s.localize(ireader.i18n.resources.Res.string.play),
                    s.localize(ireader.i18n.resources.Res.string.tp_close_listening),
                    s.localize(ireader.i18n.resources.Res.string.tp_listening_settings))) {
                    val icon=all.first { it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true }
                    val n=generateSequence(icon) { it.parent }.first { it.config.getOrNull(SemanticsActions.OnClick)!=null }
                    assertVisible(n,w);click(n)
                }
                assertEquals(1,close);assertEquals(1,play);assertEquals(1,settings)
                save(scene,"listening",w)
            } finally { scene.close() }
        }
    }

    @Test fun mainScreensRenderAtPhoneTabletLandscapeAndLargeFontSizes() {
        val books=listOf(ReaderBookUi(1,"Cuốn sách có tên dài trong thư viện",read=10,total=50))
        val chapters=(1..10).map { ireader.domain.models.entities.Chapter(id=it.toLong(),bookId=1,key="$it",name="Chương $it: Một tiêu đề dài") }
        val pages:Map<String,@Composable ()->Unit> = linkedMapOf(
            "home" to { ReaderHomeScreen(books,false,{},{},{},{},onUpdate={}) },
            "library" to { ReaderLibraryScreen(books,listOf("Tất cả","Yêu thích"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{}) },
            "settings" to { ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{}) },
            "book" to { ReaderBookScreen(books.first(),"Tác giả",chapters,{},{},{},{},{}) },
            "update" to { ReaderUpdateContent("2.0.32","https://example.org/update.json",true,ReaderUpdateState(),{},{},{},{}) }
        )
        for(w in windows) for((name,page) in pages) {
            val scene=scene(w) { ReaderContentPane { page() } }
            try {
                val all=rendered(scene)
                assertTrue(all.any { it.config.getOrNull(SemanticsProperties.Text)?.isNotEmpty()==true },"$name must have content in $w")
                save(scene,name,w)
            } finally { scene.close() }
        }
    }
}
