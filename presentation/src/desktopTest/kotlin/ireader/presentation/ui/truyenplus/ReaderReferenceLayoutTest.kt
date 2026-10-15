package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.domain.models.entities.Chapter
import ireader.domain.preferences.prefs.ReadingMode
import ireader.domain.models.common.FontFamilyModel
import ireader.domain.models.common.ColorModel
import ireader.domain.preferences.models.ReaderColors
import ireader.domain.models.prefs.PreferenceValues
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.reader.ReaderChapterTopBar
import ireader.presentation.ui.reader.setText
import kotlin.test.*
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
class ReaderReferenceLayoutTest {
    @Test fun chapterHeaderLeavesReadingActionsInTheFooter() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        var settings=0
        val scene=ImageComposeScene(width=360,height=120,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {
                    ReaderChapterTopBar(Chapter(id=273,bookId=1,key="273",name="Chương 273: Luận Kiếm Thiếp (1)"),{},{},{settings++},onListen={})
                }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun children(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::children)
            val nodes=scene.semanticsOwners.flatMap { children(it.rootSemanticsNode) }
            val clickable=nodes.filter { it.config.getOrNull(SemanticsActions.OnClick)?.action!=null }
            assertEquals(2,clickable.size,"Reader header should have only back and appearance actions")
            requireNotNull(clickable.last().config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(1,settings)
            assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text.startsWith("Chương 273") }==true })
        } finally { scene.close() }
    }
    @Test fun appearanceChoicesSendReaderValues() {
        var size=20;var height=32;var margin=16;var speed=10;var scroll=false;var theme=0L
        var alignment=PreferenceValues.PreferenceTextAlignment.Left
        var mode:ReadingMode=ReadingMode.Continues;var family:FontFamilyModel=FontFamilyModel.Default
        val scene=ImageComposeScene(width=420,height=800,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {
                    ReaderAppearanceAdvancedContent(ReaderAppearanceUi(fontSize=20,lineHeight=32,margin=16,scrollSpeed=10,themes=listOf(ReaderColors(7,ColorModel.fromArgb(0xFFFAF4E6.toInt()),ColorModel.fromArgb(0xFF222222.toInt())))),
                        ReaderAppearanceActions(onFontSize={size=it},onLineHeight={height=it},onMargin={margin=it},
                            onScrollSpeed={speed=it},onScrollEnabled={scroll=it},onTheme={theme=it},onAlignment={alignment=it},onMode={mode=it},onFont={family=it}))
                }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun descendants(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::descendants)
            var time=1_000_000_000L
            fun click(tag:String) {
                val nodes=scene.semanticsOwners.flatMap {descendants(it.rootSemanticsNode)}
                val n=nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)==tag }
                requireNotNull(n.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                time+=1_000_000_000L;scene.render(time);scene.render(time+500_000_000L)
            }
            click("reader-font-increase");assertEquals(21,size)
            click("reader-align-justify");assertEquals(PreferenceValues.PreferenceTextAlignment.Justify,alignment)
            click("reader-font-picker");click("reader-font-sans");assertEquals(FontFamilyModel.SansSerif,family)
            click("appearance-page-spacing")
            click("reader-line-increase");assertEquals(34,height)
            click("reader-margin-increase");assertEquals(24,margin)
            click("reader-mode-page");assertEquals(ReadingMode.Page,mode)
            click("appearance-page-scroll")
            click("reader-scroll-increase");assertEquals(11,speed)
            click("reader-scroll-toggle");assertTrue(scroll)
            click("appearance-page-colors");click("reader-theme-7");assertEquals(7L,theme)
        } finally { scene.close() }
    }

    @Test fun chapterFooterShowsPositionAndDisablesPreviousAtFirstChapter() {
        var listen=0;var contents=0;var next=0;var bookmark=0
        val scene=ImageComposeScene(width=360,height=120,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme {
                    ReaderChapterFooter(0,9008,false,false,{}, {next++},{contents++},{listen++},{bookmark++},{})
                }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun descendants(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::descendants)
            val nodes=scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            fun node(tag:String)=nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)==tag }
            assertTrue(SemanticsProperties.Disabled in node("reader-previous").config)
            for(tag in listOf("reader-listen","reader-contents","reader-next","reader-bookmark"))
                requireNotNull(node(tag).config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(listOf(1,1,1,1),listOf(listen,contents,next,bookmark))
            assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text=="1/9008" }==true })
        } finally { scene.close() }
    }

    @Test fun fontBoundsAndListeningModeDisableUnavailableActions() {
        for ((size,tag) in listOf(8 to "reader-font-decrease",40 to "reader-font-increase")) {
            val scene=ImageComposeScene(width=420,height=800,density=Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                    MaterialTheme { ReaderAppearanceAdvancedContent(ReaderAppearanceUi(fontSize=size,listening=true),ReaderAppearanceActions()) }
                }
            }
            try {
                scene.render(0);scene.render(1_000_000_000)
                fun descendants(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::descendants)
                val nodes=scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
                assertTrue(SemanticsProperties.Disabled in nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)==tag }.config)
                requireNotNull(nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="appearance-page-spacing"}.config[SemanticsActions.OnClick].action).invoke()
                scene.render(2_000_000_000);scene.render(3_000_000_000)
                val spacing=scene.semanticsOwners.flatMap {descendants(it.rootSemanticsNode)}
                assertTrue(SemanticsProperties.Disabled in spacing.first {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-mode-page"}.config)
                assertFalse(spacing.any {it.config.getOrNull(SemanticsProperties.TestTag)=="reader-scroll-increase"})
            } finally { scene.close() }
        }
    }

    @Test fun chapterFooterDisablesNextAtLastChapter() {
        val scene=ImageComposeScene(width=360,height=120,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme { ReaderChapterFooter(9007,9008,false,false,{},{},{},{},{},{}) }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun descendants(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::descendants)
            val nodes=scene.semanticsOwners.flatMap { descendants(it.rootSemanticsNode) }
            assertTrue(SemanticsProperties.Disabled in nodes.first { it.config.getOrNull(SemanticsProperties.TestTag)=="reader-next" }.config)
            assertTrue(nodes.any { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text=="9008/9008" }==true })
        } finally { scene.close() }
    }

    @Test fun paragraphLayoutDoesNotInsertCharactersIntoTheChapter() {
        val original = "  Hoàng Yên Trần nói: ‘Đi thôi!’\nDòng gốc."
        for(index in 0..2) assertEquals(original,setText(original,index,index==2,4,8,10,12))
        assertEquals("Đoạn cuối",setText("Đoạn cuối",2,true,0,2,1))
        assertEquals("",setText("",0,false,2,2,1))
    }

}
