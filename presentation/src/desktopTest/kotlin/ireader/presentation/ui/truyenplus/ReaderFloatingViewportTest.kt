package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.ui.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.presentation.ui.reader.ReaderScrollViewport
import kotlinx.coroutines.runBlocking
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderFloatingViewportTest {
    @Test fun chapterScrollsUnderFloatingChromeAndCanRevealItsLastLine() {
        val list=LazyListState()
        ImageComposeScene(360,400,Density(1f)) {
            ReaderScrollViewport(list,PaddingValues(top=80.dp,bottom=100.dp)) {
                items(20) {index ->Text("Đoạn $index",Modifier.fillMaxWidth().height(48.dp).testTag("paragraph-$index"))}
            }
        }.use {scene ->
            scene.render();scene.render()
            assertEquals(400,list.layoutInfo.viewportSize.height,"Chrome must not shrink or clip the scroll viewport")
            runBlocking {list.scrollToItem(2)}
            scene.render();scene.render()
            val early=nodes(scene).filter{it.config.getOrNull(SemanticsProperties.TestTag)?.startsWith("paragraph-")==true}
            assertTrue(early.any{it.boundsInRoot.top<79 && it.boundsInRoot.height>0},"Text cannot pass behind floating header")
            runBlocking {list.scrollToItem(19)}
            scene.render();scene.render()
            val last=nodes(scene).first{it.config.getOrNull(SemanticsProperties.TestTag)=="paragraph-19"}.boundsInRoot
            assertTrue(last.bottom<=301 && last.height>=47,"The mini player hides the final line: $last")
        }
    }
    @Test fun smartFollowUsesOnlyTheAreaBetweenFloatingHeaderAndPlayer() {
        assertNull(readerFollowScrollDelta(150f,180f,400f,80f,100f))
        val delta=readerFollowScrollDelta(350f,390f,400f,80f,100f)!!
        val movedTop=350f-delta;val movedBottom=390f-delta
        assertTrue(movedTop>=80f && movedBottom<=300f)
        val above=readerFollowScrollDelta(30f,60f,400f,80f,100f)!!
        assertTrue(above<0f)
        assertNull(readerFollowScrollDelta(10f,20f,100f,80f,100f),"No scrolling while settings consume the viewport")
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
    }
}
