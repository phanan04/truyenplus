package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.ReaderChapterTopBar
import ireader.presentation.ui.reader.ReaderScrollViewport
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderChromeVisualTest {
    @Test fun renderFloatingControlsAcrossOppositeAppAndPaperThemes() {
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
        val papers=listOf("white" to Color.White,"cream" to Color(0xfff4ead5),"dark" to Color(0xff171819))
        for(appDark in listOf(false,true)) for((name,paper) in papers) {
            val ink=if(name=="dark")Color.White else Color(0xff171819)
            var mode by mutableIntStateOf(0)
            val scroll=LazyListState()
            ImageComposeScene(412,800,Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),
                    LocalContentColor provides if(appDark)Color.White else Color.Black) {
                    MaterialTheme(colorScheme=if(appDark)darkColorScheme() else lightColorScheme()) {
                        ReaderPaperTheme(paper,ink) {ReaderResponsiveFrame {
                            val backdrop=remember{ReaderNavigationBackdrop()}
                            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                                Box(Modifier.fillMaxSize().background(paper)) {
                                    Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                                        ReaderScrollViewport(scroll,PaddingValues(top=80.dp,bottom=230.dp)) {
                                            items(30) {i -> Text("Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua, mang hương hoa thoang thoảng. Nàng nhìn về ngọn núi xa.",
                                                Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=14.dp).testTag("visual-paragraph-$i"),fontSize=18.sp,lineHeight=30.sp)}
                                        }
                                    }
                                    ReaderChapterTopBar(Chapter(id=1,bookId=1,key="1",name="Chương 182: Không Tuyền"),{},{},{},
                                        backgroundColor=paper,textColor=ink,bookTitle="Vạn Cổ Thần Đế",onEdit={})
                                    if(mode in 0..2)Box(Modifier.align(Alignment.BottomCenter)) {
                                        ReaderListeningDock(mode==0,options={
                                            ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,30,ListeningAudioState(),{},{},{},{},{},{},{},{},{},{},onDismiss={})
                                        },player={
                                            if(mode==1)ReaderFloatingPlayer(false,1.25f,.2f,{},{},{},{},{},{},onMinimize={},
                                                voiceLabel="Giọng hệ thống",chapterLabel="Chương 182: Không Tuyền",paragraphIndex=0,paragraphCount=88)
                                            else ReaderCollapsedPlayer(false,{},{},{},{},voiceLabel="Giọng hệ thống",onPrevious={},onNext={})
                                        })
                                    }
                                    if(mode==1 || mode==2)ReaderReturnToSpeech({},Modifier.align(Alignment.BottomEnd).padding(end=16.dp,bottom=if(mode==1)240.dp else 88.dp))
                                    if(mode==3)ReaderChapterFooter(181,2295,false,false,{},{},{},{},{},{},Modifier.align(Alignment.BottomCenter),paper)
                                    if(mode==4)ReaderAppearanceOverlay({},paper,ink) {ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())}
                                }
                            }
                        }}
                    }
                }
            }.use {scene ->
                scene.render(0);scene.render(1_000_000_000)
                runBlocking {scroll.scrollToItem(2)}
                for(i in 0..4) {
                    mode=i
                    scene.render((i+2L)*1_000_000_000)
                    val image=scene.render((i+2L)*1_000_000_000+500_000_000)
                    val file=File("build/reports/reader-chrome/$name-app-${if(appDark)"dark" else "light"}-$i.png")
                    file.parentFile.mkdirs();file.writeBytes(requireNotNull(image.encodeToData(EncodedImageFormat.PNG)).bytes)
                    val all=nodes(scene)
                    assertTrue(all.any{it.config.getOrNull(SemanticsProperties.Text).orEmpty().any{t->t.text.contains("Hoàng Yên Trần")}})
                    if(i==0)assertTrue(all.any{it.config.getOrNull(SemanticsProperties.TestTag)=="listen-options-close"})
                    if(i==1 || i==2)assertTrue(all.any{it.config.getOrNull(SemanticsProperties.TestTag)=="reader-return-to-speech"})
                }
            }
        }
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap{all(it.rootSemanticsNode)}
    }
}
