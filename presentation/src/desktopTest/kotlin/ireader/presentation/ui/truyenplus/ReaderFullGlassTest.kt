package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import ireader.core.prefs.DataStorePreferenceStore
import ireader.domain.preferences.prefs.UiPreferences
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlinx.coroutines.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderFullGlassTest {
    @Test fun remainingControlsUseGlassWithoutLosingSelectionInputOrDisabledActions() {
        val full=mutableStateOf(false)
        val chosen=mutableStateOf(false)
        val value=mutableStateOf("Novel")
        var clicks=0;var controlMounts=0
        ImageComposeScene(400,380) {MaterialTheme {
            CompositionLocalProvider(LocalReaderFullGlass provides full.value) {
                ReaderAppBackdrop {
                    Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                        ReaderGlassButton({clicks++},Modifier.fillMaxWidth()) {remember {controlMounts++};androidx.compose.material3.Text("Import")}
                        ReaderGlassOutlinedButton({clicks++},enabled=false) {androidx.compose.material3.Text("Disabled")}
                        ReaderGlassFilterChip(chosen.value,{chosen.value=!chosen.value},label={androidx.compose.material3.Text("Voice")})
                        ReaderGlassOutlinedTextField(value.value,{value.value=it},label={androidx.compose.material3.Text("Name")})
                    }
                }
            }
        }}.use {scene ->
            fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
            fun nodes()=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
            fun settle() {repeat(6){scene.render()}}
            fun image():ByteArray {settle();return scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes}
            fun find(text:String)=nodes().first {it.config.getOrNull(SemanticsProperties.Text)?.any {it.text==text}==true}
            val before=image()
            full.value=true;val glass=image()
            assertFalse(before.contentEquals(glass),"Controls still render solid Material backgrounds in full mode")
            assertTrue(find("Import").config[SemanticsActions.OnClick].action!!.invoke())
            assertTrue(find("Disabled").config.contains(SemanticsProperties.Disabled))
            val chip=find("Voice")
            assertFalse(chip.config[SemanticsProperties.Selected])
            assertTrue(chip.config[SemanticsActions.OnClick].action!!.invoke());settle()
            assertTrue(find("Voice").config[SemanticsProperties.Selected])
            val input=nodes().first {it.config.contains(SemanticsActions.SetText)}
            assertTrue(input.config[SemanticsActions.SetText].action!!.invoke(androidx.compose.ui.text.AnnotatedString("New title")))
            assertEquals("New title",value.value)
            chosen.value=false;value.value="Novel";full.value=false
            assertTrue(before.contentEquals(image()),"Off did not restore the exact controls")
            assertEquals(1,clicks)
            assertEquals(1,controlMounts,"Changing material must not remount control content")
        }
    }

    @Test fun utilityCaptureIsAbsentWhenOffAndDoesNotRemountItsContent() {
        val enabled=mutableStateOf(false)
        var captured=false;var mounts=0
        ImageComposeScene(320,180) {MaterialTheme {
            CompositionLocalProvider(LocalReaderFullGlass provides enabled.value) {
                ReaderAppBackdrop(enabled=enabled.value) {
                    remember {mounts++}
                    captured=LocalReaderNavigationBackdrop.current!=null
                }
            }
        }}.use {scene ->
            for(value in listOf(false,true,false)) {
                enabled.value=value;repeat(4){scene.render()}
                assertEquals(value,captured,"Off must keep legacy utility routes uncaptured")
            }
            assertEquals(1,mounts)
        }
    }

    @Test fun nestedCardsInheritDiffusionWithoutResamplingTheSharpPage() {
        val card=mutableStateOf(false)
        ImageComposeScene(320,180) {MaterialTheme {
            val backdrop=remember {ReaderNavigationBackdrop()}
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,LocalReaderFullGlass provides true) {
                Box(Modifier.fillMaxSize()) {
                    Row(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                    }
                    Box(Modifier.padding(16.dp).size(288.dp,148.dp).readerPanel(24,Color.White)) {
                        if(card.value)ReaderGlassPanelContent {
                            Box(Modifier.padding(12.dp).size(112.dp,96.dp).readerGlassCard(16,Color.White))
                        }
                    }
                }
            }
        }}.use {scene ->
            fun sample():java.awt.Color {
                repeat(6){scene.render()}
                val png=scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes
                return java.awt.Color(javax.imageio.ImageIO.read(png.inputStream()).getRGB(84,76))
            }
            val parent=sample();card.value=true;val nested=sample()
            assertTrue(kotlin.math.abs(parent.green-nested.green)<24 && kotlin.math.abs(parent.blue-nested.blue)<24,
                "Nested card replaced its panel's diffusion with a sharp independent capture: $parent / $nested")
        }
    }

    @Test fun fullGlassIsExplicitOnLowRamAndDoesNotEnableUnsupportedAndroid() {
        assertFalse(readerGlassCaptureAllowed(33,true,false))
        assertTrue(readerGlassCaptureAllowed(33,true,true))
        assertTrue(readerGlassCaptureAllowed(33,false,false))
        assertFalse(readerGlassCaptureAllowed(31,false,true))
    }

    @Test fun utilityCardsAndPopupMenusKeepActionsAndTextWhenModeChanges() {
        var actionCount=0
        val full=mutableStateOf(false)
        ImageComposeScene(400,800) {
            CompositionLocalProvider(LocalReaderFullGlass provides full.value) {
                MaterialTheme {
                    ReaderAppBackdrop {
                        ReaderOptionalGlassCard {androidx.compose.material3.Text("Voice settings")}
                        ReaderGlassDropdownMenu(true,{}) {
                            androidx.compose.material3.DropdownMenuItem(
                                text={androidx.compose.material3.Text("Continue reading")},onClick={actionCount++})
                        }
                    }
                }
            }
        }.use {scene->
            fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
            for(mode in listOf(false,true,false)) {
                full.value=mode;repeat(4) {scene.render()}
                val nodes=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
                assertTrue(nodes.any {it.config.getOrNull(SemanticsProperties.Text)?.any {it.text=="Voice settings"}==true})
                val item=nodes.first {it.config.getOrNull(SemanticsProperties.Text)?.any {it.text=="Continue reading"}==true}
                assertTrue(item.config[SemanticsActions.OnClick].action!!.invoke())
            }
            assertEquals(3,actionCount)
        }
    }

    @Test fun switchPersistsAndUpdatesMountedReadingAndSettingsWithoutRecreatingThem() {
        val directory=java.nio.file.Files.createTempDirectory("reader-full-glass-").toFile()
        val scope=CoroutineScope(Dispatchers.IO+SupervisorJob())
        val store=androidx.datastore.preferences.core.PreferenceDataStoreFactory.create(scope=scope,
            produceFile={File(directory,"ui.preferences_pb")})
        val preferences=UiPreferences(DataStorePreferenceStore(store))
        assertFalse(preferences.readerFullGlass().get(),"Existing installations keep their material")
        var mounts=0
        var readerMode=false
        val oldLocale=java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
        try {
            ImageComposeScene(420,950) {
                ReaderGlassPreferences(preferences) {
                    remember {mounts++}
                    MaterialTheme {
                        CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                            // A reading-paper override must retain the app-wide choice.
                            ReaderPaperTheme(Color.Black,Color.White) {readerMode=LocalReaderFullGlass.current}
                            ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{},
                                onFullGlass={preferences.readerFullGlass().set(it)})
                        }
                    }
                }
            }.use {scene->
                fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
                fun toggle()=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {
                    it.config.getOrNull(SemanticsProperties.TestTag)=="settings-full-glass"
                }
                fun settle(expected:Boolean) {
                    repeat(30) {scene.render();if(readerMode==expected)return;Thread.sleep(10)}
                    assertEquals(expected,readerMode)
                }
                settle(false)
                assertEquals(androidx.compose.ui.state.ToggleableState.Off,toggle().config[SemanticsProperties.ToggleableState])
                assertTrue(toggle().config[SemanticsActions.OnClick].action!!.invoke())
                settle(true)
                assertEquals(androidx.compose.ui.state.ToggleableState.On,toggle().config[SemanticsProperties.ToggleableState])
                assertTrue(UiPreferences(DataStorePreferenceStore(store)).readerFullGlass().get())
                assertTrue(toggle().config[SemanticsActions.OnClick].action!!.invoke())
                settle(false)
                assertFalse(preferences.readerFullGlass().get())
                assertEquals(1,mounts,"Toggling Glass must preserve navigation and playback owners")
                assertTrue(File(directory,"ui.preferences_pb").isFile)
            }
        } finally {
            java.util.Locale.setDefault(oldLocale)
            runBlocking {scope.coroutineContext[Job]!!.cancelAndJoin()}
            directory.deleteRecursively()
        }
    }

    @Test fun fullGlassChangesFrostedPanelsAndOffRestoresTheExactPreviousPixels() {
        val enabled=mutableStateOf(false)
        for(paper in listOf(Color.White,Color(0xFF1B1B1D))) {
            ImageComposeScene(320,160) {
                MaterialTheme {
                    val backdrop=remember {ReaderNavigationBackdrop()}
                    CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop,
                        LocalReaderFullGlass provides enabled.value) {
                        Box(Modifier.fillMaxSize()) {
                            Row(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Red))
                                Box(Modifier.weight(1f).fillMaxHeight().background(Color.Blue))
                            }
                            Box(Modifier.padding(20.dp).size(280.dp,96.dp).readerPanel(24,paper))
                        }
                    }
                }
            }.use {scene->
                fun sample():Int {
                    repeat(6) {scene.render()}
                    val png=scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes
                    return javax.imageio.ImageIO.read(png.inputStream()).getRGB(80,68)
                }
                enabled.value=false;val standard=sample()
                enabled.value=true;val glass=sample()
                assertNotEquals(standard,glass,"Full Glass still renders the standard Frosted material")
                enabled.value=false;assertEquals(standard,sample(),"Off must restore the current material")
            }
        }
    }
}
