package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.PlatformContext
import ireader.domain.models.BookCover
import ireader.domain.models.entities.Chapter
import ireader.i18n.LocalizeHelper
import ireader.presentation.imageloader.coil.imageloader.BookCoverMapper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.*
import ireader.presentation.ui.reader.ReaderChapterTopBar
import java.io.File
import java.util.Locale
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderGlassApprovalPreviewTest {
    @Test fun exportProductionMaterialPreviewForApproval() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(PlatformContext.INSTANCE).components {add(BookCoverMapper())}.build())
        val cover=File("../.opencode/reports/books-device-reference/reference-cover.jpg").canonicalPath
        val book=ReaderBookUi(1,"Vạn Cổ Thần Đế",BookCover(1,-200,cover,true),read=124,total=2295,
            chapter="Chương 124 · Giết người nữ ma đầu",author="Phi Thiên Ngư")
        ImageComposeScene(1572,1010,Density(1f)) {
            Row(Modifier.fillMaxSize().background(Color(0xFFDDDDDD))) {
                for(page in 0..3) {
                    val dark=page==0 || page==2
                    val colors=if(dark)TruyenPlusDarkColors else TruyenPlusLightColors
                    Column(Modifier.width(393.dp).fillMaxHeight().padding(horizontal=5.dp)) {
                        Text(listOf("01 / TRANG CHỦ · TỐI","02 / TRANG CHỦ · SÁNG","03 / ĐANG NGHE","04 / TÙY CHỈNH CHỮ")[page],
                            color=Color(0xFF333333),fontSize=12.sp,modifier=Modifier.padding(vertical=14.dp))
                        MaterialTheme(colorScheme=colors) {
                            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderDockInset provides 190.dp) {
                                ReaderBackdrop {
                                    val backdrop=remember {ReaderNavigationBackdrop()}
                                    CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                                        Box(Modifier.fillMaxSize()) {
                                            Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {
                                                if(page<2)ReaderHomeScreen(listOf(book),false,{},{},{},{},resumeBook=book,onProgress={},onSettings={})
                                                else Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=24.dp).padding(top=90.dp,bottom=300.dp),verticalArrangement=Arrangement.spacedBy(24.dp)) {
                                                    Text("Chương 124",style=ReaderBooksTokens.section)
                                                    repeat(8) {i->Text(listOf(
                                                        "Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua, mang theo hương hoa thoang thoảng. Nàng nhìn về phía ngọn núi xa.",
                                                        "Trương Nhược Trần đứng yên dưới ánh trăng, lắng nghe tiếng bước chân đang đến gần. Một câu chuyện mới lại bắt đầu.",
                                                        "Ánh sáng dịu dàng phủ xuống con đường nhỏ. Những tán cây khẽ lay động trong gió, tất cả dường như chậm lại."
                                                    )[i%3],fontFamily=FontFamily.SansSerif,fontWeight=androidx.compose.ui.text.font.FontWeight.Normal,fontSize=21.sp,lineHeight=34.sp,color=colors.onBackground)}
                                                }
                                            }
                                            if(page<2)Column(Modifier.align(Alignment.BottomCenter).padding(bottom=20.dp)) {
                                                ReaderMiniPlayer(book,{},{},isPlaying=true,onTogglePlayback={})
                                                ReaderBottomNav(ReaderDestination.Home,{},{})
                                            } else {
                                                ReaderChapterTopBar(Chapter(id=1,bookId=1,key="preview",name="Chương 124: Giết người nữ ma đầu"),{},{},{},bookTitle=book.title)
                                                if(page==2)Box(Modifier.align(Alignment.BottomCenter).padding(bottom=20.dp)) {
                                                    ReaderFloatingPlayer(true,1.25f,.34f,{},{},{},{},{},{},onMinimize={},voiceLabel="Giọng hệ thống",
                                                        chapterLabel=book.chapter,paragraphIndex=12,paragraphCount=88)
                                                } else ReaderAppearanceOverlay({}) {
                                                    ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions())
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }.use {scene->
            repeat(12) {scene.render(it*300_000_000L);Thread.sleep(30)}
            val path=File("../.opencode/reports/glass-approval.png")
            path.writeBytes(requireNotNull(scene.render(5_000_000_000L).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes)
            assertTrue(path.length()>10000)
        }
    }
}
