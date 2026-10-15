package org.ireader.app

import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ireader.presentation.ui.truyenplus.*
import org.junit.*
import org.junit.runner.RunWith

/** Pixel checks run on Android RenderNode; software desktop scenes cannot verify RenderEffect. */
@RunWith(AndroidJUnit4::class)
class ReaderGlassDeviceTest {
    @get:Rule val ui=createAndroidComposeRule<MainActivity>()
    private fun content(body:@Composable ()->Unit) {
        check(InstrumentationRegistry.getInstrumentation().targetContext.packageName.endsWith(".debug.qa"))
        ui.activity.runOnUiThread {ui.activity.setContent {
            MaterialTheme(lightColorScheme(surface=Color.White,background=Color.White),content=body)
        }}
        ui.waitForIdle()
    }
    @Test fun frostedLayerBlursStripesAndDoesNotRevealUnrecordedContent() {
        content {
            val backdrop=remember{ReaderNavigationBackdrop()}
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize().background(Color.Green)) {
                    Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                        Canvas(Modifier.fillMaxWidth().height(180.dp)) {
                            for(x in 0 until size.width.toInt() step 4)
                                drawRect(if((x/4)%2==0)Color.Black else Color.White,Offset(x.toFloat(),0f),Size(4f,size.height))
                        }
                    }
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(130.dp).background(Color.Green))
                    Box(Modifier.padding(top=20.dp).fillMaxWidth().height(120.dp)
                        .readerNavigationGlass(Color.White).testTag("striped-glass"))
                    Box(Modifier.align(Alignment.BottomCenter).padding(bottom=30.dp).size(220.dp,80.dp)
                        .readerNavigationGlass(Color.White).testTag("empty-glass"))
                }
            }
        }
        val striped=ui.onNodeWithTag("striped-glass").captureToImage().toPixelMap()
        val middle=striped.height/2
        val samples=(40 until striped.width-40).map{striped[it,middle].red}
        Assert.assertTrue("Source stripes remain sharp behind frosted glass",samples.max()-samples.min()<.04f)
        val empty=ui.onNodeWithTag("empty-glass").captureToImage().toPixelMap()
        val pixel=empty[empty.width/2,empty.height/2]
        Assert.assertTrue("Unrecorded green content leaks through glass: $pixel",pixel.red>.90f && pixel.blue>.90f &&
            kotlin.math.abs(pixel.green-pixel.red)<.02f && kotlin.math.abs(pixel.blue-pixel.red)<.02f)
    }
    @Test fun dialogSamplesItsScreenPositionRatherThanItsOwnWindowOrigin() {
        content {
            val backdrop=remember{ReaderNavigationBackdrop()}
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                    Canvas(Modifier.fillMaxSize()) {
                        drawRect(Color.Red)
                        drawRect(Color.Blue,size=Size(size.width,size.height*.3f))
                    }
                }
                ReaderGlassDialog(onDismissRequest={},title={},confirmButton={},text={
                    Box(Modifier.fillMaxWidth().height(60.dp).testTag("dialog-probe"))
                })
            }
        }
        val image=ui.onNodeWithTag("dialog-probe").captureToImage().toPixelMap()
        val pixel=image[image.width/2,image.height/2]
        // Haze 2 regular Glass brightens the captured red to pink; the top blue area
        // would invert this channel difference, while an opaque fallback would be neutral.
        Assert.assertTrue("Dialog sampled the top of the main window: $pixel",pixel.red-pixel.blue>.08f)
    }
}
