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
    @Test fun chapterHeaderDiffusesFinePageDetailsBehindItsTitle() {
        content {
            val backdrop=remember {ReaderNavigationBackdrop()}
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,LocalReaderFullGlass provides false,
                ireader.presentation.ui.core.theme.LocalLocalizeHelper provides ireader.i18n.LocalizeHelper(androidx.compose.ui.platform.LocalContext.current)) {
                Box(Modifier.fillMaxSize()) {
                    Canvas(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                        for(x in 0 until size.width.toInt() step 4)
                            drawRect(if((x/4)%2==0)Color.Black else Color.White,Offset(x.toFloat(),0f),Size(4f,size.height))
                    }
                    ireader.presentation.ui.reader.ReaderChapterTopBar(
                        ireader.domain.models.entities.Chapter(1,1,"a","Chương kiểm tra"),{},{},{},
                        backgroundColor=Color.White,textColor=Color.Black,bookTitle="Kiểm tra Glass",onEdit={})
                }
            }
        }
        Thread.sleep(300);ui.waitForIdle()
        val pixels=ui.onNodeWithTag("reader-top-capsule").captureToImage().toPixelMap()
        val y=(4*ui.activity.resources.displayMetrics.density).toInt()
        val samples=(pixels.width/3 until pixels.width*2/3).map {pixels[it,y].red}
        Assert.assertTrue("Fine page details remain sharp behind the header: ${samples.min()}..${samples.max()}",
            samples.max()-samples.min()<.05f)
    }
    @Test fun readerHeaderExpandedPlayerAndSettingsAllSampleThePageInFullGlass() {
        for(paper in listOf(Color.White,Color(0xFF1B1B1D)))for(panel in 0..3) {
            val tag=when(panel){0->"reader-identity";1->"listen-expanded-toggle";2->"listen-options-panel";else->"reader-appearance-panel"}
            content {
                val backdrop=remember {ReaderNavigationBackdrop()}
                CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,LocalReaderFullGlass provides true,
                    ireader.presentation.ui.core.theme.LocalLocalizeHelper provides ireader.i18n.LocalizeHelper(androidx.compose.ui.platform.LocalContext.current)) {
                    ReaderPaperTheme(paper,if(paper==Color.White)Color.Black else Color.White) {
                        Box(Modifier.fillMaxSize()) {
                            Row(Modifier.matchParentSize().readerNavigationSource(backdrop)) {
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFFDF594E)))
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color(0xFF457ADC)))
                            }
                            when(panel) {
                                0->ireader.presentation.ui.reader.ReaderChapterTopBar(
                                    ireader.domain.models.entities.Chapter(1,1,"a","Chương kiểm tra"),{},{},{},backgroundColor=paper,
                                    textColor=if(paper==Color.White)Color.Black else Color.White,bookTitle="Kiểm tra Glass",onEdit={})
                                1->ReaderFloatingPlayer(false,1f,.2f,{},{},{},{},{},{},modifier=Modifier.align(Alignment.BottomCenter))
                                2->Box(Modifier.align(Alignment.BottomCenter)) {
                                    ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,15,ListeningAudioState(),{},{},{},{},{},{},{},{},{},{})
                                }
                                else->ReaderAppearanceOverlay({}) {ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())}
                            }
                        }
                    }
                }
            }
            Thread.sleep(300);ui.waitForIdle()
            val pixels=ui.onNodeWithTag(tag).captureToImage().toPixelMap()
            val left=pixels[(pixels.width*.15f).toInt(),(pixels.height*.06f).toInt()]
            val right=pixels[(pixels.width*.85f).toInt(),(pixels.height*.06f).toInt()]
            Assert.assertTrue("$tag is flat/opaque instead of sampling the page on $paper: $left / $right",
                left.red-left.blue>right.red-right.blue+.08f)
            val screenshot=InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot()
            val folder=java.io.File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null),"qa-screens").apply {mkdirs()}
            java.io.File(folder,"reader-glass-${if(paper==Color.White)"light" else "dark"}-$panel.png").outputStream().use {
                screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)
            }
            screenshot.recycle()
        }
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
