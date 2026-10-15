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
    @Test fun fullGlassUsesLowerPaperForCardsAndCapturedPageForFloatingPanels() {
        for(paper in listOf(Color.White,Color(0xFF1B1B1D))) {
            content {
                val backdrop=remember {ReaderNavigationBackdrop()}
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,LocalReaderFullGlass provides true) {
                    Box(Modifier.fillMaxSize()) {
                        ReaderPaperTheme(paper,if(paper==Color.White)Color.Black else Color.White) {
                            ReaderGlassBackground(Modifier.matchParentSize())
                            Box(Modifier.fillMaxSize().readerNavigationSource(backdrop).background(Color.Red)) {
                                Box(Modifier.padding(top=160.dp).size(220.dp,80.dp).readerGlassCard(20,paper)
                                    .testTag("full-card"))
                            }
                            Box(Modifier.padding(20.dp).size(220.dp,80.dp).readerPanel(20,paper).testTag("full-panel"))
                        }
                    }
                }
            }
            val panel=ui.onNodeWithTag("full-panel").captureToImage().toPixelMap()
            val red=panel[panel.width/2,panel.height/2]
            Assert.assertTrue("Full panel lost its page capture: $red",red.red-red.blue>.15f)
            val first=ui.onNodeWithTag("full-card").captureToImage().toPixelMap()
            val card=first[first.width/2,first.height/2]
            Assert.assertTrue("Card captured its red sibling or itself: $card",kotlin.math.abs(card.red-card.blue)<.03f)
            if(paper!=Color.White)Assert.assertTrue("Dark card inherited a light tint: $card",card.red<.2f)
            Thread.sleep(500);ui.waitForIdle()
            val later=ui.onNodeWithTag("full-card").captureToImage().toPixelMap()
            Assert.assertEquals("Glass feedback changes idle pixels",card.red,later[later.width/2,later.height/2].red,.01f)
        }
    }

    @Test fun nestedCardsKeepTheirParentDiffusionOnAndroid() {
        for(paper in listOf(Color.White,Color(0xFF1B1B1D))) {
            val card=mutableStateOf(false)
            content {
                val backdrop=remember {ReaderNavigationBackdrop()}
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,LocalReaderFullGlass provides true) {
                    Box(Modifier.fillMaxSize()) {
                        Row(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                        }
                        Box(Modifier.padding(16.dp).size(288.dp,148.dp).readerPanel(24,paper).testTag("nested-diffusion")) {
                            if(card.value)ReaderGlassPanelContent {
                                Box(Modifier.padding(12.dp).size(112.dp,96.dp).readerGlassCard(16,paper))
                            }
                        }
                    }
                }
            }
            fun sample():Color {
                val pixels=ui.onNodeWithTag("nested-diffusion").captureToImage().toPixelMap()
                return pixels[(pixels.width*68/288).toInt(),(pixels.height*60/148).toInt()]
            }
            val before=sample();ui.runOnIdle {card.value=true};ui.waitForIdle();val after=sample()
            Assert.assertTrue("Nested card replaced its parent diffusion: $before / $after",
                kotlin.math.abs(before.green-after.green)<.10f && kotlin.math.abs(before.blue-after.blue)<.10f)
        }
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
                        .readerNavigationGlass(Color.White,material=ReaderGlassMaterial.Frosted).testTag("striped-glass"))
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
    @Test fun compactGlassPreservesBackdropAndDarkPaperHasNoMilkyLift() {
        for(paper in listOf(Color.White,Color(0xFF1B1B1D))) {
            content {
                val backdrop=remember {ReaderNavigationBackdrop()}
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                    Box(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxSize().readerNavigationSource(backdrop).background(paper)) {
                        Row(Modifier.fillMaxWidth().height(100.dp)) {
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                            Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                        }
                        }
                        Box(Modifier.padding(20.dp).fillMaxWidth().height(64.dp)
                            .readerNavigationGlass(paper).testTag("color-glass"))
                        Box(Modifier.padding(top=120.dp).fillMaxWidth().height(64.dp)
                            .readerNavigationGlass(paper).testTag("dark-glass"))
                    }
                }
            }
            val pixels=ui.onNodeWithTag("color-glass").captureToImage().toPixelMap()
            val left=pixels[pixels.width/4,pixels.height/2]
            val right=pixels[pixels.width*3/4,pixels.height/2]
            Assert.assertTrue("Red backdrop hidden: $left",left.red-left.blue>.35f)
            Assert.assertTrue("Blue backdrop hidden: $right",right.blue-right.red>.35f)
            if(paper!=Color.White) {
                val flat=ui.onNodeWithTag("dark-glass").captureToImage().toPixelMap()
                val pixel=flat[flat.width/2,flat.height/2]
                Assert.assertTrue("Dark app inherits milky system-light tone: $pixel",
                    maxOf(pixel.red,pixel.green,pixel.blue)<.2f)
            }
        }
    }
}
