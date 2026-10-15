package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyColumn
import kotlinx.coroutines.awaitCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderHazeIntegrationTest {
    @Test fun floatingSurfacesUseSharedHazeAndUnsupportedCaptureHasAnOpaqueFallback() {
        var elements=emptyList<String>()
        var backdropRef:ReaderNavigationBackdrop?=null
        ImageComposeScene(320,240) {
            MaterialTheme {
                            val backdrop=remember {ReaderNavigationBackdrop()}
                backdropRef=backdrop
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().readerNavigationSource(backdrop).background(Color.Blue))
                        val glass=Modifier.readerNavigationGlass(Color.White)
                        elements=glass.foldIn(emptyList()) {names,element->names+element::class.simpleName.orEmpty()}
                        Box(glass.size(100.dp))
                    }
                }
            }
        }.use {scene ->
            scene.render();scene.render()
            assertIs<dev.chrisbanes.haze.HazeState>(backdropRef!!.state)
            // Haze 2's effect is materialized through Modifier.composed by Compose.
            assertTrue(elements.any {it.contains("ComposedModifier")},"Glass modifier is missing: $elements")
            backdropRef!!.samplingEnabled=false
            scene.render();scene.render()
            assertFalse(elements.any {it.contains("ComposedModifier")},"No blur work when capture is unavailable")
        }
    }
    @Test fun activeScrollKeepsTheSameCaptureAndMaterial() {
        var backdropRef:ReaderNavigationBackdrop?=null
        var scrolling=false
        ImageComposeScene(320,240) {
            val backdrop=remember {ReaderNavigationBackdrop()}
            backdropRef=backdrop
            val list=rememberLazyListState()
            scrolling=list.isScrollInProgress
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                LazyColumn(state=list) {items(20) {Box(Modifier.fillMaxWidth().height(64.dp))}}
                ReaderTrackScroll(list,1L,false) {}
                LaunchedEffect(list) {list.scroll {awaitCancellation()}}
            }
        }.use {scene->
            repeat(4) {scene.render()}
            assertTrue(scrolling,"The test must exercise an active scroll")
            assertTrue(backdropRef!!.samplingEnabled,"Scrolling replaced glass with an opaque fallback")
        }
    }

}
