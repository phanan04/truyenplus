package org.ireader.app

import android.util.Log
import androidx.compose.ui.test.*
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
            koin.get<UiPreferences>().apply {hasCompletedOnboarding().set(true);hasCompletedFirstLaunch().set(true);language().set("vi")}
            koin.get<ReaderPreferences>().apply {ttsFollowParagraph().set(true);ttsHighlightText().set(true);ttsHighlightWords().set(false)}
            if(description.methodName=="pageThemeOverridesAppThemeAndChromeDoesNotClipChapter") {
                koin.get<UiPreferences>().apply {readerAppearance().set("dark");readerFollowsAppearance().set(false)}
                koin.get<AppPreferences>().apply {
                    backgroundColorReader().set(DomainColor(1f,1f,1f));textColorReader().set(DomainColor(0f,0f,0f))
                }
            }
            runBlocking {
                val id=koin.get<BookRepository>().upsert(Book(id=888100,sourceId=-200,title="Kiểm tra đọc và nghe",key="qa-ui",favorite=true,initialized=true))!!
                val chapters=(1..3).map {i->Chapter(id=888100+i.toLong(),bookId=id,key="qa-ui-$i",name="Chương $i: Gió đêm",
                    sourceOrder=i.toLong(),number=i.toFloat(),read=i==1,content=List(30){Text("Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua, mang hương hoa thoang thoảng. Nàng nhìn về phía ngọn núi xa.")})}
                koin.get<ChapterRepository>().insertChapters(chapters)
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
    private fun waitTag(tag:String) {ui.waitUntil(30_000){ui.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty()}}
    private fun wakeControls() {
        if(ui.onAllNodesWithTag("reader-settings").fetchSemanticsNodes().isEmpty()) {
            ui.onRoot().performTouchInput {click(Offset(width/2f,height/3f))}
            waitTag("reader-settings")
        }
    }
    private fun shot(name:String) {
        ui.waitForIdle()
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        val out=File(context.getExternalFilesDir(null),"qa-screens/$name.png").apply {parentFile!!.mkdirs()}
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).takeScreenshot(out)
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
        ui.onNodeWithTag("reader-settings").performClick();waitTag("reader-appearance-close")
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
        for(step in 2..8)ui.onNodeWithTag("listen-speed-$step").assertIsDisplayed()
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
        shot("voice-manager-bottom-player")
        ui.onNodeWithTag("voice-system").performClick()
    }
    @Test fun outsideSettingsAndImportRemainReadableWithGlassInBothThemes() {
        waitTag("nav-Settings");ui.onNodeWithTag("nav-Settings").performClick()
        for(mode in listOf("light","dark")) {
            ui.runOnIdle {GlobalContext.get().get<UiPreferences>().readerAppearance().set(mode)}
            waitTag("settings-general");ui.waitForIdle()
            shot("outside-settings-$mode")
            ui.onNodeWithText("Nhập ebook").performClick()
            ui.waitUntil(15_000){ui.onAllNodesWithText("Nhập sách").fetchSemanticsNodes().isNotEmpty()}
            ui.waitUntil(15_000){runCatching {ui.onNodeWithText("Chọn tệp từ thiết bị").isDisplayed()}.getOrDefault(false)}
            shot("outside-import-$mode")
            ui.onNodeWithText("Chọn tệp từ thiết bị").assertIsDisplayed()
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
        for(i in 0..2) {
            val start=System.nanoTime()
            wakeControls()
            ui.onNodeWithTag("reader-settings").performClick()
            waitTag("reader-appearance-close")
            ui.onNodeWithTag("reader-font-picker").performClick()
            waitTag("reader-font-serif")
            ui.onNodeWithTag(if(i%2==0)"reader-font-serif" else "reader-font-sans").performClick()
            shot("reader-font-$i")
            ui.onNodeWithTag("reader-appearance-close").performClick()
            ui.onNodeWithTag("reader-appearance-close").assertDoesNotExist()
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
    }
}
