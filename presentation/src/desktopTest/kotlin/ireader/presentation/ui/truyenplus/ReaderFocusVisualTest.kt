package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.domain.models.prefs.PreferenceValues
import ireader.presentation.ui.reader.ReaderTextPreferencesState
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderFocusVisualTest {
    @Test fun paragraphWithoutSentenceTimingStillGetsAFocusCardAndKeepsItsLayout() {
        val text="Hai tia chân khí từ đầu ngón tay nàng bay ra. Hai thị nữ từ từ khôi phục lại."
        for(dark in listOf(false,true)) {
            val paper=if(dark)Color(0xff171819) else Color(0xfffffbf3)
            val ink=if(dark)Color.White else Color.Black
            val prefs=ReaderTextPreferencesState(20,32,0,400,0,PreferenceValues.PreferenceTextAlignment.Left,ink,null,0,0,0,false,false,false,0,false)
            fun render(highlight:Boolean):java.awt.image.BufferedImage {
                ImageComposeScene(360,200) {
                    MaterialTheme {ReaderPaperTheme(paper,ink) {
                        Box(Modifier.fillMaxSize().background(paper).padding(16.dp)) {
                            ReaderSpokenParagraph(text,0,ReaderListeningContext(listOf(0),0,highlight,{}),prefs,Modifier.fillMaxWidth())
                        }
                    }}
                }.use {scene ->
                    scene.render(0);scene.render(1_000_000_000)
                    val bytes=requireNotNull(scene.render(2_000_000_000).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
                    java.io.File("build/reports/reader-focus/paragraph-$dark-$highlight.png").apply {parentFile.mkdirs();writeBytes(bytes)}
                    return javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(bytes))
                }
            }
            val plain=render(false);val focus=render(true)
            var tinted=0
            // Exclude the existing left gutter: the paragraph itself needs a visible wash.
            for(y in 18..100)for(x in 24..335)if(plain.getRGB(x,y)!=focus.getRGB(x,y))tinted++
            assertTrue(tinted>5000,"Missing paragraph focus card without sentence timing: $tinted")
            // Pure ink pixels must not move when highlighting toggles.
            for(y in 18..100)for(x in 24..335) {
                val p=plain.getRGB(x,y) and 0xffffff
                if(p==(if(dark)0xffffff else 0))assertEquals(p,focus.getRGB(x,y) and 0xffffff,"Text moved or changed at $x,$y")
            }
        }
    }
}
