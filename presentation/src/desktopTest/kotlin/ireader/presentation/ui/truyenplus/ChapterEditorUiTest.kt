package ireader.presentation.ui.truyenplus
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.models.editing.*
import ireader.core.source.model.Text as PageText
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
class ChapterEditorUiTest {
    @Test fun reviewRequiresExplicitApplyAndMemoryAndSettingsHaveSeparateActions() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        var applied=0;var memory=0;var config=0
        val snapshot=EditingSnapshot(8,7,listOf(PageText("Câu gốc.")),true)
        val scene=ImageComposeScene(412,900,Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ChapterEditorContent(ChapterEditorState(snapshot=snapshot,proposal=EditingProposal(snapshot,listOf(PageText("Câu sửa.")))),
                    onGenerate={},onCancel={},onApply={applied++},onRestore={},onMemory={memory++},onSettings={config++},onRules={},onMode={}) }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000);assertEquals(0,applied)
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap {nodes(it.rootSemanticsNode)}
            fun click(tag:String) {requireNotNull(all.first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config[SemanticsActions.OnClick].action).invoke()}
            click("editor-memory");click("editor-settings");click("editor-apply")
            assertEquals(1,applied);assertEquals(1,memory);assertEquals(1,config)
        }finally{scene.close()}
    }
    @Test fun editorFitsPhoneTabletAndLargeTextInLightAndDarkModes() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val snapshot=EditingSnapshot(8,7,listOf(PageText("Hoàng Yên Trần rút ra màu xanh ngọc chiến kiếm.")),true)
        for((width,height,scale) in listOf(Triple(320,800,1f),Triple(412,1000,1f),Triple(800,1100,1f),Triple(360,900,1.6f))) {
            for(dark in listOf(false,true)) {
                val scene=ImageComposeScene(width,height,Density(1f,scale)) {
                    CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                        MaterialTheme(colorScheme=if(dark)ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors else ireader.presentation.ui.core.theme.themes.TruyenPlusLightColors) {
                            Surface {
                                ChapterEditorContent(ChapterEditorState(snapshot=snapshot,proposal=EditingProposal(snapshot,listOf(PageText("Hoàng Yên Trần rút ra thanh chiến kiếm màu xanh ngọc.")))),
                                    onGenerate={},onCancel={},onApply={},onRestore={},onMemory={},onSettings={},onRules={},onMode={})
                            }
                        }
                    }
                }
                try {
                    scene.render();scene.render()
                    fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
                    val all=scene.semanticsOwners.flatMap{nodes(it.rootSemanticsNode)}
                    val memory=all.first{it.config.getOrNull(SemanticsProperties.TestTag)=="editor-memory"}
                    assertTrue(memory.boundsInRoot.left>=0 && memory.boundsInRoot.right<=width)
                    val settings=all.first{it.config.getOrNull(SemanticsProperties.TestTag)=="editor-settings"}
                    assertTrue(settings.boundsInRoot.left>=0 && settings.boundsInRoot.right<=width)
                    val file=java.io.File("build/reports/chapter-editing/editor-$width-$scale-${if(dark)"dark" else "light"}.png")
                    file.parentFile.mkdirs();file.writeBytes(requireNotNull(scene.render().encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)).bytes)
                } finally{scene.close()}
            }
        }
    }

}
