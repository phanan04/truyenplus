package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.runtime.remember
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material3.*
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.ReaderChapterTopBar
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class FloatingReaderControlsTest {
    @Test fun floatingControlsRemainInsetAndClickableOnPhoneAndTablet() {
        java.util.Locale.setDefault(java.util.Locale.forLanguageTag("vi"))
        for (width in listOf(320, 412, 800)) for (dark in listOf(false, true)) {
            var settings=0; var next=0
            val paper=if(dark) Color(0xFF171819) else Color(0xFFF7F4EE)
            val ink=if(dark) Color.White else Color.Black
            ImageComposeScene(width, 800, Density(1f, if(width==320) 1.5f else 1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme=if(dark) darkColorScheme() else lightColorScheme()) {
                        val backdrop=remember{ReaderNavigationBackdrop()}
                        CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                        Box(Modifier.fillMaxSize().background(paper)) {
                            Box(Modifier.fillMaxSize().readerNavigationSource(backdrop).background(paper)) {
                                Column(Modifier.padding(start=24.dp,end=24.dp,top=92.dp)) {
                                    repeat(7) {
                                        Text("Hoàng Yên Trần nhẹ gật đầu, tóc xanh bồng bềnh. Hai tia chân khí mạnh mẽ từ đầu ngón tay của nàng bay ra.",color=ink,fontFamily=FontFamily.Serif,lineHeight=30.sp,modifier=Modifier.padding(bottom=22.dp))
                                    }
                                }
                            }
                            ReaderChapterTopBar(Chapter(id=1,bookId=1,key="chapter-273",name="Chương 273: Luận Kiếm Thiếp"), {}, {}, {settings++}, backgroundColor=paper, textColor=ink)
                            CompositionLocalProvider(LocalContentColor provides ink) {
                                ReaderChapterFooter(272,9008,false,false,{},{next++},{},{},{},{},Modifier.align(Alignment.BottomCenter),backgroundColor=paper)
                            }
                        }
                        }
                    }
                }
            }.use { scene ->
                scene.render();scene.render()
                val all=nodes(scene)
                for (tag in listOf("reader-top-capsule", "reader-footer-capsule")) {
                    val bounds=all.first { it.config.getOrNull(SemanticsProperties.TestTag)==tag }.boundsInRoot
                    assertTrue(bounds.left>=10f && bounds.right<=width-10f, "$tag $width: $bounds")
                    assertTrue(bounds.width<=760f)
                }
                val tune=all.first { it.config.getOrNull(SemanticsProperties.TestTag)=="reader-settings" }
                assertTrue(tune.boundsInRoot.width>=48f)
                tune.config[SemanticsActions.OnClick].action!!.invoke()
                all.firstOrNull {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-footer-more"}?.let {more->
                    more.config[SemanticsActions.OnClick].action!!.invoke();scene.render(2_000_000_000);scene.render(3_000_000_000)
                }
                nodes(scene).first { it.config.getOrNull(SemanticsProperties.TestTag)=="reader-next" }.config[SemanticsActions.OnClick].action!!.invoke()
                assertEquals(1,settings);assertEquals(1,next)
                val file=java.io.File("build/reports/floating-reader/$width-$dark.png");file.parentFile.mkdirs()
                file.writeBytes(requireNotNull(scene.render().encodeToData(EncodedImageFormat.PNG)).bytes)
            }
        }
    }
    private fun nodes(scene:ImageComposeScene):List<SemanticsNode> {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return scene.semanticsOwners.flatMap { all(it.rootSemanticsNode) }
    }
}
