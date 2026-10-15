package org.ireader.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.services.tts_service.v2.*
import ireader.domain.services.tts_service.v2.offline.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.FixMethodOrder
import org.junit.runners.MethodSorters
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

/** Runs only in the isolated .debug.qa application. No production library or settings are touched. */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class ReaderTtsDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun requireQa() {
        check(context.packageName.endsWith(".debug.qa"))
        GlobalContext.get().get<ireader.domain.preferences.prefs.AppPreferences>().capCutVoiceId().set("")
    }
    private suspend fun ready(engine: TTSEngine) = withTimeout(30_000) {
        while (!engine.isReady()) delay(30)
    }
    private suspend fun speakAndCheck(engine: TTSEngine, text: String, id: String) = coroutineScope {
        val seen = mutableListOf<EngineEvent>()
        val start = System.nanoTime()
        val done = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(60_000) {
                engine.events.first {
                    seen += it
                    it is EngineEvent.Completed && it.utteranceId == id || it is EngineEvent.Error
                }
            }
        }
        engine.speak(text, id)
        assertTrue("Completion: $seen", done.await() is EngineEvent.Completed)
        assertTrue("No playback start: $seen", seen.any { it is EngineEvent.Started && it.utteranceId == id })
        assertTrue("No actual range: $seen", seen.any { it is EngineEvent.RangeStarted && it.utteranceId == id && it.end > it.start })
        val ranges=seen.filterIsInstance<EngineEvent.RangeStarted>()
        assertTrue("Chunk boundaries must remain coarse",ranges.any {!it.isWordBoundary})
        for(range in ranges.filter {it.isWordBoundary}) {
            assertTrue("Invalid real boundary",range.start>=0 && range.end<=text.length && range.end>range.start)
        }
        if(engine.name!="Native Android TTS")assertTrue("Offline engine must not invent word timing",ranges.none {it.isWordBoundary})
        Log.i("TruyenPlusQA","$id actualWordBoundaries=${ranges.count {it.isWordBoundary}} coarseRanges=${ranges.count {!it.isWordBoundary}}")
        Log.i("TruyenPlusQA", "PASS $id engine=${engine.name} elapsedMs=${(System.nanoTime()-start)/1_000_000}")
    }

    @Test fun aDownloadedPiperInstallsSelectsAndPlays() = runBlocking<Unit> {
        requireQa()
        val store = GlobalContext.get().get<OfflineVoiceStore>()
        val model = OfflineVoiceCatalog.models.first()
        store.select(null); store.remove(model.id)
        assertNull(store.config(model.id))
        store.startDownload(model.id)
        val result = withTimeout(300_000) { store.state.first { model.id in it.installed || model.id in it.errors } }
        assertFalse("Official model download/install failed", model.id in result.errors)
        assertTrue(store.select(model.id))
        val engine = TTSEngineFactory.createNativeEngine()
        try {
            ready(engine)
            assertEquals(model.title, engine.name)
            speakAndCheck(engine, "Gió đêm thổi qua. Hoàng Yên Trần khẽ gật đầu.", "qa-piper")
            assertEquals("Fallback must not pass", model.title, engine.name)
            engine.setSpeed(2f); engine.setPitch(1.25f)
            speakAndCheck(engine, "Chào bạn, đây là giọng đọc tiếng Việt.", "qa-piper-speed")
            engine.speak("Đoạn này được dừng để kiểm tra việc hủy phát âm thanh.", "qa-cancel")
            delay(150); engine.pause(); delay(250)
            engine.setSpeed(1f); engine.setPitch(1f)
            speakAndCheck(engine, "Tiếp tục nghe sau khi tạm dừng.", "qa-piper-resume")
        } finally { engine.release(); store.select(null) }
        // Keep the verified model installed for the background lifecycle test.
    }

    @Test fun vieneuVoicesReallyPlayThroughAudioTrack() = runBlocking<Unit> {
        requireQa()
        val model = OfflineVoiceCatalog.models.last()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        if(store.config(model.id)==null) {
            store.startDownload(model.id)
            val result=withTimeout(600_000){store.state.first {model.id in it.installed || model.id in it.errors}}
            assertFalse("Official VieNeu Nano model download/install failed",model.id in result.errors)
        }
        for (speaker in listOf(1, 9)) {
            val config = model.config(context.noBackupFilesDir.resolve("voices").absolutePath, speaker)
            val engine = TTSEngineFactory.createOfflineEngine(config)!!
            try {
                ready(engine)
                assertTrue(engine.name.startsWith(model.title))
                speakAndCheck(engine, "Chào bạn, đây là giọng đọc tiếng Việt.", "qa-vieneu-$speaker")
                assertTrue("Fallback must not pass", engine.name.startsWith(model.title))
            } finally { engine.release() }
        }
    }

    @Test fun systemEngineReallyStartsCompletesAndReportsRanges() = runBlocking<Unit> {
        requireQa()
        val store = GlobalContext.get().get<OfflineVoiceStore>()
        store.select(null)
        val engine = TTSEngineFactory.createNativeEngine()
        try {
            ready(engine)
            assertEquals("Native Android TTS", engine.name)
            speakAndCheck(engine, "Chào bạn. Đây là giọng đọc hệ thống.", "qa-system")
        } finally { engine.release() }
    }

    @Test fun piperPreparesTheFollowingParagraphBeforeCurrentPlaybackFinishes() = runBlocking<Unit> {
        requireQa()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        val config=store.config("piper-vi-vais-int8") ?: error("Install Piper in the QA app first")
        val engine=TTSEngineFactory.createOfflineEngine(config)!!
        checkParagraphContinuity(engine,"Piper")
    }

    @Test fun systemParagraphsContinueWithoutAnArtificialSynthesisDelay() = runBlocking<Unit> {
        requireQa()
        GlobalContext.get().get<OfflineVoiceStore>().select(null)
        checkParagraphContinuity(TTSEngineFactory.createNativeEngine(),"Native Android TTS")
    }

    @Test fun vieneuPreparesTheFollowingParagraphBeforeCurrentPlaybackFinishes() = runBlocking<Unit> {
        requireQa()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        val config=store.config("vieneu-v3-nano-vi",9) ?: error("Install Nano in the QA app first")
        checkParagraphContinuity(TTSEngineFactory.createOfflineEngine(config)!!,config.model.title)
    }

    private suspend fun checkParagraphContinuity(engine:TTSEngine,expectedName:String)=coroutineScope {
        val texts=listOf(
            "Gió đêm thổi qua, mang theo hương hoa thoang thoảng. Hoàng Yên Trần khẽ gật đầu, rồi chậm rãi đi về phía ngọn núi xa.",
            "Trương Nhược Trần nhìn lên bầu trời, lắng nghe tiếng gió và tiếp tục hành trình.",
            "Chương mới bắt đầu, câu chuyện vẫn tiếp tục.")
        val gaps=mutableListOf<Long>()
        val finished=CompletableDeferred<Unit>()
        var previousCompleted=0L
        val observer=launch(start=CoroutineStart.UNDISPATCHED) {engine.events.collect {event ->
            when(event) {
                is EngineEvent.Started -> if(previousCompleted>0) {
                    val gap=(System.nanoTime()-previousCompleted)/1_000_000
                    gaps+=gap;previousCompleted=0L
                    Log.i("TruyenPlusQA","${engine.name} prepared paragraph playback gapMs=$gap")
                }
                is EngineEvent.Completed -> {
                    val index=event.utteranceId.removePrefix("qa-lookahead-").toInt()
                    if(index==texts.lastIndex)finished.complete(Unit) else {
                        previousCompleted=System.nanoTime()
                        engine.speak(texts[index+1],"qa-lookahead-${index+1}")
                        engine.precacheNext(texts.getOrNull(index+2)?.let {listOf("next" to it)} ?: emptyList())
                    }
                }
                is EngineEvent.Error -> finished.completeExceptionally(AssertionError(event.toString()))
                else -> Unit
            }
        }}
        try {
            ready(engine)
            engine.speak(texts.first(),"qa-lookahead-0")
            engine.precacheNext(listOf("next" to texts[1]))
            withTimeout(120_000){finished.await()}
            assertTrue("Unexpected fallback: ${engine.name}",engine.name.startsWith(expectedName))
            assertEquals(2,gaps.size)
            assertTrue("Unexpected synthesis wait between prepared paragraphs: $gaps",gaps.all {it<750})
        } finally {observer.cancel();engine.release()}
    }

    @Test fun capCutTimeoutReportsAnErrorInsteadOfLeavingLoadingForever()=runBlocking<Unit> {
        requireQa()
        val voice=ireader.domain.services.tts_service.v2.online.CapCutVoices.voices.first()
        val source=object:ireader.domain.services.tts_service.v2.online.CapCutSpeechSource {
            override suspend fun synthesize(text:String,voice:ireader.domain.services.tts_service.v2.online.CapCutVoice):ByteArray = withTimeout(1) {delay(1000);byteArrayOf()}
        }
        val engine=ireader.domain.services.tts_service.v2.online.AndroidCapCutTtsEngine(context,source,voice)
        try {
            val error=async(start=CoroutineStart.UNDISPATCHED){withTimeout(5000){engine.events.first {it is EngineEvent.Error}}}
            engine.speak("Một câu kiểm thử.","qa-timeout")
            assertFalse((error.await() as EngineEvent.Error).retryable)
        } finally {engine.release()}
    }

    @Test fun capCutOnlineVoicesReallyDownloadPlayAndCancel()=runBlocking<Unit> {
        requireQa()
        for(voice in ireader.domain.services.tts_service.v2.online.CapCutVoices.voices) {
            val engine=TTSEngineFactory.createCapCutEngine(voice.id) ?: error("CapCut unavailable")
            try {
                ready(engine)
                speakAndCheck(engine,"Chào bạn, đây là giọng đọc tiếng Việt. Gió đêm thổi qua.","qa-capcut-${voice.id}")
                assertEquals("CapCut · ${voice.title}",engine.name)
                engine.speak("Đây là đoạn cần hủy ngay.","qa-capcut-cancel");delay(50);engine.stop()
                engine.setSpeed(1.5f)
                speakAndCheck(engine,"Tiếp tục nghe sau khi dừng.","qa-capcut-resume-${voice.id}")
            } finally {engine.release()}
        }
    }

    @Test fun allInstalledEnginesMeasureRealControllerParagraphLatencyAtBothSpeeds() = runBlocking<Unit> {
        requireQa()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        val texts=listOf("Gió đêm thổi qua, mang theo hương hoa thoang thoảng. Hoàng Yên Trần khẽ gật đầu, rồi chậm rãi đi về phía ngọn núi xa.",
            "Trương Nhược Trần nhìn lên bầu trời, lắng nghe tiếng gió và tiếp tục hành trình.",
            "Chương mới bắt đầu, câu chuyện vẫn tiếp tục.")
        val book=Book(id=900090,sourceId=-1,title="QA latency",key="qa-latency")
        val chapter=Chapter(id=900091,bookId=book.id,key="qa-latency-1",name="Chương 1")
        val loader=object:TTSContentLoader {
            override suspend fun loadChapter(bookId:Long,chapterId:Long)=TTSContentLoader.ChapterContent(book,chapter,texts,texts.indices.toList())
            override suspend fun getNextChapterId(bookId:Long,currentChapterId:Long):Long?=null
            override suspend fun getPreviousChapterId(bookId:Long,currentChapterId:Long):Long?=null
            override fun subscribeChapters(bookId:Long)=flowOf(listOf(chapter))
        }
        val engines=listOf("system","piper-vi-vais-int8","vieneu-v3-nano-vi","capcut")
        for(id in engines)for(rate in listOf(1f,2f)) {
            val config=if(id=="system" || id=="capcut")null else store.config(id,if(id.startsWith("vieneu"))9 else 0) ?: error("Install $id in QA first")
            store.select(null)
            val engine=if(id=="capcut")TTSEngineFactory.createCapCutEngine("BV421_vivn_streaming")!! else if(config==null)TTSEngineFactory.createNativeEngine() else TTSEngineFactory.createOfflineEngine(config)!!
            val controller=TTSController(loader,{engine})
            val gaps=mutableListOf<Long>();val loads=mutableListOf<Long>()
            var ended=0L;var preparing=0L;var started=0L
            val expected=if(id=="capcut")"CapCut" else if(config==null)"Native Android TTS" else config.model.title
            val observer=launch(start=CoroutineStart.UNDISPATCHED) {engine.events.collect {event->
                val now=System.nanoTime()
                when(event) {
                    is EngineEvent.Started-> {
                        if(started==0L)started=now
                        if(ended>0L){gaps+=(now-ended)/1_000_000;ended=0L}
                    }
                    is EngineEvent.Completed->ended=now
                    else->Unit
                }
            }}
            val stateObserver=launch(start=CoroutineStart.UNDISPATCHED) {controller.state.map {it.playbackState}.distinctUntilChanged().collect {state->
                val now=System.nanoTime()
                if(state==PlaybackState.LOADING)preparing=now
                else if(preparing>0L){loads+=(now-preparing)/1_000_000;preparing=0L}
            }}
            try {
                controller.dispatch(TTSCommand.Initialize);controller.dispatch(TTSCommand.LoadChapter(book.id,chapter.id))
                withTimeout(60_000){controller.state.first {it.hasContent && it.isEngineReady}}
                controller.dispatch(TTSCommand.SetSpeed(rate))
                val requested=System.nanoTime();controller.dispatch(TTSCommand.Play)
                withTimeout(180_000){controller.state.first {it.playbackState==PlaybackState.STOPPED && it.currentParagraphIndex==texts.lastIndex}}
                assertTrue("Unexpected fallback: ${engine.name}",engine.name.startsWith(expected))
                assertEquals(2,gaps.size)
                Log.i("TruyenPlusQA","LATENCY engine=$id rate=$rate firstAudioMs=${(started-requested)/1_000_000} paragraphGapsMs=$gaps loadingMs=$loads")
                assertTrue("Unbounded paragraph wait: $gaps",gaps.all {it<15_000})
            } finally {observer.cancel();stateObserver.cancel();controller.dispatch(TTSCommand.Cleanup)}
        }
    }

    @Test fun realControllerAdvancesChaptersAndTimerSurvivesBackgroundAndPause() = runBlocking<Unit> {
        requireQa()
        val store = GlobalContext.get().get<OfflineVoiceStore>()
        assertTrue("Run download test first", store.select(OfflineVoiceCatalog.models.first().id))
        val book = Book(id=900001, sourceId=-1, title="QA reading", key="qa-reader")
        val first = Chapter(id=900002, bookId=book.id, key="qa-1", name="Chương 1")
        val second = first.copy(id=900003, key="qa-2", name="Chương 2")
        val loader = object : TTSContentLoader {
            override suspend fun loadChapter(bookId:Long,chapterId:Long) = TTSContentLoader.ChapterContent(book,
                if(chapterId==first.id)first else second,
                if(chapterId==first.id)listOf("Gió đêm thổi qua.") else List(80){"Hoàng Yên Trần rút kiếm. Gió đêm mang hương hoa thoang thoảng."}, listOf(0))
            override suspend fun getNextChapterId(bookId:Long,currentChapterId:Long) = if(currentChapterId==first.id)second.id else null
            override suspend fun getPreviousChapterId(bookId:Long,currentChapterId:Long) = if(currentChapterId==second.id)first.id else null
            override fun subscribeChapters(bookId:Long) = flowOf(listOf(first,second))
        }
        val controller = TTSController(loader, { TTSEngineFactory.createNativeEngine() })
        val timer = TTSSleepTimerUseCase()
        val screenScope = CoroutineScope(SupervisorJob()+Dispatchers.Main)
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.SetAutoNextChapter(true))
            controller.dispatch(TTSCommand.LoadChapter(book.id,first.id))
            withTimeout(30_000){controller.state.first {it.hasContent && it.isEngineReady}}
            controller.dispatch(TTSCommand.Play)
            withTimeout(60_000){controller.state.first {it.chapter?.id==second.id && it.isPlaying}}
            assertEquals("Piper",controller.state.value.engineName)
            withContext(Dispatchers.Main){timer.initialize(controller,screenScope);timer.start(1)}
            screenScope.cancel()
            UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).pressHome()
            delay(7_000)
            assertTrue("Background time did not count",timer.getRemainingTimeMs()<57_000)
            controller.dispatch(TTSCommand.Pause)
            withTimeout(5_000){controller.state.first {!it.isPlaying && !it.isLoading}}
            delay(150)
            val paused = timer.getRemainingTimeMs()
            delay(3_000)
            assertTrue(timer.state.value.isPaused)
            assertEquals(paused,timer.getRemainingTimeMs())
            controller.dispatch(TTSCommand.Play)
            withTimeout(90_000){timer.state.first {!it.isEnabled}}
            withTimeout(5_000){controller.state.first {!it.isPlaying}}
            Log.i("TruyenPlusQA","PASS background timer expiration, pause/resume, automatic chapter continuation")
        } finally {
            withContext(Dispatchers.Main){timer.cleanup();controller.dispatch(TTSCommand.Cleanup)}
            screenScope.cancel();store.select(null)
        }
    }
    @Test fun vieneuKeepsVietnameseNamesNumbersAndPlaybackAfterPauseOffline() = runBlocking<Unit> {
        requireQa()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        val model=OfflineVoiceCatalog.find("vieneu-v3-nano-vi")!!
        check(store.config(model.id,9)!=null){"Run Nano download test first"}
        val engine=TTSEngineFactory.createOfflineEngine(store.config(model.id,9)!!)!!
        try {
            ready(engine);assertTrue(engine.name.startsWith(model.title))
            speakAndCheck(engine,"Hoàng Yên Trần khẽ gật đầu. Chương 273: Luận Kiếm Thiếp.","qa-nano-names")
            engine.speak("Đây là đoạn được hủy khi giọng đọc đang chuẩn bị âm thanh.","qa-nano-cancel")
            delay(120);engine.pause();delay(300)
            engine.setSpeed(2f)
            speakAndCheck(engine,"Gió đêm thổi qua, mang hương hoa thoang thoảng.","qa-nano-resume")
            assertTrue("System fallback must not pass",engine.name.startsWith(model.title))
        } finally {engine.release();store.select(null)}
    }

    @Test fun vieneuControllerContinuesToTheNextChapter() = runBlocking<Unit> {
        requireQa()
        val store=GlobalContext.get().get<OfflineVoiceStore>()
        assertTrue(store.select("vieneu-v3-nano-vi",9))
        val book=Book(id=910001,sourceId=-1,title="QA Nano",key="qa-nano")
        val first=Chapter(id=910002,bookId=book.id,key="qa-nano-1",name="Chương 1")
        val second=first.copy(id=910003,key="qa-nano-2",name="Chương 2")
        val loader=object:TTSContentLoader {
            override suspend fun loadChapter(bookId:Long,chapterId:Long)=TTSContentLoader.ChapterContent(book,
                if(chapterId==first.id)first else second,listOf("Hoàng Yên Trần khẽ gật đầu."),listOf(0))
            override suspend fun getNextChapterId(bookId:Long,currentChapterId:Long)=if(currentChapterId==first.id)second.id else null
            override suspend fun getPreviousChapterId(bookId:Long,currentChapterId:Long)=if(currentChapterId==second.id)first.id else null
            override fun subscribeChapters(bookId:Long)=flowOf(listOf(first,second))
        }
        val controller=TTSController(loader,{TTSEngineFactory.createNativeEngine()})
        try {
            controller.dispatch(TTSCommand.Initialize);controller.dispatch(TTSCommand.SetAutoNextChapter(true))
            controller.dispatch(TTSCommand.LoadChapter(book.id,first.id))
            withTimeout(30_000){controller.state.first {it.hasContent && it.isEngineReady}}
            controller.dispatch(TTSCommand.Play)
            withTimeout(90_000){controller.state.first {it.chapter?.id==second.id && it.isPlaying}}
            assertTrue("Fallback must not pass",controller.state.value.engineName.startsWith("VieNeu v3 Nano"))
        } finally {controller.dispatch(TTSCommand.Cleanup);store.select(null)}
    }

}
