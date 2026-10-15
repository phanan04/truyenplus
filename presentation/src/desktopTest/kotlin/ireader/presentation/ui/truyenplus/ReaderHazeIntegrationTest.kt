package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderHazeIntegrationTest {
    @Test fun floatingSurfacesUseSharedHazeAndScrollingSwitchesToOpaqueFallback() {
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
            assertFalse(elements.any {it.contains("ComposedModifier")},"No blur work while scrolling")
        }
    }
}
