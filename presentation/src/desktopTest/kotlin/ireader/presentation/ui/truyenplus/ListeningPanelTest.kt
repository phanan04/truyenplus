package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.use
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.Density
import ireader.i18n.LocalizeHelper
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import ireader.presentation.ui.core.theme.themes.TruyenPlusDarkColors
import ireader.presentation.ui.home.tts.CommonTTSActions
import ireader.presentation.ui.home.tts.CommonTTSScreenState
import kotlin.test.*
import java.util.Locale

@OptIn(ExperimentalComposeUiApi::class)
class ListeningPanelTest {
    @Test fun capCutBlockExplainsRecoveryAndKeepsThePlayActionAvailable() {
        val oldLocale=Locale.getDefault()
        Locale.setDefault(Locale.forLanguageTag("vi"))
        var plays=0
        try {
            ImageComposeScene(412,800,Density(1f)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
                    ReaderFloatingPlayer(false,1f,0f,{},{plays++},{},{},{},{},
                        hasError=true,errorDetail="CapCut submit rejected (-6)")
                }}
            }.use {scene ->
                repeat(4){scene.render()}
                fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
                val nodes=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
                // The card merges its informational text; nested playback buttons remain separate.
                val text=nodes.flatMap {it.config.getOrNull(SemanticsProperties.Text).orEmpty()}.joinToString {it.text}
                assertTrue(text.contains("CapCut") && text.contains("-6") && text.contains("phát lại"),text)
                nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="listen-play"}
                    .config[SemanticsActions.OnClick].action!!.invoke()
                assertEquals(1,plays)
            }
        } finally {Locale.setDefault(oldLocale)}
    }

    @Test fun voiceTimingHintFollowsControlsWithoutCoveringTheVoiceChoice() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        for(scale in listOf(1f,2f)) {
            val scene=ImageComposeScene(width=393,height=700,density=Density(1f,scale)) {
                CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
                    ReaderFloatingOptions("Giọng hệ thống",1f,true,true,true,false,30,ListeningAudioState(),
                        {},{},{},{},{},{},{},{},{},{},wordTiming=ReaderWordTiming.Conditional)
                }}
            }
            try {
                scene.render(0);scene.render(1_000_000_000)
                fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
                val nodes=scene.semanticsOwners.flatMap {all(it.rootSemanticsNode)}
                val hint=nodes.first {it.config.getOrNull(SemanticsProperties.Text)?.any {t->t.text=="Theo từ tùy khả năng giọng hệ thống"}==true}
                val volume=nodes.first {it.config.getOrNull(SemanticsProperties.TestTag)=="listen-speech-volume"}
                assertTrue(hint.boundsInRoot.top>=volume.boundsInRoot.bottom,"Timing hint overlaps voice controls at font scale $scale")
                assertTrue(hint.boundsInRoot.bottom<=700f)
            } finally {scene.close()}
        }
    }
    @Test fun chapterHasCompactSpeedChoicesAndKeepsParagraphSelection() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        val speeds = mutableListOf<Float>(); val paragraphs = mutableListOf<Int>()
        val actions = object : CommonTTSActions {
            override fun onPlay() {} ; override fun onPause() {}
            override fun onNextParagraph() {} ; override fun onPreviousParagraph() {}
            override fun onNextChapter() {} ; override fun onPreviousChapter() {}
            override fun onParagraphClick(index: Int) { paragraphs += index }
            override fun onToggleTranslation() {} ; override fun onToggleBilingualMode() {}
            override fun onToggleFullScreen() {} ; override fun onSpeedChange(speed: Float) { speeds += speed }
            override fun onAutoNextChange(enabled: Boolean) {}
        }
        val scene = ImageComposeScene(width=412,height=800,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme(colorScheme=TruyenPlusDarkColors) {
                    ReaderListenChapterScreen(CommonTTSScreenState(content=listOf("Đoạn đầu tiên.","Chọn chỗ này để nghe."), speechSpeed=1f), actions, {})
                }
            }
        }
        try {
            scene.render(0); scene.render(1_000_000_000)
            fun nodes(node: SemanticsNode): List<SemanticsNode> = listOf(node)+node.children.flatMap(::nodes)
            val all = scene.semanticsOwners.flatMap { nodes(it.rootSemanticsNode) }
            val choice=all.firstOrNull { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text=="1.25x" }==true && it.config.getOrNull(SemanticsActions.OnClick)?.action!=null }
            assertNotNull(choice,"The chapter player must expose a one-tap speed row")
            requireNotNull(choice.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(listOf(1.25f),speeds)
            val paragraph=all.first { it.config.getOrNull(SemanticsProperties.Text)?.any { it.text=="Chọn chỗ này để nghe." }==true && it.config.getOrNull(SemanticsActions.OnClick)?.action!=null }
            requireNotNull(paragraph.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            assertEquals(listOf(1),paragraphs)
            assertTrue(choice.boundsInRoot.top > 630f,"Player must leave the full chapter available above it")
        } finally { scene.close() }
    }
    @Test fun inlineOptionsUseRealCallbacksAndQuarterPitchSteps() {
        Locale.setDefault(Locale.forLanguageTag("vi"))
        var voice=0; var music=0; var next=true; var highlight=true; var scroll=true; var pitch=1f
        val scene = ImageComposeScene(width=412,height=600,density=Density(1f)) {
            CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {
                MaterialTheme(colorScheme=TruyenPlusDarkColors) {
                    ReaderFloatingOptions("Giọng đã chọn",1f,true,true,true,false,30,
                        ListeningAudioState(supported=true,musicTitle="Nhạc trên máy.mp3"),
                        {voice++},{music++},{},{},{pitch=it},{next=it},{highlight=it},{scroll=it},{},{})
                }
            }
        }
        try {
            scene.render(0); scene.render(1_000_000_000)
            fun descendants(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::descendants)
            var time=1_000_000_000L
            fun nodes()=scene.semanticsOwners.flatMap {descendants(it.rootSemanticsNode)}
            fun clickTag(tag:String) {
                requireNotNull(nodes().first {it.config.getOrNull(SemanticsProperties.TestTag)==tag}.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
                time+=1_000_000_000;scene.render(time);scene.render(time+500_000_000)
            }
            fun clickText(text:String) {
                val n=nodes().first {it.config.getOrNull(SemanticsProperties.Text)?.any {it.text==text}==true && it.config.getOrNull(SemanticsActions.OnClick)?.action!=null}
                requireNotNull(n.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            }
            clickTag("listen-voice-choice");assertEquals(1,voice)
            clickTag("listen-pitch-increase");assertEquals(1.25f,pitch)
            clickTag("listen-page-music");clickTag("listen-music-choice");assertEquals(1,music)
            clickTag("listen-page-reading")
            for(label in listOf("Tự động chuyển chương","Nổi bật đoạn văn","Tự động cuộn")) {
                val n=nodes().first {it.config.getOrNull(SemanticsProperties.ContentDescription)?.contains(label)==true}
                requireNotNull(n.config.getOrNull(SemanticsActions.OnClick)?.action).invoke()
            }
            assertFalse(next);assertFalse(highlight);assertFalse(scroll)
            assertFalse(nodes().any {it.config.getOrNull(SemanticsProperties.VerticalScrollAxisRange)!=null})
        } finally { scene.close() }
    }

}
