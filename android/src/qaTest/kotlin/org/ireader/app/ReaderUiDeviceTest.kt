package org.ireader.app

import android.util.Log
import androidx.compose.ui.test.*
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import ireader.core.source.model.Text
import ireader.domain.data.repository.*
import ireader.domain.models.entities.*
import ireader.domain.preferences.prefs.UiPreferences
import ireader.domain.preferences.prefs.ReaderPreferences
import ireader.domain.services.tts_service.v2.TTSCommand
import ireader.domain.services.tts_service.v2.TTSController
import ireader.domain.services.tts_service.v2.TTSV2ServiceStarter
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import ireader.domain.preferences.prefs.AppPreferences
import ireader.domain.models.common.DomainColor
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderUiDeviceTest {
    @get:Rule(order=0) val seed = TestRule { base, description -> object:Statement() {
        override fun evaluate() {
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".debug.qa"))
            val koin=GlobalContext.get()
            if(description.methodName in setOf("largeSystemFontKeepsTheFirstParagraphBelowTheFloatingHeader","booksRootNavigationSearchAccountAndReadingRemainConnected","paragraphListenFromHereChangesTheRealPlaybackPosition"))
                koin.get<ireader.domain.services.processstate.ProcessStateManager>().clearReaderState()
            koin.get<UiPreferences>().apply {
                val complete=description.methodName!="firstLaunchActionsFollowTheSelectedLanguage"
                hasCompletedOnboarding().set(complete);hasCompletedFirstLaunch().set(complete);language().set("vi")
            }
            koin.get<ReaderPreferences>().apply {ttsFollowParagraph().set(true);ttsHighlightText().set(true);ttsHighlightWords().set(false)}
            if(description.methodName=="voiceManagerKeepsCompactPlayerBelowItAndWordChoicePersists")
                koin.get<ireader.domain.services.tts_service.v2.offline.OfflineVoiceStore>().select(null)
            if(description.methodName in setOf("pageThemeOverridesAppThemeAndChromeDoesNotClipChapter","readingAppearanceWithoutListeningWrapsAndKeepsPaperUndimmed","voiceManagerKeepsCompactPlayerBelowItAndWordChoicePersists")) {
                koin.get<UiPreferences>().apply {readerAppearance().set("dark");readerFollowsAppearance().set(false)}
                koin.get<AppPreferences>().apply {
                    backgroundColorReader().set(DomainColor(1f,1f,1f));textColorReader().set(DomainColor(0f,0f,0f))
                }
            }
            if(description.methodName=="booksRootNavigationSearchAccountAndReadingRemainConnected")koin.get<UiPreferences>().apply {readerAppearance().set("light");readerSearchShelf().set(false)}
            val visual=description.methodName=="booksRootNavigationSearchAccountAndReadingRemainConnected"
            if(visual)koin.get<ireader.domain.preferences.prefs.LibraryPreferences>().categoryFlags().set(ireader.domain.models.DisplayMode.CompactGrid.flag)
            val cover=File(context.getExternalFilesDir(null),"reference-cover.jpg")
            val metadata=File(context.getExternalFilesDir(null),"reference-meta.json").takeIf {visual && it.isFile}?.let {org.json.JSONObject(it.readText())}
            runBlocking {
                val id=koin.get<BookRepository>().upsert(Book(id=888100,sourceId=-200,title=metadata?.optString("title")?.takeIf {it.isNotBlank()} ?: "Kiểm tra đọc và nghe",author=metadata?.optString("author").orEmpty(),cover=if(visual && cover.isFile)cover.absolutePath else "",key="qa-ui",favorite=true,initialized=true))!!
                val chapters=(1..3).map {i->Chapter(id=888100+i.toLong(),bookId=id,key="qa-ui-$i",name="Chương $i: Gió đêm",
                    sourceOrder=i.toLong(),number=i.toFloat(),read=i==1,content=List(30){Text("Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua, mang hương hoa thoang thoảng. Nàng nhìn về phía ngọn núi xa.")})}
                koin.get<ChapterRepository>().insertChapters(chapters)
                if(visual) {
                    val fixture=File(context.getExternalFilesDir(null),"visual-books.json")
                    if(fixture.isFile) {
                        val rows=org.json.JSONArray(fixture.readText())
                        for(index in 0 until rows.length()) {
                            val row=rows.getJSONObject(index);val bid=row.getLong("id")
                            koin.get<BookRepository>().upsert(Book(id=bid,sourceId=-200,title=row.getString("title"),author=row.optString("author"),description=row.optString("description"),
                                cover=File(context.getExternalFilesDir(null),row.getString("cover")).absolutePath,key=when(bid){888100L->"qa-ui";888200L->"qa-ui-second";else->"qa-visual-$bid"},favorite=true,initialized=true))
                            if(bid!=888100L)koin.get<ChapterRepository>().insertChapters(listOf(Chapter(id=bid+1,bookId=bid,key=if(bid==888200L)"qa-ui-second-1" else "qa-visual-$bid-1",name="Chương kiểm tra riêng",sourceOrder=1,number=1f,content=listOf(Text("Nội dung kiểm tra điều hướng.")))))
                        }
                    }
                }
                koin.get<HistoryRepository>().insertHistory(History(id=-1,chapterId=chapters.first().id,readAt=System.currentTimeMillis(),readDuration=5000,progress=.1f))
            }
            base.evaluate()
        }
    } }
    @get:Rule(order=1) val ui=createAndroidComposeRule<MainActivity>()
    @After fun stopQaNarration() {
        GlobalContext.get().get<TTSController>().dispatch(TTSCommand.Cleanup)
        TTSV2ServiceStarter(InstrumentationRegistry.getInstrumentation().targetContext).stopService()
    }
    private fun waitTag(tag:String) {
        android.util.Log.i("TruyenPlusUiQA","Await $tag")
        try {ui.waitUntil(30_000){ui.onAllNodesWithTag(tag,useUnmergedTree=true).fetchSemanticsNodes().isNotEmpty()}}
        catch(error:Throwable) {runCatching {shot("failed-$tag")};throw error}
    }
    private fun wakeControls() {
        if(ui.onAllNodesWithTag("reader-settings").fetchSemanticsNodes().isEmpty()) {
            ui.onRoot().performTouchInput {click(Offset(width/2f,height/3f))}
            waitTag("reader-settings")
        }
    }
    private fun openCustomization() {
        ui.waitForIdle()
        if(ui.onAllNodesWithTag("reader-appearance-customize").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithTag("reader-appearance-customize").performClick();waitTag("reader-font-picker")
        }
    }
    private fun shot(name:String) {
        ui.waitForIdle()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val suffix=InstrumentationRegistry.getArguments().getString("visualSuffix")?.let {"-$it"}.orEmpty()
        val out=File(context.getExternalFilesDir(null),"qa-screens/$name$suffix.png").apply {parentFile!!.mkdirs()}
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();android.os.SystemClock.sleep(200)
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(out)
    }
    @Test fun readingAppearanceWithoutListeningWrapsAndKeepsPaperUndimmed() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();ui.onNodeWithTag("reader-settings").performClick();waitTag("reader-appearance-close");openCustomization()
        val panel=ui.onNodeWithTag("reader-appearance-panel").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue("Text panel has a large empty area",panel.bottom-ui.onNodeWithTag("reader-font-increase").fetchSemanticsNode().boundsInRoot.bottom<=ui.activity.resources.displayMetrics.density*48)
        ui.onNodeWithTag("reader-font-picker").assertIsDisplayed()
        ui.onNodeWithTag("reader-font-increase").assertIsDisplayed()
        val pixels=ui.onRoot().captureToImage().toPixelMap()
        Assert.assertTrue("Opening appearance darkens the paper",pixels[8,pixels.height/3].luminance()>.95f)
        shot("reading-appearance-without-listening")
        ui.onNodeWithTag("reader-appearance-close").performClick()
        ui.onNodeWithTag("reader-appearance-panel").assertDoesNotExist()
    }
    @Test fun pageThemeOverridesAppThemeAndChromeDoesNotClipChapter() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls()
        ui.onRoot().performTouchInput {swipe(Offset(width/2f,height*.55f),Offset(width/2f,height*.28f),500)}
        wakeControls()
        val header=ui.onNodeWithTag("reader-top-capsule").fetchSemanticsNode().boundsInRoot
        val paragraphs=ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes()
        Assert.assertTrue("Floating header clips the chapter",paragraphs.any{it.boundsInRoot.top<header.bottom && it.boundsInRoot.height>0})
        ui.onNodeWithTag("reader-listen").performClick();waitTag("listen-settings")
        ui.onNodeWithTag("listen-settings").performClick();waitTag("listen-options-close")
        fun assertTitleInk(light:Boolean) {
            val pixels=ui.onNodeWithText("chỉnh nghe",substring=true).captureToImage().toPixelMap()
            var ink=0
            for(y in 0 until pixels.height)for(x in 0 until pixels.width) {
                val l=pixels[x,y].luminance()
                if(if(light) l>.75f else l<.1f)ink++
            }
            Assert.assertTrue("Settings title has incorrect ink",ink>8)
        }
        assertTitleInk(false);shot("chrome-white-app-dark-options")
        ui.onNodeWithTag("reader-settings").performClick();waitTag("reader-appearance-close");openCustomization()
        ui.onNodeWithTag("listen-options-close").assertDoesNotExist();shot("chrome-white-app-dark-font")
        ui.onNodeWithTag("reader-appearance-close").performClick();waitTag("listen-settings")
        ui.runOnIdle {
            val koin=GlobalContext.get()
            koin.get<UiPreferences>().readerAppearance().set("light")
            koin.get<AppPreferences>().apply {
                backgroundColorReader().set(DomainColor(.09f,.095f,.1f));textColorReader().set(DomainColor(1f,1f,1f))
            }
        }
        ui.waitForIdle()
        ui.onNodeWithTag("listen-settings").performClick();waitTag("listen-options-close")
        assertTitleInk(true);shot("chrome-dark-app-light-options")
        ui.onNodeWithTag("listen-options-close").performClick();waitTag("listen-expanded-toggle")
        if(ui.onAllNodesWithTag("listen-speed-decrease").fetchSemanticsNodes().isNotEmpty()) {
            ui.onNodeWithTag("listen-speed-decrease").assertIsDisplayed()
            ui.onNodeWithTag("listen-speed-increase").assertIsDisplayed()
        } else for(step in 2..8)ui.onNodeWithTag("listen-speed-$step").assertIsDisplayed()
        shot("chrome-dark-app-light-expanded")
        ui.onNodeWithTag("listen-expanded-toggle").performTouchInput {click(Offset(width/2f,4f))}
        waitTag("listen-compact-previous")
        val previous=ui.onNodeWithTag("listen-compact-previous").fetchSemanticsNode().boundsInRoot
        val play=ui.onNodeWithTag("listen-play").fetchSemanticsNode().boundsInRoot
        val next=ui.onNodeWithTag("listen-compact-next").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue(kotlin.math.abs(previous.right-play.left)<=3f)
        Assert.assertTrue(kotlin.math.abs(play.right-next.left)<=3f)
        ui.onRoot().performTouchInput {swipe(Offset(width/2f,height*.5f),Offset(width/2f,height*.3f),500)}
        waitTag("reader-return-to-speech");shot("chrome-dark-app-light-compact-return")
    }
    @Test fun voiceManagerKeepsCompactPlayerBelowItAndWordChoicePersists() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();ui.onNodeWithTag("reader-listen").performClick();waitTag("listen-settings")
        ui.onNodeWithTag("listen-settings").performClick();waitTag("listen-options-close")
        ui.onNodeWithTag("listen-page-reading").performClick();waitTag("listen-highlight-words")
        ui.onNodeWithTag("listen-highlight-words").performClick()
        ui.onNodeWithTag("listen-highlight-words").assertIsSelected()
        ui.onNodeWithTag("listen-page-voice").performClick()
        ui.onNodeWithTag("listen-voice-choice").performClick();waitTag("voice-manager")
        val manager=ui.onNodeWithTag("voice-manager").fetchSemanticsNode().boundsInRoot
        val player=ui.onNodeWithTag("listen-compact-toggle").fetchSemanticsNode().boundsInRoot
        val label=ui.onNodeWithTag("listen-expand").fetchSemanticsNode().boundsInRoot
        val root=ui.onRoot().fetchSemanticsNode().boundsInRoot
        Assert.assertTrue("Player jumps above voice panel",player.top>=root.height*.65f)
        Assert.assertTrue("Voice manager overlaps compact player",manager.bottom<=player.top)
        Assert.assertTrue("Status label too near edge",label.left-player.left>=ui.activity.resources.displayMetrics.density*15)
        val pixels=ui.onRoot().captureToImage().toPixelMap()
        Assert.assertTrue("Voice selection adds a dark backing",pixels[8,pixels.height/3].luminance()>.95f)
        shot("voice-manager-bottom-player")
        ui.onNodeWithTag("voice-system").performClick();waitTag("reader-floating-task")
        ui.onNodeWithTag("voice-manager").assertDoesNotExist()
        val native=ui.onNodeWithTag("reader-floating-task").fetchSemanticsNode().boundsInRoot
        val compact=ui.onNodeWithTag("listen-compact-toggle").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue("System voice panel overlaps playback",native.bottom<=compact.top)
        shot("system-voice-panel")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
        waitTag("voice-manager")
    }
    @Test fun outsideSettingsAndImportRemainReadableWithGlassInBothThemes() {
        waitTag("books-account");ui.onNodeWithTag("books-account").performClick()
        for(mode in listOf("light","dark")) {
            ui.runOnIdle {GlobalContext.get().get<UiPreferences>().readerAppearance().set(mode)}
            waitTag("settings-general");ui.waitForIdle()
            val search=ui.onNodeWithTag("nav-search").captureToImage().toPixelMap()
            val probes=listOf(.2f to .5f,.8f to .5f,.5f to .2f,.5f to .8f).map{(x,y)->search[(search.width*x).toInt(),(search.height*y).toInt()]}
            Assert.assertTrue("White patches inside search capsule in $mode",probes.maxOf{it.red}-probes.minOf{it.red}<.03f)
            shot("outside-settings-$mode")
            ui.onNodeWithText("Giao diện").performClick()
            ui.waitUntil(15_000){ui.onAllNodesWithText("Hệ thống").fetchSemanticsNodes().isNotEmpty()}
            shot("outside-appearance-$mode")
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            ui.waitUntil(15_000){ui.onAllNodesWithText("Hệ thống").fetchSemanticsNodes().isEmpty()}
            ui.onNodeWithText("Cài đặt biên tập").performScrollTo().performClick()
            ui.waitUntil(15_000){ui.onAllNodesWithText("API key").fetchSemanticsNodes().isNotEmpty()}
            shot("outside-editing-$mode")
            ui.onNodeWithText("API key").assertIsDisplayed()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            ui.waitUntil(15_000){ui.onAllNodesWithText("API key").fetchSemanticsNodes().isEmpty()}
            ui.onNodeWithText("Nhập ebook").performClick()
            ui.waitUntil(15_000){ui.onAllNodesWithText("Nhập sách").fetchSemanticsNodes().isNotEmpty()}
            waitTag("import-epub");waitTag("import-pdf")
            shot("outside-import-$mode")
            ui.onNodeWithTag("import-epub").assertIsDisplayed();ui.onNodeWithTag("import-pdf").assertIsDisplayed()
            val pixels=ui.onNodeWithText("Nhập sách").captureToImage().toPixelMap()
            var ink=0
            for(y in 0 until pixels.height)for(x in 0 until pixels.width) {
                val l=pixels[x,y].luminance()
                if(if(mode=="dark")l>.7f else l<.15f)ink++
            }
            Assert.assertTrue("Import title unreadable in $mode",ink>8)
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressBack()
            ui.waitUntil(15_000){ui.onAllNodesWithText("Nhập sách").fetchSemanticsNodes().isEmpty()}
        }
    }
    @Test fun actualReaderFontPanelAndListeningPanelsDoNotOverlap() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls()
        shot("reader-before")
        val automation=InstrumentationRegistry.getInstrumentation().uiAutomation
        android.os.ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand("dumpsys gfxinfo ${ui.activity.packageName} reset")).use {it.readBytes()}
        for(i in 0..2) {
            val start=System.nanoTime()
            wakeControls()
            ui.onNodeWithTag("reader-settings").performClick()
            waitTag("reader-appearance-close");openCustomization()
            Log.i("TruyenPlusQA","Font panel open $i elapsedMs=${(System.nanoTime()-start)/1_000_000}")
            ui.onNodeWithTag("reader-font-picker").performClick()
            waitTag("reader-font-serif")
            ui.onNodeWithTag(if(i%2==0)"reader-font-serif" else "reader-font-sans").performClick()
            shot("reader-font-$i")
            val closing=System.nanoTime()
            ui.onNodeWithTag("reader-appearance-close").performClick()
            ui.onNodeWithTag("reader-appearance-close").assertDoesNotExist()
            Log.i("TruyenPlusQA","Font panel close $i elapsedMs=${(System.nanoTime()-closing)/1_000_000}")
            Log.i("TruyenPlusQA","Font panel cycle $i elapsedMs=${(System.nanoTime()-start)/1_000_000}")
        }
        wakeControls();ui.onNodeWithTag("reader-listen").performClick()
        waitTag("listen-settings")
        ui.onNodeWithTag("listen-settings").performClick()
        waitTag("listen-options-close")
        shot("reader-listening-settings")
        // The upper font settings replaces, rather than stacks with, the listening settings.
        ui.onNodeWithTag("reader-settings").performClick()
        waitTag("reader-appearance-panel")
        ui.onNodeWithTag("listen-options-close").assertDoesNotExist()
        shot("reader-appearance-listening")
        ui.onNodeWithTag("reader-appearance-close").performClick()
        waitTag("listen-expanded-toggle")
        ui.onNodeWithTag("listen-expanded-toggle").performTouchInput { click(Offset(width/2f,4f)) }
        waitTag("listen-compact-previous")
        ui.onNodeWithTag("listen-compact-next").assertIsDisplayed()
        shot("reader-compact")
        // Installed voices must be auditable and usable through the real manager, not just the factory.
        ui.onNodeWithTag("listen-settings").performClick();waitTag("listen-voice-choice")
        ui.onNodeWithTag("listen-voice-choice").performClick();waitTag("voice-manager")
        ui.onNodeWithTag("listen-options-close").assertDoesNotExist()
        ui.onNodeWithTag("voice-preview-piper-vi-vais-int8").assertIsEnabled().performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Đang nghe thử…").fetchSemanticsNodes().isNotEmpty()}
        shot("reader-voice-preview")
        ui.onNodeWithTag("voice-use-piper-vi-vais-int8").performClick()
        ui.onNodeWithTag("voice-manager").assertDoesNotExist()
        // Selecting a voice returns to the listening settings; close them before scrolling the chapter.
        if(ui.onAllNodesWithTag("listen-options-close").fetchSemanticsNodes().isNotEmpty())
            ui.onNodeWithTag("listen-options-close").performClick()
        waitTag("listen-play");ui.onNodeWithTag("listen-play").performClick()
        val controller=GlobalContext.get().get<TTSController>()
        ui.waitUntil(30_000){controller.state.value.isPlaying && controller.state.value.spokenSentence!=null}
        Assert.assertEquals("Piper",controller.state.value.engineName)
        shot("reader-spoken-sentence")
        ui.onRoot().performTouchInput {swipe(Offset(width/2f,height*.5f),Offset(width/2f,height*.25f),500)}
        ui.waitUntil(8000){ui.onAllNodesWithText("Về câu đang nghe").fetchSemanticsNodes().isNotEmpty()}
        shot("reader-manual-scroll")
        ui.onNodeWithText("Về câu đang nghe").performClick()
        ui.onNodeWithText("Về câu đang nghe").assertDoesNotExist()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val metrics=File(context.getExternalFilesDir(null),"qa-metrics").apply {mkdirs()}
        for(tool in listOf("gfxinfo","meminfo")) {
            val fd=automation.executeShellCommand("dumpsys $tool ${context.packageName}")
            File(metrics,"reader-$tool.txt").writeBytes(android.os.ParcelFileDescriptor.AutoCloseInputStream(fd).use {it.readBytes()})
        }
    }
    @Test fun firstLaunchActionsFollowTheSelectedLanguage() {
        ui.waitUntil(15_000){ui.onAllNodesWithText("English").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("English").performClick()
        ui.waitUntil(15_000){ui.onAllNodesWithText("Start reading").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Start reading").performScrollTo().performClick()
        ui.waitUntil(15_000){ui.onAllNodesWithText("Select Storage Folder").fetchSemanticsNodes().isNotEmpty()}
        ui.onNodeWithText("Select Storage Folder").assertIsDisplayed()
        ui.onNodeWithText("extensions",substring=true).assertDoesNotExist()
        shot("onboarding-english")
    }

    @Test fun embeddedPlaybackUsesTheReadersChapterListAndChangesChapter() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();ui.onNodeWithTag("reader-listen").performClick();waitTag("listen-next-chapter")
        val controller=GlobalContext.get().get<TTSController>()
        ui.waitUntil(30_000){controller.state.value.chapter?.id==888101L && !controller.state.value.isLoading}
        ui.onNodeWithTag("listen-previous-chapter").assertIsNotEnabled()
        ui.onNodeWithTag("listen-next-chapter").assertIsEnabled().performClick()
        ui.waitUntil(30_000){controller.state.value.chapter?.id==888102L && !controller.state.value.isLoading}
        ui.onNodeWithTag("listen-previous-chapter").assertIsEnabled()
        ui.onNodeWithTag("listen-next-chapter").assertIsEnabled().performClick()
        ui.waitUntil(30_000){controller.state.value.chapter?.id==888103L && !controller.state.value.isLoading}
        ui.onNodeWithTag("listen-previous-chapter").assertIsEnabled()
        ui.onNodeWithTag("listen-next-chapter").assertIsNotEnabled()
        controller.dispatch(TTSCommand.Pause)
        shot("listening-next-chapter")
    }

    @Test fun paragraphListenFromHereChangesTheRealPlaybackPosition() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();ui.onNodeWithTag("reader-listen").performClick();waitTag("listen-settings")
        val controller=GlobalContext.get().get<TTSController>()
        ui.waitUntil(30_000){controller.state.value.chapter?.id==888101L && !controller.state.value.isLoading}
        controller.dispatch(TTSCommand.Pause)
        ui.waitUntil(10_000){!controller.state.value.isPlaying}
        val expected=controller.state.value.paragraphSourceIndices.indexOf(0)
        Assert.assertTrue("First reader paragraph has no speech mapping",expected>=0)
        controller.dispatch(TTSCommand.JumpToParagraph(expected+5))
        ui.waitUntil(10_000){controller.state.value.currentParagraphIndex==expected+5}
        wakeControls()
        ui.onAllNodes(hasText("Hoàng Yên Trần khẽ gật đầu.",substring=true) and hasClickAction()).onFirst().performClick()
        waitTag("reader-listen-from-here")
        ui.onNodeWithTag("reader-listen-from-here").performClick()
        ui.waitUntil(10_000){controller.state.value.currentParagraphIndex==expected}
        ui.onNodeWithTag("reader-listen-from-here").assertDoesNotExist()
        shot("listening-from-selected-paragraph")
    }

    @Test fun largeSystemFontKeepsTheFirstParagraphBelowTheFloatingHeader() {
        waitTag("home-continue");ui.onNodeWithTag("home-continue").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls()
        // At large font the secondary book label is deliberately omitted; a long
        // chapter title may ellipsize horizontally, but its visible line must fit.
        for(label in listOf("Chương 1: Gió đêm")) {
            var layout:androidx.compose.ui.text.TextLayoutResult?=null
            ui.onNode(hasText(label) and hasAnyAncestor(hasTestTag("reader-top-capsule")),useUnmergedTree=true).performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult) { action->
                val values=mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
                Assert.assertTrue(action(values));layout=values.first()
            }
            Assert.assertTrue("Reader ignores large system font",layout!!.layoutInput.density.fontScale>=1.9f)
            Assert.assertTrue("Header clips $label vertically",layout!!.getLineBottom(0)<=layout!!.size.height)
        }
        ui.onNode(hasText("Kiểm tra đọc và nghe") and hasAnyAncestor(hasTestTag("reader-top-capsule")),useUnmergedTree=true).assertDoesNotExist()
        val header=ui.onNodeWithTag("reader-top-capsule").fetchSemanticsNode().boundsInRoot
        val first=ui.onAllNodes(hasText("Hoàng Yên Trần khẽ gật đầu.",substring=true) and SemanticsMatcher.keyIsDefined(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult),useUnmergedTree=true)
            .fetchSemanticsNodes().filter {it.boundsInRoot.height>0}.minBy {it.boundsInRoot.top}.boundsInRoot
        Assert.assertTrue("Header overlaps the first paragraph: $header, $first",first.top>=header.bottom)
        ui.onNodeWithTag("reader-settings").performClick();waitTag("reader-appearance-close");openCustomization()
        ui.onNodeWithTag("reader-font-increase").assertIsDisplayed()
        shot("reading-large-system-font")
    }

    @Test fun appleLibraryCollectionPickerAndBookActionsKeepUsableBounds() {
        waitTag("nav-Library");ui.onNodeWithTag("nav-Library").performClick()
        waitTag("library-collections");ui.onNodeWithTag("library-collections").performClick()
        waitTag("collection-choice-0")
        val row=ui.onNodeWithTag("collection-choice-0").fetchSemanticsNode().boundsInRoot
        Assert.assertTrue("Collection picker touch area clipped: $row",row.height>=48*ui.activity.resources.displayMetrics.density)
        shot("apple-collections")
        ui.onNodeWithTag("collection-choice-0").performClick()
        waitTag("library-details-888100");ui.onNodeWithTag("library-details-888100").performClick()
        waitTag("book-read");ui.onNodeWithTag("book-read").assertIsEnabled().assertIsDisplayed()
        ui.onNodeWithTag("book-listen").assertIsEnabled().assertIsDisplayed()
        shot("apple-book-details")
        ui.onAllNodesWithText("%%",substring=true).assertCountEquals(0)
        ui.onNodeWithTag("book-read").performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();ui.onNodeWithTag("reader-settings").performClick()
        waitTag("reader-paper-next")
        val pane=ui.onNodeWithTag("reader-appearance-panel").fetchSemanticsNode().boundsInRoot
        for(tag in listOf("reader-paper-next","reader-paper-options")) {
            val n=ui.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            Assert.assertTrue("Paper control clipped: $tag $n / $pane",n.bottom<=pane.bottom && n.height>=48*ui.activity.resources.displayMetrics.density)
        }
        shot("apple-reading-paper-palette")
        ui.onNodeWithTag("reader-paper-next").performClick()
        shot("apple-reading-paper-palette-more")
        ui.onNodeWithTag("reader-paper-options").performClick()
        ui.onNodeWithContentDescription("Độ sáng tự động").assertIsDisplayed()
        shot("apple-reading-paper-brightness")
    }

    @Test fun booksRootNavigationSearchAccountAndReadingRemainConnected() {
        val device=UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        waitTag("nav-Home");waitTag("home-continue");shot("books-home")
        ui.onNodeWithTag("nav-Shelf").performClick();waitTag("books-open-search")
        ui.onNodeWithTag("nav-Shelf").assertIsSelected();shot("books-repository")
        ui.onNodeWithTag("books-account").performClick();waitTag("books-account-close");shot("books-settings")
        ui.onNodeWithTag("books-account-close").performClick();waitTag("nav-Shelf");ui.onNodeWithTag("nav-Shelf").assertIsSelected()
        ui.onNodeWithTag("books-open-search").performClick();waitTag("books-search-workspace")
        ui.onNodeWithTag("search-scope-picker").performClick();waitTag("search-scope-shelf")
        ui.onNodeWithTag("search-scope-shelf").assertIsSelected().performClick()
        ui.onNodeWithTag("books-search-field").performTextInput("Vạn Cổ")
        shot("books-search-repository")
        ui.onNodeWithTag("search-return").performClick();waitTag("nav-Shelf");ui.onNodeWithTag("nav-Shelf").assertIsSelected()
        ui.onNodeWithTag("nav-Library").performClick();waitTag("library-book-888100");shot("books-library-grid")
        ui.onNodeWithTag("library-collections").performClick();waitTag("books-collection-sheet");shot("books-collections")
        ui.onNodeWithTag("collection-choice-0").performClick();waitTag("library-book-888100")
        ui.onNodeWithTag("library-organize").performClick();waitTag("library-edit")
        ui.onNodeWithTag("library-edit").performClick();ui.onNodeWithTag("library-book-888100").performClick()
        ui.waitUntil(15_000){ui.onNodeWithTag("library-book-888100").fetchSemanticsNode().config.getOrNull(androidx.compose.ui.semantics.SemanticsProperties.Selected)==true}
        ui.onNodeWithTag("library-edit").performClick();waitTag("nav-Library")
        ui.onNodeWithTag("library-book-888100").assertIsNotSelected()
        ui.onNodeWithTag("library-organize").performClick();waitTag("library-layout")
        ui.onNodeWithTag("library-layout").performClick();waitTag("library-details-888100");shot("books-library-list")
        ui.onNodeWithTag("library-details-888100").performClick();waitTag("book-read")
        ui.onNodeWithTag("nav-Library").assertIsSelected();ui.onNodeWithTag("book-detail-close").performClick()
        waitTag("library-details-888200");ui.onNodeWithTag("library-details-888200").performScrollTo().performClick();waitTag("book-read")
        ui.onNodeWithTag("book-detail-title").assertTextEquals(runBlocking {GlobalContext.get().get<BookRepository>().findBookById(888200)!!.title});ui.onNodeWithTag("nav-Library").assertIsSelected()
        ui.onNodeWithTag("book-detail-close").performClick();waitTag("library-details-888100")
        ui.onNodeWithTag("nav-search").performClick();waitTag("books-search-field")
        ui.onNodeWithTag("search-scope-picker").performClick();waitTag("search-scope-library")
        ui.onNodeWithTag("search-scope-library").performClick()
        val title=runBlocking {GlobalContext.get().get<BookRepository>().findBookById(888100)!!.title}
        ui.onNodeWithTag("books-search-field").performTextInput(title.take(10))
        waitTag("library-book-888100");shot("books-search-library")
        ui.onNodeWithTag("library-book-888100").performClick();waitTag("book-read");ui.onNodeWithTag("book-context-close").assertIsDisplayed();shot("books-details")
        ui.onNodeWithTag("book-read").performScrollTo().performClick()
        ui.waitUntil(30_000){ui.onAllNodesWithText("Hoàng Yên Trần khẽ gật đầu.",substring=true).fetchSemanticsNodes().isNotEmpty()}
        wakeControls();shot("books-reader")
        ui.onNodeWithTag("reader-settings").performClick();waitTag("reader-appearance-home");shot("books-themes")
        ui.onNodeWithTag("reader-appearance-customize").performClick();waitTag("reader-font-picker");shot("books-customize")
        ui.onNodeWithTag("reader-appearance-close").performClick()
        wakeControls();ui.onNodeWithTag("reader-listen").performClick();waitTag("listen-settings");shot("books-listening")
        ui.onNodeWithTag("listen-settings").performClick();waitTag("listen-options-close");shot("books-listening-settings")
        ui.onNodeWithTag("listen-options-close").performClick()
        wakeControls();ui.onNodeWithTag("reader-close").performClick();waitTag("book-read");device.pressBack();waitTag("books-search-workspace")
        ui.onNodeWithTag("search-return").performClick();waitTag("nav-Library");ui.onNodeWithTag("nav-Library").assertIsSelected()
    }

}
