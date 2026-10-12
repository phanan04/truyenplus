package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.*
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*
import kotlinx.coroutines.flow.toList

@OptIn(ExperimentalComposeUiApi::class)
class ReaderGlassTest {
    @Test fun allFourPalettesHaveDistinctAccentsAndReadableSurfaces() {
        assertEquals(4, ReaderGlassTheme.entries.size)
        assertEquals(4, ReaderGlassTheme.entries.map { it.colors.primary }.distinct().size)
        for(theme in ReaderGlassTheme.entries) {
            val c=theme.colors
            val a=c.onSurface.luminance();val b=c.surface.luminance()
            assertTrue((maxOf(a,b)+.05f)/(minOf(a,b)+.05f)>=7f, theme.name)
            assertEquals(theme,ReaderGlassTheme.fromId(theme.id))
        }
        assertEquals(ReaderGlassTheme.Violet,ReaderGlassTheme.fromId("unknown"))
    }
    @Test fun playbackCacheAndTimerUpdatesDoNotInvalidateReaderContent() = kotlinx.coroutines.runBlocking {
        val original=ireader.domain.services.tts_service.v2.TTSState(paragraphSourceIndices=listOf(1,1,4))
        assertEquals(original.readerProjection(),original.copy(cachedParagraphs=setOf(1,2),sleepTimeRemaining=20).readerProjection())
        assertNotEquals(original.readerProjection(),original.copy(currentParagraphIndex=2).readerProjection())
        val updates=kotlinx.coroutines.flow.flow {
            repeat(1000) { emit(original.copy(sleepTimeRemaining=it.toLong(),cachedParagraphs=setOf(it))) }
            emit(original.copy(currentParagraphIndex=1))
        }.readerUpdates().toList()
        assertEquals(2,updates.size,"1000 cache/timer ticks should not update the reader")
        val index=ReaderSpeechIndex(listOf(1,1,4))
        assertEquals(0,index.paragraphForReaderPage(1))
        assertEquals(2,index.paragraphForReaderPage(4))
        assertNull(index.paragraphForReaderPage(3))
    }
    @Test fun allThemeChoicesSwitchPaletteAndKeepSelectedState() {
        val chosen=mutableStateOf(ReaderGlassTheme.Violet)
        var visibleColors=ReaderGlassTheme.Violet.colors
        val scene=ImageComposeScene(width=360,height=900) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderGlassTheme provides chosen.value) {
                MaterialTheme(colorScheme=chosen.value.colors) {
                    val colors=MaterialTheme.colorScheme
                    SideEffect { visibleColors=colors }
                    ReaderAppearancePicker(chosen.value) { chosen.value=it }
                }
            }
        }
        try {
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            for((i,theme) in ReaderGlassTheme.entries.withIndex()) {
                scene.render(i*1_000_000_000L);scene.render(i*1_000_000_000L+1)
                val all=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
                val choice=all.first { it.config.getOrNull(SemanticsProperties.TestTag)=="appearance-${theme.id}" }
                requireNotNull(choice.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                scene.render(i*1_000_000_000L+500_000_000)
                assertEquals(theme,chosen.value)
                assertEquals(theme.colors,visibleColors)
            }
        } finally { scene.close() }
    }
    @Test fun listeningParagraphUpdatesDoNotRecomposeUnrelatedControls() {
        val current=mutableStateOf(ReaderListeningContext(listOf(0,1),0,true) {})
        val counts=IntArray(2)
        val scene=ImageComposeScene(width=360,height=400) {
            MaterialTheme {
                CompositionLocalProvider(LocalReaderListening provides current.value) {
                    androidx.compose.foundation.layout.Column {
                        UnrelatedControlProbe(counts)
                        ListeningParagraphProbe(counts)
                    }
                }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            val controls=counts[0];val paragraphs=counts[1]
            current.value=ReaderListeningContext(listOf(0,1),1,true) {}
            scene.render(2_000_000_000);scene.render(3_000_000_000)
            assertEquals(controls,counts[0],"Changing highlight must not invalidate controls")
            assertTrue(counts[1]>paragraphs)
        } finally { scene.close() }
    }
    @Test fun coreReadingSettingsHaveNoSecondAdvancedMenu() {
        val strings=LocalizeHelper()
        val scene=ImageComposeScene(width=360,height=1000) {
            CompositionLocalProvider(LocalLocalizeHelper provides strings) {
                MaterialTheme { ReaderAppearanceContent(ReaderAppearanceUi(),ReaderAppearanceActions()) }
            }
        }
        try {
            scene.render(0);scene.render(1_000_000_000)
            fun nodes(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::nodes)
            val texts=scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }.flatMap { it.config.getOrNull(SemanticsProperties.Text).orEmpty() }.map {it.text}
            assertFalse(strings.localize(Res.string.tp_reader_more_options) in texts)
        } finally { scene.close() }
    }
}

@Composable private fun UnrelatedControlProbe(counts:IntArray) {
    SideEffect { counts[0]++ }
    androidx.compose.material3.Text("Controls")
}
@Composable private fun ListeningParagraphProbe(counts:IntArray) {
    val paragraph=LocalReaderListening.current?.currentParagraph
    SideEffect { counts[1]++ }
    androidx.compose.material3.Text("$paragraph")
}
