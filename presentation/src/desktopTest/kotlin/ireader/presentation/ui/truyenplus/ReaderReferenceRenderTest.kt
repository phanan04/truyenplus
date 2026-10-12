package ireader.presentation.ui.truyenplus

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import ireader.domain.models.common.ColorModel
import ireader.domain.models.entities.Chapter
import ireader.domain.models.prefs.PreferenceValues
import ireader.domain.preferences.models.ReaderColors
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import ireader.presentation.ui.reader.*
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import java.util.Locale
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderReferenceRenderTest {
    @Test fun renderReaderAndAppearanceInLightAndDarkAtPhoneAndTabletWidths() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val paragraphs=listOf(
            "Gió đêm thổi qua, mang theo hương hoa mộc lan thoang thoảng. Nàng đứng trên bậc thềm cũ, ánh trăng phủ lên vai.",
            "Đã bao năm trôi qua, gặp lại người trong hoàn cảnh này, trái tim vẫn khẽ run lên như thuở ban đầu.",
            "Tiếng bước chân dừng lại phía sau. Nàng quay đầu, bắt gặp ánh mắt quen thuộc.",
            "Cánh cửa cũ mở ra. Hai người bước vào căn phòng còn nguyên mùi giấy và hương trà.",
            "Nàng chậm rãi lật từng trang. Những dòng chữ đã đọc từ lâu nay mang một ý nghĩa khác.",
            "Ngoài cửa sổ, trời bắt đầu sáng. Một ngày mới đang chờ đợi.")
        val themes=listOf(0xFFFAFAFA,0xFFE8E8F0,0xFFF4E2D1,0xFFF1E4BD,0xFFE1E8CE,0xFF20242A,0xFF101112).mapIndexed { i,c ->
            ReaderColors(i.toLong(),ColorModel.fromArgb(c.toInt()),ColorModel.fromArgb((if(i<5) 0xFF242424 else 0xFFECECEC).toInt())) }
        for(width in listOf(360,800)) for(dark in listOf(false,true)) for(expanded in listOf(false,true)) {
            val background=if(dark) Color(0xFF20242A) else Color(0xFFFAFAFA)
            val ink=if(dark) Color(0xFFECECEC) else Color(0xFF242424)
            val scene=ImageComposeScene(width=width,height=1000,density=Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme(colorScheme=TruyenPlusDarkColors) {
                        Surface(color=background,contentColor=ink) {
                            Box(Modifier.fillMaxSize()) {
                                Column {
                                    ReaderChapterTopBar(Chapter(id=273,bookId=1,key="273",name="Chương 273: Luận Kiếm Thiếp (1)"),{},{},{},backgroundColor=background,textColor=ink)
                                    Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp)) {
                                        val prefs=ReaderTextPreferencesState(20,32,0,400,0,PreferenceValues.PreferenceTextAlignment.Left,ink,FontFamily.SansSerif,0,2,0,true,false,false,0,false)
                                        paragraphs.forEachIndexed { i,text -> StyleTextOptimized(Modifier.fillMaxWidth(),i,ireader.core.source.model.Text(text),i==paragraphs.lastIndex,prefs) }
                                    }
                                    ReaderChapterFooter(272,9008,false,false,{},{},{},{},{},{})
                                }
                                if(expanded) {
                                    Surface(Modifier.fillMaxSize(),color=Color.Black.copy(alpha=.3f)) {}
                                    MaterialTheme(colorScheme=MaterialTheme.colorScheme.copy(surface=background,onSurface=ink,onBackground=ink,onSurfaceVariant=ink.copy(alpha=.7f))) {
                                        Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(),color=background,contentColor=ink,shape=RoundedCornerShape(topStart=24.dp,topEnd=24.dp)) {
                                            ReaderAppearanceContent(ReaderAppearanceUi(themes=themes,themeId=if(dark) 5 else 0,fontSize=20,lineHeight=32,margin=16),ReaderAppearanceActions())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            try {
                scene.render(0);val bitmap=scene.render(1_000_000_000)
                val output=File("build/reports/truyenplus-v2/reader-reference-$width-${if(dark) "dark" else "light"}-${if(expanded) "settings" else "page"}.png")
                output.parentFile.mkdirs();output.writeBytes(requireNotNull(bitmap.encodeToData(EncodedImageFormat.PNG)).bytes)
                assertTrue(output.length()>1000)
            } finally { scene.close() }
        }
    }
}
