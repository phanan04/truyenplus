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
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.koin.core.context.GlobalContext
import java.io.File

@RunWith(AndroidJUnit4::class)
class ReaderUiDeviceTest {
    @get:Rule(order=0) val seed = TestRule { base, _ -> object:Statement() {
        override fun evaluate() {
            val context=InstrumentationRegistry.getInstrumentation().targetContext
            check(context.packageName.endsWith(".debug.qa"))
            val koin=GlobalContext.get()
            koin.get<UiPreferences>().apply {hasCompletedOnboarding().set(true);hasCompletedFirstLaunch().set(true);language().set("vi")}
            koin.get<ReaderPreferences>().apply {ttsFollowParagraph().set(true);ttsHighlightText().set(true)}
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
