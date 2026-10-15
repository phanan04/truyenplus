package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderBackdropSamplingTest {
    @Test fun transparentSourceHolesAndUnsupportedBlurNeverExposeSharpOverlays() {
        for(sampling in listOf(false,true)) {
            ImageComposeScene(320,240) {
                MaterialTheme(lightColorScheme(background=Color.White,surface=Color.White)) {
                    val backdrop=remember{if(sampling)ReaderNavigationBackdrop() else null}
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
                java.io.File("build/reports/haze2/sampling-$sampling.png").apply {parentFile.mkdirs();writeBytes(png)}
                val image=javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
                val pixel=image.getRGB(160,120)
                val red=(pixel shr 16) and 255;val green=(pixel shr 8) and 255;val blue=pixel and 255
                // Glass adds neutral ambient shading (#f1f1f1); it must never sample green overlays.
                assertTrue(red>230 && blue>230 && kotlin.math.abs(green-red)<3 && kotlin.math.abs(blue-red)<3,
                    "Unrecorded green overlays leak through glass, sampling=$sampling pixel=${Integer.toHexString(pixel)}")
            }
        }
    }
}
