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
class ReaderGlassMaterialTest {
    @Test fun compactGlassRetainsBackdropColorInsteadOfPaintingAnOpaquePill() {
        for (paper in listOf(Color.White, Color(0xFF1B1B1D))) {
            ImageComposeScene(320, 140) {
                MaterialTheme {
                    val backdrop = remember { ReaderNavigationBackdrop() }
                    CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                            }
                            Box(Modifier.padding(20.dp).size(280.dp, 64.dp).readerNavigationGlass(paper))
                        }
                    }
                }
            }.use { scene ->
                repeat(4) { scene.render(it * 100_000_000L) }
                val bytes = requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
                val file = java.io.File("build/reports/glass-repair/compact-${if(paper == Color.White) "light" else "dark"}.png")
                file.parentFile.mkdirs(); file.writeBytes(bytes)
                val image = javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes))
                val left = image.getRGB(80, 52)
                val right = image.getRGB(240, 52)
                fun channel(pixel: Int, shift: Int) = ((pixel shr shift) and 255) / 255f
                assertTrue(channel(left, 16) - channel(left, 0) > .35f,
                    "Glass hides the red backdrop: ${Integer.toHexString(left)}")
                assertTrue(channel(right, 0) - channel(right, 16) > .35f,
                    "Glass hides the blue backdrop: ${Integer.toHexString(right)}")
            }
        }
    }
}
