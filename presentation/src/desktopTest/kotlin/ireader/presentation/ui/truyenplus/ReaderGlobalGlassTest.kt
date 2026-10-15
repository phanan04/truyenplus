package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderGlobalGlassTest {
    @Test fun settingsGroupsRemainLegibleWithoutContentBleedingThroughInBothAppearances() {
        for(dark in listOf(false,true)) {
            ImageComposeScene(360,800) {
                val colors=if(dark)darkColorScheme(surface=Color.Black,background=Color.Black) else lightColorScheme(surface=Color.White,background=Color.White)
                MaterialTheme(colorScheme=colors) {
                    CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalContentColor provides colors.onSurface) {
                        Box(Modifier.fillMaxSize().background(Color.Blue)) {
                            ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{})
                        }
                    }
                }
            }.use {scene ->
                scene.render(0)
                val image=scene.render(1_000_000_000)
                fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
                val panel=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {
                    it.config.getOrNull(SemanticsProperties.TestTag)=="settings-general"
                }.boundsInRoot
                val png=requireNotNull(image.encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
                val bitmap=javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
                val color=bitmap.getRGB(panel.left.toInt()+8,panel.top.toInt()+18)
                val blue=(color and 255)/255f
                val red=((color shr 16) and 255)/255f
                assertTrue(kotlin.math.abs(blue-red)<.03f,"Background bleeds through grouped settings rows")
                if(dark)assertTrue(red<.04f,"Dark group loses its readable canvas") else assertTrue(red>.96f,"Light group loses its readable canvas")
            }
        }
    }
}
