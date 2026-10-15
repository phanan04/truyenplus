package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ireader.domain.models.prefs.PreferenceValues
import ireader.domain.services.tts_service.v2.SpokenSentencePosition
import ireader.presentation.ui.reader.ReaderTextPreferencesState
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderHighlightVisualTest {
    @Test fun actualWordAddsSubtleShadeAndFallbackKeepsSentenceInLightAndDark() {
        val text="Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua."
        val firstSentence=text.substringBefore('.')+"."
        for(dark in listOf(false,true)) {
            val ink=if(dark)Color.White else Color.Black
            val paper=if(dark)Color(0xff171819) else Color.White
            val preferences=ReaderTextPreferencesState(20,32,0,400,0,PreferenceValues.PreferenceTextAlignment.Left,
                ink,null,0,0,0,false,false,false,0,false)
            val base=ReaderListeningContext(listOf(0),0,true,ReaderSpeechIndex(listOf(0)),{},
                SpokenSentencePosition(0,0,firstSentence.length,firstSentence),wordHighlight=true)
            fun render(context:ReaderListeningContext,name:String):java.awt.image.BufferedImage {
                ImageComposeScene(360,160) {
                    MaterialTheme {ReaderPaperTheme(paper,ink) {Box(Modifier.fillMaxSize().background(paper).padding(16.dp)) {
                        ReaderSpokenParagraph(text,0,context,preferences,Modifier.fillMaxWidth())
                    }}}
                }.use {scene ->
                    scene.render(0);scene.render(1_000_000_000)
                    val png=requireNotNull(scene.render(2_000_000_000).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes
                    val out=java.io.File("build/reports/reader-highlight/$name-$dark.png").apply {parentFile.mkdirs()}
                    out.writeBytes(png)
                    return javax.imageio.ImageIO.read(java.io.ByteArrayInputStream(png))
                }
            }
            val sentence=render(base,"sentence")
            var tintedPixels=0
            for(y in 0 until sentence.height)for(x in 0 until sentence.width) {
                val rgb=sentence.getRGB(x,y)
                val r=(rgb shr 16) and 255;val g=(rgb shr 8) and 255;val b=rgb and 255
                if(maxOf(r,g,b)-minOf(r,g,b)>10)tintedPixels++
            }
            assertTrue(tintedPixels>300,"Spoken sentence needs a legible focus tint on either paper")
            val word=render(base.copy(word=SpokenSentencePosition(0,6,9,"Yên")),"word")
            var changed=0
            for(y in 0 until word.height)for(x in 0 until word.width)if(word.getRGB(x,y)!=sentence.getRGB(x,y))changed++
            assertTrue(changed>50,"Word shade missing in dark=$dark")
            assertTrue(changed<1500,"Word shade covers too much text: $changed")
        }
    }
}
