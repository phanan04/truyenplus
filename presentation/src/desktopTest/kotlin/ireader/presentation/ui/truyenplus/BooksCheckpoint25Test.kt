package ireader.presentation.ui.truyenplus

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.semantics.*
import ireader.domain.models.common.ColorModel
import ireader.domain.preferences.models.ReaderColors
import ireader.i18n.LocalizeHelper
import ireader.i18n.resources.*
import ireader.presentation.ui.core.theme.LocalLocalizeHelper
import kotlin.test.*

/** Production components measured against the supplied 393pt original screenshots. */
@OptIn(ExperimentalComposeUiApi::class)
class BooksCheckpoint25Test {
    @Test fun changingGlassOnlyInvalidatesItsConsumers() {
        fun invalidations(local:ProvidableCompositionLocal<Boolean>):Int {
            val full=mutableStateOf(false)
            var bodyCompositions=0
            var lastGlass=false
            val countBody:()->Unit={bodyCompositions++}
            val observeGlass:(Boolean)->Unit={lastGlass=it}
            val content:@Composable ()->Unit={
                Column {
                    CheckpointIndependentContent(countBody)
                    CheckpointGlassConsumer(local,observeGlass)
                }
            }
            ImageComposeScene(393,100) {
                CompositionLocalProvider(local provides full.value,content=content)
            }.use {s->
                repeat(4){s.render()}
                val before=bodyCompositions
                full.value=true;repeat(4){s.render()}
                assertTrue(lastGlass,"The selected Glass material must update immediately")
                return bodyCompositions-before
            }
        }
        assertTrue(invalidations(staticCompositionLocalOf {false})>0,"The control must detect a whole-subtree invalidation")
        assertEquals(0,invalidations(LocalReaderFullGlass),"Changing a visual material must not invalidate unrelated reader content")
    }
    @Test fun appearanceUsesNamedPaperCardsAndFullWidthCustomizeBelowTheGrid() {
        val papers=(1L..6L).map {ReaderColors(it,ColorModel(1f,1f,1f),ColorModel(0f,0f,0f))}
        ImageComposeScene(393,600) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderAppearanceContent(ReaderAppearanceUi(themes=papers,themeId=1),ReaderAppearanceActions())
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val first=node(s,"reader-theme-1").boundsInRoot
            val fourth=node(s,"reader-theme-4").boundsInRoot
            val customize=node(s,"reader-appearance-customize").boundsInRoot
            assertTrue(first.height>=88f,"Reference paper cards include a readable theme name: $first")
            assertTrue(fourth.top>=first.bottom,"Six themes use three columns and two rows")
            assertTrue(customize.width>=350f && customize.top>=fourth.bottom,"Customize is a separate full-width action")
            assertTrue(node(s,"reader-font-decrease").boundsInRoot.width>=90f,"Font controls form a two-part capsule")
        }
    }
    @Test fun navigationMatchesTheReference62PointCapsuleWithoutOversizedCaptions() {
        ImageComposeScene(393,150) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderBottomNav(ReaderDestination.Home,{},{})}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            assertEquals(62f,node(s,"nav-search").boundsInRoot.width,1f)
            assertEquals(54f,node(s,"nav-Home").boundsInRoot.height,1f)
        }
    }
    @Test fun readerUsesTrailingCloseAndOneQuietBottomMaterial() {
        var settings=0;var next=0
        ImageComposeScene(393,852) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
                ireader.presentation.ui.reader.ReaderChapterTopBar(ireader.domain.models.entities.Chapter(1,1,"a","Chương 1"),{},{},{settings++},bookTitle="Tên truyện")
                ReaderChapterFooter(1,3,false,false,{},{next++},{},{},{},{},androidx.compose.ui.Modifier.align(androidx.compose.ui.Alignment.BottomCenter))
            }
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            assertTrue(node(s,"reader-close").boundsInRoot.left>300f,"Source reader closes at the trailing edge")
            assertTrue(node(s,"reader-top-capsule").boundsInRoot.height<=52f,"Reader header must stay quiet")
            node(s,"reader-settings").config[SemanticsActions.OnClick].action!!.invoke()
            node(s,"reader-footer-more").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(2_000_000_000);s.render(3_000_000_000)
            node(s,"reader-next").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,settings);assertEquals(1,next)
        }
    }
    @Test fun homeUsesACompactReadingArcAndAnImmediateProgressShortcut() {
        var opened=0
        ImageComposeScene(393,1000) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderHomeScreen(listOf(ReaderBookUi(1,"Vạn Cổ Thần Đế",read=3,total=1488)),false,{},{},{},{},onProgress={opened++},onSettings={})}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val arc=node(s,"home-progress").boundsInRoot
            assertTrue(arc.height<=176f && arc.width>=280f,"Reference reading activity uses a semicircle, not a dashboard-sized ring: $arc")
            node(s,"home-progress-shortcut").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,opened)
        }
    }
    @Test fun bookDetailsGiveReadAndListenBalancedActionsThenOpenTheActualChapter() {
        var chosen=0L
        val chapters=listOf(ireader.domain.models.entities.Chapter(11,1,"a","Chương 1"))
        ImageComposeScene(393,1000) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderBookScreen(ReaderBookUi(1,"Vạn Cổ Thần Đế"),"Phi Thiên Ngư",chapters,{},{},{},{chosen=it.id},{})}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val read=node(s,"book-read").boundsInRoot
            val listen=node(s,"book-listen").boundsInRoot
            assertTrue(listen.width>=120f && kotlin.math.abs(read.width-listen.width)<2f,"Both reading modes need named, balanced actions")
            node(s,"book-contents").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(2_000_000_000);s.render(3_000_000_000)
            node(s,"toc-chapter-11").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(11L,chosen)
        }
    }
    @Test fun readingMenuUsesTheReferenceExpandableStackAndKeepsChapterActions() {
        var next=0
        ImageComposeScene(393,500) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderChapterFooter(1,3,false,false,{},{next++},{},{},{},{},Modifier.align(Alignment.BottomCenter))}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val menu=node(s,"reader-footer-more")
            assertTrue(menu.boundsInRoot.left>300 && menu.boundsInRoot.height>=48)
            menu.config[SemanticsActions.OnClick].action!!.invoke()
            s.render(2_000_000_000);s.render(3_000_000_000)
            val contents=node(s,"reader-contents").boundsInRoot
            val previous=node(s,"reader-previous").boundsInRoot
            assertTrue(contents.width>=250f && previous.top>=contents.bottom,"Chapter tools remain below Contents without a duplicate appearance entry")
            node(s,"reader-next").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,next)
        }
    }
    @Test fun contextualMenuKeepsReferenceWidthForShortLabelsAndReachableActions() {
        var calls=0
        ImageComposeScene(393,500) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderGlassDropdownMenu(true,{}) {
                androidx.compose.material3.DropdownMenuItem(text={androidx.compose.material3.Text("EPUB")},onClick={calls++},modifier=Modifier.semantics {testTag="reference-menu-item"})
            }}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            s.render(2_000_000_000);s.render(3_000_000_000) // Material opens a popup at scale .8 before its enter animation settles.
            val item=node(s,"reference-menu-item")
            assertTrue(item.boundsInRoot.width>=250f && item.boundsInRoot.height>=48f,"Context menus use the source's 250pt surface rather than shrink to the label: ${item.boundsInRoot}")
            item.config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(1,calls)
        }
    }
    @Test fun existingExtendedPaperPaletteDoesNotClipTheOpeningAction() {
        var picked=0L
        ImageComposeScene(393,852) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderAppearanceOverlay({}) {ReaderAppearanceContent(ReaderAppearanceUi(themes=ireader.domain.preferences.models.prefs.readerThemes,themeId=-2),ReaderAppearanceActions(onTheme={picked=it}))}}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val customize=node(s,"reader-appearance-customize").boundsInRoot
            assertTrue(customize.height>=48f && customize.bottom<=node(s,"reader-appearance-panel").boundsInRoot.bottom,"Opening choices must fit even with fourteen saved papers: $customize")
            node(s,"reader-appearance-customize").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(2_000_000_000);s.render(3_000_000_000)
            node(s,"appearance-page-colors").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(4_000_000_000);s.render(5_000_000_000)
            node(s,"reader-paper-next").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(6_000_000_000);s.render(7_000_000_000)
            node(s,"reader-theme--14").config[SemanticsActions.OnClick].action!!.invoke()
            assertEquals(-14L,picked,"Saved additional papers stay accessible in Customize")
        }
    }
    @Test fun phoneWidthKeepsBothBookActionsOnOneBalancedLine() {
        for(readCount in listOf(0,1))ImageComposeScene(360,1000) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper()) {MaterialTheme {
            ReaderBackdrop {ReaderBookScreen(ReaderBookUi(1,"Vạn Cổ Thần Đế",read=readCount,total=520),"Phi Thiên Ngư",listOf(ireader.domain.models.entities.Chapter(11,1,"a","Chương 1")),{},{},{},{},{})}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            val read=node(s,"book-read").boundsInRoot
            val listen=node(s,"book-listen").boundsInRoot
            assertEquals(read.height,listen.height,1f,"Read and Listen cannot leave mismatched action pills at 360dp")
            assertTrue(read.height<=50 && listen.height<=50)
            assertEquals(listOf(LocalizeHelper().localize(if(readCount>0)Res.string.tp_continue_reading else Res.string.tp_start_reading)),
                node(s,"book-read").config[SemanticsProperties.ContentDescription],"Screen readers retain the complete action label")
        }
    }
    @Test fun openingAnotherReaderPanelClosesTheReadingToolsMenu() {
        val panels=ReaderPanelState()
        ImageComposeScene(393,500) {CompositionLocalProvider(LocalLocalizeHelper provides LocalizeHelper(),LocalReaderPanels provides panels) {MaterialTheme {
            ReaderBackdrop {ReaderChapterFooter(0,3,false,false,{},{},{},{},{},{},Modifier.align(Alignment.BottomCenter))}
        }}}.use {s->
            s.render(0);s.render(1_000_000_000)
            node(s,"reader-footer-more").config[SemanticsActions.OnClick].action!!.invoke()
            s.render(2_000_000_000);s.render(3_000_000_000)
            assertNotNull(node(s,"reader-reading-menu"))
            panels.open(ReaderPanel.Appearance) // Same transition as tapping Aa in the top header.
            s.render(4_000_000_000);s.render(5_000_000_000)
            assertNull(runCatching {node(s,"reader-reading-menu")}.getOrNull(),"Only one reader panel can own the bottom area")
        }
    }
    private fun node(s:ImageComposeScene,t:String):SemanticsNode {
        fun all(n:SemanticsNode):List<SemanticsNode> = listOf(n)+n.children.flatMap(::all)
        return s.semanticsOwners.flatMap {all(it.rootSemanticsNode)}.first {it.config.getOrNull(SemanticsProperties.TestTag)==t}
    }
}

@Composable private fun CheckpointIndependentContent(composed:()->Unit) {
    SideEffect(composed)
    androidx.compose.material3.Text("Chapter content")
}

@Composable @NonSkippableComposable private fun CheckpointGlassConsumer(local:ProvidableCompositionLocal<Boolean>,composed:(Boolean)->Unit) {
    val full=local.current
    SideEffect {composed(full)}
    androidx.compose.material3.Text(if(full)"Glass" else "Standard")
}
