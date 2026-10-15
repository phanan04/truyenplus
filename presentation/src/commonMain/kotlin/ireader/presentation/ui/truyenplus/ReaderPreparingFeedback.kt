package ireader.presentation.ui.truyenplus
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

/** Display feedback for an actual wait, without flashing during prepared paragraph handoffs. */
@OptIn(ExperimentalCoroutinesApi::class)
internal fun Flow<Boolean>.readerPreparingFeedback():Flow<Boolean> = distinctUntilChanged().transformLatest {loading->
 emit(false)
 if(loading){delay(350);emit(true)}
}.distinctUntilChanged()
@Composable internal fun rememberPreparingFeedback(loading:Boolean):Boolean {
 val current by rememberUpdatedState(loading)
 val shown by produceState(false) {snapshotFlow {current}.readerPreparingFeedback().collect {value=it}}
 return shown
}
