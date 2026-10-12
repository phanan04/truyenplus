package ireader.presentation.ui.truyenplus

import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import ireader.domain.services.tts_service.v2.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class TTSContinuousPlaybackTest {
    private val dispatcher = StandardTestDispatcher()
    @BeforeTest fun setUp() { Dispatchers.setMain(dispatcher) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    private class Loader(val chapters: List<List<String>>, val sourceId: Long = 1) : TTSContentLoader {
        override suspend fun loadChapter(bookId: Long, chapterId: Long) = TTSContentLoader.ChapterContent(
            Book(id = bookId, title = "Book", key = "book", sourceId = sourceId),
            Chapter(id = chapterId, bookId = bookId, key = "$chapterId", name = "Chapter $chapterId",
                content = if (chapters[(chapterId - 1).toInt()].isEmpty()) listOf(ireader.core.source.model.ImageUrl("cover"))
                    else chapters[(chapterId - 1).toInt()].map { ireader.core.source.model.Text(it) }),
            chapters[(chapterId - 1).toInt()])
        override suspend fun getNextChapterId(bookId: Long, currentChapterId: Long) =
            (currentChapterId + 1).takeIf { it <= chapters.size }
        override suspend fun getPreviousChapterId(bookId: Long, currentChapterId: Long) =
            (currentChapterId - 1).takeIf { it > 0 }
        override fun subscribeChapters(bookId: Long) = emptyFlow<List<Chapter>>()
    }
    private class Engine : TTSEngine {
        override val events = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 64)
        override val name = "Test"
        val spoken = mutableListOf<Pair<String, String>>()
        var sendStarted = true
        override suspend fun speak(text: String, utteranceId: String) {
            spoken += text to utteranceId
            if (sendStarted) events.emit(EngineEvent.Started(utteranceId))
        }
        override fun isReady() = true
        override fun stop() {}
        override fun pause() {}
        override fun resume() {} // Android native resume is a no-op.
        override fun release() {}
        override fun setSpeed(speed: Float) {}
        override fun setPitch(pitch: Float) {}
        suspend fun complete() { events.emit(EngineEvent.Completed(spoken.last().second)) }
        suspend fun fail() { events.emit(EngineEvent.Error(spoken.last().second, "synthesis failure")) }
    }

    @Test fun playFromAppContinuesThroughBlankChapterTail() = runTest(dispatcher) {
        val engine = Engine()
        val controller = TTSController(Loader(listOf(listOf("First", "   "), listOf("Second"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            engine.complete()
            testScheduler.advanceUntilIdle()
            assertEquals(2L, controller.state.value.chapter?.id)
            assertEquals(listOf("First", "Second"), engine.spoken.map { it.first })
            assertTrue(controller.state.value.isPlaying)
        } finally { controller.destroy() }
    }
    @Test fun explicitAutoNextOffSurvivesPlayAndResume() = runTest(dispatcher) {
        val engine = Engine()
        val controller = TTSController(Loader(listOf(listOf("First"), listOf("Second"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            val adapter = TTSViewModelAdapter(controller, backgroundScope)
            adapter.play(autoNextChapter = false)
            testScheduler.advanceUntilIdle()
            adapter.pause(); testScheduler.advanceUntilIdle()
            adapter.play(autoNextChapter = false); testScheduler.advanceUntilIdle()
            engine.complete(); testScheduler.advanceUntilIdle()
            assertFalse(controller.state.value.autoNextChapter)
            assertEquals(1L, controller.state.value.chapter?.id)
        } finally { controller.destroy() }
    }
    @Test fun completionWithoutStartedStillReadsTheNextParagraph() = runTest(dispatcher) {
        val engine = Engine().apply { sendStarted = false }
        val controller = TTSController(Loader(listOf(listOf("...", "Readable text"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            engine.complete()
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("...", "Readable text"), engine.spoken.map { it.first })
            assertEquals(1, controller.state.value.currentParagraphIndex)
        } finally { controller.destroy() }
    }

    @Test fun unstartedFailedLastParagraphRetriesThenContinues() = runTest(dispatcher) {
        val engine = Engine().apply { sendStarted = false }
        val controller = TTSController(Loader(listOf(listOf("Temporary fault"), listOf("Next chapter"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            val failedId = engine.spoken.last().second
            engine.fail()
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("Temporary fault", "Temporary fault"), engine.spoken.map { it.first })
            engine.events.emit(EngineEvent.Error(failedId, "late duplicate"))
            testScheduler.advanceUntilIdle()
            assertEquals(1L, controller.state.value.chapter?.id, "Old errors must not cancel the retry")
            engine.fail()
            testScheduler.advanceUntilIdle()
            assertEquals(2L, controller.state.value.chapter?.id)
            assertEquals("Next chapter", engine.spoken.last().first)
            assertNull(controller.state.value.error)
        } finally { controller.destroy() }
    }

    @Test fun imageChapterIsSkippedThenRemotePlaybackCanPauseAndResume() = runTest(dispatcher) {
        val engine = Engine()
        val loader = Loader(listOf(listOf("First"), emptyList(), listOf("Third"), listOf("Fourth")),
            ireader.core.source.LocalSource.SOURCE_ID)
        val controller = TTSController(loader, { engine }, gradioEngineFactory = { engine },
            initialGradioConfig = GradioConfig("test", "test", "https://example.com"))
        try {
            controller.dispatch(TTSCommand.SetEngine(EngineType.GRADIO))
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            engine.complete()
            testScheduler.advanceUntilIdle()
            assertEquals(3L, controller.state.value.chapter?.id, "An imported image page cannot gain speech content remotely")
            controller.dispatch(TTSCommand.Pause)
            testScheduler.advanceUntilIdle()
            assertTrue(controller.state.value.isPaused)
            controller.dispatch(TTSCommand.Resume)
            testScheduler.advanceUntilIdle()
            engine.complete()
            testScheduler.advanceUntilIdle()
            assertEquals(4L, controller.state.value.chapter?.id)
        } finally { controller.destroy() }
    }

    @Test fun nativeResumeRespeaksAndLatePausedCallbacksDoNotRestart() = runTest(dispatcher) {
        val engine = Engine()
        val controller = TTSController(Loader(listOf(listOf("First", "Second"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            val oldId = engine.spoken.last().second
            controller.dispatch(TTSCommand.Pause)
            testScheduler.advanceUntilIdle()
            engine.events.emit(EngineEvent.Started(oldId))
            engine.events.emit(EngineEvent.Completed(oldId))
            testScheduler.advanceUntilIdle()
            assertTrue(controller.state.value.isPaused)
            controller.dispatch(TTSCommand.Resume)
            testScheduler.advanceUntilIdle()
            assertEquals(listOf("First", "First"), engine.spoken.map { it.first })
            engine.events.emit(EngineEvent.Completed(oldId))
            testScheduler.advanceUntilIdle()
            assertEquals(0, controller.state.value.currentParagraphIndex)
            engine.complete()
            testScheduler.advanceUntilIdle()
            assertEquals("Second", engine.spoken.last().first)
        } finally { controller.destroy() }
    }

    @Test fun mergedChunksContinueIntoTheNextChapter() = runTest(dispatcher) {
        val engine = Engine()
        val controller = TTSController(Loader(listOf(
            listOf("one two three four five six seven eight nine ten", "eleven twelve thirteen fourteen fifteen sixteen"),
            listOf("Next chapter"))), { engine })
        try {
            controller.dispatch(TTSCommand.Initialize)
            controller.dispatch(TTSCommand.LoadChapter(1, 1))
            controller.dispatch(TTSCommand.EnableChunkMode(5))
            testScheduler.advanceUntilIdle()
            TTSViewModelAdapter(controller, backgroundScope).play()
            testScheduler.advanceUntilIdle()
            val count = controller.state.value.totalChunks
            assertTrue(count > 1)
            repeat(count) { engine.complete(); testScheduler.advanceUntilIdle() }
            assertEquals(2L, controller.state.value.chapter?.id)
            assertEquals("Next chapter", engine.spoken.last().first)
        } finally { controller.destroy() }
    }

    @Test fun sentenceSplitterKeepsExplicitLimitsAndSingleLetterResponses() {
        val text = "你好世界你好世界你好世界你好世界你好世界"
        val chunks = TTSSentenceSplitter.split(text, maxChunkLength = 5)
        assertTrue(chunks.all { it.length <= 5 })
        assertEquals(text, chunks.joinToString(""))
        assertEquals(listOf("Ừ"), TTSSentenceSplitter.split("Ừ"))
    }

}
