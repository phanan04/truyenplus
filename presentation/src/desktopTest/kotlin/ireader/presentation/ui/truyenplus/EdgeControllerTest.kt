package ireader.presentation.ui.truyenplus

import ireader.domain.services.tts_service.v2.*
import ireader.domain.models.entities.Book
import ireader.domain.models.entities.Chapter
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class EdgeControllerTest {
    private class Loader : TTSContentLoader {
        override suspend fun loadChapter(bookId: Long, chapterId: Long) = TTSContentLoader.ChapterContent(
            Book(id = bookId, title = "Truyện", key = "book", sourceId = 1),
            Chapter(id = chapterId, bookId = bookId, name = "Chương", key = "chapter"), listOf("Đoạn một", "Đoạn hai"))
        override suspend fun getNextChapterId(bookId: Long, currentChapterId: Long): Long? = null
        override suspend fun getPreviousChapterId(bookId: Long, currentChapterId: Long): Long? = null
        override fun subscribeChapters(bookId: Long): Flow<List<Chapter>> = flowOf(emptyList())
    }
    private class Engine : TTSEngine {
        val emitted = MutableSharedFlow<EngineEvent>(extraBufferCapacity = 10)
        val spoken = mutableListOf<Pair<String, String>>()
        override val events = emitted
        override val name = "test"
        override suspend fun speak(text: String, utteranceId: String) { spoken += text to utteranceId; emitted.emit(EngineEvent.Started(utteranceId)) }
        override fun stop() {}
        override fun pause() {}
        override fun resume() {}
        override fun release() {}
        override fun setSpeed(speed: Float) {}
        override fun setPitch(pitch: Float) {}
        override fun isReady() = true
    }
    @Test fun failureFallsBackAtSameParagraphAndRejectsStaleEvents() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val native = Engine(); val edge = Engine()
        val controller = TTSController(Loader(), { native }, edgeEngineFactory = { edge })
        try {
            controller.dispatch(TTSCommand.SetEdgeVoice(EdgeVoice.NamMinh));advanceUntilIdle()
            controller.dispatch(TTSCommand.LoadChapter(1, 2, 1));advanceUntilIdle()
            controller.dispatch(TTSCommand.Play);advanceUntilIdle()
            assertEquals("Đoạn hai", edge.spoken.single().first)
            val oldId = edge.spoken.single().second
            edge.emitted.emit(EngineEvent.Error(oldId, "No network"));advanceUntilIdle()
            assertEquals(EngineType.NATIVE, controller.state.value.engineType)
            assertTrue(controller.state.value.edgeFallback)
            assertEquals(1, controller.state.value.currentParagraphIndex)
            assertEquals("Đoạn hai", native.spoken.single().first)
            edge.emitted.emit(EngineEvent.Completed(oldId));advanceUntilIdle()
            assertEquals(1, controller.state.value.currentParagraphIndex)
            assertTrue(controller.state.value.isPlaying)
        } finally { controller.destroy();Dispatchers.resetMain() }
    }
    @Test fun changingVoicePreservesPositionAndPausedState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val created = mutableListOf<Pair<EdgeVoice, Engine>>()
        val controller = TTSController(Loader(), { Engine() }, edgeEngineFactory = { voice -> Engine().also { created += voice to it } })
        try {
            controller.dispatch(TTSCommand.SetEdgeVoice(EdgeVoice.HoaiMy));advanceUntilIdle()
            controller.dispatch(TTSCommand.LoadChapter(1, 2, 1));advanceUntilIdle()
            controller.dispatch(TTSCommand.Play);advanceUntilIdle()
            controller.dispatch(TTSCommand.SetEdgeVoice(EdgeVoice.NamMinh));advanceUntilIdle()
            assertEquals(2, created.size)
            assertEquals("Đoạn hai", created.last().second.spoken.single().first)
            controller.dispatch(TTSCommand.Pause);advanceUntilIdle()
            controller.dispatch(TTSCommand.SetEdgeVoice(EdgeVoice.HoaiMy));advanceUntilIdle()
            assertTrue(controller.state.value.isPaused)
            assertEquals(1, controller.state.value.currentParagraphIndex)
            assertTrue(created.last().second.spoken.isEmpty())
            // A freshly created paused engine has no internal utterance to resume; controller must start it.
            controller.dispatch(TTSCommand.Resume);advanceUntilIdle()
            assertEquals("Đoạn hai", created.last().second.spoken.single().first)
        } finally { controller.destroy();Dispatchers.resetMain() }
    }
    @Test fun audioFocusGainNeverRestartsActiveOrUserPausedSpeech() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val native = Engine()
        val controller = TTSController(Loader(), { native })
        try {
            controller.dispatch(TTSCommand.LoadChapter(1, 2));advanceUntilIdle()
            controller.dispatch(TTSCommand.Play);advanceUntilIdle()
            controller.dispatch(TTSCommand.AudioFocusGained);advanceUntilIdle()
            controller.dispatch(TTSCommand.AudioFocusGained);advanceUntilIdle()
            controller.dispatch(TTSCommand.Resume);advanceUntilIdle()
            assertEquals(1, native.spoken.size, "Gaining focus must never restart already active speech")
            controller.dispatch(TTSCommand.AudioFocusLost(transient = true));advanceUntilIdle()
            assertTrue(controller.state.value.isPaused)
            controller.dispatch(TTSCommand.AudioFocusGained);advanceUntilIdle()
            assertEquals(2, native.spoken.size)
            controller.dispatch(TTSCommand.Pause);advanceUntilIdle()
            controller.dispatch(TTSCommand.AudioFocusGained);advanceUntilIdle()
            assertEquals(2, native.spoken.size, "Explicit pause must survive a focus gain")
            controller.dispatch(TTSCommand.Play);advanceUntilIdle()
            controller.dispatch(TTSCommand.AudioFocusLost(transient = false));advanceUntilIdle()
            controller.dispatch(TTSCommand.AudioFocusGained);advanceUntilIdle()
            assertEquals(3, native.spoken.size, "Permanent focus loss must not auto resume")
        } finally { controller.destroy();Dispatchers.resetMain() }
    }

}
