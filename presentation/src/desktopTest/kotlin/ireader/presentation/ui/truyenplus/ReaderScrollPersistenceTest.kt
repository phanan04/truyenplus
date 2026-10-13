package ireader.presentation.ui.truyenplus

import ireader.domain.data.repository.ChapterRepository
import ireader.presentation.ui.reader.viewmodel.ReaderScrollManager
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReaderScrollPersistenceTest {
 @Test fun scrollingUpdatesMemoryImmediatelyAndBatchesDatabaseWrites()=runTest {
  val writes=mutableListOf<Pair<Long,Long>>()
  val repository=recordingRepository(writes)
  val manager=ReaderScrollManager(this,repository)
  repeat(50){manager.saveScrollPosition(7,it.toLong());runCurrent();advanceTimeBy(10)}
  assertEquals(49L,manager.currentScrollPosition)
  assertTrue(writes.isEmpty(),"Moving across paragraphs must not write once per paragraph")
  advanceUntilIdle()
  assertEquals(listOf(7L to 49L),writes)
 }
 @Test fun forceSaveKeepsTheOldChapterPositionAfterReset()=runTest {
  val writes=mutableListOf<Pair<Long,Long>>()
  val repository=recordingRepository(writes)
  val manager=ReaderScrollManager(this,repository)
  manager.updatePosition(70)
  manager.forceSaveScrollPosition(7)
  manager.resetForNewChapter(0)
  advanceUntilIdle()
  assertEquals(listOf(7L to 70L),writes)
 }
 @Test fun leavingBeforeRestorationDoesNotOverwriteSavedProgress()=runTest {
  val writes=mutableListOf<Pair<Long,Long>>()
  val manager=ReaderScrollManager(this,recordingRepository(writes))
  manager.forceSaveScrollPosition(7)
  advanceUntilIdle()
  assertTrue(writes.isEmpty())
  manager.updatePosition(0)
  manager.forceSaveScrollPosition(7)
  advanceUntilIdle()
  assertEquals(listOf(7L to 0L),writes)
 }
 private fun recordingRepository(writes:MutableList<Pair<Long,Long>>):ChapterRepository =
  java.lang.reflect.Proxy.newProxyInstance(ChapterRepository::class.java.classLoader,arrayOf(ChapterRepository::class.java)) { _,method,args ->
   if(method.name=="updateLastPageRead") {writes+=(args!![0] as Long) to (args[1] as Long);Unit}
   else throw UnsupportedOperationException(method.name)
  } as ChapterRepository
}
