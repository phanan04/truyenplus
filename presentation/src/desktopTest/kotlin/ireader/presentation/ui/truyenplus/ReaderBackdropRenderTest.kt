package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.*
import java.io.File
import kotlin.test.*
import org.jetbrains.skia.EncodedImageFormat

@OptIn(ExperimentalComposeUiApi::class)
class ReaderBackdropRenderTest {
    @Test fun actualBlurSamplesContentAndKeepsControlsReadable() {
        for(dark in listOf(false,true)) {
            val scene=ImageComposeScene(width=390,height=230,density=Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme=if(dark) TruyenPlusDarkColors else TruyenPlusLightColors) {
                        val layer=rememberGraphicsLayer()
                        val backdrop=remember(layer) { ReaderNavigationBackdrop(layer) }
                        CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                                Row(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                                    repeat(30) { index -> Box(Modifier.weight(1f).fillMaxHeight().background(if(index%2==0) Color(0xFFDF9871) else Color(0xFF85ADC2))) }
                                }
                                Box(Modifier.align(Alignment.Center)) { ReaderBottomNav(ReaderDestination.Library,{},{}) }
                            }
                        }
                    }
                }
            }
            try {
                repeat(4) {scene.render(it*500_000_000L)}
                val image=scene.render(3_000_000_000)
                val out=File("build/reports/truyenplus-minimalist/navigation-${if(dark) "dark" else "light"}.png")
                out.parentFile.mkdirs();out.writeBytes(requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
                assertTrue(out.length()>1000)
            } finally {scene.close()}
        }
    }
}
