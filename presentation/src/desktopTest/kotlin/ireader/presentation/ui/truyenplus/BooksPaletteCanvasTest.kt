package ireader.presentation.ui.truyenplus
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.*
import ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import kotlin.test.*
@OptIn(ExperimentalComposeUiApi::class)
class BooksPaletteCanvasTest {
 @Test fun primaryCanvasMatchesTheReferenceWhiteAndBlack() {
  for((theme,expected) in listOf(TruyenPlusLightColors to 0xFFFFFF,TruyenPlusDarkColors to 0x000000)) {
   ImageComposeScene(80,80) {MaterialTheme(colorScheme=theme) {ReaderBackdrop {}}}.use {s->
    s.render(0)
    val png=requireNotNull(s.render(1_000_000_000).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
    val image=javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
    assertEquals(expected,image.getRGB(40,40) and 0xFFFFFF)
   }
  }
 }
}
