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
class ReaderLibraryPolishTest {
    @Test fun homeCanResumeAnActualHistoryBookOutsideTheLoadedLibraryPage() {
        val latest=ReaderBookUi(99,"Truyện đang đọc gần nhất",chapter="Chương 273",lastRead=100)
        var resumed=0L;var progress=0
        scene(412,1000) {ReaderHomeScreen(listOf(ReaderBookUi(1,"Truyện khác")),false,{},{},{},{},
            onContinue={resumed=it},resumeBook=latest,onProgress={progress++})}.use {s->
            s.render(0);s.render(1_000_000_000)
            val all=nodes(s)
            assertTrue(all.any {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==latest.title}==true})
            all.first {it.config.getOrNull(SemanticsProperties.TestTag)=="home-continue"}.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(99,resumed)
            all.first {it.config.getOrNull(SemanticsProperties.TestTag)=="home-progress"}.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,progress)
        }
    }
    @Test fun largeTextLibraryKeepsMultipleReadableBookNamesInTheViewport() {
        val books=(1L..4L).map {ReaderBookUi(it,"Truyện số $it · Một tên truyện dài",read=1,total=100)}
        scene(320,800,2f) {ReaderLibraryScreen(books,emptyList(),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{})}.use {s->
            s.render(0);s.render(1_000_000_000)
            val second=nodes(s).firstOrNull {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text==books[1].title}==true}
            assertNotNull(second,"A giant one-column cover hides the next book")
            val png=requireNotNull(s.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
            java.io.File("build/reports/ux43/library-large-text.png").apply {parentFile.mkdirs();writeBytes(png)}
            assertTrue(second.boundsInRoot.height>0 && second.boundsInRoot.bottom<800,"Names are clipped below the viewport: ${second.boundsInRoot}; "+nodes(s).mapNotNull {n->n.config.getOrNull(SemanticsProperties.Text)?.let {it.toString()+" "+n.boundsInRoot}})
        }
    }
    @Test fun activeMiniPlayerHasOnePlaybackActionAndNoDuplicateListeningButton() {
        var played=0;var opened=0
        scene(360,160) {ReaderMiniPlayer(ReaderBookUi(1,"Truyện"),{opened++},{opened++},false,{played++})}.use {s->
            s.render(0);s.render(1_000_000_000)
            val buttons=nodes(s).filter {it.config.getOrNull(SemanticsActions.OnClick)!=null}
            assertEquals(2,buttons.size,"Information opens reader; only one separate playback action is needed")
            buttons.last().config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,played);assertEquals(0,opened)
        }
    }
    private fun scene(w:Int,h:Int,font:Float=1f,content:@androidx.compose.runtime.Composable ()->Unit)=ImageComposeScene(w,h,Density(1f,font)) {
        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {content()}}
    }
    private fun nodes(s:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
    }
}
