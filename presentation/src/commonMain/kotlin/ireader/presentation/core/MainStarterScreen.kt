package ireader.presentation.core

import androidx.compose.runtime.Composable
import ireader.presentation.ui.truyenplus.ReaderShellRoute
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*

val LocalVisibleTabIndex = androidx.compose.runtime.compositionLocalOf { -1 }

/** Public shell: Home, local Library and reading Progress only. */
object MainStarterScreen {
    private var pendingIndex: Int? = null
    private val requests = Channel<Int>(Channel.BUFFERED)
    val tabRequests: Flow<Int> = requests.receiveAsFlow()
    private val filterRequests = Channel<Boolean>(Channel.BUFFERED)
    fun libraryFilterSheetFlow() = filterRequests.receiveAsFlow()
    suspend fun switchToTab(tabIndex: Int) { requests.send(tabIndex.takeIf { it in 0..2 } ?: 0) }
    fun setInitialTab(tabIndex: Int) { pendingIndex = tabIndex.takeIf { it in 0..2 } ?: 0 }
    internal fun consumePendingTab(): Int? = pendingIndex.also { pendingIndex = null }
    suspend fun showBottomNav(show: Boolean) { /* Selection is owned by the reduced reader shell. */ }
    @Composable operator fun invoke() { ReaderShellRoute(consumePendingTab() ?: 0) }
}
