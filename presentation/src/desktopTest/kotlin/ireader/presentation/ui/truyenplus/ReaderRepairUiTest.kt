package ireader.presentation.ui.truyenplus

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import java.util.Locale
import kotlin.test.*

@OptIn(ExperimentalComposeUiApi::class)
class ReaderRepairUiTest {
    @Test fun appearanceOverlayCanOpenAndCloseWithoutRecomposingChapter() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val open=mutableStateOf(false); var chapterCompositions=0
        val scene=ImageComposeScene(412,800,Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {
                    ReaderContentAndAppearance(
                        chapter = { SideEffect { chapterCompositions++ }; Text("Nội dung chương vẫn nguyên vẹn.") },
                        appearance = { if(open.value) ReaderAppearanceOverlay(onDismiss={open.value=false}) { Text("Cỡ chữ") } })
                }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000);val before=chapterCompositions
            repeat(3) { index ->
                open.value=true;scene.render((index*2L+2)*1_000_000_000)
                fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
                val close=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }.first { it.config.getOrNull(SemanticsProperties.TestTag)=="reader-appearance-close" }
                requireNotNull(close.config[SemanticsActions.OnClick].action).invoke()
                scene.render((index*2L+3)*1_000_000_000)
            }
            assertEquals(before,chapterCompositions)
        } finally { scene.close() }
    }
    @Test fun bookPageShowsDescriptionAndMetadataAction() {
        Locale.setDefault(Locale.forLanguageTag("vi"));var refresh=0
        val scene=ImageComposeScene(412,900,Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderBookScreen(ReaderBookUi(1,"Truyện A"),"Tác giả",emptyList(),{},{},{},{},{},
                    description="Giới thiệu thật của truyện.",onMetadata={refresh++}) }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val all=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
            assertTrue(all.any { it.config.getOrNull(SemanticsProperties.Text)?.any { t->t.text=="Giới thiệu thật của truyện." }==true })
            val button=all.first { it.config.getOrNull(SemanticsProperties.TestTag)=="book-metadata-refresh" }
            requireNotNull(button.config[SemanticsActions.OnClick].action).invoke();assertEquals(1,refresh)
        } finally { scene.close() }
    }
}
