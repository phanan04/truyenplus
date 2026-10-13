package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import ireader.domain.models.entities.Chapter
import ireader.domain.models.prefs.PreferenceValues
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.*
import java.io.File
import java.util.Locale
import org.jetbrains.skia.EncodedImageFormat
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderMinimalistRenderTest {
    private val book=ReaderBookUi(1,"Vạn Cổ Thần Đế",read=273,total=9008,chapter="Chương 273: Luận Kiếm Thiếp")
    private val paragraphs=listOf("Hoàng Yên Trần nhẹ gật đầu, nhìn về phía xa. Gió đêm thổi qua, mang theo hương hoa thoang thoảng.",
        "Cánh cửa cũ mở ra. Hai người bước vào căn phòng còn nguyên mùi giấy và hương trà.",
        "Trên bàn, cuốn sách vẫn mở ở trang đã đọc dở. Nàng chậm rãi ngồi xuống, lật từng trang giấy.",
        "Ngoài cửa sổ, trời bắt đầu sáng. Một ngày mới đang chờ đợi.")
    @Composable private fun Reading(options:Boolean) {
        val c=MaterialTheme.colorScheme
        Box(Modifier.fillMaxSize()) {
            Column {
                ReaderChapterTopBar(Chapter(id=273,bookId=1,key="273",name=book.chapter),{},{},{},backgroundColor=c.background,textColor=c.onBackground)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
                    val prefs=ReaderTextPreferencesState(20,32,0,400,0,PreferenceValues.PreferenceTextAlignment.Left,c.onBackground,FontFamily.Serif,0,2,0,true,false,false,0,false)
                    paragraphs.forEachIndexed { i,p -> StyleTextOptimized(Modifier.fillMaxWidth(),i,ireader.core.source.model.Text(p),i==paragraphs.lastIndex,prefs) }
                    Spacer(Modifier.height(190.dp))
                }
            }
            Box(Modifier.align(Alignment.BottomCenter)) {
                ReaderListeningDock(options,options={ReaderFloatingOptions("Giọng trên máy",1f,true,true,true,false,30,
                    ListeningAudioState(supported=true,musicTitle="Nhạc trên máy.mp3",musicEnabled=true),{},{},{},{},{},{},{},{},{},{})},player={
                    ReaderFloatingPlayer(false,1f,.3f,{},{},{},{},{},{},ListeningAudioState(supported=true))
                })
            }
        }
    }
    @Test fun renderHomeBookLibraryAndSettings() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        for((name,colors) in listOf("light" to ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors,"dark" to ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors)) {
            val books=listOf(book,book.copy(id=2,title="Đấu Phá Thương Khung",read=0,total=1643),book.copy(id=3,title="Tiên Nghịch",read=0,total=2000))
            val scene=ImageComposeScene(width=1560,height=900,density=Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme=colors) {
                        Row {
                            Box(Modifier.weight(1f)) { PreviewShell(ReaderDestination.Home) {ReaderHomeScreen(books,false,{},{},{},{})} }
                            Box(Modifier.weight(1f)) { PreviewShell(ReaderDestination.Library) {ReaderLibraryScreen(books,listOf("Tất cả"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{})} }
                            Box(Modifier.weight(1f)) { ReaderBackdrop {ReaderBookScreen(book,"Phi Thiên Ngư",(270..277).map {Chapter(id=it.toLong(),bookId=1,key="$it",name="Chương $it: Luận Kiếm Thiếp")},{},{},{},{},{},activeChapterId=273)} }
                            Box(Modifier.weight(1f)) { PreviewShell(ReaderDestination.Settings) {ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{},ReaderAppearanceMode.fromId(name),{})} }
                        }
                    }
                }
            }
            try {
                repeat(4) {scene.render(it*500_000_000L)}
                val out=File("build/reports/truyenplus-minimalist/overview-$name.png")
                out.parentFile.mkdirs();out.writeBytes(requireNotNull(scene.render(3_000_000_000).encodeToData(EncodedImageFormat.PNG)).bytes)
            } finally {scene.close()}
        }
    }
    @Composable private fun PreviewShell(destination:ReaderDestination,content:@Composable ()->Unit) {
        ReaderBackdrop {
            val layer=androidx.compose.ui.graphics.rememberGraphicsLayer()
            val backdrop=remember(layer) {ReaderNavigationBackdrop(layer)}
            CompositionLocalProvider(LocalReaderNavigationBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize().readerNavigationSource(backdrop)) {content()}
                Column(Modifier.align(Alignment.BottomCenter)) {ReaderMiniPlayer(book,{},{}) ; ReaderBottomNav(destination,{},{})}
            }
        }
    }
    @Test fun renderLightAndDarkLibraryReadingPlayerAndSettings() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        for((name,colors) in listOf("light" to ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors,"dark" to ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors)) {
            val scene=ImageComposeScene(width=1440,height=960,density=Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme=colors) {
                        Row {
                            Box(Modifier.weight(1f)) { ReaderBackdrop {
                                ReaderContentPane { ReaderLibraryScreen(listOf(book,book.copy(id=2,title="Đấu Phá Thương Khung",read=120,total=1643)),listOf("Tất cả","Yêu thích"),0,"",false,false,emptySet(),{},{},{},{},{},{},{},{}) }
                                Column(Modifier.align(Alignment.BottomCenter)) { ReaderMiniPlayer(book,{},{}) ; ReaderBottomNav(ReaderDestination.Library,{}) }
                            } }
                            Box(Modifier.weight(1f)) { ReaderBackdrop { Reading(false) } }
                            Box(Modifier.weight(1f)) { ReaderBackdrop { Reading(true) } }
                            Box(Modifier.weight(1f)) { ReaderBackdrop {
                                ReaderSettingsScreen(ReaderUiLanguage.Vietnamese,{},{},{},ReaderAppearanceMode.fromId(name),{})
                                Box(Modifier.align(Alignment.BottomCenter)) { ReaderBottomNav(ReaderDestination.Settings,{}) }
                            } }
                        }
                    }
                }
            }
            try {
                scene.render(0);val bitmap=scene.render(1_000_000_000)
                val out=File("build/reports/truyenplus-minimalist/$name.png")
                out.parentFile.mkdirs();out.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
                assertTrue(out.length()>1000)
            } finally { scene.close() }
        }
    }
}
