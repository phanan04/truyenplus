package org.ireader.app

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import ireader.core.source.model.Text
import ireader.domain.data.repository.*
import ireader.domain.models.entities.*
import ireader.domain.preferences.prefs.*
import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.koin.core.context.GlobalContext

/** Real Android Choreographer and injected gestures: deliberately no Compose test clock. */
class ReaderNativeJourneyTest {
    @Test fun nativeNavigationReaderAndListeningHaveNoFrozenFrames() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        val context=instrumentation.targetContext
        check(context.packageName.endsWith(".debug.qa"))
        val koin=GlobalContext.get()
        val fullGlass=InstrumentationRegistry.getArguments().getString("fullGlass")=="true"
        koin.get<UiPreferences>().readerFullGlass().set(fullGlass)
        koin.get<UiPreferences>().apply {
            hasCompletedOnboarding().set(true);hasCompletedFirstLaunch().set(true);language().set("vi");readerAppearance().set("light")
        }
        koin.get<ireader.domain.services.processstate.ProcessStateManager>().clearReaderState()
        koin.get<ireader.domain.services.tts_service.v2.offline.OfflineVoiceStore>().select(null)
        koin.get<AppPreferences>().capCutVoiceId().set("")
        koin.get<AppPreferences>().appUpdater().set(true)
        runBlocking {
            val id=koin.get<BookRepository>().upsert(Book(id=888100,sourceId=-200,title="Đo hiệu năng đọc nghe",key="qa-ui",favorite=true,initialized=true))!!
            val chapters=(1..3).map {i->Chapter(id=888100+i.toLong(),bookId=id,key="qa-ui-$i",name="Chương $i: Gió đêm",sourceOrder=i.toLong(),number=i.toFloat(),read=i==1,
                content=List(30){Text("Hoàng Yên Trần khẽ gật đầu. Gió đêm thổi qua, mang hương hoa thoang thoảng. Nàng nhìn về phía ngọn núi xa.")})}
            koin.get<ChapterRepository>().insertChapters(chapters)
            koin.get<HistoryRepository>().insertHistory(History(id=-1,chapterId=888101,readAt=System.currentTimeMillis(),readDuration=5000,progress=.1f))
        }
        val activity=instrumentation.startActivitySync(Intent(context,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val device=UiDevice.getInstance(instrumentation)
        // Follow/highlight animations can keep accessibility busy longer than chrome's
        // five-second timeout. Use our explicit waits instead of UiAutomator's 10s idle wait.
        val automationConfig=androidx.test.uiautomator.Configurator.getInstance()
        val originalIdleTimeout=automationConfig.waitForIdleTimeout
        automationConfig.waitForIdleTimeout=100
        val controller=koin.get<TTSController>()
        val frames=mutableListOf<org.json.JSONObject>()
        val actions=mutableListOf<org.json.JSONObject>()
        fun present(selector:BySelector)=device.wait(Until.hasObject(selector),15_000)==true
        fun waitFor(selector:BySelector) {Assert.assertTrue("Missing UI $selector",present(selector))}
        fun click(value:String,label:String,probe:ReaderUiFrameProbe,text:Boolean=false) {
            val selector=if(text)By.text(value) else By.desc(value)
            var target=device.wait(Until.findObject(selector),500)
            // One margin tap reveals hidden controls. A caller must not pre-toggle them:
            // two independent wake attempts can hide the controls again.
            if(target==null && value in setOf("Nghe truyện","Chủ đề và cài đặt","Tùy chỉnh nghe")) {
                device.click(4,device.displayHeight/3)
                target=device.wait(Until.findObject(selector),2000)
            }
            Assert.assertNotNull("Missing UI $value",target)
            // Navigation can replace the accessibility node between lookup and bounds.
            // Re-query before any input; never retry a click that was already sent.
            var bounds:android.graphics.Rect?=null
            repeat(5) {
                if(bounds==null) {
                    try {bounds=device.findObject(selector)?.visibleBounds}
                    catch(_:androidx.test.uiautomator.StaleObjectException) { }
                    if(bounds==null)Thread.sleep(30)
                }
            }
            val rect=requireNotNull(bounds) {"No stable bounds for $value"}
            probe.actionLabel=label
            val start=android.os.SystemClock.elapsedRealtime()
            Assert.assertTrue(device.click(rect.centerX(),rect.centerY()))
            actions.add(org.json.JSONObject().put("label",label).put("inputMs",android.os.SystemClock.elapsedRealtime()-start))
            Thread.sleep(350)
        }
        fun probe(label:String):ReaderUiFrameProbe {
            lateinit var value:ReaderUiFrameProbe
            instrumentation.runOnMainSync {value=ReaderUiFrameProbe(activity.window,label)}
            return value
        }
        fun stop(value:ReaderUiFrameProbe) {frames.add(value.snapshot());instrumentation.runOnMainSync {value.close()}}
        fun awaitState(predicate:()->Boolean) {
            val end=android.os.SystemClock.elapsedRealtime()+30_000
            while(!predicate() && android.os.SystemClock.elapsedRealtime()<end)Thread.sleep(50)
            Assert.assertTrue("Playback state not reached: ${controller.state.value.playbackState}",predicate())
        }
        try {
            waitFor(By.desc("Trang chủ"));Thread.sleep(1000)
            val legacyChecks=androidx.work.WorkManager.getInstance(context)
                .getWorkInfosByTag(ireader.domain.services.update_service.UpdateService::class.java.name).get()
            Assert.assertTrue("Optimized runtime activated the inherited IReader updater",legacyChecks.isEmpty())
            val sampling=InstrumentationRegistry.getArguments().getString("sampling")=="true"
            if(sampling)android.os.Debug.startMethodTracingSampling(
                java.io.File(context.getExternalFilesDir(null),"glass-native-methods.trace").absolutePath,32*1024*1024,500)
            val shell=probe("native-shell")
            try {
                repeat(4) {cycle->
                    for(tab in listOf("Thư viện","Kho truyện","Trang chủ"))click(tab,"$tab-$cycle",shell)
                    click("Cài đặt","account-open-$cycle",shell)
                    waitFor(By.text("Cài đặt"))
                    if(cycle==0)repeat(2) {toggle->
                        click("Glass toàn hệ thống","glass-toggle-$toggle",shell,text=true)
                        awaitState {koin.get<UiPreferences>().readerFullGlass().get()==if(toggle==0)!fullGlass else fullGlass}
                    }
                    device.pressBack();Thread.sleep(350)
                }
            } finally {stop(shell);if(sampling)android.os.Debug.stopMethodTracing()}
            val reader=probe("native-reader")
            try {
                click("Đo hiệu năng đọc nghe","open-reader",reader,text=true)
                waitFor(By.textContains("Hoàng Yên Trần khẽ gật đầu."))
                repeat(4) {i->
                    click("Chủ đề và cài đặt","font-open-$i",reader)
                    waitFor(By.text("Chủ đề và cài đặt"));click("Đóng","font-close-$i",reader)
                }
                click("Nghe truyện","open-listening",reader)
                awaitState {controller.state.value.chapter?.id==888101L && !controller.state.value.isLoading}
                if(!controller.state.value.isPlaying)controller.dispatch(TTSCommand.Play)
                awaitState {controller.state.value.isPlaying && controller.state.value.spokenSentence!=null}
                val paragraph=controller.state.value.currentParagraphIndex
                repeat(4) {i->
                    click("Tùy chỉnh nghe","listen-open-$i",reader)
                    waitFor(By.text("Tùy chỉnh nghe"));click("Đóng","listen-close-$i",reader)
                }
                reader.actionLabel="scroll-while-speaking"
                repeat(6) {device.swipe(device.displayWidth/2,device.displayHeight*55/100,device.displayWidth/2,device.displayHeight*32/100,24);Thread.sleep(100)}
                awaitState {controller.state.value.currentParagraphIndex>paragraph}
                Assert.assertTrue(controller.state.value.isPlaying || controller.state.value.isLoading)
                controller.dispatch(TTSCommand.Pause)
                awaitState {!controller.state.value.isPlaying && !controller.state.value.isLoading}
            } finally {stop(reader)}
            val idle=probe("native-reader-paused-idle")
            try {Thread.sleep(5000)} finally {stop(idle)}
        } catch(error:Throwable) {
            val diagnostic=java.io.File(context.getExternalFilesDir(null),"qa-native-failure.xml")
            device.dumpWindowHierarchy(diagnostic)
            device.takeScreenshot(java.io.File(context.getExternalFilesDir(null),"qa-native-failure.png"))
            android.util.Log.e("TruyenPlusQA","Native journey failed: ${diagnostic.readText()}")
            throw error
        } finally {
            automationConfig.waitForIdleTimeout=originalIdleTimeout
            ReaderUiFrameProbe.save(if(fullGlass)"native-journey-full-glass" else "native-journey",frames,actions)
            controller.dispatch(TTSCommand.Cleanup)
            TTSV2ServiceStarter(context).stopService()
            koin.get<UiPreferences>().readerFullGlass().set(false)
            instrumentation.runOnMainSync {activity.finish()}
        }
        Assert.assertEquals("Native user journey has frozen frames",0,frames.sumOf {it.getInt("frozen700ms")})
    }
}
