package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderBackdropSamplingTest {
    @Test fun transparentSourceHolesAndUnsupportedBlurNeverExposeSharpOverlays() {
        for(sampling in listOf(false,true)) {
            ImageComposeScene(320,240) {
                MaterialTheme(lightColorScheme(background=Color.White,surface=Color.White)) {
                    val layer=rememberGraphicsLayer()
                    val backdrop=remember(layer){if(sampling)ReaderNavigationBackdrop(layer) else null}
                    CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize()) {
                            Box(Modifier.fillMaxSize().readerNavigationSource(backdrop))
                            // Navigation/player are deliberately excluded from the captured content.
                            Box(Modifier.matchParentSize().background(Color.Green))
                            Box(Modifier.align(Alignment.Center).size(160.dp,80.dp).readerNavigationGlass(Color.White))
                        }
                    }
                }
            }.use {scene ->
                scene.render();scene.render()
                val png=requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
                val image=javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
                val pixel=image.getRGB(160,120)
                assertTrue(((pixel shr 16) and 255)>250 && (pixel and 255)>250,
                    "Unrecorded green overlays leak through glass, sampling=$sampling")
            }
        }
    }
}
