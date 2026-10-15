package ireader.presentation.ui.truyenplus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import kotlin.test.*
@OptIn(ExperimentalCoroutinesApi::class)
class ReaderPreparingFeedbackTest {
 @Test fun shortParagraphTransitionsNeverFlashButRealWaitsRemainVisible()=runTest {
  val loading=MutableStateFlow(false);val shown=mutableListOf<Boolean>()
  val job=backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)){loading.readerPreparingFeedback().collect {shown+=it}}
  repeat(3) {loading.value=true;advanceTimeBy(90);loading.value=false;runCurrent()}
  assertFalse(shown.any {it},"Prepared paragraph transitions must not flash a loading label")
  loading.value=true;advanceTimeBy(351);runCurrent();assertTrue(shown.last())
  loading.value=false;runCurrent();assertFalse(shown.last(),"Pause/start must clear feedback immediately")
  job.cancel()
 }
}
